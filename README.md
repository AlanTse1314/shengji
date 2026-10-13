<p align="center">
  <img src="applogo/app.png" alt="升级 Logo" width="120" height="120">
</p>

<h1 align="center">🃏 升级（Sheng Ji）</h1>

<p align="center">
  <strong>中国最流行的四人扑克，完整搬上 Android —— 单机打 AI，或同一 WiFi 零配置开房</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_7.0+-green?logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin_2.0-blue?logo=kotlin" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose-orange?logo=jetpackcompose" alt="UI">
  <img src="https://img.shields.io/badge/Architecture-MVVM-purple" alt="Architecture">
  <img src="https://img.shields.io/badge/License-Learning-blue" alt="License">
</p>

<p align="center">
  🎯 完整规则 · 🤖 四档 AI（含 Q-Learning 自我学习） · 📡 局域网零配置开房 · 🎨 Material 3 · 👴 老人友好
</p>

<p align="center">
  <a href="#-快速开始">快速开始</a> •
  <a href="#-功能特性">功能特性</a> •
  <a href="#-游戏规则">游戏规则</a> •
  <a href="#-技术架构">技术架构</a> •
  <a href="#-开发文档">开发文档</a>
</p>

---

## 📖 目录

- [1. 项目简介](#1-项目简介)
- [2. 功能特性](#2-功能特性)
- [3. 游戏规则](#3-游戏规则)
- [4. 快速开始](#4-快速开始)
- [5. 使用手册](#5-使用手册)
- [6. 技术架构](#6-技术架构)
- [7. 开发文档](#7-开发文档)
- [8. 测试报告](#8-测试报告)
- [9. 更新日志](#9-更新日志)
- [10. 许可证与致谢](#10-许可证与致谢)

---

## 1. 项目简介

### 1.1 什么是"升级"？

**升级**（又名"拖拉机"、"80分"、"炒地皮"）是中国最流行的四人扑克牌游戏之一。本项目将其完整搬上 Android 平台，采用现代 Android 开发技术栈，支持 **单机 AI 对战** 和 **局域网联机** 两种模式。

### 1.2 项目亮点

| 亮点 | 说明 |
|:-----|:-----|
| 🎯 **完整规则实现** | 108张牌、叫主反主、出牌校验、计分升级，完整还原经典玩法 |
| 🤖 **智能 AI 对手** | 基于启发式评估 + 蒙特卡洛模拟 + Q-Learning 自我学习，支持 4 个难度等级 |
| 📡 **局域网联机** | UDP 自动发现 + TCP 实时通信，零配置开房 |
| 🎨 **精美 UI 设计** | Material3 设计语言，飞牌动画、缩放渐入效果 |
| 🃏 **多种牌面款式** | 单车牌/姚记牌两种扑克牌样式可选 |
| 📱 **全设备适配** | 自动适配手机/平板各种尺寸，支持深色模式 |
| 🎛️ **丰富设置** | 背景主题/音效/字体大小/牌面大小/牌面款式/AI难度 |

### 1.3 适用场景

- 🎮 **休闲娱乐** - 想在手机上玩升级纸牌游戏的玩家
- 🤖 **牌技练习** - 想与 AI 对战提升牌技的用户
- 👥 **朋友聚会** - 局域网联机对战，无需联网
- 📚 **学习参考** - Android/Compose/MVVM 学习者的完整项目参考

---

## 2. 功能特性

### 2.1 单机模式 ✅

| 功能 | 说明 |
|:-----|:-----|
| 完整规则 | 108 张牌、叫主反主、出牌校验、计分升级 |
| AI 对战 | 1 人类 + 3 AI，支持首出/跟牌/叫主策略 |
| 记牌器 | AI 内置记牌器，模拟真实玩家记忆 |
| 换底牌 | 配对式换底牌（点击底牌再点击手牌一一配对） |
| 托管功能 | 中途可让 AI 接管，随时可接管回来 |
| 做庄逻辑 | 第一个亮牌者坐庄，庄队胜利队友接手，失败换闲家 |
| 飞牌动画 | 发牌时牌从中间飞向各玩家 |
| 出牌动画 | 牌出现时带缩放渐入效果 |
| 音效反馈 | 出牌/胜利/失败/无效操作均有音效 |
| 弹窗提示 | 亮主/反主/底牌/结算/退出确认弹窗 |
| AI 多难度 | 支持 EASY/MEDIUM/HARD/MASTER 四个难度等级 |
| AI 自我学习 | MASTER 难度支持 Q-Learning 在线学习 |

### 2.2 联机模式（局域网）✅

| 功能 | 说明 |
|:-----|:-----|
| UDP 自动发现 | 无需手动输入 IP，自动搜索附近房间 |
| TCP 实时通信 | 房主 = 服务端，加入者 = 客户端 |
| 老人友好设计 | 大字体、大白话权限引导，适合全年龄段 |
| 权限管理 | 网络权限动态申请，拒绝后可手动开启 |

### 2.3 设置功能 ✅

| 功能 | 说明 |
|:-----|:-----|
| 背景主题 | 6 种背景颜色主题（经典绿/深海蓝/神秘紫/中国红/暗夜黑/木纹棕） |
| 音效控制 | 开关音效 + 音量滑块调节 |
| 字体大小 | 0.7x - 2.0x 可调，设置页实时预览 |
| 牌面大小 | 0.7x - 2.0x 可调，所有卡牌/按钮同步缩放 |
| AI 难度 | 四个难度等级可选（新手/中级/高级/大师） |
| 牌面款式 | 两种扑克牌样式（单车牌/姚记牌） |

### 2.4 其他功能 ✅

| 功能 | 说明 |
|:-----|:-----|
| 对局历史 | Room 本地存储，随时回顾历史战绩 |
| Material3 主题 | 支持深色模式，视觉体验统一 |
| 屏幕自适应 | 手机/平板自动适配 |
| 存档功能 | 退出时可保存进度，下次继续 |
| 代码审查 | 完整的代码审查与测试报告 |

### 2.5 待开发功能 🚧

- [ ] 出牌回放功能
- [ ] 在线匹配（跨局域网）
- [ ] 自定义规则配置（底牌数、甩牌开关等）
- [ ] 国际化支持（英文界面）
- [ ] 更多牌面款式

---

## 3. 游戏规则

> 📋 完整规则详见 [RULES.md](RULES.md)

### 3.1 基本规则

| 项目 | 说明 |
|:-----|:-----|
| 玩家人数 | 4 人（两两对家为队友） |
| 牌数 | 2 副牌，共 108 张 |
| 手牌 | 每人 25 张，8 张底牌归庄家 |
| 目标 | 从 2 开始逐级升级，先打过 A 的队伍获胜 |

### 3.2 核心流程

```
发牌 → 亮主/反主 → 庄家换底牌 → 出牌（25轮） → 抠底 → 计分升级 → 换庄 → 下一局
```

### 3.3 牌型说明

| 牌型 | 说明 | 示例 |
|:-----|:-----|:-----|
| 单张 | 一张牌 | ♠A |
| 对子 | 同花色同点数两张 | ♠8♠8 |
| 拖拉机 | 同花色连续对子（跳过级牌） | ♠6♠6♠7♠7 |
| 甩牌 | 同花色多张组合 | ♠AKQ |

### 3.4 计分规则

| 闲家总得分 | 结果 |
|:-----------|:-----|
| 0 分 | 庄家连升 3 级（大光） |
| 1~39 分 | 庄家连升 2 级（小光） |
| 40~79 分 | 庄家升 1 级 |
| 80~119 分 | 闲家上台，换庄，等级不变 |
| 120~159 分 | 闲家升 1 级 |
| 160~199 分 | 闲家连升 2 级 |
| 200 分 | 闲家连升 3 级（大光） |

### 3.5 抠底倍数

| 牌型 | 倍数 |
|:-----|:-----|
| 单张抠底 | ×2 |
| 对子抠底 | ×4 |
| 拖拉机（两对） | ×16 |
| 更长拖拉机 | 每多一对翻倍 |

---

## 4. 快速开始

### 4.1 环境依赖

| 依赖项 | 版本要求 | 说明 |
|:-------|:---------|:-----|
| Android Studio | Ladybug 或更高 | 推荐最新稳定版 |
| JDK | 17+ | Android Studio 自带 |
| Android SDK | 35 | compileSdk = 35 |
| minSdk | 24 | Android 7.0+ |
| Gradle | 8.11.1 | 项目自带 Wrapper |

### 4.2 安装部署

#### 方式一：从源码构建（推荐）

```bash
# 1. 克隆项目
git clone https://gitee.com/yin-zhenhua/shengji.git

# 2. 进入项目目录
cd ShengJi

# 3. 用 Android Studio 打开项目
# File → Open → 选择 ShengJi/ 目录

# 4. 等待 Gradle Sync 完成（首次可能需要下载依赖）

# 5. 连接 Android 设备或启动模拟器

# 6. 点击 Run ▶ 或使用快捷键 Shift+F10
```

#### 方式二：安装 APK

```bash
# 如果有预编译的 APK 文件
adb install 升级.apk
```

### 4.3 构建命令

```bash
# 调试模式运行
./gradlew installDebug

# 构建 Release APK
./gradlew assembleRelease

# 运行单元测试
./gradlew test

# 代码检查
./gradlew lint
```

### 4.4 音效配置（可选）

将 mp3 文件放入 `app/src/main/res/raw/` 目录：

| 文件名 | 用途 | 说明 |
|:-------|:-----|:-----|
| `play_card.mp3` | 出牌音效 | 每次出牌时播放 |
| `win.mp3` | 胜利音效 | 赢得对局时播放 |
| `lose.mp3` | 失败音效 | 输掉对局时播放 |
| `invalid.mp3` | 无效操作提示音 | 出牌不合法时播放 |

> 💡 **提示**：音效文件不存在时游戏正常运行，只是没有声音。

---

## 5. 使用手册

### 5.1 基础操作教程

#### 5.1.1 开始游戏

1. 启动应用后进入首页
2. 点击 **「开始新游戏」** 进入单机模式
3. 点击 **「联机对战」** 进入局域网联机模式
4. 点击 **「历史记录」** 查看过往对局

#### 5.1.2 单机模式操作

**发牌阶段：**
- 系统自动发牌，每人 25 张
- 发牌过程中如果摸到级牌对子，可以点击亮主

**亮主/反主：**
- 点击手中的级牌对子进行亮主
- 其他玩家可以用更高级牌对子或大小王对反主
- 反主成功会更换主花色

**换底牌：**
- 庄家拿到 8 张底牌后，可以与手牌进行交换
- 点击底牌 → 点击手牌 → 配对交换
- 支持多对同时交换
- 点击 **「不换」** 跳过

**出牌：**
- 点击手牌选中（可多选）
- 点击 **「出牌」** 按钮出牌
- 首出：任意合法牌型
- 跟牌：必须跟同花色同牌型，无花色可杀牌

**托管：**
- 点击 **「托管」** 按钮，AI 自动接管出牌
- 再次点击 **「取消托管」** 恢复手动操作

#### 5.1.3 联机模式操作

**创建房间（房主）：**
1. 点击 **「创建房间」**
2. 等待其他玩家加入
3. 人满后点击 **「开始游戏」**

**加入房间：**
1. 确保与房主在同一 WiFi 网络
2. 系统自动搜索附近房间
3. 点击房间列表中的房间加入

**权限说明：**
- 首次进入联机模式需要授权网络权限
- 如果拒绝权限，可以点击 **「去设置打开」** 手动开启

### 5.2 配置项说明

| 配置项 | 位置 | 说明 |
|:-------|:-----|:-----|
| 背景主题 | 设置页 | 6种背景颜色主题可选 |
| 音效开关 | 设置页 | 开启/关闭游戏音效 |
| 音效音量 | 设置页 | 调节音效音量大小 |
| 字体大小 | 设置页 | 0.7x - 2.0x 可调 |
| 牌面大小 | 设置页 | 0.7x - 2.0x 可调 |
| 牌面款式 | 设置页 | 单车牌/姚记牌两种样式 |
| AI 难度 | 设置页 | EASY/MEDIUM/HARD/MASTER |
| `minSdk` | `app/build.gradle.kts` | 最低支持 Android 版本（默认 24） |
| `targetSdk` | `app/build.gradle.kts` | 目标 Android 版本（默认 35） |
| `UDP 端口` | `UdpDiscovery.kt` | UDP 广播端口（默认 9876） |
| `TCP 端口` | `TcpManager.kt` | TCP 通信端口（默认 9877） |

### 5.3 常见问题排错

#### Q1: 游戏闪退怎么办？

**可能原因：**
- Android 版本过低（需要 Android 7.0+）
- 设备内存不足

**解决方案：**
1. 检查设备 Android 版本是否 >= 7.0
2. 关闭后台应用释放内存
3. 清除应用数据后重试

#### Q2: 联机模式搜不到房间？

**可能原因：**
- 不在同一 WiFi 网络
- 防火墙阻止了 UDP 广播
- 未授权网络权限

**解决方案：**
1. 确保所有设备连接同一 WiFi
2. 检查防火墙设置，允许 UDP 端口 9876
3. 进入设置 → 应用 → 升级 → 权限，开启网络权限

#### Q3: 联机模式连接失败？

**可能原因：**
- TCP 端口被占用
- 网络不稳定

**解决方案：**
1. 重启应用和 WiFi
2. 尝试切换热点
3. 检查是否有其他应用占用 9877 端口

#### Q4: 游戏没有声音？

**说明：** 这是正常现象。游戏音效需要手动放置 mp3 文件到 `app/src/main/res/raw/` 目录。详见 [音效配置](#44-音效配置可选) 章节。

#### Q5: 如何清除对局历史？

**操作：**
1. 进入 **「历史记录」** 页面
2. 点击右上角 **「清除」** 按钮
3. 确认清除

#### Q6: 游戏卡顿/动画不流畅？

**可能原因：**
- 设备性能较低
- 内存不足

**解决方案：**
1. 关闭后台应用
2. 重启应用
3. 如果持续卡顿，可能是设备性能限制

---

## 6. 技术架构

### 6.1 技术栈

| 技术 | 版本 | 用途 |
|:-----|:-----|:-----|
| Kotlin | 2.0.21 | 开发语言 |
| Jetpack Compose | BOM 2024.12 | 声明式 UI 框架 |
| Material3 | - | 设计系统 |
| Navigation Compose | 2.8.5 | 页面导航 |
| Room | 2.6.1 | 本地数据库（对局历史） |
| Gson | 2.11.0 | JSON 序列化（网络通信） |
| KSP | 2.0.21-1.0.28 | Room 注解处理 |
| Coroutines | - | 异步编程 |
| AGP | 8.7.3 | Android 构建工具 |
| Gradle | 8.11.1 | 构建系统 |

### 6.2 架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│                          UI 层 (Compose)                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │HomeScreen│  │GameScreen│  │Multiplayer│  │History   │       │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘       │
│       │              │              │              │             │
│       └──────────────┴──────────────┴──────────────┘             │
│                           │ StateFlow + GameEvent                │
└───────────────────────────┼─────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                     ViewModel 层 (调度中心)                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    GameViewModel                         │   │
│  │  • 状态管理 (StateFlow<GameState>)                       │   │
│  │  • 事件处理 (handleEvent)                                │   │
│  │  • AI 调度 (processAiLead/Follow/CallMain)              │   │
│  │  • 动画控制 (飞牌/出牌动画)                               │   │
│  │  • 生命周期管理 (轮次/局次)                               │   │
│  └─────────────────────────────────────────────────────────┘   │
└───────────────────────────┼─────────────────────────────────────┘
                            │ 纯函数调用
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Data 层 (业务逻辑)                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │UseCases  │  │Evaluators│  │Validators│  │AI Player │       │
│  │(纯函数)  │  │(纯函数)  │  │(纯函数)  │  │(纯函数)  │       │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

**设计原则：**
- **单向数据流**：UI → ViewModel → Data，状态通过 StateFlow 回流
- **纯函数优先**：所有 UseCase 均为纯函数，无副作用，可单元测试
- **事件驱动**：用户操作通过 `GameEvent` 密封类传递
- **状态集中**：`GameState` 包含游戏所有状态

### 6.3 项目结构

```
ShengJi/
├── app/                              # Android 应用模块
│   ├── src/main/java/com/shengji/app/
│   │   ├── data/                     # 📦 核心数据层（纯逻辑，可单元测试）
│   │   │   ├── Card.kt              #   扑克牌数据类
│   │   │   ├── Player.kt            #   玩家数据类
│   │   │   ├── GameState.kt         #   全局状态
│   │   │   ├── GameEvent.kt         #   用户操作事件
│   │   │   ├── PlayCombo.kt         #   出牌组合
│   │   │   ├── RuleConfig.kt        #   规则配置
│   │   │   ├── DeckGenerator.kt     #   牌组生成 + 洗牌
│   │   │   ├── CardEvaluator.kt     #   牌面评估
│   │   │   ├── CardTracker.kt       #   记牌器（增强版）
│   │   │   ├── HeuristicEvaluator.kt#   启发式评估器
│   │   │   ├── MonteCarloSimulator.kt#  蒙特卡洛模拟器
│   │   │   ├── AiLearner.kt         #   AI Q-Learning 学习器
│   │   │   ├── AiDifficulty.kt      #   AI 难度枚举
│   │   │   ├── PlayValidation.kt    #   出牌校验
│   │   │   ├── RoundResolver.kt     #   轮次结算
│   │   │   ├── AiPlayer.kt          #   AI 策略（多难度）
│   │   │   ├── DealUseCase.kt       #   发牌逻辑
│   │   │   ├── CallMainUseCase.kt   #   叫主逻辑
│   │   │   ├── DigBottomUseCase.kt  #   抠底逻辑
│   │   │   ├── SettingsManager.kt   #   设置管理器
│   │   │   └── ScoringUseCase.kt    #   计分升级
│   │   ├── enums/                    # 📋 枚举定义
│   │   ├── viewmodel/                # 🎯 ViewModel 层
│   │   ├── net/                      # 🌐 网络层
│   │   ├── db/                       # 💾 数据库层
│   │   ├── ui/                       # 🎨 UI 层
│   │   ├── theme/                    # 🎨 主题
│   │   └── simulation/               # 🧪 模拟脚本
│   └── build.gradle.kts              # 应用级构建配置
├── gradle/                           # Gradle Wrapper
├── build.gradle.kts                  # 项目级构建配置
├── RULES.md                          # 📜 完整游戏规则
├── TEST_REPORT.md                    # 📊 代码审查报告
├── TEST_REPORT_FUNCTIONAL.md         # 📊 功能测试报告
└── README.md                         # 📖 本文件
```

---

## 7. 开发文档

### 7.1 编译构建流程

```bash
# 1. 克隆项目
git clone https://gitee.com/yin-zhenhua/shengji.git
cd ShengJi

# 2. 构建 Debug APK
./gradlew assembleDebug
# 输出: app/build/outputs/apk/debug/app-debug.apk

# 3. 构建 Release APK
./gradlew assembleRelease
# 输出: app/build/outputs/apk/release/app-release-unsigned.apk

# 4. 安装到设备
./gradlew installDebug

# 5. 运行单元测试
./gradlew test

# 6. 运行 Android 仪器测试
./gradlew connectedAndroidTest

# 7. 代码检查
./gradlew lint
```

### 7.2 开发调试方式

#### 使用 Android Studio

1. 打开项目，等待 Gradle Sync
2. 选择设备（模拟器或真机）
3. 点击 **Run ▶** 或 **Debug 🐛**
4. 使用 Logcat 查看日志（TAG: `ShengJi`）

#### 使用命令行

```bash
# 安装并运行
./gradlew installDebug
adb shell am start -n com.shengji.app/.MainActivity

# 查看日志
adb logcat -s ShengJi

# 清除应用数据
adb shell pm clear com.shengji.app
```

### 7.3 重要业务逻辑

#### 7.3.1 发牌流程

```kotlin
// DealUseCase.kt
fun deal(deck: List<Card>, playerIds: List<String>, playerNames: List<String>): DealResult {
    val shuffled = DeckGenerator.shuffle(deck)  // Fisher-Yates 洗牌
    val bottomCards = shuffled.take(8)           // 前 8 张为底牌
    val handCards = shuffled.drop(8)             // 剩余 100 张
    // 每人 25 张，轮流发牌
}
```

#### 7.3.2 叫主/反主逻辑

```kotlin
// CallMainUseCase.kt
fun tryCallMain(cards: List<Card>, callerIndex: Int, currentLevel: Rank, ...): CallMainResult {
    // 1. 验证是否为级牌对子或大小王对
    // 2. 检查反主优先级（方块<梅花<红桃<黑桃<小王<大王）
    // 3. 返回是否成功 + 新的主花色
}
```

#### 7.3.3 AI 策略核心

```kotlin
// HeuristicEvaluator.kt
fun scoreLead(cards: List<Card>, handCards: List<Card>, ...): Double {
    // 评估出牌价值：
    // 1. 主牌首出：清主策略（+30分）
    // 2. 副牌首出：长套优先（+20分）
    // 3. 对子/拖拉机首出：高价值（+25分）
    // 4. 分牌扣分：保留分牌等队友收
    // 5. 大牌保留：留着收分更好
}
```

#### 7.3.4 轮次结算

```kotlin
// RoundResolver.kt
fun resolveRound(plays: List<Pair<Int, List<Card>>>, leadCombo: PlayCombo, ...): RoundResult {
    // 1. 比较所有出牌的权重
    // 2. 权重 = 基础权重(主牌>首出花色>杂牌) + 最大牌比较权重
    // 3. 收集本轮所有分牌（5/10/K）
    // 4. 返回赢家索引 + 本轮得分
}
```

#### 7.3.5 抠底倍数

```kotlin
// RoundResolver.kt
fun digBottomMultiplier(lastCombo: ComboType, comboSize: Int = 1): Int {
    return when (lastCombo) {
        ComboType.SINGLE -> 2      // 单张 ×2
        ComboType.PAIR -> 4        // 对子 ×4
        ComboType.TRACTOR -> 16    // 拖拉机 ×16（每多一对翻倍）
        ComboType.SLINGSHOT -> 2   // 甩牌按单张计算
        else -> 2
    }
}
```

### 7.4 扩展开发指引

#### 添加新的 AI 策略

1. 在 `HeuristicEvaluator.kt` 中添加新的评分函数
2. 在 `AiPlayer.kt` 中调用新的评分函数
3. 在 `GameViewModel.kt` 中的 AI 调度逻辑中集成

#### 添加新的牌型

1. 在 `ComboType.kt` 中添加新的枚举值
2. 在 `PlayValidation.kt` 中添加牌型识别逻辑
3. 在 `RoundResolver.kt` 中添加比较逻辑
4. 在 `GameScreen.kt` 中添加 UI 显示

#### 添加新的游戏模式

1. 在 `GamePhase.kt` 中添加新的游戏阶段
2. 在 `GameState.kt` 中添加相关状态
3. 在 `GameEvent.kt` 中添加新的事件
4. 在 `GameViewModel.kt` 中添加处理逻辑
5. 在 `ui/screens/` 中添加新的页面

---

## 8. 测试报告

### 8.1 代码审查报告

详见 [TEST_REPORT.md](TEST_REPORT.md)

**主要发现：**
- ✅ 架构设计清晰，MVVM 分层合理
- ✅ 业务逻辑纯函数化，易于测试
- ⚠️ 部分复杂逻辑缺少单元测试
- ⚠️ 网络层错误处理可优化

### 8.2 功能测试报告

详见 [TEST_REPORT_FUNCTIONAL.md](TEST_REPORT_FUNCTIONAL.md)

**测试覆盖：**
- ✅ 发牌流程正常
- ✅ 亮主/反主逻辑正确
- ✅ 出牌校验完整
- ✅ 计分升级准确
- ⚠️ 联机模式稳定性待提升

### 8.3 已知问题

| 问题 | 严重度 | 状态 | 说明 |
|:-----|:-------|:-----|:-----|
| 拖拉机跟牌校验 | 🟡 中等 | 待修复 | 当前不要求严格牌型匹配，只要求花色正确 |
| 甩牌合法性 | 🟡 中等 | 待修复 | 简化为同花色多张，未校验"甩牌必须是对家管不住的牌" |
| 扣底最后一轮牌型 | 🟡 中等 | 待修复 | 从 `state.currentLeadCombo` 取，但该值在轮次结算时已清空 |
| 单元测试 | 🟡 中等 | 待补充 | UseCase 均为纯函数，可直接测试，但尚未编写 |

---

## 9. 更新日志

### v1.1.0 (2026-08-31)

#### 🎉 新增功能
- AI 多难度系统（EASY/MEDIUM/HARD/MASTER）
- AI Q-Learning 自我学习（MASTER 难度）
- 蒙特卡洛模拟搜索（HARD 难度）
- 字体大小调节（0.7x - 2.0x）
- 牌面大小调节（0.7x - 2.0x）
- 牌面款式选择（单车牌/姚记牌）
- 音效音量控制
- 存档保存/恢复功能
- 联机使用说明
- 设置页实时预览字体效果

#### 🐛 问题修复
- 修复平局决胜逻辑
- 修复无主分类问题
- 修复出牌按钮状态
- 修复网络队友获胜逻辑
- 修复出牌后选中状态未清除
- 修复 AI 首出评估器奖励 A/K 牌

#### 🔧 改进
- 全页面支持滚动
- 存档覆盖提示
- 记牌器增强

### v1.0.0 (2024-01-XX)

#### 🎉 新增功能
- 完整升级规则实现
- 单机 AI 对战模式
- 局域网联机模式
- Material3 主题设计
- 飞牌动画效果
- 音效反馈系统
- 对局历史记录

#### 🐛 问题修复
- 修复出牌数量校验问题
- 修复 AI 跟牌逻辑
- 修复联机模式连接稳定性

#### 📝 文档更新
- 完善 README 文档
- 添加游戏规则说明
- 添加开发文档
- 添加测试报告

---

## 10. 许可证与致谢

### 许可证

本项目仅供学习交流使用。

### 致谢

感谢以下开源项目：

- [Jetpack Compose](https://developer.android.com/jetpack/compose) — 声明式 UI 框架
- [Material3](https://m3.material.io/) — 设计系统
- [Room](https://developer.android.com/training/data-storage/room) — 本地数据库
- [Gson](https://github.com/google/gson) — JSON 序列化

### 联系方式

- 项目维护者：yzh
- 邮箱：592989785@qq.com
- 问题反馈：[Gitee Issues](https://gitee.com/yin-zhenhua/shengji/issues)

---

<p align="center">
  Made with ❤️ for card game lovers
</p>

<p align="center">
  <a href="#-快速开始">快速开始</a> •
  <a href="#-功能特性">功能特性</a> •
  <a href="#-游戏规则">游戏规则</a> •
  <a href="#-技术架构">技术架构</a> •
  <a href="#-开发文档">开发文档</a>
</p>
