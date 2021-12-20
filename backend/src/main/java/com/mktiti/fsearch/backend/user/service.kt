package com.mktiti.fsearch.backend.user

import com.mktiti.fsearch.rest.api.User

interface UserService {

    fun userInfo(username: String): UserInfo?

    fun createUser(user: User, level: Level): Boolean

    fun userCount(): Long

}