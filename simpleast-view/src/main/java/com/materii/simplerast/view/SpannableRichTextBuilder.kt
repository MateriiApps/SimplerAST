package com.materii.simplerast.view

import android.text.SpannableStringBuilder
import android.text.Spanned
import com.materii.simplerast.core.text.RichTextBuilder

class SpannableRichTextBuilder : RichTextBuilder {

    internal val builder = SpannableStringBuilder()

    override fun append(text: CharSequence) {
        builder.append(text)
    }

    override fun append(text: Char) {
        builder.append(text)
    }

    override fun insert(where: Int, text: CharSequence) {
        builder.insert(where, text)
    }

    override fun setStyle(style: Any, start: Int, end: Int) {
        builder.setSpan(style, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    override fun getChars(start: Int, end: Int, destination: CharArray, offset: Int) {
        return builder.getChars(start, end, destination, offset)
    }

    override val length: Int
        get() = builder.length

    override fun get(index: Int): Char {
        return builder[index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return builder.subSequence(startIndex, endIndex)
    }


}