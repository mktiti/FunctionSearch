package com.mktiti.fsearch.rest.api.controller

import com.mktiti.fsearch.dto.Credentials
import com.mktiti.fsearch.dto.LoginResult
import com.mktiti.fsearch.rest.api.handler.AuthHandler
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("\${api.base.path}")
@CrossOrigin("\${cross.origin}")
@Tag(name = "auth")
class AuthController(private val handler: AuthHandler) {

    @PostMapping("/login")
    @ResponseBody
    fun login(@RequestBody credentials: Credentials): LoginResult = handler.login(credentials)

}