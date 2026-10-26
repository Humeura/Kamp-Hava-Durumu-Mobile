package com.kampplus.hava.ui.forecast

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.kampplus.hava.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private const val TAG = "GeminiAPI"
private const val GEMINI_MODEL = "gemini-3.8-flash"
private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

// Singleton — her çağrıda yeni nesne yaratmaktan kaçınıyoruz
// LLM yanıtları uzun sürebilir; varsayılan 10s timeout yetmiyor
private val httpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .build()
private val gson = Gson()

// İstek için yardımcı veri sınıfları (Gson serileştirmesi)
private data class GeminiRequest(val contents: List<GContent>, val generationConfig: GGenerationConfig)
private data class GGenerationConfig(val maxOutputTokens: Int, val temperature: Double)
private data class GContent(val parts: List<GPart>)
private data class GPart(val text: String)

suspend fun fetchGeminiAdvice(
    cityName: String,
    conditionLabel: String,
    temperatureC: Int
): String? = withContext(Dispatchers.IO) {
    runCatching {
        val prompt = "$cityName şehrinde hava $conditionLabel, $temperatureC°C. " +
            "Bu hava için tek cümlelik, samimi Türkçe bir yorum yaz. Sadece cümleyi yaz."

        val requestBody = gson.toJson(
            GeminiRequest(
                contents = listOf(GContent(listOf(GPart(prompt)))),
                generationConfig = GGenerationConfig(maxOutputTokens = 80, temperature = 0.7)
            )
        ).toRequestBody("application/json".toMediaType())

        val url = "$GEMINI_BASE_URL/$GEMINI_MODEL:generateContent?key=${BuildConfig.GEMINI_API_KEY}"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val responseText = httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code} — $body")
            }
            body
        }

        // Yanıtı ayrıştır: candidates[0].content.parts[0].text
        JSONObject(responseText)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .trim()
    }.onFailure { e ->
        Log.e(TAG, "Gemini API hatası [${e::class.simpleName}]: ${e.message}", e)
    }.getOrNull()
}


@Composable
fun GeminiAiCard(aiText: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(text = "✨", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Gemini'nin Özeti",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Text(
            text = aiText,
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GeminiAiCardPreview() {
    GeminiAiCard(aiText = "Kanka hava yağmurlu gözüküyo bence şemsiye falan al")
}
