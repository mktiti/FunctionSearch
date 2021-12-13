package com.mktiti.fsearch.backend.search

import com.mktiti.fsearch.backend.context.ContextId
import com.mktiti.fsearch.core.fit.FittingMap
import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.javadoc.FunctionDoc
import com.mktiti.fsearch.core.type.SemiType
import com.mktiti.fsearch.core.util.TypeException
import com.mktiti.fsearch.model.build.intermediate.QueryImports
import org.antlr.v4.runtime.misc.ParseCancellationException

interface SearchService {

    enum class ContextStatus {
        LOADED, NOT_LOADED
    }

    data class SearchResultEntry(
            val function: FunctionObj,
            val fitting: FittingMap,
            val docs: FunctionDoc
    )

    fun typeHint(context: ContextId, namePart: String): Sequence<SemiType>

    fun preloadContext(context: ContextId): ContextStatus

    @Throws(TypeException::class, ParseCancellationException::class)
    fun search(context: ContextId, imports: QueryImports, query: String): Sequence<SearchResultEntry>

}