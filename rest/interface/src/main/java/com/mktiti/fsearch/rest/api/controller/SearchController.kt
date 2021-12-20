package com.mktiti.fsearch.rest.api.controller

import com.mktiti.fsearch.dto.*
import com.mktiti.fsearch.rest.api.handler.SearchHandler
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("\${api.base.path}")
@CrossOrigin("\${cross.origin}")
@Tag(name = "search")
class SearchController(private val handler: SearchHandler)  {

    @GetMapping("health_check")
    fun healthCheck() : HealthInfo = handler.healthCheck()

    @PostMapping("/hint")
    @ResponseBody
    fun typeHint(@RequestBody req: HintRequestDto): ResultList<TypeHint> = handler.typeHint(req.context, req.name)

    @PostMapping("/search")
    @ResponseBody
    fun syncQuery(@RequestBody req: QueryRequestDto): QueryResult = handler.syncQuery(req.context, req.query)

    @PostMapping("/preload")
    @ResponseBody
    fun preloadContext(@RequestBody ctxId: QueryCtxDto): ContextLoadStatus = handler.preloadContext(ctxId)

}
