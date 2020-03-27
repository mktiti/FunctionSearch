fun <T> List<T>.safeCutHead(): Pair<T, List<T>>? = if (isEmpty()) {
    null
} else {
    first() to drop(1)
}

fun <T> List<T>.cutHead(): Pair<T, List<T>> = safeCutHead()!!

fun <T> List<T>.safeCutLast(): Pair<List<T>, T>? = if (isEmpty()) {
    null
} else {
    dropLast(1) to last()
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

fun <T, R> Sequence<T>.lazyReduce(initial: R, combine: (R, T) -> Pair<R, Boolean>): R {
    return if (none()) {
        initial
    } else {
        val (new, stop) = combine(initial, first())
        if (stop) {
            new
        } else {
            drop(1).lazyReduce(new, combine)
        }
    }
}
