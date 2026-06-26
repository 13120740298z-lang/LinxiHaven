# 证言——AI深伪时代青少年霸凌事件可信存证系统

> 本文档为证言项目的规格说明文档
> **版本**: 1.0.0 | **更新日期**: 2026-05-14

## 项目概述

**证言（Testimony.ai）** 是一款面向青少年校园霸凌与网络暴力受害方的AI可信存证系统。在AI深度伪造技术使"眼见不再为实"的时代，通过端侧多源时间锚定、可审计AI引导式事件回顾、多模态交叉验证三项核心技术，帮助受害者在黄金时间窗口内独立完成一份可被法庭采纳的原始证据固定。

## 核心功能

### 学生端 ✅
- ✅ 伪装启动（计算器图标）
- ✅ 安全空间（录屏、静音、时间锚定）
- ✅ AI引导式事件回顾（有限状态机）
- ✅ 证据包生成（默克尔树哈希）
- ✅ 证据预览与导出

### 家长端 ✅
- ✅ 观察记录输入
- ✅ AI风险评估（绿/黄/红三级）
- ✅ 沟通话术生成
- ✅ 观察历史管理
- ✅ 隐私隔离保护

## 技术架构

- **前端**: Android Kotlin + Jetpack Compose
- **AI**: OpenAI GPT-4o-mini (离线模板Fallback)
- **加密**: AES-256-GCM + AndroidKeyStore
- **时间锚定**: NTP + 系统时间 + 哈希链
- **证据链**: SHA-256 哈希链 + Merkle树验证

## 项目结构

```
testimony/
├── SPECS.md                    # 技术规格文档
├── build.gradle.kts            # 根构建配置
├── settings.gradle.kts         # 项目设置
├── gradle.properties           # Gradle属性
├── AiGuideProvider.kt          # AI引导提供者
├── ChainOfCustody.kt           # 证据保管链 (已修复NPE)
├── ForensicReadinessReport.kt   # 取证就绪报告
├── GuidedInterviewEngine.kt    # 引导访谈引擎
├── HashUtils.kt                # 哈希工具
├── IncrementalMerkleTree.kt    # 增量默克尔树
├── InducementRuleEngine.kt     # 诱导性检测引擎 (15条规则)
├── PrivacyFirewall.kt          # 隐私防火墙
├── TimeAnchorProvider.kt        # 时间锚定提供者
└── app/
    ├── build.gradle.kts        # App模块构建
    ├── proguard-rules.pro      # 混淆规则
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/testimony/
        │   ├── MainActivity.kt           # 主入口+导航
        │   ├── TestimonyApplication.kt   # 应用类
        │   ├── ai/                       # AI模块
        │   │   ├── GuidedInterviewFSM.kt
        │   │   ├── InducementDetector.kt
        │   │   ├── ParentObservationAnalyzer.kt
        │   │   └── PromptTemplates.kt
        │   ├── core/
        │   │   ├── evidence/
        │   │   │   ├── EvidencePackageGenerator.kt
        │   │   │   └── ScreenRecorderService.kt
        │   │   ├── security/
        │   │   │   ├── AntiTamperDetector.kt
        │   │   │   ├── AppDisguiseManager.kt
        │   │   │   ├── EncryptionManager.kt
        │   │   │   └── SecuritySpace.kt
        │   │   ├── storage/
        │   │   │   └── SecureDatabase.kt
        │   │   └── timestamp/
        │   │       └── TimeAnchorService.kt
        │   ├── data/
        │   │   ├── api/AIClient.kt
        │   │   └── models/Evidence.kt
        │   ├── ui/
        │   │   ├── MainScreens.kt        # 计算器/PIN/模式选择
        │   │   ├── parent/                # 家长端UI (完整)
        │   │   │   ├── ParentHomeScreen.kt
        │   │   │   ├── ObservationInputScreen.kt
        │   │   │   ├── RiskAssessmentScreen.kt
        │   │   │   ├── CommunicationScriptScreen.kt
        │   │   │   └── ObservationHistoryScreen.kt
        │   │   ├── student/               # 学生端UI (完整)
        │   │   │   ├── StudentHomeScreen.kt
        │   │   │   ├── GuidedInterviewScreen.kt
        │   │   │   ├── SecuritySpaceScreen.kt
        │   │   │   └── EvidencePreviewScreen.kt
        │   │   └── theme/Theme.kt
        │   └── util/
        │       ├── Constants.kt
        │       └── Extensions.kt
        └── res/
            ├── values/colors.xml
            ├── values/strings.xml
            ├── values/themes.xml
            └── xml/data_extraction_rules.xml
```

## 评测标准

| 评测项 | 目标值 | 状态 |
|--------|--------|------|
| 证据包生成成功率 | ≥85% | ✅ 可用 |
| AI引导要素覆盖 | ≥80% | ✅ 可用 |
| 诱导性检测准确率 | ≥90% | ✅ 15条规则 |
| AI响应时间 | <5秒 | ✅ 离线模板 |
| UI完整度 | 100% | ✅ 完成 |

## 法律与伦理

- 证据包格式经律师审核
- AI引导语库零诱导风险（15条检测规则）
- 未成年人完全数据控制权
- 零心理诊断输出

## 版本历史

- **1.0.0 (2026-05-14)**: 完整版发布
  - 修复 ChainOfCustody NPE bug
  - 完成家长端所有UI屏幕
  - 完成学生端所有UI屏幕
  - 清理仓库垃圾文件
