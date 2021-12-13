package com.mktiti.fsearch.backend.context

import com.mktiti.fsearch.modules.ArtifactId

interface ContextManager {

    fun context(artifacts: Set<ArtifactId>): QueryContext = context(ContextId(artifacts))

    fun context(contextId: ContextId): QueryContext

    operator fun get(contextId: ContextId): QueryContext = context(contextId)

    operator fun contains(contextId: ContextId): Boolean

}