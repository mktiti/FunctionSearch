package com.mktiti.fsearch.parser.service

import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.TypeTemplate
import com.mktiti.fsearch.util.MutablePrefixTree
import com.mktiti.fsearch.util.PrefixTree

typealias IndirectResults<T> = MutablePrefixTree<String, T>

interface IndirectInfoCollector<I> {

    data class IndirectInitialData(
            val directs: IndirectResults<DirectType>,
            val templates: IndirectResults<TypeTemplate>
    )

    fun collectInitial(info: I): IndirectInitialData

}