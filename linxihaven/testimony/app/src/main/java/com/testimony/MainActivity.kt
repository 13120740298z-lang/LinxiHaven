package com.testimony

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.testimony.core.security.AppDisguiseManager
import com.testimony.ui.parent.ParentHomeScreen
import com.testimony.core.evidence.AntiForgeryCertificate
import com.testimony.ui.student.*
import com.testimony.ui.theme.TestimonyTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var appDisguiseManager: AppDisguiseManager
    private val viewModel: MainViewModel by lazy { MainViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appDisguiseManager = (application as TestimonyApplication).appDisguiseManager
        
        setContent {
            TestimonyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val startDestination = if (appDisguiseManager.isDisguised()) "calculator" else "mode_selection"
                    TestimonyApp(startDestination = startDestination)
                }
            }
        }
    }
}

/** 主应用导航控制器 */
@Composable
fun TestimonyApp(startDestination: String) {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = startDestination) {
        // 伪装计算器模式
        composable("calculator") {
            CalculatorScreen(
                onSecretGesture = { navController.navigate("pin_entry") },
                onSecretPattern = { navController.navigate("pin_entry") }
            )
        }
        
        composable("pin_entry") {
            PinEntryScreen(
                onPinEntered = { pin ->
                    navController.navigate("mode_selection") {
                        popUpTo("calculator") { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }
        
        // 模式选择
        composable("mode_selection") {
            ModeSelectionScreen(
                onSelectStudent = { navController.navigate("student_home") },
                onSelectParent = { navController.navigate("parent_home") }
            )
        }
        
        // 学生模式
        composable("student_home") {
            StudentHomeScreen(
                onNavigateToSecuritySpace = { navController.navigate("security_space") },
                onNavigateToInterview = { navController.navigate("guided_interview") },
                onNavigateToEvidence = { navController.navigate("evidence_preview") },
                onNavigateToAntiForgery = { navController.navigate("anti_forgery") },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("security_space") {
            com.testimony.ui.student.SecuritySpaceScreen(
                onNavigateToInterview = { navController.navigate("guided_interview") },
                onComplete = { navController.popBackStack("student_home", false) }
            )
        }
        
        composable("guided_interview") {
            com.testimony.ui.student.GuidedInterviewScreen(
                onComplete = { navController.navigate("evidence_preview") },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("evidence_preview") {
            com.testimony.ui.student.EvidencePreviewScreen(
                onSaveLocally = { navController.popBackStack("student_home", false) },
                onShareToSchool = { navController.popBackStack("student_home", false) },
                onShareToParent = { navController.popBackStack("student_home", false) }
            )
        }
        
        // ★★★ 防伪存证工作流 ★★★
        composable("anti_forgery") {
            AntiForgeryScreen(
                onCertificateGenerated = { cert ->
                    // 保存证书到 ViewModel 或导航到详情页
                    navController.navigate("certificate_detail")
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("certificate_detail") {
            // 这里可以从 ViewModel 获取最新证书，或通过 SavedStateHandle 传递
            CertificateDetailScreen(
                certificate = remember { generateDemoCertificate() }, // 实际应从 ViewModel 获取
                onBack = { navController.popBackStack() }
            )
        }
        
        // 家长模式
        composable("parent_home") {
            ParentHomeScreen(
                onNavigateToObservation = { navController.navigate("observation_input") },
                onNavigateToHistory = { navController.navigate("observation_history") },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("observation_input") {
            com.testimony.ui.parent.ObservationInputScreen(
                onSubmit = { navController.navigate("risk_assessment") },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("risk_assessment") {
            com.testimony.ui.parent.RiskAssessmentScreen(
                onViewScript = { navController.navigate("communication_script") },
                onBack = { navController.popBackStack("parent_home", false) }
            )
        }
        
        composable("communication_script") {
            com.testimony.ui.parent.CommunicationScriptScreen(
                onComplete = { navController.popBackStack("parent_home", false) }
            )
        }
        
        composable("observation_history") {
            com.testimony.ui.parent.ObservationHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/** 主 ViewModel - 管理全局状态 */
class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState
    
    fun setDisguisedMode(disguised: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDisguised = disguised)
        }
    }
}

data class MainUiState(
    val isDisguised: Boolean = true,
    val currentMode: AppMode = AppMode.UNKNOWN
)

enum class AppMode { STUDENT, PARENT, UNKNOWN }

/** 生成演示证书（实际应从 ViewModel 获取） */
private fun generateDemoCertificate(): AntiForgeryCertificate {
    return AntiForgeryCertificate(
        certificateId = "CERT-20260626-DEMOABC123456",
        fileName = "screenshot_evidence.png",
        fileType = "image",
        fileSize = 245888,
        fileSizeHuman = "240.1 KB",
        digitalFingerprint = com.testimony.core.evidence.DigitalFingerprint(
            hashValue = "a3f2b8c9d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2"
        ),
        verificationCode = "A3F2B8C9-D4E5F6A7",
        verificationShort = "A3F2B8C9D4E5",
        timestampAnchors = listOf(
            com.testimony.core.evidence.TimestampAnchor("本地时间", "2026-06-26 21:11:00.000", "Asia/Shanghai"),
            com.testimony.core.evidence.TimestampAnchor("UTC", "2026-06-26T13:11:00Z", "UTC"),
            com.testimony.core.evidence.TimestampAnchor("Unix", "1750959860", null)
        ),
        certificateHash = "cert_hash_demo_123",
        generatedAt = "2026-06-26T13:11:00Z",
        howToVerify = com.testimony.core.evidence.VerifyGuide(
            step1 = "保存原始文件", step2 = "计算SHA-256", step3 = "对比哈希值", step4 = "一致则真",
            windowsCommand = "CertUtil -hashfile screenshot.png SHA256",
            linuxCommand = "sha256sum screenshot.png",
            macOSCommand = "shasum -a 256 screenshot.png",
            pythonCommand = "python -c 'import hashlib;print(hashlib.sha256(open(\"screenshot.png\",\"rb\").read()).hexdigest())'",
            expectedResult = "a3f2b8c9d4e5..."
        )
    )
}
