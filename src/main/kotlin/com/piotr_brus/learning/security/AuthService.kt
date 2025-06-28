package com.piotr_brus.learning.security

import com.piotr_brus.learning.database.model.RefreshToken
import com.piotr_brus.learning.database.model.TokenType
import com.piotr_brus.learning.database.model.User
import com.piotr_brus.learning.database.repository.RefreshTokenRepository
import com.piotr_brus.learning.database.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Instant
import java.util.*

private const val userExistsMessage = "A user with that email, already exists."
private const val invalidCredentialsMessage = "Invalid credentials."
private const val hashAlgorithm = "SHA-256"
private const val invalidRefreshTokenMessage = "Invalid refreshToken."

@Service
class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    data class TokenPair(
        val accessToken: String,
        val refreshToken: String
    )

    fun register(email: String, password: String): User {
        val user = userRepository.findByEmail(email.trim())
        if (user != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, userExistsMessage)
        }
        return userRepository.save(
            User(
                email = email,
                hashedPassword = hashEncoder.encode(password)
            )
        )
    }

    fun login(email: String, password: String): TokenPair {
        val user = userRepository.findByEmail(email)
            ?: throw BadCredentialsException(invalidCredentialsMessage)

        if (!hashEncoder.matches(password, user.hashedPassword)) {
            throw BadCredentialsException(invalidCredentialsMessage)
        }

        val newAccessToken = jwtService.generateToken(user.id.toHexString(), tokenType = TokenType.ACCESS)
        val newRefreshToken = jwtService.generateToken(user.id.toHexString(), tokenType = TokenType.REFRESH)

        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
        )
    }

    @Transactional
    fun refresh(refreshToken: String): TokenPair {
        if (!jwtService.validateToken(token = refreshToken, tokenType = TokenType.REFRESH)) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), invalidRefreshTokenMessage)
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)
        val user = userRepository.findById(ObjectId(userId)).orElseThrow {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), invalidRefreshTokenMessage)
        }

        val hashed = hashToken(refreshToken)
        refreshTokenRepository.findByUserIdAndHashedToken(user.id, hashed) ?: throw ResponseStatusException(
            HttpStatusCode.valueOf(401),
            invalidRefreshTokenMessage
        )

        refreshTokenRepository.deleteByUserIdAndHashedToken(user.id, hashed)

        val newAccessToken = jwtService.generateToken(userId, TokenType.ACCESS)
        val newRefreshToken = jwtService.generateToken(userId, TokenType.REFRESH)

        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
        )
    }

    private fun storeRefreshToken(userId: ObjectId, rawRefreshToken: String) {
        val hashed = hashToken(rawRefreshToken)
        val expiryMs = jwtService.refreshTokenValidityMs
        val expiresAt = Instant.now().plusMillis(expiryMs)

        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                expiresAt = expiresAt,
                hashed,
            )
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance(hashAlgorithm)
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}