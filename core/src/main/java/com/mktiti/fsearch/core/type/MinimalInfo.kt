package com.mktiti.fsearch.core.type

import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
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
        fun of(packageName: PackageName, simpleName: String): MinimalInfo {
            return MinimalInfo(packageName, simpleName)
        }

        fun virtual(packageName: PackageName, simpleName: String): MinimalInfo {
            return MinimalInfo(packageName, simpleName, virtual = true)
        }

        val uniqueVirtual = virtual(simpleName = "_", packageName = emptyList())

        val anyWildcard = virtual(simpleName = "?", packageName = emptyList())
    }

    val nameParts: List<String>
        get() = simpleName.split('.')

    val fullName: String
        get() = (packageName + simpleName).joinToString(separator = ".")

    fun full(artifact: String) = TypeInfo(
            minimal = this,
            artifact = artifact
    )

    fun complete() = CompleteMinInfo.Static(this, emptyList())

    fun staticComplete(args: List<CompleteMinInfo.Static>) = CompleteMinInfo.Static(this, args)

    fun dynamicComplete(args: List<ApplicationParameter>) = CompleteMinInfo.Dynamic(this, args)

    fun sameAs(other: MinimalInfo): Boolean
        = packageName == other.packageName &&
            simpleName == other.simpleName &&
            virtual == other.virtual

    private fun dataEq(info: MinimalInfo) = info.simpleName == simpleName && info.packageName == packageName

    override fun equals(other: Any?): Boolean = when {
        other !is MinimalInfo -> false
        dataEq(anyWildcard) -> true
        other.dataEq(anyWildcard) -> true
        else -> if (virtual && this === uniqueVirtual) {
            false
        } else {
            dataEq(other)
        }
    }

    override fun hashCode(): Int = Objects.hash(simpleName, packageName, virtual)

    override fun toString() = (packageName + simpleName).joinToString(separator = ".")

}

sealed class CompleteMinInfo<out P : Any> : StaticApplicable {

    abstract val base: MinimalInfo
    abstract val args: List<P>

    class Static(
            override val base : MinimalInfo,
            override val args: List<Static>
    ) : CompleteMinInfo<Static>() {

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
            override val base : MinimalInfo,
            override val args: List<ApplicationParameter>
    ) : CompleteMinInfo<ApplicationParameter>() {

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