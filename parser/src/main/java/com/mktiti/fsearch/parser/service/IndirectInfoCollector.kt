package com.mktiti.fsearch.parser.service

import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.TypeTemplate

interface IndirectInfoCollector<I> {

    data class IndirectInitialData(
            val directs: Map<MinimalInfo, DirectType>,
            val templates: Map<MinimalInfo, TypeTemplate>
    )

    fun collectInitial(info: I): IndirectInitialData

}