package com.testimony.util

object Constants {
    // ========== 加密配置 ==========
    const val AES_KEY_SIZE = 256
    const val DATABASE_PASSPHRASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    
    // ========== NTP 配置 ==========
    const val NTP_TIMEOUT_MS = 3000
    const val NTP_PORT = 123
    val NTP_SERVERS = listOf(
        "time.google.com",
        "time.cloudflare.com", 
        "pool.ntp.org"
    )
    
    // ========== 证据包配置 ==========
    const val MAX_EVIDENCE_FILE_SIZE = 100 * 1024 * 1024 // 100MB
    const val EVIDENCE_COMPRESSION_LEVEL = 6
    const val MERKLE_TREE_HASH_ALGORITHM = "SHA-256"
    
    // ========== AI 配置 ==========
    const val AI_RESPONSE_TIMEOUT_MS = 90_000L // 90秒
    const val AI_MAX_RETRIES = 3
    const val AI_CACHE_EXPIRY_HOURS = 24
    
    // ========== 录屏配置 ==========
    const val SCREEN_RECORD_BIT_RATE = 2_000_000 // 2Mbps
    const val SCREEN_RECORD_FRAME_RATE = 30
    const val SCREEN_RECORD_MAX_DURATION_MS = 30 * 60 * 1000L // 30分钟
    
    // ========== 安全配置 ==========
    const val PIN_MIN_LENGTH = 4
    const val PIN_MAX_LENGTH = 8
    const val PBKDF2_ITERATIONS = 100_000
    const val MAX_LOGIN_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L // 5分钟
    
    // ========== UI 配置 ==========
    const val ANIMATION_DURATION_MS = 300
    const val DEBOUNCE_DELAY_MS = 500
}
