package com.mktiti.fsearch.rest.api.controller

import com.mktiti.fsearch.dto.ContextInfoQueryParam
import com.mktiti.fsearch.dto.FunId
import com.mktiti.fsearch.dto.ResultList
import com.mktiti.fsearch.dto.TypeInfoDto
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody

interface InfoController {

    @PostMapping("/types")
    @ResponseBody
    fun types(@RequestBody info: ContextInfoQueryParam): ResultList<TypeInfoDto>

    @PostMapping("/functions")
    @ResponseBody
    fun functions(@RequestBody info: ContextInfoQueryParam): ResultList<FunId>

}