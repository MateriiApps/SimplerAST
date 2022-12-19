package com.materii.simplerast.view

import android.graphics.Typeface
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import com.materii.simplerast.core.node.Node
import com.materii.simplerast.core.parser.Rule
import com.materii.simplerast.core.simple.CoreMarkdownRules

object TextViewMarkdown {

    fun <RC, S> createMarkdownRules(
        includeTextRule: Boolean = true,
        includeEscapeRule: Boolean = true,
    ): List<Rule<RC, Node<RC>, S>> {
        return CoreMarkdownRules.createCoreMarkdownRules(
            includeTextRule = includeTextRule,
            includeEscapeRule = includeEscapeRule,
            boldStyleProvider = { StyleSpan(Typeface.BOLD) },
            italicsStyleProvider = { StyleSpan(Typeface.ITALIC) },
            strikethroughStyleProvider = { StrikethroughSpan() },
            underlineStyleProvider = { UnderlineSpan() }
        )
    }

}