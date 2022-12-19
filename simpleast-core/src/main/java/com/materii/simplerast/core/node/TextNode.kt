package com.materii.simplerast.core.node

import com.materii.simplerast.core.text.RichTextBuilder

/**
 * Node representing simple text.
 */
open class TextNode<RC> (val content: String) : Node<RC>() {
  override fun render(builder: RichTextBuilder, renderContext: RC) {
    builder.append(content)
  }

  override fun toString() = "${javaClass.simpleName}: $content"
}
