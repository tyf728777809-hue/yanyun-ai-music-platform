# Agent Runtime Audit v0.1

更新时间：2026-06-06

## 1. 标题与元数据

- 标题：Agent Runtime 与 agent_runs 审计边界
- 作者：Codex
- 状态：Approved for local Mock implementation
- 适用范围：CreativeBriefAgent、DeepSeek 写词链路、MusicPromptAgent、CoverPromptAgent、后续 QualityEvaluationAgent、ModerationAgent 的统一审计基础
- 评审依据：PRD v0.3、技术方案 v0.2、AI Multi-Agent Creative Pipeline v0.1、DeepSeek / Knowledge Lyrics Pipeline v0.1

## 2. Context

项目已经明确后续真实模型阶段采用确定性的 Workflow / Service 编排，加专业 Agent Worker 和 Provider Adapter。当前 `provider_calls` 已能记录 Suno / MiniMax / MockMusicProvider 等外部 Provider 调用，但 DeepSeek 写词、提示词规划、质量评估、封面提示词和审核预检这类 LLM Agent 还没有统一审计边界。

真实模型受控联调前，系统需要先具备最小 `AgentRunRecorder`：每次 Agent 调用都能记录 agent 名称、版本、operation、模型名、输入输出 hash、Prompt 模板版本、耗时、状态和脱敏失败信息。首期先接入本地 Mock DeepSeek 写词链路，随后接入 `MusicPromptAgent` Mock 合约；当前已继续接入 `CreativeBriefAgent` 与 `CoverPromptAgent` Mock 合约。以上能力都不调用真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统，也不改变用户侧 OpenAPI。

## 3. Functional Requirements

- FR-1：系统 MUST 新增 `agent_runs` 表，用于记录 LLM / Agent 类调用摘要。
- FR-2：`agent_runs` MUST 支持记录逻辑关联 `work_id`，并 MAY 支持记录 `job_id`；由于创建作品时 Agent 调用可能早于 `works` 行插入，首期不对这两个字段加外键约束。
- FR-3：Agent 审计 MUST 记录 `agent_name`、`agent_version`、`operation`、`model_name`、`status`、`created_at`。
- FR-4：Agent 审计 MUST 记录输入 hash 和输出 hash，不得记录完整 Prompt、完整歌词、用户输入原文或供应商原始 payload。
- FR-5：Agent 审计 SHOULD 记录 `prompt_template_key`、`prompt_template_version`、`latency_ms`、token 数和成本字段。
- FR-6：写词链路 MUST 在每次 `DeepSeekLyricsClient.generate` 调用后记录一条 Agent run。
- FR-7：低质量内部重写触发第二次模型调用时，系统 MUST 记录两条 Agent run。
- FR-8：Agent 调用失败时，系统 MUST 记录 `FAILED`、失败码和脱敏失败信息，然后继续按原有异常路径收口。
- FR-9：自动化测试 MUST 使用 Mock/Fake，不得触发真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统。

## 4. Non-Functional Requirements

- NFR-1：Agent 审计记录不得包含真实密钥、JWT、Cookie、用户 token、AccessKey、SecretKey、完整 Prompt 或完整模型输出。
- NFR-2：失败信息入库前 MUST 截断，避免长供应商响应污染数据库和日志。
- NFR-3：新增审计不得改变当前用户侧 API 响应结构。
- NFR-4：`AgentRunRecorder` MUST 提供 No-op 实现，便于纯单元测试和后续模块隔离。
- NFR-5：本批只做本地 Mock implementation，不新增真实模型调用开关。

## 5. Acceptance Criteria

- AC-1：Given 写词链路正常调用 Mock DeepSeek，When 生成歌词，Then `AgentRunRecorder` 收到 1 条 `LyricsAgent / SUCCEEDED` 记录，覆盖 FR-3、FR-4、FR-6。
- AC-2：Given 第一次 DeepSeek 返回低质量结果，When 触发内部重写，Then `AgentRunRecorder` 收到 2 条记录，覆盖 FR-7。
- AC-3：Given DeepSeek client 抛出异常，When 写词链路失败，Then `AgentRunRecorder` 收到 1 条 `FAILED` 记录，且错误信息脱敏截断，覆盖 FR-8、NFR-2。
- AC-4：Given migration 应用，When API 启动，Then `agent_runs` 表存在，`work_id` 可记录逻辑 UUID，且约束非负 latency/token/cost 字段，覆盖 FR-1、FR-2。
- AC-5：Given 后端测试运行，When 执行 targeted Gradle 测试，Then 不发生真实外部模型或公司系统调用，覆盖 FR-9、NFR-5。

## 6. Edge Cases

- EC-1：`work_id` 不是 UUID 时，JDBC recorder SHOULD 把非法 UUID 记为 null，避免测试或早期草稿数据污染。
- EC-2：Agent 失败但审计写入失败时，后续应考虑不阻断主业务；首期 JDBC recorder 暂按普通数据库写入处理。
- EC-3：token 数未知时允许为 null。
- EC-4：Prompt 模板版本未知时允许为 null。
- EC-5：模型名未知时必须使用安全占位值，例如 `mock-deepseek-lyrics` 或 `unknown`。

## 7. API Contracts

本批不新增用户侧 OpenAPI。内部 Java 合约口径：

```ts
type AgentRunRecord = {
  work_id?: string;
  job_id?: string;
  agent_name: string;
  agent_version: string;
  operation: string;
  model_name: string;
  prompt_template_key?: string;
  prompt_template_version?: number;
  input_hash?: string;
  output_hash?: string;
  status: "QUEUED" | "RUNNING" | "SUCCEEDED" | "FAILED" | "SKIPPED";
  latency_ms?: number;
  input_tokens?: number;
  output_tokens?: number;
  cost_units?: number;
  failure_code?: string;
  failure_message?: string;
};
```

## 8. Data Models

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | UUID | primary key | Agent run id |
| `work_id` | UUID | nullable, no FK in v0.1 | 逻辑关联作品 |
| `job_id` | UUID | nullable, no FK in v0.1 | 逻辑关联生成任务 |
| `agent_name` | varchar(128) | not null | Agent 名称 |
| `agent_version` | varchar(64) | not null | Agent 版本 |
| `operation` | varchar(128) | not null | 调用场景 |
| `model_name` | varchar(128) | not null | 模型名或 mock 名 |
| `prompt_template_key` | varchar(128) | nullable | Prompt 模板 key |
| `prompt_template_version` | integer | nullable, >= 0 | Prompt 模板版本 |
| `input_hash` | varchar(256) | nullable | 输入摘要 hash |
| `output_hash` | varchar(256) | nullable | 输出摘要 hash |
| `status` | varchar(32) | not null | 调用状态 |
| `latency_ms` | integer | nullable, >= 0 | 调用耗时 |
| `input_tokens` | integer | nullable, >= 0 | 输入 token |
| `output_tokens` | integer | nullable, >= 0 | 输出 token |
| `cost_units` | numeric(18,6) | nullable, >= 0 | 成本估算 |
| `failure_code` | varchar(128) | nullable | 失败码 |
| `failure_message` | text | nullable | 脱敏失败信息 |
| `created_at` | timestamptz | not null | 创建时间 |

## 9. Out of Scope

- OS-1：不实现真实 DeepSeek API 调用。
- OS-2：不实现 Prompt 原文存档、回放后台或模型管理后台。
- OS-3：不新增用户侧接口展示 Agent run。
- OS-4：不改写 `provider_calls`；Provider 与 Agent 审计先分表记录。
- OS-5：不在本批拆分 Temporal activity；本批只建立 Agent 审计基础。
