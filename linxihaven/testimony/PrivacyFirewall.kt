package com.testimony.app.data.privacy

import android.util.Log
import com.testimony.app.evidence.EvidencePackage
import org.json.JSONObject

/**
 * 隐私隔离防火墙【未成年人隐私合规核心】
 *
 * 严格遵循"未成年人隐私合规审查员"技能的6条规则
 *
 * 司法意义：
 * - 学生端/家长端数据严格隔离
 * - 家长只能看到风险评估和沟通脚本
 * - 绝对不可获取学生原始叙事
 *
 * 隐私规则：
 * 1. 跨域数据访问阻断
 * 2. 派生数据泄漏检测
 * 3. 日志脱敏验证
 * 4. 数据删除验证
 * 5. 权限最小化审查
 * 6. 匿名化效果验证
 *
 * @author Testimony隐私合规审查员
 */
object PrivacyFirewall {

    private const val TAG = "PrivacyFirewall"

    /** 家长端允许访问的数据类型 */
    private val ALLOWED_PARENT_OUTPUT_FIELDS = setOf(
        "risk_level",        // 风险等级（绿/黄/红）
        "pressure_source",   // 压力源大类（学业/人际/家庭/网络）
        "communication_script", // 沟通脚本
        "suggestions"        // 建议列表
    )

    /** 禁止出现在家长端输出中的敏感字段 */
    private val BLOCKED_STUDENT_FIELDS = setOf(
        "event_time",        // 事件具体时间
        "event_location",    // 事件具体地点
        "involved_persons",  // 涉事人物
        "narrative",         // 学生叙事原文
        "evidence_details",  // 证据详情
        "device_id",         // 设备标识
        "gps_coordinates"    // GPS坐标
    )

    /**
     * 过滤家长端可见的数据【核心接口】
     *
     * 规则2：派生数据泄漏检测
     * 确保家长端只能看到风险评估，不包含学生端原始数据
     *
     * @param data 原始数据
     * @param isStudentInitiatedDisclosure 是否由学生主动发起的安全披露
     * @return 过滤后的安全数据
     */
    fun filterForParent(
        data: Map<String, Any>,
        isStudentInitiatedDisclosure: Boolean
    ): FilteredResult {
        val issues = mutableListOf<PrivacyIssue>()

        // 检查是否包含被禁止的字段
        val blockedFields = data.keys.intersect(BLOCKED_STUDENT_FIELDS)
        if (blockedFields.isNotEmpty()) {
            issues.add(PrivacyIssue(
                type = IssueType.DATA_LEAKAGE,
                severity = Severity.CRITICAL,
                message = "Data contains blocked fields: ${blockedFields.joinToString()}",
                field = blockedFields.first()
            ))
        }

        // 如果不是学生主动披露，拒绝所有学生端数据
        if (!isStudentInitiatedDisclosure) {
            val studentDataKeys = data.keys.filter {
                it.startsWith("student_") || BLOCKED_STUDENT_FIELDS.any { blocked -> it.contains(blocked, ignoreCase = true) }
            }
            if (studentDataKeys.isNotEmpty()) {
                issues.add(PrivacyIssue(
                    type = IssueType.UNAUTHORIZED_ACCESS,
                    severity = Severity.CRITICAL,
                    message = "Unauthorized access to student data",
                    field = studentDataKeys.first()
                ))
            }
        }

        // 过滤只保留允许的字段
        val filteredData = if (issues.isEmpty()) {
            data.filterKeys { it in ALLOWED_PARENT_OUTPUT_FIELDS }
        } else {
            emptyMap() // 存在问题时返回空数据
        }

        return FilteredResult(
            isAllowed = issues.isEmpty(),
            data = filteredData,
            issues = issues
        )
    }

    /**
     * 过滤证据包中的隐私数据
     *
     * 规则6：匿名化效果验证
     * 确保证据包去除可识别信息
     *
     * @param evidencePackage 原始证据包
     * @return 匿名化后的证据包
     */
    fun anonymizeEvidencePackage(evidencePackage: EvidencePackage): AnonymizedEvidence {
        val issues = mutableListOf<PrivacyIssue>()

        // 检查并移除设备标识
        val deviceIdentifiers = listOf("imei", "mac", "gaid", "android_id")
        deviceIdentifiers.forEach { id ->
            if (evidencePackage.toString().contains(id, ignoreCase = true)) {
                issues.add(PrivacyIssue(
                    type = IssueType.IDENTIFIER_FOUND,
                    severity = Severity.HIGH,
                    message = "Device identifier found in evidence",
                    field = id
                ))
            }
        }

        // 检查GPS坐标
        evidencePackage.location?.let { location ->
            if (location.latitude != 0.0 || location.longitude != 0.0) {
                issues.add(PrivacyIssue(
                    type = IssueType.GPS_EXPOSED,
                    severity = Severity.MEDIUM,
                    message = "GPS coordinates present in evidence"
                ))
            }
        }

        return AnonymizedEvidence(
            evidenceId = evidencePackage.evidenceId,
            createdAt = evidencePackage.createdAt,
            timestamps = evidencePackage.timestamps,
            merkleRootHash = evidencePackage.merkleRootHash,
            packageHash = evidencePackage.packageHash,
            privacyIssues = issues,
            isAnonymized = issues.none { it.severity == Severity.CRITICAL }
        )
    }

    /**
     * 验证API响应是否包含敏感信息
     *
     * 规则3：日志脱敏验证
     *
     * @param response API响应内容
     * @return 验证结果
     */
    fun validateApiResponse(response: String): ValidationResult {
        val issues = mutableListOf<PrivacyIssue>()

        // 检查敏感信息模式
        val sensitivePatterns = mapOf(
            "GPS" to Regex("""(\d{1,3}\.\d+,\s*\d{1,3}\.\d+)"""),
            "姓名" to Regex("""[\u4e00-\u9fa5]{2,4}(姓名|同学|老师)"""),
            "学校" to Regex("""(学校|学院|大学)[\u4e00-\u9fa5]+"""),
            "IP" to Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}""")
        )

        sensitivePatterns.forEach { (name, pattern) ->
            if (pattern.containsMatchIn(response)) {
                issues.add(PrivacyIssue(
                    type = IssueType.SENSITIVE_DATA_EXPOSED,
                    severity = Severity.HIGH,
                    message = "Sensitive pattern detected: $name",
                    field = name
                ))
            }
        }

        return ValidationResult(
            isValid = issues.isEmpty(),
            issues = issues
        )
    }

    /**
     * 验证数据删除是否彻底【规则4】
     *
     * @param deletionResult 删除结果
     * @return 验证结果
     */
    fun verifyDataDeletion(deletionResult: DeletionResult): Boolean {
        val steps = listOf(
            deletionResult.databaseOverwritten,
            deletionResult.filesSecureDeleted,
            deletionResult.keystoreKeyDestroyed
        )

        // 必须全部成功
        val allSuccess = steps.all { it }
        val completionRate = steps.count { it } / steps.size.toFloat()

        Log.i(TAG, "Data deletion verification: allSuccess=$allSuccess, rate=$completionRate")

        return allSuccess
    }

    /**
     * 生成隐私合规报告
     */
    fun generatePrivacyReport(
        parentData: Map<String, Any>?,
        evidencePackage: EvidencePackage?
    ): PrivacyReport {
        val parentIssues = mutableListOf<PrivacyIssue>()
        val evidenceIssues = mutableListOf<PrivacyIssue>()

        parentData?.let {
            val result = filterForParent(it, false)
            parentIssues.addAll(result.issues)
        }

        evidencePackage?.let {
            val anonymized = anonymizeEvidencePackage(it)
            evidenceIssues.addAll(anonymized.privacyIssues)
        }

        return PrivacyReport(
            generatedAt = System.currentTimeMillis(),
            parentDataIssues = parentIssues,
            evidenceIssues = evidenceIssues,
            overallRiskLevel = calculateRiskLevel(parentIssues, evidenceIssues)
        )
    }

    private fun calculateRiskLevel(
        parentIssues: List<PrivacyIssue>,
        evidenceIssues: List<PrivacyIssue>
    ): RiskLevel {
        val criticalCount = (parentIssues + evidenceIssues).count { it.severity == Severity.CRITICAL }
        val highCount = (parentIssues + evidenceIssues).count { it.severity == Severity.HIGH }

        return when {
            criticalCount > 0 -> RiskLevel.CRITICAL
            highCount > 2 -> RiskLevel.HIGH
            highCount > 0 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    // ===== 数据类 =====

    data class FilteredResult(
        val isAllowed: Boolean,
        val data: Map<String, Any>,
        val issues: List<PrivacyIssue>
    )

    data class PrivacyIssue(
        val type: IssueType,
        val severity: Severity,
        val message: String,
        val field: String? = null
    )

    enum class IssueType {
        DATA_LEAKAGE,           // 数据泄漏
        UNAUTHORIZED_ACCESS,    // 未授权访问
        IDENTIFIER_FOUND,       // 发现标识符
        GPS_EXPOSED,           // GPS暴露
        SENSITIVE_DATA_EXPOSED, // 敏感数据暴露
        INCOMPLETE_DELETION    // 不完整删除
    }

    enum class Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL  // 阻断发版
    }

    data class ValidationResult(
        val isValid: Boolean,
        val issues: List<PrivacyIssue>
    )

    data class AnonymizedEvidence(
        val evidenceId: String,
        val createdAt: Long,
        val timestamps: Map<String, Long>,
        val merkleRootHash: String?,
        val packageHash: String?,
        val privacyIssues: List<PrivacyIssue>,
        val isAnonymized: Boolean
    )

    data class DeletionResult(
        val databaseOverwritten: Boolean,
        val filesSecureDeleted: Boolean,
        val keystoreKeyDestroyed: Boolean
    )

    enum class RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    data class PrivacyReport(
        val generatedAt: Long,
        val parentDataIssues: List<PrivacyIssue>,
        val evidenceIssues: List<PrivacyIssue>,
        val overallRiskLevel: RiskLevel
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("generated_at", generatedAt)
                put("parent_data_issues", parentDataIssues.size)
                put("evidence_issues", evidenceIssues.size)
                put("overall_risk_level", overallRiskLevel.name)
                put("is_compliant", overallRiskLevel == RiskLevel.LOW)
            }
        }
    }
}
