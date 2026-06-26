package com.testimony.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
 * 风险评估屏幕
 * 展示AI分析后的风险等级和建议
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskAssessmentScreen(
    onViewScript: () -> Unit,
    onBack: () -> Unit
) {
    var riskLevel by remember { mutableStateOf("yellow") } // green, yellow, red
    var confidence by remember { mutableStateOf(78) }

    val riskColor = when (riskLevel) {
        "green" -> Success
        "yellow" -> Warning
        else -> Error
    }

    val riskLabel = when (riskLevel) {
        "green" -> "低风险"
        "yellow" -> "中等风险"
        else -> "高风险"
    }

    val riskDescription = when (riskLevel) {
        "green" -> "目前观察到的行为属于正常波动范围，但建议持续关注。"
        "yellow" -> "发现一些值得关注的行为模式，建议与孩子深入沟通。"
        else"需要重视。建议尽快与孩子沟通，必要时寻求专业帮助。"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("风险评估结果", color = Color.White) },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 风险等级显示
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(riskColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        when (riskLevel) {
                            "green" -> Icons.Default.SentimentSatisfied
                            "yellow" -> Icons.Default.SentimentNeutral
                            else -> Icons.Default.SentimentDissatisfied
                        },
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = riskColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        riskLabel,
                        fontWeight = FontWeight.Bold,
                        color = riskColor,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 置信度
            Text("分析置信度: $confidence%", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(24.dp))

            // 风险描述
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = riskColor.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Info, null, tint = riskColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(riskDescription, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 观察要点
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("观察要点", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ObservationPoint(
                        icon = Icons.Default.TrendingDown,
                        title = "行为变化",
                        description = "近期情绪波动较为明显"
                    )
                    ObservationPoint(
                        icon = Icons.Default.Group,
                        title = "社交情况",
                        description = "与同伴交流减少"
                    )
                    ObservationPoint(
                        icon = Icons.Default.School,
                        title = "学业表现",
                        description = "学习专注度有所下降"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 建议行动
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("建议行动", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ActionItem("找一个安静的时刻单独与孩子沟通")
                    ActionItem("避免直接质问，采用开放式问题")
                    ActionItem("表达理解和支持，不要急于给建议")
                    ActionItem("如果情况持续，考虑寻求心理咨询")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 沟通话术按钮
            Button(
                onClick = onViewScript,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Default.Chat, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("查看沟通话术", fontSize = MaterialTheme.typography.titleMedium.fontSize)
            }
        }
    }
}

@Composable
private fun ObservationPoint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = Secondary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Medium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ActionItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Check,
            null,
            tint = Success,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
