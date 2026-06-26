package com.testimony.core.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.testimony.util.Constants
import net.sqlcipher.database.SQLiteDatabase as CipherSQLiteDatabase
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypted Database for storing evidence metadata
 * Uses SQLCipher with Android Keystore-backed master key
 */
class SecureDatabase(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "TestimonyDatabaseMasterKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        // Initialize SQLCipher
        CipherSQLiteDatabase.loadLibs(context)

        // Ensure master key exists
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            generateMasterKey()
        }
    }

    private fun generateMasterKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keySpec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // Set true for extra security
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keySpec)
        keyGenerator.generateKey()
    }

    private fun getMasterKey(): SecretKey {
        return (keyStore.getEntry(MASTER_KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Get database passphrase derived from Keystore key
     * This creates a unique passphrase per installation
     */
    fun getDatabasePassphrase(): ByteArray {
        val key = getMasterKey()
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )

        // Derive database key using Keystore-wrapped cipher
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val input = "$deviceId:${context.packageName}:$DATABASE_SALT".toByteArray()
        val encrypted = cipher.doFinal(input)

        return encrypted
    }

    /**
     * Get writable database with secure passphrase
     */
    fun getWritableDatabase(): CipherSQLiteDatabase {
        val passphrase = getDatabasePassphrase()
        return CipherSQLiteDatabase.openOrCreateDatabase(
            context.getDatabasePath(DATABASE_NAME),
            passphrase,
            null
        )
    }

    /**
     * Get readable database with secure passphrase
     */
    fun getReadableDatabase(): CipherSQLiteDatabase {
        val passphrase = getDatabasePassphrase()
        return CipherSQLiteDatabase.openOrCreateDatabase(
            context.getDatabasePath(DATABASE_NAME),
            passphrase,
            null
        )
    }

    /**
     * Securely store PIN using PBKDF2
     */
    fun verifyPin(storedPinHash: String, inputPin: String): Boolean {
        val inputHash = hashPin(inputPin)
        return secureCompare(storedPinHash, inputHash)
    }

    fun hashAndStorePin(pin: String): String {
        return hashPin(pin)
    }

    private fun hashPin(pin: String): String {
        // Use PBKDF2 with high iteration count
        val salt = getDeviceSalt()
        val spec = javax.crypto.spec.PBEKeySpec(
            pin.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            256
        )
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    private fun getDeviceSalt(): ByteArray {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        // Get or create salt in Keystore
        val saltAlias = "TestimonyPinSalt"
        if (!keyStore.containsAlias(saltAlias)) {
            val salt = ByteArray(32)
            java.security.SecureRandom().nextBytes(salt)

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val keySpec = KeyGenParameterSpec.Builder(
                saltAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keySpec)
            val saltKey = keyGenerator.generateKey()

            // Encrypt and store salt
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, saltKey)
            val encryptedSalt = cipher.doFinal(salt)

            // Store as encrypted blob in SharedPreferences
            context.getEncryptedPrefs().edit()
                .putString("salt", Base64.encodeToString(encryptedSalt, Base64.NO_WRAP))
                .apply()
        }

        // Retrieve and decrypt salt
        val storedSalt = context.getEncryptedPrefs().getString("salt", null)
            ?: throw IllegalStateException("Salt not found")

        return Base64.decode(storedSalt, Base64.NO_WRAP)
    }

    private fun secureCompare(a: String, b: String): Boolean {
        if (a.length != b.length) return false

        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    companion object {
        const val DATABASE_NAME = "testimony_secure.db"
        const val DATABASE_VERSION = 1
        private const val DATABASE_SALT = "TestimonyDatabaseSalt_v1"
        private const val PBKDF2_ITERATIONS = 100000

        // Table names
        const val TABLE_EVIDENCE = "evidence"
        const val TABLE_INTERVIEWS = "interviews"
        const val TABLE_OBSERVATIONS = "observations"
        const val TABLE_ASSESSMENTS = "assessments"
    }
}
