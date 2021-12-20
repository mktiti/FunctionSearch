package com.mktiti.fsearch.rest.api.handler

import com.mktiti.fsearch.dto.UserInfo
import com.mktiti.fsearch.dto.UserLevel

interface UserHandler {

    object Nop : UserHandler {
        override fun selfData(selfUsername: String): UserInfo = UserInfo("", UserLevel.NORMAL, emptyList())
    }

    fun selfData(selfUsername: String): UserInfo

}
