package com.materii.simplerast.code

import com.materii.simplerast.code.CodeRules.toMatchGroupRule
import com.materii.simplerast.core.node.Node
import com.materii.simplerast.core.node.StyleNode
import com.materii.simplerast.core.parser.ParseSpec
import com.materii.simplerast.core.parser.Parser
import com.materii.simplerast.core.parser.Rule
import java.util.regex.Matcher
import java.util.regex.Pattern

object JavaScript {

    val KEYWORDS: Array<String> = arrayOf(
        "import|from|export|default|package",
        "class|enum",
        "function|super|extends|implements|arguments",
        "var|let|const|static|get|set|new",
        "return|break|continue|yield|void",
        "if|else|for|while|do|switch|async|await|case|try|catch|finally|delete|throw|NaN|Infinity",
        "of|in|instanceof|typeof",
        "debugger|with",
        "true|false|null|undefined"
    )

    val BUILT_INS: Array<String> = arrayOf(
        "String|Boolean|RegExp|Number|Date|Math|JSON|Symbol|BigInt|Atomics|DataView",
        "Function|Promise|Generator|GeneratorFunction|AsyncFunction|AsyncGenerator|AsyncGeneratorFunction",
        "Array|Object|Map|Set|WeakMap|WeakSet|Int8Array|Int16Array|Int32Array|Uint8Array|Uint16Array",
        "Uint32Array|Uint8ClampedArray|Float32Array|Float64Array|BigInt64Array|BigUint64Array|Buffer",
        "ArrayBuffer|SharedArrayBuffer",
        "Reflect|Proxy|Intl|WebAssembly",
        "console|process|require|isNaN|parseInt|parseFloat|encodeURI|decodeURI|encodeURIComponent",
        "decodeURIComponent|this|global|globalThis|eval|isFinite|module",
        "setTimeout|setInterval|clearTimeout|clearInterval|setImmediate|clearImmediate",
        "queueMicrotask|document|window",
        "Error|SyntaxError|TypeError|RangeError|ReferenceError|EvalError|InternalError|URIError",
        "AggregateError|escape|unescape|URL|URLSearchParams|TextEncoder|TextDecoder",
        "AbortController|AbortSignal|EventTarget|Event|MessageChannel",
        "MessagePort|MessageEvent|FinalizationRegistry|WeakRef",
        "regeneratorRuntime|performance"
    )

    class FunctionNode<RC>(
        pre: String, signature: String?, params: String,
        codeStyleProviders: CodeStyleProviders<RC>
    ) : Node.Parent<RC>(
        StyleNode.TextStyledNode(pre, codeStyleProviders.keywordStyleProvider),
        signature?.let {
            StyleNode.TextStyledNode(
                signature,
                codeStyleProviders.identifierStyleProvider
            )
        },
        StyleNode.TextStyledNode(params, codeStyleProviders.paramsStyleProvider)
    ) {
        companion object {
            /**
             * Matches against a JavaScript function declaration.
             *
             * ```
             * function foo(bar)
             * function baz()
             * async test()
             * static nice()
             * function* generator()
             * get token()
             * set internals()
             * ```
             */
            private val PATTERN_JAVASCRIPT_FUNC =
                """^(function\*?|static|get|set|async)(\s+[a-zA-Z_$][a-zA-Z0-9_$]*)?(\s*\(.*?\))""".toRegex(
                    RegexOption.DOT_MATCHES_ALL
                ).toPattern()

            fun <RC, S> createFunctionRule(codeStyleProviders: CodeStyleProviders<RC>) =
                object : Rule<RC, Node<RC>, S>(PATTERN_JAVASCRIPT_FUNC) {
                    override fun parse(
                        matcher: Matcher,
                        parser: Parser<RC, in Node<RC>, S>,
                        state: S
                    ): ParseSpec<RC, S> {
                        val pre = matcher.group(1)
                        val signature = matcher.group(2)
                        val params = matcher.group(3)
                        return ParseSpec.createTerminal(
                            FunctionNode(
                                pre!!,
                                signature,
                                params!!,
                                codeStyleProviders
                            ), state
                        )
                    }
                }
        }
    }

    class FieldNode<RC>(
        definition: String, name: String,
        codeStyleProviders: CodeStyleProviders<RC>
    ) : Node.Parent<RC>(
        StyleNode.TextStyledNode(definition, codeStyleProviders.keywordStyleProvider),
        StyleNode.TextStyledNode(name, codeStyleProviders.identifierStyleProvider),
    ) {
        companion object {
            /**
             * Matches against a JavaScript field definition.
             *
             * ```
             * var x = 1;
             * let y = 5;
             * const z = 10;
             * ```
             */
            private val PATTERN_JAVASCRIPT_FIELD =
                Pattern.compile("""^(var|let|const)(\s+[a-zA-Z_$][a-zA-Z0-9_$]*)""")

            fun <RC, S> createFieldRule(
                codeStyleProviders: CodeStyleProviders<RC>
            ) =
                object : Rule<RC, Node<RC>, S>(PATTERN_JAVASCRIPT_FIELD) {
                    override fun parse(
                        matcher: Matcher,
                        parser: Parser<RC, in Node<RC>, S>,
                        state: S
                    ):
                        ParseSpec<RC, S> {
                        val definition = matcher.group(1)
                        val name = matcher.group(2)
                        return ParseSpec.createTerminal(
                            FieldNode(definition!!, name!!, codeStyleProviders), state
                        )
                    }
                }
        }
    }

    class ObjectPropertyNode<RC>(
        prefix: String, property: String, suffix: String,
        codeStyleProviders: CodeStyleProviders<RC>
    ) : Node.Parent<RC>(
        StyleNode.TextStyledNode(prefix, codeStyleProviders.defaultStyleProvider),
        StyleNode.TextStyledNode(property, codeStyleProviders.identifierStyleProvider),
        StyleNode.TextStyledNode(suffix, codeStyleProviders.defaultStyleProvider),
    ) {
        companion object {
            /**
             * Matches against a JavaScript object property.
             *
             * ```
             * { foo: 'bar' }
             * ```
             */
            private val PATTERN_JAVASCRIPT_OBJECT_PROPERTY =
                Pattern.compile("""^([\{\[\,])(\s*[a-zA-Z0-9_$]+)(\s*:)""")

            fun <RC, S> createObjectPropertyRule(
                codeStyleProviders: CodeStyleProviders<RC>
            ) =
                object : Rule<RC, Node<RC>, S>(PATTERN_JAVASCRIPT_OBJECT_PROPERTY) {
                    override fun parse(
                        matcher: Matcher,
                        parser: Parser<RC, in Node<RC>, S>,
                        state: S
                    ):
                        ParseSpec<RC, S> {
                        val prefix = matcher.group(1)
                        val property = matcher.group(2)
                        val suffix = matcher.group(3)
                        return ParseSpec.createTerminal(
                            ObjectPropertyNode(prefix!!, property!!, suffix!!, codeStyleProviders),
                            state
                        )
                    }
                }
        }
    }

    /**
     * Matches against a JavaScript regex.
     *
     * ```
     * /(.*)/
     * ```
     */
    private val PATTERN_JAVASCRIPT_REGEX =
        Pattern.compile("""^/.+(?<!\\)/[dgimsuy]*""")

    /**
     * Matches against a JavaScript generic.
     *
     * ```
     * <pending>
     * ```
     */
    private val PATTERN_JAVASCRIPT_GENERIC =
        Pattern.compile("""^<.*(?<!\\)>""")

    /**
     * Matches against a JavaScript comment.
     *
     * ```
     * // Hey there
     * /* Hello */
     * ```
     */
    private val PATTERN_JAVASCRIPT_COMMENTS =
        Pattern.compile("""^(?:(?://.*?(?=\n|$))|(/\*.*?\*/))""", Pattern.DOTALL)

    /**
     * Matches against a JavaScript string.
     *
     * ```
     * 'Hi'
     * "Hello"
     * `Hey`
     * ```
     */
    private val PATTERN_JAVASCRIPT_STRINGS =
        Pattern.compile("""^('.*?(?<!\\)'|".*?(?<!\\)"|`[\s\S]*?(?<!\\)`)(?=\W|\s|$)""")

    internal fun <RC, S> createCodeRules(
        codeStyleProviders: CodeStyleProviders<RC>
    ): List<Rule<RC, Node<RC>, S>> =
        listOf(
            PATTERN_JAVASCRIPT_COMMENTS.toMatchGroupRule(stylesProvider = codeStyleProviders.commentStyleProvider),
            PATTERN_JAVASCRIPT_STRINGS.toMatchGroupRule(stylesProvider = codeStyleProviders.literalStyleProvider),
            ObjectPropertyNode.createObjectPropertyRule(codeStyleProviders),
            PATTERN_JAVASCRIPT_GENERIC.toMatchGroupRule(stylesProvider = codeStyleProviders.genericsStyleProvider),
            PATTERN_JAVASCRIPT_REGEX.toMatchGroupRule(stylesProvider = codeStyleProviders.literalStyleProvider),
            FieldNode.createFieldRule(codeStyleProviders),
            FunctionNode.createFunctionRule(codeStyleProviders),
        )
}
