package io.github.numq.krope.text

import kotlin.jvm.JvmInline

@JvmInline
value class TextRevision(val value: Long) : Comparable<TextRevision> {
    companion object {
        val ZERO = TextRevision(value = 0)
    }

    override fun compareTo(other: TextRevision) = value.compareTo(other.value)
}