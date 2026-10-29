package com.safemedi.app.sefemedi.domain.family.repository

import com.safemedi.app.sefemedi.domain.family.entity.Family
import com.safemedi.app.sefemedi.domain.user.entity.User
import com.safemedi.app.sefemedi.domain.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest(properties = [
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:family-sharing-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;NON_KEYWORDS=USER",
])
@ActiveProfiles("test")
@Transactional
class FamilySharingRepositoryTest @Autowired constructor(
    private val familyRepository: FamilyRepository,
    private val userRepository: UserRepository,
) {
    @Test
    fun `공개 설정은 정보를 소유한 사용자의 방향으로 조회한다`() {
        val requester = userRepository.save(User(socialId = "sharing-requester"))
        val target = userRepository.save(User(socialId = "sharing-target"))
        val requesterId = requireNotNull(requester.id)
        val targetId = requireNotNull(target.id)
        familyRepository.saveAllAndFlush(
            listOf(
                Family(user = requester, connectedUser = target, relation = "가족", isAllowMyInfo = true),
                Family(user = target, connectedUser = requester, relation = "가족", isAllowMyInfo = false),
            ),
        )

        val reverse = familyRepository.findByUser_IdAndConnectedUser_Id(targetId, requesterId)
        assertNotNull(reverse)
        assertEquals(targetId, reverse.user.id)
        assertEquals(false, reverse.isAllowMyInfo)
        assertEquals(true, familyRepository.findByUser_IdAndConnectedUser_Id(requesterId, targetId)?.isAllowMyInfo)
    }

    @Test
    fun `정방향 연결만 있으면 역방향 연결은 조회되지 않는다`() {
        val requester = userRepository.save(User(socialId = "one-way-requester"))
        val target = userRepository.save(User(socialId = "one-way-target"))
        familyRepository.saveAndFlush(Family(user = requester, connectedUser = target, relation = "가족"))

        assertNull(
            familyRepository.findByUser_IdAndConnectedUser_Id(requireNotNull(target.id), requireNotNull(requester.id)),
        )
    }
}
