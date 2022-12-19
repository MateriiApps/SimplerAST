package com.materii.simplerast.core.simple

import com.materii.simplerast.core.node.Node
import com.materii.simplerast.core.node.StyleNode
import com.materii.simplerast.core.node.TextNode
import com.materii.simplerast.core.parser.ParseSpec
import com.materii.simplerast.core.parser.Parser
import com.materii.simplerast.core.parser.Rule
import java.util.regex.Matcher
import java.util.regex.Pattern

object CoreMarkdownRules {

  val PATTERN_BOLD = Pattern.compile("^\\*\\*([\\s\\S]+?)\\*\\*(?!\\*)")
  val PATTERN_UNDERLINE = Pattern.compile("^__([\\s\\S]+?)__(?!_)")
  val PATTERN_STRIKETHRU = Pattern.compile("^~~(?=\\S)([\\s\\S]*?\\S)~~")
  val PATTERN_NEWLINE = Pattern.compile("""^(?:\n *)*\n""")
  val PATTERN_TEXT = Pattern.compile("^[\\s\\S]+?(?=[^0-9A-Za-z\\s\\u00c0-\\uffff]|\\n| {2,}\\n|\\w+:\\S|$)")
  val PATTERN_ESCAPE = Pattern.compile("^\\\\([^0-9A-Za-z\\s])")

  val PATTERN_ITALICS = Pattern.compile(
      // only match _s surrounding words.
      "^\\b_" + "((?:__|\\\\[\\s\\S]|[^\\\\_])+?)_" + "\\b" +
          "|" +
          // Or match *s that are followed by a non-space:
          "^\\*(?=\\S)(" +
          // Match any of:
          //  - `**`: so that bolds inside italics don't close the
          // italics
          //  - whitespace
          //  - non-whitespace, non-* characters
          "(?:\\*\\*|\\s+(?:[^*\\s]|\\*\\*)|[^\\s*])+?" +
          // followed by a non-space, non-* then *
          ")\\*(?!\\*)"
  )

  inline fun <RC, S> createBoldRule(
    crossinline boldStyleProvider: () -> Any
  ): Rule<RC, Node<RC>, S> {
    return createCoreStyleRule(PATTERN_BOLD) {
      listOf(boldStyleProvider())
    }
  }


  inline fun <RC, S> createUnderlineRule(
    crossinline underlineStyleProvider: () -> Any
  ): Rule<RC, Node<RC>, S> =
      createCoreStyleRule(PATTERN_UNDERLINE) { listOf(underlineStyleProvider()) }

  inline fun <RC, S> createStrikethroughRule(
    crossinline strikethroughStyleProvider: () -> Any
  ): Rule<RC, Node<RC>, S> =
      createCoreStyleRule(PATTERN_STRIKETHRU) { listOf(strikethroughStyleProvider()) }

  fun <RC, S> createTextRule(): Rule<RC, Node<RC>, S> {
    return object : Rule<RC, Node<RC>, S>(PATTERN_TEXT) {
      override fun parse(matcher: Matcher, parser: Parser<RC, Node<RC>, S>, state: S): ParseSpec<RC, S> {
        val node = TextNode<RC>(matcher.group())
        return ParseSpec.createTerminal(node, state)
      }
    }
  }
  fun <RC, S> createNewlineRule(): Rule<RC, Node<RC>, S> {
    return object : Rule.BlockRule<RC, Node<RC>, S>(PATTERN_NEWLINE) {
      override fun parse(matcher: Matcher, parser: Parser<RC, Node<RC>, S>, state: S): ParseSpec<RC, S> {
        val node = TextNode<RC>("\n")
        return ParseSpec.createTerminal(node, state)
      }
    }
  }

  fun <RC, S> createEscapeRule(): Rule<RC, Node<RC>, S> {
    return object : Rule<RC, Node<RC>, S>(PATTERN_ESCAPE) {
      override fun parse(matcher: Matcher, parser: Parser<RC, Node<RC>, S>, state: S): ParseSpec<RC, S> {
        return ParseSpec.createTerminal(TextNode(matcher.group(1)!!), state)
      }
    }
  }

  inline fun <RC, S> createItalicsRule(
    crossinline italicsStyleProvider: () -> Any
  ): Rule<RC, Node<RC>, S> {
    return object : Rule<RC, Node<RC>, S>(PATTERN_ITALICS) {
      override fun parse(matcher: Matcher, parser: Parser<RC, Node<RC>, S>, state: S): ParseSpec<RC, S> {
        val startIndex: Int
        val endIndex: Int
        val asteriskMatch = matcher.group(2)
        if (asteriskMatch != null && asteriskMatch.isNotEmpty()) {
          startIndex = matcher.start(2)
          endIndex = matcher.end(2)
        } else {
          startIndex = matcher.start(1)
          endIndex = matcher.end(1)
        }

        val styles = ArrayList<Any>(1)
        styles.add(italicsStyleProvider())

        val node = StyleNode<RC>(styles)
        return ParseSpec.createNonterminal(node, state, startIndex, endIndex)
      }
    }
  }

  @JvmOverloads
  @JvmStatic
  inline fun <RC, S> createCoreMarkdownRules(
    includeTextRule: Boolean = true,
    includeEscapeRule:Boolean = true,
    crossinline boldStyleProvider: () -> Any,
    crossinline underlineStyleProvider: () -> Any,
    crossinline italicsStyleProvider: () -> Any,
    crossinline strikethroughStyleProvider: () -> Any,
  ): MutableList<Rule<RC, Node<RC>, S>> {
    val rules = ArrayList<Rule<RC, Node<RC>, S>>()
    if (includeEscapeRule) {
      rules.add(createEscapeRule())
    }
    rules.add(createNewlineRule())
    rules.add(createBoldRule(boldStyleProvider))
    rules.add(createUnderlineRule(underlineStyleProvider))
    rules.add(createItalicsRule(italicsStyleProvider))
    rules.add(createStrikethroughRule(strikethroughStyleProvider))
    if (includeTextRule) {
      rules.add(createTextRule())
    }
    return rules
  }

  @JvmStatic
  fun <RC, S> createCoreStyleRule(pattern: Pattern, styleFactory: () -> List<Any>) =
      object : Rule<RC, Node<RC>, S>(pattern) {
        override fun parse(matcher: Matcher, parser: Parser<RC, in Node<RC>, S>, state: S): ParseSpec<RC, S> {
          val node = StyleNode<RC>(styleFactory())
          return ParseSpec.createNonterminal(node, state, matcher.start(1), matcher.end(1))
        }
      }
}

