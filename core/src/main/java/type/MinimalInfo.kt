package type

import PackageName
import PrefixTree
import TypeInfo

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