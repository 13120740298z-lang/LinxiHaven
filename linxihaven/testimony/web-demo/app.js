/**
 * 证言 Testimony.ai - Web Demo 应用核心
 * AI深伪时代青少年霸凌事件可信存证系统
 */

// ========== 全局状态管理 ==========
const AppState = {
    currentScreen: 'calculator',
    pinCode: '',
    correctPin: '123456',
    tapCount: 0,
    lastTapTime: 0,
    
    // API配置
    apiConfig: {
        apiKey: 'sk-f6438ea6f506a07b009e4a950aae88be2af3256605ee475ee0f78258e5e586aa',
        endpoint: 'https://api.qnaigc.com/v1',
        model: 'deepseek-v3'
    },
    
    // 安全空间
    securitySpaceActive: false,
    securityStartTime: null,
    securityTimerInterval: null,
    timestampInterval: null,
    
    // 访谈状态
    interviewState: 'STABILIZING',  // STABILIZING -> COLLECTING -> EMPOWERING -> COMPLETED
    interviewMessages: [],
    collectedElements: new Set(),
    emotionalRiskCount: 0,
    fsmTransitions: [],
    
    // 统计
    sessionCount: 0,
    packageCount: 0,

    // 计算器状态
    calcDisplay: '0',
    calcFirstOperand: null,
    calcOperator: null,
    calcWaitingForSecond: false
};

// ========== 初始化 ==========
document.addEventListener('DOMContentLoaded', () => {
    console.log('🛡️ 证言 Testimony.ai Web Demo 启动中...');
    
    // 加载保存的配置
    loadApiConfig();
    
    // 模拟加载动画
    setTimeout(() => {
        document.getElementById('loading-screen').classList.remove('active');
        setTimeout(() => {
            document.getElementById('loading-screen').classList.add('hidden');
            document.getElementById('app').classList.remove('hidden');
            initCalculator();
            updateClock();
            setInterval(updateClock, 1000);
        }, 500);
    }, 1800);

    initEventListeners();
});

// ========== 导航系统 ==========
function navigateTo(screenName) {
    // 隐藏所有屏幕
    document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
    
    // 显示目标屏幕
    const target = document.getElementById(`screen-${screenName}`);
    if (target) {
        target.classList.add('active');
        AppState.currentScreen = screenName;
        
        // 屏幕特定初始化
        if (screenName === 'interview') {
            initInterview();
        }
        if (screenName === 'security-space') {
            startSecuritySpaceSimulation();
        }
        if (screenName === 'evidence-preview') {
            drawMerkleTree();
        }
        if (screenName === 'readiness-report') {
            animateReportScores();
        }
    }
}

// ========== 计算器功能 ==========
function initCalculator() {
    const buttons = document.querySelectorAll('.calc-buttons .btn');
    buttons.forEach(btn => {
        btn.addEventListener('click', (e) => handleCalcButton(e.target));
    });
}

function updateClock() {
    const now = new Date();
    const timeStr = now.toLocaleTimeString('zh-CN', { hour12: false });
    const calcTimeEl = document.getElementById('calc-time');
    if (calcTimeEl) {
        calcTimeEl.textContent = timeStr;
    }
}

function handleCalcButton(btn) {
    const action = btn.dataset.action;
    const value = btn.dataset.value;

    switch(action) {
        case 'number':
            if (AppState.calcWaitingForSecond) {
                AppState.calcDisplay = value;
                AppState.calcWaitingForSecond = false;
            } else {
                AppState.calcDisplay = AppState.calcDisplay === '0' ? value : AppState.calcDisplay + value;
            }
            break;
        case 'decimal':
            if (!AppState.calcDisplay.includes('.')) {
                AppState.calcDisplay += '.';
            }
            break;
        case 'operator':
            AppState.calcFirstOperand = parseFloat(AppState.calcDisplay);
            AppState.calcOperator = value;
            AppState.calcWaitingForSecond = true;
            break;
        case 'equals':
            if (AppState.calcOperator && AppState.calcFirstOperand !== null) {
                const second = parseFloat(AppState.calcDisplay);
                const ops = {
                    '+': (a, b) => a + b,
                    '-': (a, b) => a - b,
                    '*': (a, b) => a * b,
                    '/': (a, b) => b !== 0 ? a / b : 0
                };
                const result = ops[AppState.calcOperator](AppState.calcFirstOperand, second);
                AppState.calcDisplay = Number.isInteger(result) ? result.toString() : result.toFixed(8).replace(/\.?0+$/, '');
                AppState.calcFirstOperand = null;
                AppState.calcOperator = null;
                AppState.calcWaitingForSecond = false;
            }
            break;
        case 'clear':
            AppState.calcDisplay = '0';
            AppState.calcFirstOperand = null;
            AppState.calcOperator = null;
            AppState.calcWaitingForSecond = false;
            break;
        case 'negate':
            AppState.calcDisplay = (-parseFloat(AppState.calcDisplay)).toString();
            break;
        case 'percent':
            AppState.calcDisplay = (parseFloat(AppState.calcDisplay) / 100).toString();
            break;
    }

    updateCalcDisplay();
}

function updateCalcDisplay() {
    const display = document.getElementById('calc-display');
    if (display) display.textContent = AppState.calcDisplay;
}

// 秘密手势检测（点击左上角区域）
document.addEventListener('DOMContentLoaded', () => {
    const calcScreen = document.getElementById('screen-calculator');
    if (calcScreen) {
        calcScreen.addEventListener('click', (e) => {
            const currentTime = Date.now();
            if (currentTime - AppState.lastTapTime < 500) {
                AppState.tapCount++;
                if (AppState.tapCount >= 6) {
                    navigateTo('pin');
                    AppState.tapCount = 0;
                    showToast('🔓 已触发秘密手势', 'success');
                }
            } else {
                AppState.tapCount = 1;
            }
            AppState.lastTapTime = currentTime;
        });
    }
});

// ========== PIN验证 ==========
function initEventListeners() {
    // PIN键盘
    document.querySelectorAll('.keypad-btn[data-key]').forEach(btn => {
        btn.addEventListener('click', () => {
            if (AppState.pinCode.length < 6) {
                AppState.pinCode += btn.dataset.key;
                updatePinDots();
                
                if (AppState.pinCode.length === 6) {
                    verifyPin();
                }
            }
        });
    });

    document.querySelector('[data-action="delete"]')?.addEventListener('click', () => {
        AppState.pinCode = AppState.pinCode.slice(0, -1);
        updatePinDots();
    });

    document.querySelector('[data-action="cancel"]')?.addEventListener('click', () => {
        AppState.pinCode = '';
        updatePinDots();
        navigateTo('calculator');
    });
}

function updatePinDots() {
    const dots = document.querySelectorAll('#pin-dots .dot');
    dots.forEach((dot, i) => {
        dot.classList.toggle('filled', i < AppState.pinCode.length);
    });
    
    const errorEl = document.getElementById('pin-error');
    if (errorEl) errorEl.classList.add('hidden');
}

function verifyPin() {
    if (AppState.pinCode === AppState.correctPin) {
        showToast('✅ 验证成功，正在进入系统...', 'success');
        setTimeout(() => {
            navigateTo('mode-selection');
            AppState.pinCode = '';
            updatePinDots();
        }, 800);
    } else {
        const errorEl = document.getElementById('pin-error');
        if (errorEl) {
            errorEl.classList.remove('hidden');
            errorEl.textContent = `PIN码错误 (${AppState.pinCode})`;
        }
        AppState.pinCode = '';
        setTimeout(updatePinDots, 1500);
    }
}

// ========== 模式选择 ==========
function selectMode(mode) {
    if (mode === 'student') {
        navigateTo('student-home');
        showToast('🎓 进入学生工作台', 'success');
    } else if (mode === 'parent') {
        navigateTo('parent-home');
        showToast('👨‍👩‍👧 进入家长工作台', 'success');
    }
}

// ========== 安全空间模拟 ==========
let securitySeconds = 0;

function startSecuritySpace() {
    navigateTo('security-space');
    AppState.securitySpaceActive = true;
    AppState.securityStartTime = Date.now();
    AppState.sessionCount++;
    updateStatDisplay();
    
    // 开始计时器
    securitySeconds = 0;
    AppState.securityTimerInterval = setInterval(() => {
        securitySeconds++;
        const h = String(Math.floor(securitySeconds / 3600)).padStart(2, '0');
        const m = String(Math.floor((securitySeconds % 3600) / 60)).padStart(2, '0');
        const s = String(securitySeconds % 60).padStart(2, '0');
        const timerEl = document.getElementById('security-timer');
        if (timerEl) timerEl.textContent = `${h}:${m}:${s}`;
    }, 1000);
    
    // 模拟时间戳更新
    startTimestampSimulation();
    
    showToast('🔒 安全空间已启动 - 录屏/时间锚定/通知静音已激活', 'success');
}

function startTimestampSimulation() {
    AppState.timestampInterval = setInterval(() => {
        const now = new Date();
        const timeStr = now.toLocaleTimeString('zh-CN', { hour12: false });
        
        setElementText('ts-system', timeStr);
        setElementText('ts-ntp', timeStr);  // 模拟NTP同步
        setElementText('ts-cell', timeStr);  // 模拟基站时间
        setElementText('ts-blockchain', timeStr);  // 模拟区块链时间
        
        // 随机调整置信度显示
        const confidences = ['HIGH', 'MEDIUM'];
        const randomConf = confidences[Math.floor(Math.random() * confidences.length)];
        setElementText('confidence-level', randomConf);
    }, 1000);
}

function exitSecuritySpace() {
    clearInterval(AppState.securityTimerInterval);
    clearInterval(AppState.timestampInterval);
    AppState.securitySpaceActive = false;
    
    // 更新证据包清单
    updateManifestItems();
    
    // 生成证据包ID
    const pkgId = `EP_${generateUUID()}`;
    setElementText('pkg-id', pkgId);
    setElementText('pkg-time', new Date().toLocaleString('zh-CN'));
    setElementHtml('pkg-status', '<span class="badge badge-success">已生成</span>');
    
    AppState.packageCount++;
    updateStatDisplay();
    
    showToast('📦 证据包已生成并加密存储', 'success');
    
    setTimeout(() => {
        navigateTo('evidence-preview');
    }, 1000);
}

function updateManifestItems() {
    const items = ['mf-recording', 'mf-operation', 'mf-sensor', 'mf-timestamp', 'mf-fsm', 'mf-merkle'];
    items.forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            const statusSpan = el.querySelector('.status');
            if (statusSpan) {
                statusSpan.className = 'status ready';
                statusSpan.textContent = '✓ 就绪';
            }
        }
    });
}

// ========== 引导式访谈引擎 ==========
const INTERVIEW_STATES = {
    STABILIZING: { name: '稳定化', step: 1, progress: 20 },
    COLLECTING: { name: '信息收集', step: 2, progress: 45 },
    INQUIRY: { name: '要素追问', step: 3, progress: 65 },
    EMPOWERING: { name: '赋能阶段', step: 4, progress: 85 },
    COMPLETED: { name: '完成', step: 5, progress: 100 }
};

const ELEMENTS = ['time', 'location', 'persons', 'behavior', 'evidence', 'consequence'];

function startGuidedInterview() {
    navigateTo('interview');
    AppState.interviewState = 'STABILIZING';
    AppState.interviewMessages = [];
    AppState.collectedElements.clear();
    AppState.emotionalRiskCount = 0;
    AppState.fsmTransitions = [];
    initInterview();
}

function initInterview() {
    // 清空聊天区
    const chatArea = document.getElementById('chat-area');
    if (chatArea) chatArea.innerHTML = '';
    
    // 发送初始引导语
    addAIMessage(
        '你好，感谢你愿意分享。我是证言的AI引导助手。\n\n在开始之前，我想先确认一件事：**你现在身边有其他人吗？你现在是安全的吗？**',
        'STABILIZING'
    );
    
    // 重置进度条
    updateInterviewProgress(INTERVIEW_STATES.STABILIZING);
    resetElementTags();
}

async function sendUserMessage() {
    const input = document.getElementById('user-input');
    const text = input?.value.trim();
    if (!text) return;
    
    // 添加用户消息
    addUserMessage(text);
    input.value = '';
    
    // 显示输入指示器
    showTypingIndicator();
    
    // 情绪风险检测
    detectEmotionalRisk(text);
    
    // 要素提取
    extractElements(text);
    
    // 状态转换
    processStateTransition(text);
    
    // 调用AI生成回复
    try {
        const response = await callAIForGuidance(text);
        hideTypingIndicator();
        addAIMessage(response.text, AppState.interviewState);
        
        // 记录状态转换
        recordFSMTransition(AppState.interviewState, response.stateChange || false);
        
        // 如果AI建议状态变更
        if (response.nextState && INTERVIEW_STATES[response.nextState]) {
            setTimeout(() => {
                AppState.interviewState = response.nextState;
                updateInterviewProgress(INTERVIEW_STATES[response.nextState]);
                
                // 如果进入新状态，发送新的引导
                addAIMessage(getStatePrompt(response.nextState), response.nextState);
            }, 1500);
        }
    } catch (error) {
        hideTypingIndicator();
        console.error('AI调用失败:', error);
        // 使用降级模板
        addAIMessage(getFallbackResponse(AppState.interviewState), AppState.interviewState);
    }
}

async function callAIForGuidance(userInput) {
    const config = AppState.apiConfig;
    if (!config.apiKey || config.apiKey.startsWith('请')) {
        return { 
            text: getFallbackResponse(AppState.interviewState), 
            nextState: getNextState(AppState.interviewState)
        };
    }
    
    try {
        const systemPrompt = buildSystemPrompt(userInput);
        const messages = [
            { role: 'system', content: systemPrompt },
            ...AppState.interviewMessages.slice(-6).flatMap(m => [
                { role: m.type === 'user' ? 'user' : 'assistant', content: m.content }
            ]),
            { role: 'user', content: userInput }
        ];
        
        const response = await fetch(`${config.endpoint}/chat/completions`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${config.apiKey}`
            },
            body: JSON.stringify({
                model: config.model,
                messages: messages,
                max_tokens: 300,
                temperature: 0.7
            })
        });
        
        if (!response.ok) throw new Error(`API error: ${response.status}`);
        
        const data = await response.json();
        const aiText = data.choices[0]?.message?.content || '';
        
        // 诱导性检测
        const detectionResult = checkInducement(aiText);
        
        return {
            text: detectionResult.isClean ? aiText : detectionResult.fallbackPhrase,
            nextState: determineNextState(userInput),
            stateChange: true
        };
    } catch (error) {
        console.error('AI API Error:', error);
        return { 
            text: getFallbackResponse(AppState.interviewState), 
            nextState: getNextState(AppState.interviewState)
        };
    }
}

function buildSystemPrompt(userInput) {
    const collectedList = [...AppState.collectedElements].join(', ') || '尚未收集';
    
    return `你是"证言助手"，一个受法律协议约束的AI引导记录员。

【绝对不可逾越的边界】
✅ 你只做引导和记录，不做心理诊断或事实判断
✅ 你表达共情，但不替代用户做出判断
❌ 禁止预设事实（如"他打你的时候"）
❌ 禁止暗示用户应该有某种感受
❌ 禁止心理诊断或性格标签
❌ 禁止封闭式诱导（"是不是"句式）

【当前状态】${INTERVIEW_STATES[AppState.interviewState]?.name}
【已收集要素】${collectedList}
【情绪风险等级】${getRiskLevel()}

请生成一段温和、不诱导的引导语（不超过150字）。`;
}

// 诱导性检测规则引擎（简化版15条规则）
function checkInducement(text) {
    if (!text || text.trim().length === 0) {
        return { isClean: true, fallbackPhrase: '', violations: [] };
    }
    
    const violations = [];
    const rules = [
        { pattern: /是谁.*?(打|骂|欺负|霸凌)/i, id: 1, desc: '预设施暴者身份' },
        { pattern: /(打.*你了|骂.*你了|欺负.*你了)/i, id: 2, desc: '预设行为' },
        { pattern: /是不是.*(他|她)/i, id: 3, desc: '预设事实确认' },
        { pattern: /应该.*觉得/i, id: 4, desc: '评判性引导' },
        { pattern: /为什么.*不/i, id: 5, desc: '责备句式' },
        { pattern: /(肯定|一定|毫无疑问)/i, id: 6, desc: '确定性词汇' },
        { pattern: /(恨.*吗|报复.*吗)/i, id: 7, desc: '封闭式诱导' },
        { pattern: /(心理.*问题|抑郁|焦虑)/i, id: 8, desc: '心理诊断' },
        { pattern: /(几次|多少次)/i, id: 9, desc: '数量假设' },
        { pattern: /(严不严重|有多严重)/i, id: 10, desc: '严重程度预设' }
    ];
    
    rules.forEach(rule => {
        if (rule.pattern.test(text)) {
            violations.push({ ruleId: rule.id, description: rule.desc });
        }
    });
    
    if (violations.length > 0) {
        return {
            isClean: false,
            fallbackPhrase: getWhitelistFallback(),
            violations: violations
        };
    }
    
    return { isClean: true, fallbackPhrase: '', violations: [] };
}

function getWhitelistFallback() {
    const phrases = [
        "你能继续告诉我你想说的内容吗？我在这里听着。",
        "不用着急，想到什么就说什么。我会在这里，静静地听着。",
        "感谢你的信任。你可以按照自己的节奏来说。"
    ];
    return phrases[Math.floor(Math.random() * phrases.length)];
}

function determineNextState(input) {
    const currentState = AppState.interviewState;
    const missingElements = ELEMENTS.filter(e => !AppState.collectedElements.has(e));
    
    // 简化版状态机
    switch (currentState) {
        case 'STABILIZING':
            return input.includes('安全') || input.includes('是的') ? 'COLLECTING' : 'STABILIZING';
        case 'COLLECTING':
            if (input.match(/说完了|没有了|完了|finished/i)) {
                return missingElements.length > 0 ? 'INQUIRY' : 'EMPOWERING';
            }
            return 'COLLECTING';
        case 'INQUIRY':
            return missingElements.length === 0 ? 'EMPOWERING' : 'INQUIRY';
        case 'EMPOWERING':
            return 'COMPLETED';
        default:
            return currentState;
    }
}

function getNextState(currentState) {
    return determineNextState('');
}

// 情绪风险关键词检测
const HIGH_RISK_KEYWORDS = [
    '想死', '不想活了', '自杀', '自残', '伤害自己', '结束生命',
    '没有希望', '活着没意思'
];

function detectEmotionalRisk(text) {
    let detected = false;
    HIGH_RISK_KEYWORDS.forEach(keyword => {
        if (text.includes(keyword)) {
            detected = true;
            AppState.emotionalRiskCount++;
            showEmotionTag('⚠️ 高风险情绪检测');
        }
    });
    return detected;
}

// 六要素提取
function extractElements(text) {
    const elementPatterns = {
        time: [/昨天|今天|上周|周一|晚上|下午|早上|中午/i],
        location: [/学校|教室|操场|食堂|厕所|宿舍|家里|网上|微信/i],
        persons: [/他们|她们|他|她|有人|同学|老师/i],
        behavior: [/骂|打|踢|推|嘲笑|孤立|威胁|侮辱|欺凌/i],
        evidence: [/截图|照片|录像|聊天记录|证据/i],
        consequence: [/难受|害怕|哭|睡不着|不想上学|成绩下降/i]
    };

    Object.entries(elementPatterns).forEach(([element, patterns]) => {
        if (!AppState.collectedElements.has(element)) {
            patterns.some(p => p.test(text));
            patterns.forEach(pattern => {
                if (pattern.test(text)) {
                    AppState.collectedElements.add(element);
                    updateElementTag(element);
                }
            });
        }
    });
}

function getRiskLevel() {
    if (AppState.emotionalRiskCount >= 3) return 'RED';
    if (AppState.emotionalRiskCount >= 1) return 'YELLOW';
    return 'GREEN';
}

function getStatePrompt(state) {
    const prompts = {
        STABILIZING: '我明白了。接下来，你可以用自己的话告诉我发生了什么。不用着急，想到什么就说什么。',
        COLLECTING: '我在认真倾听。你可以继续说，我会一直在这里。',
        INQUIRY: `为了更完整地记录这件事，我还需要了解一些信息：\n\n${getMissingElementsPrompt()}`,
        EMPOWERING: '感谢你愿意和我分享这些。你的记录已经安全地保存在设备上，只有你才能决定谁来查看它。',
        COMPLETED: '今天的记录已经完成了。你已经非常勇敢。记住，这件事不是你的错。'
    };
    return prompts[state] || '请继续...';
}

function getMissingElementsPrompt() {
    const missing = ELEMENTS.filter(e => !AppState.collectedElements.has(e));
    const names = {
        time: '发生的时间',
        location: '发生的地点',
        persons: '涉及的人',
        behavior: '具体的行为',
        evidence: '是否有截图等证据',
       后果: '对你的影响'
    };
    return missing.map(e => `- ${names[e] || e}`).join('\n');
}

function getFallbackResponse(state) {
    const responses = {
        STABILIZING: '在开始之前，我想先确认一件事：你现在身边有其他人吗？你现在是安全的吗？\n\n（选项：是的，我现在还算安全 | 环境不安全，我需要帮助）',
        COLLECTING: '我明白了。接下来，你可以用自己的话告诉我发生了什么。\n\n不用着急，想到什么就说什么。我会在这里，静静地听着。',
        INQUIRY: `关于这件事，我还想了解更多：\n\n${getMissingElementsPrompt()}`,
        EMPOWERING: '感谢你愿意分享这些。你想怎么处理这份记录？决定权完全在你手中。',
        COMPLETED: '感谢你的勇敢。你的记录已经完整保存，只有你自己能决定是否分享给他人。\n\n记住：**这件事不是你的错**。'
    };
    return responses[state] || responses.COLLECTING;
}

function addAIMessage(text, state) {
    const chatArea = document.getElementById('chat-area');
    if (!chatArea) return;
    
    const messageDiv = document.createElement('div');
    messageDiv.className = 'message ai-message';
    messageDiv.innerHTML = `
        <div class="message-header">
            <i class="fas fa-robot"></i> 证言AI助手 · ${INTERVIEW_STATES[state]?.name || state}
        </div>
        <div class="message-body">${formatMessage(text)}</div>
    `;
    chatArea.appendChild(messageDiv);
    
    AppState.interviewMessages.push({ type: 'ai', content: text, state });
    scrollToBottom(chatArea);
}

function addUserMessage(text) {
    const chatArea = document.getElementById('chat-area');
    if (!chatArea) return;
    
    const messageDiv = document.createElement('div');
    messageDiv.className = 'message user-message';
    messageDiv.innerHTML = `
        <div class="message-header">
            你 <span style="margin-left:auto;">刚刚</span>
        </div>
        <div class="message-body">${escapeHtml(text)}</div>
    `;
    chatArea.appendChild(messageDiv);
    
    AppState.interviewMessages.push({ type: 'user', content: text });
    scrollToBottom(chatArea);
}

function formatMessage(text) {
    return text
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\n/g, '<br>');
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showTypingIndicator() {
    const chatArea = document.getElementById('chat-area');
    if (!chatArea) return;
    
    const typing = document.createElement('div');
    typing.id = 'typing-indicator';
    typing.className = 'message ai-message';
    typing.innerHTML = `
        <div class="message-body" style="display:flex;align-items:center;gap:6px;">
            <span class="typing-dot" style="animation:dot-bounce 1.4s infinite;"></span>
            <span class="typing-dot" style="animation:dot-bounce 1.4s 0.2s infinite;"></span>
            <span class="typing-dot" style="animation:dot-bounce 1.4s 0.4s infinite;"></span>
        </div>
    `;
    chatArea.appendChild(typing);
    scrollToBottom(chatArea);
    
    // 添加CSS动画
    if (!document.querySelector('#typing-style')) {
        const style = document.createElement('style');
        style.id = 'typing-style';
        style.textContent = `
            @keyframes dot-bounce {
                0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
                40% { transform: scale(1); opacity: 1; }
            }
            .typing-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--primary); display: inline-block; }
        `;
        document.head.appendChild(style);
    }
}

function hideTypingIndicator() {
    const indicator = document.getElementById('typing-indicator');
    if (indicator) indicator.remove();
}

function showEmotionTag(text) {
    const container = document.getElementById('emotion-indicators');
    if (container) {
        const tag = document.createElement('span');
        tag.className = 'emotion-tag';
        tag.textContent = text;
        container.appendChild(tag);
        setTimeout(() => tag.remove(), 5000);
    }
}

function updateInterviewProgress(stateInfo) {
    const fill = document.getElementById('interview-progress');
    const text = document.getElementById('interview-step-text');
    if (fill) fill.style.width = `${stateInfo.progress}%`;
    if (text) text.textContent = `${stateInfo.step}/5 ${stateInfo.name}`;
}

function updateElementTag(element) {
    const tags = document.querySelectorAll(`#element-tags .el-tag[data-element="${element}"]`);
    tags.forEach(tag => {
        tag.classList.remove('pending');
        tag.classList.add('collected');
    });
    
    // 更新统计
    const statEl = document.getElementById('stat-elements');
    if (statEl) {
        statEl.textContent = `${AppState.collectedElements.size}/6`;
    }
}

function resetElementTags() {
    const tags = document.querySelectorAll('#element-tags .el-tag');
    tags.forEach(tag => {
        tag.classList.remove('collected');
        tag.classList.add('pending');
    });
}

function recordFSMTransition(from, to) {
    AppState.fsmTransitions.push({
        fromState: from,
        toState: to,
        timestamp: new Date().toISOString(),
        trigger: 'USER_INPUT'
    });
}

function scrollToBottom(element) {
    requestAnimationFrame(() => {
        element.scrollTop = element.scrollHeight;
    });
}

// ========== 家长端功能 ==========
function addObservationTag(tag) {
    const textarea = document.getElementById('parent-observation');
    if (textarea) {
        const currentValue = textarea.value.trim();
        textarea.value = currentValue ? `${currentValue}，${tag}` : tag;
    }
}

async function analyzeObservation() {
    const observation = document.getElementById('parent-observation')?.value.trim();
    if (!observation) {
        showToast('请先输入观察内容', 'error');
        return;
    }
    
    const analyzeBtn = document.querySelector('.analyze-btn');
    if (analyzeBtn) {
        analyzeBtn.disabled = true;
        analyzeBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 分析中...';
    }
    
    try {
        const config = AppState.apiConfig;
        
        const parentSystemPrompt = `你是证言系统的家长端AI分析助手。

任务：根据家长的观察记录，输出结构化的风险评估报告。

输出格式要求：
{
    "risk_level": "GREEN/YELLOW/RED",
    "pressure_sources": ["学业压力", "人际关系"],
    "communication_script": "具体的沟通建议话术"
}

评估标准：
- GREEN：正常波动，持续关注即可
- YELLOW：需要关注和适度干预
- RED：需要立即寻求专业帮助

重要约束：
- 不做心理诊断
- 只输出风险评估和建议
- 建议要具体可执行`;

        const response = await fetch(`${config.endpoint}/chat/completions`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${config.apiKey}`
            },
            body: JSON.stringify({
                model: config.model,
                messages: [
                    { role: 'system', content: parentSystemPrompt },
                    { role: 'user', content: `家长观察记录：${observation}` }
                ],
                max_tokens: 400,
                temperature: 0.5
            })
        });
        
        let analysisData;
        
        if (response.ok) {
            const aiResponse = await response.json();
            const aiText = aiResponse.choices[0]?.message?.content || '{}';
            
            // 尝试解析JSON
            try {
                const jsonMatch = aiText.match(/\{[\s\S]*\}/);
                analysisData = jsonMatch ? JSON.parse(jsonMatch[0]) : {};
            } catch {
                analysisData = parseAnalysisFromText(aiText);
            }
        } else {
            // 使用模拟数据作为降级方案
            analysisData = generateMockAnalysis(observation);
        }
        
        displayAnalysisResult(analysisData);
        
    } catch (error) {
        console.error('分析失败:', error);
        displayAnalysisResult(generateMockAnalysis(observation));
    } finally {
        if (analyzeBtn) {
            analyzeBtn.disabled = false;
            analyzeBtn.innerHTML = '<i class="fas fa-brain"></i> AI智能分析';
        }
    }
}

function generateMockAnalysis(observation) {
    const hasWarningWords = /不想上学|情绪低落|成绩下降|社交回避/.test(observation);
    const hasHighRiskWords = /自杀|自残|伤害|死/.test(observation);
    
    let riskLevel = 'GREEN';
    let pressureSources = ['日常学习压力'];
    let script = '建议继续保持与孩子的良好沟通，多关注其情绪变化。';
    
    if (hasWarningWords) {
        riskLevel = 'YELLOW';
        pressureSources = ['学业压力', '人际交往'];
        script = `建议您这样和孩子沟通：

"${getGreeting()}，最近注意到你好像有些心事。如果你愿意的话，可以随时和我说说。不管发生什么，爸爸妈妈都会支持你。"

💡 **门把手效应技巧**：说完后给孩子留出空间，不要期待立即回应。研究表明这种方式让孩子主动开口的概率高出4倍。`;
    }
    
    if (hasHighRiskWords) {
        riskLevel = 'RED';
        pressureSources = ['心理健康预警'];
        script = `⚠️ **重要提醒**：您的描述中包含高风险信号。

**立即行动建议：**
1. 保持冷静，不要质问孩子
2. 营造安全、无评判的沟通环境
3. 如有必要，寻求专业心理咨询师的帮助

**推荐沟通方式：**
"我看到你最近好像不太开心。无论发生什么，我们都在一起面对。你需要的时候，随时可以跟我说。"`;
    }
    
    return { risk_level: riskLevel, pressure_sources: pressureSources, communication_script: script };
}

function getGreeting() {
    const hour = new Date().getHours();
    if (hour < 12) return '早上好';
    if (hour < 18) return '下午好';
    return '晚上好';
}

function parseAnalysisFromText(text) {
    return {
        risk_level: text.includes('RED') ? 'RED' : text.includes('YELLOW') ? 'YELLOW' : 'GREEN',
        pressure_sources: ['综合因素'],
        communication_script: text
    };
}

function displayAnalysisResult(data) {
    const resultSection = document.getElementById('analysis-result');
    if (!resultSection) return;
    
    resultSection.style.display = 'block';
    
    // 风险等级
    const riskLevelEl = document.getElementById('risk-level-display');
    const riskTextEl = document.getElementById('risk-text');
    if (riskLevelEl) {
        riskLevelEl.className = `risk-level ${data.risk_level.toLowerCase()}`;
    }
    if (riskTextEl) {
        const levelNames = { GREEN: '低风险', YELLOW: '中等风险', RED: '高风险' };
        riskTextEl.textContent = levelNames[data.risk_level] || data.risk_level;
    }
    
    // 压力源
    const pressureList = document.getElementById('pressure-list');
    if (pressureList && data.pressure_sources) {
        pressureList.innerHTML = data.pressure_sources.map(s => `<li>${s}</li>`).join('');
    }
    
    // 沟通脚本
    const scriptContent = document.getElementById('script-content');
    if (scriptContent && data.communication_script) {
        scriptContent.innerHTML = formatMessage(data.communication_script);
    }
    
    // 滚动到结果
    resultSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
    showToast('📊 分析完成', 'success');
}

// ========== 证据包生成 ==========
function generateEvidencePackage() {
    showToast('⏳ 正在生成司法级加密证据包...', 'success');
    
    // 模拟生成过程
    setTimeout(() => {
        const pkgId = `EP_${generateUUID()}`;
        setElementText('pkg-id', pkgId);
        setElementText('pkg-time', new Date().toLocaleString('zh-CN'));
        setElementHtml('pkg-status', '<span class="badge badge-success">已加密</span>');
        
        AppState.packageCount++;
        updateStatDisplay();
        
        showToast('✅ 证据包已生成！包含：录屏+操作日志+传感器数据+时间戳序列+Merkle根哈希', 'success');
        
        // 可选：跳转到报告页面
        // navigateTo('readiness-report');
    }, 2000);
}

// ========== Merkle树可视化 ==========
function drawMerkleTree() {
    const canvas = document.getElementById('merkle-canvas');
    if (!canvas) return;
    
    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;
    
    // 清空画布
    ctx.clearRect(0, 0, width, height);
    
    // 设置样式
    ctx.fillStyle = '#2563eb';
    ctx.strokeStyle = '#3b82f6';
    ctx.lineWidth = 2;
    ctx.font = '11px monospace';
    
    // 叶子节点（底部）
    const leaves = ['H₁', 'H₂', 'H₃', 'H₄', 'H₅', 'H₆'];
    const leafY = height - 30;
    const leafSpacing = width / (leaves.length + 1);
    
    leaves.forEach((hash, i) => {
        const x = leafSpacing * (i + 1);
        ctx.beginPath();
        ctx.arc(x, leafY, 14, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = '#fff';
        ctx.fillText(hash, x - 10, leafY + 4);
        ctx.fillStyle = '#2563eb';
    });
    
    // 中间层
    const midY = height / 2 + 20;
    for (let i = 0; i < 3; i++) {
        const x = leafSpacing * (i * 2 + 1.5);
        ctx.beginPath();
        ctx.arc(x, midY, 16, 0, Math.PI * 2);
        ctx.fillStyle = '#7c3aed';
        ctx.fill();
        ctx.fillStyle = '#fff';
        ctx.fillText(`H${23+i}`, x - 10, midY + 4);
        ctx.fillStyle = '#2563eb';
        
        // 连接线
        ctx.strokeStyle = '#475569';
        ctx.setLineDash([4, 4]);
        ctx.beginPath();
        ctx.moveTo(x, midY - 16);
        ctx.lineTo(leafSpacing * (i * 2 + 1), leafY + 14);
        ctx.moveTo(x, midY - 16);
        ctx.lineTo(leafSpacing * (i * 2 + 2), leafY + 14);
        ctx.stroke();
    }
    
    // 根节点
    ctx.fillStyle = '#10b981';
    ctx.beginPath();
    ctx.arc(width / 2, 35, 22, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = '#fff';
    ctx.font = 'bold 13px monospace';
    ctx.fillText('Root Hash', width / 2 - 32, 40);
    
    // 连接到根节点
    ctx.strokeStyle = '#475569';
    ctx.setLineDash([4, 4]);
    for (let i = 0; i < 3; i++) {
        const x = leafSpacing * (i * 2 + 1.5);
        ctx.beginPath();
        ctx.moveTo(x, midY - 16);
        ctx.lineTo(width / 2, 57);
        ctx.stroke();
    }
    
    ctx.setLineDash([]);
}

// ========== 报告动画 ==========
function animateReportScores() {
    // 动画效果已在CSS中定义
    setTimeout(() => {
        const dims = ['dim-evidence', 'dim-time', 'dim-ai', 'dim-privacy'];
        dims.forEach((id, index) => {
            const el = document.getElementById(id);
            if (el) {
                el.style.width = '0%';
                setTimeout(() => {
                    el.style.width = el.getAttribute('style').match(/width:\s*([\d.]+)%/)?.[1] || '80%';
                }, index * 200 + 300);
            }
        });
    }, 500);
}

// ========== 工具函数 ==========
function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
        const r = Math.random() * 16 | 0;
        return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
    }).toUpperCase();
}

function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    if (!toast) return;
    
    toast.textContent = message;
    toast.className = `toast ${type} show`;
    
    setTimeout(() => {
        toast.classList.remove('show');
    }, 3500);
}

function setElementText(id, text) {
    const el = document.getElementById(id);
    if (el) el.textContent = text;
}

function setElementHtml(id, html) {
    const el = document.getElementById(id);
    if (el) el.innerHTML = html;
}

function updateStatDisplay() {
    const sessionsEl = document.getElementById('stat-sessions');
    const packagesEl = document.getElementById('stat-packages');
    if (sessionsEl) sessionsEl.textContent = AppState.sessionCount;
    if (packagesEl) packagesEl.textContent = AppState.packageCount;
}

// ========== API配置 ==========
function loadApiConfig() {
    const saved = localStorage.getItem('testimony_api_config');
    if (saved) {
        try {
            AppState.apiConfig = JSON.parse(saved);
        } catch (e) {}
    }
    
    // 更新UI
    const keyInput = document.getElementById('api-key-input');
    const epInput = document.getElementById('api-endpoint-input');
    const modelInput = document.getElementById('api-model-input');
    
    if (keyInput) keyInput.value = AppState.apiConfig.apiKey;
    if (epInput) epInput.value = AppState.apiConfig.endpoint;
    if (modelInput) modelInput.value = AppState.apiConfig.model;
}

function saveApiConfig() {
    const keyInput = document.getElementById('api-key-input');
    const epInput = document.getElementById('api-endpoint-input');
    const modelInput = document.getElementById('api-model-input');
    
    if (keyInput && epInput && modelInput) {
        AppState.apiConfig = {
            apiKey: keyInput.value,
            endpoint: epInput.value,
            model: modelInput.value
        };
        
        localStorage.setItem('testimony_api_config', JSON.stringify(AppState.apiConfig));
        closeApiConfig();
        showToast('✅ API配置已保存', 'success');
    }
}

function closeApiConfig() {
    document.getElementById('api-config-modal')?.classList.add('hidden');
}

// 全局暴露API配置入口（长按logo打开）
document.addEventListener('contextmenu', (e) => {
    if (e.target.closest('.brand-icon') || e.target.closest('.shield-logo')) {
        e.preventDefault();
        document.getElementById('api-config-modal')?.classList.remove('hidden');
    }
});

// 错误处理
window.onerror = function(msg, url, lineNo, columnNo, error) {
    console.error('Error:', msg, '\nAt:', url, ':', lineNo);
    return false;
};

console.log('%c🛡️ 证言 Testimony.ai', 'font-size:24px;color:#2563eb;font-weight:bold;');
console.log('%cAI深伪时代青少年霸凌事件可信存证系统', 'font-size:14px;color:#64748b;');
