package com.testimony.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.testimony.ui.theme.*

// ========== 计算器伪装界面 ==========

@Composable
fun CalculatorScreen(
    onSecretGesture: () -> Unit,
    onSecretPattern: () -> Unit
) {
    var display by remember { mutableStateOf("0") }
    var firstNumber by remember { mutableStateOf<String?>(null) }
    var operation by remember { mutableStateOf<String?>(null) }
    var waitingForSecondNumber by remember { mutableStateOf(false) }
    
    // 秘密手势状态
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    val secretPattern = listOf(1, 3, 5, 7, 9, 8) // 右上右下左上下右
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CalculatorBackground)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastTapTime < 500) {
                            tapCount++
                            if (tapCount >= 6) {
                                onSecretGesture()
                                tapCount = 0
                            }
                        } else {
                            tapCount = 1
                        }
                        lastTapTime = currentTime
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 显示区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Text(
                    text = display,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Light,
                    color = CalculatorDisplay,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // 按钮区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val buttons = listOf(
                    listOf("AC", "±", "%", "÷"),
                    listOf("7", "8", "9", "×"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "+"),
                    listOf("0", ".", "=")
                )

                buttons.forEach { row ->
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { button ->
                            val isOperator = button in listOf("÷", "×", "-", "+", "=")
                            val isZero = button == "0"
                            val isSpecial = button in listOf("AC", "±", "%")

                            CalculatorButton(
                                text = button,
                                modifier = Modifier
                                    .weight(if (isZero) 2f else 1f)
                                    .fillMaxHeight(),
                                backgroundColor = when {
                                    isOperator -> CalculatorOperator
                                    isSpecial -> Color(0xFFE0E0E0)
                                    else -> CalculatorButton
                                },
                                textColor = if (isOperator || isSpecial) Color.Black else CalculatorDisplay,
                                onClick = { handleButtonClick(button, display, waitingForSecondNumber, firstNumber, operation) { 
                                    newDisplay, newFirst, newOp, newWait -> 
                                    display = newDisplay
                                    firstNumber = newFirst
                                    operation = newOp
                                    waitingForSecondNumber = newWait
                                }}
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun handleButtonClick(
    button: String,
    display: String,
    waitingForSecond: Boolean,
    firstNumber: String?,
    operation: String?,
    onResult: (String, String?, String?, Boolean) -> Unit
) {
    when (button) {
        "AC" -> onResult("0", null, null, false)
        "=" -> {
            firstNumber?.let { first ->
                operation?.let { op ->
                    val second = display.toDoubleOrNull() ?: 0.0
                    val result = when (op) {
                        "+" -> first.toDouble() + second
                        "-" -> first.toDouble() - second
                        "×" -> first.toDouble() * second
                        "÷" -> if (second != 0.0) first.toDouble() / second else 0.0
                        else -> second
                    }
                    val resultStr = if (result == result.toLong().toDouble()) {
                        result.toLong().toString()
                    } else {
                        "%.8f".format(result).trimEnd('0').trimEnd('.')
                    }
                    onResult(resultStr, null, null, false)
                }
            }
        }
        in listOf("+", "-", "×", "÷") -> {
            onResult(display, display, button, true)
        }
        "±" -> {
            val num = display.toDoubleOrNull() ?: 0.0
            onResult((-num).toString(), firstNumber, operation, waitingForSecond)
        }
        "%" -> {
            val num = display.toDoubleOrNull() ?: 0.0
            onResult((num / 100).toString(), firstNumber, operation, waitingForSecond)
        }
        else -> {
            val newDisplay = if (waitingForSecond) button else if (display == "0") button else display + button
            onResult(newDisplay, firstNumber, operation, false)
        }
    }
}

@Composable
fun CalculatorButton(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = CalculatorButton,
    textColor: Color = CalculatorDisplay,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

// ========== PIN 输入界面 ==========

@Composable
fun PinEntryScreen(
    onPinEntered: (String) -> Unit,
    onCancel: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val maxLength = 6

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "输入 PIN 退出伪装",
            style = MaterialTheme.typography.titleLarge
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // PIN 点指示器
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(maxLength) { index ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (index < pin.length) Primary else Color.LightGray)
                )
            }
        }

        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 数字键盘
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("取消", "0", "⌫")
            ).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { button ->
                        when (button) {
                            "取消" -> TextButton(onClick = onCancel) { Text("取消") }
                            "⌫" -> TextButton(
                                onClick = { 
                                    if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                    error = null
                                }
                            ) { Text("删除", fontSize = 18.sp) }
                            else -> {
                                FilledTonalButton(
                                    onClick = {
                                        if (pin.length < maxLength) {
                                            pin += button
                                            if (pin.length == maxLength) {
                                                // 模拟验证 (实际应调用 AppDisguiseManager)
                                                onPinEntered(pin)
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(72.dp),
                                    shape = CircleShape
                                ) {
                                    Text(button, fontSize = 24.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ========== 模式选择界面 ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectionScreen(
    onSelectStudent: () -> Unit,
    onSelectParent: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("证言") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "选择您的身份",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            // 学生模式卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSelectStudent),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, tint = Primary, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "我是学生",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "记录霸凌或网络暴力事件",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 家长模式卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSelectParent),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Secondary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FamilyRestroom, null, tint = Secondary, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "我是家长",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "记录孩子异常行为，获取沟通建议",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
