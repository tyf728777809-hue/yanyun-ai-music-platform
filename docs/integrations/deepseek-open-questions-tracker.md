# DeepSeek 接入开放问题跟踪

更新时间：2026-06-06

## 当前结论

当前项目已完成 DeepSeek Mock 写词边界、`CreativeBriefAgent` / `LyricsAgent` 审计和真实联调 Runbook。真实 DeepSeek 调用尚未开始；执行真实联调前需要用户提供供应商 URL、API Key 和协议细节，并由代码补齐真实客户端硬开关。

## 待确认问题

| 编号 | 问题 | 当前状态 | 影响 |
|---|---|---|---|
| DQ-1 | DeepSeek API base URL | 待用户提供 | 决定真实客户端目标地址 |
| DQ-2 | 鉴权方式与 header 格式 | 待用户提供 | 决定 API Key 注入和请求构造 |
| DQ-3 | 可用模型名与模型参数 | 待用户提供 | 决定 `DEEPSEEK_MODEL_NAME`、temperature、max tokens 等参数 |
| DQ-4 | 请求 / 响应 schema | 待用户提供 | 决定真实响应解析和结构化输出校验 |
| DQ-5 | 是否支持 JSON mode / function calling | 待确认 | 影响歌词、摘要、music prompt 和风险提示的解析稳定性 |
| DQ-6 | 限流、并发和 QPS | 待确认 | 决定重试、排队和成本止损 |
| DQ-7 | 超时和最大输出长度建议 | 待确认 | 决定 `DEEPSEEK_TIMEOUT_MS` 与输入截断规则 |
| DQ-8 | 失败码与 retryable 口径 | 待真实样本 | 决定用户可见失败动作和后台重试策略 |
| DQ-9 | 计费口径与成本上限 | 待确认 | 决定单轮联调样本数量和生产保护 |
| DQ-10 | 内容安全要求 | 待确认 | 决定 DeepSeek 前后是否增加输入/输出预检 |

## 已有本地边界

- `modules:deepseek`：`DeepSeekLyricsClient`、请求/响应合约和 `MockDeepSeekLyricsClient`。
- `modules:lyrics`：统一写词编排，覆盖灵感成歌、填词成歌、润色、续写。
- `modules:creative-agent`：`CreativeBriefAgent` Mock 合约。
- `modules:agent-runtime`：`agent_runs` 审计基础。
- `docs/runbook/deepseek-controlled-real-integration.md`：真实联调步骤。
- `docs/security/deepseek-secret-and-log-handling.md`：凭据与日志规则。
- `docs/checklists/deepseek-real-integration-acceptance.md`：验收清单。

## 下一步

1. 用户提供 DeepSeek URL、API Key 和协议文档后，先补真实客户端规格，不直接进入大批量联调。
2. 实现 `RealDeepSeekLyricsClient` 和必要真实 Agent 实现，默认保持关闭。
3. 用单条灵感成歌样本验证鉴权、解析、审计和日志脱敏。
4. 再扩展到填词、润色、续写和失败样本。
