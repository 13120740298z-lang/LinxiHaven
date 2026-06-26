package com.testimony.ai

import com.testimony.data.models.*
import com.testimony.util.generateUUID

/**
 * Guided Interview Finite State Machine
 * Implements the 5-state interview flow with audit logging
 */
class GuidedInterviewFSM {

    private var currentState: InterviewState = InterviewState.CONFIRM_SAFETY
    private val stateHistory = mutableListOf<FSMTransition>()
    private val collectedElements = mutableMapOf<EventElement, String>()

    // Safety monitoring
    private var emotionalRiskCount = 0
    private var lastEmotionalIndicator: String? = null

    /**
     * Start new interview session
     */
    fun startSession(): GuidedInterviewSession {
        currentState = InterviewState.CONFIRM_SAFETY
        stateHistory.clear()
        collectedElements.clear()
        emotionalRiskCount = 0

        logTransition(
            fromState = InterviewState.CONFIRM_SAFETY,
            toState = InterviewState.CONFIRM_SAFETY,
            trigger = TransitionTrigger.USER_READY
        )

        return buildCurrentSession()
    }

    /**
     * Process user input and determine state transition
     */
    fun processUserInput(input: String): FSMResult {
        // Check for emotional risk
        val emotionalIndicators = detectEmotionalRisk(input)

        // Extract event elements from input
        val newlyExtracted = extractElements(input)

        // Determine transition based on current state and input
        val (nextState, trigger) = determineNextState(input, newlyExtracted)

        // Log transition
        logTransition(
            fromState = currentState,
            toState = nextState,
            trigger = trigger,
            userInput = input,
            coveredElements = newlyExtracted,
            emotionalIndicators = emotionalIndicators
        )

        currentState = nextState

        // Generate AI response prompt
        val response = generateAIResponse(nextState, newlyExtracted)

        return FSMResult(
            nextState = nextState,
            aiPrompt = response,
            coveredElements = collectedElements.toMap(),
            missingElements = getMissingElements(),
            emotionalRisk = calculateOverallRisk(),
            isSafetyAlert = emotionalIndicators.isNotEmpty(),
            fsmLog = stateHistory.toList()
        )
    }

    /**
     * Manually trigger state transition (e.g., user clicks "Next")
     */
    fun advanceState(trigger: TransitionTrigger): FSMResult {
        val nextState = getNextStateForTrigger(trigger)

        logTransition(
            fromState = currentState,
            toState = nextState,
            trigger = trigger
        )

        currentState = nextState

        val response = generateAIResponse(nextState, emptyList())

        return FSMResult(
            nextState = nextState,
            aiPrompt = response,
            coveredElements = collectedElements.toMap(),
            missingElements = getMissingElements(),
            emotionalRisk = calculateOverallRisk(),
            isSafetyAlert = emotionalRiskCount > 0,
            fsmLog = stateHistory.toList()
        )
    }

    /**
     * Complete the interview
     */
    fun completeSession(): GuidedInterviewSession {
        logTransition(
            fromState = currentState,
            toState = InterviewState.COMPLETED,
            trigger = TransitionTrigger.USER_REQUEST_END
        )

        currentState = InterviewState.COMPLETED

        return buildCurrentSession().copy(
            endTime = System.currentTimeMillis()
        )
    }

    /**
     * Detect emotional risk indicators in input
     */
    private fun detectEmotionalRisk(input: String): List<String> {
        val detected = mutableListOf<String>()

        // Check high-risk keywords
        for (keyword in PromptTemplates.HIGH_RISK_KEYWORDS) {
            if (input.contains(keyword, ignoreCase = true)) {
                detected.add(keyword)
                emotionalRiskCount++
            }
        }

        // Check emotion indicators
        for ((emotion, keywords) in PromptTemplates.EMOTION_INDICATORS) {
            for (keyword in keywords) {
                if (input.contains(keyword, ignoreCase = true)) {
                    detected.add("$emotion:$keyword")
                }
            }
        }

        if (detected.isNotEmpty()) {
            lastEmotionalIndicator = detected.first()
        }

        return detected
    }

    /**
     * Extract event elements from user input
     */
    private fun extractElements(input: String): List<EventElement> {
        val extracted = mutableListOf<EventElement>()

        // Simple keyword-based extraction (in production, use NER/LLM)
        val timeKeywords = listOf("昨天", "今天", "上周", "周一", "晚上", "下午", "早上", "中午",
            "昨天", "today", "yesterday", "morning", "afternoon", "evening")
        val locationKeywords = listOf("学校", "教室", "操场", "食堂", "厕所", "宿舍", "家里",
            "网上", "微信", "QQ", "Discord", "school", "home", "classroom")
        val behaviorKeywords = listOf("骂", "打", "踢", "推", "嘲笑", "孤立", "威胁",
            "侮辱", "punch", "kick", "hit", "mock", "bully", "threat")
        val consequenceKeywords = listOf("难受", "害怕", "哭", "睡不着", "不想上学", "成绩下降",
            "sad", "scared", "cry", "can't sleep", "afraid")

        if (timeKeywords.any { input.contains(it, ignoreCase = true) } && collectedElements[EventElement.TIME].isNullOrEmpty()) {
            collectedElements[EventElement.TIME] = input
            extracted.add(EventElement.TIME)
        }

        if (locationKeywords.any { input.contains(it, ignoreCase = true) } && collectedElements[EventElement.LOCATION].isNullOrEmpty()) {
            collectedElements[EventElement.LOCATION] = input
            extracted.add(EventElement.LOCATION)
        }

        if (behaviorKeywords.any { input.contains(it, ignoreCase = true) } && collectedElements[EventElement.BEHAVIOR].isNullOrEmpty()) {
            collectedElements[EventElement.BEHAVIOR] = input
            extracted.add(EventElement.BEHAVIOR)
        }

        if (consequenceKeywords.any { input.contains(it, ignoreCase = true) } && collectedElements[EventElement.CONSEQUENCE].isNullOrEmpty()) {
            collectedElements[EventElement.CONSEQUENCE] = input
            extracted.add(EventElement.CONSEQUENCE)
        }

        // For PERSONS and EVIDENCE, we need more sophisticated extraction
        // Simplified: if user mentions pronouns like "他们", "她", "他"
        val personPronouns = listOf("他们", "她们", "他", "她", "有人", "everyone", "they", "she", "he")
        if (personPronouns.any { input.contains(it) } && collectedElements[EventElement.PERSONS].isNullOrEmpty()) {
            collectedElements[EventElement.PERSONS] = input
            extracted.add(EventElement.PERSONS)
        }

        val evidenceKeywords = listOf("截图", "照片", "录像", "聊天记录", "证据",
            "screenshot", "photo", "video", "chat", "evidence")
        if (evidenceKeywords.any { input.contains(it, ignoreCase = true) } && collectedElements[EventElement.EVIDENCE].isNullOrEmpty()) {
            collectedElements[EventElement.EVIDENCE] = input
            extracted.add(EventElement.EVIDENCE)
        }

        return extracted
    }

    private fun determineNextState(input: String, extracted: List<EventElement>): Pair<InterviewState, TransitionTrigger> {
        return when (currentState) {
            InterviewState.CONFIRM_SAFETY -> {
                InterviewState.FREE_NARRATION to TransitionTrigger.USER_READY
            }

            InterviewState.FREE_NARRATION -> {
                when {
                    input.contains("说完了", "没有了", "就这样", "完了", "done", "finished") -> {
                        if (getMissingElements().isNotEmpty()) {
                            InterviewState.FACT_ELEMENTS_INQUIRY to TransitionTrigger.ELEMENTS_PARTIAL
                        } else {
                            InterviewState.SUPPLEMENTARY_STATEMENT to TransitionTrigger.ELEMENTS_COMPLETE
                        }
                    }
                    extracted.contains(EventElement.EVIDENCE) -> {
                        InterviewState.EVIDENCE_DISPLAY to TransitionTrigger.EVIDENCE_SHOWN
                    }
                    else -> {
                        InterviewState.FREE_NARRATION to TransitionTrigger.USER_COMPLETE
                    }
                }
            }

            InterviewState.FACT_ELEMENTS_INQUIRY -> {
                if (getMissingElements().isEmpty()) {
                    InterviewState.SUPPLEMENTARY_STATEMENT to TransitionTrigger.ELEMENTS_COMPLETE
                } else {
                    InterviewState.FACT_ELEMENTS_INQUIRY to TransitionTrigger.ELEMENTS_PARTIAL
                }
            }

            InterviewState.EVIDENCE_DISPLAY -> {
                InterviewState.SUPPLEMENTARY_STATEMENT to TransitionTrigger.USER_COMPLETE
            }

            InterviewState.SUPPLEMENTARY_STATEMENT -> {
                InterviewState.COMPLETED to TransitionTrigger.USER_REQUEST_END
            }

            InterviewState.COMPLETED -> {
                InterviewState.COMPLETED to TransitionTrigger.USER_REQUEST_END
            }
        }
    }

    private fun getNextStateForTrigger(trigger: TransitionTrigger): InterviewState {
        return when (trigger) {
            TransitionTrigger.USER_READY -> InterviewState.FREE_NARRATION
            TransitionTrigger.USER_COMPLETE -> InterviewState.FACT_ELEMENTS_INQUIRY
            TransitionTrigger.ELEMENTS_PARTIAL -> InterviewState.FACT_ELEMENTS_INQUIRY
            TransitionTrigger.ELEMENTS_COMPLETE -> InterviewState.SUPPLEMENTARY_STATEMENT
            TransitionTrigger.EVIDENCE_SHOWN -> InterviewState.EVIDENCE_DISPLAY
            TransitionTrigger.USER_REQUEST_END -> InterviewState.COMPLETED
            TransitionTrigger.SAFETY_CONCERN -> InterviewState.COMPLETED
            TransitionTrigger.TIME_OUT -> InterviewState.COMPLETED
        }
    }

    private fun generateAIResponse(state: InterviewState, newlyExtracted: List<EventElement>): String {
        val basePrompt = PromptTemplates.getStatePrompt(state)

        val builder = StringBuilder(basePrompt)

        // Add missing elements info
        if (state == InterviewState.FACT_ELEMENTS_INQUIRY) {
            val missing = getMissingElements()
            if (missing.isNotEmpty()) {
                builder.append("\n\n【还需要了解的信息】\n")
                builder.append(PromptTemplates.generateElementInquiryPrompt(missing))
            }
        }

        // Add extraction acknowledgment
        if (newlyExtracted.isNotEmpty()) {
            builder.append("\n\n【已记录】\n")
            newlyExtracted.forEach { element ->
                builder.append("- ${element.displayName}: 已记录\n")
            }
        }

        return builder.toString()
    }

    private fun getMissingElements(): List<EventElement> {
        return EventElement.entries.filter {
            collectedElements[it].isNullOrEmpty()
        }
    }

    private fun calculateOverallRisk(): RiskLevel {
        return when {
            emotionalRiskCount >= 3 -> RiskLevel.RED
            emotionalRiskCount >= 1 -> RiskLevel.YELLOW
            else -> RiskLevel.GREEN
        }
    }

    private fun logTransition(
        fromState: InterviewState,
        toState: InterviewState,
        trigger: TransitionTrigger,
        userInput: String? = null,
        coveredElements: List<EventElement> = emptyList(),
        emotionalIndicators: List<String> = emptyList()
    ) {
        stateHistory.add(
            FSMTransition(
                fromState = fromState,
                toState = toState,
                timestamp = System.currentTimeMillis(),
                trigger = trigger,
                userInput = userInput,
                coveredElements = coveredElements,
                emotionalIndicators = emotionalIndicators
            )
        )
    }

    private fun buildCurrentSession(): GuidedInterviewSession {
        return GuidedInterviewSession(
            currentState = currentState,
            collectedFacts = collectedElements.toMap(),
            fsmLog = stateHistory.toList(),
            emotionalRisk = calculateOverallRisk()
        )
    }

    fun getCurrentState(): InterviewState = currentState

    fun getSession(): GuidedInterviewSession = buildCurrentSession()

    fun getElementCoverage(): Float {
        val total = EventElement.entries.size
        val covered = collectedElements.count { !it.value.isNullOrEmpty() }
        return covered.toFloat() / total.toFloat()
    }

    data class FSMResult(
        val nextState: InterviewState,
        val aiPrompt: String,
        val coveredElements: Map<EventElement, String>,
        val missingElements: List<EventElement>,
        val emotionalRisk: RiskLevel,
        val isSafetyAlert: Boolean,
        val fsmLog: List<FSMTransition>
    )
}
