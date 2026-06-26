/**
 * 证言 Testimony.ai v3.0 - 防伪存证系统 前端引擎
 * 
 * 核心功能：
 * 1. AI 引导式事件记录
 * 2. 文件上传（拖拽/点击）→ 计算SHA-256指纹
 * 3. 生成密码学级防伪存证证书
 * 4. 可视化验证流程展示
 */

// ========== 状态 ==========
const state = {
    transcript: [],
    facts: { "时间": "", "地点": "", "人物": "", "行为": "", "证据载体": "", "后果": "" },
    missingElements: ["时间","地点","人物","行为","证据载体","后果"],
    riskLevel: "GREEN",
    uploadedFiles: [], // 已选择的文件列表
    isProcessing: false,
};

// ========== DOM ==========
const $ = id => document.getElementById(id);
const chatLog = $("chat-log");
const chatForm = $("chat-form");
const chatInput = $("chat-input");
const sendBtn = $("send-btn");
const healthPill = $("health-pill");
const factGrid = $("fact-grid");
const missingHint = $("missing-hint");

// 文件上传相关
const uploadZone = $("upload-zone");
const fileInput = $("file-input");
const fileListEl = $("file-list");
const certifyBtn = $("certify-btn");

// 家长分析
const obsInput = $("observation-input");
const analyzeBtn = $("analyze-btn");
const sampleBtn = $("sample-btn");
const assessResult = $("assessment-result");

// 证书展示
const certPlaceholder = $("cert-placeholder");
const certResult = $("cert-result");

// Tab
const tabs = document.querySelectorAll(".tab");
const panels = document.querySelectorAll(".panel");

// ========== 工具函数 ==========
function esc(s) {
    const d = document.createElement("div"); d.textContent = s; return d.innerHTML;
}
function fmtRisk(r) { return r==="RED"?'🔴 高风险':r==="YELLOW"?'🟡 中风险':'🟢 低风险'; }
function setHealth(t, k) { healthPill.innerHTML = t; healthPill.className = `pill ${k||""}`.trim(); }

// ========== 消息渲染 ==========
function addMsg(role, content) {
    const el = document.createElement("div");
    el.className = `message ${role}`;
    el.innerHTML = esc(content);
    chatLog.appendChild(el);
    chatLog.scrollTo({ top: chatLog.scrollHeight, behavior: 'smooth' });
}
function showTyping() {
    const el = document.createElement("div"); el.id="typing";
    el.className = "message ai typing-indicator";
    el.innerHTML = "<span></span><span></span><span></span>";
    chatLog.appendChild(el); chatLog.scrollTo({top:chatLog.scrollHeight,behavior:'smooth'});
}
function hideTyping() { const el=$("typing"); if(el) el.remove(); }

// ========== 要素面板 ==========
function renderFacts() {
    const icons = {"时间":"🕐","地点":"📍","人物":"👤","行为":"⚡","证据载体":"📎","后果":"💬"};
    factGrid.innerHTML = Object.entries(state.facts).map(([k,v]) => `
        <div class="fact-item" style="${v?'border-left:3px solid #059669;':'opacity:.55;'}">
            <strong>${icons[k]} ${k}</strong>
            <span>${v ? esc(v.substring(0,40)) : "待采集..."}</span>
        </div>`).join('');
    
    const c = Object.values(state.facts).filter(Boolean).length;
    missingHint.innerHTML = state.missingElements.length === 0
        ? `✅ 全部采集完毕 (${c}/6)`
        : `⏳ 待补充：${state.missingElements.join("、")} (${c}/6)`;
}

// ========== 网络请求 ==========
async function post(url, data) {
    const r = await fetch(url,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(data)});
    if(!r.ok){const t=await r.text();throw new Error(t||`HTTP ${r.status}`);}
    return r.json();
}
async function get(url) { const r=await fetch(url); if(!r.ok) throw new Error(`GET ${r.status}`); return r.json(); }

// ========== 健康检查 ==========
async function checkHealth() {
    try{const d=await get("/api/health");setHealth(`● ${d.service} v${d.version}`,"success");}
    catch(e){setHealth("● 后端不可用","warn");}
}

// ========== AI 聊天 ==========
async function startChat() {
    addMsg("system", "🛡️ 防伪模式已启动。你可以先描述事件，然后上传截图/录音等证据文件，我会为你生成防伪证书。");
    showTyping();
    try{
        const d=await post("/api/interview/start",{});
        hideTyping();
        addMsg("ai", d.reply);
        state.facts=d.facts;state.missingElements=d.missing_elements;
        renderFacts();
    }catch(e){
        hideTyping();
        addMsg("system",`⚠️ 初始化失败:${e.message}`);
    }
}

async function sendMsg() {
    const msg = chatInput.value.trim();
    if(!msg || state.isProcessing)return;
    
    addMsg("user",msg);
    state.transcript.push({role:"user",content:msg});
    chatInput.value="";
    sendBtn.disabled=true;state.isProcessing=true;
    showTyping();

    try{
        const d=await post("/api/interview/message",{message:msg,transcript:state.transcript,facts:state.facts});
        hideTyping();
        state.transcript.push({role:"assistant",content:d.reply});
        addMsg("ai",d.reply);
        state.facts=d.facts;state.missingElements=d.missing_elements;state.riskLevel=d.risk_level;
        renderFacts();
        
        // 提示可以上传证据
        if(d.missing_elements && d.missing_elements.length<=2 && !state.uploadedFiles.length){
            setTimeout(()=>addMsg("system","💡 信息已基本完整。你可以切换到「上传证据」标签页，上传截图或录音来生成防伪证书。"),800);
        }
    }catch(e){
        hideTyping();
        addMsg("system",`❌ 错误:${e.message}`);
    }finally{
        sendBtn.disabled=false;state.isProcessing=false;chatInput.focus();
    }
}

// ========== ★★★ 文件上传处理 ★★★ ==========
function initFileUpload() {
    // 点击上传
    uploadZone.addEventListener("click", () => fileInput.click());
    
    // 拖拽上传
    uploadZone.addEventListener("dragover", e => { e.preventDefault(); uploadZone.classList.add("dragover"); });
    uploadZone.addEventListener("dragleave", () => uploadZone.classList.remove("dragover"));
    uploadZone.addEventListener("drop", e => {
        e.preventDefault(); uploadZone.classList.remove("dragover");
        handleFiles(e.dataTransfer.files);
    });

    // 文件选择
    fileInput.addEventListener("change", () => {
        handleFiles(fileInput.files);
        fileInput.value = ""; // 允许重复选择同一文件
    });
}

function handleFiles(files) {
    Array.from(files).forEach(file => {
        // 检查大小（限制10MB）
        if(file.size > 10*1024*1024){
            alert(`${file.name} 超过10MB限制`); return;
        }
        
        // 检查重复
        if(state.uploadedFiles.some(f=>f.name===file.name && f.size===file.size)){
            return; // 已存在
        }

        // 存储文件信息
        state.uploadedFiles.push({
            name: file.name,
            size: file.size,
            type: file.type,
            file: file // 保留 File 对象引用
        });
    });

    renderFileList();
    certifyBtn.disabled = state.uploadedFiles.length === 0;
}

function renderFileList() {
    if(state.uploadedFiles.length===0){
        fileListEl.innerHTML = "";
        return;
    }

    fileListEl.innerHTML = state.uploadedFiles.map((f,i) => `
        <div class="file-item">
            <span class="file-item-icon">${getFileIcon(f.name)}</span>
            <div class="file-item-info">
                <div class="file-item-name">${esc(f.name)}</div>
                <div class="file-item-size">${formatSize(f.size)}</div>
            </div>
            <span class="file-item-remove" onclick="removeFile(${i})">✕</span>
        </div>`).join("");
}

window.removeFile = function(i) {
    state.uploadedFiles.splice(i,1);
    renderFileList();
    certifyBtn.disabled = state.uploadedFiles.length===0;
};

function getFileIcon(name) {
    const ext = name.split('.').pop().toLowerCase();
    if(['png','jpg','jpeg','gif','webp'].includes(ext)) return '🖼️';
    if(['mp4','avi','mov'].includes(ext)) return '🎬';
    if(['mp3','wav','ogg'].includes(ext)) return '🎵';
    if(ext==='pdf') return '📄';
    return '📁';
}

function formatSize(bytes) {
    for(const u of ['B','KB','MB','GB']){
        if(bytes<1024) return bytes.toFixed(1)+' '+u;
        bytes/=1024;
    } return bytes.toFixed(1)+' TB';
}

// 将 File 对象转为 base64（用于发送到后端）
async function fileToBase64(file) {
    return new Promise((resolve,reject)=>{
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = reject;
        reader.readAsDataURL(file);
    });
}

// ========== ★★★ 防伪证书生成与展示 ★★★ ==========
async function generateCertificate() {
    if(!state.uploadedFiles.length || state.isProcessing) return;

    certifyBtn.disabled=true;state.isProcessing=true;
    certResult.classList.add("hidden");
    certPlaceholder.style.display="";

    try {
        // 将所有文件转为base64
        const filesData = await Promise.all(
            state.uploadedFiles.map(async f => ({
                name: f.name,
                size: f.size,
                mime: f.type,
                data: await fileToBase64(f.file)
            }))
        );

        // 调用后端API生成证书
        const result = await post("/api/evidence/certify", {
            files: filesData,
            facts: state.facts,
            transcript: state.transcript
        });

        renderCertificate(result);

    } catch(e) {
        certResult.innerHTML = `<div style="background:#fef2f12;border-radius:14px;padding:18px;color:#991b1b;">
            <strong>❌ 生成失败</strong><p style="margin-top:8px;font-size:13px;">${esc(e.message)}</p>
        </div>`;
        certResult.classList.remove("hidden");
    } finally {
        certifyBtn.disabled=false;state.isProcessing=false;
    }
}

function renderCertificate(result) {
    certPlaceholder.style.display="none";
    
    const certs = result.evidence_certificates || [];
    
    if(certs.length === 0 || certs[0].error) {
        certResult.innerHTML = `<div style="background:#fffbeb;padding:20px;border-radius:14px;text-align:center;color:#92400e;">
            ⚠️ ${(certs[0]?.error || "无法生成证书")}</div>`;
        certResult.classList.remove("hidden");
        return;
    }

    // 渲染所有文件的证书
    let certsHtml = "";
    certs.forEach((cert, idx) => {
        if(cert.error) return;

        certsHtml += `
        <!-- 证书头部 -->
        <div class="cert-header" style="${idx>0?'margin-top:24px;':''}">
            <div style="font-size:22px;font-weight:800;">🔐 防伪存证证书</div>
            <div class="cert-id">${esc(cert.certificate_id)}</div>
        </div>

        <!-- 文件信息 -->
        <div class="metric-grid" style="margin-bottom:16px;">
            <div class="metric"><strong>📁 文件名称</strong><span>${esc(cert.file_name)}</span></div>
            <div class="metric"><strong>📏 文件大小</strong><span>${esc(cert.file_size_human)} (${cert.file_size} 字节)</span></div>
            <div class="metric"><strong>🏷️ 文件类型</strong><span>${esc(cert.file_type)}</span></div>
            <div class="metric"><strong>⏰ 存证时间</strong><span style="font-size:12px;">${new Date(cert.generated_at).toLocaleString('zh-CN')}</span></div>
        </div>

        <!-- ★★★ 数字指纹展示区 ★★★ -->
        <div class="fingerprint-display">
            <div class="fingerprint-label">🔒 SHA-256 数字指纹 (Digital Fingerprint)</div>
            <div class="fingerprint-value">${esc(cert.digital_fingerprint.hash_value)}</div>
            <div class="fingerprint-algo">
                算法：${esc(cert.digital_fingerprint.algorithm)} | 
                验证码：<span style="color:#fbbf24">${esc(cert.verification_code)}</span>
            </div>
        </div>

        <!-- 快速验证说明 -->
        <div style="text-align:center;margin:14px 0;padding:12px;background:#ecfdf5;border-radius:12px;color:#166534;font-size:13px;line-height:1.7;">
            ✨ <strong>快速验证：</strong>将此哈希值与对方声称"P图"的文件重新计算的SHA-256对比<br>
            → 结果不一致 = 对方的文件确实被修改过 ✓
        </div>

        <!-- 详细验证步骤 -->
        <div class="verify-steps">
            <div style="font-weight:700;font-size:15px;color:#1d4ed8;margin-bottom:8px;">📋 如何独立验证真伪？</div>
            
            <div class="verify-step"><div class="verify-num">1</div>
                <div class="verify-text">保存你的<strong>原始文件</strong>（不做任何编辑、压缩或修改）</div></div>
            <div class="verify-step"><div class="verify-num">2</div>
                <div class="verify-text">使用以下命令计算该文件的 SHA-256 哈希值：</div></div>
        `;

        // 验证命令（根据操作系统）
        const how = cert.how_to_verify || {};
        cmds = [
            {label:"Windows",cmd:how.windows_command},
            {label:"Mac / Linux",cmd:how.linux_command},
            {label:"Python",cmd:how.python_command},
        ];
        cmds.forEach(c => {
            certsHtml += `<div class="cmd-box"><span style="color:#94a3b8;margin-right:8px;">${c.label}:</span> ${esc(c.cmd)}</div>`;
        });

        certsHtml += `
            <div class="verify-step"><div class="verify-num">3</div>
                <div class="verify-text">将计算结果与上方数字指纹对比：</div></div>
            
            <div style="background:#064e3b;border-radius:12px;padding:16px;text-align:center;margin:10px 0;">
                <div style="color:#6ee7b7;font-size:12px;margin-bottom:6px;">预期结果（正确时应该看到这个值）:</div>
                <div class="expected-result" style="font-family:monospace;font-size:15px;letter-spacing:1.5px;">
                    ${esc(how.expected_result || "")}
                </div>
            </div>

            <div class="verify-step"><div class="verify-num">4</div>
                <div class="verify-text">
                    <strong style="color:#059669;">完全一致 → 文件100%未被篡改 ✓</strong><br>
                    <strong style="color:#dc2626;">不一致 → 文件已被修改 ✗</strong>
                </div></div>
        </div>

        <!-- 时间锚定 -->
        <div class="metric" style="margin-top:16px;background:#faf5ff;border-color:#e9d5ff;">
            <strong>⏱️ 时间锚定（证明文件在以下时刻已存在）</strong>
            <span style="font-size:12.5px;">
                ${(cert.timestamp_anchors||[]).map(t=>`• ${t.source}: ${esc(t.time)}`).join("<br>")}
            </span>
        </div>
        `;
    });

    // 批量存证信息
    if(result.batch_merkle_root) {
        certsHtml += `
        <div class="metric" style="margin-top:16px;background:#f0fdf4;border-color:#86efac;">
            <strong>🔗 批量存证 Merkle 根</strong>
            <span style="font-family:monospace;font-size:11px;word-break:break-all;">${esc(result.batch_merkle_root)}</span>
        </div>`;
    }

    // 底部声明
    certsHtml += `
    <div style="margin-top:20px;padding:18px;background:linear-gradient(145deg,#eff6ff,#dbeafe);border-radius:16px;font-size:13px;color:#1e40af;line-height:1.75;text-align:center;">
        <strong>🔐 本证书基于 SHA-256 密码学安全哈希算法</strong><br>
        任何人都可以用公开的算法独立验证，无需信任任何第三方。<br>
        这就是数学保证的真实性。
    </div>`;

    certResult.innerHTML = certsHtml;
    certResult.classList.remove("hidden");
}

// ========== 家长分析 ==========
const SAMPLE_OBSERVATION = "孩子最近连续一周不太愿意去学校，晚上睡不好做噩梦，说肚子疼不想去。也不太和同学说话，放学就把门关上不让我看手机。以前很活泼的，现在变得特别沉默。老师打电话说上课走神成绩下降。";

async function runAssessment() {
    const obs = obsInput.value.trim();
    if(!obs){
        assessResult.innerHTML=`<div class="metric"style="background:#fffbeb;"><strong>⚠️ 请输入观察内容</strong></div>`;
        assessResult.classList.remove("hidden");return;
    }

    analyzeBtn.disabled=true;assessResult.classList.add("hidden");

    try{
        const d=await post("/api/assessment",{observation:obs});
        assessResult.innerHTML=`
        <div class="metric-grid">
            <div class="metric"><strong>🎯 风险等级</strong><span>${fmtRisk(d.risk_level)}</span></div>
            <div class="metric"><strong>📈 可信度</strong><span><div style="display:flex;align-items:center;gap:8px;margin-top:6px;"><div style="flex:1;height:8px;background:#e2e8f0;border-radius:4px;overflow:hidden;"><div style="width:${Math.round(d.confidence*100)}%;height:100%;background:linear-gradient(90deg,#059669,#10b981);"></div></div><strong>${Math.round(d.confidence*100)}%</strong></div></span></div>
        </div>
        <div class="metric" style="margin-top:16px;"><strong>🔍 可能压力源</strong><span>${(d.possible_stressors||[]).map(s=>`<span style="padding:4px 12px;background:#e0e7ff;color:#3730a3;border-radius:999px;font-size:13px;display:inline-block;margin:4px;">${s}</span>`).join("")}</span></div>
        <div class="metric" style="margin-top:16px;"><strong>📋 分析摘要</strong><span>${esc(d.summary)}</span></div>
        <div class="metric" style="margin-top:16px;background:#eff6ff;"><strong>💬 建议沟通话术</strong><span style="font-style:italic;line-height:1.85;">"${esc(d.suggested_script)}"</span></div>
        <div class="metric" style="margin-top:16px;"><strong>✅ 建议行动</strong><ul class="list">${(d.suggested_actions||[]).map(a=>`<li>${a}</li>`).join("")}</ul></div>`;
        assessResult.classList.remove("hidden");
    }catch(e){
        assessResult.innerHTML=`<div class="metric"style="background:#fef2f12;"><strong>❌ 失败:${esc(e.message)}</strong></div>`;
        assessResult.classList.remove("hidden");
    }finally{analyzeBtn.disabled=false;}
}

// ========== Tab 切换 ==========
function switchTab(id) {
    tabs.forEach(b=>b.classList.toggle("active",b.dataset.tab===id));
    panels.forEach(p=>p.classList.toggle("active",p.id===id));
}

// ========== 初始化 ==========
document.addEventListener("DOMContentLoaded",()=>{
    renderFacts();checkHealth();initFileUpload();startChat();

    // 事件绑定
    tabs.forEach(b=>b.addEventListener("click",()=>switchTab(b.dataset.tab)));
    chatForm.addEventListener("submit",e=>{e.preventDefault();sendMsg();});
    chatInput.addEventListener("keydown",e=>{if(e.key==="Enter"&&!e.shiftKey){e.preventDefault();sendMsg();}});
    certifyBtn.addEventListener("click",generateCertificate);
    analyzeBtn.addEventListener("click",runAssessment);
    sampleBtn.addEventListener("click",()=>{obsInput.value=SAMPLE_OBSERVATION;analyzeBtn.focus();});
});
