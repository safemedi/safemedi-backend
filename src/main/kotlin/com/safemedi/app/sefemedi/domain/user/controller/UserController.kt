package com.safemedi.app.sefemedi.domain.user.controller

import com.safemedi.app.sefemedi.domain.user.dto.DeviceTokenDeactivateRequest
import com.safemedi.app.sefemedi.domain.user.dto.DeviceTokenDeactivateResponse
import com.safemedi.app.sefemedi.domain.user.dto.DeviceTokenRequest
import com.safemedi.app.sefemedi.domain.user.dto.DeviceTokenResponse
import com.safemedi.app.sefemedi.domain.user.dto.TutorialRequest
import com.safemedi.app.sefemedi.domain.user.dto.TutorialResponse
import com.safemedi.app.sefemedi.domain.user.dto.UserProfileUpdateRequest
import com.safemedi.app.sefemedi.domain.user.dto.UserNotificationSettingsResponse
import com.safemedi.app.sefemedi.domain.user.dto.UserNotificationSettingsUpdateRequest
import com.safemedi.app.sefemedi.domain.user.dto.UserProfileResponse
import com.safemedi.app.sefemedi.domain.user.dto.UserWithdrawalResponse
import com.safemedi.app.sefemedi.domain.user.service.UserService
import com.safemedi.app.sefemedi.domain.user.service.UserWithdrawalService
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
    private val userWithdrawalService: UserWithdrawalService,
) {

    @GetMapping("/me")
    fun getMyProfile(
        authentication: Authentication,
    ): UserProfileResponse {
        return userService.getMyProfile(
            authentication.name,
        )
    }

    @PatchMapping("/me")
    fun updateMyProfile(
        authentication: Authentication,
        @RequestBody request: UserProfileUpdateRequest,
    ): UserProfileResponse {
        return userService.updateMyProfile(
            authentication.name,
            request,
        )
    }

    @GetMapping("/notification-settings")
    fun getNotificationSettings(
        authentication: Authentication,
    ): UserNotificationSettingsResponse {
        return userService.getNotificationSettings(
            authentication.name,
        )
    }

    @PatchMapping("/notification-settings")
    fun updateNotificationSettings(
        authentication: Authentication,
        @RequestBody request: UserNotificationSettingsUpdateRequest,
    ): UserNotificationSettingsResponse {
        return userService.updateNotificationSettings(
            authentication.name,
            request,
        )
    }

    @PostMapping("/me/tutorial")
    @ResponseStatus(HttpStatus.CREATED)
    fun completeTutorial(
        authentication: Authentication,
        @RequestBody request: TutorialRequest,
    ): TutorialResponse {
        return userService.completeTutorial(
            authentication.name,
            request,
        )
    }

    @PostMapping("/device-token")
    fun registerDeviceToken(
        authentication: Authentication,
        @RequestBody request: DeviceTokenRequest,
    ): DeviceTokenResponse {
        return userService.registerDeviceToken(
            authentication.name,
            request,
        )
    }

    @DeleteMapping("/device-token")
    fun deactivateDeviceToken(
        authentication: Authentication,
        @RequestParam deviceToken: String,
    ): DeviceTokenDeactivateResponse {
        return userService.deactivateDeviceToken(
            authentication.name,
            DeviceTokenDeactivateRequest(deviceToken),
        )
    }

    @DeleteMapping("/me")
    fun withdrawMyAccount(
        authentication: Authentication,
    ): UserWithdrawalResponse {
        return userWithdrawalService.withdrawMyAccount(
            authentication.name,
        )
    }
}
