package com.testimony.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.testimony.util.Constants
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

/**
 * AES-256-GCM 加密管理器 - 优化版
 * - 支持流式加密（大文件不 OOM）
 * - 使用 Android Keystore 硬件级密钥存储
 * - 常数时间比较防时序攻击
 */
class EncryptionManager(private val context: Context) {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "TestimonyMasterKey_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_SIZE = 12
        private const val BUFFER_SIZE = 8192 // 8KB 缓冲区
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    init { ensureKeyExists() }

    /** 确保主密钥存在，不存在则创建 */
    private fun ensureKeyExists() {
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) generateMasterKey()
    }

    private fun generateMasterKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
             .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
             .setKeySize(Constants.AES_KEY_SIZE)
             .setUserAuthenticationRequired(false)
             .setRandomizedEncryptionRequired(true)
             .build()
        )
        keyGenerator.generateKey()
    }

    private fun getMasterKey(): SecretKey = 
        (keyStore.getEntry(MASTER_KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey

    // ========== 基础加密操作 ==========

    fun encrypt(data: ByteArray): EncryptedPayload = encryptWithKey(data, getMasterKey())

    fun decrypt(payload: EncryptedPayload): ByteArray = decryptWithKey(payload, getMasterKey())

    private fun encryptWithKey(data: ByteArray, key: SecretKey): EncryptedPayload {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return EncryptedPayload(
            ciphertext = cipher.doFinal(data),
            iv = cipher.iv,
            keyId = MASTER_KEY_ALIAS,
            algorithm = "AES-256-GCM"
        )
    }

    private fun decryptWithKey(payload: EncryptedPayload, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, payload.iv))
        return cipher.doFinal(payload.ciphertext)
    }

    // ========== 流式加密 - 大文件优化 ==========

    /**
     * 流式加密文件 - 内存友好，不 OOM
     * @param input 输入文件
     * @param output 输出文件
     * @param onProgress 进度回调 (0.0 - 1.0)
     */
    fun encryptFileStream(input: File, output: File, onProgress: ((Float) -> Unit)? = null): EncryptedPayload {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getMasterKey())
        
        output.outputStream().use { fos ->
            // 写入 IV
            fos.write(cipher.iv)
            
            input.inputStream().use { fis ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                var totalBytesRead = 0L
                val fileSize = input.length()
                
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    val encryptedChunk = if (bytesRead == BUFFER_SIZE) {
                        cipher.update(buffer)
                    } else {
                        cipher.update(buffer, 0, bytesRead)
                    }
                    encryptedChunk?.let { fos.write(it) }
                    totalBytesRead += bytesRead
                    onProgress?.invoke(totalBytesRead.toFloat() / fileSize)
                }
                
                // 写入最终块
                fos.write(cipher.doFinal())
            }
        }
        
        return EncryptedPayload(
            ciphertext = ByteArray(0), // 流式加密不保留密文副本
            iv = cipher.iv,
            keyId = MASTER_KEY_ALIAS,
            algorithm = "AES-256-GCM"
        )
    }

    /** 流式解密 */
    fun decryptFileStream(encryptedFile: File, outputFile: File, onProgress: ((Float) -> Unit)? = null) {
        encryptedFile.inputStream().use { fis ->
            // 读取 IV
            val iv = ByteArray(GCM_IV_SIZE)
            fis.read(iv)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
            
            outputFile.outputStream().use { fos ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                var totalBytesRead = 0L
                val fileSize = encryptedFile.length() - GCM_IV_SIZE
                
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    val decryptedChunk = if (bytesRead == BUFFER_SIZE) {
                        cipher.update(buffer)
                    } else {
                        cipher.update(buffer, 0, bytesRead)
                    }
                    decryptedChunk?.let { fos.write(it) }
                    totalBytesRead += bytesRead
                    onProgress?.invoke(totalBytesRead.toFloat() / fileSize)
                }
                fos.write(cipher.doFinal())
            }
        }
    }

    // ========== 字符串加密 ==========

    fun encryptString(plainText: String): EncryptedPayload = 
        encrypt(plainText.toByteArray(Charsets.UTF_8))

    fun decryptToString(payload: EncryptedPayload): String = 
        String(decrypt(payload), Charsets.UTF_8)

    // ========== Base64 便捷方法 ==========

    fun encryptToBase64(plainText: String): String = 
        encryptString(plainText).toBase64()

    fun decryptFromBase64(base64: String): String = 
        decryptToString(EncryptedPayload.fromBase64(base64))

    // ========== 哈希工具 ==========

    fun generateContentHash(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(data).joinToString("") { "%02x".format(it) }
    }

    fun generateFileHash(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fis ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** 常数时间哈希比较 - 防时序攻击 */
    fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }

    // ========== 数据类 ==========

    data class EncryptedPayload(
        val ciphertext: ByteArray,
        val iv: ByteArray,
        val keyId: String,
        val algorithm: String
    ) {
        fun toBase64(): String {
            val combined = iv + ciphertext
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        }

        fun toJson(): String = """{"ciphertext":"${Base64.encodeToString(ciphertext, Base64.NO_WRAP)}","iv":"${Base64.encodeToString(iv, Base64.NO_WRAP)}","keyId":"$keyId","algorithm":"$algorithm"}"""

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as EncryptedPayload
            return ciphertext.contentEquals(other.ciphertext) && iv.contentEquals(other.iv)
        }

        override fun hashCode(): Int {
            var result = ciphertext.contentHashCode()
            result = 31 * result + iv.contentHashCode()
            return result
        }

        companion object {
            fun fromBase64(base64: String): EncryptedPayload {
                val combined = Base64.decode(base64, Base64.NO_WRAP)
                val iv = combined.sliceArray(0 until GCM_IV_SIZE)
                val ciphertext = combined.sliceArray(GCM_IV_SIZE until combined.size)
                return EncryptedPayload(ciphertext, iv, MASTER_KEY_ALIAS, "AES-256-GCM")
            }
        }
    }
}
