package com.testimony.app.util

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * 哈希工具类【司法级实现】
 *
 * 提供统一的哈希计算接口
 * 支持流式哈希（避免大文件OOM）
 *
 * @author Testimony数字取证专家
 */
object HashUtils {

    private const val BUFFER_SIZE = 8192

    /**
     * 计算字节数组的SHA-256哈希
     */
    fun sha256(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }

    /**
     * 计算字符串的SHA-256哈希
     */
    fun sha256(text: String): ByteArray = sha256(text.toByteArray())

    /**
     * 计算文件的SHA-256哈希【流式实现】
     *
     * 性能优化：使用流式读取，避免大文件导致OOM
     */
    fun sha256File(file: File): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)

        FileInputStream(file).use { fis ->
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        return digest.digest()
    }

    /**
     * 计算文件的SHA-256哈希（十六进制字符串）
     */
    fun sha256FileHex(file: File): String = bytesToHex(sha256File(file))

    /**
     * 计算字符串的SHA-256哈希（十六进制字符串）
     */
    fun sha256Hex(text: String): String = bytesToHex(sha256(text))

    /**
     * 计算多个文件的组合哈希
     */
    fun sha256Files(files: List<File>): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")

        files.sortedBy { it.name }.forEach { file ->
            if (file.exists() && file.isFile) {
                digest.update(sha256File(file))
            }
        }

        return digest.digest()
    }

    /**
     * 计算多个文件的组合哈希（十六进制字符串）
     */
    fun sha256FilesHex(files: List<File>): String = bytesToHex(sha256Files(files))

    /**
     * 字节数组转十六进制字符串
     */
    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * 十六进制字符串转字节数组
     */
    fun hexToBytes(hex: String): ByteArray {
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    /**
     * 验证文件哈希
     *
     * @param file 要验证的文件
     * @param expectedHash 期望的哈希值（十六进制）
     * @return 是否匹配
     */
    fun verifyFileHash(file: File, expectedHash: String): Boolean {
        return try {
            val actualHash = sha256FileHex(file).lowercase()
            actualHash == expectedHash.lowercase()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 计算哈希链
     *
     * 将多个哈希串联成链
     */
    fun hashChain(hashes: List<ByteArray>): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        hashes.forEach { digest.update(it) }
        return digest.digest()
    }

    /**
     * 验证哈希链
     */
    fun verifyHashChain(hashes: List<ByteArray>, chainHash: ByteArray): Boolean {
        return hashChain(hashes).contentEquals(chainHash)
    }
}
