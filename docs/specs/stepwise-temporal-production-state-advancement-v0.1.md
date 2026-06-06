# Stepwise Temporal Production State Advancement v0.1

更新时间：2026-06-06

## 1. 标题与元数据

- 标题：分步 Temporal 生产态推进规格
- 作者：Codex
- 状态：Draft for implementation planning
- 适用范围：`StepwiseSongProductionWorkflow`、`SongProductionStepActivities`、`music-worker`、真实音乐/封面/视频模型联调前的生产状态推进
- 关联文档：
  - `docs/specs/temporal-activity-decomposition-v0.1.md`
  - `docs/specs/temporal-song-production-orchestration-v0.1.md`
  - `docs/specs/reliable-song-production-orchestration-v0.1.md`
  - `docs/specs/ai-agent-architecture-direction-v0.1.md`
  - `docs/specs/dreammaker-music-provider-v0.1.md`

## 2. 背景

当前本地用户实测主路径仍应使用同步 Mock 模式：`MUSIC_WORKFLOW_DISPATCH_MODE=sync`、`MUSIC_PROVIDER=mock`，由 `music-api` 在请求内推进到 `GENERATED / PACKAGE_READY`。这条路径已经用于前端真实后端 smoke，不依赖 Temporal worker，也不调用真实 DeepSeek、DreamMaker、Image 2 或公司系统。

Temporal 分进程路径已经证明 API 可以通过 outbox 启动 worker。最近新增的 `stepwise-recording` worker 模式进一步证明了 `StepwiseSongProductionWorkflow` 可以按 13 个步骤写入 `generation_job_steps` 审计记录。不过该模式只做步骤审计，不推进 `works`、`generation_jobs`、`media_assets`、`publish_packages` 等生产状态；它故意停留在 `GENERATING / QUOTA_LOCKING / PACKAGE_NOT_READY`，不能作为用户可测完成链路。

真实模型阶段不能长期依赖一个粗粒度 `MockSongProductionWorkflow` 委托，也不能直接把 `stepwise-recording` 当生产模式打开。后续需要新增明确的分步生产态模式：每个 step activity 既记录步骤审计，也按既有状态机推进作品、任务、媒体资产、发布包和权益事务，并且必须能在失败和重试时保持幂等。

## 3. 目标

- 把当前“录步骤”能力演进为“分步推进生产状态”的可实施基线。
- 保持默认同步 Mock 用户实测路径不退化。
- 在真实 Suno / MiniMax、Image 2、DeepSeek 和公司 Adapter 打开前，先获得可审计、可恢复、可定位失败的生产链路。
- 明确 `stepwise-recording` 与后续 `stepwise-production` 的边界，避免验收口径混淆。

## 4. 功能需求

- FR-1：系统 MUST 保留 `legacy` 和默认同步 Mock 主路径，直到 `stepwise-production` 通过独立 smoke。
- FR-2：`stepwise-recording` MUST 继续只作为录步和 Temporal 边界验证模式，不得把作品推进到 `GENERATED / PACKAGE_READY`。
- FR-3：系统 SHOULD 新增独立的 `stepwise-production` worker 模式，避免复用 `stepwise-recording` 造成行为歧义。
- FR-4：`stepwise-production` 中每个 activity MUST 写入或更新对应 `generation_job_steps` 记录，并按步骤推进领域状态。
- FR-5：`stepwise-production` MUST 复用现有领域表，不为第一版新增非必要表。
- FR-6：权益锁定、失败释放和成功扣减 MUST 与当前主链路口径一致：确认出歌时锁定，发布包可用后扣减，失败按原因释放或允许重试。
- FR-7：音乐生成步骤 MUST 能选择 `mock`、`suno` 或 `minimax` Provider；自动化和默认本地仍必须使用 `mock`。
- FR-8：供应商音频成功后 MUST 先导入平台对象存储，再写入 `AUDIO` 媒体资产和发布包，不得直接把供应商临时 URL 暴露为平台交接资产。
- FR-9：封面、视频、timeline 和发布包组装 MUST 写入 `media_assets` / `publish_packages`，字段口径与同步 Mock 主链路保持一致。
- FR-10：发布包进入 `PACKAGE_READY` 前 MUST 完成发布包质量门和 `ModerationAdapter.preCheckPublishPackage` 或等价 Mock 边界。
- FR-11：所有失败 MUST 映射到现有 OpenAPI v0.1 可读状态、失败码、`recommended_action` 和 `available_actions`。
- FR-12：Workflow MUST 负责状态推进顺序；Agent、Provider 和 Adapter activity MUST NOT 自行决定跳过状态机、提交权益或交接发布包。
- FR-13：真实外部调用 MUST 受硬开关、环境变量和 runbook 控制；自动化测试 MUST NOT 调用真实 DeepSeek、DreamMaker、Image 2 或公司系统。

## 5. 非功能需求

- NFR-1：默认用户实测路径 MUST 保持简单：启动基础设施和 `music-api` 后，前端可通过 `prototypes/Claude-web-v1` 走同步 Mock 闭环。
- NFR-2：`stepwise-production` activity MUST 使用 `work_id + job_id + step_name` 或可证明等价的业务键做幂等。
- NFR-3：外部供应商 task id、object key、package key、agent run id MUST 可追踪，但日志和数据库失败信息 MUST 脱敏并限长。
- NFR-4：Temporal history MUST NOT 保存完整 Prompt、完整歌词大段副本、供应商完整响应、JWT、API Key、用户 token 或大体积媒体 payload。
- NFR-5：每个真实外部调用 activity MUST 有显式 timeout、max attempts、retryable / non-retryable 规则和成本止损。
- NFR-6：`stepwise-production` smoke SHOULD 能证明 API 与 worker 分进程运行，并最终生成 MP4 发布包。

## 6. 模式定义

| 模式 | 用途 | 是否推进作品完成 | 是否适合用户实测 | 是否允许自动化真实外部调用 |
|---|---|---:|---:|---:|
| `sync + mock` | 当前推荐本地用户实测主路径 | 是 | 是 | 否 |
| `legacy` worker | Temporal v0.1 兼容路径，单 activity 委托现有 workflow | 是 | 可用于内部联调 | 否 |
| `stepwise-recording` | 分步 activity 顺序和 step audit 验证 | 否 | 否 | 否 |
| `stepwise-production` | 后续分步生产态推进模式 | 是 | 通过 smoke 后可用 | 否，除非手动受控联调 |

## 7. 状态推进口径

`stepwise-production` MUST 按以下口径推进状态。字段名以 OpenAPI v0.1 和当前数据库模型为准，具体 Java 枚举可沿用现有实现。

| Step | 作品/任务状态口径 | 必要持久化 |
|---|---|---|
| `LOCK_QUOTA` | `GENERATING / QUOTA_LOCKING` | `generation_jobs` running，quota lock transaction，step audit |
| `GENERATE_MUSIC_PROMPT` | `GENERATING / MUSIC_PROMPTING` 或保持当前可表达阶段 | `agent_runs`，step audit |
| `PRE_CHECK_MUSIC_PROMPT` | 仍为生成中；阻断时失败 | `agent_runs` 或 moderation trace，step audit |
| `SUBMIT_MUSIC` | `GENERATING / MUSIC_GENERATING` | `provider_calls` with provider task id，step audit |
| `POLL_MUSIC` | `GENERATING / MUSIC_GENERATING` | provider status summary，step audit |
| `IMPORT_AUDIO` | `GENERATING / MUSIC_GENERATING` | `media_assets(AUDIO)`，object key，step audit |
| `GENERATE_COVER_PROMPT` | `GENERATING / COVER_GENERATING` | `agent_runs`，step audit |
| `GENERATE_COVER` | `GENERATING / COVER_GENERATING` | `media_assets(COVER)`，step audit |
| `RENDER_VIDEO` | `GENERATING / VIDEO_RENDERING` | `media_assets(VIDEO)`、`media_assets(TIMELINE)`，step audit |
| `EVALUATE_PACKAGE` | `GENERATING / PACKAGE_BUILDING` | quality agent run or deterministic quality record，step audit |
| `PRE_CHECK_PUBLISH_PACKAGE` | `GENERATING / PACKAGE_BUILDING` | moderation adapter result，step audit |
| `ASSEMBLE_PUBLISH_PACKAGE` | `GENERATED / PACKAGE_READY` 前的组包 | `publish_packages`，package object key，URL TTL，step audit |
| `COMMIT_QUOTA` | `GENERATED / PACKAGE_READY` | quota commit transaction，`generation_jobs=SUCCEEDED`，work package ready |
| `RELEASE_QUOTA` | 失败收口 | quota release transaction，`generation_jobs=FAILED`，work failure |

如果当前 OpenAPI 枚举无法表达中间阶段，第一版 MAY 复用最接近阶段，但 MUST 在 `generation_job_steps` 中保留精确 step 名。

## 8. 验收标准

- AC-1：Given 默认同步 Mock 配置，When 运行 API 主链路 smoke 和前端真实后端 smoke，Then 作品仍进入 `GENERATED / PACKAGE_READY`，覆盖 FR-1、NFR-1。
- AC-2：Given `stepwise-recording` worker 模式，When 运行录步 smoke，Then 只写入完整 step audit，作品不得被误判为 `PACKAGE_READY`，覆盖 FR-2。
- AC-3：Given `stepwise-production + mock` 模式，When 用户确认出歌，Then worker 分步推进并最终生成音频、封面、视频、timeline 和发布包，覆盖 FR-3、FR-4、FR-9。
- AC-4：Given `stepwise-production + mock` 模式，When `SUBMIT_MUSIC` 后发生可重试失败，Then 重试不得重复提交已存在 provider task，覆盖 FR-7、NFR-2。
- AC-5：Given 供应商音频返回远程 URL，When `IMPORT_AUDIO` 成功，Then 发布包使用平台对象存储 URL，而不是供应商临时 URL，覆盖 FR-8。
- AC-6：Given 发布包预检阻断，When Workflow 收口，Then 作品进入可读失败或 `PACKAGE_BLOCKED` 状态，权益释放，发布包不可交接，覆盖 FR-10、FR-11。
- AC-7：Given 自动化测试运行，When Gradle、API smoke、前端 smoke 执行，Then 不发起真实 DeepSeek、DreamMaker、Image 2 或公司系统请求，覆盖 FR-13。

## 9. 边界情况

- EC-1：Workflow 重放时不得重新生成随机 object key 或 provider task id；必须从已有持久化记录恢复。
- EC-2：`SUBMIT_MUSIC` 成功但 worker 崩溃时，后续执行必须通过 `provider_calls.provider_trace_id` 或 step reference 恢复轮询。
- EC-3：对象存储写入成功但数据库写入失败时，重试必须复用确定性 object key 或记录孤儿对象清理事项。
- EC-4：发布包已写入但权益 commit 失败时，作品不得对用户显示为已可交接，必须进入后台可复核失败状态。
- EC-5：`RELEASE_QUOTA` 自身失败时，用户侧失败状态仍需可读，同时保留后台补偿所需 trace。
- EC-6：同一个 work 的重复 confirm / retry 必须由现有 idempotency 和 `works.version` 控制，不能创建并行生产链路。

## 10. 内部合约

第一版 `SongProductionStepResult.references` 可继续承载小型 trace，但 production 模式 SHOULD 把可恢复信息写入数据库，仅在 references 中返回 id/key 摘要。

```ts
type StepReference = {
  generation_job_step_id?: string;
  agent_run_id?: string;
  provider_call_id?: string;
  provider_task_id?: string;
  object_key?: string;
  media_asset_id?: string;
  publish_package_id?: string;
};

type StepFailure = {
  failure_code: string;
  failure_message: string;
  retryable: boolean;
  recommended_action: "RETRY" | "RETRY_MUSIC" | "EDIT" | "MANUAL_REVIEW" | "NONE";
};
```

## 11. 数据模型

第一版 SHOULD 复用以下既有表：

| 表 | 生产态职责 |
|---|---|
| `works` | 用户可见状态、阶段、失败、包状态、并发 version |
| `generation_jobs` | 单次生产 job 的整体状态、阶段和失败码 |
| `generation_job_steps` | 每个分步 activity 的审计、幂等、失败和 trace |
| `provider_calls` | Suno / MiniMax / Image 2 等外部 Provider 调用摘要 |
| `agent_runs` | LLM Agent 调用摘要、模型名、模板版本、输入输出 hash |
| `media_assets` | 音频、封面、视频、timeline 平台资产 |
| `publish_packages` | 发布包 object key、URL、TTL 和交接状态 |
| `quota_transactions` | 权益锁定、释放、提交事务摘要 |
| `workflow_outbox` | API 到 worker 的启动请求 |

如实现发现必须新增补偿队列表或孤儿对象清理表，必须先更新规格，不得在实现中临时扩表。

## 12. 非目标

- OS-1：本规格不要求立即接入真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统。
- OS-2：本规格不改变 OpenAPI v0.1 用户侧响应结构。
- OS-3：本规格不要求把 `apps/web` 正式前端替换为 `prototypes/Claude-web-v1`。
- OS-4：本规格不把模型选择完整暴露给普通用户。
- OS-5：本规格不允许自动化测试通过真实模型调用来证明成功。

## 13. 推荐实施顺序

1. 保持 `sync + mock` 用户实测主路径稳定，先跑通 API smoke、OpenAPI contract smoke、前端真实后端 smoke。
2. 保持 `stepwise-recording` 为录步模式，继续用独立 smoke 验证 13 个 step 顺序和审计。
3. 新增 `stepwise-production` 配置值和测试，先用 fake activity 证明状态推进、失败释放和包 ready 收口。
4. 把 `MockSongProductionWorkflow` 中的副作用能力逐步抽到可复用 production step service，避免复制业务规则。
5. 先实现 `mock` provider 的 stepwise production smoke，再进入 Suno / MiniMax 受控真实联调。
6. 每接入一个真实模型，只打开一个环节，其余仍走 Mock/Fake，并更新 runbook、验收清单和 `docs/project-progress.md`。
