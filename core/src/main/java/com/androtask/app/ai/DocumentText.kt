package com.androtask.app.ai

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/** Turns files Gemini can't read directly into plain text. */
object DocumentText {
    /** Extracts paragraph text from a .docx (Office Open XML) file. */
    fun fromDocx(bytes: ByteArray): String {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == "word/document.xml") {
                    val xml = zip.readBytes().decodeToString()
                    return xml
                        .replace(Regex("</w:p>"), "\n")
                        .replace(Regex("<w:tab/>"), "\t")
                        .replace(Regex("<w:br/>"), "\n")
                        .replace(Regex("<[^>]+>"), "")
                        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                        .replace("&quot;", "\"").replace("&apos;", "'")
                        .lines().joinToString("\n") { it.trimEnd() }
                        .replace(Regex("\n{3,}"), "\n\n")
                        .trim()
                }
            }
        }
        throw AiException("That Word file couldn't be read. Try saving it as PDF.", AiException.Kind.BAD_RESPONSE)
    }
}
