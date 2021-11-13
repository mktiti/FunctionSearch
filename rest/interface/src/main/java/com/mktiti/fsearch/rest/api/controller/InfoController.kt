package com.mktiti.fsearch.rest.api.controller

import com.mktiti.fsearch.dto.ContextInfoQueryParam
import com.mktiti.fsearch.dto.FunId
import com.mktiti.fsearch.dto.ResultList
import com.mktiti.fsearch.dto.TypeInfoDto
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody

interface InfoController {

    @RequestMapping("/types", method = [RequestMethod.POST])
    @ResponseBody
    fun types(@RequestBody info: ContextInfoQueryParam): ResultList<TypeInfoDto>

    @RequestMapping("/functions", method = [RequestMethod.POST])
    @ResponseBody
    fun functions(@RequestBody info: ContextInfoQueryParam): ResultList<FunId>

}