package com.materii.simplerast.core.node

import com.materii.simplerast.core.text.RichTextBuilder
import org.jetbrains.annotations.TestOnly

/**
 * @param RC RenderContext
 * @param T Type of Span to apply
 */
open class StyleNode<RC>(val styles: List<Any>) : Node.Parent<RC>() {

  override fun render(builder: RichTextBuilder, renderContext: RC) {
    val startIndex = builder.length

    // First render all child nodes, as these are the nodes we want to apply the styles to.
    super.render(builder, renderContext)

    styles.forEach { builder.setStyle(it, startIndex, builder.length) }
  }

  fun interface SpanProvider<RC> {
    fun get(renderContext: RC) : Iterable<Any>
  }

  /**
   * A slightly optimized version of styling text in a terminal fashion.
   * Use this if you know the node is terminal and will have no children as it will not render them.
   *
   * @see TextNode
   */
  class TextStyledNode<RC>(content: String, val stylesProvider: SpanProvider<RC>) : TextNode<RC>(content) {
    override fun render(builder: RichTextBuilder, renderContext: RC) {
      val startIndex = builder.length
      super.render(builder, renderContext)

      stylesProvider.get(renderContext).forEach {
        builder.setStyle(it, startIndex, builder.length)
      }
    }
  }

  companion object {
    /**
     * Convenience method for creating a [StyleNode] when we already know what
     * the text content will be.
     */
    @JvmStatic
    @TestOnly
    fun <RC> wrapText(content: String, styles: List<Any>): StyleNode<Any> {
      val styleNode = StyleNode<Any>(styles)
      styleNode.addChild(TextNode(content))
      return styleNode
    }
  }
}
