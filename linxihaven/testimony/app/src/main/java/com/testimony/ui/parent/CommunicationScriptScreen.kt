package com.testimony.ui.parent

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.testimony.ui.theme.*

/**
 * 沟通话术屏幕
 * 提供与孩子沟通的建议话术
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunicationScriptScreen(
    onComplete: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("开场白", "深入了解", "表达支持", "行动建议")

    val scripts = mapOf(
        0 to listOf(
            ScriptItem(
                title = "自然开场",
                script = "\"最近看你在家好像有点心事，可以和我说说吗？\"",
                tip = "选择一个孩子放松的时刻，不要让孩子感到被审问"
            ),
            ScriptItem(
                title = "观察切入",
                script = "\"我注意到你最近好像不太开心，是发生了什么事吗？\"",
                tip = "用观察到的事实开场，避免主观判断"
            )
        ),
        1 to listOf(
            ScriptItem(
                title = "开放式提问",
                script = "\"能和我说说具体是什么事情让你感到困扰吗？\"",
                tip = "使用\"什么\"、\"怎么\"、\"为什么\"等问题"
            ),
            ScriptItem(
                title = "共情回应",
                script = "\"听起来这件事让你很不舒服，我理解你的感受。\"",
                tip = "先接纳情绪，再讨论事实"
            )
        ),
        2 to listOf(
            ScriptItem(
                title = "表达理解",
                script = "\"无论发生什么，我们都是一家人，会一起面对。\"",
                tip = "让孩子知道家是安全的后盾"
            ),
            ScriptItem(
                title = "强调非责怪",
                script = "\"这不是你的错，你没有做错任何事。\"",
                tip = "如果涉及霸凌，必须明确这一点"
            )
        ),
        3 to listOf(
            ScriptItem(
                title = "询问需求",
                script = "\"你觉得需要我们怎么帮助你？你希望我做什么？\"",
                tip = "尊重孩子的意愿，不要擅自做主"
            ),
            ScriptItem(
                title = "提出建议",
                script = "\"如果下次再遇到这种情况，我们可以...你觉得怎么样？\"",
                tip = "以建议而非命令的方式提出方案"
            )
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("沟通话术建议", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Secondary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab选择
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Surface,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // 话术内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                scripts[selectedTab]?.forEach { scriptItem ->
                    ScriptCard(scriptItem)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 注意事项
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Warning)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("沟通注意事项", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "• 保持耐心，不要急于求成\n" +
                                "• 控制情绪，避免指责\n" +
                                "• 尊重孩子的隐私\n" +
                                "• 相信孩子说的话\n" +
                                "• 必要时寻求专业帮助",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 完成按钮
                OutlinedButton(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("我知道了")
                }
            }
        }
    }
}

data class ScriptItem(
    val title: String,
    val script: String,
    val tip: String
)

@Composable
private fun ScriptCard(scriptItem: ScriptItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(scriptItem.title, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(12.dp))
            
            // 话术内容
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Secondary.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text("\"", style = MaterialTheme.typography.headlineSmall, color = Secondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(scriptItem.script, style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 提示
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Lightbulb,
                    null,
                    tint = Warning,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    scriptItem.tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
