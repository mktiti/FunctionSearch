package com.mktiti.fsearch.util.cache

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class SelfMapIntern<T> : InternCache<T> {

    private val selfMap: MutableMap<T, AtomicReference<WeakReference<T>>> = ConcurrentHashMap(1_000)
    private val cleanLock: ReadWriteLock = ReentrantReadWriteLock()

    override fun intern(value: T): T {
        return cleanLock.readLock().withLock {
            val asNew = WeakReference(value)

            val refStore = selfMap.computeIfAbsent(value) {
                AtomicReference(asNew)
            }

            do {
                val ref = refStore.get()
                val storedValue = ref?.get()
                if (storedValue != null) {
                    return storedValue
                }
            } while (!refStore.compareAndSet(ref, asNew))
            value
        }
    }

    override fun clean() {
        cleanLock.writeLock().withLock {
            selfMap.entries.removeIf {
                it.value.get()?.get() == null
            }
        }
    }

}