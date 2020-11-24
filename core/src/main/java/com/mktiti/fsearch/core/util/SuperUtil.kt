package com.mktiti.fsearch.core.util

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.Type
import com.mktiti.fsearch.core.type.TypeHolder

object SuperUtil {

    fun resolveSupers(infoRepo: JavaInfoRepo, typeResolver: TypeResolver, type: Type.NonGenericType): Sequence<TypeHolder.Static> = when (val primitive = infoRepo.ifPrimitive(type.info)) {
        null -> type.superTypes
        else -> typeResolver[infoRepo.boxed(primitive)]?.superTypes ?: emptyList()
    }.asSequence()

    fun resolveSuperInfosDeep(infoRepo: JavaInfoRepo, typeResolver: TypeResolver, info: MinimalInfo): Set<MinimalInfo> {
        fun resolveSupersInner(info: MinimalInfo): List<MinimalInfo> {
            return (typeResolver.semi(info)?.superTypes?.flatMap { superType ->
                resolveSupersInner(superType.info.base)
            } ?: emptyList()) + info
        }

        val bases = infoRepo.ifPrimitive(info)?.let { listOf(infoRepo.boxed(it), info) } ?: listOf(info)
        return bases.flatMap { resolveSupersInner(it) }.toSet()
    }

}
