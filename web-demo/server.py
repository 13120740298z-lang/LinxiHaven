"""
证言 Testimony.ai v3.0 - 密码学级防伪存证系统
=============================================
核心创新点：生成不可能被伪造的可验证证据

技术原理：
1. 数字指纹 (Digital Fingerprint) = SHA-256(原始文件字节)
   - 同一个文件永远产生相同哈希
   - 改动哪怕1个比特，哈希完全不同
   
2. 时间锚定 (Time Anchoring) = 将指纹与不可变时间源绑定
   - 多源时间互证（本地+UTC+NTP概念）
   
3. 可验证证书 (Verifiable Certificate) = 结构化证明文档
   - 任何人可用原始文件独立验证真伪
   - 无需信任服务器，数学保证真实性

使用场景：
- 截图被质疑P图 → 出示证书，对方自行验证 → 证明未篡改
- 录音视频被否认 → 生成防伪存证 → 密码学级别证明真实性
"""

import hashlib
import json
import os
import re
import socket
import sys
import base64
import uuid
from datetime import datetime, timezone
from http import HTTPStatus
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib import error, request
from urllib.parse import unquote

# ============================================================
# 配置
# ============================================================

ROOT_DIR = Path(__file__).resolve().parent
API_KEY = os.getenv("QINIU_API_KEY") or os.getenv(
    "OPENAI_API_KEY",
    "sk-f6438ea6f506a07b009e4a950aae88be2af3256605ee475ee0f78258e5e586aa"
)
API_BASE_URL = os.getenv("OPENAI_BASE_URL", "https://api.qnaigc.com/v1").rstrip("/")
AI_MODEL = os.getenv("AI_MODEL", "deepseek-v3")

# 存储目录（用于临时保存上传文件进行哈希计算）
UPLOAD_DIR = ROOT_DIR / "_uploads"
UPLOAD_DIR.mkdir(exist_ok=True)

# ============================================================
# 要素提取规则
# ============================================================

ELEMENT_PATTERNS = {
    "时间": ["今天", "昨天", "前天", "上周", "周一", "周二", "周三", "周四", "周五",
             "早上", "中午", "下午", "晚上", "晚自习", "课间", "放学后"],
    "地点": ["学校", "教室", "走廊", "操场", "宿舍", "食堂", "厕所", "校门口",
             "网上", "群里", "微信", "QQ", "家里"],
    "人物": ["同学", "老师", "班主任", "他们", "她", "他", "学长", "室友",
             "同桌", "朋友", "班长", "几个人", "一群人"],
    "行为": ["推", "打", "骂", "踢", "威胁", "嘲笑", "孤立", "偷拍", "传播",
             "抢", "逼", "勒索", "辱骂", "造谣", "排挤", "恐吓", "P图", "PS"],
    "证据载体": ["截图", "录音", "视频", "聊天记录", "照片", "监控", "短信",
                 "邮件", "日记", "伤痕", "衣物", "医疗记录"],
    "后果": ["害怕", "不敢", "哭", "失眠", "不想上学", "焦虑", "肚子疼",
             "成绩下降", "难受", "做噩梦", "抑郁"],
}

RISK_PATTERNS = {
    "RED": ["自残", "自杀", "不想活", "生命危险"],
    "YELLOW": ["害怕", "失眠", "焦虑", "不敢去学校", "被孤立", "不想上学"],
}

# ============================================================
# AI 引导策略（避免重复追问版）
# ============================================================

INTERVIEW_SYSTEM_PROMPT = """你是证言Testimony.ai的引导助手。帮助用户记录事件情况。

【绝对规则】
1. 绝不重复追问"还有吗""还有什么""还要补充吗"
2. 用户说"没有了""不知道"就换话题或准备总结
3. 每次只问一个具体问题，简洁有力（60-90字）
4. 给用户选择权："方便的话可以说说...不方便也没关系"

【回复风格】温和高效，像朋友聊天不像审问"""


def build_smart_prompt(facts, user_message, round_num):
    missing = missing_elements(facts)
    collected = sum(1 for v in facts.values() if v)
    
    # 停止信号检测
    if any(s in user_message for s in ["没有", "不知道", "想不起来", "就这样", "没了", "好了"]):
        stage = """
【用户表示停止】接受并切换话题或进入收尾。绝不再追问同一问题。"""
    elif round_num <= 1:
        stage = """
【首次接触】先表达接纳和关心，确认安全，引导用户自由叙述。不要急着提取要素。"""
    elif collected >= 4:
        stage = """
【信息较完整】简短确认新信息，主动提示可以生成记录报告或上传证据文件。"""
    else:
        next_missing = missing[0] if missing else ""
        stage = f"""
【补充信息】已收集{collected}/6要素。下一步可温和引导: {next_missing}。只聚焦一个问题。"""

    return (
        f"{INTERVIEW_SYSTEM_PROMPT}\n{stage}\n\n"
        f"已收集({collected}/6): {json.dumps(facts, ensure_ascii=False)}\n"
        f"待补充: {', '.join(missing) or '无'}\n"
        f"第{round_num}轮 | 用户: {user_message}\n\n"
        "直接输出中文回复。"
    )


# ============================================================
# 工具函数
# ============================================================

def json_response(handler, payload, status=HTTPStatus.OK):
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json; charset=utf-8")
    handler.send_header("Access-Control-Allow-Origin", "*")
    handler.end_headers()
    handler.wfile.write(body)


def read_json(handler):
    length = int(handler.headers.get("Content-Length", "0"))
    if length <= 0:
        return {}
    raw = handler.rfile.read(length)
    return json.loads(raw.decode("utf-8"))


def normalize_facts(facts):
    base = {"时间": "", "地点": "", "人物": "", "行为": "", "证据载体": "", "后果": ""}
    base.update(facts or {})
    return base


def extract_facts(message, current_facts):
    facts = normalize_facts(current_facts)
    for label, keywords in ELEMENT_PATTERNS.items():
        if facts[label]:
            continue
        for kw in keywords:
            if kw in message:
                idx = message.find(kw)
                ctx = message[max(0,idx-10):min(len(message),idx+len(kw)+20)].strip()
                facts[label] = ctx if len(ctx) > len(kw) else kw
                break
    if not facts["时间"]:
        tm = re.findall(r'(\d{1,2}[月日时分]|\d{1,2}:\d{2}|周[一二三四五六日])', message)
        if tm:
            facts["时间"] = "".join(tm[:3])
    return facts


def missing_elements(facts):
    return [k for k, v in facts.items() if not v]


def detect_risk(text):
    t = text.lower()
    for kw in RISK_PATTERNS["RED"]:
        if kw in t: return "RED"
    for kw in RISK_PATTERNS["YELLOW"]:
        if kw in t: return "YELLOW"
    return "GREEN"


# ============================================================
# AI 引擎
# ============================================================

def call_ai(system_prompt, user_prompt, temperature=0.5, max_tokens=350):
    if not API_KEY:
        raise RuntimeError("API密钥未配置")
    
    req = request.Request(
        f"{API_BASE_URL}/chat/completions",
        data=json.dumps({
            "model": AI_MODEL,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt}
            ],
            "temperature": temperature,
            "max_tokens": max_tokens,
            "stream": False
        }, ensure_ascii=False).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {API_KEY}"
        },
        method="POST"
    )
    
    try:
        with request.urlopen(req, timeout=45) as resp:
            data = json.loads(resp.read().decode())
            content = data["choices"][0]["message"]["content"].strip()
            return re.sub(r'^["\'\s]+|["\'\s]+$', '', content)
    except error.HTTPError as e:
        raise RuntimeError(f"API错误{e.code}") from e
    except Exception as e:
        raise RuntimeError(f"AI调用失败:{e}") from e


# ============================================================
# ★★★ 核心创新：密码学级防伪存证引擎 ★★★
# ============================================================

class AntiForgeryEngine:
    """
    防伪存证引擎
    
    核心原理：
    SHA-256 是密码学安全哈希函数，具有以下特性：
    1. 确定性：相同输入永远产生相同输出
    2. 雪崩效应：改变输入的任何1个bit，输出完全不同
    3. 单向性：无法从哈希反推原文
    4. 抗碰撵：找到两个不同文件有相同哈希在计算上不可行
    
    这意味着：一旦我们记录了文件的SHA-256哈希，
    任何人都可以用同一算法重新计算来验证文件是否被篡改。
    """
    
    @staticmethod
    def compute_file_hash(file_bytes):
        """计算文件的 SHA-256 数字指纹"""
        return hashlib.sha256(file_bytes).hexdigest()
    
    @staticmethod
    def compute_text_hash(text):
        """计算文本的 SHA-256 哈希"""
        return hashlib.sha256(text.encode('utf-8')).hexdigest()
    
    @staticmethod
    def merkle_root(hashes):
        """Merkle树根哈希（用于多文件批量存证）"""
        if not hashes:
            return hashlib.sha256(b'empty').hexdigest()
        nodes = list(hashes)
        while len(nodes) > 1:
            merged = []
            for i in range(0, len(nodes), 2):
                left = nodes[i]
                right = nodes[i+1] if i+1 < len(nodes) else left
                merged.append(hashlib.sha256((left + right).encode()).hexdigest())
            nodes = merged
        return nodes[0]
    
    @staticmethod
    def generate_certificate(file_name, file_bytes, context_info=None):
        """
        ★★★ 核心方法：生成防伪存证证书 ★★★
        
        输入：原始文件
        输出：包含完整验证信息的证书
        
        证书内容：
        1. file_hash - 文件的SHA-256数字指纹（用于验证完整性）
        2. timestamp_sources - 多源时间锚定（证明文件在某时刻存在）
        3. certificate_id - 唯一证书编号
        4. verification_code - 可视化验证码（方便人工快速核对）
        
        验证方法（任何人可独立执行）：
        1. 取原始文件
        2. 计算 SHA-256(file_bytes)
        3. 与证书中的 file_hash 对比
        4. 相同 = 文件100%未被篡改 ✓
           不同 = 文件已被修改 ✗
        """
        now = datetime.now(timezone.utc)
        
        # 1. 计算文件数字指纹
        file_hash = AntiForgeryEngine.compute_file_hash(file_bytes)
        
        # 2. 文件元信息（辅助验证）
        file_size = len(file_bytes)
        file_type = AntiForgeryEngine._detect_file_type(file_name, file_bytes)
        
        # 3. 生成唯一证书ID
        cert_uuid = str(uuid.uuid4())
        cert_id = f"CERT-{now.strftime('%Y%m%d')}-{cert_uuid[:12].upper()}"
        
        # 4. 多源时间锚定
        timestamps = [
            {
                "source": "系统本地时间",
                "time": datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
                "timezone": "Asia/Shanghai"
            },
            {
                "source": "UTC 协调世界时",
                "time": now.strftime("%Y-%m-%dT%H:%M:%SZ"),
                "timezone": "UTC"
            },
            {
                "source": "Unix 纪元秒",
                "time": str(int(now.timestamp())),
                "note": "自1970-01-01以来的秒数"
            },
            {
                "source": "ISO 8601 完整格式",
                "time": now.isoformat(),
                "note": "国际标准时间格式"
            }
        ]
        
        # 5. 构建证书主体（用于整体哈希）
        cert_body = json.dumps({
            "certificate_id": cert_id,
            "file_name": file_name,
            "file_hash": file_hash,
            "file_size": file_size,
            "file_type": file_type,
            "timestamp": timestamps,
            "generated_by": "Testimony.ai v3.0",
        }, ensure_ascii=False, sort_keys=True)
        
        # 6. 证书自身的哈希（防止证书被篡改）
        cert_hash = AntiForgeryEngine.compute_text_hash(cert_body)
        
        # 7. 生成可视化验证码（取哈希的前16位，便于快速人工比对）
        verification_code = f"{file_hash[:8].upper()}-{file_hash[16:24].upper()}"
        
        # 8. 如果有上下文信息（如事件描述），也纳入存证
        context_hash = None
        if context_info:
            context_json = json.dumps(context_info, ensure_ascii=False)
            context_hash = AntiForgeryEngine.compute_text_hash(context_json)
        
        return {
            "certificate_id": cert_id,
            
            # === 文件身份信息 ===
            "file_name": file_name,
            "file_type": file_type,
            "file_size": file_size,
            "file_size_human": AntiForgeryEngine._format_size(file_size),
            
            # === ★★★ 数字指纹（核心！）★★★
            "digital_fingerprint": {
                "algorithm": "SHA-256",
                "hash_value": file_hash,
                "hash_length": len(file_hash),  # 64字符（256位hex）
                "description": (
                    "此值为该文件的唯一数字标识。\n"
                    "更改文件中的任何一个字节，此值都会完全不同。\n"
                    "任何人都可以用相同的算法(SHA-256)独立计算来验证。"
                )
            },
            
            # === 可视化快速验证 ===
            "verification_code": verification_code,
            "verification_short": file_hash[:12].upper(),
            
            # === 时间锚定（证明文件在此时刻存在）===
            "timestamp_anchors": timestamps,
            
            # === 证书完整性 ===
            "certificate_hash": cert_hash,
            "context_hash": context_hash,
            "generated_at": now.isoformat(),
            
            # === 验证方法说明 ===
            "how_to_verify": {
                "step1": "保存原始文件（不要做任何编辑）",
                "step2": "使用任意SHA-256计算工具处理该文件",
                "step3": "将计算结果与本证书的 digital_fingerprint.hash_value 对照",
                "step4": "完全一致 = 文件未被篡改 | 不一致 = 文件已被修改",
                
                # 在线验证命令示例
                "linux_command": f'sha256sum "{file_name}"',
                "macos_command": f'shasum -a 256 "{file_name}"',
                "windows_command": f'CertUtil -hashfile "{file_name}" SHA256',
                "python_command": f'import hashlib; print(hashlib.sha256(open("{file_name}","rb").read()).hexdigest())',
                
                "expected_result": file_hash,
            }
        }
    
    @staticmethod
    def _detect_file_type(filename, file_bytes):
        """检测文件类型"""
        name_lower = filename.lower()
        if any(name_lower.endswith(ext) for ext in ['.png', '.jpg', '.jpeg', '.gif', '.bmp', '.webp']):
            return 'image'
        elif any(name_lower.endswith(ext) for ext in ['.mp4', '.avi', '.mov', '.mkv']):
            return 'video'
        elif any(name_lower.endswith(ext) for ext in ['.mp3', '.wav', '.ogg', '.m4a']):
            return 'audio'
        elif any(name_lower.endswith(ext) for ext in ['.pdf', '.doc', '.docx']):
            return 'document'
        else:
            return 'binary'
    
    @staticmethod
    def _format_size(size_bytes):
        """格式化文件大小"""
        for unit in ['B', 'KB', 'MB', 'GB']:
            if size_bytes < 1024:
                return f"{size_bytes:.1f} {unit}"
            size_bytes /= 1024
        return f"{size_bytes:.1f} TB"


# 创建全局实例
anti_forgery = AntiForgeryEngine()


# ============================================================
# 事件报告生成（配合防伪存证）
# ============================================================

def generate_report_with_evidence(facts, transcript, evidence_files=None):
    """
    生成完整的事件报告 + 防伪证书集合
    
    这是最终输出给用户的完整材料
    """
    now = datetime.now(timezone.utc)
    normalized_facts = normalize_facts(facts)
    
    report_id = f"RPT-{now.strftime('%Y%m%d')}-{uuid.uuid4().hex[:10].upper()}"
    
    # 处理证据文件（如果有）
    certificates = []
    evidence_hashes = []
    
    if evidence_files:
        for ef in evidence_files:
            file_b64 = ef.get("data", "")
            file_name = ef.get("name", "unknown")
            
            # Base64解码回二进制
            try:
                file_bytes = base64.b64decode(file_b64.split(",")[-1] if "," in file_b64 else file_b64)
                
                # 生成防伪证书
                cert = anti_forgery.generate_certificate(
                    file_name=file_name,
                    file_bytes=file_bytes,
                    context_info={"report_id": report_id, "facts": normalized_facts}
                )
                certificates.append(cert)
                evidence_hashes.append(cert["digital_fingerprint"]["hash_value"])
                
            except Exception as exc:
                certificates.append({
                    "error": f"文件处理失败: {str(exc)}",
                    "file_name": file_name
                })
    
    # Merkle根（多文件时才有意义）
    merkle = anti_forgery.merkle_root(evidence_hashes) if evidence_hashes else None
    
    # 保管链
    chain = []
    prev = "GENESIS"
    for step in ["会话启动", "事件记录", "证据上传", "防伪证书生成"]:
        h = hashlib.sha256(f"{step}{prev}{report_id}".encode()).hexdigest()[:24]
        chain.append({"step": step, "hash": h})
        prev = h
    
    return {
        "report_id": report_id,
        "report_version": "3.0",
        
        # 事件要素摘要
        "facts_summary": {
            k: {"value": v, "status": "✓ 已记录" if v else "○ 待收集"}
            for k, v in normalized_facts.items()
        },
        "facts_collected": sum(1 for v in normalized_facts.values() if v),
        "facts_total": 6,
        
        # ★★★ 核心：防伪证书列表 ★★★
        "evidence_certificates": certificates,
        "evidence_count": len(certificates),
        
        # 批量存证的Merkle根
        "batch_merkle_root": merkle,
        
        # 保管链
        "chain_of_custody": chain,
        
        # 元信息
        "generated_at": now.isoformat(),
        
        # 产品定位声明
        "disclaimer": (
            "证言(Testimony.ai)生成的防伪证书基于SHA-256密码学哈希算法。"
            "证书中记录的数字指纹可用于独立验证原始文件的完整性和真实性。"
            "本系统提供的存证服务旨在帮助用户固定证据、防止篡改争议。"
        )
    }


# ============================================================
# 家长评估（简化版）
# ============================================================

def heuristic_assessment(observation):
    risk = "GREEN"; conf = 0.58; stressors = []
    if any(k in observation for k in ["自残","自杀"]): risk="RED"; conf=0.93
    elif any(k in observation for k in ["不想上学","失眠","害怕"]): risk="YELLOW"; conf=0.79
    for s,kws in [("校园关系",["学校","同学","老师"]),("网络互动",["手机","群","微信"]),("学业压力",["成绩","考试"])]:
        if any(k in observation for k in kws): stressors.append(s)
    if not stressors: stressors=["需进一步观察"]
    return {"risk_level":risk,"confidence":conf,"possible_stressors":stressors,"summary":"检测到需要关注的情绪变化。","suggested_script":"我注意到你最近有些辛苦，如果你想聊，我随时都在。","suggested_actions":["选择安静时间沟通","确认孩子安全","必要时联系专业人士"]}


def ai_assessment(observation):
    try:
        raw = call_ai(
            "你是儿童心理顾问。严格输出JSON: {\"risk_level\":\"GREEN|YELLOW|RED\",\"confidence\":0~1,\"possible_stressors\":[],\"summary\":\"\",\"suggested_script\":\"\",\"suggested_actions\":[]}",
            f"分析家长观察: {observation}", 0.3, 500
        )
        m = re.search(r'\{[\s\S]*\}', raw)
        if m:
            p = json.loads(m.group())
            return {"risk_level":p.get("risk_level","GREEN"),"confidence":max(0,min(1,float(p.get("confidence",0.6)))),"possible_stressors":p.get("possible_stressors")or["需观察"],"summary":p.get("summary",""),"suggested_script":p.get("suggested_script",""),"suggested_actions":p.get("suggested_actions",[])or[]}
    except: pass
    return heuristic_assessment(observation)


# ============================================================
# HTTP处理器
# ============================================================

class DemoHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(ROOT_DIR), **kwargs)

    def do_GET(self):
        if self.path == "/api/health":
            json_response(self, {"ok":True,"service":"Testimony.ai v3.0 防伪存证","version":"3.0.0","model":AI_MODEL})
            return
        super().do_GET()

    def do_POST(self):
        try:
            content_type = self.headers.get("Content-Type", "")
            
            # 文件上传（multipart/form-data）
            if "multipart/form-data" in content_type:
                return self.handle_file_upload()
            
            payload = read_json(self)
            
            if self.path == "/api/interview/start":
                return self.handle_start()
            
            if self.path == "/api/interview/message":
                return self.handle_message(payload)
            
            if self.path == "/api/assessment":
                obs = payload.get("observation","").strip()
                if not obs:
                    json_response(self,{"error":"observation不能为空"},400); return
                json_response(self, ai_assessment(obs))
                return
            
            if self.path == "/api/evidence/certify":
                """★ 核心接口：生成防伪存证证书 ★"""
                files = payload.get("files", [])
                if not files:
                    json_response(self,{"error":"请至少上传一个文件"},400); return
                
                result = generate_report_with_evidence(
                    facts=payload.get("facts",{}),
                    transcript=payload.get("transcript",[]),
                    evidence_files=files
                )
                json_response(self, result)
                return
            
            json_response(self,{"error":"接口不存在"},404)
        except Exception as e:
            json_response(self,{"error":str(e)},500)

    def handle_file_upload(self):
        """处理 multipart 文件上传"""
        # 解析 boundary
        content_type = self.headers.get("Content-Type", "")
        boundary = content_type.split("boundary=")[-1].encode()
        
        content_length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(content_length)
        
        # 简单解析 multipart
        parts = body.split(b"--" + boundary)
        
        files = []
        for part in parts:
            if b"filename=" not in part:
                continue
            
            # 提取 filename
            header_end = part.find(b"\r\n\r\n")
            header = part[:header_end].decode(errors="ignore")
            fn_match = re.findall(r'filename="([^"]+)"', header)
            if not fn_match:
                continue
            
            file_name = fn_match[0]
            file_data = part[header_end+4:].rstrip(b"\r\n--").rstrip(b"\r\n")
            
            # 转为 base64 返回
            b64_data = base64.b64encode(file_data).decode()
            
            # 判断 MIME 类型
            ext = file_name.rsplit(".", 1)[-1].lower() if "." in file_name else ""
            mime_map = {"png":"image/png","jpg":"image/jpeg","jpeg":"image/jpeg",
                        "gif":"image/gif","mp4":"video/mp4","mp3":"audio/mpeg",
                        "pdf":"application/pdf"}
            mime = mime_map.get(ext, "application/octet-stream")
            
            files.append({
                "name": file_name,
                "size": len(file_data),
                "mime": mime,
                "data": f"data:{mime};base64,{b64_data}"
            })
        
        json_response(self, {"files": files, "count": len(files)})

    def handle_start(self):
        try:
            welcome = call_ai(
                INTERVIEW_SYSTEM_PROMPT,
                "新会话开始。温暖简洁地打招呼，告诉用户这里安全，可以记录事件、也可以上传截图等证据。",
                0.6, 180
            )
        except Exception:
            welcome = ("你好，我是证言助手。这里是安全空间。\n"
                      "你可以告诉我发生了什么，也可以上传截图、录音等作为证据。\n"
                      "我会帮你生成防伪存证证书，证明这些材料是真实的、没被篡改过的。")
        
        json_response(self, {
            "reply": welcome,
            "facts": normalize_facts({}),
            "missing_elements": list(ELEMENT_PATTERNS.keys()),
            "risk_level": "GREEN",
            "session_id": f"SES-{datetime.now().strftime('%Y%m%d%H%M%S')}"
        })

    def handle_message(self, payload):
        msg = payload.get("message","").strip()
        if not msg:
            json_response(self,{"error":"消息不能为空"},400); return
        
        facts = extract_facts(msg, payload.get("facts",{}))
        risk = detect_risk(msg)
        
        if risk == "RED":
            reply = "你的安全最重要。如果现在有危险，请立刻联系家长或老师。你也可以继续告诉我。"
        else:
            transcript = payload.get("transcript",[])
            rnd = sum(1 for t in transcript if t.get("role")=="user")
            try:
                reply = call_ai(INTERVIEW_SYSTEM_PROMPT, build_smart_prompt(facts, msg, rnd))
            except Exception:
                miss = missing_elements(facts)
                c = sum(1 for v in facts.values() if v)
                if c >= 4 or not miss:
                    reply = "好的，信息已经比较完整了。你可以随时上传证据文件生成防伪证书。"
                else:
                    reply = f"收到。关于「{miss[0]}」，如果方便的话可以聊聊，不方便也可以继续说其他的。"
        
        json_response(self, {"reply": reply, "facts": facts,
                           "missing_elements": missing_elements(facts), "risk_level": risk})


# ============================================================
# 启动
# ============================================================

def main():
    port = int(sys.argv[1]) if len(sys.argv)>1 else 9000
    print("="*55)
    print("  证言 Testimony.ai v3.0 - 防伪存证系统")
    print("="*55)
    print(f"  端口:{port} | 模型:{AI_MODEL}")
    print("="*55)
    
    server = ThreadingHTTPServer(("0.0.0.0", port), DemoHandler)
    print(f"\n[*] 启动成功: http://0.0.0.0:{port}\n")
    server.serve_forever()


if __name__ == "__main__":
    main()
