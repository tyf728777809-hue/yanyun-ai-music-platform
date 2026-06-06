# QualityEvaluationAgent v0.1

更新时间：2026-06-06

## 1. 标题与元数据

- 标题：发布包质量评估 Agent Mock 合约
- 作者：Codex
- 状态：Approved for implementation
- 适用范围：确认出歌后的音频、封面、视频、timeline 和发布包可用性质量门
- 关联文档：
  - `docs/specs/ai-agent-architecture-direction-v0.1.md`
  - `docs/specs/ai-agent-orchestration-engineering-design-v0.1.md`
  - `docs/specs/agent-runtime-audit-v0.1.md`
  - `docs/specs/cover-video-rendering-v0.1.md`

## 2. 背景

当前本地链路已经能生成 Mock 音频、封面、MP4、timeline 和发布包，并在封面生成前补齐 `CoverPromptAgent`。但发布包进入公司审核和交接前，仍缺少一个可审计的质量门，用于判断媒体资产是否满足基础商用交付口径。

本批目标不是引入真实质量评估模型，而是在不调用真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统的前提下，补齐 `QualityEvaluationAgent` 的 Mock 合约和审计记录。首期只接入确认出歌后的发布包质量门，歌词质量门和音乐质量门后续可复用同一合约扩展。

## 3. 功能需求

- FR-1：系统 MUST 新增 `QualityEvaluationAgent` 合约，输入作品 ID、质量门类型、标题、歌词、音乐 Provider、音频/封面/视频/timeline 资产摘要和上下文。
- FR-2：`QualityEvaluationAgent` MUST 输出质量门、决策、分数、原因、推荐动作、是否可重试和 metadata。
- FR-3：`SongProductionWorkflow` MUST 在发布包准备、发布包审核和发布包写入前调用 `QualityEvaluationAgent`。
- FR-4：当 `QualityEvaluationAgent` 返回非 `PASS` 决策时，Workflow MUST 收口为 `PACKAGE_BUILD_FAILED`，释放已锁权益，不进入 `PACKAGE_READY`。
- FR-5：`QualityEvaluationAgent` MUST 通过 `AgentRunRecorder` 写入 `QualityEvaluationAgent` run，包含 agent 名称、版本、operation、模型名、输入/输出 hash、模板版本、状态和耗时。
- FR-6：`QualityEvaluationAgent` 抛异常时，Workflow MUST 沿 `PACKAGE_BUILD_FAILED` 收口，并释放已锁权益。
- FR-7：自动化测试 MUST 使用 MockQualityEvaluationAgent，不触发真实模型或真实公司系统。

## 4. 非功能需求

- NFR-1：本批 MUST 不改变用户侧 OpenAPI v0.1 字段。
- NFR-2：MockQualityEvaluationAgent MUST deterministic，便于测试和 smoke 复验。
- NFR-3：质量评估 metadata MUST 不包含密钥、JWT、签名 URL 或完整供应商响应。
- NFR-4：本批不得新增数据库表或 migration，复用 `agent_runs`。
- NFR-5：自动化测试默认不得触发真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统调用。

## 5. 验收标准

- AC-1：Given MockQualityEvaluationAgent，When 确认出歌生成全部媒体资产，Then Workflow 在发布包准备前调用质量门，覆盖 FR-3。
- AC-2：Given MockQualityEvaluationAgent，When 媒体资产完整，Then `agent_runs` 可记录 `QualityEvaluationAgent / PACKAGE_QUALITY_GATE / SUCCEEDED`，覆盖 FR-5。
- AC-3：Given QualityEvaluationAgent 返回 `RETRY`，When Workflow 执行，Then Workflow 失败码为 `PACKAGE_BUILD_FAILED`，释放权益，不调用发布包准备和审核，覆盖 FR-4。
- AC-4：Given QualityEvaluationAgent 抛异常，When Workflow 执行，Then Workflow 失败码为 `PACKAGE_BUILD_FAILED`，释放权益，不进入 `PACKAGE_READY`，覆盖 FR-6。
- AC-5：Given 自动化测试运行，When 执行 Gradle 测试，Then 不触发真实模型或公司系统调用，覆盖 FR-7、NFR-5。

## 6. 边界情况

- EC-1：音频资产缺失或音频时长为空时，MockQualityEvaluationAgent SHOULD 返回 `RETRY`。
- EC-2：封面资产缺失或尺寸不是 16:9 时，MockQualityEvaluationAgent SHOULD 返回 `RETRY`。
- EC-3：视频资产缺失或尺寸不是 16:9 时，MockQualityEvaluationAgent SHOULD 返回 `RETRY`。
- EC-4：timeline 资产缺失时，MockQualityEvaluationAgent SHOULD 返回 `RETRY`。
- EC-5：歌词为空时，MockQualityEvaluationAgent SHOULD 降低分数并返回 `RETRY`。

## 7. 内部 API 合约

```ts
type QualityGate = "LYRICS" | "MUSIC" | "COVER" | "VIDEO" | "PUBLISH_PACKAGE";

type QualityDecision = "PASS" | "REWRITE" | "RETRY" | "BLOCK" | "MANUAL_REVIEW";

type QualityEvaluationRequest = {
  work_id: string;
  gate: QualityGate;
  song_title?: string;
  lyrics_text?: string;
  music_provider?: string;
  audio_object_key?: string;
  audio_duration_ms?: number;
  cover_object_key?: string;
  cover_width?: number;
  cover_height?: number;
  video_object_key?: string;
  video_width?: number;
  video_height?: number;
  video_duration_ms?: number;
  timeline_object_key?: string;
  context: Record<string, unknown>;
};

type QualityEvaluationResult = {
  gate: QualityGate;
  decision: QualityDecision;
  score: number;
  reasons: string[];
  recommended_action?: string;
  retryable: boolean;
  metadata: Record<string, unknown>;
};
```

## 8. 数据模型

本批复用 `agent_runs`：

| 字段 | 口径 |
|---|---|
| `agent_name` | `QualityEvaluationAgent` |
| `agent_version` | `v0.1` |
| `operation` | `PACKAGE_QUALITY_GATE` |
| `model_name` | `mock-quality-evaluation` |
| `prompt_template_key` | `quality.evaluation.v1` |
| `prompt_template_version` | `1` |
| `input_hash` | 请求摘要 SHA-256 |
| `output_hash` | 结果摘要 SHA-256 |
| `status` | `SUCCEEDED` 或 `FAILED` |

## 9. 非目标

- 不接入真实质量评估模型。
- 不新增用户侧接口字段。
- 不改变前端状态派生规则。
- 不替代公司审核系统。
- 不实现歌词质量自动重写循环。
- 不改变现有权益扣减主口径。
