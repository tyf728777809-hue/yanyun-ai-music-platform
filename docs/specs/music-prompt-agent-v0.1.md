# Music Prompt Agent v0.1

更新时间：2026-06-06

## 1. 标题与元数据

- 标题：音乐生成提示词 Agent 边界
- 作者：Codex
- 状态：Approved for local Mock implementation
- 适用范围：确认出歌后、调用 Suno / MiniMax / MockMusicProvider 前的音乐提示词规划
- 评审依据：AI Multi-Agent Creative Pipeline v0.1、Agent Runtime Audit v0.1、DreamMaker Music Provider Integration Spec v0.1

## 2. Context

当前 `SongProductionWorkflow` 在确认出歌后直接把歌词草案中的 `music_prompt` 传给 `MusicProvider`。这能跑通 Mock 主链路，但真实 Suno / MiniMax 联调前还缺少一个可审计、可替换、可按 Provider 调整参数的音乐提示词 Agent。

本批目标是在不调用真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统的前提下，新增 `MusicPromptAgent` Mock 合约，并把其输出接入音乐 Provider 请求。这样后续真实音乐模型联调时，可以先替换 Agent 策略，而不改用户侧 OpenAPI 或业务状态机。

## 3. Functional Requirements

- FR-1：系统 MUST 新增 `MusicPromptAgent` 合约，输入作品标题、摘要、歌词、歌词阶段 music prompt seed、vocal preference 和目标音乐 Provider。
- FR-2：`MusicPromptAgent` MUST 输出最终音乐生成 prompt 和 provider options。
- FR-3：`SongProductionWorkflow` MUST 在调用 `MusicProvider.submit` 前调用 `MusicPromptAgent`。
- FR-4：`MusicProvider.submit` MUST 使用 `MusicPromptAgent` 输出的 prompt，而不是直接使用 workflow input 中的原始 prompt seed。
- FR-5：`MusicPromptAgent` MUST 通过 `AgentRunRecorder` 记录 `MusicPromptAgent` run，包含 agent 名称、版本、operation、model、输入/输出 hash、模板版本、状态和耗时。
- FR-6：`MusicPromptAgent` 失败时，workflow MUST 收口为 `MUSIC_GENERATION_FAILED`，释放已锁权益，并记录失败 job。
- FR-7：自动化测试 MUST 使用 MockMusicPromptAgent，不触发真实模型或真实供应商。

## 4. Non-Functional Requirements

- NFR-1：Agent 审计不得保存完整歌词、完整 Prompt、真实密钥、JWT、Cookie、用户 token 或供应商原始 payload。
- NFR-2：MockMusicPromptAgent MUST deterministic，便于测试和 smoke 复验。
- NFR-3：本批不得改变用户侧 OpenAPI 响应结构。
- NFR-4：本批不得改变默认 `MUSIC_PROVIDER=mock` 的本地主链路成功口径。

## 5. Acceptance Criteria

- AC-1：Given MockMusicPromptAgent，When workflow 确认出歌，Then MusicProvider 收到的 `music_prompt` 来自 Agent 输出，覆盖 FR-3、FR-4。
- AC-2：Given MockMusicPromptAgent，When workflow 确认出歌，Then `agent_runs` 可记录 `MusicPromptAgent / SUCCEEDED`，覆盖 FR-5。
- AC-3：Given MusicPromptAgent 抛异常，When workflow 执行，Then 作品失败码为 `MUSIC_GENERATION_FAILED`，权益释放，Provider 不被调用，覆盖 FR-6。
- AC-4：Given 后端 targeted tests 运行，When 执行 Gradle 测试，Then 不发生真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统调用，覆盖 FR-7。

## 6. Edge Cases

- EC-1：原始 music prompt seed 为空时，MockMusicPromptAgent MUST 使用安全默认国风音乐提示词。
- EC-2：目标 Provider 为空时，MockMusicPromptAgent SHOULD 使用 `MOCK` 作为 provider profile。
- EC-3：vocal preference 为空时，MockMusicPromptAgent SHOULD 使用 `AUTO`。
- EC-4：Agent 失败信息进入业务失败状态前必须脱敏截断。

## 7. API Contracts

本批不新增用户侧 OpenAPI。内部合约：

```ts
type MusicPromptRequest = {
  work_id: string;
  song_title: string;
  song_summary?: string;
  lyrics_text: string;
  music_prompt_seed?: string;
  vocal_preference?: string;
  music_provider?: "MOCK" | "SUNO" | "MINIMAX";
};

type MusicPromptResult = {
  music_prompt: string;
  provider_options: Record<string, unknown>;
};
```

## 8. Data Models

本批复用 `agent_runs`：

| 字段 | 口径 |
|---|---|
| `agent_name` | `MusicPromptAgent` |
| `agent_version` | `v0.1` |
| `operation` | `MUSIC_PROMPT` |
| `model_name` | `mock-music-prompt` |
| `prompt_template_key` | `music.prompt.v1` |
| `prompt_template_version` | `1` |
| `input_hash` / `output_hash` | 不保存原文，只保存 SHA-256 |

## 9. Out of Scope

- OS-1：不调用真实 DeepSeek 或其他 LLM 生成音乐提示词。
- OS-2：不实现面向用户的模型选择 UI。
- OS-3：不改变 Suno / MiniMax DreamMaker run/status 协议。
- OS-4：不修改发布包 JSON 的用户侧结构。
