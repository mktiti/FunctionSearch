package com.mktiti.fsearch.core.repo

import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.CompleteMinInfo.Static.Companion.holders
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType

interface TypeResolver {

    operator fun get(info: MinimalInfo): DirectType?

    operator fun get(name: String, allowSimple: Boolean = false): DirectType?

    fun template(info: MinimalInfo): TypeTemplate?

    fun template(name: String, allowSimple: Boolean = false): TypeTemplate?

    operator fun get(info: CompleteMinInfo.Static): NonGenericType? {
        return if (info.args.isEmpty()) {
            get(info.base)
        } else {
            template(info.base)?.staticApply(info.args.holders())
        }
    }

    operator fun get(info: CompleteMinInfo.Dynamic): Type.DynamicAppliedType? {
        val template = template(info.base) ?: return null
        return template.dynamicApply(info.args)
    }

    fun getAny(info: CompleteMinInfo<*>): Type<*>? = when (info) {
        is CompleteMinInfo.Static -> get(info)
        is CompleteMinInfo.Dynamic -> get(info)
    }

    fun allSemis(): Sequence<SemiType>

}

class SingleRepoTypeResolver(
        private val repo: TypeRepo
) : TypeResolver {

    override fun get(info: MinimalInfo) = repo[info]

    override fun template(info: MinimalInfo) = repo.template(info)

    override fun get(name: String, allowSimple: Boolean): DirectType? = repo[name, allowSimple]

    override fun template(name: String, allowSimple: Boolean): TypeTemplate? = repo.template(name, allowSimple)

    override fun allSemis(): Sequence<SemiType> {
        return repo.allTypes.asSequence() + repo.allTemplates.asSequence()
    }

}

class FallbackResolver(
        private val primary: TypeResolver,
        private val fallback: TypeResolver
) : TypeResolver {

    companion object {
        fun withVirtuals(virtualTypes: Map<MinimalInfo, DirectType>, resolver: TypeResolver): TypeResolver {
            return if (virtualTypes.isEmpty()) {
                resolver
            } else {
                FallbackResolver(VirtualTypeRepo(virtualTypes), resolver)
            }
        }

        fun withVirtuals(virtualTypes: Collection<DirectType>, resolver: TypeResolver): TypeResolver {
            return withVirtuals(virtualTypes.map { it.info to it }.toMap(), resolver)
        }
    }

    constructor(primary: TypeRepo, fallback: TypeResolver) : this(SingleRepoTypeResolver(primary), fallback)

    private fun <T : Any> resolve(query: TypeResolver.() -> T?): T? = primary.query() ?: fallback.query()

    override fun get(info: MinimalInfo): DirectType? = resolve { get(info) }

    override fun get(name: String, allowSimple: Boolean): DirectType? = resolve { get(name, allowSimple) }

    override fun template(info: MinimalInfo): TypeTemplate? = resolve { template(info) }

    override fun template(name: String, allowSimple: Boolean): TypeTemplate? = resolve { template(name, allowSimple) }

    override fun allSemis(): Sequence<SemiType> = primary.allSemis() + fallback.allSemis()

}

class SimpleCombiningTypeResolver(
        private val resolvers: Collection<TypeResolver>
) : TypeResolver {

    private fun <T : Any> first(query: (TypeResolver) -> T?): T? = resolvers.asSequence().mapNotNull(query).firstOrNull()

    override fun get(info: MinimalInfo) = first { it[info] }

    override fun template(info: MinimalInfo) = first { it.template(info) }

    override fun get(name: String, allowSimple: Boolean) = first { it[name, allowSimple] }

    override fun template(name: String, allowSimple: Boolean) = first { it.template(name, allowSimple) }

    override fun allSemis(): Sequence<SemiType> {
        return resolvers.asSequence().flatMap { it.allSemis() }
    }

}