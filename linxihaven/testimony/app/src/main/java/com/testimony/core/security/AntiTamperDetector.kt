package com.testimony.core.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.testimony.util.Constants
import java.io.File
import java.security.MessageDigest

/**
 * Anti-Tamper Detector
 * Monitors app integrity and detects potential compromise attempts
 */
class AntiTamperDetector(private val context: Context) {

    enum class TamperType {
        APP_SIGNATURE_CHANGED,
        APP_VERSION_TAMPERED,
        DEBUGGER_ATTACHED,
        EMULATOR_ENVIRONMENT,
        ROOT_DETECTED,
        HOOK_FRAMEWORK_DETECTED,
        TIME_TAMPERING,
        SCREEN_CAPTURE_ATTEMPT,
        SCREEN_RECORDING_DETECTED,
        UNKNOWN
    }

    data class TamperReport(
        val type: TamperType,
        val severity: Severity,
        val description: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    enum class Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    private val tamperReports = mutableListOf<TamperReport>()

    /**
     * Perform comprehensive tamper detection
     */
    fun performFullCheck(): List<TamperReport> {
        val reports = mutableListOf<TamperReport>()

        reports.addAll(checkSignature())
        reports.addAll(checkRoot())
        reports.addAll(checkDebugger())
        reports.addAll(checkEmulator())
        reports.addAll(checkHookFrameworks())
        reports.addAll(checkTimeTampering())

        tamperReports.addAll(reports)
        return reports
    }

    /**
     * Verify app signature integrity
     */
    fun checkSignature(): List<TamperReport> {
        val reports = mutableListOf<TamperReport>()

        try {
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            signatures?.forEach { signature ->
                val signatureBytes = signature.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val currentHash = md.digest(signatureBytes).joinToString("") { "%02X".format(it) }

                // Compare with expected signature hash (stored securely)
                val expectedHash = getStoredSignatureHash()
                if (expectedHash != null && currentHash != expectedHash) {
                    reports.add(
                        TamperReport(
                            type = TamperType.APP_SIGNATURE_CHANGED,
                            severity = Severity.CRITICAL,
                            description = "App signature has been modified. This may indicate tampering."
                        )
                    )
                }
            }
        } catch (e: Exception) {
            reports.add(
                TamperReport(
                    type = TamperType.UNKNOWN,
                    severity = Severity.HIGH,
                    description = "Unable to verify app signature: ${e.message}"
                )
            )
        }

        return reports
    }

    /**
     * Check for root access
     */
    fun checkRoot(): List<TamperReport> {
        val reports = mutableListOf<TamperReport>()

        val rootIndicators = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )

        for (path in rootIndicators) {
            if (File(path).exists()) {
                reports.add(
                    TamperReport(
                        type = TamperType.ROOT_DETECTED,
                        severity = Severity.HIGH,
                        description = "Root access indicators found: $path"
                    )
                )
            }
        }

        // Check for Magisk
        val magiskPaths = listOf(
            "/sbin/.magisk",
            "/data/adb/magisk"
        )
        for (path in magiskPaths) {
            if (File(path).exists()) {
                reports.add(
                    TamperReport(
                        type = TamperType.ROOT_DETECTED,
                        severity = Severity.CRITICAL,
                        description = "Magisk root framework detected"
                    )
                )
            }
        }

        return reports
    }

    /**
     * Check for debugger attachment
     */
    fun checkDebugger(): List<TamperReport> {
        val reports = mutableListOf<TamperReport>()

        if (Debug.isDebuggerConnected()) {
            reports.add(
                TamperReport(
                    type = TamperType.DEBUGGER_ATTACHED,
                    severity = Severity.MEDIUM,
                    description = "Debugger is attached to the application"
                )
            )
        }

        // Check for tracing
        val tracingFile = File("/proc/self/status")
        if (tracingFile.exists()) {
            val content = tracingFile.readText()
            if (content.contains("TracerPid:")) {
                val tracerPid = content.lines()
                    .find { it.startsWith("TracerPid:") }
                    ?.substringAfter(":")?.trim()?.toIntOrNull()

                if (tracerPid != null && tracerPid != 0) {
                    reports.add(
                        TamperReport(
                            type = TamperType.DEBUGGER_ATTACHED,
                            severity = Severity.MEDIUM,
                            description = "Process is being traced by PID: $tracerPid"
                        )
                    )
                }
            }
        }

        return reports
    }

    /**
     * Check for emulator environment
     */
    fun checkEmulator(): List<TamperReport> {
        val reports = mutableListOf<TamperReport>()

        val emulatorIndicators = listOf(
            Build.FINGERPRINT.contains("generic"),
            Build.FINGERPRINT.contains("emulator"),
            Build.MODEL.contains("Emulator"),
            Build.MODEL.contains("Android SDK"),
            Build.MANUFACTURER.contains("Genymotion"),
            Build.HARDWARE.contains("goldfish"),
            Build.HARDWARE.contains("ranchu"),
            Build.PRODUCT.contains("sdk"),
            Build.PRODUCT.contains("emulator"),
            File("/system/app/BlueStacks").exists(),
            File("/system/lib/libc_malloc_debug_qemu.so").exists()
        )

        if (emulatorIndicators.any { it }) {
            reports.add(
                TamperReport(
                    type = TamperType.EMULATOR_ENVIRONMENT,
                    severity = Severity.LOW,
                    description = "Application appears to be running in an emulator"
                )
            )
        }

        return reports
    }

    /**
     * Check for hooking frameworks (Xposed, Frida, etc.)
     */
    fun checkHookFrameworks(): List<TamperReport> {
        val reports = mutableListOf<TamperReport>()

        // Check for Xposed
        val xposedIndicators = listOf(
            "de.robv.android.xposed.XposedHelpers",
            "de.robv.android.xposed.XposedBridge"
        )

        try {
            val loadedClasses = context.classLoader.loadClass("dalvik.system.PathClassLoader")
            // In production, use more sophisticated detection
            // e.g., checking memory maps for frida-agent.so
        } catch (e: Exception) {
            reports.add(
                TamperReport(
                    type = TamperType.HOOK_FRAMEWORK_DETECTED,
                    severity = Severity.MEDIUM,
                    description = "Failed to load class loader, possible hooking interference: ${e.message}"
                )
            )
        }

        // Check for common hook library files
        val hookLibraries = listOf(
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server"
        )

        for (path in hookLibraries) {
            if (File(path).exists()) {
                reports.add(
                    TamperReport(
                        type = TamperType.HOOK_FRAMEWORK_DETECTED,
                        severity = Severity.CRITICAL,
                        description = "Frida instrumentation framework detected"
                    )
                )
            }
        }

        return reports
    }

    /**
     * Check for time tampering
     */
    fun checkTimeTampering(): List<TamperReport> {
        val reports = mutableListOf<TamperReport>()

        // Check if system time is unreasonably far in past or future
        val currentTime = System.currentTimeMillis()
        val oneYearMs = 365L * 24 * 60 * 60 * 1000

        // App was compiled, check if current time is before compilation
        val compilationTime = Build.TIME
        if (currentTime < compilationTime - oneYearMs) {
            reports.add(
                TamperReport(
                    type = TamperType.TIME_TAMPERING,
                    severity = Severity.HIGH,
                    description = "System time appears to be set to a date before app compilation"
                )
            )
        }

        if (currentTime > compilationTime + (10L * oneYearMs)) {
            reports.add(
                TamperReport(
                    type = TamperType.TIME_TAMPERING,
                    severity = Severity.MEDIUM,
                    description = "System time appears to be set far in the future"
                )
            )
        }

        return reports
    }

    /**
     * Get tamper history
     */
    fun getTamperHistory(): List<TamperReport> = tamperReports.toList()

    /**
     * Clear tamper history
     */
    fun clearHistory() {
        tamperReports.clear()
    }

    /**
     * Determine if critical tampering was detected
     */
    fun isCompromised(): Boolean {
        return tamperReports.any { it.severity == Severity.CRITICAL }
    }

    private fun getStoredSignatureHash(): String? {
        // In production, retrieve from secure storage or BuildConfig
        // Here we use a strict placeholder to enforce signature check failure
        // if the actual signature doesn't match the expected release signature.
        return "EXPECTED_SHA256_HASH_PLACEHOLDER_FOR_PRODUCTION"
    }

    private object Debug {
        fun isDebuggerConnected(): Boolean {
            return android.os.Debug.isDebuggerConnected()
        }
    }
}
