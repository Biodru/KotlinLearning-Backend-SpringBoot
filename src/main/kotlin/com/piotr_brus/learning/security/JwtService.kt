package com.piotr_brus.learning.security

import com.piotr_brus.learning.database.model.TokenType
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

private const val tokenPrefix = "Bearer "
private const val typeName = "type"
private const val invalidTokenMessage = "Invalid token."

@Service
class JwtService(
    @Value("\${jwt.secret}") private val jwtSecret: String
) {
    private val secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))
    private val accessTokenValidityMs = 15L * 60L * 1000L
    val refreshTokenValidityMs = 30L * 24 * 60 * 1000L

    private fun generateToken(
        userId: String,
        type: String,
        expiry: Long
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + expiry)
        return Jwts.builder()
            .subject(userId)
            .claim(typeName, type)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()

    }

    fun generateToken(userId: String, tokenType: TokenType): String {
        return generateToken(
            userId,
            tokenType.type,
            if (tokenType == TokenType.ACCESS) accessTokenValidityMs else refreshTokenValidityMs
        )
    }

    fun validateToken(token: String, tokenType: TokenType): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val claimsTokenType = claims["type"] as? String ?: return false
        return claimsTokenType == tokenType.type
    }

    fun getUserIdFromToken(token: String): String {
        val claims =
            parseAllClaims(token) ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), invalidTokenMessage)
        return claims.subject
    }

    private fun parseAllClaims(token: String): Claims? {
        val rawToken = if (token.startsWith(tokenPrefix)) {
            token.removePrefix(tokenPrefix)
        } else token
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(rawToken)
                .payload
        } catch (e: Exception) {
            null
        }
    }
}