# LinxiHaven 代码规范配置

## 概述

本目录包含 LinxiHaven 项目的代码规范配置文件，用于确保代码质量和一致性。

## 配置文件说明

| 文件 | 说明 |
|------|------|
| .editorconfig | 编辑器统一配置（缩进、换行等） |
| detekt.yml | Detekt 静态代码分析配置 |
| ktlint.gradle.kts | Ktlint 代码风格检查配置 |

## 使用方法

### Ktlint 检查

\\\ash
./gradlew ktlintCheck
\\\

### Detekt 分析

\\\ash
./gradlew detekt
\\\

### 修复自动可修复的问题

\\\ash
./gradlew ktlintFormat
\\\

## 代码规范要点

1. **命名规范**: 使用有意义的英文命名，避免缩写
2. **注释规范**: 公共API必须添加KDoc注释
3. **空安全**: 优先使用空安全操作符
4. **Coroutines**: 使用结构化并发
5. **异常处理**: 不捕获通用异常

## 评分标准

| 指标 | 目标值 |
|------|--------|
| 代码复杂度 | ≤ 15/方法 |
| 方法长度 | ≤ 50行 |
| 文件长度 | ≤ 500行 |
| 嵌套深度 | ≤ 3层 |
| 注释覆盖率 | ≥ 30% |

---
Generated: 2026-05-16