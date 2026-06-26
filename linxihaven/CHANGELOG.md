# 更新日志

所有重要的项目更新都会记录在此文件中。格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

## [Unreleased]

### 新增
- 完整的项目文档 (README.md)
- 贡献指南 (CONTRIBUTING.md)
- Apache License 2.0 许可证

### 待完成
- [ ] 添加单元测试
- [ ] 配置 CI/CD 流程
- [ ] 添加应用截图
- [ ] 国际化支持 (i18n)

---

## [v0.2.0] - 2026-05-14

### 新增
- **证据预览界面** - EvidencePreviewScreen 组件
- **风险评估界面** - RiskAssessmentScreen 组件，支持参数化配置
- **时间锚定服务增强** - CellTower 时间源真实实现

### 修复
- RiskAssessmentScreen 硬编码示例数据问题
- TimeAnchorService CellTower 占位实现问题
- 代码 lint 错误

### 改进
- 观察要点和建议行动支持外部参数传入
- 多网络制式支持 (GSM/LTE/WCDMA/5G NR)

---

## [v0.1.0] - 2026-05-13

### 新增
- **核心证据模块**
  - EvidencePackageGenerator - 证据包生成器
  - ChainOfCustody - 证据保管链
  - IncrementalMerkleTree - 增量默克尔树

- **安全模块**
  - EncryptionManager - AES-256-GCM 加密
  - AppDisguiseManager - 应用伪装和渐进式锁定
  - SecureDatabase - 加密数据库

- **时间戳模块**
  - TimeAnchorService - 四源时间锚定 (系统/NTP/GPS/基站)

- **AI 模块**
  - GuidedInterviewFSM - 引导访谈状态机
  - InducementDetector - 诱导性检测引擎
  - PromptTemplates - 对话模板

- **UI 界面**
  - 学生端: 引导访谈、安全空间、历史记录
  - 家长端: 风险评估、证据查看、建议行动

### 技术特性
- 100% Kotlin 实现
- Jetpack Compose UI
- Material Design 3
- Android Keystore 集成
- PBKDF2 密钥派生 (100,000 次迭代)

---

## 旧版本

暂无早期版本记录。

---

## 版本规范

本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/) 规范：

- **主版本号**: 不兼容的 API 修改
- **次版本号**: 向后兼容的功能新增
- **修订号**: 向后兼容的问题修复

### 版本状态标签

| 标签 | 说明 |
|------|------|
| `Unreleased` | 未发布，正在开发中 |
| `Latest` | 最新稳定版本 |
| `Deprecated` | 已弃用 |

---

## 链接

- [发布页面](https://www.synnovator.com/linxi/LinxiHaven/releases)
- [问题追踪](https://www.synnovator.com/linxi/LinxiHaven/issues)
