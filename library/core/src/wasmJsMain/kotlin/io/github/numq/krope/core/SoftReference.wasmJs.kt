package io.github.numq.krope.core

internal actual class SoftReference<T : Any> actual constructor(referent: T) {
    private var ref: T? = referent

    actual fun get(): T? = ref

    actual fun clear() {
        ref = null
    }
}