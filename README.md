<div align="center">

#  MineMind

**在 Minecraft Java 单人客户端中，直接通过聊天栏与 AI 对话的 NeoForge Mod。**

MineMind 允许玩家在不离开游戏的情况下，通过原版聊天栏进入 AI 对话模式，并使用自己的 API Key 调用文本对话模型；在支持图片输入的模型上，也可以显式附带当前游戏画面。  
它面向 **本地、安全、可控** 的 AI 辅助游戏体验：聊天、读取游戏文本上下文、记录长期记忆，既可以由玩家快捷标记强制触发，也可以由模型在白名单工具内自主请求。

<br />

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62B47A?style=for-the-badge&logo=minecraft&logoColor=white)
![NeoForge](https://img.shields.io/badge/NeoForge-21.11.42-F16436?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Client](https://img.shields.io/badge/Mode-Singleplayer_Client-409EFF?style=for-the-badge)

</div>

---

## 简介

**MineMind** 是一个面向 **Java 版 Minecraft 单人客户端场景** 的 AI 对话 Mod。  
玩家可以使用 `/ai` 命令管理 AI 模型、API Key、对话上下文和长期记忆；开启 AI 模式后，普通聊天输入会被客户端拦截并发送给 AI，AI 回复会直接显示在 Minecraft 聊天区域。

> [!NOTE]
> MineMind 不提供模型服务，也不托管用户数据。  
> 需要自行配置对应 Provider 的 API Key，所有 Key 和记忆内容均保存在本地配置文件中。

---

## 核心功能

<table>
  <tr>
    <td width="50%">
      <h3>游戏内 AI 对话</h3>
      <p>通过原版聊天栏直接进入 AI 对话模式，无需离开游戏，也没有任何多余的Screen。</p>
    </td>
    <td width="50%">
      <h3>多 Provider 管理</h3>
      <p>我们内置支持了 OpenAI、DeepSeek、Qwen、KiMi、GLM、Seed、Grok、Gemini 的 Provider。</p>
    </td>
  </tr>
  <tr>
    <td width="50%">
      <h3>本地游戏上下文</h3>
      <p>玩家可显式使用 <code>@hotbar</code>、<code>@inventory</code>、<code>@here</code> 等标记附加游戏信息，模型也能在白名单内自主调用文本工具。</p>
    </td>
    <td width="50%">
      <h3>长期记忆</h3>
      <p>支持本地 Markdown 长期记忆，可显式写入、读取和遗忘记忆条目。</p>
    </td>
  </tr>
  <tr>
    <td width="50%">
      <h3>可配置请求参数</h3>
      <p>支持配置 API Key、Base URL、模型 ID、请求超时、最大上下文消息数和截图质量。</p>
    </td>
    <td width="50%">
      <h3>显式图片输入</h3>
      <p>使用 <code>@image</code> 可截取当前游戏画面，并与文本一起发送给支持图片输入的模型。</p>
    </td>
  </tr>
</table>

## ✅ 当前支持范围

### 已支持

- [x] Java 版 Minecraft `1.21.11`
- [x] NeoForge `21.11.42`
- [x] Java `21`
- [x] 单人客户端使用
- [x] 文本对话模型
- [x] 支持图片输入模型的 `@image` 当前画面截图请求
- [x] OpenAI-compatible Chat Completions 数据流
- [x] Gemini 原生 `generateContent` 对话接口
- [x] OpenAI、DeepSeek、Qwen、KiMi、GLM、Seed、Grok、Gemini
- [x] 每个 Provider 独立保存 API Key、Base URL 和模型 ID
- [x] 从 Provider 获取可用模型列表，并限制切换到列表中的文本对话模型
- [x] 在模型列表中为支持图片识别的模型追加 `[图片]` 标记
- [x] 默认保留最近 `20` 条短期上下文消息
- [x] 玩家主动触发的快捷栏、背包、位置、附近实体和准星目标文本上下文
- [x] 玩家主动触发的长期记忆写入、读取和删除
- [x] 模型自主工具调用，支持位置、背包、准星目标、附近实体和长期记忆等白名单工具
- [x] API Key 本地保存，聊天栏和状态输出只显示尾号

### 暂不支持

- [ ] 多人服务器权限管理、统一计费或玩家管理
- [ ] 语音、嵌入、实时语音等非文本模型能力
- [ ] 跨游戏自动会话历史

> [!IMPORTANT]
> MineMind 当前只支持是 **单人客户端 AI 辅助 Mod**，暂不支持多人服务器 AI 托管。

## 快速开始

### 1. 配置 API Key

进入单人游戏存档后，先配置所需 AI Provider 的 API Key。

示例：

#### OpenAI

```text
/ai key openai <你的 OpenAI API Key>
```

#### DeepSeek

```text
/ai key deepseek <你的 DeepSeek API Key>
```

查看已配置 Key 的 Provider：

```text
/ai key list
```

> [!WARNING]
> API Key 会以明文形式保存在本地配置文件中。  
> 请不要把您的配置文件分享给他人！

### 2. 查看并切换模型

先获取当前 Provider 可用的文本对话模型列表：

```text
/ai model deepseek
```

然后切换到列表中的指定模型：

```text
/ai model deepseek <列表中的模型 ID>
```

查看当前 Provider 和模型：

```text
/ai model
```

模型列表中带 `[图片]` 的型号支持 `@image`：

```text
[AI] 标记：[图片] 支持 @image 图片识别
[AI] glm-4.6v [图片], glm-4v-plus-0111 [图片], glm-4.7
```

### 3. 开启 AI 对话模式

```text
/ai on
```

开启后，普通聊天输入会发送给 AI，而不是作为普通聊天消息发送。

```text
SanJin > 你好
[AI] 正在等待回复......
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

### 4. 附加本地 Minecraft 信息

在输入中加入快捷标记，即可把本地游戏信息强制附加到本轮 AI 请求中。

```text
SanJin > @inventory 我能合成火把吗？
[AI] 已附加：背包
[AI] 正在等待回复......
DeepSeek > 你现在背包里有木棍和煤炭，可以合成火把。
```

常用标记示例：

```text
@hotbar     读取快捷栏
@inventory  读取背包
@here       读取当前位置
@nearby     读取附近实体
@target     读取准星目标
@image      截取当前游戏画面
```

v0.8 起，模型也可以在普通对话中自主请求白名单工具。例如玩家直接问“我现在在哪个生态群系？”，模型可以调用位置工具后再回答。

```text
┌──────────── Minecraft 聊天区域 ────────────┐
│ SanJin > 我现在在哪个生态群系？             │
│ [AI] 正在等待回复......                    │
│ [AI] 模型调用工具：位置                    │
│ DeepSeek > 你当前位于樱花林生态群系。       │
└───────────────────────────────────────────┘
```

附带图片时，MineMind 会提示本轮请求可能更慢：

```text
┌──────────── Minecraft 聊天区域 ────────────┐
│ SanJin > @image 这个画面里我该注意什么？   │
│ [AI] 已附加：图片                          │
│ [AI] 附带图片的请求回复速度会稍慢           │
│ [AI] 截图可能包含聊天栏、坐标或服务器信息   │
│ [AI] 正在等待回复......                    │
│ OpenAI > 你当前画面中...                   │
└───────────────────────────────────────────┘
```

### 5. 使用长期记忆

MineMind 支持把玩家显式指定的信息写入本地 Markdown 长期记忆文件。

#### 写入记忆

```text
SanJin > @remember @here 我的家在樱花林旁边
[AI] 尝试记忆写入，记录日期：2026-05-12
[AI] 已附加：位置
[AI] 记忆成功写入！本次写入内容：玩家的家位于当前坐标附近，靠近樱花林。
```

#### 读取记忆

```text
SanJin > @memory 我的家附近适合做什么？
[AI] 已附加：长期记忆
[AI] 正在等待回复......
DeepSeek > 你之前记录的家靠近樱花林，可以考虑做一条赏花小路...
```

#### 删除记忆

```text
SanJin > @forget 删除我的家位置
[AI] 尝试记忆删除，记录日期：2026-05-12
[AI] 已附加：长期记忆
[AI] 记忆成功删除！本次删除内容：2026-05-12 14:30 | 玩家: SanJin | 玩家的家位于当前坐标附近，靠近樱花林。
```

## 🕹️ 命令列表

### 模式与对话

| 命令 | 说明 |
| --- | --- |
| `/ai help` | 查看 MineMind 命令帮助 |
| `/ai on` | 开启 AI 对话模式 |
| `/ai off` | 关闭 AI 对话模式 |
| `/ai ask <内容>` | 单次向 AI 提问，不切换模式 |
| `/ai clear` | 清空当前会话上下文 |
| `/ai status` | 查看 AI 模式、请求状态、Provider、模型、Key、上下文等状态 |

### 模型与 Provider

| 命令 | 说明 |
| --- | --- |
| `/ai model` | 查看当前 Provider 和模型 |
| `/ai model <provider>` | 获取该 Provider 当前可用文本模型列表 |
| `/ai model <provider> <id>` | 切换到模型列表中的指定模型 |

### API Key 与 Base URL

| 命令 | 说明 |
| --- | --- |
| `/ai key <provider> <key>` | 设置 Provider API Key |
| `/ai key list` | 查看已配置 API Key 的 Provider，只显示尾号 |
| `/ai key remove <provider>` | 删除 Provider API Key |
| `/ai base` | 查看当前 Provider 的 API Base URL |
| `/ai base <provider> <url>` | 设置 Provider API Base URL |

### 请求配置

| 命令 | 说明 |
| --- | --- |
| `/ai timeout` | 查看当前请求超时 |
| `/ai timeout <秒>` | 设置请求超时，范围 `5-120` 秒 |
| `/ai max-history` | 查看最大上下文消息数 |
| `/ai max-history <数量>` | 设置最大上下文消息数，范围 `2-100` |
| `/ai image-quality` | 查看当前截图质量 |
| `/ai image-quality <low\|medium\|high>` | 设置 `@image` 截图质量 |

## 快捷工具

MineMind 支持玩家主动触发的本地文本工具，也支持模型在白名单内自主请求工具。工具结果只会在**本轮请求**中提供给模型，不会自动写入短期对话历史。

| 标记 | 附加内容 |
| --- | --- |
| `@hotbar` | 读取 9 格快捷栏，包含槽位、物品 ID、显示名、数量、耐久、组件和最多 8 个 tag |
| `@inventory` | 读取快捷栏、主背包、护甲和副手，合并同类堆叠并限制长度 |
| `@here` | 读取维度、整数坐标、朝向、生态群系和脚下方块 |
| `@nearby` | 读取 16 格内客户端可见实体，按类型汇总数量、最近距离、类别和是否敌对 |
| `@target` | 读取 20 格内准星命中的方块或实体 |
| `@image` | 截取当前游戏画面，并作为图片输入附加到本轮请求 |

### 模型自主工具

默认开启。模型可以自主调用以下工具：

| 工具 ID | 内容 |
| --- | --- |
| `tool.hotbar.read` | 读取快捷栏 |
| `tool.inventory.read` | 读取背包 |
| `tool.location.read` | 读取当前位置 |
| `tool.entities.nearby` | 读取附近实体 |
| `tool.target.read` | 读取准星目标 |
| `tool.memory.read` | 读取长期记忆 |
| `tool.memory.write` | 写入长期记忆 |
| `tool.memory.delete` | 删除长期记忆 |

执行自主工具时，聊天栏会提示：

```text
[AI] 模型调用工具：背包、位置、长期记忆
[AI] 已写入长期记忆：玩家的基地位于樱花林附近。
[AI] 已删除长期记忆：1 条
```

### 使用规则

- 玩家显式输入已知标记时，MineMind 会强制读取对应本地信息。
- 普通对话中，模型可以在白名单内自主请求文本工具。
- 模型每轮最多请求 `4` 个工具，单次对话最多进行 `3` 轮工具调用。
- 单次对话最多允许 `1` 次长期记忆写入或删除。
- 已知标记只要作为独立文本出现即可触发，位置不限。
- 已知标记可以写成 `@image` 或 `@image:` 这两种形式。
- 标记大小写不敏感。
- 未知 `@xxx` 会作为普通文本保留。
- 同一标记重复出现时只执行一次。
- `@inventory` 已包含快捷栏信息，同时输入 `@hotbar` 时不会重复附加快捷栏。
- `@image` 只在当前模型支持图片输入时可用；当前已适配 OpenAI、Gemini，以及按 OpenAI-compatible 格式接入的 Qwen、KiMi、GLM、Seed 等视觉对话模型。
- `@image` 不属于模型自主工具，模型不能主动截图。
- `@image` 会使用当前游戏画面截图，可能包含聊天栏、坐标或服务器信息。
- 附带图片的请求体更大，回复速度通常会比纯文本请求稍慢。
- 只输入工具标记时，MineMind 会发送默认问题：

```text
请根据已附加的 Minecraft 信息进行简短说明。
```

## 长期记忆

长期记忆保存在本地 Markdown 文件中：

```text
<Minecraft 游戏目录>/config/minemind-memory.md
```

| 标记 | 行为 |
| --- | --- |
| `@remember <内容>` | 调用当前模型整理内容后写入长期记忆，聊天栏返回最终写入内容 |
| `@memory <问题>` | 读取相关长期记忆并附加到本轮 AI 请求 |
| `@forget <内容>` | 调用当前模型理解删除意图，选择候选长期记忆条目后由后端删除 |

### 记忆规则

- `@remember` 和 `@forget` 只在输入开头作为第一个标记时触发。
- `@remember` 支持联动 `@hotbar`、`@inventory`、`@here`、`@nearby`、`@target`。
- `@remember` 会根据玩家输入和已附加工具结果整理记忆。
- `@remember` 会消耗一次当前模型 API 请求；模型整理失败时不会写入文件。
- `@remember` 不会转发模型回复，只显示写入状态和最终写入内容。
- `@forget` 会把删除意图和候选记忆条目交给当前模型理解，再由后端按模型返回的条目编号删除。
- `@forget` 模型失败时不会修改文件。
- `@forget` 也支持联动 `@hotbar`、`@inventory`、`@here`、`@nearby`、`@target`。
- `@memory` 可作为独立标记出现在输入任意位置。
- `@memory` 读取时优先匹配玩家问题中的关键词，再用最近条目补齐，最多附加 `20` 条。
- 长期记忆不会自动进入短期上下文；只有显式使用 `@memory` 才会附加到本轮请求。
- 只输入 `@memory` 时，MineMind 会发送默认问题：

```text
请根据已附加的长期记忆进行简短说明。
```

> [!CAUTION]
> 长期记忆文件是本地明文 Markdown。  
> 不建议写入真实身份信息、账号凭据、隐私地址或其他敏感内容！

## 自定义 Provider

配置文件中已有的未知 Provider 会被保留。  
如果需要添加自定义 Provider，需要手动配置以下内容：

- API Key
- Base URL
- 模型 ID  
  - 模型 ID 必须来自该 Provider 的 `/models` 返回结果。

命令示例：

```text
/ai key my-provider <api-key>
/ai base my-provider https://example.com/v1
/ai model my-provider <模型列表中的模型>
```

---

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
- 截图质量：`low`、`medium`、`high`
- AI 模式开关
- `streaming` 字段占位

<details>
<summary><strong>点击展开配置示例</strong></summary>

```json
{
  "currentProvider": "deepseek",
  "timeoutSeconds": 45,
  "maxHistoryMessages": 20,
  "imageQuality": "medium",
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

</details>

### 自动修复规则

配置加载时会自动修复部分非法字段：

- 非法 `timeout` 会恢复到合法范围。
- 非法 `max history` 会恢复到合法范围。
- 非法 `imageQuality` 会恢复为 `medium`。
- 内置 Provider 的非法 Base URL 会恢复为默认值。
- 自定义 Provider 的非法 Base URL 会被清空。

## 错误提示

如果您在游戏内遇到了报错提示，可以通过下表进行排查。

| 类型 | 典型原因 |
| --- | --- |
| 网络错误 | 断网、DNS、代理、TLS 或连接失败 |
| 认证错误 | API Key 无效、过期、权限不足 |
| 模型错误 | 模型不存在或不支持文本对话 |
| 额度错误 | 余额不足、配额耗尽、限流 |
| 请求错误 | 参数非法、上下文过长 |
| 服务错误 | Provider 5xx、模型暂不可用 |
| 本地错误 | 配置损坏、Base URL 无效、响应解析失败 |

---

### MineMind

**让 Minecraft 聊天栏成为你的本地 AI 助手入口。**

</div>
