package com.androtask.app.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Turns whatever another app shares (LinkedIn/Indeed "Share", a copied ad, a link) into job text.
 */
object JobPosting {
    private val urlRegex = Regex("""https?://[^\s"'<>]+""")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).followRedirects(true).build()

    fun firstUrl(text: String): String? = urlRegex.find(text)?.value?.trimEnd('.', ',', ')')

    /** True when the shared text is basically just a link (plus maybe a title), not the ad itself. */
    fun isMostlyLink(text: String): Boolean {
        val url = firstUrl(text) ?: return false
        return text.replace(url, "").trim().length < 300
    }

    /**
     * Downloads a job page and extracts its description. Prefers schema.org JobPosting data,
     * which most job boards embed, and falls back to the page's visible text.
     */
    fun fetch(url: String): String {
        val html = try {
            http.newCall(
                Request.Builder().url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36")
                    .header("Accept-Language", "en")
                    .build(),
            ).execute().use { r ->
                if (!r.isSuccessful) throw AiException("The job page couldn't be opened (HTTP ${r.code}). Paste the job description instead.", AiException.Kind.NETWORK)
                r.body?.string().orEmpty()
            }
        } catch (e: java.io.IOException) {
            throw AiException("The job page couldn't be downloaded. Paste the job description instead.", AiException.Kind.NETWORK, e)
        } catch (e: IllegalArgumentException) {
            throw AiException("That doesn't look like a valid link. Paste the job description instead.", AiException.Kind.NETWORK, e)
        }
        val text = fromHtml(html)
        if (text.length < 200) {
            throw AiException("Couldn't read the job description from that page (it may need a login). Paste the text instead.", AiException.Kind.BAD_RESPONSE)
        }
        return text
    }

    fun fromHtml(html: String): String {
        jsonLdJob(html)?.let { return it }
        val body = html
            .replace(Regex("(?is)<(script|style|noscript|svg|head|nav|footer)[^>]*>.*?</\\1>"), " ")
            .replace(Regex("(?i)<br\\s*/?>|</p>|</li>|</h\\d>|</div>"), "\n")
            .replace(Regex("<[^>]+>"), " ")
        return decodeEntities(body)
            .lines().map { it.replace(Regex("[ \t ]+"), " ").trim() }.filter { it.isNotEmpty() }
            .joinToString("\n").take(MAX_CHARS)
    }

    private fun jsonLdJob(html: String): String? {
        val blocks = Regex("""(?is)<script[^>]+application/ld\+json[^>]*>(.*?)</script>""").findAll(html).map { it.groupValues[1] }
        for (block in blocks) {
            val root = runCatching { json.parseToJsonElement(block.trim()) }.getOrNull() ?: continue
            val job = findJob(root) ?: continue
            fun str(k: String) = (job[k] as? JsonPrimitive)?.content?.trim().orEmpty()
            val org = (job["hiringOrganization"] as? JsonObject)?.get("name")?.jsonPrimitive?.content.orEmpty()
            val desc = fromHtml(str("description").replace("&lt;", "<").replace("&gt;", ">"))
            if (desc.length < 100) continue
            return buildString {
                if (str("title").isNotEmpty()) appendLine("Job title: ${str("title")}")
                if (org.isNotEmpty()) appendLine("Company: $org")
                appendLine(desc)
            }.take(MAX_CHARS).trim()
        }
        return null
    }

    private fun findJob(e: JsonElement): JsonObject? = when (e) {
        is JsonObject -> {
            val type = e["@type"]
            val isJob = (type as? JsonPrimitive)?.content == "JobPosting" ||
                (type as? JsonArray)?.any { (it as? JsonPrimitive)?.content == "JobPosting" } == true
            if (isJob) e else e["@graph"]?.let { findJob(it) }
        }
        is JsonArray -> e.firstNotNullOfOrNull { findJob(it) }
        else -> null
    }

    private fun decodeEntities(s: String) = s
        .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'").replace("&rsquo;", "’").replace("&bull;", "•")

    const val MAX_CHARS = 12_000
}
