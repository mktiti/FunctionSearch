package com.mktiti.fsearch.core.type

import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution
import com.mktiti.fsearch.core.util.castIfAllInstance

interface StaticApplicable {
    fun staticApply(typeArgs: List<TypeHolder.Static>): TypeHolder.Static?
}

interface WeakSelfApplicable {
    fun applySelf(self: TypeHolder.Static): TypeHolder<*, *>
}

interface WeakDynamicApplicable : WeakSelfApplicable {
    fun dynamicApply(typeArgs: List<ApplicationParameter>): TypeHolder<*, *>?
}

interface SelfApplicable : WeakSelfApplicable{
    override fun applySelf(self: TypeHolder.Static): TypeHolder.Dynamic
}

interface DynamicApplicable : WeakDynamicApplicable, SelfApplicable {
    override fun dynamicApply(typeArgs: List<ApplicationParameter>): TypeHolder.Dynamic?
}

interface TypeApplicable {

    val staticApplicable: Boolean

    fun staticApply(typeArgs: List<TypeHolder.Static>): Type.NonGenericType.StaticAppliedType?

    fun dynamicApply(typeArgs: List<ApplicationParameter>): Type.DynamicAppliedType?

    fun apply(typeArgs: List<ApplicationParameter>): Type<*>? {
        val argsAsStatic = typeArgs.map {
            when (it) {
                is TypeSubstitution<*, *> -> it.holder
                is ApplicationParameter.Wildcard.Direct -> {
                    // StaticTypeSubstitution(NonGenericType.DirectType(TypeInfo.anyWildcard, emptyList(), null))
                    null
                }
                else -> null
            }
        }.castIfAllInstance<TypeHolder.Static>()

        return if (argsAsStatic == null) {
            dynamicApply(typeArgs)
        } else {
            if (staticApplicable) {
                staticApply(argsAsStatic)
            } else {
                dynamicApply(typeArgs)
            }
        }
    }

}