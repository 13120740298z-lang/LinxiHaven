package com.testimony.ai

import android.content.Context
import android.util.Log
import com.testimony.data.models.InterviewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Inducement Detector
 * Prevents leading questions and biased framing in AI guidance
 */
class InducementDetector(private val context: Context) {

    /**
     * Result of inducement detection
     */
    data class DetectionResult(
        val isSafe: Boolean,
        val riskLevel: RiskLevel,
        val issues: List<InducementIssue>,
        val suggestedReplacement: String? = null
    )

    enum class RiskLevel {
        SAFE,      // No issues
        LOW,       // Minor wording concerns
        MEDIUM,    // Potential inducement
        HIGH       // Clear inducement, must reject
    }

    data class InducementIssue(
        val type: IssueType,
        val description: String,
        val problematicPhrase: String,
        val position: IntRange
    )

    enum class IssueType {
        PRESUMES_FACT,      // Presumes the event happened
        LEADING_QUESTION,   // Leading question with embedded answer
        EMOTIONAL_PRESSURE, // Creates emotional pressure
        ASSUMES_INTENT,      // Assumes the other party's intent
        NEGATIVE_LABEL,      // Uses negative labels or assumptions
        DOUBLE_NEGATIVE      // Confusing double negative
    }

    /**
     * Check if a prompt text is safe for AI guidance
     */
    suspend fun checkPrompt(prompt: String): DetectionResult = withContext(Dispatchers.Default) {
        val issues = mutableListOf<InducementIssue>()

        // Check for presumptive facts
        issues.addAll(checkPresumptiveFacts(prompt))

        // Check for leading questions
        issues.addAll(checkLeadingQuestions(prompt))

        // Check for emotional pressure
        issues.addAll(checkEmotionalPressure(prompt))

        // Check for intent assumptions
        issues.addAll(checkIntentAssumptions(prompt))

        // Check for negative labels
        issues.addAll(checkNegativeLabels(prompt))

        // Determine overall risk level
        val riskLevel = when {
            issues.any { it.type in listOf(IssueType.PRESUMES_FACT, IssueType.LEADING_QUESTION, IssueType.ASSUMES_INTENT) } -> RiskLevel.HIGH
            issues.any { it.type == IssueType.EMOTIONAL_PRESSURE } -> RiskLevel.MEDIUM
            issues.any { it.type in listOf(IssueType.NEGATIVE_LABEL, IssueType.DOUBLE_NEGATIVE) } -> RiskLevel.LOW
            else -> RiskLevel.SAFE
        }

        val isSafe = riskLevel == RiskLevel.SAFE

        DetectionResult(
            isSafe = isSafe,
            riskLevel = riskLevel,
            issues = issues,
            suggestedReplacement = if (!isSafe) generateReplacement(prompt, issues) else null
        )
    }

    private fun checkPresumptiveFacts(text: String): List<InducementIssue> {
        val issues = mutableListOf<InducementIssue>()

        // Patterns that presume the event happened
        val presumptivePatterns = listOf(
            // Chinese
            "他欺负你了" to "他" to "当事件涉及'他欺负'时",
            "她针对你" to "针对你" to "当预设'针对'发生时",
            "你被欺负" to "被欺负" to "当使用被动受害语气时",
            "发生了霸凌" to "霸凌" to "当直接使用'霸凌'定性时",
            "他们欺负" to "他们欺负" to "当预设加害者行为时",
            // English
            "did he bully" to "bully" to "When using 'bully' as established fact",
            "she is harassing" to "harassing" to "When using 'harassing' as established fact",
            "they hurt you" to "hurt you" to "When presuming harm"
        )

        for ((pattern, trigger, description) in presumptivePatterns) {
            val index = text.indexOf(pattern, ignoreCase = true)
            if (index >= 0) {
                issues.add(
                    InducementIssue(
                        type = IssueType.PRESUMES_FACT,
                        description = description,
                        problematicPhrase = pattern,
                        position = index..(index + pattern.length)
                    )
                )
            }
        }

        return issues
    }

    private fun checkLeadingQuestions(text: String): List<InducementIssue> {
        val issues = mutableListOf<InducementIssue>()

        // Leading question patterns
        val leadingPatterns = listOf(
            // Doesn't it make you feel...
            "难道不觉得" to "用反问句预设感受",
            "don't you feel" to "Using tag question to preset feeling",
            // Isn't it true that...
            "不是吗" to "用反问句确认预设",
            "isn't it true" to "Using leading tag question",
            // You were... weren't you
            "对不对" to "用短促反问引导",
            "right\\?" to "Using short confirmation tag",
            // Questions that embed the answer
            "他是不是" to "用选择问句嵌入预设答案",
            "wasn't he" to "Embedding answer in question"
        )

        for ((pattern, description) in leadingPatterns) {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            val match = regex.find(text)
            if (match != null) {
                issues.add(
                    InducementIssue(
                        type = IssueType.LEADING_QUESTION,
                        description = description,
                        problematicPhrase = match.value,
                        position = match.range
                    )
                )
            }
        }

        return issues
    }

    private fun checkEmotionalPressure(text: String): List<InducementIssue> {
        val issues = mutableListOf<InducementIssue>()

        // Patterns that create emotional pressure
        val pressurePatterns = listOf(
            // Urgency
            "快点说" to "催促性语言",
            "hurry" to "Urgency pressure",
            // Consequence emphasis
            "不说的话" to "威胁性暗示",
            "if you don't" to "Threatening implication",
            // Guilt induction
            "我都这么关心你了" to "内疚诱导",
            "after all I've done" to "Guilt induction",
            // Authority appeal
            "你要诚实" to "诚实绑架",
            "you must be honest" to "Authority pressure"
        )

        for ((pattern, description) in pressurePatterns) {
            val index = text.indexOf(pattern, ignoreCase = true)
            if (index >= 0) {
                issues.add(
                    InducementIssue(
                        type = IssueType.EMOTIONAL_PRESSURE,
                        description = description,
                        problematicPhrase = pattern,
                        position = index..(index + pattern.length)
                    )
                )
            }
        }

        return issues
    }

    private fun checkIntentAssumptions(text: String): List<InducementIssue> {
        val issues = mutableListOf<InducementIssue>()

        // Patterns that assume the other party's intent
        val intentPatterns = listOf(
            // He故意的
            "故意" to "预设加害意图",
            "intentionally" to "Assuming malicious intent",
            // 他就是看不惯你
            "看不惯" to "预设嫉妒动机",
            "jealous of" to "Assuming jealousy motive",
            // 他想欺负你
            "想.*欺负".toRegex() to "预设欺负意图"
        )

        for (pattern in intentPatterns) {
            when (pattern) {
                is String -> {
                    val index = text.indexOf(pattern, ignoreCase = true)
                    if (index >= 0) {
                        issues.add(
                            InducementIssue(
                                type = IssueType.ASSUMES_INTENT,
                                description = "预设加害方意图",
                                problematicPhrase = pattern,
                                position = index..(index + pattern.length)
                            )
                        )
                    }
                }
                is Regex -> {
                    val match = pattern.find(text)
                    if (match != null) {
                        issues.add(
                            InducementIssue(
                                type = IssueType.ASSUMES_INTENT,
                                description = "预设加害方意图",
                                problematicPhrase = match.value,
                                position = match.range
                            )
                        )
                    }
                }
            }
        }

        return issues
    }

    private fun checkNegativeLabels(text: String): List<InducementIssue> {
        val issues = mutableListOf<InducementIssue>()

        // Negative character labels
        val labelPatterns = listOf(
            "坏人" to "负面标签",
            "bad person" to "Negative label",
            "恶霸" to "定性标签",
            "bully" to "Character label"
        )

        for ((pattern, description) in labelPatterns) {
            val index = text.indexOf(pattern, ignoreCase = true)
            if (index >= 0) {
                issues.add(
                    InducementIssue(
                        type = IssueType.NEGATIVE_LABEL,
                        description = description,
                        problematicPhrase = pattern,
                        position = index..(index + pattern.length)
                    )
                )
            }
        }

        return issues
    }

    private fun generateReplacement(original: String, issues: List<InducementIssue>): String {
        // Generate a safe replacement based on detected issues
        var replacement = original

        // Replace common problematic patterns with neutral alternatives
        val replacements = mapOf(
            "他欺负你了" to "你描述的情况",
            "她针对你" to "你经历的",
            "你被欺负" to "发生的事情",
            "发生了霸凌" to "你描述的事件",
            "他们欺负" to "涉及的人员",
            "did he bully" to "what happened with",
            "she is harassing" to "what she did",
            "don't you feel" to "how do you feel",
            "Isn't it true" to "Can you tell me"
        )

        for ((original, safe) in replacements) {
            replacement = replacement.replace(original, safe, ignoreCase = true)
        }

        return replacement
    }

    /**
     * Batch check for multiple prompts
     */
    suspend fun batchCheck(prompts: List<String>): List<DetectionResult> {
        return prompts.map { checkPrompt(it) }
    }

    /**
     * Validate entire conversation for inducement
     */
    suspend fun validateConversation(
        aiPrompts: List<String>,
        userResponses: List<String>
    ): ConversationValidationResult {
        val promptResults = batchCheck(aiPrompts)

        val overallSafe = promptResults.all { it.isSafe }

        val issues = promptResults.flatMap { it.issues }

        val suggestion = if (!overallSafe) {
            promptResults.firstOrNull { !it.isSafe }?.suggestedReplacement
        } else null

        return ConversationValidationResult(
            isValid = overallSafe,
            promptResults = promptResults,
            totalIssues = issues.size,
            suggestedFix = suggestion
        )
    }

    data class ConversationValidationResult(
        val isValid: Boolean,
        val promptResults: List<DetectionResult>,
        val totalIssues: Int,
        val suggestedFix: String?
    )

    companion object {
        private const val TAG = "InducementDetector"

        // Minimum acceptable rate for inducement detection
        const val MIN_DETECTION_RATE = 0.90
        const val MAX_FALSE_POSITIVE_RATE = 0.05
    }
}
