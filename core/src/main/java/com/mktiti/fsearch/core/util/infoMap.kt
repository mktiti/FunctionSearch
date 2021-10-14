package com.mktiti.fsearch.core.util

import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.util.PrefixTree
import java.util.stream.Stream
import java.util.stream.StreamSupport

interface InfoMap<out V> {

    companion object {
        fun <V> fromPrefix(tree: PrefixTree<String, V>): InfoMap<V> = PrefixTreeInfoMap(tree)

        fun <V> fromMap(store: Map<MinimalInfo, V>): InfoMap<V> = MapInfoMap(store)

        fun <V> combine(maps: Collection<InfoMap<V>>, mapper: (Collection<V>) -> V?): InfoMap<V> = CombinedInfoMap(maps, mapper)

        fun <V> empty(): InfoMap<V> = fromMap(emptyMap())
    }

    operator fun get(info: MinimalInfo): V?

    fun all(): Stream<out V>

}

private class CombinedInfoMap<V>(
        private val stores: Collection<InfoMap<V>>,
        private val mapper: (Collection<V>) -> V?
) : InfoMap<V> {

    override fun get(info: MinimalInfo): V? {
        val mapped = stores.mapNotNull {
            it[info]
        }

        return mapper(mapped)
    }

    override fun all(): Stream<V> = stores.stream().flatMap { it.all() }

}

fun <T> InfoMap<Collection<T>>.flatAll(): Stream<T> = all().flatMap { it.stream() }

private class MapInfoMap<V>(
        private val store: Map<MinimalInfo, V>
) : InfoMap<V> {

    override fun get(info: MinimalInfo): V? = store[info]

    override fun all() = store.values.stream()

}

private class PrefixTreeInfoMap<V>(
        private val prefixTree: PrefixTree<String, V>
) : InfoMap<V> {

    override fun get(info: MinimalInfo): V? = prefixTree[info.packageName + info.simpleName]

    override fun all(): Stream<V> = StreamSupport.stream(prefixTree.spliterator(), false)

}
