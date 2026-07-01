package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    suspend fun analyzePothole(bitmap: Bitmap): GeminiAnalysisResult? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured. Falling back to local/sensor-based analysis.")
            return@withContext null
        }

        try {
            val base64Image = bitmap.toBase64()

            // Build payload
            val prompt = """
                Analyze the provided road surface image. Identify the primary pothole or road damage in the frame. Estimate:
                - widthCm (Double, typical 15-120)
                - lengthCm (Double, typical 15-120)
                - depthCm (Double, typical 2-25)
                - severity (String: "Low", "Medium", "High", "Critical")
                - confidence (Float, 0.0 to 1.0)
                - details (String, 1-sentence description of the road surface issue)

                Return JSON format ONLY. Do not write markdown blocks or backticks. Example:
                {
                  "widthCm": 45.0,
                  "lengthCm": 50.0,
                  "depthCm": 8.0,
                  "severity": "High",
                  "confidence": 0.88,
                  "details": "Deep asphalt pothole posing risk to vehicles"
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObject = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val promptPart = JSONObject().apply {
                                put("text", prompt)
                            }
                            val imagePart = JSONObject().apply {
                                val inlineDataObject = JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                }
                                put("inlineData", inlineDataObject)
                            }
                            put(promptPart)
                            put(imagePart)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObject)
                }
                put("contents", contentsArray)

                // Enforce JSON format
                val generationConfig = JSONObject().apply {
                    val responseFormat = JSONObject().apply {
                        put("responseMimeType", "application/json")
                    }
                    put("responseFormat", responseFormat)
                    put("temperature", 0.2)
                }
                put("generationConfig", generationConfig)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Request failed with code: ${response.code}")
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            Log.d(TAG, "Gemini Raw Response: $responseBody")

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates") ?: return@withContext null
            val firstCandidate = candidates.optJSONObject(0) ?: return@withContext null
            val content = firstCandidate.optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null
            val firstPart = parts.optJSONObject(0) ?: return@withContext null
            val textResult = firstPart.optString("text") ?: return@withContext null

            val resultJson = JSONObject(textResult.trim())
            return@withContext GeminiAnalysisResult(
                widthCm = resultJson.optDouble("widthCm", 30.0),
                lengthCm = resultJson.optDouble("lengthCm", 35.0),
                depthCm = resultJson.optDouble("depthCm", 4.5),
                severity = resultJson.optString("severity", "Medium"),
                confidence = resultJson.optDouble("confidence", 0.75).toFloat(),
                details = resultJson.optString("details", "Pothole detected on roadway surface.")
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API: ${e.message}", e)
            return@withContext null
        }
    }
}

data class GeminiAnalysisResult(
    val widthCm: Double,
    val lengthCm: Double,
    val depthCm: Double,
    val severity: String,
    val confidence: Float,
    val details: String
)
