package com.mktiti.fsearch.backend.auth

import com.mktiti.fsearch.rest.api.User
import java.time.Instant

interface AuthenticationService {

    object Nop : AuthenticationService {
        override fun checkCredentials(username: String, password: String): Nothing? = null
    }

    fun checkCredentials(username: String, password: String): User?

}

interface JwtService {

    sealed interface JwtContent {
        sealed interface Invalid : JwtContent {
            object BadFormat : Invalid
            object Expired : Invalid
        }
        class Valid(val user: User) : JwtContent
    }

    data class BlacklistEntry(
            val username: String?,
            val before: Instant
    )

    fun blacklist(username: String, before: Instant = Instant.now()) = blacklist(BlacklistEntry(username, before))

    fun blacklistAll(before: Instant = Instant.now()) = blacklist(BlacklistEntry(null, before))

    fun blacklist(entry: BlacklistEntry)

    fun blacklisted(): Collection<BlacklistEntry>

    fun isBlacklisted(username: String, issuedAt: Instant): Boolean

    fun parse(jwtString: String): JwtContent

    fun issueJwt(user: User, expiry: Instant? = null): String

}
