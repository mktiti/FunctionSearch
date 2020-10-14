package com.mktiti.fsearch.core.repo

import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.CompleteMinInfo.Static.Companion.holders
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.Type
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.TypeTemplate

interface TypeResolver {

    operator fun get(info: MinimalInfo): DirectType?

    fun template(info: MinimalInfo): TypeTemplate?

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

}

class SingleRepoTypeResolver(
        private val repo: TypeRepo
) : TypeResolver {

    override fun get(info: MinimalInfo) = repo[info]

    override fun template(info: MinimalInfo) = repo.template(info)

}

class SimpleMultiRepoTypeResolver(
        private val repos: Collection<TypeRepo>
) : TypeResolver {

    private fun <T : Any> first(query: (TypeRepo) -> T?): T? = repos.asSequence().mapNotNull(query).firstOrNull()

    override fun get(info: MinimalInfo) = first { it[info] }

    override fun template(info: MinimalInfo) = first { it.template(info) }

}
