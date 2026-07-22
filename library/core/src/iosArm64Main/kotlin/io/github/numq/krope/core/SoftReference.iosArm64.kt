package io.github.numq.krope.core

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference

internal actual class SoftReference<T : Any> actual constructor(referent: T) {
    @OptIn(ExperimentalNativeApi::class)
    private var ref: WeakReference<T>? = WeakReference(referent)

    @OptIn(ExperimentalNativeApi::class)
    actual fun get(): T? = ref?.get()

    @OptIn(ExperimentalNativeApi::class)
    actual fun clear() {
        ref?.clear()

        ref = null
    }
}