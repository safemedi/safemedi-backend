package com.safemedi.app.sefemedi.domain.user.repository

import com.safemedi.app.sefemedi.domain.user.dto.DeviceTokenDeactivateRequest
import com.safemedi.app.sefemedi.domain.user.dto.DeviceTokenRequest
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.entity.UserDevice
import com.safemedi.app.sefemedi.domain.user.service.UserService
import com.safemedi.app.sefemedi.global.error.BusinessException
import com.safemedi.app.sefemedi.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@SpringBootTest(properties = [
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:device-concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;NON_KEYWORDS=USER",
])
@ActiveProfiles("test")
class UserDeviceConcurrencyTest {
    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var deviceRepository: UserDeviceRepository
    @Autowired
    private lateinit var userService: UserService
    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Test
    fun `계정 전환 중 이전 계정의 삭제는 커밋을 기다린 뒤 현재 소유권을 검증한다`() {
        transactionTemplate.executeWithoutResult {
            val previous = userRepository.save(User(socialId = "previous-owner"))
            userRepository.save(User(socialId = "new-owner"))
            deviceRepository.save(UserDevice(user = previous, deviceToken = "shared-device", deviceType = "ANDROID"))
        }
        val registered = CountDownLatch(1)
        val releaseRegistration = CountDownLatch(1)
        val deletionStarted = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val registration = executor.submit {
                transactionTemplate.executeWithoutResult {
                    userService.registerDeviceToken("new-owner", DeviceTokenRequest("shared-device", "ANDROID"))
                    registered.countDown()
                    check(releaseRegistration.await(10, TimeUnit.SECONDS))
                }
            }
            assertTrue(registered.await(10, TimeUnit.SECONDS))
            val deletion = executor.submit<BusinessException> {
                deletionStarted.countDown()
                assertThrows(BusinessException::class.java) {
                    userService.deactivateDeviceToken("previous-owner", DeviceTokenDeactivateRequest("shared-device"))
                }
            }
            assertTrue(deletionStarted.await(10, TimeUnit.SECONDS))
            assertThrows(TimeoutException::class.java) { deletion.get(200, TimeUnit.MILLISECONDS) }
            releaseRegistration.countDown()
            registration.get(10, TimeUnit.SECONDS)
            assertEquals(ErrorCode.DEVICE_TOKEN_ACCESS_DENIED, deletion.get(10, TimeUnit.SECONDS).errorCode)

            transactionTemplate.executeWithoutResult {
                val device = requireNotNull(deviceRepository.findByDeviceToken("shared-device"))
                assertEquals("new-owner", device.user.socialId)
                assertTrue(device.isActive)
            }
        } finally {
            releaseRegistration.countDown()
            executor.shutdownNow()
        }
    }
}
