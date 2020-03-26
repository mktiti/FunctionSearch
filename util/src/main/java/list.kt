
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
