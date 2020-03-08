/*

fun printType(type: Type) {
    type.superTree().walkDf { node, depth ->
        print("    ".repeat(depth))
        println(node.value.fullName())
    }
    println("==================")
}
*/

inline fun <T : Any> Boolean.elseNull(onTrue: () -> T): T? = if (this) onTrue() else null

fun <A, B> List<A>.zipIfSameLength(other: List<B>): List<Pair<A, B>>? = (size == other.size).elseNull { zip(other) }

fun <T : Any> Collection<T>.genericString(mapper: (T) -> String) =
    joinToString(prefix = "<", separator = ", ", postfix = ">", transform = mapper)

fun <T : Any> List<T?>.liftNull(): List<T>? {
    val filtered = filterNotNull()
    return if (filtered.size == size) {
        filtered
    } else {
        null
    }
}

inline fun <K, V, reified S> Map<K, V>.castIfAllValuesInstance(): Map<K, S>? = mapValues {
    if (it is S) {
        it
    } else {
        return null
    }
}

inline fun <T : Any, reified S> Collection<T>.castIfAllInstance(): List<S>? {
    val instances = filterIsInstance<S>()
    return if (instances.size == size) instances else null
}

/*
fun printType(type: TType) {
    type.supers().walkDf { node, depth ->
        print("    ".repeat(depth))
        println(node.value.fullName())
    }
    println("==================")
}
*/

fun printType(type: Type) {
    val info = when (type) {
        is Type.NonGenericType.DirectType -> "Direct Type"
        is Type.NonGenericType.StaticAppliedType -> "Statically Applied Type"
        is Type.GenericType.TypeTemplate -> "Type template"
        is Type.GenericType.DynamicAppliedType -> "Dynamically Applied Type"
    }
    println("$info ${type.name}")

    type.supersTree.walkDf { node, depth ->
        print("    ".repeat(depth))
        println(node.value.fullName)
    }
    println("==================")
}

data class TreeNode<out T>(val value: T, val children: List<TreeNode<T>>) {

    fun walkDf(depth: Int = 0, onNode: (TreeNode<T>, Int) -> Unit) {
        onNode(this, depth)
        children.forEach {
            it.walkDf(depth + 1, onNode)
        }
    }

}
