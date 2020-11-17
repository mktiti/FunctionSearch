package com.mktiti.fsearch.backend

import com.mktiti.fsearch.core.javadoc.DocStore
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.util.show.JavaTypeStringResolver
import com.mktiti.fsearch.core.util.show.TypeStringResolver
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.modules.DomainRepo
import com.mktiti.fsearch.parser.query.AntlrQueryParser
import com.mktiti.fsearch.parser.query.QueryParser

class ContextId(
        val artifacts: Set<ArtifactId>
) {

    val hash by lazy {
        // Order independent XOR hash of elements
        artifacts.map(ArtifactId::hashCode).fold(0) { acc, elem ->
            acc xor elem
        }
    }

    override fun hashCode() = hash

    override fun equals(other: Any?) = (other is ContextId) && artifacts == other.artifacts

}

interface QueryContext {

    val id: ContextId
    val infoRepo: JavaInfoRepo
    val javaRepo: JavaRepo
    val domain: DomainRepo
    val docStore: DocStore
    val queryParser: QueryParser
    val stringResolver: TypeStringResolver

}

class SimpleQueryContext(
        override val id: ContextId,
        override val infoRepo: JavaInfoRepo,
        override val javaRepo: JavaRepo,
        override val domain: DomainRepo,
        override val docStore: DocStore,
        override val queryParser: QueryParser = AntlrQueryParser(javaRepo, infoRepo, domain.typeResolver),
        override val stringResolver: TypeStringResolver = JavaTypeStringResolver(infoRepo, docStore)
) : QueryContext
