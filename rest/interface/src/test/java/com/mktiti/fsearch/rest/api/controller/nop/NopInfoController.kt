package com.mktiti.fsearch.rest.api.controller.nop

import com.mktiti.fsearch.dto.ContextInfoQueryParam
import com.mktiti.fsearch.dto.FunId
import com.mktiti.fsearch.dto.ResultList
import com.mktiti.fsearch.dto.TypeInfoDto
import com.mktiti.fsearch.rest.api.controller.InfoController
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("\${api.base.path}")
@CrossOrigin("\${cross.origin}")
@Tag(name = "info")
class NopInfoController : InfoController {

    override fun types(info: ContextInfoQueryParam): ResultList<TypeInfoDto> = nop()

    override fun functions(info: ContextInfoQueryParam): ResultList<FunId> = nop()

}