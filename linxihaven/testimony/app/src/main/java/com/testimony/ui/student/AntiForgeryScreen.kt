package com.testimony.ui.student

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.testimony.core.evidence.AntiForgeryCertificate
import com.testimony.core.evidence.AntiForgeryEngine
import com.testimony.core.evidence.PendingFile
import com.testimony.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

/**
 * ╔═══════════════════════════════════════════════════════╗
 * ║     🔐 防伪存证工作流 - 主界面                          ║
 * ║                                                        ║
 * ║  功能：                                                 ║
 * ║  1. 从相册选择 / 拍照上传证据文件                       ║
 * ║  2. 预览已选文件列表                                     ║
 * ║  3. 一键生成密码学级防伪证书                             ║
 * ║  4. 查看完整证书与验证指南                               ║
 * ╚═══════════════════════════════════════════════════════╝
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntiForgeryScreen(
    onCertificateGenerated: (AntiForgeryCertificate) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var selectedFiles by remember { mutableStateOf<List<SelectedFileInfo>>(emptyList()) }
    var isGenerating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 文件选择器（多选）
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            // 将 Uri 转换为文件信息显示
            val newFiles = uris.mapNotNull { uri ->
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameCol = cursor.getColumnIndex("_display_name")
                    val sizeCol = cursor.getColumnIndex("_size")
                    if (cursor.moveToFirst()) {
                        SelectedFileInfo(
                            uri = uri,
                            name = cursor.getString(nameCol) ?: "unknown",
                            size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L,
                            mimeType = context.contentResolver.getType(uri) ?: "*/*"
                        )
                    } else null
                }
            }
            selectedFiles = selectedFiles + newFiles
        }
    }

    // 照相机启动器
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        // 相机拍照后的处理可以在这里添加
        if (success) {
            // 可以添加临时文件的 URI 处理
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔐 防伪存证", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Brush.linearGradient(
                        colors = listOf(Color(0xFF6D28D9), Color(0xFF7C3AED), Color(0xFFA855F7))
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

            // ========== 核心价值说明卡片 ==========
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎯 ", fontSize = 24.sp)
                        Text("为什么需要防伪存证？", fontWeight = FontWeight.ExtraBold, color = Color(0xFF7C3AED), fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    ValueCheckItem("被质疑\"P图污蔑\"", "用 SHA-256 数字指纹证明文件未被篡改")
                    ValueCheckItem("对方否认说过这话", "时间锚定证明文件在特定时刻确实存在")
                    ValueCheckItem("证据被篡改质疑", "哈希值一改就变，数学保证真实性")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ========== 文件上传区域 ==========
            Text("📁 选择要存证的证据文件", fontWeight = FontWeight.Bold, fontSize = 17.sp)

            Spacer(modifier = Modifier.height(12.dp))

            // 拖拽/点击上传区域样式
            UploadZoneCard(
                onClick = { filePickerLauncher.launch("image/*,video/*,audio/*,.pdf") },
                fileCount = selectedFiles.size,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 快捷操作按钮行
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { /* 打开相机 */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("拍照取证")
                }

                OutlinedButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("更多类型")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ========== 已选文件列表 ==========
            if (selectedFiles.isNotEmpty()) {
                Text("✅ 已选择 ${selectedFiles.size} 个文件", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)

                Spacer(modifier = Modifier.height(10.dp))

                selectedFiles.forEachIndexed { index, fileInfo ->
                    FileListItem(
                        fileInfo = fileInfo,
                        onRemove = {
                            selectedFiles = selectedFiles.toMutableList().also { it.removeAt(index) }
                        }
                    )

                    if (index < selectedFiles.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ========== 生成证书按钮 ==========
                Button(
                    onClick = {
                        isGenerating = true
                        scope.launch {
                            try {
                                // 将 Uri 转为临时文件并计算哈希
                                val pendingFiles = selectedFiles.map { sf ->
                                    // 实际实现需要将 Uri 复制到临时文件
                                    PendingFile(
                                        file = File(context.cacheDir, sf.name), // 占位，实际需处理Uri
                                        name = sf.name,
                                        size = sf.size,
                                        mimeType = sf.mimeType
                                    ).also { pf ->
                                        // 异步计算指纹
                                        // pf.fingerprint = AntiForgeryEngine.computeFileHash(pf.file)
                                    }
                                }

                                // 生成证书
                                val result = AntiForgeryEngine.generateBatchCertificates(
                                    files = pendingFiles,
                                    reportId = "RPT-${System.currentTimeMillis()}",
                                    mapOf("source" to "android_app")
                                )

                                if (result.certificates.isNotEmpty()) {
                                    onCertificateGenerated(result.certificates.first())
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isGenerating && selectedFiles.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                        )
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("正在计算数字指纹...", color = Color.White)
                    } else {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "🔐 生成防伪存证证书",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Text(
                    text = "⚡ 文件将在本地计算 SHA-256 哈希值，不会上传到任何外部服务器",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            } else {
                // 空状态提示
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "还没有选择文件",
                            color = Color.Gray,
                            fontSize = 15.sp
                        )
                        Text(
                            "点击上方区域添加截图、录音或视频",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ========== 子组件定义 ==========

/**
 * 上传区域卡片
 */
@Composable
private fun UploadZoneCard(
    onClick: () -> Unit,
    fileCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (fileCount > 0) Color(0xFFF0FDF4) else Color(0xFFEEF2FF)
        ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (fileCount > 0) 2.dp else 2.dp,
            color = if (fileCount > 0) Color(0xFF86EFAC) else Color(0xFFC7D2FE)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = if (fileCount > 0) "✅" else "📤", fontSize = 42.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (fileCount > 0) "再添加更多文件" else "点击或拖拽上传证据文件",
                fontWeight = FontWeight.Bold,
                color = if (fileCount > 0) Color(0xFF166534) else Color(0xFF4338CA),
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "支持截图、照片、录音、视频、PDF等\n" +
                      if (fileCount > 0)"已选择 $fileCount 个文件"
                      else "文件将在本地计算哈希指纹，不会外传",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 已选文件列表项
 */
@Composable
private fun FileListItem(
    fileInfo: SelectedFileInfo,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文件图标/缩略图
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(getFileTypeColor(fileInfo.mimeType).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    fileInfo.mimeType.startsWith("image/") -> {
                        AsyncImage(
                            model = fileInfo.uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Icon(
                            getFileTypeIcon(fileInfo.mimeType),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = getFileTypeColor(fileInfo.mimeType)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 文件信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileInfo.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = formatFileSize(fileInfo.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // 删除按钮
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "移除",
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 核心价值检查项
 */
@Composable
private fun ValueCheckItem(title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "✓ ",
            color = Color.Green,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp
        )
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF581C87))
            Text(desc, fontSize = 12.sp, color = Color(0xFF7C3AED).copy(alpha = 0.75f), lineHeight = 16.sp)
        }
    }
}

// ========== 数据类 ==========

data class SelectedFileInfo(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String
)

// ========== 工具函数 ==========

@Composable
private fun getFileTypeIcon(mimeType: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        mimeType.startsWith("image/") -> Icons.Default.Image
        mimeType.startsWith("video/") -> Icons.Default.VideoFile
        mimeType.startsWith("audio/") -> Icons.Default.AudioFile
        mimeType == "application/pdf" -> Icons.Default.PictureAsPdf
        else -> Icons.Default.Description
    }
}

@Composable
private fun getFileTypeColor(mimeType: String): Color {
    return when {
        mimeType.startsWith("image/") -> Color(0xFF3B82F6)
        mimeType.startsWith("video/") -> Color(0xFF8B5CF6)
        mimeType.startsWith("audio/") -> Color(0xFFF59E0B)
        else -> Color(0xFF6B7280)
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024 * 1024L -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024))
        else -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
    }
}
