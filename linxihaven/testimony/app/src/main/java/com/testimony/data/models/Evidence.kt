package com.testimony.data.models

import com.testimony.util.Constants
import com.testimony.util.generateUUID
import com.testimony.util.toReadableTimestamp

/**
 * Evidence Package Data Model
 */
data class EvidencePackage(
    val id: String = generateUUID(),
    val createdAt: Long = System.currentTimeMillis(),
    val createdAtReadable: String = createdAt.toReadableTimestamp(),
    val timestampSources: List<TimestampSource> = emptyList(),
    val screenRecordingPath: String? = null,
    val operationLogPath: String? = null,
    val sensorLogPath: String? = null,
    val merkleRootHash: String,
    val location: LocationData? = null,
    val encryption: EncryptionMetadata,
    val status: EvidenceStatus = EvidenceStatus.GENERATED,
    val sharedWith: List<ShareTarget> = emptyList()
)

data class TimestampSource(
    val source: String,
    val timestamp: Long,
    val isVerified: Boolean = true
)

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val address: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class EncryptionMetadata(
    val algorithm: String = "AES-256-GCM",
    val keyId: String,
    val encryptedAt: Long = System.currentTimeMillis()
)

enum class EvidenceStatus {
    GENERATED,
    ENCRYPTED,
    STORED_LOCALLY,
    SHARED_WITH_SCHOOL,
    SHARED_WITH_PARENT,
    ARCHIVED,
    DELETED
}

enum class ShareTarget {
    LOCAL_ONLY,
    SCHOOL,
    PARENT
}

/**
 * Guided Interview Session Data Model
 */
data class GuidedInterviewSession(
    val sessionId: String = generateUUID(),
    val evidenceId: String? = null,
    val currentState: InterviewState = InterviewState.CONFIRM_SAFETY,
    val collectedFacts: Map<EventElement, String> = emptyMap(),
    val fsmLog: List<FSMTransition> = emptyList(),
    val emotionalRisk: RiskLevel = RiskLevel.GREEN,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val userMessages: List<ChatMessage> = emptyList(),
    val aiMessages: List<ChatMessage> = emptyList()
)

enum class InterviewState {
    CONFIRM_SAFETY,       // 状态1：确认环境安全
    FREE_NARRATION,       // 状态2：自由叙述阶段
    FACT_ELEMENTS_INQUIRY, // 状态3：缺失要素追问
    EVIDENCE_DISPLAY,     // 状态4：证据展示引导
    SUPPLEMENTARY_STATEMENT, // 状态5：补充陈述
    COMPLETED             // 完成
}

enum class EventElement(val displayName: String, val description: String) {
    TIME("时间", "事件发生的时间"),
    LOCATION("地点", "事件发生的地点"),
    PERSONS("人物", "涉及的人物"),
    BEHAVIOR("行为", "具体发生了什么"),
    EVIDENCE("证据载体", "有哪些证据"),
    CONSEQUENCE("后果", "造成了什么影响")
}

enum class RiskLevel {
    GREEN,  // 低风险
    YELLOW, // 中风险
    RED     // 高风险，需要关注
}

data class FSMTransition(
    val fromState: InterviewState,
    val toState: InterviewState,
    val timestamp: Long = System.currentTimeMillis(),
    val trigger: TransitionTrigger,
    val userInput: String? = null,
    val coveredElements: List<EventElement> = emptyList(),
    val emotionalIndicators: List<String> = emptyList()
)

enum class TransitionTrigger {
    USER_READY,
    USER_COMPLETE,
    ELEMENTS_PARTIAL,
    ELEMENTS_COMPLETE,
    EVIDENCE_SHOWN,
    USER_REQUEST_END,
    SAFETY_CONCERN,
    TIME_OUT
}

data class ChatMessage(
    val id: String = generateUUID(),
    val content: String,
    val isFromAI: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val state: InterviewState,
    val isSafetyAlert: Boolean = false
)

/**
 * Parent Observation Data Model
 */
data class Observation(
    val id: String = generateUUID(),
    val parentId: String,
    val observedAt: Long = System.currentTimeMillis(),
    val content: String, // 自然语言描述
    val childAge: Int? = null,
    val context: String? = null // 学校日/周末/假期等
)

data class RiskAssessment(
    val id: String = generateUUID(),
    val observationId: String,
    val level: RiskLevel,
    val possibleStressors: List<String>,
    val confidence: Float, // 0.0 - 1.0
    val suggestedScript: String,
    val generatedAt: Long = System.currentTimeMillis(),
    val modelVersion: String = "v1.0"
)

data class CommunicationScript(
    val id: String = generateUUID(),
    val assessmentId: String,
    val script: String,
    val suggestedActions: List<String>,
    val expectedOutcome: String,
    val usageGuidance: String,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * User Profile
 */
data class UserProfile(
    val id: String = generateUUID(),
    val role: UserRole,
    val nickname: String? = null,
    val age: Int? = null,
    val linkedParentId: String? = null, // For students
    val linkedStudentIds: List<String> = emptyList(), // For parents
    val preferences: UserPreferences = UserPreferences(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class UserRole {
    STUDENT,
    PARENT,
    SCHOOL_ADMIN,
    COUNSELOR
}

data class UserPreferences(
    val theme: String = "light",
    val notificationsEnabled: Boolean = true,
    val aiTone: String = "gentle" // gentle, neutral, professional
)
