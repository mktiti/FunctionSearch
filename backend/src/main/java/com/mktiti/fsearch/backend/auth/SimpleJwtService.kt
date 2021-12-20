package com.mktiti.fsearch.backend.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.mktiti.fsearch.backend.ProjectInfo
import com.mktiti.fsearch.backend.auth.JwtService.BlacklistEntry
import com.mktiti.fsearch.backend.auth.JwtService.JwtContent.Invalid.BadFormat
import com.mktiti.fsearch.backend.auth.JwtService.JwtContent.Invalid.Expired
import com.mktiti.fsearch.backend.auth.JwtService.JwtContent.Valid
import com.mktiti.fsearch.rest.api.Role
import com.mktiti.fsearch.rest.api.User
import com.mktiti.fsearch.util.logError
import com.mktiti.fsearch.util.logInfo
import com.mktiti.fsearch.util.logger
import java.time.Instant
import java.util.Date.from

class SimpleJwtService(
        private val config: JwtConfig
) : JwtService {

    companion object {
        private const val usernameClaim = "username"
        private const val roleClaim = "role"

        private val issuer = "JvmSearch v${ProjectInfo.version}"
    }

    private val log = logger()

    private val algorithm = try {
        Algorithm.HMAC256(config.secret)
    } catch (illegalArg: IllegalArgumentException) {
        log.logError(illegalArg) { "Failed to create JWT algorithm (HMAC256), possible problem with secret" }
        error("Failed to create JWT algorithm")
    }

    private val verifier = JWT.require(algorithm)
            .withIssuer(issuer)
            .withClaimPresence(usernameClaim)
            .withClaimPresence(roleClaim)
            .build()

    override fun parse(jwtString: String): JwtService.JwtContent {
        fun onError(exception: Exception): BadFormat {
            log.logInfo(exception) { "JWT parsing failed/JWT invalid - $jwtString" }
            return BadFormat
        }

        return try {
            val token = verifier.verify(jwtString)
            val username = token.getClaim(usernameClaim)?.asString() ?: return BadFormat
            val role = token.getClaim(roleClaim)?.asString()?.let(Role::valueOf) ?: return BadFormat

            return if (isBlacklisted(username, token.issuedAt.toInstant())) {
                Expired
            } else {
                Valid(User(username, role))
            }

        } catch (expired: TokenExpiredException) {
            Expired
        } catch (verificationException: JWTVerificationException) {
            onError(verificationException)
        } catch (decodeException: JWTDecodeException) {
            onError(decodeException)
        } catch (decodeException: JWTDecodeException) {
            onError(decodeException)
        }
    }

    private fun defaultExpiry(): Instant? = config.expiry?.let { exp ->
        Instant.now() + exp
    }

    override fun issueJwt(user: User, expiry: Instant?): String {
        return JWT.create()
                .withClaim(usernameClaim, user.username)
                .withClaim(roleClaim, user.role.name)
                .withExpiresAt(from(expiry ?: defaultExpiry()))
                .withIssuedAt(from(Instant.now()))
                .withIssuer(issuer)
                .sign(algorithm)
    }

    // TODO
    override fun blacklist(entry: BlacklistEntry) {}

    // TODO
    override fun blacklisted() = emptyList<BlacklistEntry>()

    // TODO
    override fun isBlacklisted(username: String, issuedAt: Instant): Boolean = false

}