package com.androtask.app.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.androtask.app.layout.DrawOp
import com.androtask.app.layout.TextMeasurer
import com.androtask.app.layout.TextStyle
import com.androtask.app.layout.layoutResume
import com.androtask.app.model.FontChoice
import com.androtask.app.model.Resume
import java.io.File
import kotlin.math.min

/** Turns a [Resume] into a PDF using the shared layout engine. */
object PdfExporter {

    private val typefaces = HashMap<Triple<FontChoice, Boolean, Boolean>, Typeface>()

    private fun typeface(style: TextStyle): Typeface = typefaces.getOrPut(Triple(style.font, style.bold, style.italic)) {
        val family = if (style.font == FontChoice.SERIF) Typeface.SERIF else Typeface.SANS_SERIF
        val s = when {
            style.bold && style.italic -> Typeface.BOLD_ITALIC
            style.bold -> Typeface.BOLD
            style.italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        Typeface.create(family, s)
    }

    private fun textPaint(style: TextStyle) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = typeface(style)
        textSize = style.size
        color = style.color.toInt()
    }

    val measurer = TextMeasurer { text, style -> textPaint(style).measureText(text) }

    /** Writes the resume as a PDF to [out] and returns the page count. */
    fun export(resume: Resume, out: File): Int {
        val layout = layoutResume(resume, measurer)
        val pdf = PdfDocument()
        try {
            layout.pages.forEachIndexed { index, page ->
                val info = PdfDocument.PageInfo.Builder(page.width.toInt(), page.height.toInt(), index + 1).create()
                val p = pdf.startPage(info)
                draw(p.canvas, page.ops)
                pdf.finishPage(p)
            }
            out.parentFile?.mkdirs()
            out.outputStream().use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
        return layout.pages.size
    }

    private fun draw(canvas: Canvas, ops: List<DrawOp>) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        for (op in ops) when (op) {
            is DrawOp.Text -> canvas.drawText(op.text, op.x, op.baseline, textPaint(op.style))
            is DrawOp.Rect -> {
                fill.style = Paint.Style.FILL; fill.color = op.color.toInt()
                canvas.drawRect(op.x, op.y, op.x + op.w, op.y + op.h, fill)
            }
            is DrawOp.Line -> {
                fill.style = Paint.Style.STROKE; fill.strokeWidth = op.width; fill.color = op.color.toInt()
                canvas.drawLine(op.x1, op.y1, op.x2, op.y2, fill)
            }
            is DrawOp.Circle -> {
                fill.style = if (op.filled) Paint.Style.FILL else Paint.Style.STROKE
                fill.strokeWidth = 0.8f; fill.color = op.color.toInt()
                canvas.drawCircle(op.cx, op.cy, op.r, fill)
            }
            is DrawOp.Image -> drawCircularPhoto(canvas, op)
        }
    }

    private fun drawCircularPhoto(canvas: Canvas, op: DrawOp.Image) {
        val bmp = BitmapFactory.decodeFile(op.path) ?: return
        // Centre-crop to a square, then draw as a circle.
        val side = min(bmp.width, bmp.height)
        val square = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
        val shader = BitmapShader(square, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val scale = op.size / side
        shader.setLocalMatrix(android.graphics.Matrix().apply { setScale(scale, scale); postTranslate(op.x, op.y) })
        val r = op.size / 2f
        canvas.drawCircle(op.x + r, op.y + r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
        canvas.drawCircle(op.x + r, op.y + r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.WHITE
        })
    }

    /** Renders each page of a PDF file to a bitmap [widthPx] wide, for the preview screen. */
    fun renderPages(pdf: File, widthPx: Int): List<Bitmap> {
        ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                return (0 until renderer.pageCount).map { i ->
                    renderer.openPage(i).use { page ->
                        val h = (widthPx.toFloat() * page.height / page.width).toInt()
                        val bmp = Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                }
            }
        }
    }
}
