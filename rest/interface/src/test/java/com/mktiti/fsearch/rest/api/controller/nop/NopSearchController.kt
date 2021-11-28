package com.mktiti.fsearch.rest.api.controller.nop

import com.mktiti.fsearch.dto.*
import com.mktiti.fsearch.rest.api.controller.SearchController
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("\${api.base.path}")
@CrossOrigin("\${cross.origin}")
@Tag(name = "search")
class NopSearchController : SearchController {

    override fun healthCheck(): HealthInfo = nop()

    override fun typeHint(req: HintRequestDto): ResultList<TypeHint> = nop()

    override fun syncQuery(req: QueryRequestDto): QueryResult = nop()

    override fun preloadContext(ctxId: QueryCtxDto): ContextLoadStatus = nop()

}
