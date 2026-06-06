# DeepSeek 接入开放问题跟踪

更新时间：2026-06-06

## 当前结论

当前项目已完成 DeepSeek Mock 写词边界、`CreativeBriefAgent` / `LyricsAgent` 审计、真实联调 Runbook 和 `RealDeepSeekLyricsClient`。真实客户端使用 OpenAI 兼容 `POST /chat/completions`，默认 `DEEPSEEK_BASE_URL=https://api.deepseek.com`、`DEEPSEEK_MODEL_NAME=deepseek-v4-pro`，并使用 `response_format=json_object` 解析歌词结构。真实 DeepSeek 调用尚未开始；执行真实联调前仍需安全注入 API Key 并确认成本止损。

## 待确认问题

| 编号 | 问题 | 当前状态 | 影响 |
|---|---|---|---|
| DQ-1 | DeepSeek API base URL | 已确认：`https://api.deepseek.com` | 真实客户端默认值已配置 |
| DQ-2 | 鉴权方式与 header 格式 | 已确认：`Authorization: Bearer <DEEPSEEK_API_KEY>` | API Key 仍只允许安全注入 |
| DQ-3 | 可用模型名与模型参数 | 已确认：默认 `deepseek-v4-pro` | temperature、max tokens 可通过 env 调整 |
| DQ-4 | 请求 / 响应 schema | 已实现：OpenAI chat completions + JSON object 内容 | 真实样本后继续校验字段稳定性 |
| DQ-5 | 是否支持 JSON mode / function calling | 已确认：`response_format=json_object` | 已用于结构化解析 |
| DQ-6 | 限流、并发和 QPS | 待确认 | 决定重试、排队和成本止损 |
| DQ-7 | 超时和最大输出长度建议 | 待确认 | 决定 `DEEPSEEK_TIMEOUT_MS` 与输入截断规则 |
| DQ-8 | 失败码与 retryable 口径 | 待真实样本 | 决定用户可见失败动作和后台重试策略 |
| DQ-9 | 计费口径与成本上限 | 待确认 | 决定单轮联调样本数量和生产保护 |
| DQ-10 | 内容安全要求 | 待确认 | 决定 DeepSeek 前后是否增加输入/输出预检 |

## 已有本地边界

- `modules:deepseek`：`DeepSeekLyricsClient`、请求/响应合约、`MockDeepSeekLyricsClient` 和 `RealDeepSeekLyricsClient`。
- `modules:lyrics`：统一写词编排，覆盖灵感成歌、填词成歌、润色、续写。
- `modules:creative-agent`：`CreativeBriefAgent` Mock 合约。
- `modules:agent-runtime`：`agent_runs` 审计基础。
- `docs/runbook/deepseek-controlled-real-integration.md`：真实联调步骤。
- `docs/security/deepseek-secret-and-log-handling.md`：凭据与日志规则。
- `docs/checklists/deepseek-real-integration-acceptance.md`：验收清单。

## 下一步

1. 用户在本地 shell 或 Secret 系统安全注入 DeepSeek API Key 后，先做 1 条灵感成歌样本。
2. 验证鉴权、JSON 解析、`agent_runs` 审计和日志脱敏。
3. 再扩展到填词、润色、续写和失败样本。
4. 根据真实 401 / 403 / 429 / timeout / JSON 解析失败样本细化 retryable 口径。
