package com.materii.simplerast.core.parser

import com.materii.simplerast.core.node.Node

/**
 * Facilitates fast parsing of the source text.
 *
 *
 * For nonterminal subtrees, the provided root will be added to the main, and text between
 * startIndex (inclusive) and endIndex (exclusive) will continue to be parsed into Nodes and
 * added as children under this root.
 *
 *
 * For terminal subtrees, the root will simply be added to the tree and no additional parsing will
 * take place on the text.
 *
 * @param RC The type of render context needed by the node that this contains.
 * @param S The type of state that child nodes will use. This is mainly used to just pass through
 *          the state back to the parser.
 */
class ParseSpec<RC, S> {
  val root: Node<RC>
  val isTerminal: Boolean
  val state: S
  var startIndex: Int = 0
  var endIndex: Int = 0

  constructor(root: Node<RC>, state: S, startIndex: Int, endIndex: Int) {
    this.root = root
    this.state = state
    this.isTerminal = false
    this.startIndex = startIndex
    this.endIndex = endIndex
  }

  constructor(root: Node<RC>, state: S) {
    this.root = root
    this.state = state
    this.isTerminal = true
  }

  fun applyOffset(offset: Int) {
    startIndex += offset
    endIndex += offset
  }

  companion object {

    @JvmStatic
    fun <RC, S> createNonterminal(node: Node<RC>, state: S, startIndex: Int, endIndex: Int): ParseSpec<RC, S> {
      return ParseSpec(node, state, startIndex, endIndex)
    }

    @JvmStatic
    fun <RC, S> createTerminal(node: Node<RC>, state: S): ParseSpec<RC, S> {
      return ParseSpec(node, state)
    }
  }
}

