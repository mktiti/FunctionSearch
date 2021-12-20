package com.mktiti.fsearch.backend.handler

import com.mktiti.fsearch.backend.user.UserInfo
import com.mktiti.fsearch.backend.user.UserService
import com.mktiti.fsearch.rest.api.handler.UserHandler
import com.mktiti.fsearch.dto.UserInfo as UserInfoDto

class BasicUserHandler(
        private val userService: UserService
) : UserHandler {

    override fun selfData(selfUsername: String): UserInfoDto {
        val info: UserInfo = userService.userInfo(selfUsername) ?: error("Data not found for logged-in user")
        return info.toDto()
    }

}