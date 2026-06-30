# AI Companion v0.3.2 发布说明

## 核心改进

### 真正的 AI 链接与操控

v0.3.2 彻底改写了 AI 决策系统，不再是简单的关键词匹配判断，而是：

1. **结构化 JSON 指令**：AI 模型返回完整的 JSON，包含思考过程、目标和动作序列
2. **动作序列执行**：支持一次决策连续执行多个动作（如：转向 -> 前进 -> 挖掘 -> 放置）
3. **动作历史记忆**：将最近的动作记录发送给模型，让 AI 知道自己的行为历史
4. **丰富的动作类型**：move, jump, look, attack, use, mine, place, selectSlot, chat, stop

### 仅客户端可见的日志系统

新增 `AiBotLogger`，在 AI Bot 客户端模式下显示：
- `[思考]` AI 的推理过程
- `[动作]` 正在执行的动作
- `[感知]` 环境信息和模型回复
- `[警告]` 异常和危险状态
- `[信息]` 一般状态提示

日志仅显示在安装 mod 的 AI Bot 客户端，不会发送到服务器聊天。

## 文件变更

- 新增 `AiBotLogger.java`
- 新增 `AiBotAction.java`
- 新增 `AiBotActionQueue.java`
- 重写 `AiBotController.java`
