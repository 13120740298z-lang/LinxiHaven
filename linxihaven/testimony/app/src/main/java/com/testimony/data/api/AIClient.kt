package com.testimony.data.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.testimony.ai.PromptTemplates
import com.testimony.data.models.InterviewState
import com.testimony.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences

/**
 * AI 客户端 - 优化版
 * - 添加重试机制
 * - 添加离线缓存
 * - 更好的错误处理
 */
class AIClient(private val context: Context) {
    companion object {
        private const val ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages"
        private const val CLAUDE_MODEL = "claude-sonnet-4-20250514"
        private const val MAX_TOKENS = 1024
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
        private const val CACHE_PREFS_NAME = "ai_cache"
        private const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24小时
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS) // AI 响应可能需要更长时间
        .retryOnConnectionFailure(true)
        .build()

    private val cache: SharedPreferences by lazy {
        context.getSharedPreferences(CACHE_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private var apiKeyProvider: (() -> String?)? = null

    fun setApiKeyProvider(provider: () -> String?) {
        apiKeyProvider = provider
    }

    private fun getApiKey(): String? = apiKeyProvider?.invoke()

    // ========== 主要 API ==========

    /**
     * 生成访谈响应
     * @param state 当前状态
     * @param userInput 用户输入
     * @param sessionContext 会话上下文
     */
    suspend fun generateInterviewResponse(
        state: InterviewState,
        userInput: String,
        sessionContext: Map<String, Any>
    ): AIResponse = withContext(Dispatchers.IO) {
        // 生成缓存键
        val cacheKey = generateCacheKey(state, userInput, sessionContext)
        
        // 检查缓存
        getCachedResponse(cacheKey)?.let { cached ->
            return@withContext AIResponse(
                success = true,
                content = cached,
                state = state,
                fromCache = true
            )
        }

        val systemPrompt = PromptTemplates.getInterviewSystemPrompt(state)
        val userMessage = PromptTemplates.buildInterviewUserMessage(state, userInput, sessionContext)

        try {
            val response = callClaudeAPIWithRetry(systemPrompt, userMessage)
            
            // 缓存响应
            cacheResponse(cacheKey, response)
            
            // 检测安全警报
            val isSafetyAlert = detectSafetyAlert(response)
            
            AIResponse(
                success = true,
                content = response,
                state = state,
                isSafetyAlert = isSafetyAlert
            )
        } catch (e: Exception) {
            AIResponse(
                success = false,
                error = "AI 响应失败: ${e.message}",
                state = state
            )
        }
    }

    /**
     * 分析家长观察记录
     */
    suspend fun analyzeObservation(
        observation: String,
        childAge: Int? = null,
        context: String? = null
    ): AIResponse = withContext(Dispatchers.IO) {
        val cacheKey = "obs_${observation.hashCode()}_${childAge ?: 0}"
        
        getCachedResponse(cacheKey)?.let { cached ->
            return@withContext AIResponse(success = true, content = cached, fromCache = true)
        }

        val systemPrompt = PromptTemplates.getParentAnalysisPrompt()
        val userMessage = PromptTemplates.buildParentAnalysisMessage(observation, childAge, context)

        try {
            val response = callClaudeAPIWithRetry(systemPrompt, userMessage)
            cacheResponse(cacheKey, response)
            
            AIResponse(success = true, content = response)
        } catch (e: Exception) {
            AIResponse(success = false, error = "分析失败: ${e.message}")
        }
    }

    // ========== 核心实现 ==========

    private suspend fun callClaudeAPIWithRetry(
        systemPrompt: String,
        userMessage: String,
        retryCount: Int = 0
    ): String = withContext(Dispatchers.IO) {
        try {
            callClaudeAPI(systemPrompt, userMessage)
        } catch (e: Exception) {
            if (retryCount < MAX_RETRIES) {
                kotlinx.coroutines.delay(RETRY_DELAY_MS * (retryCount + 1))
                callClaudeAPIWithRetry(systemPrompt, userMessage, retryCount + 1)
            } else {
                throw e
            }
        }
    }

    private fun callClaudeAPI(systemPrompt: String, userMessage: String): String {
        val apiKey = getApiKey() ?: throw IllegalStateException("API Key 未设置")

        val requestBody = JSONObject().apply {
            put("model", CLAUDE_MODEL)
            put("max_tokens", MAX_TOKENS)
            put("system", systemPrompt)
            put("messages", JSONArray().put(
                JSONObject().put("role", "user").put("content", userMessage)
            ))
        }

        val request = Request.Builder()
            .url(ANTHROPIC_API_URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("API 请求失败: ${response.code}")
            }
            
            val responseBody = response.body?.string() ?: throw IOException("空响应")
            val json = JSONObject(responseBody)
            
            if (json.has("error")) {
                throw IOException("API 错误: ${json.getJSONObject("error").getString("message")}")
            }
            
            json.getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
        }
    }

    // ========== 缓存管理 ==========

    private fun generateCacheKey(state: InterviewState, input: String, context: Map<String, Any>): String {
        val contextHash = context.hashCode()
        return "fsm_${state.name}_${input.hashCode()}_$contextHash"
    }

    private fun getCachedResponse(key: String): String? {
        val cached = cache.getString(key, null) ?: return null
        val timestamp = cache.getLong("${key}_time", 0)
        
        // 检查是否过期
        if (System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS) {
            cache.edit().remove(key).remove("${key}_time").apply()
            return null
        }
        return cached
    }

    private fun cacheResponse(key: String, response: String) {
        cache.edit()
            .putString(key, response)
            .putLong("${key}_time", System.currentTimeMillis())
            .apply()
    }

    fun clearCache() {
        cache.edit().clear().apply()
    }

    // ========== 安全检测 ==========

    private fun detectSafetyAlert(response: String): Boolean {
        val highRiskPatterns = listOf(
            "自杀", "自残", "生命危险", "立即报警",
            "suicide", "self-harm", "life-threatening"
        )
        return highRiskPatterns.any { response.contains(it, ignoreCase = true) }
    }

    /**
     * 解析风险评估结果
     */
    fun parseRiskAssessment(aiResponse: String): ParsedRiskAssessment? {
        return try {
            // 尝试从响应中提取 JSON
            val jsonStr = extractJsonFromResponse(aiResponse) ?: return parseFromText(aiResponse)
            val json = JSONObject(jsonStr)
            
            ParsedRiskAssessment(
                riskLevel = json.optString("riskLevel", "UNKNOWN"),
                confidence = json.optDouble("confidence", 0.0).toFloat(),
                possibleStressors = json.optJSONArray("possibleStressors")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList(),
                explanation = json.optString("explanation", ""),
                suggestedScript = json.optString("suggestedScript", ""),
                suggestedActions = json.optJSONArray("suggestedActions")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()
            )
        } catch (e: Exception) {
            parseFromText(aiResponse)
        }
    }

    private fun extractJsonFromResponse(response: String): String? {
        val start = response.indexOf('{')
        val end = response.lastIndexOf('}')
        return if (start >= 0 && end > start) response.substring(start, end + 1) else null
    }

    private fun parseFromText(text: String): ParsedRiskAssessment {
        val riskLevel = when {
            text.contains("RED", ignoreCase = true) || text.contains("高风险", ignoreCase = true) -> "RED"
            text.contains("YELLOW", ignoreCase = true) || text.contains("中风险", ignoreCase = true) -> "YELLOW"
            else -> "GREEN"
        }
        
        return ParsedRiskAssessment(
            riskLevel = riskLevel,
            confidence = 0.5f,
            possibleStressors = emptyList(),
            explanation = text.take(500),
            suggestedScript = "",
            suggestedActions = emptyList()
        )
    }

    // ========== 数据类 ==========

    data class AIResponse(
        val success: Boolean,
        val content: String? = null,
        val error: String? = null,
        val isSafetyAlert: Boolean = false,
        val state: InterviewState? = null,
        val fromCache: Boolean = false
    )

    data class ParsedRiskAssessment(
        val riskLevel: String,
        val confidence: Float,
        val possibleStressors: List<String>,
        val explanation: String,
        val suggestedScript: String,
        val suggestedActions: List<String>
    )
}
