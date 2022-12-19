package com.materii.simplerast.markdown

import com.materii.simplerast.core.node.Node
import com.materii.simplerast.core.node.StyleNode
import com.materii.simplerast.core.parser.ParseSpec
import com.materii.simplerast.core.parser.Parser
import com.materii.simplerast.core.parser.Rule
import com.materii.simplerast.core.simple.CoreMarkdownRules
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Support for full markdown representations.
 *
 * @see com.discord.simpleast.core.simple.CoreMarkdownRules
 */
object MarkdownRules {
  /**
   * Handles markdown list syntax. Must have a whitespace after list itemcharacter `*`
   * Example:
   * ```
   * * item 1
   * * item 2
   * ```
   */
  val PATTERN_LIST_ITEM = """^\*[ \t](.*)(?=\n|$)""".toPattern()
  /**
   * Handles markdown header syntax. Must have a whitespace after header character `#`
   * Example:
   * ```
   * # Header 1
   * ## Header 2
   * ### Header 3
   * ```
   */
  val PATTERN_HEADER_ITEM = """^\s*(#+)[ \t](.*) *(?=\n|$)""".toPattern()
  /**
   * Handles alternate version of headers. Must have 3+ `=` characters.
   * Example:
   * ```
   * Alternative Header 1
   * ====================
   *
   * Alternative Header 2
   * ----------
   * ```
   */
  val PATTERN_HEADER_ITEM_ALT = """^\s*(.+)\n *(=|-){3,} *(?=\n|$)""".toPattern()
  /**
   * Similar to [PATTERN_HEADER_ITEM_ALT] but allows specifying a class type annotation for styling
   * at the end of the line.
   * Example:
   * ```
   * Alternative Header 1 {red large}
   * ====================
   * ```
   */
  val PATTERN_HEADER_ITEM_ALT_CLASSED =
      """^\s*(?:(?:(.+)(?: +\{([\w ]*)\}))|(.*))[ \t]*\n *([=\-]){3,}[ \t]*(?=\n|$)""".toRegex().toPattern()

  class ListItemRule<RC, S>(private val bulletSpanProvider: () -> Any) :
      Rule.BlockRule<RC, Node<RC>, S>(PATTERN_LIST_ITEM) {

    override fun parse(matcher: Matcher, parser: Parser<RC, in Node<RC>, S>, state: S)
        : ParseSpec<RC, S> {
      val node = MarkdownListItemNode<RC>(bulletSpanProvider)
      return ParseSpec.createNonterminal(node, state, matcher.start(1), matcher.end(1))
    }
  }

  open class HeaderRule<RC, S>(pattern: Pattern,
                           protected val styleSpanProvider: (Int) -> Any) :
      Rule.BlockRule<RC, Node<RC>, S>(pattern) {

    constructor(styleSpanProvider: (Int) -> Any) : this(PATTERN_HEADER_ITEM, styleSpanProvider)

    protected open fun createHeaderStyleNode(headerStyleGroup: String): StyleNode<RC> {
      val numHeaderIndicators = headerStyleGroup.length
      return StyleNode(listOf(styleSpanProvider(numHeaderIndicators)))
    }

    override fun parse(matcher: Matcher, parser: Parser<RC, in Node<RC>, S>, state: S): ParseSpec<RC, S> =
        ParseSpec.createNonterminal(
                createHeaderStyleNode(matcher.group(1)),
                state, matcher.start(2), matcher.end(2))
  }

  open class HeaderLineRule<RC, S>(pattern: Pattern = PATTERN_HEADER_ITEM_ALT, styleSpanProvider: (Int) -> Any) :
      HeaderRule<RC, S>(pattern, styleSpanProvider) {

    override fun parse(matcher: Matcher, parser: Parser<RC, in Node<RC>, S>, state: S)
        : ParseSpec<RC, S> = ParseSpec.createNonterminal(
            createHeaderStyleNode(matcher.group(2)), state, matcher.start(1), matcher.end(1))

    override fun createHeaderStyleNode(headerStyleGroup: String): StyleNode<RC> {
      val headerIndicator = when (headerStyleGroup) {
        "=" -> 1
        else -> 2
      }
      return StyleNode(listOf(styleSpanProvider(headerIndicator)))
    }
  }

  /**
   * Allow [HeaderLineRule]'s to specify custom styles via markdown.
   *
   * This is not part of the markdown specification but is useful for flexible headers.
   *
   * Example:
   * ```
   * My Line Header in Red {red}
   * ==========
   * ```
   *
   * @param RC RenderContext
   * @param T type of span applied for classes
   * @see PATTERN_HEADER_ITEM_ALT_CLASSED
   */
  open class HeaderLineClassedRule<RC, S>(styleSpanProvider: (Int) -> Any,
                                          @Suppress("MemberVisibilityCanBePrivate")
                                               val classSpanProvider: (String) -> Any?,
                                          @Suppress("MemberVisibilityCanBePrivate")
                                               protected val innerRules: List<Rule<RC, Node<RC>, S>>,
  ) :
      HeaderLineRule<RC, S>(PATTERN_HEADER_ITEM_ALT_CLASSED, styleSpanProvider) {

      constructor(
          styleSpanProvider: (Int) -> Any,
          classSpanProvider: (String) -> Any?,
          boldStyleProvider: () -> Any,
          underlineStyleProvider: () -> Any,
          italicsStyleProvider: () -> Any,
          strikethroughStyleProvider: () -> Any,
      ) : this(styleSpanProvider, classSpanProvider,
            CoreMarkdownRules.createCoreMarkdownRules<RC, S>(
                includeTextRule = false,
                boldStyleProvider = boldStyleProvider,
                underlineStyleProvider = underlineStyleProvider,
                italicsStyleProvider = italicsStyleProvider,
                strikethroughStyleProvider = strikethroughStyleProvider
            ) + CoreMarkdownRules.createTextRule())

    override fun parse(matcher: Matcher, parser: Parser<RC, in Node<RC>, S>, state: S): ParseSpec<RC, S> {
      val defaultStyleNode = createHeaderStyleNode(matcher.group(4))
      val headerBody = matcher.group(1) ?: matcher.group(3)
      val children = parser.parse(headerBody, state, innerRules)
      @Suppress("UNCHECKED_CAST")
      children.forEach { defaultStyleNode.addChild(it as Node<RC>) }

      val classes = matcher.group(2)?.trim()?.split(' ')
      val classSpans = classes?.mapNotNull { classSpanProvider(it) } ?: emptyList()

      val headerNode = if (classSpans.isNotEmpty()) {
        // Apply class stylings last
        StyleNode<RC>(classSpans).apply { addChild(defaultStyleNode) }
      } else {
        defaultStyleNode
      }

      return ParseSpec.createTerminal(headerNode, state)
    }
  }

  @Suppress("MemberVisibilityCanBePrivate")
  @JvmStatic
  inline fun <RC, S> createHeaderRules(
      crossinline style: (Int) -> Any,
  ): List<Rule<RC, Node<RC>, S>> {
    return listOf(
        HeaderRule { style(it) },
        HeaderLineRule { style(it) }
    )
  }

  @JvmStatic
  inline fun <RC, S> createMarkdownRules(
      crossinline headerStyle: (Int) -> Any,
      crossinline bulletStyle: () -> Any
  ) = createHeaderRules<RC, S>(headerStyle) + ListItemRule { bulletStyle() }
}