package com.mktiti.fsearch.modules

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.FallbackResolver
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.parser.service.FunctionCollector
import java.util.stream.Stream
import kotlin.streams.toList

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

    val staticFunctions: Stream<FunctionObj>

    val instanceFunctions: Stream<FunctionObj>

    val allFunctions: Collection<FunctionObj>
        get() = staticFunctions.toList() + instanceFunctions.toList()

}

data class SimpleDomainRepo(
        override val typeResolver: TypeResolver,
        private val staticStore: Collection<FunctionObj>,
        private val instanceStore: Collection<FunctionObj>
) : DomainRepo {

    constructor(typeResolver: TypeResolver, functions: FunctionCollector.FunctionCollection)
            : this(typeResolver, functions.staticFunctions, functions.instanceMethods)

    override val staticFunctions: Stream<FunctionObj>
        get() = staticStore.stream()

    override val instanceFunctions: Stream<FunctionObj>
        get() = instanceStore.stream()

}

class FallbackDomainRepo(
        private val repo: DomainRepo,
        private val fallbackRepo: DomainRepo
) : DomainRepo {

    override val typeResolver: TypeResolver = FallbackResolver(repo.typeResolver, fallbackRepo.typeResolver)

    override val staticFunctions: Stream<FunctionObj>
        get() = Stream.concat(repo.staticFunctions, fallbackRepo.staticFunctions)

    override val instanceFunctions: Stream<FunctionObj>
        get() = Stream.concat(repo.instanceFunctions, fallbackRepo.instanceFunctions)

}

class FunFallbackDomainRepo(
        override val typeResolver: TypeResolver,
        private val fallback: DomainRepo
) : DomainRepo {

    override val staticFunctions: Stream<FunctionObj>
        get() = fallback.staticFunctions

    override val instanceFunctions: Stream<FunctionObj>
        get() = fallback.instanceFunctions

}

