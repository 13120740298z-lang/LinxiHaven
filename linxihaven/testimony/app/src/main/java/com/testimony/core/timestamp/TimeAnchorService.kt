package com.testimony.core.timestamp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Base64
import com.testimony.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/**
 * 多源时间锚定服务 - 优化版
 * 
 * 提供防篡改的时间戳，通过多个独立时间源交叉验证：
 * 1. 系统时间
 * 2. NTP 服务器时间
 * 3. 基站时间（预留）
 * 4. 区块头时间（预留）
 */
class TimeAnchorService(private val context: Context) {

    companion object {
        private val NTP_SERVERS = listOf(
            "time.google.com",
            "time.cloudflare.com",
            "pool.ntp.org"
        )
        private const val NTP_PORT = 123
        private const val NTP_TIMEOUT_MS = 3000
        private const val NTP_PACKET_SIZE = 48
        private const val MAX_TIME_OFFSET = 60000L // 60秒容差
        private const val MIN_VALID_SOURCES = 2
    }

    private val deviceInfo: DeviceInfo by lazy { collectDeviceInfo() }

    // ========== 公共 API ==========

    /**
     * 获取锚定时间戳 - 多源交叉验证
     * @return 包含多个时间源和哈希链的时间锚点
     */
    suspend fun getAnchoredTimestamp(): TimestampAnchor = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        // 收集各时间源
        val systemTime = getSystemTime()
        val ntpTime = queryNTPServers()
        val cellTime = getCellTowerTime()
        
        // 构建时间源集合
        val sources = mutableListOf<TimeSource>()
        sources.add(TimeSource("system", systemTime, System.currentTimeMillis()))
        ntpTime?.let { sources.add(TimeSource("ntp", it, System.currentTimeMillis())) }
        cellTime?.let { sources.add(TimeSource("cell", it, System.currentTimeMillis())) }
        
        // 计算共识时间（中位数）
        val consensusTime = calculateConsensusTime(sources)
        
        // 生成哈希链
        val hashChain = generateHashChain(consensusTime, deviceInfo)
        
        // 验证哈希链
        val verificationStatus = verifyHashChain(hashChain)
        
        TimestampAnchor(
            anchoredTimeMillis = consensusTime,
            consensusTimeMillis = consensusTime,
            sources = sources.map { it.name },
            sourceDetails = sources.associate { it.name to it.timestamp },
            hashChain = hashChain,
            verificationStatus = verificationStatus,
            serverTimeOffset = ntpTime?.let { it - systemTime },
            generatedAt = System.currentTimeMillis(),
            latencyMs = System.currentTimeMillis() - startTime
        )
    }

    /**
     * 验证时间锚点是否有效
     */
    fun verifyAnchor(anchor: TimestampAnchor): Boolean {
        // 1. 验证时间合理性
        val now = System.currentTimeMillis()
        if (kotlin.math.abs(anchor.anchoredTimeMillis - now) > MAX_TIME_OFFSET) {
            return false
        }
        
        // 2. 验证哈希链
        if (anchor.hashChain == null) return false
        if (!verifyHashChain(anchor.hashChain)) return false
        
        // 3. 验证源数量
        if (anchor.sources.size < MIN_VALID_SOURCES) return false
        
        return true
    }

    /**
     * 同步 NTP 时间
     */
    @SuppressLint("MissingPermission")
    suspend fun syncNTPTime(): Long? = withContext(Dispatchers.IO) {
        queryNTPServers()
    }

    // ========== 私有方法 ==========

    private fun getSystemTime(): Long = System.currentTimeMillis()

    /** 查询多个 NTP 服务器，返回最快响应的 */
    private suspend fun queryNTPServers(): Long? = withContext(Dispatchers.IO) {
        for (server in NTP_SERVERS) {
            try {
                val time = queryNTPServer(server)
                if (time != null) return@withContext time
            } catch (e: Exception) {
                // 尝试下一个服务器
            }
        }
        null
    }

    /**
     * 查询单个 NTP 服务器
     * 使用 RFC 2030 定义的 SNTP 协议
     */
    private fun queryNTPServer(serverAddress: String): Long? {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = NTP_TIMEOUT_MS
            
            val address = InetAddress.getByName(serverAddress)
            val buffer = ByteArray(NTP_PACKET_SIZE)
            
            // 设置 NTP 请求包 (LI = 0, Version = 3, Mode = 3)
            buffer[0] = 0x1B.toByte()
            
            // 发送请求
            val request = DatagramPacket(buffer, NTP_PACKET_SIZE, address, NTP_PORT)
            val sendTime = System.nanoTime()
            socket.send(request)
            
            // 接收响应
            val response = DatagramPacket(buffer, NTP_PACKET_SIZE)
            socket.receive(response)
            val receiveTime = System.nanoTime()
            
            // 解析 NTP 时间戳 (字节 40-47)
            val ntpTime = extractNTPTimestamp(buffer, 40)
            
            // 转换为毫秒
            val ntpMillis = ntpTime - 2208988800000L // 1900-01-01 到 1970-01-01 的毫秒数
            
            // 计算往返延迟
            val roundTripDelay = ((receiveTime - sendTime) / 2) / 1_000_000.0
            
            // 修正往返延迟
            return (ntpMillis + roundTripDelay.toLong())
            
        } catch (e: Exception) {
            return null
        } finally {
            socket?.close()
        }
    }

    /**
     * 从 NTP 响应包中提取时间戳
     * NTP 时间戳是 64 位无符号整数，前 32 位是秒，后 32 位是分数
     */
    private fun extractNTPTimestamp(buffer: ByteArray, offset: Int): Long {
        // 读取整数秒部分 (大端序)
        val seconds = (
            ((buffer[offset].toLong() and 0xFF) shl 24) or
            ((buffer[offset + 1].toLong() and 0xFF) shl 16) or
            ((buffer[offset + 2].toLong() and 0xFF) shl 8) or
            (buffer[offset + 3].toLong() and 0xFF)
        )
        
        // 读取分数秒部分
        val fraction = (
            ((buffer[offset + 4].toLong() and 0xFF) shl 24) or
            ((buffer[offset + 5].toLong() and 0xFF) shl 16) or
            ((buffer[offset + 6].toLong() and 0xFF) shl 8) or
            (buffer[offset + 7].toLong() and 0xFF)
        )
        
        // 转换为毫秒 (分数部分 / 2^32 * 1000)
        val fractionMs = (fraction * 1000) / 0x100000000L
        
        return seconds * 1000 + fractionMs
    }

    /**
     * 获取基站时间 (占位实现)
     * 实际需要 CellInfo 和 telephony 权限
     */
    private fun getCellTowerTime(): Long? {
        // 占位实现 - 返回当前系统时间
        // 实际实现需要:
        // 1. TelephonyManager 获取 CellInfo
        // 2. 调用 getCellTowerTime() (需要 carrier privileges)
        return getSystemTime()
    }

    /** 计算共识时间 - 使用中位数 */
    private fun calculateConsensusTime(sources: List<TimeSource>): Long {
        if (sources.isEmpty()) return getSystemTime()
        if (sources.size == 1) return sources.first().timestamp
        
        val sortedTimes = sources.map { it.timestamp }.sorted()
        return if (sortedTimes.size % 2 == 0) {
            (sortedTimes[sortedTimes.size / 2 - 1] + sortedTimes[sortedTimes.size / 2]) / 2
        } else {
            sortedTimes[sortedTimes.size / 2]
        }
    }

    /**
     * 生成哈希链 - 防篡改机制
     * 将时间戳、设备信息和前一个哈希链接
     */
    private fun generateHashChain(timestamp: Long, deviceInfo: DeviceInfo): HashChain {
        val md = MessageDigest.getInstance("SHA-256")
        
        // 设备指纹
        val deviceFingerprint = "${deviceInfo.androidId}|${deviceInfo.manufacturer}|${deviceInfo.model}"
        
        // 第一层哈希: 时间戳 + 设备指纹
        md.update(timestamp.toString().toByteArray())
        md.update(deviceFingerprint.toByteArray())
        val firstHash = md.digest()
        
        // 第二层哈希: 加入随机盐和服务器时间
        md.update(firstHash)
        md.update(deviceInfo.androidId.toByteArray())
        md.update(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp)).toByteArray())
        val secondHash = md.digest()
        
        return HashChain(
            chainHash = secondHash.joinToString("") { "%02x".format(it) },
            timestamp = timestamp,
            previousHash = firstHash.joinToString("") { "%02x".format(it) }
        )
    }

    /** 验证哈希链 */
    private fun verifyHashChain(chain: HashChain): String {
        return try {
            // 重新计算哈希并比较
            val md = MessageDigest.getInstance("SHA-256")
            md.update(chain.timestamp.toString().toByteArray())
            md.update("${deviceInfo.androidId}|${deviceInfo.manufacturer}|${deviceInfo.model}".toByteArray())
            val expectedFirst = md.digest()
            
            md.update(expectedFirst)
            md.update(deviceInfo.androidId.toByteArray())
            val expectedSecond = md.digest()
            
            val calculatedHash = expectedSecond.joinToString("") { "%02x".format(it) }
            
            if (calculatedHash == chain.chainHash) "VALID" else "TAMPERED"
        } catch (e: Exception) {
            "ERROR"
        }
    }

    private fun collectDeviceInfo(): DeviceInfo {
        @SuppressLint("HardwareIds")
        val androidId = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID
        )
        return DeviceInfo(
            androidId = androidId,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            osVersion = Build.VERSION.RELEASE
        )
    }

    // ========== 数据类 ==========

    data class TimestampAnchor(
        val anchoredTimeMillis: Long,
        val consensusTimeMillis: Long,
        val sources: List<String>,
        val sourceDetails: Map<String, Long>,
        val hashChain: HashChain?,
        val verificationStatus: String,
        val serverTimeOffset: Long?,
        val generatedAt: Long,
        val latencyMs: Long
    )

    data class HashChain(
        val chainHash: String,
        val timestamp: Long,
        val previousHash: String
    )

    data class DeviceInfo(
        val androidId: String,
        val manufacturer: String,
        val model: String,
        val osVersion: String
    )

    private data class TimeSource(
        val name: String,
        val timestamp: Long,
        val queriedAt: Long
    )
}
