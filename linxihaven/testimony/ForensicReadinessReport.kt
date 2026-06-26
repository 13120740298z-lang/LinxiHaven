package com.testimony.app.evidence

import com.testimony.app.BuildConfig

/**
 * 司法采纳准备度报告【证据架构师核心输出】
 *
 * 根据 SPECS.md 评测标准，评估项目的司法采纳准备度
 *
 * 评测维度：
 * 1. 证据链完整度
 * 2. 时间抗篡改能力
 * 3. AI引导合规性
 * 4. 隐私隔离有效性
 *
 * @author Testimony司法架构师
 */
object ForensicReadinessReport {

    /**
     * 生成完整的司法采纳准备度报告
     */
    fun generateReport(config: ReadinessConfig): ReadinessReport {
        return ReadinessReport(
            generatedAt = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            evidenceChainReport = assessEvidenceChain(config),
            timeAnchoringReport = assessTimeAnchoring(config),
            aiGuidanceReport = assessAiGuidance(config),
            privacyIsolationReport = assessPrivacyIsolation(config),
            overallScore = calculateOverallScore(config)
        )
    }

    /**
     * 评估证据链完整度
     */
    private fun assessEvidenceChain(config: ReadinessConfig): EvidenceChainReport {
        val checks = listOf(
            ChecklistItem(
                name = "录屏文件含PTS",
                passed = config.hasScreenRecording,
                requirement = "FR3.1"
            ),
            ChecklistItem(
                name = "触摸事件日志",
                passed = config.hasOperationLog,
                requirement = "FR3.2"
            ),
            ChecklistItem(
                name = "传感器数据日志",
                passed = config.hasSensorLog,
                requirement = "FR3.3"
            ),
            ChecklistItem(
                name = "多源时间戳序列",
                passed = config.hasMultiSourceTimestamp,
                requirement = "FR3.4"
            ),
            ChecklistItem(
                name = "默克尔树根哈希",
                passed = config.hasMerkleTree,
                requirement = "FR3.5"
            ),
            ChecklistItem(
                name = "证据包加密压缩",
                passed = config.hasEncryptedZip,
                requirement = "FR3.6"
            ),
            ChecklistItem(
                name = "哈希链完整性",
                passed = config.hasHashChain,
                requirement = "数字取证标准"
            ),
            ChecklistItem(
                name = "链式验证可执行",
                passed = config.canVerifyChain,
                requirement = "独立验证要求"
            )
        )

        val passedCount = checks.count { it.passed }
        val totalCount = checks.size
        val completionRate = passedCount.toFloat() / totalCount

        return EvidenceChainReport(
            checklist = checks,
            completionRate = completionRate,
            isComplete = completionRate >= 1.0f,
            issues = checks.filter { !it.passed }.map { it.name }
        )
    }

    /**
     * 评估时间抗篡改能力
     */
    private fun assessTimeAnchoring(config: ReadinessConfig): TimeAnchoringReport {
        val checks = listOf(
            ChecklistItem(
                name = "系统时间源",
                passed = config.hasSystemTime,
                requirement = "基础时间源"
            ),
            ChecklistItem(
                name = "NTP时间源",
                passed = config.hasNtpTime,
                requirement = "网络时间同步"
            ),
            ChecklistItem(
                name = "基站时间源",
                passed = config.hasCellTowerTime,
                requirement = "运营商同步"
            ),
            ChecklistItem(
                name = "区块链时间源",
                passed = config.hasBlockchainTime,
                requirement = "不可篡改锚定"
            ),
            ChecklistItem(
                name = "四源并发获取",
                passed = config.hasConcurrentFetch,
                requirement = "总超时5秒"
            ),
            ChecklistItem(
                name = "融合算法实现",
                passed = config.hasFusionAlgorithm,
                requirement = "中位数+共识"
            ),
            ChecklistItem(
                name = "偏差阈值设置",
                passed = config.hasDeviationThreshold,
                requirement = "±60秒标记异常"
            ),
            ChecklistItem(
                name = "单调时钟对齐",
                passed = config.hasMonotonicClock,
                requirement = "录屏日志同步"
            )
        )

        val passedCount = checks.count { it.passed }
        val totalCount = checks.size
        val completionRate = passedCount.toFloat() / totalCount

        return TimeAnchoringReport(
            checklist = checks,
            completionRate = completionRate,
            isResistant = completionRate >= 0.875f,
            fourSourceDeviationThreshold = config.deviationThresholdMs,
            issues = checks.filter { !it.passed }.map { it.name }
        )
    }

    /**
     * 评估AI引导合规性
     */
    private fun assessAiGuidance(config: ReadinessConfig): AiGuidanceReport {
        val checks = listOf(
            ChecklistItem(
                name = "FSM状态机实现",
                passed = config.hasFsm,
                requirement = "5个核心状态"
            ),
            ChecklistItem(
                name = "诱导性检测15条",
                passed = config.has15Rules,
                requirement = "规则引擎AST实现"
            ),
            ChecklistItem(
                name = "白名单语料库",
                passed = config.hasWhitelistPrompts,
                requirement = "法律审核固定语"
            ),
            ChecklistItem(
                name = "连续3次拦截触发",
                passed = config.hasEscalationTrigger,
                requirement = "人类专家接管"
            ),
            ChecklistItem(
                name = "状态转换日志",
                passed = config.hasTransitionLog,
                requirement = "哈希链绑定"
            ),
            ChecklistItem(
                name = "结构化JSON输出",
                passed = config.hasStructuredOutput,
                requirement = "可审计格式"
            ),
            ChecklistItem(
                name = "Prompt角色约束",
                passed = config.hasPromptConstraint,
                requirement = "引导记录员角色"
            ),
            ChecklistItem(
                name = "降级白名单执行",
                passed = config.hasDowngradeMechanism,
                requirement = "拦截后降级"
            )
        )

        val passedCount = checks.count { it.passed }
        val totalCount = checks.size
        val completionRate = passedCount.toFloat() / totalCount

        // 计算拦截率
        val interceptionRate = if (config.totalAiCalls > 0) {
            config.blockedAiCalls.toFloat() / config.totalAiCalls
        } else 0f

        val downgradeRate = if (config.totalAiCalls > 0) {
            config.downgradedAiCalls.toFloat() / config.totalAiCalls
        } else 0f

        return AiGuidanceReport(
            checklist = checks,
            completionRate = completionRate,
            isCompliant = completionRate >= 1.0f,
            interceptionRate = interceptionRate,
            downgradeRate = downgradeRate,
            issues = checks.filter { !it.passed }.map { it.name }
        )
    }

    /**
     * 评估隐私隔离有效性
     */
    private fun assessPrivacyIsolation(config: ReadinessConfig): PrivacyIsolationReport {
        val checks = listOf(
            ChecklistItem(
                name = "跨域访问阻断",
                passed = config.hasCrossDomainBlock,
                requirement = "Rule1"
            ),
            ChecklistItem(
                name = "派生数据泄漏检测",
                passed = config.hasDerivedDataDetection,
                requirement = "Rule2"
            ),
            ChecklistItem(
                name = "日志脱敏验证",
                passed = config.hasLogSanitization,
                requirement = "Rule3"
            ),
            ChecklistItem(
                name = "覆盖擦除实现",
                passed = config.hasSecureDeletion,
                requirement = "Rule4"
            ),
            ChecklistItem(
                name = "权限最小化审查",
                passed = config.hasMinPermission,
                requirement = "Rule5"
            ),
            ChecklistItem(
                name = "匿名化效果验证",
                passed = config.hasAnonymization,
                requirement = "Rule6"
            ),
            ChecklistItem(
                name = "家长端不可查学生原始数据",
                passed = config.parentCannotAccessStudentData,
                requirement = "完全隔离"
            ),
            ChecklistItem(
                name = "学生数据完全自主控制",
                passed = config.studentHasDataControl,
                requirement = "查看/删除/分享"
            )
        )

        val passedCount = checks.count { it.passed }
        val totalCount = checks.size
        val completionRate = passedCount.toFloat() / totalCount

        return PrivacyIsolationReport(
            checklist = checks,
            completionRate = completionRate,
            isEffective = completionRate >= 1.0f,
            crossDomainAccessDetectionRate = config.crossDomainAccessDetectionRate,
            issues = checks.filter { !it.passed }.map { it.name }
        )
    }

    /**
     * 计算总体评分
     */
    private fun calculateOverallScore(config: ReadinessConfig): OverallScore {
        val evidenceChainReport = assessEvidenceChain(config)
        val timeAnchoringReport = assessTimeAnchoring(config)
        val aiGuidanceReport = assessAiGuidance(config)
        val privacyIsolationReport = assessPrivacyIsolation(config)

        val weights = mapOf(
            "evidenceChain" to 0.3f,
            "timeAnchoring" to 0.25f,
            "aiGuidance" to 0.25f,
            "privacyIsolation" to 0.2f
        )

        val weightedScore = (
            evidenceChainReport.completionRate * weights["evidenceChain"]!! +
            timeAnchoringReport.completionRate * weights["timeAnchoring"]!! +
            aiGuidanceReport.completionRate * weights["aiGuidance"]!! +
            privacyIsolationReport.completionRate * weights["privacyIsolation"]!!
        )

        val adoptionLevel = when {
            weightedScore >= 0.95f -> AdoptionLevel.COURT_READY
            weightedScore >= 0.85f -> AdoptionLevel.SCHOOL_READY
            weightedScore >= 0.70f -> AdoptionLevel.PILOT_READY
            weightedScore >= 0.50f -> AdoptionLevel.DEVELOPMENT
            else -> AdoptionLevel.INITIAL
        }

        return OverallScore(
            totalScore = weightedScore,
            adoptionLevel = adoptionLevel,
            evidenceChainScore = evidenceChainReport.completionRate,
            timeAnchoringScore = timeAnchoringReport.completionRate,
            aiGuidanceScore = aiGuidanceReport.completionRate,
            privacyIsolationScore = privacyIsolationReport.completionRate
        )
    }

    // ===== 数据类 =====

    data class ReadinessConfig(
        // 证据链
        val hasScreenRecording: Boolean = true,
        val hasOperationLog: Boolean = true,
        val hasSensorLog: Boolean = true,
        val hasMultiSourceTimestamp: Boolean = true,
        val hasMerkleTree: Boolean = true,
        val hasEncryptedZip: Boolean = true,
        val hasHashChain: Boolean = true,
        val canVerifyChain: Boolean = true,

        // 时间锚定
        val hasSystemTime: Boolean = true,
        val hasNtpTime: Boolean = true,
        val hasCellTowerTime: Boolean = true,
        val hasBlockchainTime: Boolean = true,
        val hasConcurrentFetch: Boolean = true,
        val hasFusionAlgorithm: Boolean = true,
        val hasDeviationThreshold: Boolean = true,
        val hasMonotonicClock: Boolean = true,
        val deviationThresholdMs: Long = 60_000L,

        // AI引导
        val hasFsm: Boolean = true,
        val has15Rules: Boolean = true,
        val hasWhitelistPrompts: Boolean = true,
        val hasEscalationTrigger: Boolean = true,
        val hasTransitionLog: Boolean = true,
        val hasStructuredOutput: Boolean = true,
        val hasPromptConstraint: Boolean = true,
        val hasDowngradeMechanism: Boolean = true,
        val totalAiCalls: Int = 0,
        val blockedAiCalls: Int = 0,
        val downgradedAiCalls: Int = 0,

        // 隐私隔离
        val hasCrossDomainBlock: Boolean = true,
        val hasDerivedDataDetection: Boolean = true,
        val hasLogSanitization: Boolean = true,
        val hasSecureDeletion: Boolean = true,
        val hasMinPermission: Boolean = true,
        val hasAnonymization: Boolean = true,
        val parentCannotAccessStudentData: Boolean = true,
        val studentHasDataControl: Boolean = true,
        val crossDomainAccessDetectionRate: Float = 1.0f
    )

    data class ReadinessReport(
        val generatedAt: Long,
        val appVersion: String,
        val evidenceChainReport: EvidenceChainReport,
        val timeAnchoringReport: TimeAnchoringReport,
        val aiGuidanceReport: AiGuidanceReport,
        val privacyIsolationReport: PrivacyIsolationReport,
        val overallScore: OverallScore
    )

    data class EvidenceChainReport(
        val checklist: List<ChecklistItem>,
        val completionRate: Float,
        val isComplete: Boolean,
        val issues: List<String>
    )

    data class TimeAnchoringReport(
        val checklist: List<ChecklistItem>,
        val completionRate: Float,
        val isResistant: Boolean,
        val fourSourceDeviationThreshold: Long,
        val issues: List<String>
    )

    data class AiGuidanceReport(
        val checklist: List<ChecklistItem>,
        val completionRate: Float,
        val isCompliant: Boolean,
        val interceptionRate: Float,
        val downgradeRate: Float,
        val issues: List<String>
    )

    data class PrivacyIsolationReport(
        val checklist: List<ChecklistItem>,
        val completionRate: Float,
        val isEffective: Boolean,
        val crossDomainAccessDetectionRate: Float,
        val issues: List<String>
    )

    data class OverallScore(
        val totalScore: Float,
        val adoptionLevel: AdoptionLevel,
        val evidenceChainScore: Float,
        val timeAnchoringScore: Float,
        val aiGuidanceScore: Float,
        val privacyIsolationScore: Float
    )

    data class ChecklistItem(
        val name: String,
        val passed: Boolean,
        val requirement: String
    )

    enum class AdoptionLevel {
        INITIAL,       // 初始阶段
        DEVELOPMENT,   // 开发阶段
        PILOT_READY,   // 试点就绪
        SCHOOL_READY,  // 学校可用
        COURT_READY    // 法庭采纳级
    }
}
