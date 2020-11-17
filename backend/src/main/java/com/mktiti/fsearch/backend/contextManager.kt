package com.mktiti.fsearch.backend

import com.mktiti.fsearch.core.repo.FallbackResolver
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.util.show.JavaTypeStringResolver
import com.mktiti.fsearch.modules.*

interface ContextManager {

    fun context(artifacts: Set<ArtifactId>): QueryContext = context(ContextId(artifacts))

    fun context(contextId: ContextId): QueryContext

    operator fun get(contextId: ContextId): QueryContext = context(contextId)

}

class SimpleMapContextManager(
        private val infoRepo: JavaInfoRepo,
        private val javaRepo: JavaRepo,
        private val jclDomain: DomainRepo,
        private val artifactManager: ArtifactManager,
        private val docManager: DocManager
) : ContextManager {

    private val store = mutableMapOf<ContextId, QueryContext>()

    override fun context(contextId: ContextId) = store.getOrPut(contextId) {
        val onlyCtxDomain = artifactManager.getWithDependencies(contextId.artifacts)
        val extendedDomain = SimpleDomainRepo(
                typeResolver = FallbackResolver(onlyCtxDomain.typeResolver, jclDomain.typeResolver),
                functions = onlyCtxDomain.functions + jclDomain.functions
        )

        val docStore = docManager.forArtifacts(contextId.artifacts)
        // TODO load jcl docs

        SimpleQueryContext(
                id = contextId,
                infoRepo = infoRepo,
                javaRepo = javaRepo,
                domain = extendedDomain,
                docStore = docStore
        )
    }

}
