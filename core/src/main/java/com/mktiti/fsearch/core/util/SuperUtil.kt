package com.mktiti.fsearch.core.util

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.Type
import com.mktiti.fsearch.core.type.TypeHolder

object SuperUtil {

    fun resolveSupers(infoRepo: JavaInfoRepo, typeResolver: TypeResolver, type: Type.NonGenericType): Sequence<TypeHolder.Static> = when (val primitive = infoRepo.ifPrimitive(type.info)) {
        null -> type.superTypes
        else -> typeResolver[infoRepo.boxed(primitive)]?.superTypes ?: emptyList()
    }.asSequence()

    fun anyNgSuper(infoRepo: JavaInfoRepo, resolver: TypeResolver, base: TypeHolder.Static, predicate: (Type.NonGenericType) -> Boolean): Boolean {
        val resolved = base.with(resolver) ?: return false
        return if (predicate(resolved)) {
            true
        } else {
            resolveSupers(infoRepo, resolver, resolved).any {
                anyNgSuper(infoRepo, resolver, it, predicate)
            }
        }
    }

}
