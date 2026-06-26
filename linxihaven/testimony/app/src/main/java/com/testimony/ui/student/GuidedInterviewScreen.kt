package com.testimony.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.testimony.ai.GuidedInterviewFSM
import com.testimony.data.models.ChatMessage
import com.testimony.data.models.InterviewState
import com.testimony.data.models.RiskLevel
import com.testimony.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidedInterviewScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val fsm = remember { GuidedInterviewFSM() }
    var session by remember { mutableStateOf(fsm.startSession()) }
    var userInput by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var showSafetyAlert by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Send initial greeting
    LaunchedEffect(Unit) {
        val result = fsm.processUserInput("")
        messages = messages + ChatMessage(
            content = "你好，感谢你打开证言。\n\n我是来帮助你的引导员。请放心，这里你说什么都是安全的。\n\n在我们开始之前，我想确认一下：你现在在一个可以安全说话的地方吗？比如自己的房间，周围没有其他人在看你的屏幕？",
            isFromAI = true,
            state = InterviewState.CONFIRM_SAFETY
        )
    }

    // Scroll to bottom when new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("事件回顾") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "退出")
                    }
                },
                actions = {
                    // Progress indicator
                    Text(
                        text = "${(fsm.getElementCoverage() * 100).toInt()}%",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = { fsm.getElementCoverage() },
                modifier = Modifier.fillMaxWidth(),
                color = Primary
            )

            // Risk indicator
            if (showSafetyAlert) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = RiskYellow.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = RiskYellow
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "如果你感到不舒服，我们可以暂停或结束",
                            color = RiskYellow,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Chat messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message = message)
                }
            }

            // Input area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("请输入...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    FilledIconButton(
                        onClick = {
                            if (userInput.isNotBlank()) {
                                // Add user message
                                messages = messages + ChatMessage(
                                    content = userInput,
                                    isFromAI = false,
                                    state = session.currentState
                                )

                                // Process with FSM
                                val result = fsm.processUserInput(userInput)
                                session = fsm.getSession()

                                // Check for safety alert
                                if (result.isSafetyAlert) {
                                    showSafetyAlert = true
                                }

                                // Add AI response
                                val aiContent = when (result.nextState) {
                                    InterviewState.FREE_NARRATION -> "很好，谢谢你确认。\n\n现在，请告诉我发生了什么。你可以按照自己的节奏说，我会认真听。\n\n不用着急，想到什么就说什么。如果过程中有任何不舒服，随时告诉我，我们可以暂停。"
                                    InterviewState.FACT_ELEMENTS_INQUIRY -> "谢谢你告诉我这些。我想更清楚地了解一些细节：\n\n${result.missingElements.joinToString("\n") { "• ${it.displayName}: ${it.description}" }}"
                                    InterviewState.SUPPLEMENTARY_STATEMENT -> "在我们结束之前，还有什么想补充的吗？比如你觉得重要但还没说到的，或者有什么特别想强调的？"
                                    InterviewState.COMPLETED -> "感谢你信任证言，也感谢你勇敢地说出这些。\n\n你的记录已经安全保存了。你现在可以：\n• 把它安全地存储在这里\n• 选择分享给信任的成年人\n• 或者先放着，之后再决定\n\n记住，这不是你的错。你做得很好。"
                                    else -> "我听到了。继续说吧，或者如果你已经说完了，告诉我\"我说完了\"。"
                                }

                                messages = messages + ChatMessage(
                                    content = aiContent,
                                    isFromAI = true,
                                    state = result.nextState,
                                    isSafetyAlert = result.isSafetyAlert
                                )

                                // Clear input
                                userInput = ""

                                // If completed, show completion
                                if (result.nextState == InterviewState.COMPLETED) {
                                    kotlinx.coroutines.delay(2000)
                                    onComplete()
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Primary
                        )
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "发送")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isAI = message.isFromAI

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAI) Arrangement.Start else Arrangement.End
    ) {
        if (isAI) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isAI) 4.dp else 16.dp,
                bottomEnd = if (isAI) 16.dp else 4.dp
            ),
            color = if (isAI) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                Primary
            }
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (message.isSafetyAlert) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = RiskYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "安全提示",
                            style = MaterialTheme.typography.labelSmall,
                            color = RiskYellow
                        )
                    }
                }

                Text(
                    text = message.content,
                    color = if (isAI) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                )
            }
        }

        if (!isAI) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidencePreviewScreen(
    onSaveLocally: () -> Unit,
    onShareToSchool: () -> Unit,
    onShareToParent: () -> Unit
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("证据预览") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
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
            Icon(
                Icons.Default.Verified,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "证据包已生成",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "包含：录屏、对话记录、时间戳、位置信息",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Options
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedOption == "local") {
                        Primary.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(
                            end = if (selectedOption == "local") 0.dp else 0.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == "local",
                        onClick = { selectedOption = "local" }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "本地加密保存",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "只有你能查看",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedOption == "school") {
                        Primary.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == "school",
                        onClick = { selectedOption = "school" }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = Secondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "提交给学校",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "匿名提交，保护你的隐私",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedOption == "parent") {
                        Primary.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == "parent",
                        onClick = { selectedOption = "parent" }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Default.FamilyRestroom,
                        contentDescription = null,
                        tint = RiskGreen
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "分享给家长",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "安全披露，让他们了解情况",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    when (selectedOption) {
                        "local" -> onSaveLocally()
                        "school" -> onShareToSchool()
                        "parent" -> onShareToParent()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = selectedOption != null,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("确认")
            }
        }
    }
}
