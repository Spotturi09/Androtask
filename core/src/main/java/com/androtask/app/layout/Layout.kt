package com.androtask.app.layout

import com.androtask.app.model.FontChoice

/**
 * A tiny, platform-independent page layout engine.
 *
 * Templates describe a resume as text flowing through one or more [Column]s; the engine
 * wraps text, breaks pages and records plain [DrawOp]s. The Android exporter replays those
 * ops onto a PdfDocument canvas, and JVM unit tests replay them onto an AWT image, so the
 * exact same layout is used for the PDF, the in-app preview, and the tests.
 */

data class TextStyle(
    val size: Float,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val color: Long = 0xFF222222,
    val font: FontChoice = FontChoice.SANS,
) {
    val lineHeight: Float get() = size * 1.32f
}

/** Measures the advance width of a single line of text in points. */
fun interface TextMeasurer {
    fun width(text: String, style: TextStyle): Float
}

sealed interface DrawOp {
    data class Text(val x: Float, val baseline: Float, val text: String, val style: TextStyle) : DrawOp
    data class Rect(val x: Float, val y: Float, val w: Float, val h: Float, val color: Long) : DrawOp
    data class Line(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val width: Float, val color: Long) : DrawOp
    data class Circle(val cx: Float, val cy: Float, val r: Float, val color: Long, val filled: Boolean = true) : DrawOp
    data class Image(val x: Float, val y: Float, val size: Float, val path: String) : DrawOp
}

class Page(val width: Float, val height: Float) {
    val ops = mutableListOf<DrawOp>()
}

class Document(val pages: List<Page>)

enum class Align { LEFT, CENTER, RIGHT }

class DocumentBuilder(
    val measurer: TextMeasurer,
    val pageWidth: Float = A4_WIDTH,
    val pageHeight: Float = A4_HEIGHT,
    private val onNewPage: (page: Page, index: Int) -> Unit = { _, _ -> },
) {
    private val pages = mutableListOf<Page>()

    fun page(index: Int): Page {
        while (pages.size <= index) {
            val p = Page(pageWidth, pageHeight)
            pages += p
            onNewPage(p, pages.lastIndex)
        }
        return pages[index]
    }

    val pageCount: Int get() = pages.size

    fun build(): Document = Document(pages.toList().ifEmpty { listOf(page(0)) })

    /** Wraps [text] into lines no wider than [maxWidth]. Explicit newlines are kept. */
    fun wrap(text: String, style: TextStyle, maxWidth: Float): List<String> {
        val out = mutableListOf<String>()
        for (para in text.split('\n')) {
            val words = para.split(' ').filter { it.isNotEmpty() }
            if (words.isEmpty()) { out += ""; continue }
            var line = ""
            for (word in words) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (measurer.width(candidate, style) <= maxWidth) {
                    line = candidate
                    continue
                }
                if (line.isNotEmpty()) out += line
                // A single word wider than the line: hard-break it.
                var rest = word
                while (measurer.width(rest, style) > maxWidth && rest.length > 1) {
                    var cut = rest.length - 1
                    while (cut > 1 && measurer.width(rest.substring(0, cut), style) > maxWidth) cut--
                    out += rest.substring(0, cut)
                    rest = rest.substring(cut)
                }
                line = rest
            }
            out += line
        }
        return out
    }

    companion object {
        const val A4_WIDTH = 595f
        const val A4_HEIGHT = 842f
    }
}

/**
 * A vertical flow of content at a fixed x/width that continues onto new pages.
 * [top] gives the first usable y on each page (so page 0 can start below a header).
 */
class Column(
    private val doc: DocumentBuilder,
    val x: Float,
    val width: Float,
    private val top: (pageIndex: Int) -> Float,
    private val bottom: Float,
) {
    var pageIndex = 0
        private set
    var y = top(0)
        private set

    private val page: Page get() = doc.page(pageIndex)

    fun newPage() {
        pageIndex++
        y = top(pageIndex)
    }

    /** Starts a new page unless [height] still fits on this one. */
    fun ensure(height: Float) {
        if (y + height > bottom && y > top(pageIndex)) newPage()
    }

    fun space(h: Float) {
        y += h
    }

    fun add(op: DrawOp) {
        page.ops += op
    }

    fun textHeight(text: String, style: TextStyle, indent: Float = 0f): Float =
        doc.wrap(text, style, width - indent).size * style.lineHeight

    fun paragraph(text: String, style: TextStyle, indent: Float = 0f, align: Align = Align.LEFT) {
        if (text.isBlank()) return
        for (line in doc.wrap(text, style, width - indent)) {
            ensure(style.lineHeight)
            drawLine(line, style, indent, align)
            y += style.lineHeight
        }
    }

    private fun drawLine(line: String, style: TextStyle, indent: Float, align: Align) {
        val w = doc.measurer.width(line, style)
        val lx = when (align) {
            Align.LEFT -> x + indent
            Align.CENTER -> x + indent + (width - indent - w) / 2f
            Align.RIGHT -> x + width - w
        }
        add(DrawOp.Text(lx, y + style.size, line, style))
    }

    /** Bullet point with a hanging indent. */
    fun bullet(text: String, style: TextStyle, bulletColor: Long = style.color, indent: Float = 12f) {
        val lines = doc.wrap(text, style, width - indent)
        lines.forEachIndexed { i, line ->
            ensure(style.lineHeight)
            if (i == 0) add(DrawOp.Circle(x + 3.5f, y + style.size * 0.62f, 1.6f, bulletColor))
            drawLine(line, style, indent, Align.LEFT)
            y += style.lineHeight
        }
    }

    /** [left] text wrapping on the left, [right] text right-aligned on the first line. */
    fun leftRight(left: String, leftStyle: TextStyle, right: String, rightStyle: TextStyle) {
        val rightW = if (right.isBlank()) 0f else doc.measurer.width(right, rightStyle) + 10f
        val lines = doc.wrap(left, leftStyle, (width - rightW).coerceAtLeast(width * 0.4f))
        val h = maxOf(leftStyle.lineHeight, rightStyle.lineHeight)
        lines.forEachIndexed { i, line ->
            ensure(h)
            add(DrawOp.Text(x, y + leftStyle.size, line, leftStyle))
            if (i == 0 && right.isNotBlank()) {
                add(DrawOp.Text(x + width - (rightW - 10f), y + leftStyle.size, right, rightStyle))
            }
            y += if (i == 0) h else leftStyle.lineHeight
        }
    }

    fun rule(color: Long, thickness: Float = 0.8f, length: Float = width) {
        add(DrawOp.Line(x, y, x + length, y, thickness, color))
    }
}
