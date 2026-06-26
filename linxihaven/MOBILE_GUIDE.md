# 📱 证言 Testimony.ai - Android 移动端开发指南

> **密码学级防伪存证系统 v3.0**

---

## 🏗️ 项目结构

```
linxihaven/testimony/
├── app/
│   ├── build.gradle.kts          # 应用构建配置
│   └── src/main/
│       ├── AndroidManifest.xml    # 应用清单
│       ├── java/com/testimony/
│       │   ├── MainActivity.kt    # 主入口 + 导航控制器
│       │   ├── TestimonyApplication.kt
│       │   │
│       │   ├── core/evidence/     # ★★★ 核心引擎 ★★★
│       │   │   ├── AntiForgeryEngine.kt      # 防伪存证引擎 (SHA-256)
│       │   │   ├── EvidencePackageGenerator.kt  # 证据包生成器
│       │   │   └── ScreenRecorderService.kt     # 录屏服务
│       │   │
│       │   ├── ui/student/         # 学生端界面
│       │   │   ├── StudentHomeScreen.kt        # 首页（含防伪入口）
│       │   │   ├── AntiForgeryScreen.kt        # ★ 防伪存证主界面
│       │   │   ├── CertificateDetailScreen.kt  # ★ 证书详情展示
│       │   │   ├── GuidedInterviewScreen.kt    # AI 引导访谈
│       │   │   ├── SecuritySpaceScreen.kt      # 安全区
│       │   │   └── EvidencePreviewScreen.kt    # 证据预览
│       │   │
│       │   └── ui/theme/           # 主题配置
│       │       └── Theme.kt                    # 颜色/样式定义
│       │
│       └── res/                   # 资源文件
│           ├── values/strings.xml
│           └── values/colors.xml
│
├── build.gradle.kts              # 项目级构建配置
├── settings.gradle.kts           # 项目设置
├── HashUtils.kt                 # 哈希工具类
├── IncrementalMerkleTree.kt     # Merkle树实现
├── TimeAnchorProvider.kt        # 时间锚定提供者
└── ChainOfCustody.kt            # 证据保管链
```

---

## 🔐 防伪存证工作流

### 用户操作流程

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   学生首页       │────▶│   防伪存证界面   │────▶│   证书详情页     │
│                 │     │                 │     │                 │
│ [🔐 防伪存证]   │     │ 1. 选择/拍照    │     │ 1. 数字指纹展示  │
│                 │     │ 2. 上传截图/录音 │     │ 2. 时间锚定列表  │
│ 其他功能：       │     │ 3. 点击生成     │     │ 3. 验证步骤指南  │
│ - 安全空间       │     │                 │     │ 4. 复制/分享/导出 │
│ - AI 引导访谈    │     │ [🔒 生成证书]   │     │                 │
│ - 证据包管理     │     └─────────────────┘     └─────────────────┘
└─────────────────┘
```

### 导航路由

```kotlin
// MainActivity.kt 中定义的路由

student_home → anti_forgery → certificate_detail
     │              ↑                      ↑
     │         AntiForgeryScreen    CertificateDetailScreen
     │         (文件选择+生成)      (证书展示+验证指南)
     │
     ├─→ security_space     (安全空间)
     ├─→ guided_interview   (AI引导)
     └─→ evidence_preview   (证据预览)
```

---

## 🚀 打包 APK

### 方式一：GitHub Actions（推荐）

**触发自动构建：**
- 推送代码到 `main` 分支
- 创建 tag: `v3.0.0`（会自动创建 Release）

**手动触发：**
1. 进入 GitHub 仓库的 Actions 页面
2. 选择 "Build Android APK"
3. 选择 `debug` 或 `release`
4. 运行工作流

**获取产物：**
- Actions → 对应运行 → Artifacts → 下载 APK/AAB

### 方式二：本地命令行

```bash
cd linxihaven

# Debug 版本（测试用）
./gradlew assembleDebug

# 输出路径: testimony/build/outputs/apk/debug/app-debug.apk

# Release 版本（需签名密钥）
./gradlew assembleRelease

# AAB 格式（提交 Google Play）
./gradlew bundleRelease
```

### 方式三：Android Studio

1. File → Open → 选择 `linxihaven` 目录
2. 等待 Gradle 同步完成
3. Build → Generate Signed Bundle / APK...
4. 选择 release 或 debug 变体
5. 完成！

---

## 🔑 签名配置（Release）

在 GitHub Secrets 中添加：

| Secret 名称 | 说明 |
|-------------|------|
| `KEYSTORE_BASE64` | Base64 编码的 `.jks` 密钥文件 |
| `KEYSTORE_PASSWORD` | Keystore 密码 |
| `KEY_ALIAS` | 密钥别名 |
| `KEY_PASSWORD` | 密钥密码 |

**生成签名密钥：**
```bash
keytool -genkeypair \
  -v \
  -keystore testimony-release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias testimony-key

# 转为 base64（用于 GitHub Secrets）
base64 -w 0 testimony-release.jks
```

---

## 📋 核心功能清单

### ✅ 已实现

| 功能 | 文件 | 状态 |
|------|------|------|
| SHA-256 指纹计算 | `AntiForgeryEngine.kt` | ✓ |
| 本地哈希（不上传服务器） | 同上 | ✓ |
| 多源时间锚定 | 同上 | ✓ |
| 防伪证书数据模型 | 同上 | ✓ |
| 批量证书生成 | 同上 | ✓ |
| Merkle根哈希 | `IncrementalMerkleTree.kt` | ✓ |
| 相册选择文件 | `AntiForgeryScreen.kt` | ✓ |
| 拍照取证 | 同上 | ✓ |
| 已选文件列表展示 | 同上 | ✓ |
| 证书头部（渐变） | `CertificateDetailScreen.kt` | ✓ |
| **数字指纹深色展示区** | 同上 | ✓ |
| 可视化验证码 | 同上 | ✓ |
| 时间锚定卡片 | 同上 | ✓ |
| 详细验证步骤指南 | 同上 | ✓ |
| 多平台命令示例 | 同上 | ✓ |
| 一键复制指纹 | 同上 | ✓ |
| 分享/导出按钮 | 同上 | ✓ |
| 学生首页防伪入口 | `StudentHomeScreen.kt` | ✓ |
| 导航路由集成 | `MainActivity.kt` | ✓ |

---

## 🎨 UI 设计规范

### 配色方案

```
主色调 (Primary):     #4F46E5 (靛蓝紫)
渐变色:               #4F46E5 → #7C3AED → #A855F7
数字指纹背景:         #1E1B4B (深靛色)
成功/验证通过:       #059669 (翠绿)
警告:                #D97706 (琥珀)
危险:                #DC2626 (红色)

字体:
- 哈希值: Courier New (等宽)
- 标题: 系统默认 Bold
- 正文: 系统默认 Regular
```

### 关键组件尺寸

```
圆角半径:
- 卡片: 16dp ~ 18dp
- 按钮: 12dp ~ 14dp
- 上传区域: 18dp
- 数字指纹框: 18dp

间距:
- 卡片内边距: 16dp ~ 24dp
- 元素间距: 12dp ~ 20dp

按钮高度:
- 主按钮: 50dp ~ 56dp
- 次要按钮: 48dp
```

---

## 🔧 开发注意事项

### 依赖版本

```kotlin
// build.gradle.kts
composeBom = "2024.xx.xx"
kotlin = "1.9.x"
compileSdk = 34
minSdk = 24
targetSdk = 34
```

### 权限需求

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />           <!-- 拍照 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" /> <!-- 相册 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" /> <!-- 保存证书 -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />         <!-- 录音取证 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />  <!-- 后台服务 -->
```

### 性能优化建议

1. **大文件处理**: 使用协程 + Dispatchers.IO 计算 SHA-256，避免阻塞 UI
2. **图片缩略图**: 使用 Coil 加载缩略图，避免 OOM
3. **证书缓存**: 生成的证书可缓存到本地数据库（SQLCipher）
4. **批量上传**: 支持多选文件时显示进度条

---

## 📞 技术支持

遇到问题？检查以下几点：

1. **Gradle 同步失败** → 清理缓存 `./gradlew clean`
2. **Compose 版本冲突** → 统一使用 BOM 管理
3. **签名问题** → 确保 keystore.jks 存在且密码正确
4. **API 34 兼容性** → 检查权限请求方式是否适配 Android 14

---

<div align="center">

**🔐 证言 Testimony.ai v3.0**

*让每一个声音都被安全地记录*

</div>
