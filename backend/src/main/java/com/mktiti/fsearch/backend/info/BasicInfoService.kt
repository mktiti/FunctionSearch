package com.mktiti.fsearch.backend.info

import com.mktiti.fsearch.backend.context.ContextId
import com.mktiti.fsearch.backend.context.ContextManager
import com.mktiti.fsearch.backend.context.QueryContext
import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.type.SemiType
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.modules.ArtifactManager
import com.mktiti.fsearch.util.logError
import com.mktiti.fsearch.util.logger
import kotlin.streams.asSequence

class BasicInfoService(
        private val contextManager: ContextManager,
        private val artifactManager: ArtifactManager
) : InfoService {

    private val log = logger()

    private fun <T> ContextId.resolved(block: QueryContext.() -> T): T = contextManager[this].block()

    private fun <T, F> Sequence<T>.optionalFilter(value: F?, filter: (T, F) -> Boolean): Sequence<T> {
        return if (value == null) {
            this
        } else {
            filter { filter(it, value) }
        }
    }

    override fun types(context: ContextId, namePartOpt: String?): Sequence<SemiType> = context.resolved {
        domain.typeResolver
                .allSemis()
                .optionalFilter(namePartOpt) { semi, namePart -> namePart in semi.fullName }
    }

    override fun functions(context: ContextId, namePartOpt: String?): Sequence<FunctionObj> = context.resolved {
        domain.allFunctions.asSequence()
                .optionalFilter(namePartOpt) { semi, namePart -> namePart in semi.info.name }
    }

    override fun all(): Sequence<ArtifactId> = artifactManager.allStored().asSequence()

    override fun create(id: ArtifactId) {
        log.logError { "TODO - Artifact create not yet implemented" }
    }

    override fun byGroup(group: List<String>): Sequence<ArtifactId> {
        return all().filter { it.group == group }
    }

    override fun byName(group: List<String>, name: String): Sequence<ArtifactId> = byGroup(group).filter {
        it.name == name
    }

    override operator fun contains(id: ArtifactId): Boolean = byName(id.group, id.name).any {
        it.version == id.version
    }

    override fun remove(id: ArtifactId): Boolean {
        log.info("Artifact ($id) removed")
        return artifactManager.remove(id)
    }

}