package com.materii.simplerast.core.text

interface RichTextBuilder : CharSequence {

    fun setStyle(style: Any, start: Int, end: Int)

    fun append(text: CharSequence)

    fun append(text: Char)

    fun insert(where: Int, text: CharSequence)

    fun getChars(start: Int, end: Int, destination: CharArray, offset: Int)

}