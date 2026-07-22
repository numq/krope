package io.github.numq.krope.core

import java.lang.ref.SoftReference as JavaSoftReference

internal actual class SoftReference<T : Any> actual constructor(referent: T) {
    private var ref: JavaSoftReference<T>? = JavaSoftReference(referent)

    actual fun get(): T? = ref?.get()

    actual fun clear() {
        ref?.clear()

        ref = null
    }
}