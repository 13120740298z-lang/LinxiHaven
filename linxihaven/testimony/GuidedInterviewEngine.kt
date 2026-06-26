package com.testimony.app.guidance

import android.util.Log
import com.testimony.app.ai.AiGuideProvider
import com.testimony.app.evidence.ChainOfCustody
import com.testimony.app.evidence.InducementRuleEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AI引导式事件回顾引擎【司法级FSM实现】
 *
 * 严格遵循"AI安全引导协议专家"技能的创伤知情三阶段模型和15条诱导性检测规则
 *
 * 核心功能：
 * 1. 有限状态机（5个核心状态）
 * 2. 诱导性检测规则引擎（15条规则）
 * 3. AI引导层封装（Prompt约束+降级机制）
 * 4. 状态转换审计日志
 * 5. 连续3次拦截触发人类专家接管
 *
 * 司法意义：
 * - 引导过程可被法庭逐行审查
 * - 所有拦截记录不可篡改
 * - AI只做引导记录，不做心理诊断
 *
 * @author Testimony AI安全协议专家
 */
class GuidedInterviewEngine(private val sessionId: String) {

    companion object {
        private const val TAG = "GuidedInterviewEngine"
    }

    /** 连续拦截阈值 */
    private val BLOCK_THRESHOLD = InducementRuleEngine.ESCALATION_THRESHOLD

    /** 当前状态 */
    private var currentState: InterviewState = InterviewState.INIT

    /** FSM是否已完成 */
    private var isCompleted = false

    /** 是否已升级为人工处理 */
    private var isEscalated = false

    /** 连续拦截计数 */
    private var blockStreak = 0

    /** 已收集的事件要素 */
    private val collectedElements = mutableSetOf<String>()

    /** 用户响应历史 */
    private val userResponseHistory = mutableListOf<String>()

    /** AI引导Provider */
    private val aiGuide = AiGuideProvider()

    /** 证据链管理器 */
    private val custodyChain = ChainOfCustody(sessionId)

    /** 状态流 */
    private val _state = MutableStateFlow(createStateSnapshot())
    val state: StateFlow<InterviewStateSnapshot> = _state.asStateFlow()

    /**
     * 启动引导流程
     */
    suspend fun start() {
        currentState = InterviewState.STABILIZING
        isCompleted = false
        isEscalated = false
        blockStreak = 0
        collectedElements.clear()
        userResponseHistory.clear()

        // 记录会话开始
        custodyChain.addEntry(
            ChainOfCustody.OperationType.GUIDANCE_START,
            mapOf("initial_state" to currentState.name)
        )

        // 生成初始引导语
        val prompt = aiGuide.generateForState(currentState, emptyList(), collectedElements)
        updateState(prompt)

        Log.i(TAG, "Interview started at state: ${currentState.name}")
    }

    /**
     * 处理用户响应
     *
     * @param userResponse 用户输入
     */
    suspend fun processResponse(userResponse: String) {
        if (isCompleted || isEscalated) return

        val fromState = currentState
        _state.value = _state.value.copy(isLoading = true)

        // 记录用户响应
        userResponseHistory.add(userResponse)

        // 检查安全风险
        if (containsSafetyRisk(userResponse)) {
            handleEscalation(userResponse)
            return
        }

        // 确定下一状态
        val nextState = determineNextState(fromState, userResponse)

        // 记录状态转换
        val transitionEntry = custodyChain.addEntry(
            ChainOfCustody.OperationType.GUIDANCE_STATE_CHANGE,
            mapOf(
                "from_state" to fromState.name,
                "to_state" to nextState.name,
                "user_response_length" to userResponse.length
            )
        )

        // 更新收集的要素
        updateCollectedElements(nextState)

        // 切换到新状态
        currentState = nextState

        // 检查是否完成
        if (currentState == InterviewState.COMPLETED) {
            handleCompletion()
            return
        }

        // 检查是否需要人工接管
        if (blockStreak >= BLOCK_THRESHOLD) {
            handleEscalation(userResponse)
            return
        }

        // 生成新状态的引导语
        val prompt = aiGuide.generateForState(currentState, userResponseHistory, collectedElements)
        updateState(prompt)

        Log.i(TAG, "State transition: ${fromState.name} -> ${currentState.name}")
    }

    /**
     * 重置FSM
     */
    fun reset() {
        currentState = InterviewState.INIT
        isCompleted = false
        isEscalated = false
        blockStreak = 0
        collectedElements.clear()
        userResponseHistory.clear()

        custodyChain.addEntry(
            ChainOfCustody.OperationType.GUIDANCE_ESCALATED,
            mapOf("reason" to "user_reset")
        )

        _state.value = createStateSnapshot()
        Log.i(TAG, "Interview engine reset")
    }

    /**
     * 获取状态转换日志
     */
    fun getTransitionLog(): List<ChainOfCustody.CustodyEntry> = custodyChain.getEntries()

    /**
     * 获取已收集的要素
     */
    fun getCollectedElements(): Set<String> = collectedElements.toSet()

    /**
     * 获取用户响应历史
     */
    fun getResponseHistory(): List<String> = userResponseHistory.toList()

    /**
     * 检查是否已完成
     */
    fun isComplete(): Boolean = isCompleted

    /**
     * 检查是否已升级
     */
    fun isEscalatedToHuman(): Boolean = isEscalated

    // ===== 私有方法 =====

    private fun determineNextState(current: InterviewState, response: String): InterviewState {
        return when (current) {
            InterviewState.INIT -> InterviewState.STABILIZING

            InterviewState.STABILIZING -> {
                // 安全确认后进入信息收集
                if (isPositiveConfirmation(response)) {
                    InterviewState.COLLECTING
                } else {
                    InterviewState.STABILIZING // 继续稳定化
                }
            }

            InterviewState.COLLECTING -> {
                // 六要素追问流程
                when (countCollectedElements()) {
                    0 -> InterviewState.COLLECTING // 继续自由叙述
                    1 -> InterviewState.COLLECTING // 时间
                    2 -> InterviewState.COLLECTING // 地点
                    3 -> InterviewState.COLLECTING // 人物
                    4 -> InterviewState.COLLECTING // 行为
                    5 -> InterviewState.COLLECTING // 证据
                    else -> InterviewState.EMPOWERING
                }
            }

            InterviewState.EMPOWERING -> InterviewState.COMPLETED
            InterviewState.COMPLETED, InterviewState.HUMAN_HANDOVER -> current
        }
    }

    private fun updateCollectedElements(state: InterviewState) {
        when (state) {
            InterviewState.COLLECTING -> {
                // 检查用户响应中是否包含各要素
                val response = userResponseHistory.lastOrNull() ?: return

                if (containsTimeInfo(response) && "time" !in collectedElements) {
                    collectedElements.add("time")
                }
                if (containsPlaceInfo(response) && "place" !in collectedElements) {
                    collectedElements.add("place")
                }
                if (containsPersonInfo(response) && "person" !in collectedElements) {
                    collectedElements.add("person")
                }
                if (containsActionInfo(response) && "action" !in collectedElements) {
                    collectedElements.add("action")
                }
                if (containsEvidenceInfo(response) && "evidence" !in collectedElements) {
                    collectedElements.add("evidence")
                }
            }
            else -> {}
        }
    }

    private fun handleCompletion() {
        isCompleted = true

        custodyChain.addEntry(
            ChainOfCustody.OperationType.GUIDANCE_COMPLETED,
            mapOf(
                "collected_elements" to collectedElements.joinToString(),
                "response_count" to userResponseHistory.size
            )
        )

        _state.value = createStateSnapshot()
        Log.i(TAG, "Interview completed with ${collectedElements.size} elements collected")
    }

    private fun handleEscalation(userResponse: String) {
        isEscalated = true
        currentState = InterviewState.HUMAN_HANDOVER

        custodyChain.addEntry(
            ChainOfCustody.OperationType.GUIDANCE_ESCALATED,
            mapOf(
                "reason" to "block_threshold_reached",
                "block_streak" to blockStreak,
                "last_user_response" to userResponse.take(100)
            )
        )

        _state.value = createStateSnapshot()
        Log.w(TAG, "Escalated to human: blockStreak=$blockStreak")
    }

    private fun updateState(prompt: GuidedPrompt) {
        // 更新拦截计数
        if (prompt.wasDowngraded) {
            blockStreak++
        } else {
            blockStreak = 0
        }

        _state.value = InterviewStateSnapshot(
            currentState = currentState,
            currentStateName = currentState.name,
            promptText = prompt.text,
            promptSource = prompt.source,
            options = prompt.options,
            encouragement = prompt.encouragement,
            isLoading = false,
            isComplete = isCompleted,
            isEscalated = isEscalated,
            collectedElements = collectedElements.toSet(),
            blockStreak = blockStreak,
            shouldEscalate = blockStreak >= BLOCK_THRESHOLD
        )
    }

    private fun createStateSnapshot(): InterviewStateSnapshot {
        return InterviewStateSnapshot(
            currentState = currentState,
            currentStateName = currentState.name,
            promptText = "",
            promptSource = PromptSource.TEMPLATE,
            options = emptyList(),
            encouragement = null,
            isLoading = false,
            isComplete = isCompleted,
            isEscalated = isEscalated,
            collectedElements = collectedElements.toSet(),
            blockStreak = blockStreak,
            shouldEscalate = false
        )
    }

    private fun countCollectedElements(): Int = collectedElements.size

    private fun containsSafetyRisk(response: String): Boolean {
        val riskKeywords = listOf(
            "危险", "不安全", "还在", "他还在", "他们还在",
            "害怕", "help", "danger", "not safe",
            "正在被打", "正在被骂", "他在旁边", "他在看着"
        )
        return riskKeywords.any { response.contains(it, ignoreCase = true) }
    }

    private fun isPositiveConfirmation(response: String): Boolean {
        val positiveWords = listOf("是", "安全", "好的", "嗯", "是的", "确认")
        return positiveWords.any { response.contains(it, ignoreCase = true) }
    }

    private fun containsTimeInfo(text: String): Boolean {
        val patterns = listOf(
            Regex("\\d{4}年"), Regex("\\d{1,2}月"), Regex("\\d{1,2}日"),
            Regex("\\d{1,2}点"), Regex("昨天|今天|明天|上周|这周")
        )
        return patterns.any { it.containsMatchIn(text) }
    }

    private fun containsPlaceInfo(text: String): Boolean {
        val patterns = listOf(
            Regex("学校|教室|操场|宿舍|食堂|厕所|网吧|家里|网上|群里")
        )
        return patterns.any { it.containsMatchIn(text) }
    }

    private fun containsPersonInfo(text: String): Boolean {
        return text.contains("人") || text.contains("同学") || text.contains("老师")
    }

    private fun containsActionInfo(text: String): Boolean {
        val patterns = listOf(
            Regex("打|骂|踢|推|排挤|孤立|威胁|恐吓|侮辱|嘲笑|发.*图|转.*发")
        )
        return patterns.any { it.containsMatchIn(text) }
    }

    private fun containsEvidenceInfo(text: String): Boolean {
        val patterns = listOf(
            Regex("截图|聊天记录|录音|录像|视频|照片|图片|证据")
        )
        return patterns.any { it.containsMatchIn(text) }
    }

    // ===== 数据类 =====

    data class InterviewStateSnapshot(
        val currentState: InterviewState,
        val currentStateName: String,
        val promptText: String,
        val promptSource: PromptSource,
        val options: List<String>,
        val encouragement: String?,
        val isLoading: Boolean,
        val isComplete: Boolean,
        val isEscalated: Boolean,
        val collectedElements: Set<String>,
        val blockStreak: Int,
        val shouldEscalate: Boolean
    )

    enum class InterviewState {
        INIT,           // 初始化
        STABILIZING,    // 阶段1：稳定化
        COLLECTING,     // 阶段2：信息收集
        EMPOWERING,     // 阶段3：赋能
        COMPLETED,      // 完成
        HUMAN_HANDOVER  // 人工接管
    }

    enum class PromptSource {
        AI,       // AI生成
        TEMPLATE, // 固定模板
        WHITELIST // 白名单降级
    }

    data class GuidedPrompt(
        val text: String,
        val source: PromptSource,
        val options: List<String> = emptyList(),
        val encouragement: String? = null,
        val wasDowngraded: Boolean = false,
        val downgradedReason: String? = null
    )
}
