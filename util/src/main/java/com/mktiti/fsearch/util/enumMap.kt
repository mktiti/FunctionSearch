package com.mktiti.fsearch.util

interface EnumMap<K : Enum<K>, V> {

    companion object {
        inline fun <reified K : Enum<K>, V> eager(noinline mapper: (key: K) -> V): EnumMap<K, V> {
            val results = enumValues<K>().map(mapper)
            return object : EnumMap<K, V> {
                override fun get(key: K): V = results[key.ordinal]
            }
        }

        fun <K : Enum<K>, V> lazy(mapper: (key: K) -> V): EnumMap<K, V> = FunEnumMap(mapper)

        fun <K : Enum<K>, V> unsafe(map: Map<K, V>): EnumMap<K, V> = MapEnumMap(map)
    }

    operator fun get(key: K): V

}

private class MapEnumMap<K : Enum<K>, V>(
        private val map: Map<K, V>
) : EnumMap<K, V> {

    override fun get(key: K): V = map.getValue(key)

}

private class FunEnumMap<K : Enum<K>, V>(
        private val mapper: (key: K) -> V
) : EnumMap<K, V> {

    private val store = mutableMapOf<K, V>()

    override fun get(key: K): V = store.computeIfAbsent(key, mapper)

}
