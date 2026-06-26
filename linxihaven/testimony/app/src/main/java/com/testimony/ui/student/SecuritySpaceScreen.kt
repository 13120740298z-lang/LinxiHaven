package com.testimony.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.testimony.ui.theme.*

/**
 * 安全空间屏幕
 * 提供录制证据的安全环境
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySpaceScreen(
    onNavigateToInterview: () -> Unit,
    onComplete: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableLongStateOf(0L) }
    var showDuration by remember { mutableStateOf("00:00") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("安全空间", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary
                ),
                navigationIcon = {
                    IconButton(onClick = onComplete) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 安全状态指示
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Shield,
                        null,
                        tint = Success,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("环境安全", fontWeight = FontWeight.Medium)
                        Text(
                            "所有操作将被加密记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 录制状态
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) Error.copy(alpha = 0.1f) else Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isRecording) {
                        // 录制中指示
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Error)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("录制中", color = Error, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(showDuration, style = MaterialTheme.typography.displaySmall)
                    } else {
                        Icon(
                            Icons.Default.Videocam,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = Primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("准备就绪", color = Primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 录制控制按钮
            Button(
                onClick = {
                    isRecording = !isRecording
                    if (!isRecording) {
                        // 停止录制后进入访谈
                        onNavigateToInterview()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Error else Primary
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRecording) "停止录制" else "开始录制", fontSize = MaterialTheme.typography.titleMedium.fontSize)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 提示信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = Info, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("温馨提示", fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• 录制过程中请保持网络连接\n• 证据将自动加密存储\n• 完成后可选择导出方式",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
