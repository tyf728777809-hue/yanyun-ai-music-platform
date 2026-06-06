# Cover Prompt Agent v0.1

更新时间：2026-06-06

## 1. 标题与元数据

- 标题：封面生成提示词 Agent 边界
- 作者：Codex
- 状态：Approved for local Mock implementation
- 适用范围：确认出歌后、调用 Image 2 / MockCoverGenerationService 前的封面视觉提示词规划
- 评审依据：AI Agent Orchestration Engineering Design v0.1、Agent Runtime Audit v0.1、Cover and Video Rendering Pipeline v0.1

## 2. Context

当前出歌 Workflow 已经通过 `CoverGenerationService` 生成 Mock 封面资产，但封面生成请求只接收歌曲标题、摘要、歌词和 music prompt，缺少独立的封面提示词规划 Agent。真实 Image 2 接入前，需要先把“根据作品内容生成视觉 prompt”的创意步骤与“调用生图 Provider”的确定性步骤拆开。

`CoverPromptAgent` 的职责是把歌曲标题、摘要、歌词、音乐提示词和歌词阶段产生的 cover prompt seed 转换为结构化视觉提示词。`ImageProviderAdapter` 或当前 MockCoverGenerationService 只负责按该提示词生成/导入封面资产，不负责理解歌词和规划视觉风格。

本批只做本地 Mock 合约和审计接入，不调用真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统。

## 3. Functional Requirements

- FR-1：系统 MUST 新增 `CoverPromptAgent` 合约，输入作品 ID、歌曲标题、摘要、歌词、music prompt、cover prompt seed 和目标尺寸。
- FR-2：`CoverPromptAgent` MUST 输出视觉 prompt、negative prompt、宽高、风格约束和 provider options。
- FR-3：`SongProductionWorkflow` MUST 在调用 `CoverGenerationService.generateCover` 前调用 `CoverPromptAgent`。
- FR-4：`CoverGenerationService.generateCover` MUST 使用 `CoverPromptAgent` 输出的 visual prompt，而不是只依赖原始歌词和 music prompt。
- FR-5：`CoverPromptAgent` MUST 通过 `AgentRunRecorder` 写入 `CoverPromptAgent` run，包含 agent 名称、版本、operation、模型名、输入/输出 hash、模板版本、状态和耗时。
- FR-6：`CoverPromptAgent` 失败时，Workflow MUST 沿当前媒体生成失败路径收口，不调用真实 Image 2。
- FR-7：自动化测试 MUST 使用 MockCoverPromptAgent，不触发真实模型或真实供应商。

## 4. Non-Functional Requirements

- NFR-1：Agent 审计不得保存完整歌词、完整 Prompt、真实密钥、JWT、Cookie、用户 token 或供应商原始 payload。
- NFR-2：MockCoverPromptAgent MUST deterministic，便于测试和 smoke 复验。
- NFR-3：本批不得改变用户侧 OpenAPI 响应结构。
- NFR-4：本批不得新增数据库表或 migration，复用 `agent_runs`。
- NFR-5：封面目标尺寸默认 MUST 保持 1920x1080，满足 MP4 16:9 成片和发布包预览需要。

## 5. Acceptance Criteria

- AC-1：Given MockCoverPromptAgent，When 确认出歌，Then Workflow 在 `CoverGenerationService.generateCover` 前调用 `CoverPromptAgent`，覆盖 FR-3。
- AC-2：Given MockCoverPromptAgent，When 生成封面，Then `CoverGenerationRequest.visual_prompt` 来自 Agent 输出，覆盖 FR-4。
- AC-3：Given MockCoverPromptAgent，When 生成封面成功，Then `agent_runs` 可记录 `CoverPromptAgent / SUCCEEDED`，覆盖 FR-5。
- AC-4：Given CoverPromptAgent 抛异常，When Workflow 执行，Then Provider/Service 不继续真实生图，Workflow 按媒体生成失败路径收口，覆盖 FR-6。
- AC-5：Given 后端 targeted tests 运行，When 执行 Gradle 测试，Then 不发生真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统调用，覆盖 FR-7。

## 6. Edge Cases

- EC-1：cover prompt seed 为空时，Agent MUST 使用歌曲标题、摘要和歌词生成安全默认视觉 prompt。
- EC-2：歌曲标题为空时，Agent SHOULD 使用 “Yanyun AI Music Cover” 作为标题占位。
- EC-3：目标尺寸为空或非法时，Agent SHOULD 使用 1920x1080。
- EC-4：歌词为空时，Agent SHOULD 仍可根据摘要和 music prompt 生成封面 prompt。
- EC-5：Agent 失败信息进入审计前必须脱敏截断。

## 7. API Contracts

本批不新增用户侧 OpenAPI。内部合约：

```ts
type CoverPromptRequest = {
  work_id: string;
  song_title?: string;
  song_summary?: string;
  lyrics_text?: string;
  music_prompt?: string;
  cover_prompt_seed?: string;
  width?: number;
  height?: number;
};

type CoverPromptResult = {
  visual_prompt: string;
  negative_prompt?: string;
  width: number;
  height: number;
  style_constraints: string[];
  provider_options: Record<string, unknown>;
};
```

## 8. Data Models

本批复用 `agent_runs`：

| 字段 | 口径 |
|---|---|
| `agent_name` | `CoverPromptAgent` |
| `agent_version` | `v0.1` |
| `operation` | `COVER_PROMPT` |
| `model_name` | `mock-cover-prompt` |
| `prompt_template_key` | `cover.prompt.v1` |
| `prompt_template_version` | `1` |
| `input_hash` / `output_hash` | 不保存原文，只保存 SHA-256 |

## 9. Out of Scope

- OS-1：不调用真实 Image 2 或其他生图服务；真实 Image 2 接入前按 `docs/runbook/image2-controlled-real-integration.md`、`docs/security/image2-secret-and-log-handling.md` 和 `docs/checklists/image2-real-integration-acceptance.md` 执行。
- OS-2：不新增用户侧模型选择或封面 prompt 编辑 UI。
- OS-3：不新增完整 prompt 存档后台。
- OS-4：不改变发布包 JSON 结构。
- OS-5：不改变视频渲染模板或字幕安全区策略。
