package com.mktiti.fsearch.backend.spring

import com.mktiti.fsearch.backend.api.HintRequestDto
import com.mktiti.fsearch.backend.api.QueryRequestDto
import com.mktiti.fsearch.backend.api.SearchHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
class SpringSearchHandler @Autowired constructor(private val backingHandler: SearchHandler) {

    @PostMapping("/hint")
    @ResponseBody
    fun typeHint(@RequestBody req: HintRequestDto) = backingHandler.typeHint(req.context, req.name)

    @PostMapping("/search")
    @ResponseBody
    fun syncQuery(@RequestBody req: QueryRequestDto) = backingHandler.syncQuery(req.context, req.query)

}