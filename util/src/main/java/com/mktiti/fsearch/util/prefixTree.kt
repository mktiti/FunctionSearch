@file:Suppress("UNUSED")

package com.mktiti.fsearch.util

interface PrefixTree<N, out L> : Iterable<L> {

    val empty: Boolean
    val size: Int

    operator fun get(nodes: List<N>): L?

    fun get(parents: List<N>, last: N): L?

    operator fun get(vararg nodes: N): L? = get(nodes.toList())

    fun subtree(nodes: List<N>): PrefixTree<N, L>?

}

interface MutablePrefixTree<N, L> : PrefixTree<N, L> {

    operator fun set(nodes: List<N>, newValue: L): Boolean

    operator fun set(node: N, newValue: L): Boolean

    operator fun set(nodes: List<N>, last: N, newValue: L): Boolean

    fun mutableSubtree(nodes: List<N>): MutablePrefixTree<N, L>?

    fun mutableSubtreeSafe(nodes: List<N>): MutablePrefixTree<N, L>

    fun removeIf(pred: (L) -> Boolean)
}

fun <N, L> mapPrefixTree(): PrefixTree<N, L> = MapPrefixNode()

fun <N, L> mapMutablePrefixTree(): MutablePrefixTree<N, L> = MapPrefixNode()

// TODO abstract away similarities
private class MapPrefixNode<N, L>(
        private var value: L? = null,
        private val subNodes: MutableMap<N, MapPrefixNode<N, L>> = HashMap()
) : MutablePrefixTree<N, L> {

    override val size: Int
        get() = (if (value == null) 0 else 1) + subNodes.values.sumBy { it.size }

    override val empty: Boolean
        get() = value == null && subNodes.all { it.value.empty }

    override fun get(parents: List<N>, last: N): L? {
        return when (val cut = parents.safeCutHead()) {
            null -> subNodes[last]?.value
            else -> {
                val (head, tail) = cut
                subNodes[head]?.get(tail, last)
            }
        }
    }

    override fun get(nodes: List<N>): L? {
        return when (val cut = nodes.safeCutHead()) {
            null -> value
            else -> {
                val (head, tail) = cut
                subNodes[head]?.get(tail)
            }
        }
    }

    override fun mutableSubtreeSafe(nodes: List<N>): MutablePrefixTree<N, L> {
        return when (val cut = nodes.safeCutHead()) {
            null -> this
            else -> {
                val (head, tail) = cut
                subNodes.computeIfAbsent(head) { MapPrefixNode() }.mutableSubtreeSafe(tail)
            }
        }
    }

    override fun mutableSubtree(nodes: List<N>): MutablePrefixTree<N, L>? {
        return when (val cut = nodes.safeCutHead()) {
            null -> this
            else -> {
                val (head, tail) = cut
                subNodes[head]?.mutableSubtree(tail)
            }
        }
    }

    override fun subtree(nodes: List<N>): PrefixTree<N, L>? = mutableSubtree(nodes)

    private fun set(newValue: L): Boolean {
        return (value != null).apply {
            value = newValue
        }
    }

    override fun set(node: N, newValue: L): Boolean {
        return subNodes.computeIfAbsent(node) { MapPrefixNode() }.set(newValue)
    }

    override fun set(nodes: List<N>, newValue: L): Boolean {
        return when (val cut = nodes.safeCutHead()) {
            null -> {
                set(newValue)
            }
            else -> {
                val (head, tail) = cut
                subNodes.computeIfAbsent(head) { MapPrefixNode() }.set(tail, newValue)
            }
        }
    }

    override fun set(nodes: List<N>, last: N, newValue: L): Boolean {
        return when (val cut = nodes.safeCutHead()) {
            null -> set(last, newValue)
            else -> {
                val (head, otherPre) = cut
                subNodes.computeIfAbsent(head) { MapPrefixNode() }.set(otherPre, last, newValue)
            }
        }
    }

    override fun removeIf(pred: (L) -> Boolean) {
        value?.let { frozenValue ->
            if (frozenValue != null && pred(frozenValue)) {
                value = null
            }
        }

        subNodes.values.forEach { child -> child.removeIf(pred) }
        subNodes -= subNodes.filter { it.value.empty }.keys
    }

    override fun iterator(): Iterator<L> = iterator {
        value?.let { yield(it) }
        subNodes.values.forEach {
            yieldAll(it)
        }
    }
}