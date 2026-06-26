package com.testimony.core.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 应用伪装管理器 - 简化版
 * 支持计算器伪装、PIN验证、秘密手势
 */
class AppDisguiseManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "disguise_prefs"
        private const val KEY_IS_DISGUISED = "is_disguised"
        private const val KEY_DISGUISE_TYPE = "disguise_type"
        private const val KEY_GESTURE_PATTERN = "gesture_pattern"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val PIN_SALT_KEY = "TestimonyPinSaltKey"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_SIZE = 12
        
        private const val PBKDF2_ITERATIONS = 100_000
        private const val HASH_ALGORITHM = "SHA-256"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    init { ensureKeyExists() }

    private fun ensureKeyExists() {
        if (!keyStore.containsAlias(PIN_SALT_KEY)) {
            generateSaltKey()
            // 生成随机盐并存储
            val salt = generateSalt()
            val encryptedSalt = encryptSaltToKeystore(salt)
            prefs.edit()
                .putString(KEY_PIN_SALT, encryptedSalt)
                .apply()
        }
    }

    private fun generateSaltKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                PIN_SALT_KEY,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
             .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
             .setKeySize(256)
             .build()
        )
        keyGenerator.generateKey()
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(32)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun encryptSaltToKeystore(salt: ByteArray): String {
        val entry = keyStore.getEntry(PIN_SALT_KEY, null) as KeyStore.SecretKeyEntry
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, entry.secretKey)
        val encryptedSalt = cipher.doFinal(salt)
        return Base64.encodeToString(cipher.iv + encryptedSalt, Base64.NO_WRAP)
    }

    private fun decryptSaltFromKeystore(encryptedB64: String): ByteArray {
        val entry = keyStore.getEntry(PIN_SALT_KEY, null) as KeyStore.SecretKeyEntry
        val combined = Base64.decode(encryptedB64, Base64.NO_WRAP)
        val iv = combined.sliceArray(0 until GCM_IV_SIZE)
        val encrypted = combined.sliceArray(GCM_IV_SIZE until combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, entry.secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(encrypted)
    }

    /** 使用 PBKDF2 哈希 PIN */
    fun hashPin(pin: String): String {
        val encryptedSalt = prefs.getString(KEY_PIN_SALT, null) 
            ?: throw IllegalStateException("盐未初始化")
        val salt = decryptSaltFromKeystore(encryptedSalt)
        
        val md = java.security.MessageDigest.getInstance(HASH_ALGORITHM)
        var result = md.digest(salt + pin.toByteArray())
        
        // PBKDF2 简化实现
        repeat(PBKDF2_ITERATIONS - 1) {
            md.reset()
            result = md.digest(result + pin.toByteArray())
        }
        
        return result.joinToString("") { "%02x".format(it) }
    }

    /** 验证 PIN */
    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return constantTimeEquals(hashPin(pin), storedHash)
    }

    /** 设置 PIN */
    fun setPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
    }

    /** 常数时间比较 - 防时序攻击 */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }

    // ========== 伪装控制 ==========

    fun isDisguised(): Boolean = prefs.getBoolean(KEY_IS_DISGUISED, true)

    fun setDisguised(disguised: Boolean) {
        prefs.edit().putBoolean(KEY_IS_DISGUISED, disguised).apply()
    }

    fun getDisguiseType(): DisguiseType {
        val typeName = prefs.getString(KEY_DISGUISE_TYPE, null)
        return typeName?.let { DisguiseType.valueOf(it) } ?: DisguiseType.CALCULATOR
    }

    fun setDisguiseType(type: DisguiseType) {
        prefs.edit().putString(KEY_DISGUISE_TYPE, type.name).apply()
    }

    fun exitDisguiseMode(pin: String): Boolean {
        if (verifyPin(pin)) {
            setDisguised(false)
            return true
        }
        return false
    }

    // ========== 秘密手势 ==========

    fun validateSecretGesture(gesturePattern: List<Int>): Boolean {
        val storedPattern = prefs.getString(KEY_GESTURE_PATTERN, null) ?: return false
        return storedPattern == gesturePattern.joinToString(",")
    }

    fun setSecretGesture(pattern: List<Int>) {
        prefs.edit().putString(KEY_GESTURE_PATTERN, pattern.joinToString(",")).apply()
    }

    fun getAvailableDisguises(): List<DisguiseType> = DisguiseType.entries

    enum class DisguiseType {
        CALCULATOR,
        NOTES,
        CLOCK,
        FLASHLIGHT
    }
}
