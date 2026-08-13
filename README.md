# 升级（Sheng Ji）— Android 纸牌游戏

> 支持单机 AI 对战和局域网联机的升级纸牌游戏，Jetpack Compose + MVVM 架构。

---

## 游戏简介

- **4 人对战**，两两对家为队友，使用 2 副牌（108 张）
- 每人 25 张手牌，8 张底牌归庄家
- 目标：从 2 开始逐级升级，先打过 A 的队伍获胜

### 核心玩法

| 环节 | 说明 |
|------|------|
| 叫主 | 一对级牌亮主确定主花色；可用大小王对反主（无主局） |
| 扣底 | 庄家拿 8 张底牌后扣回 8 张，底牌分影响最终结算 |
| 出牌 | 首出任意牌型，跟牌必须跟同花色同牌型；无花色可杀牌 |
| 牌型 | 单张、对子（同花色同点数）、拖拉机（同花色连续对子） |
| 计分 | 5=5分, 10=10分, K=10分，全场 200 分 |
| 抠底 | 闲家赢最后一轮拿底牌，分值按牌型翻倍（单张×2, 对子×4, 拖拉机×16） |
| 升级 | 0分升3级, 1-39分升2级, 40-79分升1级, 80+分闲家上庄 |

> 完整规则详见 [RULES.md](RULES.md) 或游戏内「规则」页面。

---

## 功能特性

### 单机模式
- 1 人类 + 3 AI 对战，完整升级规则
- AI 策略：首出跑分、跟牌出最小、无花色时杀牌
- **配对式换底牌**：庄家可将底牌与手牌一一配对交换
- **正确轮次管理**：按座位顺序出牌，上轮赢家本轮首出
- **托管功能**：中途可让 AI 接管，随时可接管回来
- 出牌动画 + 音效反馈

### 联机模式（局域网）
- UDP 自动广播发现（无需手动输入 IP）
- TCP 消息同步（房主 = 服务端，加入者 = 客户端）
- 老人向设计：大字体、大白话权限引导

### 其他
- Room 本地存储对局历史
- Material3 主题，适配深色模式

---

## 项目结构

```
app/src/main/java/com/shengji/app/
├── data/                    # 核心数据层（纯逻辑，可单元测试）
│   ├── Card.kt              # 扑克牌数据类
│   ├── Player.kt            # 玩家数据类
│   ├── GameState.kt         # 全局状态
│   ├── GameEvent.kt         # 用户操作事件密封类
│   ├── PlayCombo.kt         # 出牌组合数据类
│   ├── RuleConfig.kt        # 规则配置
│   ├── DeckGenerator.kt     # 牌组生成 + Fisher-Yates 洗牌
│   ├── CardEvaluator.kt     # 牌面评估（主牌判断/比较/分值）
│   ├── CardTracker.kt       # 牌面跟踪
│   ├── HeuristicEvaluator.kt # 启发式评估器
│   ├── PlayValidation.kt    # 出牌校验（牌型识别/跟牌规则）
│   ├── RoundResolver.kt     # 轮次结算（赢家判定/分值收集）
│   ├── AiPlayer.kt          # AI 策略
│   ├── DealUseCase.kt       # 发牌逻辑
│   ├── CallMainUseCase.kt   # 叫主逻辑
│   ├── DigBottomUseCase.kt  # 抠底逻辑
│   └── ScoringUseCase.kt    # 计分升级 + 庄家轮换
├── enums/                   # 枚举（Suit, Rank, GamePhase, ComboType, PlayerType）
├── viewmodel/               # GameViewModel（StateFlow + 事件驱动）
├── net/                     # 局域网联机（UDP 发现 + TCP 通信）
│   ├── NetworkModels.kt     # 网络数据模型
│   ├── NetworkPermissionHelper.kt # 权限辅助
│   ├── TcpManager.kt        # TCP 通信管理
│   └── UdpDiscovery.kt      # UDP 自动发现
├── db/                      # Room 本地存储
│   ├── AppDatabase.kt       # 数据库实例
│   ├── GameRecordDao.kt     # 数据访问对象
│   └── GameRecordEntity.kt  # 对局记录实体
├── ui/
│   ├── components/          # 游戏组件（GameComponents, SettlementDialog, SoundManager）
│   ├── navigation/          # 导航图（NavGraph）
│   └── screens/             # 页面（Home, Game, Multiplayer, History）
├── theme/                   # Material3 主题
├── simulation/              # 模拟脚本（验证游戏逻辑）
├── MainActivity.kt          # Activity 入口
└── ShengJiApplication.kt    # Application 类
```

### 架构

```
UI (Compose) ──StateFlow + GameEvent──▶ ViewModel ──纯函数调用──▶ Data Layer
  GameScreen                              │
  HomeScreen                         AI 调度 + 轮次生命周期
  MultiplayerScreen
```

- UI 只消费状态，通过 `handleEvent()` 发送操作
- ViewModel 做胶水，调度 UseCase 纯函数
- UseCase 全部纯函数，无副作用

---

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | 2.0.21 | 开发语言 |
| Jetpack Compose | BOM 2024.12 | UI 框架 |
| Material3 | - | 设计系统 |
| Navigation Compose | 2.8.5 | 页面导航 |
| Room | 2.6.1 | 本地数据库 |
| Gson | 2.11.0 | JSON 序列化 |
| KSP | 2.0.21-1.0.28 | Room 注解处理 |
| Coroutines | - | 异步编程 |
| AGP | 8.7.3 | Android 构建工具 |
| Gradle | 8.11.1 | 构建系统 |

---

## 快速开始

### 环境要求
- Android Studio Ladybug 或更高版本
- JDK 17+
- Android SDK 35

### 运行
1. 用 Android Studio 打开 `ShengJi/` 目录
2. 等待 Gradle Sync 完成
3. 连接手机或启动模拟器，点击 Run

### 音效配置（可选）
将 mp3 文件放入 `app/src/main/res/raw/`：

| 文件名 | 用途 |
|--------|------|
| `play_card.mp3` | 出牌音效 |
| `win.mp3` | 胜利音效 |
| `lose.mp3` | 失败音效 |
| `invalid.mp3` | 无效操作提示音 |

文件不存在时游戏正常运行，只是没有声音。

---

## 相关文档

- [RULES.md](RULES.md) — 完整游戏规则详解
- [TEST_REPORT.md](TEST_REPORT.md) — 代码审查与测试报告

---

## 更新日志

### v1.6 — 交互优化
- 配对式换底牌：点击底牌再点击手牌一一配对，支持多对同时交换
- 修复出牌轮次：按座位顺序出牌，不再总是玩家第二个出

### v1.5 — 托管功能
- 游戏中可切换人类/AI 控制
- 托管时 AI 自动出牌，可随时接管回来

### v1.4 — 局域网联机
- UDP 自动广播发现（1.5 秒间隔，3 秒超时）
- TCP 服务端/客户端通信，JSON 消息协议
- 权限管理（老人向大白话引导）

### v1.3 — 体验优化
- 抠底倍数修复，分数 clamp 到 0-200
- AI 策略优化，手牌滚动适配
- 音效系统（SoundPool），退出确认弹窗

### v1.2 — 可视化 UI
- CardView 可复用组件，GameScreen 对局界面
- 结算弹窗 + 叫主弹窗，首页 + 历史记录

### v1.1 — 游戏逻辑
- 牌组生成 + 洗牌，发牌/叫主/出牌校验/轮次结算
- AI 策略，抠底/计分升级/庄家轮换

### v1.0 — 基础框架
- Android 项目搭建，Compose 模板，核心数据类，Room 存储

---

## 许可证

本项目仅供学习交流使用。
