package io.github.numq.krope.core

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal class ThreadSafeLruCache<K, V>(private val maxSize: Int) {
    private val lock = SynchronizedObject()

    private val map = LinkedHashMap<K, V>()

    private fun trimToSize() {
        while (map.size > maxSize) {
            val eldestKey = map.keys.first()

            map.remove(eldestKey)
        }
    }

    val size: Int get() = synchronized(lock) { map.size }

    operator fun get(key: K): V? = synchronized(lock) {
        val value = map.remove(key)

        if (value != null) {
            map[key] = value
        }

        value
    }

    fun getOrPut(key: K, defaultValue: () -> V): V = synchronized(lock) {
        val existing = map.remove(key)

        val value = existing ?: defaultValue()

        map[key] = value

        trimToSize()

        value
    }

    operator fun set(key: K, value: V) = synchronized(lock) {
        map.remove(key)

        map[key] = value

        trimToSize()
    }

    fun clear() = synchronized(lock) {
        map.clear()
    }
}