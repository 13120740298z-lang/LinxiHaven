# 证言 Testimony.ai - 决赛路演版

> **上海市青少年科技创新大赛 · 决赛路演项目**
>
> 基于创伤知情原则的 AI 引导式校园霸凌可信存证系统

---

## 🎯 项目简介

**证言 (Testimony.ai)** 是一套面向未成年人的校园霸凌事件智能存证解决方案。
系统采用**创伤知情三阶段引导协议**（稳定化 → 信息收集 → 赋能），
通过 AI 引导助手以温和、非诱导的方式帮助受害者记录事件，
并生成符合**司法取证标准**的证据链文档。

## ✨ 核心特性

### 🤖 AI 引擎（七牛云 MaaS 深度集成）
- **大模型**: DeepSeek-V3 / 通义千问 Plus
- **三阶段引导协议**: 创伤知情专业话术
- **零诱导检测**: 15 条司法语言学规则
- **优雅降级**: AI 不可用时自动切换启发式引擎

### 📊 多角色路演闭环
| 角色 | 功能 | 技术实现 |
|------|------|----------|
| 学生访客 | AI 引导式事件记录 | 三阶段访谈协议 |
| 家长/监护人 | 观察行为风险评估 | AI + 规则双引擎 |
| 评委/管理员 | 证据包预览与验证 | Merkle 哈希链 |

### 🔒 司法级证据包引擎
```
┌─────────────────────────────────────┐
│         证言证据包 v2.0              │
├─────────────────────────────────────┤
│ 🆔 证据编号    EVD-YYYYMMDD-HHMMSS  │
│ 🔐 Merkle根   SHA256(64位)         │
│ 📦 包体摘要    SHA256(64位)         │
│                                     │
│ ⏰ 多源时间锚定:                     │
│   ├─ 系统本地时间                    │
│   ├─ UTC 协调世界时                   │
│   ├─ 服务器主机名                    │
│   └─ Unix 时间戳                     │
│                                     │
│ 🔗 保管链 (Chain of Custody):        │
│   ├─ SESSION_INIT ✓                 │
│   ├─ INTERVIEW_COMPLETE ✓           │
│   ├─ FACTS_EXTRACTED ✓             │
│   └─ EVIDENCE_PACKAGED ✓            │
└─────────────────────────────────────┘
```

### 🛡️ 安全机制
- **沙箱隔离**: 访客模式数据不写入核心数据库
- **风险实时监控**: RED/YELLOW/GREEN 三级预警
- **XSS 防护**: 全局 HTML 转义
- **API 密钥安全**: 环境变量优先 + 内置兜底

## 🚀 快速开始

### 本地开发
```bash
cd web-demo

# 安装依赖（仅需 Python 3.8+）
# 无需额外 pip 安装，使用标准库

# 启动服务
python server.py 9000

# 浏览器访问
# http://localhost:9000
```

### 部署到 ECS 服务器

#### 方式一：自动部署脚本
```bash
# 设置环境变量
$env:ECS_HOST="220.154.134.151"
$env:ECS_PASSWORD="Testimony#2026Ops!"
$env:QINIU_API_KEY="sk-f6438ea6f506a07b009e4a950aae88be2af3256605ee475ee0f78258e5e586aa"

# 执行部署
python deploy.py
```

#### 方式二：手动部署
```bash
# 1. 上传文件到服务器
scp -r web-demo/* root@220.154.134.151:/opt/linxihaven_demo/

# 2. SSH 到服务器
ssh root@220.154.134.151

# 3. 配置环境变量并启动
cd /opt/linxihaven_demo
export QINIU_API_KEY="sk-f6438ea6f506a07b009e4a950aae88be2af3256605ee475ee0f78258e5e586aa"
nohup python3 server.py 9000 > server.log 2>&1 &

# 4. 验证服务
curl http://localhost:9000/api/health
```

## 🌐 路演访问地址

**正式环境**: http://220.154.134.151:9000

> 可将此链接生成二维码贴载到展板和演示 PPT 中

## 📁 项目结构

```
web-demo/
├── index.html          # 主页面（现代化 UI）
├── app.js               # 前端交互引擎
├── server.py            # 后端 API 服务
└── README.md            # 项目文档

deploy.py                # 一键部署脚本
```

## 🔧 API 接口文档

### 健康检查
```
GET /api/health
→ { ok, service, version, model, api_status, features[] }
```

### 访谈接口
```
POST /api/interview/start
→ { reply, facts{}, missing_elements[], risk_level, session_id }

POST /api/interview/message
Body: { message, transcript[], facts{} }
→ { reply, facts{}, missing_elements[], risk_level }
```

### 家长评估
```
POST /api/assessment
Body: { observation }
→ { risk_level, confidence, possible_stressors[], summary,
     suggested_script, suggested_actions[] }
```

### 证据包生成
```
POST /api/evidence
Body: { facts{}, transcript[] }
→ { evidence_id, merkle_root, package_hash,
     timestamp_sources[], chain_entries[], ... }
```

## 💡 使用说明

### 学生访客体验流程
1. 打开页面后自动进入 AI 引导访谈
2. 按提示描述事件情况（时间、地点、人物等）
3. 系统自动提取六要素并温和引导补充缺失信息
4. 要素收集完成后可生成证据包预览

### 家长观察分析
1. 切换至「家长观察分析」标签页
2. 输入孩子近期的行为变化观察
3. 点击「生成风险评估报告」获取专业建议
4. 包含风险等级、压力源识别、沟通话术、行动建议

## 🏆 技术亮点

| 技术 | 应用场景 |
|------|----------|
| **创伤知情原则** | 引导话术设计、安全优先逻辑 |
| **Merkle 树哈希** | 证据完整性验证、防篡改 |
| **多源时间锚定** | 司法级时间可信度证明 |
| **Chain of Custody** | 证据保管链完整追踪 |
| **AI 双引擎** | 大模型 + 启发式降级方案 |
| **响应式设计** | 移动端/桌面端完美适配 |

## 📄 开源协议

本项目参加上海市青少年科技创新大赛，代码仅供学习交流使用。

---

<div align="center">

**🎉 证言 Testimony.ai - 让每一个声音都被听见**

*Build with ❤️ for Shanghai Youth Innovation Competition*

</div>
