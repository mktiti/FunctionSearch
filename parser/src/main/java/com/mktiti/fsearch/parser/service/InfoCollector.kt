package com.mktiti.fsearch.parser.service

import com.mktiti.fsearch.parser.type.DirectCreator
import com.mktiti.fsearch.parser.type.TemplateCreator
import com.mktiti.fsearch.util.MutablePrefixTree

typealias SemiTypeData<T> = MutablePrefixTree<String, T>

interface InfoCollector<I> {

    data class InitialData(
            val directs: SemiTypeData<DirectCreator>,
            val templates: SemiTypeData<TemplateCreator>
    )

    fun collectInitial(info: I): InitialData

}
