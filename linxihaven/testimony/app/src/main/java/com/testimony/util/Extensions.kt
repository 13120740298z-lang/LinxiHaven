package com.testimony.util

import java.security.MessageDigest
import java.util.UUID

/** 生成随机 UUID */
fun generateUUID(): String = UUID.randomUUID().toString()

/** SHA-256 哈希 */
fun String.sha256(): String {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(this.toByteArray()).joinToString("") { "%02x".format(it) }
}

/** Base64 编码 */
fun ByteArray.toBase64(): String = 
    android.util.Base64.encodeToString(this, android.util.Base64.NO_WRAP)

/** Base64 解码 */
fun String.fromBase64(): ByteArray = 
    android.util.Base64.decode(this, android.util.Base64.NO_WRAP)

/** 安全字符串比较 */
fun String.constantTimeEquals(other: String): Boolean {
    if (length != other.length) return false
    var result = 0
    for (i in indices) result = result or (this[i].code xor other[i].code)
    return result == 0
}

/** 格式化时间戳 */
fun Long.toReadableDateTime(): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(this))
}

/** 格式化文件大小 */
fun Long.toReadableSize(): String {
    if (this < 1024) return "$this B"
    val kb = this / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.1f GB".format(mb / 1024.0)
}

/** 协程重试 */
suspend fun <T> retry(
    times: Int = 3,
    delayMs: Long = 1000,
    block: suspend () -> T
): T {
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            kotlinx.coroutines.delay(delayMs * (it + 1))
        }
    }
    return block()
}
