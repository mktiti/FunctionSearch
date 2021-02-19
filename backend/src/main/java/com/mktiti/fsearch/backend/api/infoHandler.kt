package com.mktiti.fsearch.backend.api

import com.mktiti.fsearch.dto.FunId
import com.mktiti.fsearch.dto.QueryCtxDto
import com.mktiti.fsearch.dto.TypeInfoDto

interface InfoHandler {

    object Nop : InfoHandler {
        override fun types(context: QueryCtxDto, namePartOpt: String?): Collection<TypeInfoDto> = emptyList()

        override fun functions(context: QueryCtxDto, namePartOpt: String?): Collection<FunId> = emptyList()
    }

    fun types(context: QueryCtxDto, namePartOpt: String?): Collection<TypeInfoDto>

    fun functions(context: QueryCtxDto, namePartOpt: String?): Collection<FunId>

}
