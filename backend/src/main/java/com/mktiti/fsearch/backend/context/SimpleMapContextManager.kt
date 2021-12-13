package com.mktiti.fsearch.backend.context

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.modules.ArtifactManager
import com.mktiti.fsearch.modules.DomainRepo
import com.mktiti.fsearch.modules.FallbackDomainRepo
import com.mktiti.fsearch.modules.docs.DocManager

class SimpleMapContextManager(
        private val infoRepo: JavaInfoRepo,
        private val javaRepo: JavaRepo,
        private val jclDomain: DomainRepo,
        private val artifactManager: ArtifactManager,
        private val docManager: DocManager
) : ContextManager {

    private val store = mutableMapOf<ContextId, QueryContext>()

    override fun contains(contextId: ContextId) = store.containsKey(contextId)

    override fun context(contextId: ContextId) = store.getOrPut(contextId) {
        val onlyCtxDomain = artifactManager.getWithDependencies(contextId.artifacts)
        val extendedDomain = FallbackDomainRepo(
                repo = jclDomain,
                fallbackRepo = onlyCtxDomain
        )

        val docStore = docManager.forArtifacts(contextId.artifacts)
        SimpleQueryContext(
                id = contextId,
                infoRepo = infoRepo,
                javaRepo = javaRepo,
                domain = extendedDomain,
                docResolver = docStore
        )
    }

}
