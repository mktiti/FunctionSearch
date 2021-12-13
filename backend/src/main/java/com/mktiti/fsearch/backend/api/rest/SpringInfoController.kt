package com.mktiti.fsearch.backend.api.rest

import com.mktiti.fsearch.backend.handler.InfoHandler
import com.mktiti.fsearch.dto.ContextInfoQueryParam
import com.mktiti.fsearch.dto.FunId
import com.mktiti.fsearch.dto.ResultList
import com.mktiti.fsearch.dto.TypeInfoDto
import com.mktiti.fsearch.rest.api.controller.InfoController
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("\${api.base.path}")
@CrossOrigin("\${cross.origin}")
@Tag(name = "info")
class SpringInfoController @Autowired constructor(private val backingHandler: InfoHandler) : InfoController {

    override fun types(info: ContextInfoQueryParam): ResultList<TypeInfoDto> = backingHandler.types(info.context, info.namePart)

    override fun functions(info: ContextInfoQueryParam): ResultList<FunId> = backingHandler.functions(info.context, info.namePart)

}