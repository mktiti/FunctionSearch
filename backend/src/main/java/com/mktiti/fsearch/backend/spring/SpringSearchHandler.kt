package com.mktiti.fsearch.backend.spring

import com.mktiti.fsearch.backend.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
class SpringSearchHandler @Autowired constructor(private val backingHandler: SearchHandler) {

    @GetMapping("health_check")
    fun healthCheck() = "OK"

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