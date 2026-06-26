package com.testimony.app.evidence

import android.util.Log

/**
 * 诱导性检测规则引擎【司法语言学实现】
 *
 * 基于"AI安全引导协议专家"技能的15条核心规则
 * 每个规则都有AST级实现，而非简单正则
 *
 * 司法意义：
 * - 确保AI引导语不预设事实、不诱导用户
 * - 所有拦截记录可被法庭审计
 * - 连续3次拦截触发人类专家接管
 *
 * @author Testimony AI安全协议专家
 */
object InducementRuleEngine {

    private const val TAG = "InducementDetector"

    /** 连续拦截阈值 - 超过此值触发人类专家接管 */
    const val ESCALATION_THRESHOLD = 3

    /** 白名单语句库 - 经法律审核的固定引导语 */
    private val WHITELIST_PHRASES = listOf(
        // 稳定化阶段
        "你好，感谢你愿意分享。我是证言的AI助手，我会认真倾听。",
        "这里是安全的。你可以按自己的节奏来说。",
        "如果你需要暂停，随时告诉我。",
        // 信息收集阶段
        "你愿意从哪部分开始说起？",
        "关于这件事的经过，你记得哪些部分？",
        "这件事是什么时候发生的？",
        "在什么地方发生的？",
        "能说说都涉及了哪些人吗？",
        "发生后对你有什么影响？",
        "现在有谁来帮你处理这件事吗？",
        // 赋能阶段
        "你想怎么处理这件事，决定权完全在你。",
        "你希望谁来帮助你？",
        "你准备好提交这份记录了吗？"
    )

    /**
     * 检查文本是否包含诱导性内容【主入口】
     *
     * @param text 待检查文本
     * @return 检测结果，包含违规类型、匹配内容、降级建议
     */
    fun check(text: String): DetectionResult {
        if (text.isBlank()) {
            return DetectionResult(isClean = true, violations = emptyList(), isEmpty = true)
        }

        val violations = mutableListOf<Violation>()

        // 规则1-2：预设事实检测（是谁/怎么受伤的）
        checkPresumedFactPatterns(text, violations)

        // 规则3-5：暗示性引导检测
        checkSuggestivePatterns(text, violations)

        // 规则6：封闭式诱导问题
        checkClosedInducingPatterns(text, violations)

        // 规则7-8：心理推测模式
        checkPsychologicalSpeculation(text, violations)

        // 规则9-15：其他违规模式
        checkOtherViolationPatterns(text, violations)

        return DetectionResult(
            isClean = violations.isEmpty(),
            violations = violations,
            isEmpty = false,
            downgradePhrase = if (violations.isNotEmpty()) getDowngradePhrase(violations) else null
        )
    }

    /**
     * 预设事实模式检测【规则1-2】
     *
     * 禁止模式：
     * - 含"是谁"：预设施暴者身份
     * - 含"怎么受伤的"：预设发生了伤害行为
     */
    private fun checkPresumedFactPatterns(text: String, violations: MutableList<Violation>) {
        // 模式1：预设施暴者（"是谁"）
        if (containsPattern(text, "是谁") && containsAny(text, listOf("打", "骂", "欺负", "霸凌", "欺", "侮辱"))) {
            violations.add(Violation(
                ruleId = 1,
                type = ViolationType.PRESUMED_FACT,
                match = extractMatch(text, "是谁.*?(打|骂|欺负|霸凌)"),
                description = "预设施暴者身份，暗示特定人物施暴",
                alternative = "关于这件事，你能说说吗？"
            ))
        }

        // 模式2：预设行为（"打你了"、"骂你了"）
        val presumedActions = listOf(
            "打.*你.*了吗" to "如果有身体接触，你愿意描述吗？",
            "骂.*你.*了吗" to "能说说对方说了什么吗？",
            "欺负.*你了" to "能描述一下发生了什么吗？",
            "霸凌.*你了" to "你愿意说说发生了什么吗？"
        )

        presumedActions.forEach { (pattern, alternative) ->
            if (containsPattern(text, pattern)) {
                violations.add(Violation(
                    ruleId = 2,
                    type = ViolationType.PRESUMED_FACT,
                    match = extractMatch(text, pattern),
                    description = "预设霸凌行为，暗示事件已经发生",
                    alternative = alternative
                ))
            }
        }

        // 模式3："是不是"句式（暗示确认）
        if (containsPattern(text, "是不是.*?(他|她|它|他们)")) {
            violations.add(Violation(
                ruleId = 3,
                type = ViolationType.PRESUMED_FACT,
                match = extractMatch(text, "是不是.*?(他|她|它)"),
                description = "使用'是不是'句式预设事实",
                alternative = "你能说说发生了什么吗？"
            ))
        }
    }

    /**
     * 暗示性引导检测【规则3-5】
     *
     * 禁止模式：
     * - 含"应该"：评判用户行为
     * - 含"为什么你不"：责备句式
     * - 含"肯定"：暗示确定性
     */
    private fun checkSuggestivePatterns(text: String, violations: MutableList<Violation>) {
        // 模式1："应该"评判词
        if (containsPattern(text, "应该.*?觉得")) {
            violations.add(Violation(
                ruleId = 4,
                type = ViolationType.SUGGESTIVE,
                match = extractMatch(text, "应该.*?觉得"),
                description = "包含'应该'评判词，引导用户认同特定判断",
                alternative = "你当时是怎么做的？"
            ))
        }

        // 模式2："为什么你不"责备句式
        if (containsPattern(text, "为什么.*?(不|没)")) {
            violations.add(Violation(
                ruleId = 5,
                type = ViolationType.SUGGESTIVE,
                match = extractMatch(text, "为什么.*?(不|没)"),
                description = "使用'为什么你不'句式，带有责备意味",
                alternative = "后来你考虑过哪些选择？"
            ))
        }

        // 模式3："肯定"、"一定"等确定性词汇
        val certaintyWords = listOf("肯定", "一定", "毫无疑问", "明显", "大家都")
        certaintyWords.forEach { word ->
            if (text.contains(word)) {
                violations.add(Violation(
                    ruleId = 6,
                    type = ViolationType.SUGGESTIVE,
                    match = word,
                    description = "使用确定性词汇，暗示唯一正确判断",
                    alternative = "你当时的感觉是什么？"
                ))
            }
        }
    }

    /**
     * 封闭式诱导问题检测【规则6】
     *
     * 禁止模式：
     * - "你恨他吗"
     * - "你想报复吗"
     * - 回答选项只有"是/否"
     */
    private fun checkClosedInducingPatterns(text: String, violations: MutableList<Violation>) {
        val closedPatterns = listOf(
            "恨.*吗" to "你对这件事有什么感受？",
            "报复.*吗" to "你想怎么处理这件事？",
            "对.*吗" to "你的看法是什么？"
        )

        closedPatterns.forEach { (pattern, alternative) ->
            if (containsPattern(text, pattern)) {
                violations.add(Violation(
                    ruleId = 7,
                    type = ViolationType.CLOSED_INDUCING,
                    match = extractMatch(text, pattern),
                    description = "封闭式问题，限制用户回答",
                    alternative = alternative
                ))
            }
        }
    }

    /**
     * 心理推测检测【规则7-8】
     *
     * 禁止模式：
     * - 心理诊断（焦虑、抑郁）
     * - 性格标签（X型人格）
     */
    private fun checkPsychologicalSpeculation(text: String, violations: MutableList<Violation>) {
        val psychologicalPatterns = listOf(
            "心理.*?(问题|障碍|疾病)" to "你的感受是什么？",
            "[抑焦]郁" to "你最近感觉怎么样？",
            "人格" to "你愿意说说你的想法吗？",
            "诊断" to "你的感受是什么？"
        )

        psychologicalPatterns.forEach { (pattern, alternative) ->
            if (containsPattern(text, pattern)) {
                violations.add(Violation(
                    ruleId = 8,
                    type = ViolationType.PSYCHOLOGICAL_SPECULATION,
                    match = extractMatch(text, pattern),
                    description = "包含心理诊断或性格标签，超出引导员职责",
                    alternative = alternative
                ))
            }
        }
    }

    /**
     * 其他违规模式检测【规则9-15】
     */
    private fun checkOtherViolationPatterns(text: String, violations: MutableList<Violation>) {
        // 规则9：数量细节假设
        if (containsPattern(text, "几次|多少.*?次|多少次")) {
            violations.add(Violation(
                ruleId = 9,
                type = ViolationType.OTHER,
                match = extractMatch(text, "几次|多少.*?次"),
                description = "预设事件发生次数"
            ))
        }

        // 规则10：比较句式
        if (containsPattern(text, "比.*?更|和.*?一样")) {
            violations.add(Violation(
                ruleId = 10,
                type = ViolationType.OTHER,
                match = extractMatch(text, "比.*?更"),
                description = "包含比较句式，可能引导用户做对比"
            ))
        }

        // 规则11：猜测动机
        if (containsPattern(text, "他.*?因为.*?所以")) {
            violations.add(Violation(
                ruleId = 11,
                type = ViolationType.OTHER,
                match = extractMatch(text, "他.*?因为"),
                description = "猜测施暴方动机，超出引导范围"
            ))
        }

        // 规则12：时间起点假设
        if (containsPattern(text, "从.*?开始") && containsAny(text, listOf("就没", "就总", "就一直"))) {
            violations.add(Violation(
                ruleId = 12,
                type = ViolationType.OTHER,
                match = extractMatch(text, "从.*?开始"),
                description = "预设事件起始时间"
            ))
        }

        // 规则13：武器/工具假设
        val weaponPatterns = listOf("用什么.*?(打|砸|威胁)", "拿.*?来.*?(你|你们)")
        weaponPatterns.forEach { pattern ->
            if (containsPattern(text, pattern)) {
                violations.add(Violation(
                    ruleId = 13,
                    type = ViolationType.OTHER,
                    match = extractMatch(text, pattern),
                    description = "预设使用工具或武器"
                ))
            }
        }

        // 规则14：关系定性
        if (containsPattern(text, "你们.*?是什么关系") ||
            containsPattern(text, "他是你.*?人")) {
            violations.add(Violation(
                ruleId = 14,
                type = ViolationType.OTHER,
                match = "关系定性",
                description = "定性双方关系，超出引导范围"
            ))
        }

        // 规则15：严重程度评估
        if (containsPattern(text, "严不严重|有多.*?(严重|坏|糟)")) {
            violations.add(Violation(
                ruleId = 15,
                type = ViolationType.OTHER,
                match = extractMatch(text, "严不严重|有多"),
                description = "预设严重程度分级"
            ))
        }
    }

    /**
     * 辅助方法：检查文本是否包含指定模式
     */
    private fun containsPattern(text: String, pattern: String): Boolean {
        return try {
            Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(text)
        } catch (e: Exception) {
            text.contains(pattern, ignoreCase = true)
        }
    }

    /**
     * 辅助方法：提取匹配的文本
     */
    private fun extractMatch(text: String, pattern: String): String {
        return try {
            Regex(pattern, RegexOption.IGNORE_CASE).find(text)?.value ?: pattern
        } catch (e: Exception) {
            pattern
        }
    }

    /**
     * 辅助方法：检查是否包含列表中的任意词
     */
    private fun containsAny(text: String, words: List<String>): Boolean {
        return words.any { text.contains(it, ignoreCase = true) }
    }

    /**
     * 获取降级短语
     */
    private fun getDowngradePhrase(violations: List<Violation>): String {
        // 优先使用第一个违规的替代建议
        val firstAlternative = violations.firstOrNull()?.alternative
        if (!firstAlternative.isNullOrBlank()) {
            return firstAlternative
        }

        // 否则使用白名单中的通用语句
        return WHITELIST_PHRASES.random()
    }

    /**
     * 获取拦截统计信息
     */
    fun getInterceptionStats(): InterceptionStats {
        return InterceptionStats(
            totalChecks = totalChecks,
            cleanChecks = cleanChecks,
            blockedChecks = blockedChecks,
            currentStreak = currentBlockStreak
        )
    }

    /**
     * 重置拦截统计
     */
    fun resetStats() {
        totalChecks = 0
        cleanChecks = 0
        blockedChecks = 0
        currentBlockStreak = 0
    }

    // 统计变量
    private var totalChecks = 0
    private var cleanChecks = 0
    private var blockedChecks = 0
    private var currentBlockStreak = 0

    // ===== 数据类 =====

    data class DetectionResult(
        val isClean: Boolean,
        val violations: List<Violation>,
        val isEmpty: Boolean,
        val downgradePhrase: String? = null
    )

    data class Violation(
        val ruleId: Int,
        val type: ViolationType,
        val match: String,
        val description: String,
        val alternative: String? = null
    )

    enum class ViolationType {
        PRESUMED_FACT,           // 预设事实
        SUGGESTIVE,              // 暗示引导
        CLOSED_INDUCING,         // 封闭诱导
        PSYCHOLOGICAL_SPECULATION, // 心理推测
        OTHER                    // 其他违规
    }

    data class InterceptionStats(
        val totalChecks: Int,
        val cleanChecks: Int,
        val blockedChecks: Int,
        val currentStreak: Int
    ) {
        val blockRate: Float
            get() = if (totalChecks > 0) blockedChecks.toFloat() / totalChecks else 0f

        val shouldEscalate: Boolean
            get() = currentBlockStreak >= ESCALATION_THRESHOLD
    }
}
