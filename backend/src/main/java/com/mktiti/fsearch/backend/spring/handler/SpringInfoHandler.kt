package com.mktiti.fsearch.backend.spring.handler

import com.mktiti.fsearch.backend.api.InfoHandler
import com.mktiti.fsearch.dto.*
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("\${api.base.path}")
@CrossOrigin("\${cross.origin}")
@Tag(name = "info")
class SpringInfoHandler @Autowired constructor(private val backingHandler: InfoHandler) {

    @RequestMapping("/types", method = [RequestMethod.POST])
    @ResponseBody
    fun types(@RequestBody info: ContextInfoQueryParam): ResultList<TypeInfoDto>
            = backingHandler.types(info.context, info.namePart)

    @RequestMapping("/functions", method = [RequestMethod.POST])
    @ResponseBody
    fun functions(@RequestBody info: ContextInfoQueryParam): ResultList<FunId>
            = backingHandler.functions(info.context, info.namePart)

}