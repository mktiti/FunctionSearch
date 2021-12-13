package com.mktiti.fsearch.backend.search

import com.mktiti.fsearch.backend.context.ContextId
import com.mktiti.fsearch.backend.context.ContextManager
import com.mktiti.fsearch.backend.context.QueryContext
import com.mktiti.fsearch.backend.search.SearchService.ContextStatus
import com.mktiti.fsearch.backend.search.SearchService.SearchResultEntry
import com.mktiti.fsearch.core.fit.JavaQueryFitter
import com.mktiti.fsearch.core.repo.FallbackResolver
import com.mktiti.fsearch.core.type.SemiType
import com.mktiti.fsearch.core.util.TypeException
import com.mktiti.fsearch.model.build.intermediate.QueryImports
import com.mktiti.fsearch.util.logDebug
import com.mktiti.fsearch.util.logInfo
import com.mktiti.fsearch.util.logger
import org.antlr.v4.runtime.misc.ParseCancellationException
import java.util.concurrent.Executors
import kotlin.streams.asSequence

class BasicSearchService(
        private val contextManager: ContextManager
) : SearchService {

    private val service = Executors.newCachedThreadPool()
    private val log = logger()

    private fun <T> ContextId.resolved(block: QueryContext.() -> T): T = contextManager[this].block()

    override fun typeHint(context: ContextId, namePart: String): Sequence<SemiType> {
        log.logDebug { "Type hint request for '$namePart'" }
        return context.resolved {
            domain.typeResolver.allSemis().filter {
                with(it.info) {
                    simpleName.startsWith(namePart, true) || fullName.startsWith(namePart, true)
                }
            }
        }
    }

    override fun preloadContext(context: ContextId): ContextStatus {
        return if (context in contextManager) {
            log.logDebug { "Context [$context] already preloaded" }
            ContextStatus.LOADED
        } else {
            service.submit {
                contextManager[context]
            }
            log.logInfo { "Context [$context] submitted for preload" }
            ContextStatus.NOT_LOADED
        }
    }

    @Throws(TypeException::class, ParseCancellationException::class)
    override fun search(context: ContextId, imports: QueryImports, query: String): Sequence<SearchResultEntry> {
        log.logInfo { "Searching '$query' [$context]" }
        return context.resolved {
            val (parsedQuery, virtuals) = queryParser.parse(query, imports)

            val resolver = FallbackResolver.withVirtuals(virtuals, domain.typeResolver)
            val fitter = JavaQueryFitter(infoRepo, resolver)

            fitter.findFittings(parsedQuery, domain.staticFunctions, domain.instanceFunctions).asSequence().map { (fn, fit) ->
                SearchResultEntry(fn, fit, docResolver.getOrEmpty(fn.info))
            }
        }
    }


}