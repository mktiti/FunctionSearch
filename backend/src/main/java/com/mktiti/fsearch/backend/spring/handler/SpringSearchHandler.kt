package com.mktiti.fsearch.backend.spring.handler

import com.mktiti.fsearch.backend.api.*
import com.mktiti.fsearch.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("\${api.base.path}")
@CrossOrigin("\${cross.origin}")
@Tag(name = "search")
class SpringSearchHandler @Autowired constructor(private val backingHandler: SearchHandler) {

    @GetMapping("health_check")
    fun healthCheck() = MessageDto("OK")

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
