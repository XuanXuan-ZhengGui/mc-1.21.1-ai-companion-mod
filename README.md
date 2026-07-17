# AI Companion Mod — v0.5.0

Minecraft 1.21.1 Fabric 客户端 Mod：在游戏内接入 AI 模型，支持聊天对话、可交互的 AI 玩家（通过外部 Mindcraft/mineflayer 进程）、AI 同伴实体回退模式、地图查看，以及对公网/域名/局域网服务器的直接接入与日志追踪。

## 主要特性（v0.5.0）

- 游戏内 AI 聊天：在聊天中以 `@模型名` 或 `@*模型名` 与模型对话（支持本地 Ollama / llama.cpp 与云端）。
- 可交互 AI 玩家（真实客户端）：启用 AI Bot（`aiBotMode`）并配置 Mindcraft 后端后，Mod 会生成配置并启动 Node/mindcraft 进程，让 bot 作为真实玩家连接到目标服务器（支持域名、公网 IP、内网 IP、`host:port`）。
- 自定义目标服务器：配置项 `targetServerIp` 支持域名或 `host:port`（优先使用）；若未填写，Mod 会尝试检测当前正在游玩的世界（单机 LAN 或当前远程服务器）并连接相同地址。
- GUI 控件：Mindcraft 配置界面提供：
  - `mindcraftPath`（Node 项目目录）
  - `targetServerIp`（支持 `example.com:25565` / `1.2.3.4:25565` / `192.168.1.10`）
  - `mindserverPort`（回退默认端口）
  - 启动 / 停止 Mindcraft 按钮
  - 日志查看器（实时显示 Node stdout，写入 `mindcraftPath/logs/mindcraft.log`）
- 日志与持久化：Node stdout 会被按行写入 `mindcraftPath/logs/mindcraft.log`，且 Mod 会在游戏内显示带前缀的日志行 (`[Mindcraft]`)。
- 更稳健的连接策略：自动检测局域网内网 IP（用于 LAN）、通过多种方法读取当前服务器地址/端口（公开 getter、反射、网络 handler），并在无法检测时回退到 `targetServerIp`。
- 版本与许可：v0.5.0，MIT

---

## 快速开始（最短路径）

1. 环境准备
   - JDK 21
   - Gradle 8.10+
   - Fabric Loader + Fabric API（运行客户端）
   - Node.js 18+（用于 Mindcraft / mineflayer 后端）

2. 构建

```bash
# 在项目根目录
gradle build
```

构建产物位于 `build/libs` 或 `dist/ai-companion-0.5.0.jar`。

3. 安装与运行

- 将 `ai-companion-0.5.0.jar` 放到 `%minecraft%/.minecraft/mods/`。
- 启动 Fabric 客户端并进入世界（单人或联机）。
- 打开 Mod GUI（ESC → AI 接入 → Mindcraft Config）：
  - 填写 `mindcraftPath`（指向一个已 `npm install` 的 Mindcraft 项目）
  - 可填写 `targetServerIp`（domain 或 `host:port`）或留空使用自动检测
  - 启用 `aiBotMode`，并点击“保存并启动 Bot”或直接点击“启动 Mindcraft”
- 单机局域网：ESC → 对局域网开放（允许作弊），Mod 会自动检测并写入配置、启动 Node 并让 bot 加入。

4. 日志与调试

- Node stdout 会写入 `mindcraftPath/logs/mindcraft.log`。
- 游戏内聊天或 GUI 日志窗口会显示以 `[Mindcraft]` 为前缀的实时输出。

5. `targetServerIp` 示例

- `example.com:25565`
- `203.0.113.5:25565`
- `192.168.1.100`

---

## 安全与网络注意

- 公网连接需要目标服务器开放端口并可达（防火墙/NAT/端口映射）。
- 若服务器需要在线认证（正版），Bot 可能需要额外的 auth 支持（可选功能）。
- 写入 `mindcraftPath` 的文件可能包含 API keys（`keys.json`），请保护目录权限。

---

## 开发者说明（简要）

- 源码路径：`src/main/java/com/xuanxuan/aicompanion/client/...`
- 关键模块：
  - `AiCompanionClient`：mod 初始化、事件监听、LAN/目标检测、启动 Mindcraft
  - `MindcraftProcessManager`：生成 `keys.json` / `andy.json` / `settings.js`，启动/停止 Node 进程，写日志
  - `CompanionEntityManager`：客户端仅可视实体回退逻辑（`aiBotMode=false`）
  - `AiRouter`：本地/云端模型路由与 HTTP 调用
  - `GUI`：`MindcraftConfigScreen`（`targetServerIp`、启动/停止、日志查看）
- 配置文件：`config/ai-companion.properties` 包含 `targetServerIp`、`mindcraftPath`、`mindserverPort`、`apiKey`、`modelName`、`aiBotMode` 等

---

## 常见问题

- Bot 未加入：检查 `mindcraftPath/settings.js` 中 host/port、Node 是否能连目标服务器、`logs/mindcraft.log` 中的错误。
- DNS 解析失败：在运行机器上确认域名可解析。
- 认证失败：如服务器启用正版认证，启用 online auth 后续功能或使用 offline server。

---

## 版本历史（v0.5.0）

- 新增：真实 AI 玩家（通过 Mindcraft/mineflayer）
- 新增：`targetServerIp` 支持（域名、公网/内网 IP:port）
- 新增：GUI 控件（host/port 输入、启动/停止、日志查看）
- 新增：Node 日志文件写入
- 改进：自动检测 LAN/IP 与当前连接服务器地址
- 升级：`mod_version` -> `0.5.0`
