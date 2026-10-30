package com.safemedi.app.sefemedi.global.config

import com.safemedi.app.sefemedi.global.error.ErrorCode
import com.safemedi.app.sefemedi.global.error.ErrorResponse
import com.safemedi.app.sefemedi.global.jwt.JwtAuthenticationFilter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import tools.jackson.databind.json.JsonMapper

@Configuration
class SecurityConfig(

    private val jsonMapper: JsonMapper,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {

    @Bean
    fun filterChain(
        http: HttpSecurity,
    ): SecurityFilterChain {

        http
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS,
                )
            }
            .authorizeHttpRequests {
                it.requestMatchers("/auth/logout", "/api/v1/auth/logout").authenticated()

                it.requestMatchers(
                    "/auth/**",
                    "/api/v1/auth/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/actuator/health",
                    "/actuator/prometheus",
                    "/.well-known/**",
                ).permitAll()

                it.requestMatchers(
                    "/api/v1/users/me",
                    "/api/v1/users/me/medical-summary",
                    "/api/v1/users/me/tutorial",
                    "/api/v1/users/device-token",
                    "/api/v1/users/notification-settings",
                    "/api/v1/prescriptions/analyze",
                    "/api/v1/prescriptions",
                    "/api/v1/prescriptions/**",
                    "/api/v1/families",
                    "/api/v1/families/**",
                    "/api/v1/family-invitations",
                    "/api/v1/family-invitations/**",
                    "/api/v1/medication-records/today",
                    "/api/v1/medication-records/**",
                    "/api/v1/notifications/**",
                    "/api/v1/drugs/search",
                    "/api/v1/diseases/search",
                    "/api/v1/allergies/drugs/search",
                    "/api/v1/map/**",
                ).authenticated()

                it.anyRequest().denyAll()
            }
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, _ ->
                    writeErrorResponse(
                        response = response,
                    )
                }
                it.accessDeniedHandler { _, response, _ ->
                    response.status = HttpStatus.FORBIDDEN.value()
                }
            }
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java,
            )

        return http.build()
    }

    private fun writeErrorResponse(
        response: HttpServletResponse,
    ) {
        val errorCode = ErrorCode.INVALID_ACCESS_TOKEN
        response.status = errorCode.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        jsonMapper.writeValue(
            response.writer,
            ErrorResponse(
                code = errorCode.code,
                message = errorCode.message,
            ),
        )
    }
}
