
fun <T> identity(x: T): T = x

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

inline fun <T : Any, reified S> Collection<T>.castIfAllInstance(): List<S>? = map {
    if (it is S) it else return null
}

typealias NodeVisitor<T> = (node: TreeNode<T>, depth: Int, hasMoreSiblings: Boolean) -> Unit

data class TreeNode<out T>(val value: T, val children: List<TreeNode<T>>) {

    fun walkDf(onNode: NodeVisitor<T>) {
        walkDfInner(onNode = onNode, depth = 0, hasMoreSiblings = false)
    }

    private fun walkDfInner(onNode: NodeVisitor<T>, depth: Int, hasMoreSiblings: Boolean) {
        onNode(this, depth, hasMoreSiblings)
        children.forEachIndexed { i, node ->
            node.walkDfInner(onNode, depth + 1, i < children.size - 1)
        }
    }

}
