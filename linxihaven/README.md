# LinxiHaven - 青少年霸凌可信存证系统

<div align="center">

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple)](build.gradle.kts)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](app/build.gradle.kts)

**基于《证言（Testimony.ai）》理念的青少年霸凌证据收集与存证解决方案**

</div>

---

## 📖 项目简介

LinxiHaven 是一款面向青少年的**霸凌证据存证应用**，帮助用户（学生、家长、教育工作者）在遭遇校园霸凌时，通过安全、可靠的方式收集和保存证据。

### 核心价值

- **可信存证** - 证据经过加密和哈希锚定，难以被篡改
- **隐私保护** - 数据采用AES-256-GCM加密，仅用户可访问
- **司法级安全** - 多源时间锚定、证据链完整、哈希链验证
- **创伤知情** - AI引导对话遵循创伤知情原则，避免二次伤害

---

## 🎯 核心功能

### 学生端
| 功能 | 说明 |
|------|------|
| 引导式访谈 | AI驱动的结构化证据收集，通过创伤知情对话引导用户描述事件 |
| 证据固定 | 截图、录音、录像等多媒体证据的安全存储 |
| 安全空间 | 隐蔽入口，保护用户隐私不被发现 |
| 加密存储 | 所有数据使用设备级加密保护 |

### 家长端
| 功能 | 说明 |
|------|------|
| 风险评估 | 基于观察数据生成风险等级和建议 |
| 证据查看 | 查看已收集的证据包 |
| 建议行动 | 提供专业的应对指导和资源链接 |

### 安全特性
- **多源时间锚定** - NTP时间同步 + GPS位置 + 基站信息交叉验证
- **哈希链验证** - 证据完整性可验证，防篡改
- **渐进式锁定** - 防暴力破解，异常访问自动锁定

---

## 🏗️ 技术架构

### 技术栈

```
📱 Android App
├── UI Layer        - Jetpack Compose (Material Design 3)
├── Domain Layer   - Use Cases / FSM State Machine
├── Data Layer     - Room Database / Encrypted SharedPreferences
└── Core Module
    ├── Evidence   - EvidencePackageGenerator, ChainOfCustody
    ├── Security   - EncryptionManager, AppDisguiseManager
    └── Timestamp  - TimeAnchorService (NTP + GPS + CellTower)
```

### 核心模块

| 模块 | 路径 | 功能 |
|------|------|------|
| `testimony/core/evidence` | 证据生成器、Merkle树验证、保管链 |
| `testimony/core/security` | AES-256-GCM加密、PBKDF2密钥派生、伪装模式 |
| `testimony/core/timestamp` | 四源时间锚定（系统/NTP/GPS/基站） |
| `testimony/ai` | 诱导性检测、引导访谈FSM、Prompt模板 |
| `testimony/ui` | Compose UI组件、家长端、学生端 |

---

## 🚀 快速开始

### 环境要求

| 工具 | 版本要求 |
|------|----------|
| Android Studio | Hedgehog (2023.1.1) 或更高 |
| JDK | 17+ |
| Kotlin | 1.9.22 |
| Android SDK | API 26+ (Android 8.0) |
| Gradle | 8.4+ |

### 构建步骤

```bash
# 1. Clone 项目
git clone https://www.synnovator.com/linxi/LinxiHaven.git
cd LinxiHaven

# 2. 打开 Android Studio
# File -> Open -> 选择 LinxiHaven 目录

# 3. 同步 Gradle
# Android Studio 会自动下载依赖

# 4. 运行应用
# Run -> Run 'app'
```

### 项目结构

```
LinxiHaven/
├── testimony/                    # 主要应用模块
│   └── app/src/main/java/com/testimony/
│       ├── MainActivity.kt       # 应用入口
│       ├── MainScreens.kt       # 导航屏幕
│       ├── ai/                   # AI相关模块
│       │   ├── GuidedInterviewFSM.kt      # 引导访谈状态机
│       │   ├── InducementDetector.kt     # 诱导性检测
│       │   └── PromptTemplates.kt        # 对话模板
│       ├── core/                # 核心功能
│       │   ├── evidence/        # 证据模块
│       │   ├── security/        # 安全模块
│       │   └── timestamp/       # 时间戳模块
│       └── ui/                  # UI层
│           ├── parent/          # 家长端界面
│           └── student/        # 学生端界面
├── build.gradle.kts             # 根构建配置
└── settings.gradle.kts          # 项目设置
```

---

## 🔒 安全机制

### 加密方案

| 层级 | 算法 | 用途 |
|------|------|------|
| 数据加密 | AES-256-GCM | 文件和数据库内容加密 |
| 密钥派生 | PBKDF2 (100,000次迭代) | 从密码生成加密密钥 |
| 密钥存储 | Android Keystore | 安全存储密钥材料 |
| 应用锁定 | 渐进式延迟 | 防暴力破解攻击 |

### 证据完整性

```
证据包结构:
├── evidence.zip          # 原始证据（压缩）
├── metadata.json        # 元数据（时间、位置、设备信息）
├── signatures.json      # 多源时间签名
└── merkle_proof.json    # Merkle树验证证明
```

---

## 📝 AI 伦理设计

### 创伤知情原则

本项目的AI引导对话设计遵循以下原则：

1. **非评判性** - 不对用户行为做出道德评判
2. **自主性** - 尊重用户的选择权，不强迫
3. **安全性** - 识别危险信号并提供资源
4. **隐私性** - 最小化数据收集

### 诱导性检测

系统内置6类诱导性检测规则：
- 引导性提问检测
- 情绪标签检测
- 极端化倾向检测
- 自我伤害关键词检测
- 威胁恐吓检测
- 虚假信息检测

---

## 📄 许可证

本项目采用 Apache License 2.0 开源许可证。

```
Copyright 2024 LinxiHaven Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 提交 Issue

- 使用清晰的标题描述问题
- 提供复现步骤（如适用）
- 标明问题类型（Bug / Feature / Documentation）

### 提交代码

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

## 📧 联系方式

- 仓库地址: https://www.synnovator.com/linxi/LinxiHaven
- 问题反馈: https://www.synnovator.com/linxi/LinxiHaven/issues

---

<div align="center">

**让每个声音都被听见，让每份证据都被守护。**

</div>
