# 技术方案需补项

文档日期：2026-06-05

## 1. 背景

`yanyun-ai-music-platform-tech-design-v0.1.md` 已经覆盖总体架构、服务形态、工作流、Provider/Adapter、数据库草案、API 草案、可观测、测试和开发拆分。

在正式进入仓库初始化和后续批次开发前，需要补齐若干会影响工程边界、数据模型和验收口径的决策，避免第 1 批脚手架完成后返工。

## 2. 必须补齐的高优先级事项

### 2.1 用户侧前端工程

现状：

- PRD 明确本阶段需要创作首页、灵感成歌页、填词成歌页、歌词工坊页、歌词确认页、生成中页、成品预览页、我的作品页。
- 技术方案 v0.1 的仓库结构未包含用户侧前端应用。

建议补充：

- 在仓库结构中增加 `apps/web`。
- 明确前端技术栈，建议候选：
  - React + Vite：适合后台/API 驱动型应用，工程轻，适合本地快速跑通。
  - Next.js：适合后续需要 SSR、路由约定、部署平台集成时使用。
  - 公司指定栈：如果公司已有前端基建，应优先兼容。
- 第 1 批是否创建 `apps/web` scaffold 需要明确。

建议默认决策：

- 暂定 `apps/web = React + Vite + TypeScript`。
- 若公司有明确前端技术栈，再替换；替换记录必须写入进度文档。

### 2.2 OpenAPI 接口契约

现状：

- 技术方案已有 API 草案，但尚未形成可冻结、可生成客户端或用于 Mock 的 OpenAPI 文档。

建议补充：

- 新增 `docs/api/openapi-v0.1.yaml`。
- 覆盖用户身份、作品创建、作品查询、歌词改写、确认出歌、封面重生、视频重渲染、发布包、配置后台的接口。
- 明确统一错误响应、分页、幂等 Header、鉴权 Header、Mock 用户 Header。

建议默认决策：

- 第 1 批可只预留 springdoc-openapi；正式 API 契约在第 1 批后、第 2 批前冻结。
- 如果先做前端原型，则 OpenAPI 应提前到第 1 批前完成。

### 2.3 权益扣减时机

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

建议默认决策：

- 商用用户口径采用 B：MP4 发布包生成后扣减主权益。
- 内部成本记录仍在 MiniMax 成功后立即记录。
- 如果公司权益系统要求按歌曲音频扣减，则改用 A，并在 PRD/技术方案中同步解释。

### 2.4 审核链路落点

现状：

- 技术方案定义了 `ModerationAdapter.preCheckUserInput`、`preCheckLyrics`、`preCheckPublishPackage`。
- LyricsPreparationWorkflow 和 SongProductionWorkflow 里有前置审核点，但发布包生成前的审核落点尚不明确。

建议补充：

- 用户输入创建前：`preCheckUserInput`。
- 歌词确认前或确认时：`preCheckLyrics`。
- 发布包对外可获取前：`preCheckPublishPackage`。

建议默认决策：

- 本地阶段全部 MockAllow。
- 正式接入时，发布包未通过审核不得进入 `PACKAGE_READY`。
- 审核失败应记录失败原因，但不等同社区发布失败。

### 2.5 并发控制与 `works.version`

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

建议默认决策：

- 数据库层增加 `version` 字段。
- 状态变更使用乐观锁。
- `confirm`、`polish`、`cover/regenerate`、`video/rerender` 等关键操作在服务层做状态校验和并发保护。

### 2.6 Outbox 或启动补偿

现状：

- 技术方案提到“启动 Temporal workflow 与数据库事务之间使用 Outbox 或状态补偿”，但未定具体方案。

建议补充：

| 方案 | 说明 | 适用性 |
|---|---|---|
| Outbox | DB 事务写入事件，由后台分发启动 Workflow | 更可靠，初期实现略复杂 |
| 状态补偿 | API 写状态后启动 Workflow，失败时定时扫描补偿 | 初期简单，但要写清扫描逻辑 |

建议默认决策：

- 第 1-3 批可用状态补偿，降低初始复杂度。
- 在 SongProductionWorkflow 进入真实模型调用前，引入 Outbox 或可靠补偿任务。

### 2.7 Provider 模式与真实 API 调用边界

现状：

- 技术方案已有 `fake|mixed|real` Provider 模式。
- AGENTS.md 已要求自动化测试不得调用真实 DeepSeek、MiniMax、Image 2。

建议补充：

- 明确 `.env.example` 只放变量名。
- 默认 `PROVIDER_MODE=fake`。
- CI 和自动化测试强制 fake 或 Mock HTTP。
- `real` 模式只能手动联调使用。

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

### 2.10 项目进度记录机制

现状：

- 已新增 `docs/project-progress.md` 和 `AGENTS.md` 规则。

建议补充到技术方案或工程规则：

- 阶段性任务完成后必须更新 `docs/project-progress.md`。
- 技术栈、方案、接口、数据库、部署策略变化时，必须记录变更原因和影响范围。

## 3. 建议新增文档

建议在仓库中逐步补齐：

- `docs/api/openapi-v0.1.yaml`
- `docs/adr/0001-frontend-stack.md`
- `docs/adr/0002-quota-commit-timing.md`
- `docs/adr/0003-work-concurrency-control.md`
- `docs/workflow/temporal-workflows-v0.1.md`
- `docs/database/schema-v0.1.md`
- `docs/render/render-worker-template-v0.1.md`
- `docs/runbook/local-development.md`

## 4. 对第 1 批的影响

第 1 批仓库初始化前，至少应确认：

- 是否创建 `apps/web`。
- `AGENTS.md` 是否保留并扩展工程命令。
- `.gitignore` 和 `.env.example` 是否纳入初始化。
- README 是否明确当前未实现范围。

第 1 批可以先不等待以下事项完全冻结：

- 真实公司账号协议。
- 真实审核系统协议。
- 真实任务权益系统协议。
- Image 2 具体 HTTP 参数。
- 社区视频最大文件大小、最大时长、推荐码率。

这些可继续通过 Adapter、配置和后续文档补齐。

## 5. 推荐下一步

1. 确认前端栈与是否第 1 批创建 `apps/web`。
2. 确认主权益扣减时机。
3. 确认并发控制默认采用 `works.version`。
4. 输出 OpenAPI v0.1。
5. 按 `docs/codex-batch-01-repository-initialization.md` 执行第 1 批工程初始化。

