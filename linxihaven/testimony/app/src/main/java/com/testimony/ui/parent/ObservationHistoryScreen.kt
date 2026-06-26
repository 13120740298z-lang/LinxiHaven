package com.testimony.ui.parent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
 * 观察历史屏幕
 * 展示家长记录的所有观察
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObservationHistoryScreen(
    onBack: () -> Unit
) {
    // 模拟历史数据
    val observations = remember {
        listOf(
            Observation(
                id = "1",
                date = "2024-05-14",
                type = "情绪异常",
                summary = "孩子回家后情绪低落，不愿说话",
                riskLevel = "yellow"
            ),
            Observation(
                id = "2",
                date = "2024-05-10",
                type = "社交退缩",
                summary = "拒绝参加朋友的生日聚会",
                riskLevel = "yellow"
            ),
            Observation(
                id = "3",
                date = "2024-05-05",
                type = "学习下降",
                summary = "近期作业完成质量下降",
                riskLevel = "green"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("观察历史", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Secondary),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        if (observations.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "暂无观察记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "点击\"记录观察\"开始记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 统计卡片
                item {
                    StatisticsCard(observations)
                }

                // 观察列表
                items(observations) { observation ->
                    ObservationCard(observation)
                }
            }
        }
    }
}

data class Observation(
    val id: String,
    val date: String,
    val type: String,
    val summary: String,
    val riskLevel: String
)

@Composable
private fun StatisticsCard(observations: List<Observation>) {
    val greenCount = observations.count { it.riskLevel == "green" }
    val yellowCount = observations.count { it.riskLevel == "yellow" }
    val redCount = observations.count { it.riskLevel == "red" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("观察统计", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("低风险", greenCount.toString(), Success)
                StatItem("中风险", yellowCount.toString(), Warning)
                StatItem("高风险", redCount.toString(), Error)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, count: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            count,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ObservationCard(observation: Observation) {
    val riskColor = when (observation.riskLevel) {
        "green" -> Success
        "yellow" -> Warning
        else -> Error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 风险指示
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .padding(top = 6.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = riskColor
                ) {}
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        observation.type,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        observation.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    observation.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}
