package com.mktiti.fsearch.modules

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.TypeResolver

data class ArtifactId(
        val group: List<String>,
        val name: String,
        val version: String
) {

    companion object {
        fun parse(simple: String): ArtifactId? {
            val parts = simple.split(':')
            if (parts.size != 3) {
                return null
            }

            val (groupName, name, version) = parts
            return ArtifactId(
                    group = groupName.split('.'),
                    name = name,
                    version = version
            )
        }
    }

    override fun toString() = listOf(group.joinToString(separator = "."), name, version).joinToString(separator = ":")

}

interface DomainRepo {

    val typeResolver: TypeResolver

    // TODO
    val functions: Collection<FunctionObj>

}

data class SimpleDomainRepo(
        override val typeResolver: TypeResolver,
        override val functions: Collection<FunctionObj>
) : DomainRepo
