package com.mktiti.fsearch.backend.handler.basic

import com.mktiti.fsearch.backend.handler.InfoHandler
import com.mktiti.fsearch.backend.info.InfoService
import com.mktiti.fsearch.dto.FunId
import com.mktiti.fsearch.dto.QueryCtxDto
import com.mktiti.fsearch.dto.ResultList
import com.mktiti.fsearch.dto.TypeInfoDto

class BasicInfoHandler(
        private val infoService: InfoService,
        private val resultLimit: Int = 50
) : InfoHandler {

    override fun types(context: QueryCtxDto, namePartOpt: String?): ResultList<TypeInfoDto> {
        return infoService.types(context.artifactsId(), namePartOpt)
                .map { it.asDto() }
                .limitedResult(resultLimit)
    }

    override fun functions(context: QueryCtxDto, namePartOpt: String?): ResultList<FunId> {
        return infoService.functions(context.artifactsId(), namePartOpt)
                .map { it.asDto() }
                .limitedResult(resultLimit)
    }
}