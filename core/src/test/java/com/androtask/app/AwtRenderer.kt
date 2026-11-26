package com.androtask.app

import com.androtask.app.layout.Document
import com.androtask.app.layout.DrawOp
import com.androtask.app.layout.TextMeasurer
import com.androtask.app.layout.TextStyle
import com.androtask.app.model.FontChoice
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** JVM stand-in for the Android PDF canvas, used to test and eyeball layouts. */
object AwtRenderer {
    init { System.setProperty("java.awt.headless", "true") }

    private val frc = FontRenderContext(null, true, true)

    fun font(s: TextStyle): Font {
        val family = if (s.font == FontChoice.SERIF) Font.SERIF else Font.SANS_SERIF
        val style = (if (s.bold) Font.BOLD else 0) or (if (s.italic) Font.ITALIC else 0)
        return Font(family, style, 1).deriveFont(s.size)
    }

    val measurer = TextMeasurer { text, style -> font(style).getStringBounds(text, frc).width.toFloat() }

    private fun color(c: Long) = Color(c.toInt(), true)

    fun renderToPng(doc: Document, out: File, scale: Float = 1.5f) {
        out.parentFile.mkdirs()
        val gap = 20
        val w = (doc.pages.first().width * scale).toInt()
        val h = (doc.pages.first().height * scale).toInt()
        val img = BufferedImage(w * doc.pages.size + gap * (doc.pages.size - 1), h, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = Color(0xDDDDDD); g.fillRect(0, 0, img.width, img.height)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        doc.pages.forEachIndexed { i, page ->
            val pg = g.create() as java.awt.Graphics2D
            pg.translate(i * (w + gap), 0); pg.scale(scale.toDouble(), scale.toDouble())
            pg.color = Color.WHITE; pg.fillRect(0, 0, page.width.toInt(), page.height.toInt())
            for (op in page.ops) when (op) {
                is DrawOp.Text -> { pg.font = font(op.style); pg.color = color(op.style.color); pg.drawString(op.text, op.x, op.baseline) }
                is DrawOp.Rect -> { pg.color = color(op.color); pg.fill(java.awt.geom.Rectangle2D.Float(op.x, op.y, op.w, op.h)) }
                is DrawOp.Line -> { pg.color = color(op.color); pg.stroke = BasicStroke(op.width); pg.draw(java.awt.geom.Line2D.Float(op.x1, op.y1, op.x2, op.y2)) }
                is DrawOp.Circle -> { pg.color = color(op.color); val e = Ellipse2D.Float(op.cx - op.r, op.cy - op.r, op.r * 2, op.r * 2); if (op.filled) pg.fill(e) else pg.draw(e) }
                is DrawOp.Image -> { pg.color = Color(0x9AA5B1); pg.fill(Ellipse2D.Float(op.x, op.y, op.size, op.size)) }
            }
            pg.dispose()
        }
        g.dispose()
        ImageIO.write(img, "png", out)
    }
}
