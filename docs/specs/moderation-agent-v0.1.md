# ModerationAgent v0.1

更新时间：2026-06-06

## 1. 标题与元数据

- 标题：音乐 Prompt AI 预检 Agent Mock 合约
- 作者：Codex
- 状态：Approved for implementation
- 适用范围：确认出歌后、调用 Suno / MiniMax / MockMusicProvider 前的音乐 Prompt 风险预检
- 关联文档：
  - `docs/specs/ai-agent-architecture-direction-v0.1.md`
  - `docs/specs/ai-agent-orchestration-engineering-design-v0.1.md`
  - `docs/specs/ai-multi-agent-creative-pipeline-v0.1.md`
  - `docs/specs/agent-runtime-audit-v0.1.md`

## 2. 背景

当前输入、歌词和发布包审核由 `ModerationAdapter` 持有，代表公司审核或 Mock 审核边界。真实模型阶段还需要一个 LLM 辅助风险识别层，用于在 Prompt、歌词、封面或发布包进入外部系统前给出预检建议。

本批目标不是替代公司审核系统，而是补齐 `ModerationAgent` 的 Mock 合约和审计记录。首期只接入音乐 Prompt 预检：`MusicPromptAgent` 生成 provider prompt 后，`MusicProviderAdapter` 提交任务前，先由 `ModerationAgent` 做风险预检。

## 3. 功能需求

- FR-1：系统 MUST 新增 `ModerationAgent` 合约，输入作品 ID、目标类型、待预检文本和上下文。
- FR-2：`ModerationAgent` MUST 输出目标类型、决策、风险码、用户可读提示、推荐动作和 metadata。
- FR-3：`SongProductionWorkflow` MUST 在调用 `MusicProvider.submit` 前，对 `MusicPromptAgent` 输出的 music prompt 调用 `ModerationAgent`。
- FR-4：当 `ModerationAgent` 返回 `BLOCK` 或 `MANUAL_REVIEW` 时，Workflow MUST 不调用音乐 Provider，并按现有失败状态收口。
- FR-5：`ModerationAgent` MUST 通过 `AgentRunRecorder` 写入 `ModerationAgent` run，包含 agent 名称、版本、operation、模型名、输入/输出 hash、模板版本、状态和耗时。
- FR-6：`ModerationAgent` 抛异常时，Workflow MUST 不调用音乐 Provider，并按音乐生成失败路径释放权益。
- FR-7：自动化测试 MUST 使用 MockModerationAgent，不触发真实模型或真实公司系统。

## 4. 非功能需求

- NFR-1：本批 MUST 不改变用户侧 OpenAPI v0.1 字段。
- NFR-2：MockModerationAgent MUST deterministic，便于测试和 smoke 复验。
- NFR-3：ModerationAgent metadata MUST 不包含密钥、JWT、签名 URL 或完整供应商响应。
- NFR-4：本批不得新增数据库表或 migration，复用 `agent_runs`。
- NFR-5：公司审核最终口径仍以 `ModerationAdapter` 或公司真实审核系统为准，`ModerationAgent` 不得绕过公司审核。

## 5. 验收标准

- AC-1：Given MockModerationAgent，When 确认出歌，Then Workflow 在调用 MusicProvider 前执行音乐 Prompt 预检，覆盖 FR-3。
- AC-2：Given MockModerationAgent，When 音乐 Prompt 预检通过，Then `agent_runs` 可记录 `ModerationAgent / MUSIC_PROMPT_PRECHECK / SUCCEEDED`，覆盖 FR-5。
- AC-3：Given MockModerationAgent 返回 `BLOCK`，When Workflow 执行，Then Provider 不被调用，Workflow 释放权益并收口失败，覆盖 FR-4。
- AC-4：Given ModerationAgent 抛异常，When Workflow 执行，Then Provider 不被调用，Workflow 按音乐生成失败路径收口，覆盖 FR-6。
- AC-5：Given 自动化测试运行，When 执行 Gradle 测试，Then 不触发真实模型或公司系统调用，覆盖 FR-7、NFR-5。

## 6. 边界情况

- EC-1：文本为空时，MockModerationAgent SHOULD 返回 `PASS`，避免阻断缺省 Mock 路径。
- EC-2：文本包含 `[BLOCK]` 时，MockModerationAgent SHOULD 返回 `BLOCK`，用于自动化测试。
- EC-3：目标类型为空时，MockModerationAgent SHOULD 使用 `MUSIC_PROMPT` 作为默认目标。
- EC-4：预检阻断时，首期映射到现有 `MUSIC_GENERATION_FAILED`，不新增 OpenAPI 失败码。

## 7. 内部 API 合约

```ts
type ModerationTarget =
  | "USER_INPUT"
  | "LYRICS"
  | "MUSIC_PROMPT"
  | "COVER_PROMPT"
  | "PUBLISH_PACKAGE";

type ModerationAgentDecision = "PASS" | "BLOCK" | "MANUAL_REVIEW";

type ModerationAgentRequest = {
  work_id: string;
  target: ModerationTarget;
  text?: string;
  context: Record<string, unknown>;
};

type ModerationAgentResult = {
  target: ModerationTarget;
  decision: ModerationAgentDecision;
  risk_codes: string[];
  message?: string;
  recommended_action?: "PASS" | "RETURN_TO_EDIT" | "MANUAL_REVIEW";
  metadata: Record<string, unknown>;
};
```

## 8. 数据模型

本批复用 `agent_runs`：

| 字段 | 口径 |
|---|---|
| `agent_name` | `ModerationAgent` |
| `agent_version` | `v0.1` |
| `operation` | `MUSIC_PROMPT_PRECHECK` |
| `model_name` | `mock-moderation-agent` |
| `prompt_template_key` | `moderation.agent.v1` |
| `prompt_template_version` | `1` |
| `input_hash` | 请求摘要 SHA-256 |
| `output_hash` | 结果摘要 SHA-256 |
| `status` | `SUCCEEDED` 或 `FAILED` |

## 9. 非目标

- 不接入真实审核模型。
- 不替代公司审核 Adapter。
- 不新增用户侧接口字段。
- 不新增 OpenAPI 失败码。
- 不实现输入、歌词、封面和发布包所有预检点；这些后续可复用同一合约扩展。
