package com.materii.simplerast.core.node

import com.materii.simplerast.core.text.RichTextBuilder

/**
 * Represents a single node in an Abstract Syntax Tree. It can (but does not need to) have children.
 *
 * @param RC The render context, can be any object that holds what's required for rendering. See [render].
 */
open class Node<RC>(private var children: MutableCollection<Node<RC>>? = null) {

  fun getChildren(): Collection<Node<RC>>? = children

  fun hasChildren(): Boolean = children?.isNotEmpty() == true

  fun addChild(child: Node<RC>) {
    children = (children ?: ArrayList()).apply {
      add(child)
    }
  }

  open fun render(builder: RichTextBuilder, renderContext: RC) {}

  /**
   * Wrapper around [Node] which simply renders all children.
   */
  open class Parent<RC>(vararg children: Node<RC>?) : Node<RC>(children.mapNotNull { it }.toMutableList()) {
    override fun render(builder: RichTextBuilder, renderContext: RC) {
      getChildren()?.forEach { it.render(builder, renderContext) }
    }

    override fun toString() = "${javaClass.simpleName} >\n" +
      getChildren()?.joinToString("\n->", prefix = ">>", postfix = "\n>|") {
        it.toString()
      }
  }
}
