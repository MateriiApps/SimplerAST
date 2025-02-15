package com.materii.simplerast.code

import com.materii.simplerast.core.node.Node
import com.materii.simplerast.core.node.StyleNode
import com.materii.simplerast.core.node.TextNode
import com.materii.simplerast.core.parser.ParseSpec
import com.materii.simplerast.core.parser.Parser
import com.materii.simplerast.core.parser.Rule
import com.materii.simplerast.core.simple.CoreMarkdownRules
import com.materii.simplerast.core.text.RichTextBuilder
import com.materii.simplerast.core.text.StyleInclusion
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Support for full markdown representations.
 *
 * @see com.discord.simpleast.core.simple.CoreMarkdownRules
 */
@Suppress("MemberVisibilityCanBePrivate")
object CodeRules {
    /**
     * Handles markdown syntax for code blocks for a given language.
     *
     * Examples:
     * inlined ```test```
     * inlined ```kt language code blocks need newline```
     * inlined block start ```
     * fun test()
     * ```
     *
     * ```kotlin
     * class Test: Runnable {
     *   override fun run() {
     *     val x = new BigInt(5)
     *   }
     * }
     * ```
     */
    val PATTERN_CODE_BLOCK: Pattern =
        Pattern.compile("""^```(?:([\w+\-.]+?)?(\s*\n))?([^\n].*?)\n*```""", Pattern.DOTALL)

    val PATTERN_CODE_INLINE: Pattern =
        Pattern.compile("""^(``?)([^`]*)\1""", Pattern.DOTALL)

    private const val CODE_BLOCK_LANGUAGE_GROUP = 1
    private const val CODE_BLOCK_WS_PREFIX = 2
    private const val CODE_BLOCK_BODY_GROUP = 3

    /**
     * This is needed to simplify the other rule parsers to only need a leading pattern match.
     * We also don't want to remove extraneous newlines/ws like what the [CoreMarkdownRules.createNewlineRule] does.
     */
    val PATTERN_LEADING_WS_CONSUMER: Pattern = Pattern.compile("""^(?:\n\s*)+""")

    /**
     * This is needed to simplify the other rule parsers to only need a leading pattern match.
     * The pattern splits on each token (symbol/word) unlike [CoreMarkdownRules.createTextRule] which merges
     * symbols and words until another symbol is reached.
     */
    val PATTERN_TEXT: Pattern =
        Pattern.compile("""^[\s\S]+?(?=\b|[^0-9A-Za-z\s\u00c0-\uffff]|\n| {2,}\n|\w+:\S|$)""")

    val PATTERN_NUMBERS: Pattern = Pattern.compile("""^\b\d+?\b""")

    internal fun createWordPattern(vararg words: String) =
        Pattern.compile("""^\b(?:${words.joinToString("|")})\b""")

    fun <RC, S> Pattern.toMatchGroupRule(
        group: Int = 0,
        stylesProvider: StyleNode.SpanProvider<RC>? = null
    ) =
        object : Rule<RC, Node<RC>, S>(this) {
            override fun parse(
                matcher: Matcher, parser: Parser<RC, in Node<RC>, S>, state: S
            ): ParseSpec<RC, S> {
                val content = matcher.group(group).orEmpty()
                val node = stylesProvider
                    ?.let { StyleNode.TextStyledNode<RC>(content, it) }
                    ?: TextNode(content)
                return ParseSpec.createTerminal(node, state)
            }
        }

    fun <RC, S> createDefinitionRule(
        codeStyleProviders: CodeStyleProviders<RC>, vararg identifiers: String
    ) =
        object :
            Rule<RC, Node<RC>, S>(Pattern.compile("""^\b(${identifiers.joinToString("|")})(\s+\w+)""")) {
            override fun parse(
                matcher: Matcher,
                parser: Parser<RC, in Node<RC>, S>,
                state: S
            ): ParseSpec<RC, S> {
                val definition = matcher.group(1)!!
                val signature = matcher.group(2)!!
                return ParseSpec.createTerminal(
                    CodeNode.DefinitionNode(definition, signature, codeStyleProviders),
                    state
                )
            }
        }

    fun <RC, S> createCodeLanguageMap(codeStyleProviders: CodeStyleProviders<RC>)
        : Map<String, List<Rule<RC, Node<RC>, S>>> {

        val kotlinRules = createGenericCodeRules<RC, S>(
            codeStyleProviders,
            additionalRules = Kotlin.createKotlinCodeRules(codeStyleProviders),
            definitions = arrayOf("object", "class", "interface"),
            builtIns = Kotlin.BUILT_INS,
            keywords = Kotlin.KEYWORDS
        )

        val protoRules = createGenericCodeRules<RC, S>(
            codeStyleProviders,
            additionalRules = listOf(
                createSingleLineCommentPattern("//")
                    .toMatchGroupRule(stylesProvider = codeStyleProviders.commentStyleProvider),
                Pattern.compile("""^"[\s\S]*?(?<!\\)"(?=\W|\s|$)""")
                    .toMatchGroupRule(stylesProvider = codeStyleProviders.literalStyleProvider),
            ),
            definitions = arrayOf("message|enum|extend|service"),
            builtIns = arrayOf(
                "true|false",
                "string|bool|double|float|bytes",
                "int32|uint32|sint32|int64|unit64|sint64",
                "map"
            ),
            keywords = arrayOf(
                "required|repeated|optional|option|oneof|default|reserved",
                "package|import",
                "rpc|returns"
            )
        )

        val pythonRules = createGenericCodeRules<RC, S>(
            codeStyleProviders,
            additionalRules = listOf(
                createSingleLineCommentPattern("#")
                    .toMatchGroupRule(stylesProvider = codeStyleProviders.commentStyleProvider),
                Pattern.compile("""^"[\s\S]*?(?<!\\)"(?=\W|\s|$)""")
                    .toMatchGroupRule(stylesProvider = codeStyleProviders.literalStyleProvider),
                Pattern.compile("""^'[\s\S]*?(?<!\\)'(?=\W|\s|$)""")
                    .toMatchGroupRule(stylesProvider = codeStyleProviders.literalStyleProvider),
                Pattern.compile("""^@(\w+)""")
                    .toMatchGroupRule(stylesProvider = codeStyleProviders.genericsStyleProvider)
            ),
            definitions = arrayOf("class", "def", "lambda"),
            builtIns = arrayOf("True|False|None"),
            keywords = arrayOf(
                "from|import|global|nonlocal",
                "async|await|class|self|cls|def|lambda",
                "for|while|if|else|elif|break|continue|return",
                "try|except|finally|raise|pass|yeild",
                "in|as|is|del",
                "and|or|not|assert"
            )
        )

        val rustRules = createGenericCodeRules<RC, S>(
            codeStyleProviders,
            additionalRules = listOf(
                createSingleLineCommentPattern("//")
                    .toMatchGroupRule(stylesProvider = codeStyleProviders.commentStyleProvider),
                Pattern.compile("""^"[\s\S]*?(?<!\\)"(?=\W|\s|$)""")
                    .toMatchGroupRule(stylesProvider = codeStyleProviders.literalStyleProvider),
                Pattern.compile("""^#!?\[.*?\]\n""")
                    .toMatchGroupRule(stylesProvider = codeStyleProviders.genericsStyleProvider)
            ),
            definitions = arrayOf("struct", "trait", "mod"),
            builtIns = arrayOf(
                "Self|Result|Ok|Err|Option|None|Some",
                "Copy|Clone|Eq|Hash|Send|Sync|Sized|Debug|Display",
                "Arc|Rc|Box|Pin|Future",
                "true|false|bool|usize|i64|u64|u32|i32|str|String"
            ),
            keywords = arrayOf(
                "let|mut|static|const|unsafe",
                "crate|mod|extern|pub|pub(super)|use",
                "struct|enum|trait|type|where|impl|dyn|async|await|move|self|fn",
                "for|while|loop|if|else|match|break|continue|return|try",
                "in|as|ref"
            )
        )

        val xmlRules = listOf<Rule<RC, Node<RC>, S>>(
            Xml.PATTERN_XML_COMMENT
                .toMatchGroupRule(stylesProvider = codeStyleProviders.commentStyleProvider),
            Xml.createTagRule(codeStyleProviders),
            PATTERN_LEADING_WS_CONSUMER.toMatchGroupRule(),
            PATTERN_TEXT.toMatchGroupRule(),
        )

        val queryLangauge = listOf<Rule<RC, Node<RC>, S>>(
            createSingleLineCommentPattern("#")
                .toMatchGroupRule(stylesProvider = codeStyleProviders.commentStyleProvider),
            Pattern.compile("""^"[\s\S]*?(?<!\\)"(?=\W|\s|$)""")
                .toMatchGroupRule(stylesProvider = codeStyleProviders.literalStyleProvider),
            createWordPattern("true|false|null").pattern().toPattern(Pattern.CASE_INSENSITIVE)
                .toMatchGroupRule(stylesProvider = codeStyleProviders.genericsStyleProvider),
            createWordPattern(
                "select|from|join|where|and|as|distinct|count|avg",
                "order by|group by|desc|sum|min|max",
                "like|having|in|is|not"
            ).pattern().toPattern(Pattern.CASE_INSENSITIVE)
                .toMatchGroupRule(stylesProvider = codeStyleProviders.keywordStyleProvider),
            PATTERN_NUMBERS.toMatchGroupRule(stylesProvider = codeStyleProviders.literalStyleProvider),
            PATTERN_LEADING_WS_CONSUMER.toMatchGroupRule(),
            PATTERN_TEXT.toMatchGroupRule(),
        )

        val crystalRules = createGenericCodeRules<RC, S>(
            codeStyleProviders,
            additionalRules = Crystal.createCrystalCodeRules(codeStyleProviders),
            definitions = arrayOf("def", "class"),
            builtIns = Crystal.BUILT_INS,
            keywords = Crystal.KEYWORDS
        )

        val javascriptRules = createGenericCodeRules<RC, S>(
            codeStyleProviders,
            additionalRules = JavaScript.createCodeRules(codeStyleProviders),
            definitions = arrayOf("class"),
            builtIns = JavaScript.BUILT_INS,
            keywords = JavaScript.KEYWORDS
        )

        val typescriptRules = createGenericCodeRules<RC, S>(
            codeStyleProviders,
            additionalRules = TypeScript.createCodeRules(codeStyleProviders),
            definitions = arrayOf(
                "class", "interface", "enum",
                "namespace", "module", "type"
            ),
            builtIns = TypeScript.BUILT_INS,
            keywords = TypeScript.KEYWORDS,
            types = TypeScript.TYPES
        )

        val diffRules = listOf<Rule<RC, Node<RC>, S>>(
            Pattern.compile("""^-.*""")
                .toMatchGroupRule(stylesProvider = codeStyleProviders.keywordStyleProvider),
            Pattern.compile("""^\+.*""")
                .toMatchGroupRule(stylesProvider = codeStyleProviders.typesStyleProvider),
            PATTERN_LEADING_WS_CONSUMER.toMatchGroupRule(),
            PATTERN_TEXT.toMatchGroupRule()
        )

        return mapOf(
            "kt" to kotlinRules,
            "kotlin" to kotlinRules,

            "protobuf" to protoRules,
            "proto" to protoRules,
            "pb" to protoRules,

            "py" to pythonRules,
            "python" to pythonRules,

            "rs" to rustRules,
            "rust" to rustRules,

            "cql" to queryLangauge,
            "sql" to queryLangauge,

            "xml" to xmlRules,
            "http" to xmlRules,

            "cr" to crystalRules,
            "crystal" to crystalRules,

            "js" to javascriptRules,
            "javascript" to javascriptRules,

            "ts" to typescriptRules,
            "typescript" to typescriptRules,

            "diff" to diffRules
        )
    }

    private fun createSingleLineCommentPattern(prefix: String) =
        Pattern.compile("""^(?:$prefix.*?(?=\n|$))""")

    private fun <RC, S> createGenericCodeRules(
        codeStyleProviders: CodeStyleProviders<RC>,
        additionalRules: List<Rule<RC, Node<RC>, S>>,
        definitions: Array<String>,
        builtIns: Array<String>,
        keywords: Array<String>,
        types: Array<String> = arrayOf(" ")
    ): List<Rule<RC, Node<RC>, S>> =
        additionalRules +
            listOf(
                createDefinitionRule(codeStyleProviders, *definitions),
                createWordPattern(*builtIns).toMatchGroupRule(stylesProvider = codeStyleProviders.genericsStyleProvider),
                createWordPattern(*keywords).toMatchGroupRule(stylesProvider = codeStyleProviders.keywordStyleProvider),
                createWordPattern(*types).toMatchGroupRule(stylesProvider = codeStyleProviders.typesStyleProvider),
                PATTERN_NUMBERS.toMatchGroupRule(stylesProvider = codeStyleProviders.literalStyleProvider),
                PATTERN_LEADING_WS_CONSUMER.toMatchGroupRule(),
                PATTERN_TEXT.toMatchGroupRule(),
            )

    /**
     * @param textStyleProvider appearance of the text inside the block
     * @param languageMap maps language identifer to a list of rules to parse the syntax
     * @param wrapperNodeProvider set if you want to provide additional styling on the code representation.
     *    Useful for setting code blocks backgrounds.
     */
    fun <RC, S> createCodeRule(
        textStyleProvider: StyleNode.SpanProvider<RC>,
        languageMap: Map<String, List<Rule<RC, Node<RC>, S>>>,
        wrapperNodeProvider: (CodeNode<RC>, Boolean, S) -> Node<RC> =
            @Suppress("UNUSED_ANONYMOUS_PARAMETER")
            { codeNode, startsWithNewline, state -> codeNode },
        richTextFactory: () -> RichTextBuilder
    ): Rule<RC, Node<RC>, S> {
        return object : Rule<RC, Node<RC>, S>(PATTERN_CODE_BLOCK) {
            override fun parse(matcher: Matcher, parser: Parser<RC, in Node<RC>, S>, state: S)
                : ParseSpec<RC, S> {
                val language = matcher.group(CODE_BLOCK_LANGUAGE_GROUP)
                val codeBody = matcher.group(CODE_BLOCK_BODY_GROUP).orEmpty()
                val startsWithNewline = matcher.group(CODE_BLOCK_WS_PREFIX)?.contains('\n') ?: false

                val languageRules = language?.let { languageMap[it] }

                val content = languageRules?.let {
                    @Suppress("UNCHECKED_CAST")
                    val children = parser.parse(codeBody, state, languageRules) as List<Node<RC>>
                    CodeNode.Content.Parsed(codeBody, children)
                } ?: CodeNode.Content.Raw(codeBody)

                val codeNode = CodeNode(content, language, textStyleProvider, richTextFactory)
                return ParseSpec.createTerminal(
                    wrapperNodeProvider(
                        codeNode,
                        startsWithNewline,
                        state
                    ), state
                )
            }
        }
    }

    fun <RC, S> createInlineCodeRule(
        textStyleProvider: StyleNode.SpanProvider<RC>,
        bgStyleProvider: StyleNode.SpanProvider<RC>,
        richTextFactory: () -> RichTextBuilder
    ): Rule<RC, Node<RC>, S> {
        return object : Rule<RC, Node<RC>, S>(PATTERN_CODE_INLINE) {
            override fun parse(matcher: Matcher, parser: Parser<RC, in Node<RC>, S>, state: S)
                : ParseSpec<RC, S> {
                val codeBody = matcher.group(2).orEmpty()
                if (codeBody.isEmpty()) {
                    return ParseSpec.createTerminal(TextNode(matcher.group()), state)
                }

                val content = CodeNode.Content.Raw(codeBody)

                val codeNode = CodeNode(content, null, textStyleProvider, richTextFactory)
                // We can't use a StyleNode here as we can't share background spans.
                val node = object : Node.Parent<RC>(codeNode) {
                    override fun render(builder: RichTextBuilder, renderContext: RC) {
                        val startIndex = builder.length
                        super.render(builder, renderContext)
                        bgStyleProvider.get(renderContext).forEach {
                            builder.setStyle(it, startIndex, builder.length, StyleInclusion.ExclusiveExclusive)
                        }
                    }
                }
                return ParseSpec.createTerminal(node, state)
            }
        }
    }
}
