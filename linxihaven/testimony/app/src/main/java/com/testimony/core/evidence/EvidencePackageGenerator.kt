package com.testimony.core.evidence

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.testimony.core.evidence.ScreenRecorderService.RecordingMetadata
import com.testimony.core.security.EncryptionManager
import com.testimony.core.security.EncryptionManager.EncryptedPayload
import com.testimony.core.timestamp.TimeAnchorService
import com.testimony.core.timestamp.TimeAnchorService.TimestampAnchor
import com.testimony.data.models.EventElement
import com.testimony.data.models.InterviewState
import com.testimony.util.generateUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 证据包生成器 - 优化版
 * - 异步处理，不阻塞主线程
 * - 更好的错误处理
 * - 流式处理大文件
 */
class EvidencePackageGenerator(
    private val context: Context,
    private val encryptionManager: EncryptionManager,
    private val timeAnchorService: TimeAnchorService
) {
    companion object {
        private const val BUFFER_SIZE = 8192
        private const val ZIP_COMPRESSION_LEVEL = 6 // 平衡速度和大小
    }

    // ========== 主入口 ==========

    /**
     * 生成完整证据包
     * @param sessionId 会话ID
     * @param recordingMetadata 录屏元数据
     * @param operationLog 操作日志内容
     * @param sensorData 传感器数据
     * @param fsmTransitions FSM状态转换记录
     * @param eventElements 收集的事件要素
     * @param location 位置信息（可选）
     * @param onProgress 进度回调
     */
    suspend fun generatePackage(
        sessionId: String,
        recordingMetadata: RecordingMetadata?,
        operationLog: String,
        sensorData: String,
        fsmTransitions: List<FSMTransitionRecord>,
        eventElements: Map<EventElement, String>,
        location: LocationData? = null,
        onProgress: ((Float, String) -> Unit)? = null
    ): EvidencePackageResult = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke(0f, "初始化...")
            
            val packageId = "EP_${generateUUID()}"
            val timestamp = timeAnchorService.getAnchoredTimestamp()
            val evidenceDir = File(context.cacheDir, "evidence_$packageId").apply { mkdirs() }
            
            // 1. 复制/处理录屏文件
            onProgress?.invoke(0.1f, "处理录屏...")
            val screenRecordingFile = recordingMetadata?.let { processRecording(it, evidenceDir) }
            
            // 2. 生成操作日志文件
            onProgress?.invoke(0.25f, "生成操作日志...")
            val operationLogFile = writeJsonFile(evidenceDir, "operation_log.json", operationLog)
            
            // 3. 生成传感器日志文件
            onProgress?.invoke(0.35f, "生成传感器日志...")
            val sensorLogFile = writeJsonFile(evidenceDir, "sensor_log.json", sensorData)
            
            // 4. 生成FSM转换记录
            onProgress?.invoke(0.45f, "生成状态记录...")
            val fsmLogFile = generateFSMLogFile(evidenceDir, fsmTransitions)
            
            // 5. 生成元数据文件
            onProgress?.invoke(0.55f, "生成元数据...")
            val metadata = generateMetadata(
                packageId, timestamp, screenRecordingFile, operationLogFile, 
                sensorLogFile, fsmLogFile, fsmTransitions, eventElements, location
            )
            val metadataFile = writeJsonFile(evidenceDir, "metadata.json", metadata)
            
            // 6. 生成完整性清单
            onProgress?.invoke(0.7f, "验证完整性...")
            val manifest = generateIntegrityManifest(
                packageId, timestamp,
                listOfNotNull(screenRecordingFile, operationLogFile, sensorLogFile, fsmLogFile, metadataFile)
            )
            val manifestFile = writeJsonFile(evidenceDir, "integrity.json", manifest)
            
            // 7. 创建ZIP包
            onProgress?.invoke(0.85f, "打包证据...")
            val zipFile = File(context.cacheDir, "evidence_package_$packageId.zip")
            createZipPackage(evidenceDir, zipFile)
            
            // 8. 加密ZIP包
            onProgress?.invoke(0.95f, "加密证据...")
            val encryptedZip = File(context.cacheDir, "evidence_encrypted_$packageId.tes")
            encryptionManager.encryptFileStream(zipFile, encryptedZip)
            
            // 9. 清理临时文件
            onProgress?.invoke(1f, "清理...")
            evidenceDir.deleteRecursively()
            zipFile.delete()
            
            EvidencePackageResult(
                success = true,
                packageId = packageId,
                encryptedFilePath = encryptedZip.absolutePath,
                fileHash = encryptionManager.generateFileHash(encryptedZip),
                timestamp = timestamp,
                fileCount = manifest.fileCount,
                totalSize = manifest.totalSize
            )
        } catch (e: Exception) {
            EvidencePackageResult(
                success = false,
                error = "证据包生成失败: ${e.message}"
            )
        }
    }

    // ========== 验证证据包 ==========

    suspend fun verifyPackage(packagePath: String): VerificationResult = withContext(Dispatchers.IO) {
        try {
            val encryptedFile = File(packagePath)
            if (!encryptedFile.exists()) {
                return@withContext VerificationResult(false, "文件不存在")
            }

            // 1. 解密文件
            val tempZip = File(context.cacheDir, "verify_temp_${System.currentTimeMillis()}.zip")
            encryptionManager.decryptFileStream(encryptedFile, tempZip)
            
            // 2. 解压并验证
            val extractDir = File(context.cacheDir, "verify_extract_${System.currentTimeMillis()}").apply { mkdirs() }
            extractZip(tempZip, extractDir)
            
            // 3. 读取完整性清单
            val manifestFile = File(extractDir, "integrity.json")
            if (!manifestFile.exists()) {
                return@withContext VerificationResult(false, "完整性清单缺失")
            }
            
            val manifest = JSONObject(manifestFile.readText())
            val storedMerkleRoot = manifest.getString("merkleRootHash")
            
            // 4. 重新计算Merkle根
            val files = extractDir.listFiles()?.filter { it.name != "integrity.json" } ?: emptyList()
            val calculatedMerkleRoot = calculateMerkleRoot(files)
            
            // 5. 比较
            val isValid = encryptionManager.constantTimeEquals(storedMerkleRoot, calculatedMerkleRoot)
            
            // 清理
            tempZip.delete()
            extractDir.deleteRecursively()
            
            VerificationResult(
                isValid = isValid,
                merkleRootMatch = isValid,
                fileIntegrityOk = isValid,
                packageTimestamp = manifest.optLong("generatedAt")
            )
        } catch (e: Exception) {
            VerificationResult(false, "验证失败: ${e.message}")
        }
    }

    // ========== 辅助方法 ==========

    private fun processRecording(metadata: RecordingMetadata, outputDir: File): File? {
        val inputFile = File(metadata.filePath)
        if (!inputFile.exists()) return null
        
        val outputFile = File(outputDir, "screen_recording.mp4")
        inputFile.copyTo(outputFile, overwrite = true)
        return outputFile
    }

    private fun writeJsonFile(dir: File, filename: String, content: String): File {
        val file = File(dir, filename)
        file.writeText(content)
        return file
    }

    private fun generateFSMLogFile(dir: File, transitions: List<FSMTransitionRecord>): File {
        val jsonArray = JSONArray()
        transitions.forEach { t ->
            jsonArray.put(JSONObject().apply {
                put("fromState", t.fromState.name)
                put("toState", t.toState.name)
                put("timestamp", t.timestamp)
                put("trigger", t.trigger)
                put("userInput", t.userInput)
                put("aiResponse", t.aiResponse)
                put("emotionalRisk", t.emotionalRisk)
            })
        }
        return writeJsonFile(dir, "fsm_log.json", jsonArray.toString(2))
    }

    private fun generateMetadata(
        packageId: String,
        timestamp: TimestampAnchor,
        screenRecording: File?,
        operationLog: File,
        sensorLog: File,
        fsmLog: File,
        fsmTransitions: List<FSMTransitionRecord>,
        eventElements: Map<EventElement, String>,
        location: LocationData?
    ): String {
        val files = JSONArray()
        listOfNotNull(screenRecording, operationLog, sensorLog, fsmLog).forEach { f ->
            files.put(JSONObject().apply {
                put("name", f.name)
                put("hash", encryptionManager.generateFileHash(f))
                put("size", f.length())
                put("timestamp", f.lastModified())
            })
        }

        val transitions = JSONArray()
        fsmTransitions.forEach { t ->
            transitions.put(JSONObject().apply {
                put("fromState", t.fromState.name)
                put("toState", t.toState.name)
                put("timestamp", t.timestamp)
                put("emotionalRisk", t.emotionalRisk)
            })
        }

        val elements = JSONObject()
        eventElements.forEach { (k, v) -> elements.put(k.name, v) }

        return JSONObject().apply {
            put("packageId", packageId)
            put("evidenceId", packageId)
            put("generatedAt", System.currentTimeMillis())
            put("anchoredTime", timestamp.anchoredTimeMillis)
            put("anchoredSources", JSONArray(timestamp.sources))
            put("hashChain", timestamp.hashChain?.let {
                JSONObject().put("chainHash", it.chainHash)
            })
            put("fsmTransitions", transitions)
            put("collectedElements", elements)
            put("fileManifest", files)
            put("deviceInfo", JSONObject().apply {
                put("androidId", getAndroidId())
                put("manufacturer", Build.MANUFACTURER)
                put("model", Build.MODEL)
                put("osVersion", Build.VERSION.RELEASE)
            })
            location?.let {
                put("location", JSONObject().apply {
                    put("latitude", it.latitude)
                    put("longitude", it.longitude)
                    put("accuracy", it.accuracy)
                    put("timestamp", it.timestamp)
                })
            }
        }.toString(2)
    }

    private fun generateIntegrityManifest(
        packageId: String,
        timestamp: TimestampAnchor,
        files: List<File>
    ): String {
        val merkleRoot = calculateMerkleRoot(files)
        return JSONObject().apply {
            put("packageId", packageId)
            put("merkleRootHash", merkleRoot)
            put("fileCount", files.size)
            put("totalSize", files.sumOf { it.length() })
            put("generatedAt", System.currentTimeMillis())
            put("timestampAnchor", JSONObject().apply {
                put("anchoredTime", timestamp.anchoredTimeMillis)
                put("sources", JSONArray(timestamp.sources))
                put("verificationStatus", timestamp.verificationStatus)
            })
        }.toString(2)
    }

    /** 计算 Merkle 树根哈希 */
    fun calculateMerkleRoot(files: List<File>): String {
        if (files.isEmpty()) return ""
        
        // 计算每个文件的哈希
        var hashes = files.map { file ->
            val content = file.readBytes()
            hashWithMetadata(content, file.name, file.length())
        }.toMutableList()
        
        // 构建 Merkle 树
        while (hashes.size > 1) {
            val newLevel = mutableListOf<String>()
            for (i in hashes.indices step 2) {
                val left = hashes[i]
                val right = if (i + 1 < hashes.size) hashes[i + 1] else left
                newLevel.add(hashPair(left, right))
            }
            hashes = newLevel
        }
        
        return hashes.first()
    }

    private fun hashWithMetadata(content: ByteArray, filename: String, size: Long): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(content)
        md.update(filename.toByteArray())
        md.update(size.toString().toByteArray())
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun hashPair(left: String, right: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(left.toByteArray())
        md.update(right.toByteArray())
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun createZipPackage(sourceDir: File, outputFile: File) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            sourceDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val entry = ZipEntry(file.name)
                zos.putNextEntry(entry)
                file.inputStream().use { fis ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var len: Int
                    while (fis.read(buffer).also { len = it } > 0) {
                        zos.write(buffer, 0, len)
                    }
                }
                zos.closeEntry()
            }
        }
    }

    private fun extractZip(zipFile: File, destDir: File) {
        java.util.zip.ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: java.util.zip.ZipEntry? = zis.nextEntry
            while (entry != null) {
                val newFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun getAndroidId(): String = 
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    // ========== 数据类 ==========

    data class EvidencePackageResult(
        val success: Boolean,
        val packageId: String? = null,
        val encryptedFilePath: String? = null,
        val fileHash: String? = null,
        val timestamp: TimestampAnchor? = null,
        val fileCount: Int = 0,
        val totalSize: Long = 0,
        val error: String? = null
    )

    data class VerificationResult(
        val isValid: Boolean,
        val merkleRootMatch: Boolean = false,
        val fileIntegrityOk: Boolean = false,
        val packageTimestamp: Long = 0,
        val error: String? = null
    )

    data class FSMTransitionRecord(
        val fromState: InterviewState,
        val toState: InterviewState,
        val timestamp: Long,
        val trigger: String,
        val userInput: String,
        val aiResponse: String,
        val emotionalRisk: String
    )

    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val timestamp: Long
    )
}
