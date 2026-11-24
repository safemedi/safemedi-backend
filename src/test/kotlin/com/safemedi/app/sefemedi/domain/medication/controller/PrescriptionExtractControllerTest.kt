package com.safemedi.app.sefemedi.domain.medication.controller

import com.safemedi.app.sefemedi.domain.medication.dto.PrescriptionExtractResponse
import com.safemedi.app.sefemedi.domain.medication.service.PrescriptionExtractService
import com.safemedi.app.sefemedi.global.error.GlobalExceptionHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class PrescriptionExtractControllerTest {
    private val service = mock(PrescriptionExtractService::class.java)
    private val mvc = MockMvcBuilders.standaloneSetup(PrescriptionExtractController(service))
        .setControllerAdvice(GlobalExceptionHandler())
        .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver()).build()
    private val image = MockMultipartFile("image", "photo.jpg", "image/jpeg", byteArrayOf(1))

    @AfterEach
    fun cleanup() { SecurityContextHolder.clearContext() }

    @Test
    fun `인증 사용자 사진을 서비스로 전달하고 초안을 반환한다`() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("user", null)
        `when`(service.extract("user", image)).thenReturn(PrescriptionExtractResponse("처방전", null, null, emptyList()))
        mvc.perform(multipart("/api/v1/prescriptions/extract").file(image))
            .andExpect(status().isOk).andExpect(jsonPath("$.title").value("처방전"))
        verify(service).extract("user", image)
    }

    @Test
    fun `사진 누락이나 여러 사진 업로드는 거부한다`() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("user", null)
        mvc.perform(multipart("/api/v1/prescriptions/extract"))
            .andExpect(status().isBadRequest)
        mvc.perform(multipart("/api/v1/prescriptions/extract").file(image).file(image))
            .andExpect(status().isBadRequest)
        verifyNoInteractions(service)
    }

    @Test
    fun `인증 정보가 없으면 AI 서비스를 호출하지 않는다`() {
        mvc.perform(multipart("/api/v1/prescriptions/extract").file(image))
            .andExpect(status().isUnauthorized)
        verifyNoInteractions(service)
    }
}
