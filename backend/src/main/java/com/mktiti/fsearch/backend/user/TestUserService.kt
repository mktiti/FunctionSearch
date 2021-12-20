package com.mktiti.fsearch.backend.user

import com.mktiti.fsearch.rest.api.Role
import com.mktiti.fsearch.rest.api.User
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap

class TestUserService(
        testUsers: List<TestUser>
) : UserService {

    private val userMap = ConcurrentHashMap(testUsers.associate { user ->
        user.username to UserInfo(User(user.username, Role.USER), user.registerDate.atStartOfDay().toInstant(ZoneOffset.UTC), user.level)
    })

    override fun userInfo(username: String): UserInfo? = userMap[username]

    override fun createUser(user: User, level: Level): Boolean {
        val newUser = UserInfo(User(user.username, Role.USER), Instant.now(), level)
        return userMap.putIfAbsent(user.username, newUser) == null
    }

    override fun userCount(): Long = userMap.size.toLong()

}