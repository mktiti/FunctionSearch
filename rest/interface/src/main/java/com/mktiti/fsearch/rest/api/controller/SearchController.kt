package com.mktiti.fsearch.rest.api.controller

import com.mktiti.fsearch.dto.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody

interface SearchController  {

    @GetMapping("health_check")
    fun healthCheck() : MessageDto

    @PostMapping("/hint")
    @ResponseBody
    fun typeHint(@RequestBody req: HintRequestDto): ResultList<TypeHint>

    @PostMapping("/search")
    @ResponseBody
    fun syncQuery(@RequestBody req: QueryRequestDto): QueryResult

    @PostMapping("/preload")
    @ResponseBody
    fun preloadContext(@RequestBody ctxId: QueryCtxDto): ContextLoadStatus

}
