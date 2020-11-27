package com.mktiti.fsearch.backend.spring

import com.mktiti.fsearch.backend.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SpringSearchHandler @Autowired constructor(private val backingHandler: SearchHandler) {

    @PostMapping("/hint")
    @ResponseBody
    fun typeHint(@RequestBody req: HintRequestDto) = backingHandler.typeHint(req.context, req.name)

    @PostMapping("/search")
    @ResponseBody
    fun syncQuery(@RequestBody req: QueryRequestDto) = backingHandler.syncQuery(req.context, req.query)

    @PostMapping("/preload")
    @ResponseBody
    fun preloadContext(@RequestBody ctxId: QueryCtxDto): ContextLoadStatus = backingHandler.preloadContext(ctxId)

}