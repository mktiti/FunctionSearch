package com.mktiti.fsearch.util.cache

import com.mktiti.fsearch.util.safeCutHead
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class MapInternTrie<T> : InternCache<List<T>> {

    private val cleanLock: ReadWriteLock = ReentrantReadWriteLock()

    private class Node<T> {

        private val refStore = AtomicReference<WeakReference<List<T>>>()
        private val children: MutableMap<T, Node<T>> = ConcurrentHashMap()

        val isEmpty: Boolean
            get() {
                return refStore.get()?.get() == null && children.values.all { it.isEmpty }
            }

        private fun getOrSetValue(value: List<T>): List<T> {
            val storedValue = refStore.get()?.get()
            return if (storedValue != null) {
                storedValue
            } else {
                val asNew = WeakReference(value)

                do {
                    val ref = refStore.get()
                    val refValue = ref?.get()
                    if (refValue != null) {
                        return refValue
                    }
                } while (!refStore.compareAndSet(ref, asNew))
                value
            }
        }

        fun intern(remaining: List<T>, value: List<T>): List<T> {
            val cut = remaining.safeCutHead()

            return if (cut == null) {
                getOrSetValue(value)
            } else {
                val (head, tail) = cut
                val child = children.computeIfAbsent(head) {
                    Node()
                }
                child.intern(tail, value)
            }
        }

        fun clean() {
            val ref = refStore.get()
            if (ref != null && ref.get() == null) {
                refStore.compareAndSet(ref, null)
            }

            children.entries.removeIf { (_, v) ->
                v.clean()
                v.isEmpty
            }
        }

    }

    private val root = Node<T>()

    override fun intern(value: List<T>): List<T> {
        return cleanLock.readLock().withLock {
            root.intern(value, value)
        }
    }

    override fun clean() {
        cleanLock.writeLock().withLock {
            root.clean()
        }
    }

}