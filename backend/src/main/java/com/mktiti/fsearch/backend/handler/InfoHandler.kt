package com.mktiti.fsearch.backend.handler

import com.mktiti.fsearch.dto.FunId
import com.mktiti.fsearch.dto.QueryCtxDto
import com.mktiti.fsearch.dto.ResultList
import com.mktiti.fsearch.dto.TypeInfoDto

interface InfoHandler {

    object Nop : InfoHandler {
        override fun types(context: QueryCtxDto, namePartOpt: String?): ResultList<TypeInfoDto> = ResultList.empty()

        override fun functions(context: QueryCtxDto, namePartOpt: String?): ResultList<FunId> = ResultList.empty()
    }

    fun types(context: QueryCtxDto, namePartOpt: String?): ResultList<TypeInfoDto>

    fun functions(context: QueryCtxDto, namePartOpt: String?): ResultList<FunId>

}
