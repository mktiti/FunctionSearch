package com.mktiti.fsearch.backend.info

import com.mktiti.fsearch.backend.context.ContextId
import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.type.SemiType
import com.mktiti.fsearch.modules.ArtifactId

interface InfoService {

    fun types(context: ContextId, namePartOpt: String?): Sequence<SemiType>

    fun functions(context: ContextId, namePartOpt: String?): Sequence<FunctionObj>

    fun all(): Sequence<ArtifactId>

    fun create(id: ArtifactId)

    fun byGroup(group: List<String>): Sequence<ArtifactId>

    fun byName(group: List<String>, name: String): Sequence<ArtifactId>

    operator fun contains(id: ArtifactId): Boolean

    fun remove(id: ArtifactId): Boolean

}