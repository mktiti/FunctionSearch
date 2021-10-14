package com.mktiti.fsearch.model.build.intermediate

import com.mktiti.fsearch.core.type.MinimalInfo

sealed class QueryImport {

    abstract val packageName: List<String>

    data class TypeImport(val info: MinimalInfo) : QueryImport() {
        override val packageName: List<String>
            get() = info.packageName

        override fun potentialInfo(simpleName: String): MinimalInfo? = if (simpleName == info.simpleName) {
            info
        } else {
            null
        }
    }

    data class PackageImport(override val packageName: List<String>) : QueryImport() {
        override fun potentialInfo(simpleName: String): MinimalInfo = MinimalInfo(packageName, simpleName)
    }

    abstract fun potentialInfo(simpleName: String): MinimalInfo?

}

data class QueryImports(
        val imports: List<QueryImport> // Ordered imports
) {

    companion object {
        val none = QueryImports(emptyList())
    }

    fun potentialInfos(simpleName: String): Sequence<MinimalInfo>
        = imports.asSequence().mapNotNull { it.potentialInfo(simpleName) }

}
