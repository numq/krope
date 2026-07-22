package io.github.numq.krope.core

internal interface NodeFactory {
    fun create(text: String): Node
}