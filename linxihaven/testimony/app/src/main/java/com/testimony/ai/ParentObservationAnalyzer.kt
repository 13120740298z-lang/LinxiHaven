package com.testimony.ai

import android.content.Context
import com.testimony.data.models.Observation
import com.testimony.data.models.RiskAssessment
import com.testimony.data.models.RiskLevel
import com.testimony.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Parent Observation Analyzer
 * Analyzes parental observations to generate risk assessments and communication scripts
 */
class ParentObservationAnalyzer(private val context: Context) {

    /**
     * Analyze observation and generate risk assessment
     */
    suspend fun analyze(observation: Observation): RiskAssessmentResult = withContext(Dispatchers.Default) {
        val text = observation.content.lowercase()

        // Analyze risk indicators
        val indicators = analyzeRiskIndicators(text)

        // Calculate risk level
        val riskLevel = calculateRiskLevel(indicators)

        // Identify possible stressors
        val stressors = identifyStressors(text)

        // Calculate confidence
        val confidence = calculateConfidence(indicators)

        // Generate explanation
        val explanation = generateExplanation(indicators, observation)

        RiskAssessmentResult(
            observation = observation,
            riskLevel = riskLevel,
            possibleStressors = stressors,
            confidence = confidence,
            explanation = explanation,
            suggestedScript = generateScript(riskLevel, stressors),
            suggestedActions = generateActions(riskLevel),
            emotionalIndicators = indicators.emotional,
            behavioralIndicators = indicators.behavioral,
            contextFactors = indicators.context
        )
    }

    private data class RiskIndicators(
        val emotional: List<String>,
        val behavioral: List<String>,
        val context: List<String>,
        val severity: Int // 0-10
    )

    private fun analyzeRiskIndicators(text: String): RiskIndicators {
        val emotional = mutableListOf<String>()
        val behavioral = mutableListOf<String>()
        val context = mutableListOf<String>()
        var severity = 0

        // Emotional indicators
        val emotionalKeywords = mapOf(
            "sad" to listOf("不开心", "沮丧", "情绪低落", "sad", "depressed", "down"),
            "fear" to listOf("害怕", "恐惧", "担心", "害怕上学", "fear", "afraid", "scared"),
            "anger" to listOf("愤怒", "生气", "易怒", "angry", "irritable", "frustrated"),
            "withdrawn" to listOf("沉默", "封闭", "不理人", "withdrawn", "quiet", "isolated"),
            "anxious" to listOf("焦虑", "紧张", "不安", "anxious", "nervous", "worried")
        )

        for ((emotion, keywords) in emotionalKeywords) {
            if (keywords.any { text.contains(it) }) {
                emotional.add(emotion)
                severity += when (emotion) {
                    "sad" -> 2
                    "fear" -> 3
                    "anger" -> 2
                    "withdrawn" -> 2
                    "anxious" -> 2
                    else -> 1
                }
            }
        }

        // Behavioral indicators
        val behavioralKeywords = mapOf(
            "sleep" to listOf("失眠", "睡不好", "做噩梦", "insomnia", "nightmare", "sleep issues"),
            "appetite" to listOf("不吃饭", "食欲下降", "eating", "appetite change", "not eating"),
            "academic" to listOf("成绩下降", "不想上学", "逃学", "grades", "school refusal", "skipping"),
            "social" to listOf("不和朋友玩", "被孤立", "social", "friends", "isolated"),
            "self_harm" to listOf("自残", "伤自己", "self harm", "cutting", "suicide")
        )

        for ((behavior, keywords) in behavioralKeywords) {
            if (keywords.any { text.contains(it) }) {
                behavioral.add(behavior)
                severity += when (behavior) {
                    "sleep" -> 2
                    "appetite" -> 2
                    "academic" -> 3
                    "social" -> 2
                    "self_harm" -> 10
                    else -> 1
                }
            }
        }

        // Context factors
        val contextKeywords = listOf(
            "recently" to listOf("最近", "这段时间", "recently", "lately"),
            "school_related" to listOf("学校", "同学", "老师", "school", "classmates", "teacher"),
            "online" to listOf("网上", "手机", "游戏", "online", "internet", "gaming", "phone"),
            "repeated" to listOf("一直", "反复", "连续", "repeatedly", "consistently", "every day")
        )

        for ((factor, keywords) in contextKeywords) {
            if (keywords.any { text.contains(it) }) {
                context.add(factor)
                severity += 1
            }
        }

        return RiskIndicators(
            emotional = emotional,
            behavioral = behavioral,
            context = context,
            severity = severity.coerceAtMost(10)
        )
    }

    private fun calculateRiskLevel(indicators: RiskIndicators): RiskLevel {
        return when {
            indicators.behavioral.contains("self_harm") -> RiskLevel.RED
            indicators.severity >= 7 -> RiskLevel.RED
            indicators.severity >= 4 -> RiskLevel.YELLOW
            else -> RiskLevel.GREEN
        }
    }

    private fun identifyStressors(text: String): List<String> {
        val stressors = mutableListOf<String>()

        val stressorPatterns = mapOf(
            "学业压力" to listOf("考试", "作业", "成绩", "升学", "exam", "homework", "grades"),
            "同伴关系" to listOf("朋友", "同学", "孤立", "排挤", "friend", "classmate", "excluded"),
            "网络问题" to listOf("网络霸凌", "游戏", "社交媒体", "cyberbullying", "online", "social media"),
            "家庭因素" to listOf("父母", "家庭", "亲子", "parents", "family", "arguments"),
            "校园霸凌" to listOf("欺负", "霸凌", "打", "骂", "bully", "harassment", "physical"),
            "未知原因" to listOf("不知道", "不清楚", "不明", "unknown", "unclear")
        )

        for ((stressor, keywords) in stressorPatterns) {
            if (keywords.any { text.contains(it) }) {
                stressors.add(stressor)
            }
        }

        if (stressors.isEmpty()) {
            stressors.add("需要进一步观察")
        }

        return stressors
    }

    private fun calculateConfidence(indicators: RiskIndicators): Float {
        // Confidence increases with more specific indicators
        val baseConfidence = 0.4f
        val emotionalBonus = indicators.emotional.size * 0.1f
        val behavioralBonus = indicators.behavioral.size * 0.15f
        val contextBonus = indicators.context.size * 0.05f

        return (baseConfidence + emotionalBonus + behavioralBonus + contextBonus).coerceAtMost(0.95f)
    }

    private fun generateExplanation(indicators: RiskIndicators, observation: Observation): String {
        return buildString {
            append("根据您的观察：")

            if (indicators.emotional.isNotEmpty()) {
                append("孩子表现出一些情绪上的变化。")
            }

            if (indicators.behavioral.isNotEmpty()) {
                append("同时有一些行为上的改变值得关注。")
            }

            if (indicators.context.isNotEmpty()) {
                append("这些变化与${observation.context ?: "日常生活"}相关。")
            }
        }
    }

    private fun generateScript(riskLevel: RiskLevel, stressors: List<String>): String {
        return when (riskLevel) {
            RiskLevel.GREEN -> """
您好，我想和你聊聊。

我注意到你最近好像有些不一样，我有些担心你。

如果你愿意的话，可以和我说说最近怎么样吗？

无论发生什么，我都在这里支持你。
            """.trimIndent()

            RiskLevel.YELLOW -> """
孩子，我想找个时间我们好好聊聊。

不是要批评你什么，我只是想了解你是不是有什么心事。

如果你不想说也没关系，但我希望你知道，我是站在你这边的。
            """.trimIndent()

            RiskLevel.RED -> """
孩子，我注意到你最近变化很大，我很担心你。

不管发生了什么，我都希望你能告诉我。

如果你觉得很难开口，我们可以找专业的老师或者心理咨询师来帮助你。

最重要的是，我知道这可能不是你的错，我爱你并且支持你。
            """.trimIndent()
        }
    }

    private fun generateActions(riskLevel: RiskLevel): List<String> {
        return when (riskLevel) {
            RiskLevel.GREEN -> listOf(
                "继续保持开放的沟通渠道",
                "观察孩子是否持续有变化",
                "记录孩子的好时刻和困难时刻"
            )
            RiskLevel.YELLOW -> listOf(
                "安排一次不带压力的谈话",
                "联系学校老师了解情况",
                "考虑寻求学校心理咨询支持",
                "避免直接质问，给孩子安全感"
            )
            RiskLevel.RED -> listOf(
                "立即联系学校辅导员或班主任",
                "考虑寻求专业心理咨询",
                "保持冷静，避免过度反应",
                "确保孩子的安全是首要任务",
                "如有需要，联系当地心理援助热线"
            )
        }
    }

    data class RiskAssessmentResult(
        val observation: Observation,
        val riskLevel: RiskLevel,
        val possibleStressors: List<String>,
        val confidence: Float,
        val explanation: String,
        val suggestedScript: String,
        val suggestedActions: List<String>,
        val emotionalIndicators: List<String>,
        val behavioralIndicators: List<String>,
        val contextFactors: List<String>
    )
}
