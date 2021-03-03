package com.mktiti.fsearch.client.cli.util

interface IntRangeMap<out E> {

    val values: Collection<E>
        get() = entries.map { it.second }

    val keys: Collection<IntRange>
        get() = entries.map { it.first }

    val entries: Collection<Pair<IntRange, E>>

    val size: Int
        get() = entries.size

    operator fun get(key: Int): E?

}

interface MutableIntRangeMap<E> : IntRangeMap<E> {

    operator fun set(range: IntRange, value: E)

    operator fun minusAssign(range: IntRange)

}

class ListIntRangeMap<E> : MutableIntRangeMap<E> {

    private val backingList: MutableList<Pair<IntRange, E>> = ArrayList()

    override val entries: Collection<Pair<IntRange, E>>
        get() = backingList

    override fun get(key: Int): E? {
        val candidate = backingList.asSequence()
                .takeWhile { (range, _) -> range.first <= key }
                .firstOrNull()

        return if (candidate != null && key in candidate.first) {
            candidate.second
        } else {
            null
        }
    }

    override fun minusAssign(range: IntRange) {
        backingList.removeIf { it.first == range }
    }

    override fun set(range: IntRange, value: E) {
        val entry = range to value

        val last = backingList.lastOrNull()
        if (last == null || last.first.last < range.first) {
            backingList += entry
        } else {
            val index: Int = backingList.indexOfFirst { (r, _) ->
                range.last < r.first
            }

            if (index < 0) {
                error("Cannot fit range $range into map, conflict!")
            }

            when (val prev = backingList.getOrNull(index - 1)?.first) {
                null -> backingList.add(0, entry)
                else -> if (prev.last < range.first) {
                    backingList.add(index, entry)
                } else {
                    error("Cannot fit range $range into map, conflict!")
                }
            }
        }
    }


}
