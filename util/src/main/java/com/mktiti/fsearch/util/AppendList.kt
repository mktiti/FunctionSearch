package com.mktiti.fsearch.util

data class AppendList<E>(
        val start: List<E>,
        val end: E
) : List<E> {

    companion object {
        fun <T> List<T>.append(end: T) = AppendList(this, end)

        fun <T> singleton(element: T) = AppendList(emptyList(), element)
    }

    override val size: Int
        get() = start.size + 1

    override fun contains(element: E): Boolean = element in start || end == element

    override fun containsAll(elements: Collection<E>): Boolean {
        return start.containsAll(elements - end)
    }

    override fun get(index: Int): E = when {
        index < start.size -> start[index]
        index == start.size -> end
        else -> throw IndexOutOfBoundsException("Index: $index, Size: $size")
    }

    override fun indexOf(element: E): Int {
        return start.indexOfOrNull(element).orElse {
            if (end == element) start.size else -1
        }
    }

    override fun isEmpty(): Boolean = false

    override fun iterator(): Iterator<E> = iterator {
        yieldAll(start)
        yield(end)
    }

    override fun lastIndexOf(element: E): Int = if (end == element) {
        start.size
    } else {
        start.lastIndexOf(end)
    }

    override fun listIterator(): ListIterator<E> = listIterator(0)

    override fun listIterator(index: Int) = object : ListIterator<E> {
        var currentIndex = index

        override fun hasNext(): Boolean = currentIndex < start.size

        override fun hasPrevious(): Boolean = currentIndex > 0

        override fun next(): E = if (hasNext()) {
            get(currentIndex).also {
                currentIndex++
            }
        } else {
            throw NoSuchElementException()
        }

        override fun nextIndex(): Int = if (hasNext()) {
            currentIndex + 1
        } else {
            size
        }

        override fun previous(): E = if (hasPrevious()) {
            get(currentIndex).also {
                currentIndex--
            }
        } else {
            throw NoSuchElementException()
        }

        override fun previousIndex(): Int = if (hasPrevious()) {
            currentIndex - 1
        } else {
            size
        }

    }

    override fun subList(fromIndex: Int, toIndex: Int): List<E> = if (fromIndex == 0 && toIndex == start.size) {
        start
    } else if (fromIndex == start.size && toIndex == size) {
        listOf(end)
    } else if (fromIndex == toIndex) {
        emptyList()
    } else if (toIndex == size) {
        copy(start = start.subList(fromIndex, start.size))
    } else {
        start.subList(fromIndex, toIndex)
    }

}