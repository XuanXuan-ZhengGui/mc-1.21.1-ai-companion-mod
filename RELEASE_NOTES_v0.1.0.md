# AI Companion v0.1.0

这是 `mc-1.21.1-ai-companion-mod` 的首个发布版本，适用于 Minecraft 1.21.1 Fabric 客户端。

## 版本亮点

- 按 `L` 键可在游戏内打开已加载地图界面。
- 未加载区块会显示为黑色，已加载区块显示为绿色，玩家所在区块显示为黄色。
- 按 `ESC` 打开游戏菜单后，可通过“加入本地或云端AI”进入 AI 配置界面。
- AI 配置支持云端 API，以及本地 `ollama` / `llama.cpp` 服务商。
- 配置模型名后，可在游戏内使用 `@*模型名` 或 `@模型名` 与 AI 对话。
- 支持“聊天”和“加入游戏”两种模式入口。

## 本地服务默认地址

- `ollama`: `127.0.0.1:11434`
- `llama.cpp`: `127.0.0.1:8080`

## 安装方式

1. 确认已安装 Minecraft 1.21.1、Fabric Loader 和 Fabric API。
2. 下载构建产物 `ai-companion-0.1.0.jar`。
3. 将 jar 文件放入 Minecraft 的 `mods` 文件夹。
4. 启动游戏，在世界中按 `L` 或按 `ESC` 进入 AI 配置。

## 构建方式

```powershell
gradle build
```

构建完成后，jar 文件通常位于：

```text
build/libs/
```

## 已知限制

- 云端 API 的输入界面已完成，但具体云端服务商的请求协议需要后续按服务商格式补充。
- `ollama` 和 `llama.cpp` 已提供本地 HTTP 调用入口，但需要用户先在本机启动对应服务。
- “加入游戏”当前提供模式入口和聊天能力，AI 角色实体、移动和行为逻辑仍属于后续版本目标。
- 当前仓库未包含 Gradle Wrapper，需要本机已有 Java 21 与 Gradle 才能直接构建。
