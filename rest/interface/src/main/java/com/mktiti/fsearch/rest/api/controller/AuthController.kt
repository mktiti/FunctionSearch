package com.mktiti.fsearch.rest.api.controller

import com.mktiti.fsearch.dto.Credentials
import com.mktiti.fsearch.dto.LoginResult
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody

interface AuthController {

    @PostMapping("/login")
    @ResponseBody
    fun login(@RequestBody credentials: Credentials): LoginResult

}