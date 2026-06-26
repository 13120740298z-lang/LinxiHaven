package com.testimony.app.evidence

import android.util.Log
import com.testimony.app.util.HashUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 证据链管理【司法级实现】
 *
 * 司法意义：
 * - 操作日志不可变，后一条包含前一条的哈希
 * - 每次操作生成包含四源时间戳的操作记录
 * - 完整操作链可追溯、可验证
 *
 * 数据结构：
 * {
 *   "entry_id": "UUID",
 *   "operator_uid": "用户ID",
 *   "timestamp_ms": 1234567890123,
 *   "timestamp_sources": { "system": ..., "ntp": ..., ... },
 *   "operation_type": "RECORDING_START|EVIDENCE_PACK|...",
 *   "prev_hash": "前一条日志的SHA-256",
 *   "current_hash": "本条日志的SHA-256",
 *   "metadata": { ... }
 * }
 *
 * @author Testimony数字取证专家
 */
class ChainOfCustody(private val sessionId: String, private val storageDir: File? = null) {

    private val logEntries = mutableListOf<CustodyEntry>()
    private var lastHash: ByteArray? = null

    init {
        // Restore last hash from persistent storage if available to prevent chain breakage on restart
        storageDir?.let {
            val hashFile = File(it, "chain_last_hash_$sessionId.bin")
            if (hashFile.exists()) {
                lastHash = hashFile.readBytes()
            }
        }
    }

    /**
     * 添加操作记录【核心接口】
     *
     * @param type 操作类型
     * @param metadata 额外元数据
     * @param trustedTimestamp 四源可信时间戳（可选）
     * @return 新增的记录条目
     */
    fun addEntry(
        type: OperationType,
        metadata: Map<String, Any> = emptyMap(),
        trustedTimestamp: TrustedTimestampData? = null
    ): CustodyEntry {
        val timestamp = trustedTimestamp?.fusedTimestamp ?: System.currentTimeMillis()

        val entry = CustodyEntry(
            entryId = generateEntryId(),
            operatorUid = currentOperatorUid ?: "ANONYMOUS",
            timestampMs = timestamp,
            timestampSources = trustedTimestamp?.sources ?: emptyMap(),
            operationType = type,
            prevHash = lastHash?.let { HashUtils.bytesToHex(it) } ?: "GENESIS",
            metadata = metadata
        )

        // 计算当前哈希（包含前一条哈希，形成链）
        entry.currentHash = calculateEntryHash(entry)
        lastHash = entry.currentHash
        
        // Persist lastHash to prevent chain breakage
        storageDir?.let {
            try {
                File(it, "chain_last_hash_$sessionId.bin").writeBytes(lastHash!!)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist lastHash", e)
            }
        }

        logEntries.add(entry)
        Log.d(TAG, "Custody entry added: ${entry.entryId} [${entry.operationType.name}]")

        return entry
    }

    /**
     * 批量添加操作记录
     */
    fun addEntries(entries: List<Pair<OperationType, Map<String, Any>>>) {
        entries.forEach { (type, metadata) ->
            addEntry(type, metadata)
        }
    }

    /**
     * 验证链完整性
     */
    fun verifyChain(): ChainVerificationResult {
        if (logEntries.isEmpty()) {
            return ChainVerificationResult(
                isValid = true,
                totalEntries = 0,
                issues = emptyList()
            )
        }

        val issues = mutableListOf<String>()
        var expectedPrevHash: ByteArray? = null

        logEntries.forEachIndexed { index, entry ->
            // 验证第一条记录
            if (index == 0) {
                if (entry.prevHash != "GENESIS") {
                    issues.add("Entry $index: First entry should have prevHash=GENESIS")
                }
            } else {
                // 验证链连接 - 修复NPE风险
                val prevHash = expectedPrevHash
                val currHash = entry.currentHash
                if (prevHash != null && currHash != null && !prevHash.contentEquals(currHash)) {
                    issues.add("Entry $index: Hash chain broken at position $index")
                }
            }

            // 验证哈希计算
            val calculatedHash = calculateEntryHash(entry.copy(currentHash = null))
            if (!calculatedHash.contentEquals(entry.currentHash)) {
                issues.add("Entry $index: Hash mismatch - entry may have been tampered")
            }

            expectedPrevHash = entry.currentHash
        }

        return ChainVerificationResult(
            isValid = issues.isEmpty(),
            totalEntries = logEntries.size,
            issues = issues
        )
    }

    /**
     * 导出为JSON数组
     */
    fun exportToJson(): JSONArray {
        return JSONArray().apply {
            logEntries.forEach { entry ->
                put(entry.toJson())
            }
        }
    }

    /**
     * 导出到文件
     */
    fun exportToFile(file: File): Boolean {
        return try {
            file.writeText(exportToJson().toString(2))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export custody chain", e)
            false
        }
    }

    /**
     * 从文件导入
     */
    fun importFromFile(file: File): Boolean {
        return try {
            val jsonArray = JSONArray(file.readText())
            logEntries.clear()
            lastHash = null

            for (i in 0 until jsonArray.length()) {
                val entry = CustodyEntry.fromJson(jsonArray.getJSONObject(i))
                logEntries.add(entry)
                if (entry.currentHash != null) {
                    lastHash = entry.currentHash
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import custody chain", e)
            false
        }
    }

    /**
     * 获取所有记录
     */
    fun getEntries(): List<CustodyEntry> = logEntries.toList()

    /**
     * 获取最后一条记录
     */
    fun getLastEntry(): CustodyEntry? = logEntries.lastOrNull()

    /**
     * 获取记录数量
     */
    fun getEntryCount(): Int = logEntries.size

    /**
     * 设置当前操作者
     */
    fun setOperator(uid: String) {
        currentOperatorUid = uid
    }

    // ===== 内部方法 =====

    private fun generateEntryId(): String {
        return "CE_${sessionId}_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    private fun calculateEntryHash(entry: CustodyEntry): ByteArray {
        val data = buildString {
            append(entry.entryId)
            append(entry.operatorUid)
            append(entry.timestampMs)
            append(entry.operationType.name)
            append(entry.prevHash)
            append(JSONObject(entry.metadata).toString())
        }
        return HashUtils.sha256(data.toByteArray())
    }

    // ===== 数据类 =====

    data class CustodyEntry(
        val entryId: String,
        val operatorUid: String,
        val timestampMs: Long,
        val timestampSources: Map<String, Long>,
        val operationType: OperationType,
        val prevHash: String,
        var currentHash: ByteArray? = null,
        val metadata: Map<String, Any> = emptyMap()
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("entry_id", entryId)
                put("operator_uid", operatorUid)
                put("timestamp_ms", timestampMs)
                put("timestamp_sources", JSONObject(timestampSources))
                put("operation_type", operationType.name)
                put("prev_hash", prevHash)
                put("current_hash", currentHash?.let { HashUtils.bytesToHex(it) })
                put("metadata", JSONObject(metadata))
            }
        }

        companion object {
            fun fromJson(json: JSONObject): CustodyEntry {
                return CustodyEntry(
                    entryId = json.getString("entry_id"),
                    operatorUid = json.getString("operator_uid"),
                    timestampMs = json.getLong("timestamp_ms"),
                    timestampSources = json.optJSONObject("timestamp_sources")?.let { obj ->
                        obj.keys().asSequence().associateWith { obj.getLong(it) }
                    } ?: emptyMap(),
                    operationType = OperationType.valueOf(json.getString("operation_type")),
                    prevHash = json.getString("prev_hash"),
                    currentHash = json.optString("current_hash").takeIf { it.isNotEmpty() }
                        ?.let { HashUtils.hexToBytes(it) },
                    metadata = json.optJSONObject("metadata")?.let { obj ->
                        obj.keys().asSequence().associateWith { obj.get(it) }
                    } ?: emptyMap()
                )
            }
        }
    }

    enum class OperationType {
        // 安全空间操作
        SECURE_SPACE_ENTER,
        SECURE_SPACE_EXIT,
        SESSION_START,
        SESSION_END,

        // 录屏操作
        RECORDING_START,
        RECORDING_STOP,
        RECORDING_PAUSE,
        RECORDING_RESUME,

        // 证据操作
        EVIDENCE_PACK_CREATED,
        EVIDENCE_PACK_ENCRYPTED,
        EVIDENCE_PACK_STORED,
        EVIDENCE_PACK_SUBMITTED,
        EVIDENCE_PACK_SHARED,

        // AI引导操作
        GUIDANCE_START,
        GUIDANCE_STATE_CHANGE,
        GUIDANCE_COMPLETED,
        GUIDANCE_ESCALATED,

        // 安全操作
        USER_AUTHENTICATION,
        DATA_DELETION,
        KEY_ROTATION
    }

    data class TrustedTimestampData(
        val fusedTimestamp: Long,
        val sources: Map<String, Long>
    )

    data class ChainVerificationResult(
        val isValid: Boolean,
        val totalEntries: Int,
        val issues: List<String>
    )

    companion object {
        private const val TAG = "ChainOfCustody"
        private var currentOperatorUid: String? = null
    }
}
