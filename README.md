# MineMind

MineMind 是一个面向 Java 版 Minecraft 单人客户端场景的 NeoForge Mod。它允许玩家在不离开游戏的情况下，通过原版聊天栏进入 AI 对话模式，并使用玩家自己的 API Key 调用文本对话模型。

## 核心能力

- 通过客户端 `/ai` 命令管理 AI 对话。
- 支持单次提问和 AI 对话模式。
- AI 模式下普通聊天输入会被客户端拦截并发送给 AI。
- AI 回复直接显示在 Minecraft 聊天区域。
- 支持 OpenAI、DeepSeek、Qwen、KiMi、GLM、Seed、Grok、Gemini 内置 Provider。
- 支持每个 Provider 独立保存 API Key、Base URL 和模型 ID。
- 支持从 Provider 获取可用模型列表，并限制只能切换到列表中的文本对话模型。
- 支持短期上下文，默认保留最近 20 条消息。
- 支持玩家显式 `@` 快捷标记读取本地 Minecraft 文本信息，并只附加到本轮请求。
- 请求异步执行，不阻塞游戏主线程。
- API Key 只保存在本地配置文件，聊天栏和状态输出只显示尾号。

## 当前范围

已支持：

- Java 版 Minecraft `1.21.11`
- NeoForge `21.11.42`
- Java `21`
- 单人客户端使用
- 文本对话模型
- OpenAI-compatible Chat Completions 数据流
- Gemini 原生 `generateContent` 文本对话接口
- OpenAI、DeepSeek、Qwen、KiMi、GLM、Seed、Grok、Gemini
- 玩家主动触发的快捷栏、背包、位置、附近实体和准星目标文本上下文

暂不支持：

- 多人服务器权限管理、统一计费或玩家管理
- 模型自主工具调用
- 图片、语音、嵌入、实时语音等非文本模型能力
- 长期记忆和跨游戏会话历史

## 快速开始

### 1. 配置 API Key

进入单人游戏存档后，需要配置所需AI模型的API Key。示例如下：

OpenAI：

```text
/ai key openai <你的 OpenAI API Key>
```

DeepSeek：

```text
/ai key deepseek <你的 DeepSeek API Key>
```

### 2. 查看并切换模型

需要先输入指令获取当前 Provider 可用的文本对话模型列表， 示例：

```text
/ai model deepseek
```

然后选择列表中的模型：

```text
/ai model deepseek <列表中的模型 ID>
```

### 3. 开启 AI 对话模式

```text
/ai on
```

开启后，普通聊天输入会发送给 AI，而不是作为普通聊天消息发送。请求与回复格式示例：

```text
SanJin > 你好
[AI] 正在等待回复......
DeepSeek > 你好，请问有什么帮助？
```

附加本地 Minecraft 信息时，可以在输入中加入快捷标记：

```text
SanJin > @inventory 我能合成火把吗？
[AI] 已附加：背包
[AI] 正在等待回复......
DeepSeek > 你现在背包里有木棍和煤炭，可以合成火把。
```

退出 AI 模式：

```text
/ai off
```

当然你也可以不进入 AI 模式，直接单次提问：

```text
/ai ask 解释一下红石比较器怎么用
```

## 命令列表

| 命令 | 说明 |
| --- | --- |
| `/ai help` | 查看 MineMind 命令帮助 |
| `/ai on` | 开启 AI 对话模式 |
| `/ai off` | 关闭 AI 对话模式 |
| `/ai ask <内容>` | 单次向 AI 提问，不切换模式 |
| `/ai model` | 查看当前 Provider 和模型 |
| `/ai model <provider>` | 获取该 Provider 当前可用文本模型列表 |
| `/ai model <provider> <id>` | 切换到模型列表中的指定模型 |
| `/ai key <provider> <key>` | 设置 Provider API Key |
| `/ai key list` | 查看已配置 API Key 的 Provider，只显示尾号 |
| `/ai key remove <provider>` | 删除 Provider API Key |
| `/ai base` | 查看当前 Provider 的 API Base URL |
| `/ai base <provider> <url>` | 设置 Provider API Base URL |
| `/ai timeout` | 查看当前请求超时 |
| `/ai timeout <秒>` | 设置请求超时，范围 `5-120` 秒 |
| `/ai max-history` | 查看最大上下文消息数 |
| `/ai max-history <数量>` | 设置最大上下文消息数，范围 `2-100` |
| `/ai status` | 查看 AI 模式、请求状态、Provider、模型、Key、上下文等状态 |
| `/ai clear` | 清空当前会话上下文 |

## 快捷工具

MineMind v0.5 支持玩家主动触发的本地文本工具。工具只会在本轮请求中附加给模型。

| 标记 | 附加内容 |
| --- | --- |
| `@hotbar` | 读取 9 格快捷栏，包含槽位、物品 ID、显示名、数量、耐久、组件和最多 8 个 tag |
| `@inventory` | 读取快捷栏、主背包、护甲和副手，合并同类堆叠并限制长度 |
| `@here` | 读取维度、整数坐标、朝向、生态群系和脚下方块 |
| `@nearby` | 读取 16 格内客户端可见实体，按类型汇总数量、最近距离、类别和是否敌对 |
| `@target` | 读取 20 格内准星命中的方块或实体 |

使用规则：

- 只有显式输入已知标记才会读取本地信息。
- 已知标记只要作为独立文本出现即可触发，位置不限。
- 标记大小写不敏感，未知 `@xxx` 会作为普通文本保留。
- 同一标记重复出现时只执行一次。
- `@inventory` 已包含快捷栏信息，同时输入 `@hotbar` 时不会重复附加快捷栏。
- 只输入工具标记时，MineMind 会发送默认问题：`请根据已附加的 Minecraft 信息进行简短说明。`

聊天区域示意：

```text
┌──────────── Minecraft 聊天区域 ────────────┐
│ SanJin > @here @nearby 附近适合建家吗？     │
│ [AI] 已附加：位置、附近生物                 │
│ [AI] 正在等待回复......                     │
│ DeepSeek > 这里地势平稳，但附近有敌对生物... │
└───────────────────────────────────────────┘
```

### 自定义 Provider

配置文件中已有的未知 Provider 会被保留，自定义 Provider 需要手动配置：

- API Key
- Base URL
- 模型 ID 必须来自该 Provider 的 `/models` 返回结果

命令示例：

```text
/ai key my-provider <api-key>
/ai base my-provider https://example.com/v1
/ai model my-provider <模型列表中的模型>
```

## 本地配置

配置文件路径：

```text
<Minecraft 游戏目录>/config/minemind.json
```

配置内容包含：

- 当前 Provider
- 当前模型 ID
- 各 Provider API Key
- 各 Provider Base URL
- 请求超时时间
- 最大上下文消息数
- AI 模式开关
- streaming 字段占位

示例结构：

```json
{
  "currentProvider": "deepseek",
  "timeoutSeconds": 45,
  "maxHistoryMessages": 20,
  "streaming": false,
  "aiMode": false,
  "providers": {
    "openai": {
      "displayName": "OpenAI",
      "model": "",
      "baseUrl": "https://api.openai.com/v1",
      "apiKey": ""
    },
    "deepseek": {
      "displayName": "DeepSeek",
      "model": "",
      "baseUrl": "https://api.deepseek.com",
      "apiKey": ""
    },
    "qwen": {
      "displayName": "Qwen",
      "model": "",
      "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
      "apiKey": ""
    },
    "kimi": {
      "displayName": "KiMi",
      "model": "",
      "baseUrl": "https://api.moonshot.ai/v1",
      "apiKey": ""
    },
    "glm": {
      "displayName": "GLM",
      "model": "",
      "baseUrl": "https://open.bigmodel.cn/api/paas/v4",
      "apiKey": ""
    },
    "seed": {
      "displayName": "Seed",
      "model": "",
      "baseUrl": "https://ark.cn-beijing.volces.com/api/v3",
      "apiKey": ""
    },
    "grok": {
      "displayName": "Grok",
      "model": "",
      "baseUrl": "https://api.x.ai/v1",
      "apiKey": ""
    },
    "gemini": {
      "displayName": "Gemini",
      "model": "",
      "baseUrl": "https://generativelanguage.googleapis.com/v1beta",
      "apiKey": ""
    }
  }
}
```

配置加载时会自动修复部分非法字段：

- 非法 timeout 会恢复到合法范围。
- 非法 max history 会恢复到合法范围。
- 内置 Provider 的非法 Base URL 会恢复为默认值。
- 自定义 Provider 的非法 Base URL 会被清空。

## 安全说明

- MineMind 只支持 API Key，不支持账号密码登录。
- API Key 以明文形式保存在本地 `config/minemind.json`。
- API Key 不会上传到服务器，不会同步到多人服务器，也不会显示完整内容。
- `/ai status` 和 `/ai key list` 只显示 Key 尾号。
- 日志和聊天输出不应包含完整 API Key。
- 不要把包含真实 API Key 的配置文件提交到 Git 仓库或分享给他人。

## 错误提示

MineMind 常见错误映射提示：

| 类型 | 典型原因 |
| --- | --- |
| 网络错误 | 断网、DNS、代理、TLS 或连接失败 |
| 认证错误 | API Key 无效、过期、权限不足 |
| 模型错误 | 模型不存在或不支持文本对话 |
| 额度错误 | 余额不足、配额耗尽、限流 |
| 请求错误 | 参数非法、上下文过长 |
| 服务错误 | Provider 5xx、模型暂不可用 |
| 本地错误 | 配置损坏、Base URL 无效、响应解析失败 |
