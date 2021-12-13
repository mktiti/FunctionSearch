package com.mktiti.fsearch.backend.api.rest

import com.mktiti.fsearch.backend.handler.AuthHandler
import com.mktiti.fsearch.dto.Credentials
import com.mktiti.fsearch.dto.LoginResult
import com.mktiti.fsearch.rest.api.controller.AuthController
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("\${api.base.path}")
@CrossOrigin("\${cross.origin}")
@Tag(name = "auth")
class SpringAuthController @Autowired constructor(private val backingHandler: AuthHandler) : AuthController {

    override fun login(credentials: Credentials): LoginResult = backingHandler.login(credentials)

}