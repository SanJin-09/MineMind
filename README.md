# MineMind

MineMind 是一个 Java 版 Minecraft 客户端 Mod，让玩家在单人游戏中通过原版聊天栏使用自己的 API Key 与 AI 对话。

## v0.1 命令

```text
/ai help
/ai on
/ai off
/ai ask <内容>
/ai model
/ai model <provider> <id>
/ai key <provider> <key>
/ai key list
/ai key remove <provider>
/ai base
/ai base <provider> <url>
/ai timeout [秒]
/ai max-history [数量]
/ai status
/ai clear
```

默认服务商是 OpenAI，默认模型是 `gpt-4.1`。
