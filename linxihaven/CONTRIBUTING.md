# 贡献指南

感谢您对 LinxiHaven 项目的兴趣！我们欢迎各种形式的贡献，包括但不限于代码、文档、Bug报告和功能建议。

---

## 📋 目录

- [行为准则](#行为准则)
- [如何贡献](#如何贡献)
- [开发环境设置](#开发环境设置)
- [代码规范](#代码规范)
- [提交规范](#提交规范)
- [Pull Request 流程](#pull-request-流程)

---

## 行为准则

本项目采用 [Contributor Covenant](https://www.contributor-covenant.org/) 行为准则。我们期望所有参与者都能保持友好、包容和尊重的态度。

### 我们的承诺

作为贡献者，我们承诺为所有用户提供一个无骚扰的体验，无论年龄、身体尺寸、残疾、种族、性别特征、经验水平、教育程度、社会经济地位、国籍、个人外表、种族、宗教或性取向。

---

## 如何贡献

### 🐛 报告 Bug

1. 在提交 Bug 报告之前，请先搜索 [已有的 Issues](https://www.synnovator.com/linxi/LinxiHaven/issues)
2. 如果没有找到相同的 Bug，请创建新的 Issue
3. 使用 Bug 报告模板，并提供以下信息：
   - 清晰的标题和描述
   - 复现步骤
   - 预期行为 vs 实际行为
   - 截图（如果适用）
   - 环境信息（Android 版本、设备型号等）

### 💡 提出功能建议

1. 搜索已有的功能请求
2. 详细描述您希望的功能
3. 解释为什么这个功能对项目有价值
4. 提供可能的实现方案（可选）

### 📝 改进文档

文档改进是非常受欢迎的贡献！如果您发现：
- 文档缺失或过时
- 拼写错误或语法问题
- 示例代码不工作
- 解释不够清晰

请随时提交 PR 或 Issue。

---

## 开发环境设置

### 前置要求

- Android Studio Hedgehog (2023.1.1) 或更高
- JDK 17+
- Android SDK API 26+

### 克隆项目

```bash
git clone https://www.synnovator.com/linxi/LinxiHaven.git
cd LinxiHaven
```

### 打开项目

1. 启动 Android Studio
2. 选择 File -> Open
3. 选择 `LinxiHaven` 目录
4. 等待 Gradle 同步完成

### 运行测试

```bash
# 运行单元测试
./gradlew test

# 运行 lint 检查
./gradlew lint

# 构建 Debug APK
./gradlew assembleDebug
```

---

## 代码规范

### Kotlin 编码规范

本项目遵循 [Kotlin 官方编码约定](https://kotlinlang.org/docs/coding-conventions.html)，并使用以下额外规则：

#### 命名规范

```kotlin
// 类名使用 PascalCase
class EvidencePackageGenerator

// 函数和变量使用 camelCase
fun generateEvidencePackage()
val encryptedContent

// 常量使用 UPPER_SNAKE_CASE
const val MAX_RETRY_COUNT = 3

// 包名全部小写
package com.testimony.core.evidence
```

#### 文档注释

```kotlin
/**
 * 生成证据包的类
 *
 * @param config 证据包配置
 * @return 生成的证据包
 * @throws EvidenceException 当生成失败时抛出
 */
fun generate(config: EvidenceConfig): EvidencePackage
```

#### 权限注解

```kotlin
@RequiresPermission(Manifest.permission.CAMERA)
fun captureEvidence(): ByteArray

@WorkerThread
fun processInBackground()
```

### 项目结构

```
com/testimony/
├── core/                    # 核心功能
│   ├── evidence/           # 证据相关
│   ├── security/           # 安全相关
│   └── timestamp/         # 时间戳相关
├── ai/                     # AI 模块
├── ui/                     # UI 层
│   ├── parent/            # 家长端
│   └── student/          # 学生端
└── util/                  # 工具类
```

---

## 提交规范

### 提交信息格式

```
<类型>: <简短描述>

[可选的详细说明]

[可选的关联 Issue]
```

### 类型标签

| 标签 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档更改 |
| `style` | 代码格式调整（不影响功能） |
| `refactor` | 代码重构 |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `chore` | 构建或辅助工具变动 |

### 示例

```
feat(evidence): 添加证据包压缩功能

- 使用 ZIP 格式压缩证据文件
- 支持选择性压缩多媒体文件
- 添加压缩级别配置选项

Closes #123
```

```
fix(security): 修复加密密钥缓存问题

当应用进入后台时清除敏感数据缓存
Related to issue #89
```

---

## Pull Request 流程

### 创建 PR

1. 从 `main` 分支创建新分支
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. 进行更改并提交
   ```bash
   git add .
   git commit -m "feat: Add new feature"
   ```

3. 推送分支
   ```bash
   git push origin feature/your-feature-name
   ```

4. 在 Git 平台上创建 Pull Request

### PR 模板

```markdown
## 描述
<!-- 简要描述您的更改 -->

## 更改类型
- [ ] Bug 修复
- [ ] 新功能
- [ ] 文档更新
- [ ] 代码重构

## 测试
<!-- 描述您如何测试这些更改 -->
- [ ] 我已添加测试
- [ ] 手动测试通过

## 截图（如果适用）
<!-- 添加相关截图 -->

## 检查清单
- [ ] 我的代码遵循项目的代码规范
- [ ] 我进行了自检
- [ ] 我更新了相关文档
```

### 代码审查

所有 PR 需要通过：
1. ✅ 代码风格检查
2. ✅ 单元测试
3. ✅ 至少一个维护者的审查

---

## 📞 获取帮助

如果您在贡献过程中遇到问题：

1. 查看 [README](README.md) 和 [Wiki](https://www.synnovator.com/linxi/LinxiHaven/wiki)
2. 搜索 [已有的 Issues](https://www.synnovator.com/linxi/LinxiHaven/issues)
3. 创建新的 Issue 并添加 `question` 标签

---

再次感谢您的贡献！🎉
