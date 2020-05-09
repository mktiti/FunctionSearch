package com.mktiti.fsearch.core.type

import com.mktiti.fsearch.util.PrefixTree

data class MinimalInfo(
        val packageName: PackageName,
        val simpleName: String
) {

    fun full(artifact: String) = TypeInfo(
            packageName = packageName,
            name = simpleName,
            artifact = artifact
    )

    override fun toString() = (packageName + simpleName).joinToString(separator = ".")

}

operator fun <T> PrefixTree<String, T>.get(info: MinimalInfo) = get(info.packageName, info.simpleName)