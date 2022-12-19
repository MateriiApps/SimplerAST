package com.materii.simplerast.view

import android.text.SpannableStringBuilder
import android.widget.TextView
import androidx.annotation.StringRes
import com.materii.simplerast.core.node.Node
import com.materii.simplerast.core.parser.Parser
import com.materii.simplerast.core.parser.Rule
import com.materii.simplerast.core.simple.CoreRenderer

object TextViewRenderer {

    @JvmStatic
    fun renderBasicMarkdown(@StringRes sourceResId: Int, textView: TextView) {
        val source = textView.context.getString(sourceResId)
        renderBasicMarkdown(source, textView)
    }

    @JvmStatic
    fun renderBasicMarkdown(source: CharSequence, textView: TextView) {
        textView.text = renderBasicMarkdown(source).builder
    }

    @JvmStatic
    fun renderBasicMarkdown(source: CharSequence): SpannableRichTextBuilder {
        return CoreRenderer.render(
            SpannableRichTextBuilder(),
            source,
            TextViewMarkdown.createMarkdownRules(),
            null,
            null
        )
    }

    @JvmStatic
    fun <RC, S> render(
        source: CharSequence,
        rules: Collection<Rule<RC, Node<RC>, S>>,
        initialState: S,
        renderContext: RC
    ): SpannableRichTextBuilder {
        return CoreRenderer.render(
            SpannableRichTextBuilder(),
            source,
            rules,
            initialState,
            renderContext
        )
    }

    @JvmStatic
    fun <RC, S> render(
        source: CharSequence,
        parser: Parser<RC, Node<RC>, S>,
        initialState: S,
        renderContext: RC
    ): SpannableStringBuilder {
        return CoreRenderer.render(
            SpannableRichTextBuilder(),
            parser.parse(source, initialState),
            renderContext
        ).builder
    }

}