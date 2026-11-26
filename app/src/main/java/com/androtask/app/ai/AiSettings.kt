package com.androtask.app.ai

import android.content.Context

/**
 * Gemini settings stored in app-private preferences on this device.
 * For a published app, move to Firebase AI Logic so no API key lives on users' devices.
 */
class AiSettings(context: Context) {
    private val prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString("api_key", "").orEmpty()
        set(v) = prefs.edit().putString("api_key", v.trim()).apply()

    var model: String
        get() = prefs.getString("model", null)?.takeIf { it.isNotBlank() } ?: GeminiClient.DEFAULT_MODEL
        set(v) = prefs.edit().putString("model", v.trim()).apply()

    val configured: Boolean get() = apiKey.isNotBlank()

    fun client() = GeminiClient(apiKey, model)
}
