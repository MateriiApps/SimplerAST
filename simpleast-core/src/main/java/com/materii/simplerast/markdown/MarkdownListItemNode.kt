package com.materii.simplerast.markdown

import com.materii.simplerast.core.node.Node
import com.materii.simplerast.core.text.RichTextBuilder


open class MarkdownListItemNode<RC>(val bulletSpanProvider: () -> Any) : Node<RC>() {

    override fun render(builder: RichTextBuilder, renderContext: RC) {
        val startIndex = builder.length

        // First render all child nodes, as these are the nodes we want to apply the styles to.
        getChildren()?.forEach { it.render(builder, renderContext) }

        builder.setStyle(bulletSpanProvider(), startIndex, startIndex + 1)
    }
}
