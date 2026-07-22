package io.github.numq.krope.core

import java.lang.ref.SoftReference as JvmSoftReference

internal actual class SoftReference<T : Any> actual constructor(referent: T) {
    private var ref: JvmSoftReference<T>? = JvmSoftReference(referent)

    actual fun get(): T? = ref?.get()

    actual fun clear() {
        ref?.clear()

        ref = null
    }
}