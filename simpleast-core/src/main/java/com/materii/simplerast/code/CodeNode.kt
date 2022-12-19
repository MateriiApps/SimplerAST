package com.materii.simplerast.code

import com.materii.simplerast.core.node.Node
import com.materii.simplerast.core.node.StyleNode
import com.materii.simplerast.core.node.TextNode
import com.materii.simplerast.core.text.RichTextBuilder
import com.materii.simplerast.core.text.StyleInclusion

@Suppress("EqualsOrHashCode")
open class CodeNode<RC>(
    content: Content,
    @Suppress("unused")
    private val language: String?,
    private val stylesProvider: StyleNode.SpanProvider<RC>,
    private inline val richTextFactory: () -> RichTextBuilder
) : TextNode<RC>(content.body) {

    init {
        if (content is Content.Parsed<*>) {
            @Suppress("UNCHECKED_CAST")
            content.children.forEach { addChild(it as Node<RC>) }
        }
    }

    sealed class Content(val body: String) {
        class Raw(body: String) : Content(body)
        class Parsed<RC>(raw: String, val children: List<Node<RC>>) : Content(raw)
    }

    override fun render(builder: RichTextBuilder, renderContext: RC) {
        val styles = stylesProvider.get(renderContext)

        if (hasChildren()) {
            // In order to apply the styling from this parent node we need to do some span-fu, and
            // buffer the parsed results into `codeSpan` and then re-insert. This sets the spans from the
            // parent with the proper priority.
            val codeSpan = richTextFactory()
            styles.forEach {
                codeSpan.setStyle(it, 0, 0, StyleInclusion.InclusiveInclusive)
            }
            // First render all child nodes, as these are the nodes we want to apply the styles to.
            getChildren()?.forEach { it.render(codeSpan, renderContext) }
            builder.append('\u200A')  // HACK: use space to terminate span
            builder.insert(builder.length - 1, codeSpan)
        } else {
            val startIndex = builder.length
            builder.append(content)
            styles.forEach {
                builder.setStyle(it, startIndex, builder.length, StyleInclusion.InclusiveExclusive)
            }
        }
    }

    override fun equals(other: Any?): Boolean = other is CodeNode<*> &&
        other.language == this.language &&
        other.content == this.content

    class DefinitionNode<RC>(
        pre: String, name: String,
        codeStyleProviders: CodeStyleProviders<RC>
    ) : Parent<RC>(
        StyleNode.TextStyledNode(pre, codeStyleProviders.keywordStyleProvider),
        StyleNode.TextStyledNode(name, codeStyleProviders.typesStyleProvider)
    )
}

