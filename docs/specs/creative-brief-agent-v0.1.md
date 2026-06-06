# Creative Brief Agent v0.1

更新时间：2026-06-06

## 1. 标题与元数据

- 标题：创作简报 Agent 边界
- 作者：Codex
- 状态：Approved for local Mock implementation
- 适用范围：灵感成歌、填词成歌、AI 润色、AI 续写进入写词模型前的用户需求理解与结构化创作简报
- 评审依据：AI Agent Orchestration Engineering Design v0.1、Agent Runtime Audit v0.1、DeepSeek / Knowledge Lyrics Pipeline v0.1

## 2. Context

当前写词链路已经通过 `LyricsGenerationService` 编排知识库、Prompt 模板和 Mock DeepSeek，并通过 `LyricsAgent` 写入 `agent_runs`。这能跑通本地闭环，但真实 DeepSeek 接入前，用户需求理解、主题提炼、情绪标签、叙事视角和创作约束仍混在写词 Prompt 中，不利于后续分阶段调优和审计。

`CreativeBriefAgent` 的目标是在调用 `LyricsAgent` 前，先把用户输入、当前歌词、修改指令、曲风偏好和知识库引用整理成结构化 `CreativeBrief`。后续真实 DeepSeek 写词时，可以先替换这个 Agent 的真实模型实现，而不改变用户侧 OpenAPI 或作品状态机。

本批只做本地 Mock 合约和审计接入，不调用真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统。

## 3. Functional Requirements

- FR-1：系统 MUST 新增 `CreativeBriefAgent` 合约，输入用户、作品、写词 operation、用户输入、当前歌词、修改指令、标题偏好、曲风偏好、人声偏好和燕云知识库引用。
- FR-2：`CreativeBriefAgent` MUST 输出结构化 `CreativeBrief`，包含用户意图摘要、主题、情绪标签、叙事视角、音乐方向、燕云引用、创作约束和风险提示。
- FR-3：`LyricsGenerationService` MUST 在渲染写词 Prompt 和调用 `DeepSeekLyricsClient` 前调用 `CreativeBriefAgent`。
- FR-4：写词 Prompt / DeepSeek 请求 MUST 接收 `CreativeBriefAgent` 输出的简报上下文。
- FR-5：`CreativeBriefAgent` MUST 通过 `AgentRunRecorder` 写入 `CreativeBriefAgent` run，包含 agent 名称、版本、operation、模型名、输入/输出 hash、模板版本、状态和耗时。
- FR-6：`CreativeBriefAgent` 失败时，写词链路 MUST 按原有异常路径失败，并记录脱敏失败审计。
- FR-7：自动化测试 MUST 使用 MockCreativeBriefAgent，不触发真实模型或真实供应商。

## 4. Non-Functional Requirements

- NFR-1：Agent 审计不得保存完整歌词、完整 Prompt、真实密钥、JWT、Cookie、用户 token 或供应商原始 payload。
- NFR-2：MockCreativeBriefAgent MUST deterministic，便于测试和 smoke 复验。
- NFR-3：本批不得改变用户侧 OpenAPI 响应结构。
- NFR-4：本批不得改变默认 Mock 写词输出的主链路可用性。
- NFR-5：本批不得新增数据库表或 migration，复用 `agent_runs`。

## 5. Acceptance Criteria

- AC-1：Given MockCreativeBriefAgent，When 灵感成歌生成歌词，Then `CreativeBriefAgent` 在 `LyricsAgent` 前被调用，覆盖 FR-3。
- AC-2：Given MockCreativeBriefAgent，When 写词 Prompt 渲染，Then Prompt 上下文包含创作简报摘要，覆盖 FR-4。
- AC-3：Given MockCreativeBriefAgent，When 生成歌词成功，Then `agent_runs` 可记录 `CreativeBriefAgent / SUCCEEDED`，覆盖 FR-5。
- AC-4：Given CreativeBriefAgent 抛异常，When 写词链路执行，Then 系统记录 `FAILED / CREATIVE_BRIEF_AGENT_FAILED`，并继续按原异常路径收口，覆盖 FR-6。
- AC-5：Given 后端 targeted tests 运行，When 执行 Gradle 测试，Then 不发生真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统调用，覆盖 FR-7。

## 6. Edge Cases

- EC-1：用户输入为空但当前歌词存在时，Agent MUST 使用当前歌词生成简报。
- EC-2：用户输入和当前歌词都为空但修改指令存在时，Agent MUST 使用修改指令生成简报。
- EC-3：知识库引用为空时，Agent MUST 使用安全默认燕云引用占位，不阻断写词。
- EC-4：曲风偏好为空时，Agent SHOULD 使用安全默认国风影视感方向。
- EC-5：Agent 失败信息进入审计前必须脱敏截断。

## 7. API Contracts

本批不新增用户侧 OpenAPI。内部合约：

```ts
type CreativeBriefRequest = {
  user_id: string;
  work_id: string;
  operation: "INSPIRATION" | "LYRICS" | "POLISH" | "CONTINUE";
  user_input?: string;
  current_lyrics?: string;
  instruction?: string;
  requested_title?: string;
  music_style?: string;
  vocal_preference?: string;
  yanyun_references: string[];
};

type CreativeBriefResult = {
  user_intent_summary: string;
  theme: string;
  mood_tags: string[];
  narrative_viewpoint?: string;
  music_direction: string;
  yanyun_references: string[];
  constraints: string[];
  risk_notes: string[];
};
```

## 8. Data Models

本批复用 `agent_runs`：

| 字段 | 口径 |
|---|---|
| `agent_name` | `CreativeBriefAgent` |
| `agent_version` | `v0.1` |
| `operation` | 写词 operation，例如 `INSPIRATION`、`LYRICS`、`POLISH`、`CONTINUE` |
| `model_name` | `mock-creative-brief` |
| `prompt_template_key` | `creative.brief.v1` |
| `prompt_template_version` | `1` |
| `input_hash` / `output_hash` | 不保存原文，只保存 SHA-256 |

## 9. Out of Scope

- OS-1：不调用真实 DeepSeek 或其他 LLM 生成创作简报。
- OS-2：不新增用户侧字段展示创作简报。
- OS-3：不新增数据库表保存完整简报。
- OS-4：不改变 AI 润色/续写次数规则。
- OS-5：不拆分 Temporal activity；本批只接入写词前的 Mock Agent 合约。
