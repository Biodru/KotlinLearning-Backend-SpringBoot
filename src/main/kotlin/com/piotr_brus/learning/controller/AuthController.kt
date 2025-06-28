package com.piotr_brus.learning.controller

import com.piotr_brus.learning.security.AuthService
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private const val invalidEmailMessage = "Invalid e-mail format."
private const val passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{9,}\$"
private const val invalidPasswordMessage =
    "Password must be at least 9 characters long and contain at least one digit, uppercase and lowercase character."

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {
    data class AuthRequest(
        @field:Email(message = invalidEmailMessage)
        val email: String,
        @field:Pattern(
            regexp = passwordRegex,
            message = invalidPasswordMessage,
        )
        val password: String,
    )

    data class RefreshRequest(
        val refreshToken: String
    )

    @PostMapping("/register")
    fun register(
        @RequestBody body: AuthRequest
    ) {
        authService.register(body.email, body.password)
    }

    @PostMapping("/login")
    fun login(
        @RequestBody body: AuthRequest
    ): AuthService.TokenPair {
        return authService.login(body.email, body.password)
    }

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody body: RefreshRequest
    ): AuthService.TokenPair {
        return authService.refresh(body.refreshToken)
    }
}