package com.frombeyond.r2sl.ui.recipes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import android.widget.ImageView
import com.frombeyond.r2sl.data.local.RecipeImageStorageManager
import kotlin.math.max
import kotlin.math.min

/**
 * * Interactive crop editor: fixed 4:3 frame with inner 600px guide bands on an 800x600 export.
 */
class RecipeCropEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val imageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.MATRIX
    }
    private val overlayView = CropOverlayView(context)

    private var bitmap: Bitmap? = null
    private val imageMatrix = Matrix()
    private val cropRect = RectF()
    private var minScale = 1f

    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())

    init {
        addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(overlayView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        overlayView.isClickable = false
    }

    fun setImageBitmap(source: Bitmap) {
        bitmap = source
        imageView.setImageBitmap(source)
        post { resetTransform() }
    }

    fun exportCroppedBitmap(): Bitmap? {
        val source = bitmap ?: return null
        val output = Bitmap.createBitmap(
            RecipeImageStorageManager.OUTPUT_WIDTH,
            RecipeImageStorageManager.OUTPUT_HEIGHT,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val exportMatrix = Matrix(imageMatrix)
        val scaleX = RecipeImageStorageManager.OUTPUT_WIDTH / cropRect.width()
        val scaleY = RecipeImageStorageManager.OUTPUT_HEIGHT / cropRect.height()
        exportMatrix.postTranslate(-cropRect.left, -cropRect.top)
        exportMatrix.postScale(scaleX, scaleY)
        canvas.drawBitmap(source, exportMatrix, null)
        return output
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateCropRect()
        resetTransform()
    }

    private fun updateCropRect() {
        if (width == 0 || height == 0) return
        val targetRatio = RecipeImageStorageManager.OUTPUT_WIDTH.toFloat() /
            RecipeImageStorageManager.OUTPUT_HEIGHT
        var cropWidth = width.toFloat()
        var cropHeight = cropWidth / targetRatio
        if (cropHeight > height) {
            cropHeight = height.toFloat()
            cropWidth = cropHeight * targetRatio
        }
        val left = (width - cropWidth) / 2f
        val top = (height - cropHeight) / 2f
        cropRect.set(left, top, left + cropWidth, top + cropHeight)
        overlayView.cropRect = RectF(cropRect)
        overlayView.invalidate()
    }

    private fun resetTransform() {
        val source = bitmap ?: return
        if (cropRect.width() <= 0f || cropRect.height() <= 0f) return
        imageMatrix.reset()
        val scale = max(
            cropRect.width() / source.width,
            cropRect.height() / source.height
        )
        minScale = scale
        imageMatrix.postScale(scale, scale)
        val dx = cropRect.centerX() - (source.width * scale) / 2f
        val dy = cropRect.centerY() - (source.height * scale) / 2f
        imageMatrix.postTranslate(dx, dy)
        applyMatrix()
    }

    private fun applyMatrix() {
        constrainMatrix()
        imageView.imageMatrix = imageMatrix
    }

    private fun constrainMatrix() {
        val source = bitmap ?: return
        val drawableRect = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
        imageMatrix.mapRect(drawableRect)

        var dx = 0f
        var dy = 0f
        if (drawableRect.width() < cropRect.width()) {
            dx = cropRect.centerX() - drawableRect.centerX()
        } else {
            if (drawableRect.left > cropRect.left) {
                dx = cropRect.left - drawableRect.left
            } else if (drawableRect.right < cropRect.right) {
                dx = cropRect.right - drawableRect.right
            }
        }
        if (drawableRect.height() < cropRect.height()) {
            dy = cropRect.centerY() - drawableRect.centerY()
        } else {
            if (drawableRect.top > cropRect.top) {
                dy = cropRect.top - drawableRect.top
            } else if (drawableRect.bottom < cropRect.bottom) {
                dy = cropRect.bottom - drawableRect.bottom
            }
        }
        if (dx != 0f || dy != 0f) {
            imageMatrix.postTranslate(dx, dy)
        }

        val mapped = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
        imageMatrix.mapRect(mapped)
        val currentScale = mapped.width() / source.width
        if (currentScale < minScale) {
            val factor = minScale / currentScale
            imageMatrix.postScale(factor, factor, cropRect.centerX(), cropRect.centerY())
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            applyMatrix()
        }
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val factor = max(0.5f, min(3f, detector.scaleFactor))
            imageMatrix.postScale(factor, factor, detector.focusX, detector.focusY)
            applyMatrix()
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            imageMatrix.postTranslate(-distanceX, -distanceY)
            applyMatrix()
            return true
        }
    }

    private class CropOverlayView(context: Context) : android.view.View(context) {
        var cropRect: RectF = RectF()
        private val dimPaint = Paint().apply {
            color = Color.parseColor("#99000000")
            style = Paint.Style.FILL
        }
        private val borderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        private val guidePaint = Paint().apply {
            color = Color.parseColor("#CCFFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (cropRect.isEmpty) return

            canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, dimPaint)
            canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), dimPaint)
            canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, dimPaint)
            canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, dimPaint)

            canvas.drawRect(cropRect, borderPaint)

            val bandRatio = RecipeImageStorageManager.INNER_GUIDE_WIDTH.toFloat() /
                RecipeImageStorageManager.OUTPUT_WIDTH
            val innerWidth = cropRect.width() * bandRatio
            val innerLeft = cropRect.centerX() - innerWidth / 2f
            val innerRight = cropRect.centerX() + innerWidth / 2f
            canvas.drawLine(innerLeft, cropRect.top, innerLeft, cropRect.bottom, guidePaint)
            canvas.drawLine(innerRight, cropRect.top, innerRight, cropRect.bottom, guidePaint)
        }
    }
}
