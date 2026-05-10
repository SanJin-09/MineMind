# MineMind TODO

从当前 `v0.1` 到完整需求版本的执行计划。

## 当前状态：v0.4 已完成

- [x] 清理 NeoForge 模板示例方块、物品、创造栏、配置和示例文案。
- [x] 初始化客户端 `/ai` 命令框架。
- [x] 支持 `/ai help/on/off/ask/model/key/status/clear/base`。
- [x] 支持单人客户端 AI 模式聊天拦截。
- [x] 支持 OpenAI-compatible Chat Completions 请求。
- [x] 支持本地 JSON 配置、API Key 保存和尾号隐藏。
- [x] 支持短期上下文、异步请求、错误分类和长回复分段。
- [x] 聊天前缀和正文分色，AI 模式消息前缀保持 `[AI]`，模型回复显示 Provider 名称。
- [x] 设置 API Key 后自动获取并缓存可用文本模型列表。
- [x] 内置 OpenAI、DeepSeek、Qwen、KiMi、GLM、Seed、Grok、Gemini Provider。

## v0.2：补齐 MVP 稳定性

- [x] 增加 `/ai key list` 或增强 `/ai status`，让已配置服务商状态更清晰。
- [x] 增加 `/ai base` 查看当前服务商 Base URL 的无参形式。
- [x] 增加 `/ai timeout <秒>`，允许玩家调整请求超时。
- [x] 增加 `/ai max-history <数量>`，允许玩家调整上下文最大消息数。
- [x] 校验配置文件字段范围，自动修复非法 timeout、history、base URL。
- [x] 在请求开始时显示“正在请求”提示，并在完成或失败时结束该状态。
- [x] 确认所有异常路径都会释放请求锁，避免后续请求一直提示处理中。
- [x] 为配置读写、错误分类、上下文裁剪添加单元测试或轻量测试入口。

## v0.3：Provider 抽象完善

- [x] 将 OpenAI-compatible 请求抽象为统一 `AiProvider` 接口。
- [x] 将服务商预设迁移到独立注册表，避免散落在配置类中。
- [x] 每个 Provider 支持默认 display name 和 base URL。
- [x] 支持 Provider 级别 API Key、Base URL、模型 ID 独立保存。
- [x] 增加 Provider 建议补全，命令输入时能补全内置服务商。

## v0.4：主流服务商接入

- [x] DeepSeek：已在 v0.3 按 OpenAI-compatible Provider 接入。
- [x] Qwen / 通义千问：按兼容接口接入，模型只能从服务商 API 返回的可用纯文本模型列表中选择。
- [x] KiMi / Moonshot：按兼容接口接入，模型只能从服务商 API 返回的可用纯文本模型列表中选择。
- [x] GLM / 智谱：按兼容接口接入，模型只能从服务商 API 返回的可用纯文本模型列表中选择。
- [x] Seed / 豆包 / 火山：按兼容接口接入，模型只能从服务商 API 返回的可用纯文本模型列表中选择。
- [x] Grok / xAI：按兼容接口接入，模型只能从服务商 API 返回的可用纯文本模型列表中选择。
- [x] Gemini / Google：实现 Gemini 原生文本对话接口适配。
- [x] 为每个 Provider 验证认证失败、模型不存在、限流、5xx、空回复的错误映射。

## v0.5：聊天体验和上下文

- [ ] 优化 `/ai help` 排版，按“模式、模型、密钥、状态、上下文”分组显示。
- [ ] 优化 `/ai status`，展示当前模式、服务商、模型、Base URL、Key 状态、上下文条数。
- [ ] 支持 `/ai clear` 后明确显示已删除多少条上下文。
- [ ] AI 模式下保留最近 20 条上下文，且允许配置上限。
- [ ] 每次进入世界默认新会话，不加载长期历史。
- [ ] 玩家输入普通文本时只发送给 AI，不向多人服务器发送。
- [ ] 在多人服务器中禁用 AI 模式，并给出明确提示。
- [ ] 长回复分段时保持前缀一致，避免单条消息过长导致显示难读。

## v0.6：安全和隐私

- [ ] 全面检查日志路径，确保 API Key 不出现在日志、异常、调试输出中。
- [ ] 配置文件只保存在本地 Minecraft `config/minemind.json`。
- [ ] 不读取玩家账号密码，不实现账号密码登录。
- [ ] 不上传配置文件，不与服务器同步 API Key。
- [ ] `/ai status`、错误提示、调试信息只显示 Key 尾号。
- [ ] 对配置文件损坏、JSON 解析失败、写入失败给出可理解的聊天栏提示。
- [ ] 文档明确说明 API Key 本地明文保存的风险和文件位置。

## v0.7：配置和可维护性

- [ ] 整理配置 schema，保留 `currentProvider`、`providers`、`timeoutSeconds`、`maxHistoryMessages`、`streaming`。
- [ ] 支持未来配置迁移版本号，例如 `configVersion`。
- [ ] 添加 README 使用指南：安装、启动、设置 Key、切换模型、常见错误。
- [ ] 添加 Provider 配置示例，说明如何填写兼容接口 Base URL。
- [ ] 添加贡献说明或开发说明，包含 `sh gradlew runClient`、`compileJava`、`build`。
- [ ] 统一中文文案风格，避免命令提示和错误提示口径不一致。

## v0.8：质量验证

- [ ] 建立手动测试清单：单人世界、多人服务器、无 Key、错 Key、错模型、断网、超时。
- [ ] 建立命令回归清单：`help/on/off/ask/model/key/remove/status/clear/base`。
- [ ] 验证聊天拦截不会影响 `/` 开头的原版命令。
- [ ] 验证退出世界、切换世界、死亡重生后上下文和请求状态正常。
- [ ] 验证请求异步执行，不阻塞游戏移动、视角和聊天输入。
- [ ] 验证构建产物不包含模板示例内容。
- [ ] 每次发布前执行 `sh gradlew clean build`。

## v1.0：完整需求版验收

- [ ] 单人客户端场景下，玩家可通过原版聊天栏进入 AI 对话模式。
- [ ] 普通聊天在 AI 模式下被客户端拦截，只发送给 AI。
- [ ] AI 回复以 Minecraft 聊天消息显示。
- [ ] 支持 API Key 设置、删除、隐藏显示。
- [ ] 支持 OpenAI、Gemini、Grok、DeepSeek、Qwen、KiMi、Seed、GLM。
- [ ] 模型型号只能从所选服务商 API 返回的可用纯文本模型列表中选择。
- [ ] 支持每个服务商独立 API Key 和 Base URL。
- [ ] 支持明确错误分类：网络、认证、模型、额度、请求、服务、本地。
- [ ] 支持短期上下文、清空上下文、默认新会话。
- [ ] 不新增实体、方块、物品。
- [ ] 不支持多人服务器统一计费、玩家管理、账号密码登录、图片、语音、长期记忆。
- [ ] 所有核心命令、错误提示和配置行为都有验证记录。
