package io.github.numq.krope.core

internal expect class SoftReference<T : Any>(referent: T) {
    fun get(): T?

    fun clear()
}