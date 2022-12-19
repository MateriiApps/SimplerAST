package com.materii.simplerast.core.simple

import com.materii.simplerast.core.node.Node
import com.materii.simplerast.core.parser.Parser
import com.materii.simplerast.core.parser.Rule
import com.materii.simplerast.core.text.RichTextBuilder

object CoreRenderer {

  @JvmStatic
  fun <RC, S, T : RichTextBuilder> render(
    builder: T,
    source: CharSequence,
    rules: Collection<Rule<RC, Node<RC>, S>>,
    initialState: S,
    renderContext: RC
  ): T {
    val parser = Parser<RC, Node<RC>, S>().addRules(rules)
    return render(builder, source, parser, initialState, renderContext)
  }

  @JvmStatic
  fun <RC, S, T : RichTextBuilder> render(
    builder: T,
    source: CharSequence,
    parser: Parser<RC, Node<RC>, S>,
    initialState: S,
    renderContext: RC
  ): T {
    return render(builder, parser.parse(source, initialState), renderContext)
  }

  @JvmStatic
  fun <RC, T : RichTextBuilder> render(
    builder: T,
    ast: Collection<Node<RC, >>,
    renderContext: RC
  ): T {
    for (node in ast) {
      node.render(builder, renderContext)
    }
    return builder
  }
}
