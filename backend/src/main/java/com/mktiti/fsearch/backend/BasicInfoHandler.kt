package com.mktiti.fsearch.backend

import com.mktiti.fsearch.backend.api.InfoHandler
import com.mktiti.fsearch.backend.api.asDto
import com.mktiti.fsearch.backend.api.artifactsId
import com.mktiti.fsearch.dto.FunId
import com.mktiti.fsearch.dto.QueryCtxDto
import com.mktiti.fsearch.dto.TypeInfoDto
import kotlin.streams.asSequence

class BasicInfoHandler(
        private val contextManager: ContextManager
) : InfoHandler {

    private fun context(context: QueryCtxDto): QueryContext = contextManager[context.artifactsId()]

    private fun <T> Sequence<T>.limited() = take(50).toList()

    private fun <T, F> Sequence<T>.optionalFilter(value: F?, filter: (T, F) -> Boolean): Sequence<T> {
        return if (value == null) {
            this
        } else {
            filter { filter(it, value) }
        }
    }

    override fun types(context: QueryCtxDto, namePartOpt: String?): Collection<TypeInfoDto> {
        return context(context).domain.typeResolver
                .allSemis()
                .optionalFilter(namePartOpt) { semi, namePart -> namePart in semi.fullName }
                .map { it.asDto() }
                .limited()
    }

    override fun functions(context: QueryCtxDto, namePartOpt: String?): Collection<FunId> {
        return with(context(context)) {
            domain.allFunctions.asSequence()
                    .optionalFilter(namePartOpt) { semi, namePart -> namePart in semi.info.name }
                    .map { it.asDto() }
                    .limited()
        }
    }

}