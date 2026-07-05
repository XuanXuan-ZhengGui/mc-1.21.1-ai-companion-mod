# AI Companion Mod for Minecraft 1.21.1

Minecraft 1.21.1 Fabric 客户端 Mod，在游戏中接入 AI 模型，支持聊天对话、AI 同伴实体跟随，以及通过 Mindcraft 框架实现 AI Bot 自主控制角色。

## 功能

### 地图查看
- 按 `M` 键打开已加载地图界面
- 已加载区块显示为绿色，未加载区块显示为黑色，玩家所在区块显示为黄色

### AI 聊天模式
- 在游戏中输入 `@模型名` 或 `@*模型名` 与 AI 对话
- 支持云端 API 和本地模型（Ollama / Llama.cpp）

### AI 同伴实体
- "加入游戏"模式生成 AI 玩家实体，跟随玩家移动
- 检测聊天关键词自动通过 AI 回复

### AI Bot 模式（v4.0 新增）
- 通过 Mindcraft 框架（mineflayer 协议）连接本机局域网世界
- AI Bot 可以自主移动、采集资源、建造、战斗
- **游戏内 GUI 配置**：无需手动编辑配置文件

## Mindcraft Bot 使用方法

### 前置条件
- Minecraft 1.21.1 + Fabric Loader + Fabric API
- Node.js 18+（用于运行 Mindcraft 后端）
- 云端 AI API 密钥（支持 OpenAI 兼容协议，如阿里云百炼、DeepSeek 等）

### 快速开始

1. **安装 Mod**：将 `dist/ai-companion-4.0.jar` 放入 `.minecraft/mods/` 目录
2. **启动游戏**：打开或创建一个世界
3. **开启局域网**：ESC → 对局域网开放（勾选允许作弊）
4. **打开配置**：ESC → AI 接入 → **Mindcraft Bot**
5. **填写配置**：

| 配置项 | 说明 | 示例 |
|--------|------|------|
| Bot 玩家名 | 3-16 位字母数字下划线 | `XuanXuan` |
| API 协议 | OpenAI 兼容协议填 `openai` | `openai` |
| API 地址 | 模型服务的 API 端点 | `https://xxx.aliyuncs.com/compatible-mode/v1` |
| API Key | 你的 API 密钥 | `sk-xxxxx` |
| 模型名 | 使用的模型名称 | `qwen-max` |
| Mindcraft 路径 | mindbot 目录的绝对路径 | `d:\mindbot` |

6. **点击"保存并启动 Bot"**：Mod 会自动生成配置文件并启动 Node.js 后端进程
7. Bot 会在游戏中自动连接并加入你的世界

### 配置说明
- **API 协议**：大多数云端服务商（阿里云百炼、DeepSeek、OpenRouter 等）都兼容 OpenAI 协议，填写 `openai` 即可
- **Mindcraft 路径**：需要指向一个已安装依赖（`npm install`）的 Mindcraft/mineflayer 项目目录
- Bot 运行状态会在配置界面底部显示（绿色=运行中，红色=未启动）

## 项目结构

```
src/main/java/com/xuanxuan/aicompanion/client/
├── AiCompanionClient.java          # 主入口，ClientModInitializer
├── ai/AiRouter.java                # AI 请求路由（云端/Ollama/Llama.cpp）
├── bot/                            # AI Bot 自主控制模块
├── config/AiCompanionConfig.java    # 配置管理
├── entity/CompanionEntityManager   # AI 同伴实体管理
├── gui/                            # GUI 界面
│   ├── MindcraftConfigScreen.java  # Mindcraft Bot 配置界面 (v4.0)
│   ├── AiProviderScreen.java       # AI 服务选择
│   ├── CloudConfigScreen.java      # 云端 API 配置
│   └── LocalModelScreen.java       # 本地模型配置
├── mixin/                          # Minecraft 注入
└── mindcraft/
    └── MindcraftProcessManager.java # Mindcraft 进程管理 (v4.0)
```

## 构建

需要 Java 21 和 Gradle 8.10+：

```powershell
gradle build
```

或通过 GitHub Actions 自动构建（推送代码后自动触发）。

## 零第三方依赖

本项目仅依赖 Fabric API 和 Minecraft 本身，无额外运行时库。

## 版本

- 当前版本：`v4.0`
- 许可证：MIT
- 仓库：https://github.com/XuanXuan-ZhengGui/mc-1.21.1-ai-companion-mod
