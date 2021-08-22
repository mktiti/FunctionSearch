package com.mktiti.fsearch.modules

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.FallbackResolver
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.util.InfoMap
import com.mktiti.fsearch.parser.intermediate.FunctionCollector
import java.util.stream.Stream

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

    val instanceFunctions: InfoMap<Collection<FunctionObj>>

    val allFunctions: Stream<FunctionObj>
        get() = Stream.concat(staticFunctions, instanceFunctions.all().flatMap { it.stream() })

}

data class SimpleDomainRepo(
        override val typeResolver: TypeResolver,
        private val staticStore: Collection<FunctionObj>,
        override val instanceFunctions: InfoMap<Collection<FunctionObj>>
) : DomainRepo {

    constructor(typeResolver: TypeResolver, functions: FunctionCollector.FunctionCollection)
            : this(typeResolver, functions.staticFunctions, functions.instanceMethods)

    override val staticFunctions: Stream<FunctionObj>
        get() = staticStore.stream()

}

private class MapCombiner<V>(
        private val primary: InfoMap<V>,
        private val fallback: InfoMap<V>
) : InfoMap<V> {

    override fun get(info: MinimalInfo) = primary[info] ?: fallback[info]

    override fun all(): Stream<V> = Stream.concat(primary.all(), fallback.all())

}

class FallbackDomainRepo(
        private val repo: DomainRepo,
        private val fallbackRepo: DomainRepo
) : DomainRepo {

    override val typeResolver: TypeResolver = FallbackResolver(repo.typeResolver, fallbackRepo.typeResolver)

    override val staticFunctions: Stream<FunctionObj>
        get() = Stream.concat(repo.staticFunctions, fallbackRepo.staticFunctions)

    override val instanceFunctions: InfoMap<Collection<FunctionObj>>
            = MapCombiner(repo.instanceFunctions, fallbackRepo.instanceFunctions)

}

class FunFallbackDomainRepo(
        override val typeResolver: TypeResolver,
        private val fallback: DomainRepo
) : DomainRepo {

    override val staticFunctions: Stream<FunctionObj>
        get() = fallback.staticFunctions

    override val allFunctions: Stream<FunctionObj>
        get() = fallback.allFunctions

    override val instanceFunctions
        get() = fallback.instanceFunctions

}

