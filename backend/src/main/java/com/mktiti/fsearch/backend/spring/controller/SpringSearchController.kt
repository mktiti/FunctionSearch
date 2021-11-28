package com.mktiti.fsearch.backend.spring.controller

import com.mktiti.fsearch.backend.ProjectInfo
import com.mktiti.fsearch.backend.api.SearchHandler
import com.mktiti.fsearch.dto.*
import com.mktiti.fsearch.rest.api.controller.SearchController
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("\${api.base.path}")
@CrossOrigin("\${cross.origin}")
@Tag(name = "search")
class SpringSearchController @Autowired constructor(private val backingHandler: SearchHandler) : SearchController {

    companion object {
        private val okHealth = HealthInfo(
                version = ProjectInfo.version,
                buildTimestamp = ProjectInfo.builtAt,
                ok = true
        )
    }

    override fun healthCheck() = okHealth

    override fun typeHint(req: HintRequestDto) = backingHandler.typeHint(req.context, req.name)

    override fun syncQuery(req: QueryRequestDto) = backingHandler.syncQuery(req.context, req.query)

    override fun preloadContext(ctxId: QueryCtxDto): ContextLoadStatus = backingHandler.preloadContext(ctxId)

}
