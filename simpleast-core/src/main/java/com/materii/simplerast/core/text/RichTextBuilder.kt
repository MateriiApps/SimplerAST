package com.materii.simplerast.core.text

enum class StyleInclusion {
    InclusiveInclusive,
    InclusiveExclusive,
    ExclusiveInclusive,
    ExclusiveExclusive
}

interface RichTextBuilder : CharSequence {

    fun setStyle(style: Any, start: Int, end: Int, inclusion: StyleInclusion)

    fun append(text: CharSequence)

    fun append(text: Char)

    fun insert(where: Int, text: CharSequence)

    fun getChars(start: Int, end: Int, destination: CharArray, offset: Int)

}