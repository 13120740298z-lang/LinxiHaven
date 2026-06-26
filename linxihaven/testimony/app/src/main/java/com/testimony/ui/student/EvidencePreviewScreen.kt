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
 * 证据预览屏幕
 * 展示生成的证据包并提供导出选项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidencePreviewScreen(
    onSaveLocally: () -> Unit,
    onShareToSchool: () -> Unit,
    onShareToParent: () -> Unit
) {
    var packageId by remember { mutableStateOf("EP_20240514_001") }
    var fileHash by remember { mutableStateOf("a7f3c8e9d2b1f5...") }
    var generatedAt by remember { mutableStateOf("2024-05-14 14:30:00") }
    var merkleRoot by remember { mutableStateOf("c9d8e7f6a5b4c3d2e1f0...") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("证据预览", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            // 成功状态
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Success.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = Success, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("证据包已生成", fontWeight = FontWeight.Bold)
                        Text(
                            "ID: $packageId",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 证据详情
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("证据详情", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    DetailRow("生成时间", generatedAt)
                    DetailRow("证据包哈希", fileHash)
                    DetailRow("Merkle根", merkleRoot)
                    DetailRow("证据数量", "3 项")
                    DetailRow("完整度", "100%")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 完整性验证
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Info.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Verified, null, tint = Info)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("完整性验证", fontWeight = FontWeight.Medium)
                        Text(
                            "哈希链验证通过 | 时间锚定有效",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(Icons.Default.CheckCircle, null, tint = Success)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 导出选项
            Text("选择导出方式", fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 16.dp))

            ExportButton(
                icon = Icons.Default.PhoneAndroid,
                title = "本地加密保存",
                subtitle = "安全存储在您的设备",
                onClick = onSaveLocally
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExportButton(
                icon = Icons.Default.School,
                title = "匿名提交学校",
                subtitle = "加密传输，保护隐私",
                onClick = onShareToSchool
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExportButton(
                icon = Icons.Default.FamilyRestroom,
                title = "分享给家长",
                subtitle = "仅家长可查看",
                onClick = onShareToParent
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ExportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}
