package com.mktiti.fsearch.rest.api.controller

import com.mktiti.fsearch.dto.ContextInfoQueryParam
import com.mktiti.fsearch.dto.FunId
import com.mktiti.fsearch.dto.ResultList
import com.mktiti.fsearch.dto.TypeInfoDto
import com.mktiti.fsearch.rest.api.handler.InfoHandler
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("\${api.base.path}")
@CrossOrigin("\${cross.origin}")
@Tag(name = "info")
class InfoController(private val handler: InfoHandler) {

    @PostMapping("/types")
    @ResponseBody
    fun types(@RequestBody info: ContextInfoQueryParam): ResultList<TypeInfoDto> = handler.types(info.context, info.namePart)

    @PostMapping("/functions")
    @ResponseBody
    fun functions(@RequestBody info: ContextInfoQueryParam): ResultList<FunId> = handler.functions(info.context, info.namePart)

}