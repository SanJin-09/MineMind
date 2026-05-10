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

暂不支持：

- 多人服务器权限管理、统一计费或玩家管理
- 工具调用、世界状态读取
- 图片、语音、嵌入、实时语音等非文本模型能力
- 长期记忆和跨游戏会话历史

## 快速开始

### 1. 启动开发客户端

```bash
sh gradlew runClient
```

### 2. 进入单人世界

MineMind v0.x 面向单人游戏测试。进入世界后，聊天栏会显示当前 AI 配置摘要。

### 3. 配置 API Key

OpenAI：

```text
/ai key openai <你的 OpenAI API Key>
```

DeepSeek：

```text
/ai key deepseek <你的 DeepSeek API Key>
```

其他内置 Provider：

```text
/ai key qwen <你的 Qwen API Key>
/ai key kimi <你的 KiMi API Key>
/ai key glm <你的 GLM API Key>
/ai key seed <你的 Seed API Key>
/ai key grok <你的 Grok API Key>
/ai key gemini <你的 Gemini API Key>
```

### 4. 查看并切换模型

先获取当前 Provider 可用的文本对话模型列表：

```text
/ai model deepseek
```

然后选择列表中的模型：

```text
/ai model deepseek <列表中的模型 ID>
```

### 5. 开启 AI 对话模式

```text
/ai on
```

开启后，普通聊天输入会发送给 AI，而不是作为普通聊天消息发送。AI 回复格式示例：

```text
DeepSeek > 你好，请问有什么帮助？
```

退出 AI 模式：

```text
/ai off
```

也可以不进入 AI 模式，直接单次提问：

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

## Provider

内置 Provider 不再设置默认推荐模型。玩家必须先执行 `/ai model <provider>`，从服务商 API 返回的可用纯文本模型列表中选择型号。

| Provider ID | Display Name | 默认 Base URL | 接口类型 |
| --- | --- | --- | --- |
| `openai` | `OpenAI` | `https://api.openai.com/v1` | OpenAI-compatible |
| `deepseek` | `DeepSeek` | `https://api.deepseek.com` | OpenAI-compatible |
| `qwen` | `Qwen` | `https://dashscope.aliyuncs.com/compatible-mode/v1` | OpenAI-compatible |
| `kimi` | `KiMi` | `https://api.moonshot.ai/v1` | OpenAI-compatible |
| `glm` | `GLM` | `https://open.bigmodel.cn/api/paas/v4` | OpenAI-compatible |
| `seed` | `Seed` | `https://ark.cn-beijing.volces.com/api/v3` | OpenAI-compatible |
| `grok` | `Grok` | `https://api.x.ai/v1` | OpenAI-compatible |
| `gemini` | `Gemini` | `https://generativelanguage.googleapis.com/v1beta` | Gemini native |

如果你的账号所属平台区域使用不同 API 域名，可以通过 `/ai base <provider> <url>` 覆盖 Base URL。

### 自定义 Provider

配置文件中已有的未知 Provider 会被保留，并按 OpenAI-compatible fallback 处理。自定义 Provider 需要手动配置：

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

MineMind 会把常见错误映射为可读中文提示：

| 类型 | 典型原因 |
| --- | --- |
| 网络错误 | 断网、DNS、代理、TLS 或连接失败 |
| 认证错误 | API Key 无效、过期、权限不足 |
| 模型错误 | 模型不存在或不支持文本对话 |
| 额度错误 | 余额不足、配额耗尽、限流 |
| 请求错误 | 参数非法、上下文过长 |
| 服务错误 | Provider 5xx、模型暂不可用 |
| 本地错误 | 配置损坏、Base URL 无效、响应解析失败 |

如果你的 API Key 没有额度，聊天栏应显示额度不足或限流相关提示，而不是模型不存在。

## 开发与验证

常用命令：

```bash
sh gradlew runClient
sh gradlew runAiSelfTest
sh gradlew compileJava
sh gradlew build
```

推荐提交前至少执行：

```bash
sh gradlew runAiSelfTest
sh gradlew compileJava
sh gradlew build
```

项目内置轻量自测入口：

```text
src/test/java/com/sanjin/minemind/ai/AiSelfTest.java
```

当前自测覆盖：

- Provider 注册表
- OpenAI、DeepSeek、Qwen、KiMi、GLM、Seed、Grok、Gemini 默认配置
- timeout / max history 规则
- Base URL 修复
- HTTP 错误分类
- 文本模型过滤
- 上下文裁剪与清空

## 路线图

下一阶段计划：

- 继续优化聊天帮助、状态展示和错误提示。
- 添加 README 以外的用户手册和发布检查清单。
- 建立更完整的手动回归测试矩阵。

长期目标：

- 保持 Minecraft 原版聊天栏作为主要交互入口。
- 保持客户端本地配置和玩家自有 API Key 模式。
- 不引入实体、方块、物品等与 AI 对话无关的游戏内容。
