package com.mktiti.fsearch.core.type

import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution

sealed class SamType<out S : Substitution>(
        val explicit: Boolean,
        val inputs: List<Substitution>,
        val output: S
) {

    class DirectSam(
            explicit: Boolean,
            inputs: List<StaticTypeSubstitution>,
            output: StaticTypeSubstitution
    ) : SamType<StaticTypeSubstitution>(explicit, inputs, output) {

      //  override fun staticApply(typeArgs: List<NonGenericType>) = this
        override fun dynamicApply(typeArgs: List<ApplicationParameter>) = this

    }

    class GenericSam(
            explicit: Boolean,
            inputs: List<Substitution>,
            output: Substitution
    ) : SamType<Substitution>(explicit, inputs, output) {

        override fun dynamicApply(typeArgs: List<ApplicationParameter>): SamType<Substitution>? {
            fun Substitution.apply(): Substitution? = dynamicApply(typeArgs) as? Substitution

            val appliedIns = inputs.map { it.apply() ?: return null }
            val appliedOut = output.apply() ?: return null

            return GenericSam(
                    inputs = appliedIns,
                    output = appliedOut,
                    explicit = explicit
            )
        }
/*
        override fun staticApply(typeArgs: List<NonGenericType>): DirectSam? {
            fun Substitution.apply() = staticApply(typeArgs)?.let { StaticTypeSubstitution(it) }

            val appliedIns = inputs.map { it.apply() ?: return null }
            val appliedOut = output.apply() ?: return null

            return DirectSam(
                    inputs = appliedIns,
                    output = appliedOut,
                    explicit = explicit
            )
        }


 */
    }

    abstract fun dynamicApply(typeArgs: List<ApplicationParameter>): SamType<Substitution>?

    // abstract fun staticApply(typeArgs: List<NonGenericType>): DirectSam?

}