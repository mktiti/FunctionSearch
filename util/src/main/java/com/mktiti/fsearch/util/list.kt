@file:Suppress("UNUSED")

package com.mktiti.fsearch.util

fun <T> List<T>.indexOfOrNull(element: T): Int? = when (val index = indexOf(element)) {
    -1 -> null
    else -> index
}

fun <T> T.repeat(count: Int): List<T> = (0 until count).map { this }

fun <T> List<T>.safeCutHead(): Pair<T, List<T>>? = if (isEmpty()) {
    null
} else {
    first() to subList(1, size)
}

fun <T> List<T>.cutHead(): Pair<T, List<T>> = safeCutHead()!!

fun <T> List<T>.safeCutLast(): Pair<List<T>, T>? = if (isEmpty()) {
    null
} else {
    subList(0, size - 1) to last()
}

fun <T> List<T>.cutLast(): Pair<List<T>, T> = safeCutLast()!!

fun <T> Collection<T>.allPermutations(): List<List<T>> {
    // TODO - quick and dirty
    fun List<T>.allPermutationsInner(): List<List<T>> {
        return if (isEmpty()) {
            listOf(emptyList())
        } else {
            (0 until size).flatMap { i ->
                val elem = get(i)
                val rest = take(i) + drop(i + 1)
                rest.allPermutations().map { tail ->
                    listOf(elem) + tail
                }
            }
        }
    }

    return toList().allPermutationsInner()
}

tailrec fun <T, R> Iterator<T>.rollIndexed(initial: R, startIndex: Int = 0, combine: (Int, R, T) -> Pair<R, Boolean>): R {
    return if (hasNext()) {
        val (combined, shouldStop) = combine(startIndex, initial, next())
        return if (shouldStop) {
            combined
        } else {
            rollIndexed(combined, startIndex + 1, combine)
        }
    } else {
        initial
    }
}

tailrec fun <T, R> List<T>.rollIndexed(initial: R, startIndex: Int = 0, combine: (Int, R, T) -> Pair<R, Boolean>): R {
    val (head, tail) = safeCutHead() ?: return initial
    val (combined, shouldStop) = combine(startIndex, initial, head)
    return if (shouldStop) {
        combined
    } else {
        tail.rollIndexed(combined, startIndex + 1, combine)
    }
}

fun <T, R> Iterator<T>.roll(initial: R, combine: (R, T) -> Pair<R, Boolean>): R = rollIndexed(
        initial = initial,
        startIndex = 0,
        combine = { _, acc, elem -> combine(acc, elem) }
)


fun <T, R> List<T>.roll(initial: R, combine: (R, T) -> Pair<R, Boolean>): R = rollIndexed(
        initial = initial,
        startIndex = 0,
        combine = { _, acc, elem -> combine(acc, elem) }
)
