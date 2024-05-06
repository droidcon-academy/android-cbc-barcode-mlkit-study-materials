package com.droidcon.scandroid

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable

class QrCodeHighlightDrawable(
	private val rect: Rect
) : Drawable() {

	private val paint = Paint().apply {
		style = Paint.Style.STROKE
		color = Color.WHITE
		strokeWidth = 20F
	}

	override fun draw(canvas: Canvas) {
		canvas.drawRect(rect, paint)
	}

	override fun setAlpha(alpha: Int) {
		paint.alpha = alpha
	}

	override fun setColorFilter(colorFilter: ColorFilter?) {
		paint.colorFilter = colorFilter
	}

	@Deprecated("Deprecated in Java")
	override fun getOpacity(): Int = PixelFormat.OPAQUE
}