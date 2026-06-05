# 技术方案需补项与已决策事项

文档日期：2026-06-05

## 1. 背景

`yanyun-ai-music-platform-tech-design-v0.2.md` 已经覆盖总体架构、服务形态、工作流、Provider/Adapter、数据库草案、API 草案、可观测、测试、开发拆分和已确认的商用级建设口径。

在正式进入仓库初始化和后续批次开发前，需要补齐若干会影响工程边界、数据模型和验收口径的决策，避免第 1 批脚手架完成后返工。

截至 PRD v0.3 和技术方案 v0.2，本文件中的关键事项已完成决策，后续实现应按“已决策”口径执行。

## 2. 必须补齐的高优先级事项

### 2.1 用户侧前端工程（已决策）

现状：

- PRD 明确本阶段需要创作首页、灵感成歌页、填词成歌页、歌词工坊页、歌词确认页、生成中页、成品预览页、我的作品页。
- 技术方案 v0.2 已将用户侧前端应用纳入仓库结构。

已决策：

- 在仓库结构中增加 `apps/web`。
- `apps/web` 使用 React + Vite + TypeScript。
- 用户侧 Web 移动端优先，同时兼容 PC Web。
- 第 1 批创建 `apps/web` scaffold，但不实现完整用户业务页面。
- 若公司有明确前端技术栈要求，后续通过 ADR 替换，并记录到 `docs/project-progress.md`。

### 2.2 OpenAPI 接口契约（已决策）

现状：

- 技术方案已有 API 草案，但尚未形成可冻结、可生成客户端或用于 Mock 的 OpenAPI 文档。

建议补充：

- 新增 `docs/api/openapi-v0.1.yaml`。
- 覆盖用户身份、作品创建、作品查询、歌词改写、确认出歌、封面重生、视频重渲染、发布包、配置后台的接口。
- 明确统一错误响应、分页、幂等 Header、鉴权 Header、Mock 用户 Header。

已决策：

- 第 1 批预留 springdoc-openapi 和 `docs/api/` 目录。
- OpenAPI v0.1 在第 1 批后、第 2 批前冻结。
- OpenAPI v0.1 必须覆盖作品状态、生成阶段、剩余改词次数、权益提示、失败可执行动作和发布包交接状态。

### 2.3 权益扣减时机（已决策）

现状：

- PRD 表述为：确认出歌时锁定权益，成功生成可用歌曲后扣减；同时也强调因视频失败导致作品不可用时不应扣减主要出歌权益。
- 技术方案 SongProductionWorkflow 在音频质检通过后执行 `QuotaAdapter.commit`，随后才生成封面、时间轴、视频和发布包。

风险：

- 如果业务定义“可用作品”必须是 MP4 发布包，则音频通过后扣减会早于用户可拿到成果。
- 如果业务定义“可用歌曲”即音频通过，则当前流程可接受，但视频失败后的用户体验和权益解释要单独设计。

建议补充一个明确口径：

| 方案 | 扣减时机 | 优点 | 风险 |
|---|---|---|---|
| A | 音频质检通过后扣减 | 成本归因清晰，MiniMax 成本已发生 | MP4 失败时用户可能认为作品不可用却已扣权益 |
| B | MP4 发布包生成后扣减 | 用户拿到完整产物才扣，体验更一致 | MiniMax 成本已发生但后续失败不扣，成本压力更高 |

已决策：

- 采用 B：MP4 发布包可用后扣减主权益。
- 用户确认出歌时先锁定权益。
- 内部成本记录仍在 MiniMax 成功后立即记录。
- 若公司权益系统要求按歌曲音频扣减，必须更新 PRD、技术方案和进度文档后再变更。

### 2.4 审核链路落点（已决策）

现状：

- 技术方案定义了 `ModerationAdapter.preCheckUserInput`、`preCheckLyrics`、`preCheckPublishPackage`。
- LyricsPreparationWorkflow 和 SongProductionWorkflow 里有前置审核点，但发布包生成前的审核落点尚不明确。

建议补充：

- 用户输入创建前：`preCheckUserInput`。
- 歌词确认前或确认时：`preCheckLyrics`。
- 发布包对外可获取前：`preCheckPublishPackage`。

已决策：

- 本地阶段全部 MockAllow。
- 用户输入创建前：`preCheckUserInput`。
- 歌词确认前或确认时：`preCheckLyrics`。
- 发布包对外可获取前：`preCheckPublishPackage`。
- 发布包未通过审核或接入前检查不得进入 `PACKAGE_READY`。
- 审核失败记录为本平台发布包阻断，不等同社区发布失败。

### 2.5 并发控制与 `works.version`（已决策）

现状：

- 技术方案提到使用乐观锁 `version` 字段，或 `SELECT FOR UPDATE`。
- 当前 `works` 表草案未包含 `version` 字段。

建议补充：

- `works` 表增加：

```sql
version INTEGER NOT NULL DEFAULT 0
```

- API 层对高并发写操作使用事务 + 行级锁或乐观锁。
- 同一 Work 的 Workflow ID 按 `work_id` 固定命名，防止重复启动。

已决策：

- `works` 表增加 `version INTEGER NOT NULL DEFAULT 0`。
- 状态变更使用乐观锁，关键状态写入可配合 `SELECT FOR UPDATE`。
- `confirm`、`polish`、`cover/regenerate`、`video/rerender` 等关键操作在服务层做状态校验和并发保护。

### 2.6 Outbox 或启动补偿（已决策）

现状：

- 已新增 `workflow_outbox` 表和本地 outbox dispatcher，可在 `MUSIC_WORKFLOW_DISPATCH_MODE=outbox` 下验证确认出歌/音乐重试的可靠启动边界。

建议补充：

| 方案 | 说明 | 适用性 |
|---|---|---|
| Outbox | DB 事务写入事件，由后台分发启动 Workflow | 更可靠，初期实现略复杂 |
| 状态补偿 | API 写状态后启动 Workflow，失败时定时扫描补偿 | 初期简单，但要写清扫描逻辑 |

已决策：

- 第 1-3 批可用状态补偿，降低初始复杂度。
- 在 SongProductionWorkflow 进入真实模型调用前，引入可靠 Outbox 或等价补偿机制。
- 第 3 批已落地 Outbox v0.1：默认 sync 保持本地 Mock 兼容；显式 outbox 模式会同事务写入 generation job 和 outbox event，由本地 dispatcher 异步推进作品状态。
- 第 4 批已落地 Temporal v0.1：`WORKFLOW_OUTBOX_DISPATCH_TARGET=local|temporal` 可切换本地委托或 Temporal 启动；Temporal 模式下 API dispatcher 只负责按 `work_id + job_id` deterministic workflow id 启动 workflow，独立 `music-worker` 注册 workflow/activity 并复用当前生产委托推进作品。
- Temporal activity 自动重试暂不放开，v0.1 固定 `maximumAttempts=1`，避免在权益、Provider 调用、媒体资产和发布包写入幂等性未审计前造成重复副作用。

### 2.7 Provider 模式与真实 API 调用边界（已决策）

现状：

- 技术方案已有 `fake|mixed|real` Provider 模式。
- AGENTS.md 已要求自动化测试不得调用真实 DeepSeek、MiniMax、Image 2。
- 2026-06-05 新增产品要求：音乐生成 Provider 后续需同时预留 Suno 和 MiniMax，两者都要接入；上线时可通过配置或运营策略选择对用户开放哪个模型。
- 用户提供了飞书资料链接：`https://ycnts90jb6sm.feishu.cn/docx/G9v9dhd76oyiLmxwdQXcYGqhnHg`。资料已读取并整理；后续用户补充确认 DreamMaker 使用 AccessKey/SecretKey 生成 HS256 JWT，并以 `Authorization: Bearer <jwt>` 调用。

建议补充：

- 明确 `.env.example` 只放变量名。
- 默认 `PROVIDER_MODE=fake`。
- CI 和自动化测试强制 fake 或 Mock HTTP。
- `real` 模式只能手动联调使用。
- Provider 抽象需覆盖 `SunoMusicProvider` 与 `MiniMaxMusicProvider`，并在配置中心预留 `MUSIC_PROVIDER=suno|minimax|mock` 或等价开关。

当前进展：

- 已新增 `modules:music-provider`，预置统一 Provider 合约和 `MockMusicProvider`。
- 已新增 `modules:suno`，实现通过 DreamMaker run/status 协议提交与轮询的 `SunoMusicProvider` 骨架。
- 已扩展 `modules:minimax`，实现通过 DreamMaker run/status 协议提交与轮询的 `MiniMaxMusicProvider` 骨架。
- 已在 `.env.example` 预留 `MUSIC_PROVIDER`、`DREAMMAKER_API_BASE_URL`、`DREAMMAKER_ACCESS_KEY`、`DREAMMAKER_SECRET_KEY`、可选 `DREAMMAKER_USER_ACCESS_TOKEN`、`SUNO_MODEL`、`MINIMAX_MODEL`。
- 已新增 `MusicProviderSelection` 并接入 `music-api` 配置：`MUSIC_PROVIDER=mock|suno|minimax` 可选择 Provider，默认 `mock`。
- `music-api` 与 `music-worker` 均已注册 Mock、Suno、MiniMax 三类 Provider bean；当前自动化测试仍只用 Mock/Fake，不调用真实 API。真实联调需要本地安全注入 AccessKey/SecretKey，并显式设置 `DREAMMAKER_REAL_CALLS_ENABLED=true`。
- `DreamMakerHttpClient` 已实现每次请求生成 HS256 JWT：AccessKey 写入 `iss`，SecretKey 用作签名密钥，`exp=now+1800s`，`nbf=now-5s`。
- `DreamMakerHttpClient` 与配置属性已下沉到共享 `modules:dreammaker`，确保 API sync/local 和 Temporal worker 路径使用同一套鉴权、安全开关和脱敏逻辑。

### 2.8 Remotion 商用许可与字体授权

现状：

- 技术方案提到生产环境字体必须公司确认授权。
- Remotion 用于商业自动化渲染时需要确认许可和费用口径。

建议补充：

- 增加“视频渲染许可与素材授权”小节。
- 明确字体、背景纹理、封面模板、音频可视化素材不得使用未授权资源。
- 商用上线前确认 Remotion 许可。

### 2.9 OpenSearch 中文检索配置

现状：

- 技术方案写“OpenSearch 中文检索 + 标签过滤 + Prompt Builder 重排”。
- 未明确 analyzer。

建议补充：

- 本地默认可用 `cjk` analyzer。
- 若部署环境支持 ICU plugin，可评估 `icu_analyzer`。
- 语料 chunk 应保留 tags、heading_path、content_type，便于标签过滤和回溯。

### 2.10 项目进度记录机制（已决策）

现状：

- 已新增 `docs/project-progress.md` 和 `AGENTS.md` 规则。

建议补充到技术方案或工程规则：

- 阶段性任务完成后必须更新 `docs/project-progress.md`。
- 技术栈、方案、接口、数据库、部署策略变化时，必须记录变更原因和影响范围。

## 3. 建议新增文档

建议在仓库中逐步补齐：

- `docs/api/openapi-v0.1.yaml`
- `docs/adr/0001-user-web-scope.md`
- `docs/adr/0002-commercial-grade-stack.md`
- `docs/adr/0003-quota-commit-timing.md`（如公司权益口径变化时新增）
- `docs/adr/0004-work-concurrency-control.md`（如实现时需要替换当前 `works.version` 方案时新增）
- `docs/workflow/temporal-workflows-v0.1.md`
- `docs/database/schema-v0.1.md`
- `docs/render/render-worker-template-v0.1.md`
- `docs/runbook/local-development.md`

## 4. 对第 1 批的影响

第 1 批仓库初始化必须执行：

- 创建 `apps/web` scaffold。
- 保留并扩展 `AGENTS.md` 工程命令。
- 纳入 `.gitignore` 和 `.env.example`。
- README 明确当前未实现范围。
- Docker Compose 纳入 PostgreSQL、Redis、Temporal、MinIO、OpenSearch、Prometheus、Grafana。
- 商用级组件完成工程预置，但不实现业务主链路。

第 1 批可以先不等待以下事项完全冻结：

- 真实公司账号协议。
- 真实审核系统协议。
- 真实任务权益系统协议。
- Image 2 具体 HTTP 参数。
- 社区视频最大文件大小、最大时长、推荐码率。

这些可继续通过 Adapter、配置和后续文档补齐。

## 5. 推荐下一步

1. 第 5 批进入 DreamMaker Suno/MiniMax 受控真实联调准备：先补安全配置、联调 runbook、Provider 请求/响应记录与人工开关。
2. 第 6 批补 DeepSeek/知识库写词润色链路。
3. 第 7-8 批补封面、Remotion/FFmpeg MP4 成片和 MinIO/S3 发布包强化。
