package com.mktiti.fsearch.core.type

import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard
import com.mktiti.fsearch.core.util.genericString
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.core.util.zipIfSameLength
import com.mktiti.fsearch.util.PrefixTree
import java.util.*

data class MinimalInfo(
        val packageName: PackageName,
        val simpleName: String
) {

    companion object {
        fun fromFull(info: TypeInfo) = MinimalInfo(
                packageName = info.packageName,
                simpleName = info.name
        )
    }

    val nameParts: List<String>
        get() = simpleName.split('.')

    fun full(artifact: String) = TypeInfo(
            packageName = packageName,
            name = simpleName,
            artifact = artifact
    )

    fun complete() = CompleteMinInfo.Static(this, emptyList())

    fun staticComplete(args: List<CompleteMinInfo.Static>) = CompleteMinInfo.Static(this, args)

    fun dynamicComplete(args: List<ApplicationParameter>) = CompleteMinInfo.Dynamic(this, args)

    override fun toString() = (packageName + simpleName).joinToString(separator = ".")

}

sealed class CompleteMinInfo<out P : Any>(
        val base: MinimalInfo,
        val args: List<P>
) {

    class Static(
            base : MinimalInfo,
            args: List<Static>
    ) : CompleteMinInfo<Static>(base, args) {

        override fun staticApply(typeArgs: List<Static>): Static = this

        override fun equals(other: Any?): Boolean = if (other is Static) {
            base == other.base && (args.zipIfSameLength(other.args)?.all { (a, b) -> a == b } ?: false)
        } else {
            false
        }

        override fun hashCode() = Objects.hash(base, args)

    }

    class Dynamic(
            base : MinimalInfo,
            args: List<ApplicationParameter>
    ) : CompleteMinInfo<ApplicationParameter>(base, args) {

        override fun staticApply(typeArgs: List<Static>): Static? {
            val applied = args.map { arg ->
                when (arg) {
                    is Wildcard -> null
                    is Substitution -> arg.staticApply(typeArgs)
                }
            }.liftNull() ?: return null

            return Static(base, applied)
        }

        fun dynamicApply(typeArgs: List<ApplicationParameter>): Dynamic? {
            val applied = args.map { it.dynamicApply(typeArgs) }.liftNull() ?: return null
            return Dynamic(base, applied)
        }

        fun applySelf(self: Static): Dynamic = Dynamic(base, args.map { it.applySelf(self) })

    }

    abstract fun staticApply(typeArgs: List<Static>): Static?

    override fun toString() = buildString {
        append(base)
        if (args.isNotEmpty()) {
            append(args.genericString())
        }
    }

}

operator fun <T> PrefixTree<String, T>.get(info: MinimalInfo) = get(info.packageName, info.simpleName)