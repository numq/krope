package io.github.numq.krope.core

internal sealed interface RopeBatch {
    val offset: Int

    data class Delete(override val offset: Int, val length: Int) : RopeBatch

    data class Insert(override val offset: Int, val text: String) : RopeBatch
}