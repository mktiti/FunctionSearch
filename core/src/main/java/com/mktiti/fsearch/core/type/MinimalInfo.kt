package com.mktiti.fsearch.core.type

import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard
import com.mktiti.fsearch.core.util.genericString
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.core.util.zipIfSameLength
import com.mktiti.fsearch.util.PrefixTree
import java.util.*

data class MinimalInfo(
        val packageName: PackageName,
        val simpleName: String,
        val virtual: Boolean = false
) {

    companion object {
        fun fromFull(info: TypeInfo) = MinimalInfo(
                packageName = info.packageName,
                simpleName = info.name
        )

        val uniqueVirtual = MinimalInfo(simpleName = "_", packageName = emptyList(), virtual = true)

        val anyWildcard = MinimalInfo(simpleName = "?", packageName = emptyList(), virtual = true)
    }

    val nameParts: List<String>
        get() = simpleName.split('.')

    val fullName: String
        get() = (packageName + simpleName).joinToString(separator = ".")

    fun full(artifact: String) = TypeInfo(
            packageName = packageName,
            name = simpleName,
            artifact = artifact
    )

    fun complete() = CompleteMinInfo.Static(this, emptyList())

    fun staticComplete(args: List<CompleteMinInfo.Static>) = CompleteMinInfo.Static(this, args)

    fun dynamicComplete(args: List<ApplicationParameter>) = CompleteMinInfo.Dynamic(this, args)

    override fun equals(other: Any?): Boolean = when {
        other !is MinimalInfo -> false
        this === anyWildcard -> true
        other === anyWildcard -> true
        virtual -> this === other && this !== uniqueVirtual
        else -> {
            other.simpleName == simpleName && other.packageName == packageName
        }
    }

    override fun hashCode(): Int = Objects.hash(simpleName, packageName, virtual)

    override fun toString() = (packageName + simpleName).joinToString(separator = ".")

}

sealed class CompleteMinInfo<out P : Any>(
        val base: MinimalInfo,
        val args: List<P>
) : StaticApplicable {

    class Static(
            base : MinimalInfo,
            args: List<Static>
    ) : CompleteMinInfo<Static>(base, args) {

        companion object {
            fun List<Static>.holders() = map { it.holder() }
        }

        override fun staticApply(typeArgs: List<TypeHolder.Static>): TypeHolder.Static = holder()

        override fun equals(other: Any?): Boolean = if (other is Static) {
            base == other.base && (args.zipIfSameLength(other.args)?.all { (a, b) -> a == b } ?: false)
        } else {
            false
        }

        override fun hashCode() = Objects.hash(base, args)

        override fun holder(): TypeHolder.Static = TypeHolder.Static.Indirect(this)

    }

    class Dynamic(
            base : MinimalInfo,
            args: List<ApplicationParameter>
    ) : CompleteMinInfo<ApplicationParameter>(base, args) {

        override fun staticApply(typeArgs: List<TypeHolder.Static>): TypeHolder.Static? {
            val applied = args.map { arg ->
                when (arg) {
                    is BoundedWildcard -> null
                    is Substitution -> arg.staticApply(typeArgs)?.info
                } ?: return null
            }

            return Static(base, applied).holder()
        }

        fun dynamicApply(typeArgs: List<ApplicationParameter>): Dynamic? {
            val applied = args.map { it.dynamicApply(typeArgs) }.liftNull() ?: return null
            return Dynamic(base, applied)
        }

        fun applySelf(self: TypeHolder.Static): Dynamic = Dynamic(base, args.map { it.applySelf(self) })

        override fun holder(): TypeHolder.Dynamic = TypeHolder.Dynamic.Indirect(this)

    }

    abstract fun holder(): TypeHolder<*, *>

    override fun toString() = buildString {
        append(base)
        if (args.isNotEmpty()) {
            append(args.genericString())
        }
    }

}

operator fun <T> PrefixTree<String, T>.get(info: MinimalInfo) = get(info.packageName, info.simpleName)