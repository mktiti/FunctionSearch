package com.mktiti.fsearch.core.util

import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.Type
import com.mktiti.fsearch.core.type.TypeHolder

object SuperUtil {

    fun anyNgSuper(resolver: TypeResolver, base: TypeHolder.Static, predicate: (Type.NonGenericType) -> Boolean): Boolean {
        val resolved = base.with(resolver) ?: return false
        return if (predicate(resolved)) {
            true
        } else {
            resolved.superTypes.asSequence().any { anyNgSuper(resolver, it, predicate) }
        }
    }

}
