package com.mktiti.fsearch.rest.api.controller

import com.mktiti.fsearch.dto.UserInfo
import com.mktiti.fsearch.rest.api.UserOnly
import com.mktiti.fsearch.rest.api.handler.UserHandler
import com.mktiti.fsearch.rest.api.loggedUser
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import javax.servlet.ServletRequest

@RestController
@RequestMapping("\${api.base.path}")
@CrossOrigin("\${cross.origin}")
@Tag(name = "user")
class UserController(private val handler: UserHandler) {

    @GetMapping("/users/self")
    @ResponseBody
    @UserOnly
    fun selfData(request: ServletRequest): UserInfo = handler.selfData(request.loggedUser().username)

}