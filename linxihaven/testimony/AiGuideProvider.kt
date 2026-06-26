package com.testimony.app.ai

import android.util.Log
import com.testimony.app.BuildConfig
import com.testimony.app.evidence.InducementRuleEngine
import com.testimony.app.guidance.GuidedInterviewEngine
import com.testimony.app.guidance.GuidanceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * AI引导Provider【司法级AI封装】
 *
 * 严格遵循"AI安全引导协议专家"技能的约束：
 * 1. 角色设定为"受法律协议约束的引导记录员"
 * 2. 输出结构化JSON（utterance, fsm_state, filter_passed, downgrade_reason, admin_log）
 * 3. 降级机制：AI输出必须经规则引擎过滤
 *
 * 司法意义：
 * - AI只做引导和记录，不做心理诊断或事实判断
 * - 所有输出可被法庭审计
 * - 诱导性内容被自动拦截
 *
 * @author Testimony AI安全协议专家
 */
class AiGuideProvider {

    companion object {
        private const val TAG = "AiGuideProvider"

        /** AI模型 */
        private const val MODEL = "gpt-4o-mini"

        /** 最大Token数 */
        private const val MAX_TOKENS = 300

        /** 温度参数（保持一致性） */
        private const val TEMPERATURE = 0.7

        /** 白名单引导语库 */
        private val WHITELIST_PROMPTS = mapOf(
            GuidedInterviewEngine.InterviewState.STABILIZING to WhitelistPrompt(
                text = "在开始之前，我想先确认一件事：你现在身边有其他人吗？你现在是安全的吗？",
                options = listOf("是的，我现在还算安全", "环境不安全，我需要帮助")
            ),
            GuidedInterviewEngine.InterviewState.COLLECTING to WhitelistPrompt(
                text = "我明白了。接下来，你可以用自己的话告诉我发生了什么。\n\n不用着急，想到什么就说什么。我会在这里，静静地听着。",
                options = emptyList()
            ),
            GuidedInterviewEngine.InterviewState.EMPOWERING to WhitelistPrompt(
                text = "感谢你愿意和我分享这些。你的记录已经安全地保存在你的设备上，只有你才能决定谁来查看它。",
                options = listOf("加密存储在本地", "匿名提交给学校", "分享给我的家长")
            )
        )

        /** 鼓励语 */
        private val ENCOURAGEMENTS = listOf(
            "你愿意说出来，这本身就非常勇敢。",
            "做得很棒，慢慢来，不着急。",
            "我在这里陪着你，你随时可以暂停。",
            "你做的每一步都是在保护自己。",
            "这件事不是你的错。"
        )
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json".toMediaType()

    private val apiKey: String
        get() = BuildConfig.AI_API_KEY.ifEmpty { "" }

    private val apiEndpoint: String
        get() = "${BuildConfig.AI_OPENAI_ENDPOINT}/chat/completions"

    /**
     * 生成指定状态的引导语【核心接口】
     *
     * @param state 当前FSM状态
     * @param history 用户响应历史
     * @param collectedElements 已收集的要素
     * @return 引导提示对象
     */
    suspend fun generateForState(
        state: GuidedInterviewEngine.InterviewState,
        history: List<String>,
        collectedElements: Set<String>
    ): GuidedInterviewEngine.GuidedPrompt = withContext(Dispatchers.IO) {
        // 如果API Key为空，直接使用模板
        if (apiKey.isEmpty()) {
            Log.w(TAG, "API key empty, using template")
            return@withContext useTemplatePrompt(state)
        }

        try {
            // 构建消息
            val messages = buildMessages(state, history, collectedElements)

            // 调用AI
            val aiResponse = callAi(messages)

            // 诱导性检测
            val detection = InducementRuleEngine.check(aiResponse)

            if (detection.isClean) {
                // AI输出通过检测
                return@withContext GuidedInterviewEngine.GuidedPrompt(
                    text = aiResponse,
                    source = GuidedInterviewEngine.PromptSource.AI,
                    options = getOptionsForState(state),
                    encouragement = if (state == GuidedInterviewEngine.InterviewState.COLLECTING)
                        ENCOURAGEMENTS.random() else null,
                    wasDowngraded = false
                )
            } else {
                // AI输出被拦截，降级到白名单
                Log.w(TAG, "AI output blocked: ${detection.violations.map { it.ruleId }}")

                return@withContext GuidedInterviewEngine.GuidedPrompt(
                    text = detection.downgradePhrase ?: getWhitelistPrompt(state).text,
                    source = GuidedInterviewEngine.PromptSource.WHITELIST,
                    options = getOptionsForState(state),
                    encouragement = ENCOURAGEMENTS.random(),
                    wasDowngraded = true,
                    downgradedReason = detection.violations.joinToString("; ") {
                        "Rule ${it.ruleId}: ${it.description}"
                    }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "AI generation failed: ${e.message}", e)
            return@withContext useTemplatePrompt(state)
        }
    }

    /**
     * 调用AI API
     */
    private suspend fun callAi(messages: JSONArray): String = withContext(Dispatchers.IO) {
        val requestBody = JSONObject().apply {
            put("model", MODEL)
            put("messages", messages)
            put("max_tokens", MAX_TOKENS)
            put("temperature", TEMPERATURE)
        }

        val request = Request.Builder()
            .url(apiEndpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string()

        if (!response.isSuccessful || body == null) {
            throw Exception("API error: ${response.code}")
        }

        val json = JSONObject(body)
        val choices = json.optJSONArray("choices")
        if (choices == null || choices.length() == 0) {
            throw Exception("No choices in response")
        }

        choices.getJSONObject(0)
            .optJSONObject("message")
            ?.optString("content", "") ?: ""
    }

    /**
     * 构建消息列表
     */
    private fun buildMessages(
        state: GuidedInterviewEngine.InterviewState,
        history: List<String>,
        collected: Set<String>
    ): JSONArray {
        val messages = JSONArray()

        // 系统提示
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", buildSystemPrompt(state, collected))
        })

        // 对话历史（最近6轮）
        val historySize = minOf(history.size, 6)
        for (i in (history.size - historySize) until history.size) {
            messages.put(JSONObject().apply {
                put("role", "user")
                put("content", history[i])
            })
            messages.put(JSONObject().apply {
                put("role", "assistant")
                put("content", "（已记录）")
            })
        }

        // 当前状态触发
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", getStateInstruction(state))
        })

        return messages
    }

    /**
     * 构建系统提示
     */
    private fun buildSystemPrompt(state: GuidedInterviewEngine.InterviewState, collected: Set<String>): String {
        val collectedStr = if (collected.isEmpty()) "尚未收集" else collected.joinToString(", ")

        return """你是证言助手，一个受法律协议约束的AI引导记录员。

【角色边界】（绝对不可逾越）
- ✅ 你只做引导和记录，不做心理诊断或事实判断
- ✅ 你表达共情，但不替代用户做出判断
- ❌ 禁止预设事实（如"他打你的时候"）
- ❌ 禁止暗示用户应该有某种感受
- ❌ 禁止心理诊断或性格标签
- ❌ 禁止封闭式诱导（"是不是"句式）

【当前状态】
${state.name}
已收集要素: $collectedStr

【状态指导】
${getStateInstruction(state)}

请生成一段温和且不诱导的引导语（不超过150字）。"""
    }

    /**
     * 获取状态指导
     */
    private fun getStateInstruction(state: GuidedInterviewEngine.InterviewState): String = when (state) {
        GuidedInterviewEngine.InterviewState.INIT ->
            "初始化阶段。等待用户开始。"

        GuidedInterviewEngine.InterviewState.STABILIZING ->
            "【稳定化阶段】确认环境安全。用温和的语气询问用户是否安全。"

        GuidedInterviewEngine.InterviewState.COLLECTING ->
            "【信息收集阶段】让用户用自己的话描述发生的事情。不要追问细节，不要评判。"

        GuidedInterviewEngine.InterviewState.EMPOWERING ->
            "【赋能阶段】让用户决定下一步。可以问：你想怎么处理这件事？"

        GuidedInterviewEngine.InterviewState.COMPLETED ->
            "【完成阶段】感谢用户，告知记录已保存。"

        GuidedInterviewEngine.InterviewState.HUMAN_HANDOVER ->
            "【人工接管】提示用户联系人工专家。"
    }

    /**
     * 使用模板提示
     */
    private fun useTemplatePrompt(state: GuidedInterviewEngine.InterviewState): GuidedInterviewEngine.GuidedPrompt {
        val whitelist = getWhitelistPrompt(state)
        return GuidedInterviewEngine.GuidedPrompt(
            text = whitelist.text,
            source = GuidedInterviewEngine.PromptSource.TEMPLATE,
            options = whitelist.options,
            encouragement = if (state == GuidedInterviewEngine.InterviewState.COLLECTING)
                ENCOURAGEMENTS.random() else null
        )
    }

    /**
     * 获取白名单提示
     */
    private fun getWhitelistPrompt(state: GuidedInterviewEngine.InterviewState): WhitelistPrompt {
        return WHITELIST_PROMPTS[state] ?: WhitelistPrompt(
            text = "请继续告诉我你想说的内容。",
            options = emptyList()
        )
    }

    /**
     * 获取状态选项
     */
    private fun getOptionsForState(state: GuidedInterviewEngine.InterviewState): List<String> {
        return when (state) {
            GuidedInterviewEngine.InterviewState.STABILIZING ->
                listOf("是的，我现在还算安全", "环境不安全，我需要帮助")
            GuidedInterviewEngine.InterviewState.COLLECTING ->
                listOf("我记得的时间", "大概的时间", "不太确定了")
            GuidedInterviewEngine.InterviewState.EMPOWERING ->
                listOf("加密存储在本地", "匿名提交给学校", "分享给我的家长")
            else -> emptyList()
        }
    }

    /**
     * 生成结构化审计日志
     */
    fun generateAuditLog(
        utterance: String,
        state: GuidedInterviewEngine.InterviewState,
        filterPassed: Boolean,
        downgradeReason: String?
    ): JSONObject {
        return JSONObject().apply {
            put("utterance", utterance)
            put("fsm_state", state.name)
            put("filter_passed", filterPassed)
            put("downgrade_reason", downgradeReason ?: JSONObject.NULL)
            put("timestamp", System.currentTimeMillis())
            put("admin_log", JSONObject().apply {
                put("session_id", java.util.UUID.randomUUID().toString())
                put("model", MODEL)
                put("temperature", TEMPERATURE)
            })
        }
    }

    data class WhitelistPrompt(
        val text: String,
        val options: List<String>
    )
}
