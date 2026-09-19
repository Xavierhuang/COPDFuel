package com.copdhealthtracker.ui.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.copdhealthtracker.R

/**
 * A view that allows the user to draw a signature with their finger or stylus.
 */
class SignaturePad @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val strokePaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.colorPrimary)
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 4f
    }

    private val paths = mutableListOf<Path>()
    private var currentPath = Path()
    private var lastX = 0f
    private var lastY = 0f
    private var hasDrawn = false

    var strokeWidth: Float
        get() = strokePaint.strokeWidth
        set(value) { strokePaint.strokeWidth = value }

    var strokeColor: Int
        get() = strokePaint.color
        set(value) { strokePaint.color = value }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (p in paths) {
            canvas.drawPath(p, strokePaint)
        }
        canvas.drawPath(currentPath, strokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                hasDrawn = true
                currentPath.moveTo(x, y)
                lastX = x
                lastY = y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath.quadTo(lastX, lastY, (lastX + x) / 2f, (lastY + y) / 2f)
                lastX = x
                lastY = y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentPath.lineTo(x, y)
                paths.add(Path(currentPath))
                currentPath.reset()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun isEmpty(): Boolean = !hasDrawn

    fun clear() {
        paths.clear()
        currentPath.reset()
        hasDrawn = false
        invalidate()
    }

    /**
     * Returns the signature as a bitmap, or null if empty.
     */
    fun getSignatureBitmap(): Bitmap? {
        if (isEmpty()) return null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        for (p in paths) {
            canvas.drawPath(p, strokePaint)
        }
        canvas.drawPath(currentPath, strokePaint)
        return bitmap
    }
}
