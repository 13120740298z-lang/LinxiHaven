package com.testimony.ui.student

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.testimony.core.evidence.AntiForgeryCertificate
import com.testimony.ui.theme.*

/**
 * ╔═══════════════════════════════════════════════════════╗
 * ║     📋 防伪证书详情页 - 核心展示界面                    ║
 * ║                                                        ║
 * ║  展示内容：                                             ║
 * ║  1. 证书头部（编号、时间）                              ║
 * ║  2. ★ 数字指纹（SHA-256）★ - 最核心的展示区域          ║
 * ║  3. 文件元信息                                         ║
 * ║  4. 时间锚定列表                                       ║
 * ║  5. ★ 独立验证流程指南 ★                               ║
 * ║  6. 操作按钮（复制/分享/导出）                          ║
 * ╚═══════════════════════════════════════════════════════╝
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificateDetailScreen(
    certificate: AntiForgeryCertificate,
    onBack: () -> Unit = {},
    onShare: (AntiForgeryCertificate) -> Unit = {},
    onExport: (AntiForgeryCertificate) -> Unit = {}
) {
    val context = LocalContext.current
    var showVerifyGuide by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔐 防伪证书", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { onShare(certificate) }) {
                        Icon(Icons.Default.Share, contentDescription = "分享", tint = Color.White)
                    }
                    IconButton(onClick = { onExport(certificate) }) {
                        Icon(Icons.Default.Download, contentDescription = "导出", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                    )
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // ========== 1. 证书头部 ==========
            CertificateHeaderCard(certificate)

            Spacer(modifier = Modifier.height(16.dp))

            // ========== 2. 文件信息卡片 ==========
            FileInfoCard(certificate)

            Spacer(modifier = Modifier.height(16.dp))

            // ========== 3. ★★★ 数字指纹展示区 ★★★ ==========
            FingerprintDisplayCard(certificate)

            Spacer(modifier = Modifier.height(16.dp))

            // ========== 4. 快速验证说明 ==========
            QuickVerificationHint()

            Spacer(modifier = Modifier.height(16.dp))

            // ========== 5. 时间锚定 ==========
            TimestampAnchorsCard(certificate)

            Spacer(modifier = Modifier.height(20.dp))

            // ========== 6. 详细验证指南（可折叠） ==========
            AnimatedVisibility(
                visible = showVerifyGuide,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                VerifyGuideSection(certificate)
            }

            Button(
                onClick = { showVerifyGuide = !showVerifyGuide },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
            ) {
                Text(if (showVerifyGuide) "收起验证指南" else "展开完整验证指南")
                Icon(
                    if (showVerifyGuide) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ========== 7. 底部操作区 ==========
            ActionButtonsRow(certificate, context)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ==================== 子组件 ====================

/**
 * 证书头部卡片
 */
@Composable
private fun CertificateHeaderCard(cert: AntiForgeryCertificate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Brush.verticalGradient(
            colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFFA855F7))
        ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.VerifiedUser,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "🔐 防伪存证证书",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 证书编号（等宽字体）
            Text(
                text = cert.certificateId,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

/**
 * 文件信息卡片
 */
@Composable
private fun FileInfoCard(cert: AntiForgeryCertificate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(getTypeColor(cert.fileType).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    getTypeIcon(cert.fileType),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = getTypeColor(cert.fileType)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(cert.fileName, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(
                    "${cert.fileSizeHuman} · ${getTypeLabel(cert.fileType)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray
                )
            }
        }
    }
}

/**
 * ★★★ 数字指纹展示区 - 最核心的视觉组件 ★★★
 */
@Composable
private fun FingerprintDisplayCard(cert: AntiForgeryCertificate) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
        border = BorderStroke(1.dp, Color(0xFF312E81).copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部渐变线动画效果
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(Brush.horizontalGradient(
                        colors = listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF06B6D4))
                    ))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 算法标签
            Text(
                text = "🔒 SHA-256 数字指纹 (Digital Fingerprint)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 哈希值主体（等宽字体，大号显示）
            ClickableText(
                text = cert.digitalFingerprint.hashValue,
                onClick = {
                    copyToClipboard(context, cert.digitalFingerprint.hashValue)
                    copied = true
                },
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF67E8F9),
                    letterSpacing = 2.5.sp
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            if (copied) {
                Text("✓ 已复制", color = Color.Green, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 算法信息和验证码
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "算法：${cert.digitalFingerprint.algorithm}",
                    fontSize = 12.sp,
                    color = Color(0xFFA78BFA)
                )
                HorizontalDivider(
                    modifier = Modifier.height(14.dp).width(1.dp),
                    color = Color(0xFF4C1D95)
                )
                Text(
                    "验证码：",
                    fontSize = 12.sp,
                    color = Color(0xFFA78BFA)
                )
                Text(
                    cert.verificationCode,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFBBF24)
                )
            }
        }
    }
}

/**
 * 快速验证提示
 */
@Composable
private fun QuickVerificationHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        border = BorderStroke(1.dp, Color(0xFF86EFAC))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF166534), modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("✨ 快速验证方法", fontWeight = FontWeight.Bold, color = Color(0xFF166534), fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    "将此哈希值与对方声称\"P图\"的文件重新计算的 SHA-256 对比\n→ 结果不一致 = 对方的文件确实被修改过 ✓",
                    fontSize = 13.sp,
                    color = Color(0xFF15803D),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * 时间锚定卡片
 */
@Composable
private fun TimestampAnchorsCard(cert: AntiForgeryCertificate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
        border = BorderStroke(1.dp, Color(0xFFE9D5FF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF7C3AED))
                Spacer(Modifier.width(8.dp))
                Text("⏱️ 多源时间锚定", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF6D28D9))
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            cert.timestampAnchors.forEachIndexed { index, anchor ->
                if (index > 0) Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("• ", color = Color(0xFFA78BFA))
                    Text(anchor.source, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.width(100.dp), color = Color(0xFF581C87))
                    Text(": ${anchor.time}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Gray)
                }
                
                anchor.note?.let { note ->
                    Text(
                        "  ($note)",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(start = 114.dp)
                    )
                }
            }
        }
    }
}

/**
 * 详细验证指南
 */
@Composable
private fun VerifyGuideSection(cert: AntiForgeryCertificate) {
    val guide = cert.howToVerify

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Text(
                "📋 如何独立验证文件真伪？",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1D4ED8)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 步骤列表
            VerifyStepItem(number = 1, text = guide.step1)
            VerifyStepItem(number = 2, text = guide.step2)
            VerifyStepItem(number = 3, text = guide.step3)
            VerifyStepItem(number = 4, text = guide.step4)

            Spacer(modifier = Modifier.height(16.dp))

            // 命令示例
            Text("💻 验证命令", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1E40AF))

            Spacer(modifier = Modifier.height(10.dp))

            CommandBox(label = "Windows", command = guide.windowsCommand)
            CommandBox(label = "Mac / Linux", command = guide.linuxCommand)
            CommandBox(label = "Python", command = guide.pythonCommand)

            Spacer(modifier = Modifier.height(16.dp))

            // 预期结果
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF064E3B))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("预期正确结果:", color = Color(0xFF6EE7B7), fontSize = 12.sp)
                    Text(
                        guide.expectedResult,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4ADE80),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * 步骤项组件
 */
@Composable
private fun VerifyStepItem(number: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(0xFF4F46E5)),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontSize = 14.sp, color = Color(0xFF1E40AF), lineHeight = 20.sp)
    }
}

/**
 * 命令框组件
 */
@Composable
private fun CommandBox(label: String, command: String) {
    Column {
        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E293B))
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(command, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF38BDF8))
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * 底部操作按钮行
 */
@Composable
private fun ActionButtonsRow(cert: AntiForgeryCertificate, context: Context) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = { copyToClipboard(context, cert.digitalFingerprint.hashValue) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("复制指纹")
        }

        OutlinedButton(
            onClick = { /* 分享 */ },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("分享证书")
        }

        OutlinedButton(
            onClick = { /* 导出PDF */ },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7C3AED))
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("导出PDF")
        }
    }
    
    // 免责声明
    Text(
        text = "本证书基于 SHA-256 密码学安全哈希算法生成。\n任何人可用公开算法独立验证，无需信任第三方。",
        style = MaterialTheme.typography.bodySmall,
        color = Gray,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        lineHeight = 17.sp
    )
}

// ==================== 工具函数 ====================

@Composable
private fun getTypeIcon(type: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        "image" -> Icons.Default.Image
        "video" -> Icons.Default.VideoFile
        "audio" -> Icons.Default.AudioFile
        else -> Icons.Default.Description
    }
}

@Composable
private fun getTypeColor(type: String): Color {
    return when (type) {
        "image" -> Color(0xFF3B82F6)
        "video" -> Color(0xFF8B5CF6)
        "audio" -> Color(0xFFF59E0B)
        else -> Color.Gray
    }
}

@Composable
private fun getTypeLabel(type: String): String {
    return when (type) {
        "image" -> "图片"
        "video" -> "视频"
        "audio" -> "音频"
        "document" -> "文档"
        else -> "文件"
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("证言指纹", text)
    clipboard.setPrimaryClip(clip)
}
