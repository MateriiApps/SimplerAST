package com.discord.simpleast.sample.spans

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import android.text.style.LineBackgroundSpan
import androidx.annotation.ColorInt
import com.materii.simplerast.core.node.Node
import com.materii.simplerast.core.text.RichTextBuilder

/**
 * Creates a block background for code sections.
 */
class BlockBackgroundNode<R>(
    private val inQuote: Boolean, vararg children: Node<R>
): Node.Parent<R>(*children) {

  override fun render(builder: RichTextBuilder, renderContext: R) {
    // Ensure the block we want to append starts on a newline.
    ensureEndsWithNewline(builder)

    val codeStartIndex = builder.length
    super.render(builder, renderContext)
    // BlockBackgroundSpan requires this to function
    ensureEndsWithNewline(builder)

    val fillColor = Color.DKGRAY
    val strokeColor = Color.BLACK
    val backgroundSpan = BlockBackgroundSpan(
        fillColor, strokeColor,
        strokeWidth = 2,
        strokeRadius = 15,
        leftMargin = if (inQuote) 40 else 0
    )
    builder.setStyle(
        backgroundSpan,
        codeStartIndex,
        builder.length,
    )

    // Apply a leading margin to all lines in the block.
    val leadingMarginSpan = LeadingMarginSpan.Standard(15)
    builder.setStyle(
        leadingMarginSpan,
        codeStartIndex,
        builder.length,
    )
  }

  private fun ensureEndsWithNewline(builder: RichTextBuilder) {
    if (builder.isNotEmpty()) {
      val lastChar = CharArray(6)
      builder.getChars(builder.length - 1, builder.length, lastChar, 0)
      if (lastChar[0] != '\n') {
        builder.append('\n')
      }
    }
  }
}

/**
 * Computes the position of the paragraph on the screen and draws the desired background.
 */
class BlockBackgroundSpan(
  @ColorInt fillColor: Int,
  @ColorInt strokeColor: Int,
  strokeWidth: Int,
  strokeRadius: Int,
  val leftMargin: Int
) : LineBackgroundSpan {

  private val fillPaint = Paint().apply {
    this.style = Paint.Style.FILL
    this.color = fillColor
  }

  private val strokePaint = Paint().apply {
    this.style = Paint.Style.STROKE
    this.color = strokeColor
    this.strokeWidth = strokeWidth.toFloat()
    this.isAntiAlias = true
  }

  private val rect = RectF()
  private val radius = strokeRadius.toFloat()

  fun draw(canvas: Canvas) {
    canvas.drawRoundRect(rect, radius, radius, fillPaint)
    canvas.drawRoundRect(rect, radius, radius, strokePaint)
  }

  override fun drawBackground(
    canvas: Canvas,
    paint: Paint,
    left: Int,
    right: Int,
    top: Int,
    baseline: Int,
    bottom: Int,
    text: CharSequence,
    start: Int,
    end: Int,
    lnum: Int
  ) {
    if (text !is Spanned) return

    if (text.getSpanStart(this) == start) {
      rect.left = left.toFloat() + leftMargin
      rect.top = top.toFloat()
    }

    if (text.getSpanEnd(this) == end) {
      rect.right = right.toFloat()
      rect.bottom = bottom.toFloat()
      draw(canvas)
    }
  }
}
