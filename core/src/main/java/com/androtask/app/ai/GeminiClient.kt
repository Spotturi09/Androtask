package com.androtask.app.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/** A part of a Gemini prompt: plain text or an inline file (PDF, image). */
sealed interface Part {
    data class Text(val text: String) : Part
    class File(val mimeType: String, val bytes: ByteArray) : Part
}

/** Errors shown to the user, with a plain-language message. */
class AiException(message: String, val kind: Kind, cause: Throwable? = null) : Exception(message, cause) {
    enum class Kind { NO_KEY, BAD_KEY, QUOTA, BLOCKED, NETWORK, BAD_RESPONSE, BAD_MODEL, TOO_LARGE }
}

/**
 * Minimal client for the Gemini API `generateContent` REST endpoint.
 * No SDK dependency, so it runs on Android and in plain JVM tests.
 */
class GeminiClient(
    private val apiKey: String,
    val model: String = DEFAULT_MODEL,
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    private val timeoutMs: Int = 120_000,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Sends [parts] and returns the model's text answer. */
    fun generate(parts: List<Part>, jsonOutput: Boolean = false, temperature: Double = 0.3): String {
        if (apiKey.isBlank()) throw AiException("Add your Gemini API key in AI settings first.", AiException.Kind.NO_KEY)
        val body = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", buildJsonArray {
                        parts.forEach { p ->
                            add(when (p) {
                                is Part.Text -> buildJsonObject { put("text", p.text) }
                                is Part.File -> buildJsonObject {
                                    putJsonObject("inline_data") {
                                        put("mime_type", p.mimeType)
                                        put("data", Base64.getEncoder().encodeToString(p.bytes))
                                    }
                                }
                            })
                        }
                    })
                })
            })
            putJsonObject("generationConfig") {
                put("temperature", temperature)
                if (jsonOutput) put("responseMimeType", "application/json")
            }
        }.toString()

        val (code, text) = request("POST", "$baseUrl/models/$model:generateContent", body)
        if (code !in 200..299) throw httpError(code, text)
        return extractText(text)
    }

    /**
     * Checks internet access, the API key and the model with a tiny request.
     * Returns normally on success, otherwise throws an [AiException] that says what's wrong.
     */
    fun checkConnection() {
        if (apiKey.isBlank()) throw AiException("Enter your Gemini API key first.", AiException.Kind.NO_KEY)
        val (code, text) = request("GET", "$baseUrl/models/$model", null)
        if (code !in 200..299) throw httpError(code, text)
    }

    private fun request(method: String, url: String, body: String?): Pair<Int, String> {
        try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 20_000
                readTimeout = timeoutMs
                setRequestProperty("x-goog-api-key", apiKey.trim())
                if (body != null) {
                    val bytes = body.toByteArray()
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setFixedLengthStreamingMode(bytes.size)
                    outputStream.use { it.write(bytes) }
                }
            }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.use { it.readBytes().decodeToString() }.orEmpty()
            conn.disconnect()
            return code to text
        } catch (e: IOException) {
            throw networkError(e)
        }
    }

    /** Turns a low-level network failure into a message that names the actual problem. */
    private fun networkError(e: IOException): AiException {
        val detail = "${e.javaClass.simpleName}: ${e.message ?: "no details"}"
        val msg = when (e) {
            is java.net.UnknownHostException ->
                "This device can't look up Google's servers, so it seems to be offline. Check Wi-Fi or mobile data " +
                    "(on an emulator, open Chrome to test; a Cold Boot usually fixes it)."
            is java.net.SocketTimeoutException ->
                "Gemini took too long to answer. Try again, or use a smaller file."
            is java.net.ConnectException ->
                "Couldn't connect to Gemini. Check your internet connection or VPN/firewall."
            is javax.net.ssl.SSLException ->
                "Secure connection to Gemini failed. Check the device's date and time, or try another network."
            else -> "Couldn't reach Gemini."
        }
        return AiException("$msg\n\nDetails: $detail", AiException.Kind.NETWORK, e)
    }

    private fun httpError(code: Int, body: String): AiException {
        val msg = runCatching {
            json.parseToJsonElement(body).jsonObject["error"]!!.jsonObject["message"]!!.jsonPrimitive.content
        }.getOrDefault(body.take(200))
        return when {
            code == 400 && msg.contains("API key", true) ->
                AiException("Your Gemini API key was rejected: $msg", AiException.Kind.BAD_KEY)
            code == 403 -> AiException("Gemini refused this API key: $msg", AiException.Kind.BAD_KEY)
            code == 404 -> AiException("Model \"$model\" wasn't found. Change the model name in AI settings.", AiException.Kind.BAD_MODEL)
            code == 413 -> AiException("That file is too large for Gemini. Try a smaller file.", AiException.Kind.TOO_LARGE)
            code == 429 -> AiException("Gemini usage limit reached. Wait a minute and try again.", AiException.Kind.QUOTA)
            code == 401 -> AiException("Your Gemini API key was rejected ($msg). Check it in AI settings.", AiException.Kind.BAD_KEY)
            else -> AiException("Gemini error ($code): $msg", AiException.Kind.BAD_RESPONSE)
        }
    }

    private fun extractText(body: String): String {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: throw AiException("Gemini sent an unreadable answer. Try again.", AiException.Kind.BAD_RESPONSE)
        (root["promptFeedback"] as? JsonObject)?.get("blockReason")?.let {
            throw AiException("Gemini declined this request (${it.jsonPrimitive.content}).", AiException.Kind.BLOCKED)
        }
        val candidate = root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw AiException("Gemini returned no answer. Try again.", AiException.Kind.BAD_RESPONSE)
        val finish = candidate["finishReason"]?.jsonPrimitive?.content
        if (finish == "SAFETY" || finish == "PROHIBITED_CONTENT") {
            throw AiException("Gemini declined this request ($finish).", AiException.Kind.BLOCKED)
        }
        val text = candidate["content"]?.jsonObject?.get("parts")?.jsonArray
            ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.content }?.joinToString("")
        if (text.isNullOrBlank()) throw AiException("Gemini returned an empty answer. Try again.", AiException.Kind.BAD_RESPONSE)
        return text
    }

    companion object {
        /** Auto-updating alias for Google's current Flash model; can be changed in AI settings. */
        const val DEFAULT_MODEL = "gemini-flash-latest"
    }
}
