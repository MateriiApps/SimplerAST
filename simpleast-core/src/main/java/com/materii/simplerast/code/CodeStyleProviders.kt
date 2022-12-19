package com.materii.simplerast.code

import com.materii.simplerast.core.node.StyleNode

data class CodeStyleProviders<RC>(
    val defaultStyleProvider: StyleNode.SpanProvider<RC> = emptyProvider(),
    val commentStyleProvider: StyleNode.SpanProvider<RC> = emptyProvider(),
    val literalStyleProvider: StyleNode.SpanProvider<RC> = emptyProvider(),
    val keywordStyleProvider: StyleNode.SpanProvider<RC> = emptyProvider(),
    val identifierStyleProvider: StyleNode.SpanProvider<RC> = emptyProvider(),
    val typesStyleProvider: StyleNode.SpanProvider<RC> = emptyProvider(),
    val genericsStyleProvider: StyleNode.SpanProvider<RC> = emptyProvider(),
    val paramsStyleProvider: StyleNode.SpanProvider<RC> = emptyProvider(),
)

private fun <RC> emptyProvider() = StyleNode.SpanProvider<RC> { emptyList() }