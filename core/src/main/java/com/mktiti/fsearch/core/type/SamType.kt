package com.mktiti.fsearch.core.type

import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.Type.NonGenericType
import sun.security.krb5.internal.APOptions

sealed class SamType<out P>(
        val explicit: Boolean,
        val inputs: List<P>,
        val output: P
) {

    class DirectSam(
            explicit: Boolean,
            inputs: List<TypeHolder.Static>,
            output: TypeHolder.Static
    ) : SamType<TypeHolder.Static>(explicit, inputs, output)

    class GenericSam(
            explicit: Boolean,
            inputs: List<ApplicationParameter>,
            output: ApplicationParameter
    ) : SamType<ApplicationParameter>(explicit, inputs, output) {

        fun dynamicApply(typeArgs: List<ApplicationParameter>): GenericSam? {
            val appliedIns = inputs.map { it.dynamicApply(typeArgs) ?: return null }
            val appliedOut = output.dynamicApply(typeArgs) ?: return null

            return GenericSam(
                    inputs = appliedIns,
                    output = appliedOut,
                    explicit = explicit
            )
        }

        fun staticApply(typeArgs: List<TypeHolder.Static>): DirectSam? {
            fun ApplicationParameter.apply(): TypeHolder.Static? {
                return (this as? Substitution)?.staticApply(typeArgs)
            }

            val appliedIns = inputs.map { it.apply() ?: return null }
            val appliedOut = output.apply() ?: return null

            return DirectSam(
                    inputs = appliedIns,
                    output = appliedOut,
                    explicit = explicit
            )
        }

    }

    // abstract fun dynamicApply(typeArgs: List<ApplicationParameter>): SamType<Substitution>?

    // abstract fun staticApply(typeArgs: List<NonGenericType>): DirectSam?

}