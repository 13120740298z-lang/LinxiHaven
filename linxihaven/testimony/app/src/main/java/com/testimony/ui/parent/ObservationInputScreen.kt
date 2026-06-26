package com.testimony.ui.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.testimony.ui.theme.*

/**
 * 观察记录输入屏幕
 * 家长记录孩子的异常行为
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObservationInputScreen(
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    var behaviorType by remember { mutableStateOf("") }
    var occurrenceTime by remember { mutableStateOf("") }
    var occurrencePlace by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("偶尔") }

    val behaviorOptions = listOf("情绪异常", "社交退缩", "学习下降", "身体伤痕", "网络异常", "其他")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记录观察", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Secondary),
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // 提示
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Info.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Info, null, tint = Info)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "请记录您观察到的孩子异常行为。这些信息将帮助您更好地与孩子沟通。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 行为类型
            Text("异常行为类型", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                behaviorOptions.forEach { option ->
                    FilterChip(
                        selected = behaviorType == option,
                        onClick = { behaviorType = if (behaviorType == option) "" else option },
                        label = { Text(option) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 发生时间
            OutlinedTextField(
                value = occurrenceTime,
                onValueChange = { occurrenceTime = it },
                label = { Text("发生时间") },
                placeholder = { Text("例如：上周三下午") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Schedule, null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 发生地点
            OutlinedTextField(
                value = occurrencePlace,
                onValueChange = { occurrencePlace = it },
                label = { Text("发生地点") },
                placeholder = { Text("例如：学校、家里、网络") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Place, null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 发生频率
            Text("发生频率", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("偶尔", "有时", "经常").forEach { freq ->
                    FilterChip(
                        selected = frequency == freq,
                        onClick = { frequency = freq },
                        label = { Text(freq) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 详细描述
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("详细描述") },
                placeholder = { Text("请描述您观察到的具体情况...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 6
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 提交按钮
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = behaviorType.isNotEmpty() && description.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Default.Send, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("提交分析", fontSize = MaterialTheme.typography.titleMedium.fontSize)
            }
        }
    }
}
