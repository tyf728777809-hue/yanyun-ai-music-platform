# 项目进度记录

更新时间：2026-06-06 14:55 CST

## 当前阶段

项目已完成第 8 批 MinIO/S3 发布包强化，并已按用户要求在前端并行开发期间推进第 10 批公司 Adapter 接入与部署交接准备和第 11 批 render-worker 本地进程调用边界：数据库 migration、Work 领域状态机、Mock Adapter 边界、OpenAPI v0.1 主路径 API、本地 Mock 作曲与发布包、DreamMaker Provider 骨架、Outbox v0.1、API outbox 到独立 Temporal worker 的编排边界、DreamMaker 真实 Provider 硬开关、DeepSeek / Knowledge / Prompt / Lyrics 写词边界、CoverGenerationService / VideoRenderService 媒体生成边界、render-worker 本地 16:9 MP4 样例渲染、Java 可配置调用 render-worker CLI 的本地进程模式、发布包 JSON 的 local / S3-MinIO 可切换对象存储，以及内部公司接入 readiness 报告均已落地。当前仍未执行真实 Suno/MiniMax、真实 DeepSeek、真实 Image 2 或公司系统调用。

第 9 批前端原型已由用户侧交付到 `prototypes/Claude-web-v1`，并完成本地测试、构建和 390px / 1440px smoke 初审；该原型可作为当前前端验收对象，但尚未严格通过 OpenAPI v0.1 和原前端任务包验收。已新增 `docs/frontend/claude-web-v1-acceptance-fix-task-package.md`，要求前端实现者补齐“我的作品”列表、发布交接信息、`PACKAGE_BLOCKED`、润色必填、`RETRY_COVER` / `RERENDER_VIDEO`、错误 `request_id` 和关键状态展示。

为后续前后端联调提速，Mock 音乐 Provider 已新增可配置模拟音频时长：默认仍为 180000ms，保持 3 分钟商用口径；本地 smoke 可通过 `MOCK_MUSIC_DURATION_MS=1000` 临时压短，用于快速验证 Java 到 render-worker 的 MP4 成片链路。

本地主链路 smoke 已脚本化：新增 `scripts/smoke/api-main-flow.sh`，在 API 已启动后可一键验证健康检查、填词成歌、确认出歌、发布包获取/刷新/标记交接、readiness、PostgreSQL 状态、本地对象文件；local-process 模式下还会使用 `ffprobe` 验证 MP4。

第 2 批后续小阶段已补齐 `Idempotency-Key` 的基础重放语义：同用户、同 operation、同 key、同请求内容会重放第一次成功响应；同 key 不同请求内容返回 `IDEMPOTENCY_CONFLICT`。

音乐生成 Provider 工程边界已根据用户要求预置：统一 `MusicProvider` 合约、`MockMusicProvider`、`SunoMusicProvider` 和 `MiniMaxMusicProvider` 均已建好；当前 Suno/MiniMax 只暴露边界和测试，不调用真实 API。

当前 Mock 出歌流程已接入 `MockMusicProvider`，不再在 `WorkService` 内直接硬编码音频生成结果；后续切换 Suno 或 MiniMax 时已有主链路落点。

`confirmWork` 已进一步拆到 `MockSongProductionWorkflow`：`WorkService` 只负责状态校验和接口返回，出歌编排集中处理权益锁定、音乐 Provider、媒体资产、发布包、发布包审核、权益扣减、失败释放和 generation job 收口。当前仍是同步 Mock Workflow，为后续 Temporal Workflow 接入做准备。

对象存储边界已升级：发布包 JSON 会通过 `ObjectStorageClient` 写入本地文件存储或 S3/MinIO 兼容对象存储，object key 使用 `yanyun-ai-music/{env}/{yyyy}/{MM}/{dd}/{work_id}/package/publish-package.json` 分层结构；`package_url` 由对象存储客户端按持久化 `package_object_key` 签发或刷新，不再在业务代码中硬编码本地 URL。

音乐 Provider 选择已从硬编码 `MOCK` 改为配置驱动：`MUSIC_PROVIDER=mock|suno|minimax` 会映射到统一 `MusicProviderSelection`。当前默认 `mock` 可完整成功；`suno` / `minimax` 已接入 DreamMaker submit + poll 骨架，但缺少 `DREAMMAKER_ACCESS_KEY` / `DREAMMAKER_SECRET_KEY` 时会在发起外部请求前进入 `MUSIC_GENERATION_FAILED` 可重试失败，并释放已锁权益。

DreamMaker / Suno / MiniMax 接入骨架已落地：新增共享 `modules:dreammaker`，`SunoMusicProvider` 和 `MiniMaxMusicProvider` 已可按 DreamMaker run/status 协议构造请求、提交任务、轮询状态、映射失败码，并在成功时返回供应商音频源 URL。Workflow 已补远程音频导入边界，会先把供应商音频 URL 导入本地对象存储，再写入 `AUDIO` 媒体资产。默认仍走 `mock`，自动化测试不调用真实供应商；真实联调需要安全配置 `DREAMMAKER_ACCESS_KEY` / `DREAMMAKER_SECRET_KEY`。

DreamMaker 鉴权口径已根据用户补充资料从待确认项改为已决策：每次请求使用 AccessKey 作为 JWT `iss`、SecretKey 做 HS256 签名，`exp=now+1800s`、`nbf=now-5s`，并以 `Authorization: Bearer <jwt>` 发起请求；`DREAMMAKER_USER_ACCESS_TOKEN` 仅作为可选 `X-Access-Token` 透传。

第 3 批可靠编排基础已启动并完成 Outbox v0.1：新增 `workflow_outbox` 表、`MUSIC_WORKFLOW_DISPATCH_MODE=sync|outbox`、本地 outbox dispatcher 和确认出歌/音乐重试的异步启动边界。默认 `sync` 保持现有 Mock 主链路兼容；显式 `outbox` 模式会在 API 事务内抢占作品状态、写入 generation job 和 outbox event，随后由 dispatcher 异步推进到 `GENERATED / PACKAGE_READY`。

第 4 批 Temporal 真实编排基础已完成：`WORKFLOW_OUTBOX_DISPATCH_TARGET=local|temporal` 可切换本地委托和 Temporal 启动；Temporal 模式下 API dispatcher 只负责按 `work_id + job_id` deterministic workflow id 启动 workflow，独立 `music-worker` 注册 workflow/activity 并复用当前生产委托推进作品到 `GENERATED / PACKAGE_READY`。

第 5 批 DreamMaker 受控真实联调准备已完成：`DREAMMAKER_REAL_CALLS_ENABLED=false` 作为默认硬开关，API 和 worker 共享 `modules:dreammaker` 中的 HTTP client 与 properties；Temporal worker 已注册 Suno/MiniMax Provider；联调需按 `docs/runbook/dreammaker-controlled-real-integration.md` 手动开启。

第 6 批 DeepSeek / 知识库写词润色 Mock 链路已完成：`WorkService` 不再硬编码灵感成歌、填词成歌、润色和续写文本，而是通过 `LyricsGenerationService` 编排 `KnowledgeService`、`PromptTemplateService` 和 `DeepSeekLyricsClient`；当前默认均为 Mock/Fake，不调用真实 DeepSeek。AI 润色和 AI 续写共享 2 次用户侧编辑次数，低质量结果的内部自动重写不消耗用户次数。

第 7 批封面与视频成片基础已完成：`MockSongProductionWorkflow` 不再直接硬编码封面、视频和时间轴路径，而是通过 `CoverGenerationService` 与 `VideoRenderService` 生成 `COVER`、`VIDEO`、`TIMELINE` 资产描述，并连同 `AUDIO` 统一写入 `media_assets`；发布包 JSON 已按媒体资产 object key 组装 `video.url`、`cover.url` 和 `lyrics.timeline_url`。`apps/render-worker` 已新增 `LyricVideo16x9` composition，并可本地渲染 H.264/AAC、1920x1080、约 8 秒的 MP4 样例。

第 8 批 MinIO/S3 发布包强化已完成：新增 S3/MinIO 兼容 `ObjectStorageClient`、统一对象存储配置、结构化发布包 object key、presigned GET URL 签发、`refresh-url` 按数据库 `package_object_key` 重新签发，以及本地文件模式 TTL 口径。Docker Compose 内置 MinIO 已完成发布包写入、对象存在性、URL 下载和刷新签名 smoke；默认 local 模式仍可跑通。

第 10 批公司 Adapter 接入与部署交接准备已并行完成：新增 `CompanyIntegrationProperties`、`IntegrationReadinessService` 和内部接口 `GET /internal/integration-readiness`，可结构化展示账号、审核、权益、发布、分享、音乐 Provider、对象存储、Workflow dispatch 和 DreamMaker 硬开关的当前模式、实现、阻塞状态和所需环境变量；新增公司交接文档，明确真实公司系统仍由公司开发替换 Adapter 实现。

第 11 批 render-worker 本地进程调用边界已完成：`apps/render-worker` 新增 `render:job` CLI，支持按输入 `duration_ms` 动态生成 Remotion composition 时长和句级 timeline；`modules:production` 新增 `LocalProcessVideoRenderService`，可在 `RENDER_WORKER_MODE=local-process` 时调用 Node render-worker，把 MP4 与 timeline JSON 写入当前对象存储并返回真实媒体资产描述。默认仍为 `mock`，自动化测试不会触发真实长视频渲染。

render-worker 本地进程模式已完成首次 API 端到端 smoke，并修复工作目录解析问题：Gradle `bootRun` 下 Java 进程工作目录是 `apps/music-api`，原先默认 `apps/render-worker` 会解析失败；现在本地进程服务会在相对路径不存在时向上查找父目录，因此默认 `apps/render-worker` 可在仓库内启动时正确解析。若 JAR 放到仓库外运行，仍建议显式设置绝对 `RENDER_WORKER_WORKING_DIRECTORY`。

- `yanyun-ai-music-platform-prd-v0.3.md`：商用级产品范围基线。
- `yanyun-ai-music-platform-tech-design-v0.2.md`：商用级技术方案基线。
- `docs/adr/0001-user-web-scope.md`：用户侧 Web 范围决策。
- `docs/adr/0002-commercial-grade-stack.md`：商用级技术栈决策。
- `docs/api/openapi-v0.1.yaml`：作品、生成阶段、失败动作、权益提示和发布包交接接口契约。
- `database/migrations/V202606050245__init_work_domain.sql`：作品域核心业务表。
- `docs/frontend/gemini-batch-02-mock-workflow-task-package.md`：交给 Gemini 的第 2 批前端任务包。
- `docs/specs/deepseek-knowledge-lyrics-v0.1.md`：第 6 批 DeepSeek / 知识库写词润色 Mock 链路规格。
- `docs/specs/cover-video-rendering-v0.1.md`：第 7 批封面与 Remotion/FFmpeg MP4 成片基础规格。
- `docs/specs/minio-s3-publish-package-storage-v0.1.md`：第 8 批 MinIO/S3 发布包强化规格。
- `docs/specs/company-adapter-deployment-handoff-v0.1.md`：第 10 批公司 Adapter 接入与部署交接准备规格。
- `docs/specs/render-worker-local-process-integration-v0.1.md`：第 11 批 Java 到 render-worker 本地进程调用边界规格。
- `docs/handover/company-adapter-deployment-handoff-v0.1.md`：公司开发替换 Mock Adapter 与部署交接说明。
- `docs/frontend/claude-web-v1-acceptance-fix-task-package.md`：Claude Web v1 前端原型验收修复任务包。

## 进度记录规则

- 本文件是项目的持久进度记录，用于避免长线开发中因上下文压缩、换会话或换技术方案导致信息丢失。
- 每次完成阶段性任务后，必须在最终回复前更新本文件。
- 阶段性任务包括：需求或方案调整、技术栈变更、架构决策、工程初始化、批次开发完成、测试验收、重要问题修复、外部系统协议确认、部署或联调进展。
- 更新内容至少包括：完成事项、关键决策、验证结果、当前风险、待确认事项和下一步。
- 不覆盖历史工作日志；新的阶段性进展追加到“工作日志”，必要时同步调整“当前阶段”“待确认事项”和“下一步建议”。
- 如果 PRD、技术方案、OpenAPI、数据库设计或部署方案发生变化，应同步更新相关文档，或在本文件中明确标注“待同步”。

## 已完成

- 初始化 Git 仓库，默认分支为 `main`。
- 初步阅读 PRD v0.2 和技术方案 v0.1。
- 确认产品目标是正式商用级 AI 作曲与视频成片平台，不是一次性活动页。
- 确认技术主方向为模块化单体、Temporal Worker、独立 Remotion 渲染 Worker、Provider/Adapter 边界。
- 整理 Codex 第 1 批仓库初始化任务说明。
- 整理技术方案进入工程初始化前需要补齐的事项。
- 固化 Git commit 提醒规则：默认不主动提交；阶段性任务完成且适合形成快照时提醒用户是否 commit。
- 输出 PRD v0.3，明确商用级目标、本地完整跑通、外部系统职责边界、移动端优先兼容 PC Web、权益扣减和量化验收。
- 输出技术方案 v0.2，明确完整商用级技术栈、第 1 批工程预置、`apps/web`、发布包审核落点、`works.version` 和 OpenAPI 覆盖范围。
- 新增两份 ADR，固化用户侧 Web 范围和商用级技术栈不降级的决策。
- 升级项目级 `AGENTS.md`，将项目身份、固定决策、外部系统边界、前后端规则、验证规则、进度记录、Git 和安全规则固化为后续 Agent 执行手册。
- 补充前端和图片资产协作规则：需要图片资产时优先用 Image 生成；前端视觉实现优先整理任务包交给 Gemini，本 Agent 负责需求拆解、接口、状态、验收和 review。
- 输出 OpenAPI v0.1 接口契约，覆盖用户信息、作品创建、作品状态、AI 改词/续写、确认出歌、封面重生、视频重渲、发布包获取/刷新/标记交接。
- 初始化 Java 21 + Spring Boot 3 + Gradle Kotlin DSL 多模块工程，包含 `apps/music-api`、`apps/music-worker` 和 `modules/*` 商用级边界。
- 初始化 `apps/web` React + Vite + TypeScript scaffold，移动端优先、兼容 PC Web，只做工程验证页。
- 初始化 `apps/render-worker` Node.js 22 + TypeScript + Remotion scaffold，保留最小 16:9 composition 和 smoke test。
- 新增本地 Docker Compose 基础设施：PostgreSQL 16、Redis 7、Temporal、Temporal UI、MinIO、OpenSearch、Prometheus、Grafana。
- 新增 `.gitignore`、`.dockerignore`、`.env.example`、README、本地运行手册、数据库/知识库预留目录。
- 为 `music-worker` 增加 Temporal 启动连接探测，启动时会验证 `localhost:7233` 可连接，失败时输出明确 target/namespace/taskQueue。
- 已提交第 1 批工程初始化快照：`992762a chore: initialize commercial-grade project scaffold`。
- 新增 Flyway migration，落库 `works`、`work_inputs`、`lyrics_drafts`、`generation_jobs`、`media_assets`、`publish_packages`、`provider_calls`、`quota_transactions`、知识库、Prompt 模板、系统配置和幂等键等表。
- 实现 `work-domain` 领域枚举与状态机，覆盖 OpenAPI v0.1 的作品状态、生成阶段、发布包状态、失败码和可执行动作。
- 实现本地 Mock Adapter 边界：账号、权益、审核、发布包交接均通过接口隔离，真实公司系统后续替换实现即可。
- 实现 `music-api` 主路径接口：`/api/v1/me`、灵感成歌、填词成歌、作品列表/详情、润色/续写、确认出歌、封面重生、视频重渲、发布包获取/刷新/标记交接。
- 实现 Mock 作品生成闭环：创建作品后得到歌词草案；确认出歌后生成 Mock 媒体资源、发布包 JSON、下载 URL，并将状态推进到 `GENERATED` / `PACKAGE_READY`；标记交接后进入 `PACKAGE_FETCHED`。
- 新增第 2 批 Gemini 前端任务包，明确移动端优先 + PC Web 的页面、接口、状态、错误和验收范围。
- 根据用户补充，记录音乐生成后续需同时接入 Suno 与 MiniMax，并预留可配置开放策略；飞书资料已通过 `lark-cli` 授权读取，真实 run/status 接口要点已整理到集成说明。
- 实现 `Idempotency-Key` 基础语义，覆盖当前所有 POST 主路径，成功响应写入 `idempotency_keys`，重复提交可重放响应，参数冲突返回 `IDEMPOTENCY_CONFLICT`。
- 新增 `modules:music-provider`，定义音乐生成统一 Provider 合约、请求、结果、状态、Provider 类型和注册选择器。
- 新增 `modules:suno`，预置 `SunoMusicProvider` 边界；后续已升级为 DreamMaker submit + poll 骨架，自动化测试仍不会调用真实 Suno API。
- 扩展 `modules:minimax`，预置 `MiniMaxMusicProvider` 边界；后续已升级为 DreamMaker submit + poll 骨架，自动化测试仍不会调用真实 MiniMax API。
- `.env.example` 增加 `MUSIC_PROVIDER=mock` 和 DreamMaker 统一接入变量名，不包含真实凭据。
- `music-api` 已依赖 `modules:music-provider`，通过 Spring 配置注册 `MockMusicProvider` 和 `MusicProviderRegistry`。
- `WorkService.confirmWork` 已通过 `MusicProviderRegistry.require(MOCK)` 生成 Mock 音频结果，再继续封面、视频、发布包链路。
- 新增 `MockSongProductionWorkflow`，将确认出歌后的生成编排从 `WorkService` 中拆出，集中处理权益锁定、Provider 调用、媒体资产、发布包、发布包审核、权益提交和失败释放。
- `generation_jobs` 已增加完成状态更新方法，`SONG_PRODUCTION` job 成功后会收口到 `SUCCEEDED / PACKAGE_READY`，失败后会收口到 `FAILED / FAILED` 并记录失败码。
- 新增 `MockSongProductionWorkflowTest`，覆盖成功出发布包和音乐 Provider 失败时释放已锁权益两个关键路径。
- 新增 `modules:storage` 对象存储合约和 `LocalObjectStorageClient`，本地阶段可把发布包 JSON 写到 `build/local-object-storage/yanyun-works-local`。
- `MockSongProductionWorkflow` 已接入对象存储写入；发布包准备或写入失败会进入 `PACKAGE_BUILD_FAILED`，释放已锁权益，并标记为可重试失败。
- 本地运行手册已补充 package JSON 写入位置和检查方式。
- 新增 S3/MinIO 兼容对象存储实现，支持 endpoint override、bucket、region、path-style、自动创建本地 bucket 和 presigned GET URL。
- 发布包 object key 已改为 `yanyun-ai-music/{env}/{yyyy}/{MM}/{dd}/{work_id}/package/publish-package.json`；`refresh-url` 已改为使用持久化 `package_object_key` 重新签发 URL。
- `.env.example` 和 API/worker 配置已补齐 `OBJECT_STORAGE_PROVIDER=local|s3`、`S3_ENDPOINT`、`S3_PUBLIC_ENDPOINT`、bucket、region、TTL 等本地对象存储变量。
- 新增公司接入 readiness 边界：`modules:config-center` 提供 `IntegrationReadinessService`，`music-api` 暴露内部 `GET /internal/integration-readiness`，用于交接和部署前检查 Mock/真实接入状态。
- 新增公司 Adapter 交接说明，覆盖账号、审核、权益、发布、分享的替换接口、部署变量、smoke 步骤和禁止事项。
- 新增 render-worker 本地进程调用边界：`RENDER_WORKER_MODE=local-process` 时 Java `VideoRenderService` 可调用 `apps/render-worker` 的 `render:job` CLI，把生成的 MP4 和 timeline JSON 写入对象存储；默认 `mock` 不触发真实渲染。
- 新增 `MOCK_MUSIC_DURATION_MS`，允许本地 smoke 临时压短 Mock 音频时长；默认仍为 180000ms。
- 完成 `prototypes/Claude-web-v1` 前端原型初审：测试、typecheck、build 和 390px / 1440px Playwright smoke 已通过，但仍有契约和验收缺口。
- 新增 `docs/frontend/claude-web-v1-acceptance-fix-task-package.md`，把前端缺口收敛成可交给前端实现者的修复任务包。
- 新增 `MusicProviderSelection`，解析 `MUSIC_PROVIDER=mock|suno|minimax`；`music-api` 已注册 Mock、Suno、MiniMax 三类 Provider bean。
- `MockSongProductionWorkflow` 已按配置选择音乐 Provider；Provider 未实现或抛异常时会进入 `MUSIC_GENERATION_FAILED`，释放权益并关闭 job。
- 修正 `IdempotencyService` 外层事务边界：`ResponseStatusException` 不再回滚业务失败状态，避免配置到未实现 Provider 时作品仍停留在 `LYRICS_READY`。

## 当前关键判断

- PRD v0.3 和技术方案 v0.2 作为新的项目启动基线。
- 项目按正式商用级平台建设，不按 Demo 或临时活动页设计。
- 本地阶段必须跑通完整生成链路；公司账号、审核、权益、发布、分享等真实接入由公司开发后续替换 Mock Adapter。
- 用户侧由本项目提供 `apps/web`，移动端优先，同时兼容 PC Web。
- 前端视觉和页面实现优先整理成可转交 Gemini 的任务包，本 Agent 不默认承担最终高保真前端实现。
- 后续如需要图片资产，优先使用 Image 生成，并在提交前确认用途、尺寸、来源和是否纳入仓库。
- 当前前端验收对象为 `prototypes/Claude-web-v1`；`apps/web` 仍是正式工程 scaffold。前端视觉实现继续由外部前端实现者负责，本 Agent 默认产出任务包、接口状态 review 和联调验收，不直接重写高保真前端。
- `MOCK_MUSIC_DURATION_MS` 只用于本地 Mock 和联调提速，不代表真实音乐模型时长策略。
- Temporal v0.1 先证明 API outbox 到独立 worker 的可靠启动边界；activity 自动重试固定为 1 次，等权益、Provider、媒体和发布包写入幂等性审计完成后再放开。
- Suno/MiniMax 真实调用必须同时满足 AK/SK 安全注入和 `DREAMMAKER_REAL_CALLS_ENABLED=true`；默认关闭，自动化测试不得真实调用。
- `/internal/integration-readiness` 只用于内部交接/部署前检查，不是用户侧 API；它只读取配置和静态边界，不调用真实公司系统或真实供应商。
- 第 1 批代码应搭建完整商用级工程边界和本地基础设施，不实现业务主链路。
- Boot 应用只产出可执行 jar，不提交 plain jar、构建缓存、node_modules、大体积媒体或真实密钥。

## 上一轮文档基线验证结果

- 已做文档一致性搜索，PRD、技术方案、补项文档、第 1 批任务说明、ADR 和 `AGENTS.md` 已同步到 PRD v0.3 / 技术方案 v0.2 基线。
- 已检查关键新增口径：`apps/web`、React + Vite + TypeScript、移动端优先兼容 PC Web、`PACKAGE_BLOCKED`、`ModerationAdapter.preCheckPublishPackage`、`works.version`、OpenAPI v0.1 覆盖范围。
- 旧版 `v0.2` / `v0.1` 引用仅保留在历史记录、版本记录或“下一步交付物”语境中。
- 已做敏感信息搜索，未发现真实密钥、Token、私钥或生产凭据。
- 已检查 `AGENTS.md` 覆盖项目身份、Source of Truth、固定技术决策、Mock Adapter 边界、前后端规则、图片资产规则、Gemini 前端任务包规则、验证规则、进度记录和 Git commit 指引。
- 本轮只修改 Markdown 文档，未运行代码构建或测试。
- 用户已要求执行 commit，本轮文档基线已纳入 Git 快照。

## 第 1 批工程初始化验证结果

- `docs/api/openapi-v0.1.yaml` 已通过 Ruby YAML 语法解析检查。
- `./gradlew clean build` 成功。
- `./gradlew test` 成功。
- `./gradlew spotlessCheck` 成功。
- `cd apps/web && npm run build && npm test` 成功，Vitest 1 个测试通过。
- `cd apps/render-worker && npm run build && npm test` 成功，Node smoke test 1 个测试通过。
- `docker compose -f deploy/docker-compose.yml --env-file .env.example config` 成功。
- `docker compose -f deploy/docker-compose.yml --env-file .env.example up -d` 成功；PostgreSQL、Redis、Temporal、MinIO、OpenSearch、Prometheus、Grafana 均已启动，主要服务 healthcheck 为 healthy，Temporal UI 已启动。
- `music-api` 可执行 jar smoke 成功：`/health` 返回 `status=OK`，`/actuator/health` 返回 `UP`。
- `music-worker` 可执行 jar smoke 成功：`/actuator/health` 返回 `UP`，启动日志确认 `Temporal connection verified. target=localhost:7233, namespace=default, taskQueue=song-production-local`。
- 敏感信息扫描未发现真实密钥、Token、私钥或 Cookie；命中的 `task-queue` 为技术方案/配置字段，不是敏感凭据。
- 大文件扫描未发现需要提交的大体积产物。
- 本机环境变更：已通过 Homebrew 安装 `openjdk@21` 和 `gradle`；Homebrew 为 Gradle 额外安装了 `openjdk` 依赖。项目验证命令均显式使用 `/opt/homebrew/opt/openjdk@21`。

## 第 2 批首个后端 Mock 闭环验证结果

- `./gradlew test` 成功。
- `./gradlew spotlessCheck` 成功。
- `./gradlew :apps:music-api:bootJar` 成功。
- `./gradlew :apps:music-api:bootJar spotlessCheck test` 成功。
- `music-api` 启动成功，Flyway 连接本地 PostgreSQL 并应用 `V202606050245__init_work_domain.sql`；二次启动时验证 schema 已 up-to-date。
- HTTP smoke 成功：`POST /api/v1/works/inspiration` 创建作品，返回 `LYRICS_READY` 和 `WAITING_CONFIRM`。
- HTTP smoke 成功：`GET /api/v1/works/{work_id}` 返回歌词草案、权益提示、可执行动作和发布交接提示。
- HTTP smoke 成功：`POST /api/v1/works/{work_id}/confirm` 将作品推进到 `GENERATED` / `PACKAGE_READY`。
- HTTP smoke 成功：`GET /api/v1/works/{work_id}/publish-package` 返回 `PACKAGE_READY`、Mock MP4、封面、歌词、时间轴和发布包 URL。
- HTTP smoke 成功：`POST /api/v1/works/{work_id}/publish-package/mark-fetched` 将发布包推进到 `PACKAGE_FETCHED`。
- HTTP smoke 成功：`POST /api/v1/works/lyrics` 的 `work_code` 已验证为 `YYM-YYYYMMDD-XXXXXX` 格式。
- 本轮未启动真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统请求。

## 第 2 批幂等语义验证结果

- `./gradlew :apps:music-api:test spotlessCheck test` 成功。
- `./gradlew :apps:music-api:bootJar` 成功。
- HTTP smoke 成功：同一 `Idempotency-Key`、同一请求内容重复调用 `POST /api/v1/works/lyrics`，返回同一个 `work_id` 和同一份成功响应。
- HTTP smoke 成功：同一 `Idempotency-Key`、不同请求内容调用 `POST /api/v1/works/lyrics`，返回 HTTP 409，错误码为 `IDEMPOTENCY_CONFLICT`。
- `music-api` 已在 smoke 后停止，未留下占用 `8080` 的 API 进程。

## 第 2 批音乐 Provider 边界验证结果

- `./gradlew spotlessApply spotlessCheck test` 成功。
- `MockMusicProviderTest` 成功：Mock Provider 返回 `SUCCEEDED`、音频对象 key 和模拟时长。
- `MiniMaxMusicProviderTest` 成功：Provider 类型为 `MINIMAX`，本地阶段调用真实提交方法会抛出未实现异常，避免自动化误触真实 API。
- `SunoMusicProviderTest` 成功：Provider 类型为 `SUNO`，本地阶段调用真实提交方法会抛出未实现异常，避免自动化误触真实 API。

## 第 2 批 MockMusicProvider 接入验证结果

- `./gradlew spotlessCheck test` 成功。
- `./gradlew :apps:music-api:bootJar` 成功。
- HTTP smoke 成功：`POST /api/v1/works/lyrics` 创建作品后，`POST /api/v1/works/{work_id}/confirm` 通过 `MockMusicProvider` 推进到 `GENERATED` / `PACKAGE_READY`。
- HTTP smoke 成功：作品详情中的 `media_assets.audio_url` 使用 `MockMusicProvider` 返回的 `audio/{work_id}.mp3` 对象 key，`video_duration_ms` 为 180000。
- `music-api` 已在 smoke 后停止，未留下占用 `8080` 的 API 进程。

## 第 2 批 Mock Workflow 编排验证结果

- `./gradlew spotlessApply spotlessCheck test` 成功。
- `MockSongProductionWorkflowTest` 成功：成功路径会写入 4 类媒体资产、发布包 JSON、提交权益，并将 `SONG_PRODUCTION` job 更新为 `SUCCEEDED / PACKAGE_READY`。
- `MockSongProductionWorkflowTest` 成功：音乐 Provider 失败路径会释放已锁权益、记录 `MUSIC_GENERATION_FAILED`、标记作品失败，并将 job 更新为 `FAILED / FAILED`。
- `./gradlew :apps:music-api:bootJar` 成功。
- 新 JAR HTTP smoke 成功：`POST /api/v1/works/lyrics` 创建作品后，`POST /api/v1/works/{work_id}/confirm` 通过 `MockSongProductionWorkflow` 推进到 `GENERATED` / `PACKAGE_READY`。
- 新 JAR HTTP smoke 成功：`GET /api/v1/works/{work_id}/publish-package` 返回发布包 URL、Mock MP4 URL、封面、歌词和时间轴。
- PostgreSQL 抽查成功：最新 smoke 作品的 `generation_jobs` 中 `SONG_PRODUCTION|SUCCEEDED|PACKAGE_READY|completed_at=true`，没有残留运行中的生成 job。

## 第 2 批 Mock 对象存储验证结果

- `./gradlew spotlessApply spotlessCheck test` 成功。
- `LocalObjectStorageClientTest` 成功：可写入本地对象文件并返回公开 URL；路径穿越 key 会被拒绝。
- `MockSongProductionWorkflowTest` 成功：成功路径会调用 `ObjectStorageClient.putObject` 写入 package JSON；storage 写入失败会释放权益、记录 `PACKAGE_BUILD_FAILED`、标记作品失败，并将 job 更新为 `FAILED / FAILED`。
- `./gradlew :apps:music-api:bootJar` 成功。
- 新 JAR HTTP smoke 成功：作品确认出歌后返回 `PACKAGE_READY` 和 `package_url=http://localhost:9000/yanyun-works-local/packages/{work_id}.json`。
- 本地文件 smoke 成功：`build/local-object-storage/yanyun-works-local/packages/{work_id}.json` 已真实写出，文件内 `work_id` 和 `video.url` 与接口返回一致。
- PostgreSQL 抽查成功：最新 smoke 作品的 `SONG_PRODUCTION` job 为 `SUCCEEDED / PACKAGE_READY`，且 `completed_at` 非空。

## 第 2 批 Provider 配置选择验证结果

- `./gradlew spotlessApply spotlessCheck test :apps:music-api:bootJar` 成功。
- `MusicProviderSelectionTest` 成功：空配置默认 `MOCK`，`mock` / `suno` / `MINIMAX` 可解析，未知 Provider 会拒绝。
- `MockSongProductionWorkflowTest` 成功：配置选择 `MINIMAX` 时会调用对应 Provider；配置选择 `SUNO` 且 Provider 抛未实现异常时，会释放权益、标记 `MUSIC_GENERATION_FAILED`，并关闭 job。
- 默认 `MUSIC_PROVIDER=mock` HTTP smoke 成功：确认出歌后进入 `GENERATED / PACKAGE_READY`，发布包文件已写出。
- `MUSIC_PROVIDER=suno` HTTP smoke 成功验证失败边界：确认出歌返回 HTTP 409，作品详情持久化为 `FAILED / FAILED / MUSIC_GENERATION_FAILED`，`retryable=true`，`package_status=PACKAGE_NOT_READY`。
- PostgreSQL 抽查成功：`suno` 失败作品的 `SONG_PRODUCTION` job 为 `FAILED / FAILED / MUSIC_GENERATION_FAILED / completed_at=true`，权益流水包含 `LOCK_GENERATE|LOCKED` 和 `RELEASE_GENERATE|RELEASED`。

## 第 2 批失败恢复与重试闭环验证结果

- 新增 `POST /api/v1/works/{work_id}/music/retry`，仅允许 `FAILED`、`retryable=true` 且失败码属于音乐生成类失败的作品重试。
- `ConfirmWorkRequest` 与 `RetryMusicRequest` 均支持可选 `music_provider`，用于本地和联调阶段按请求选择 `mock` / `suno` / `minimax`。
- `MockSongProductionWorkflow` 已支持请求级 Provider 覆盖服务端 `MUSIC_PROVIDER` 配置；默认配置 `suno` 失败后，可在重试请求中切回 `mock` 完成恢复。
- `WorkStateMachineTest` 成功：`MUSIC_GENERATION_FAILED` 可重试，`PACKAGE_BUILD_FAILED` 不开放音乐重试。
- `MockSongProductionWorkflowTest` 成功：请求级 `music_provider=mock` 可覆盖服务端 `MUSIC_PROVIDER=suno`，并完成出歌链路。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/openapi-v0.1.yaml')"` 成功，OpenAPI v0.1 YAML 可解析。
- `./gradlew spotlessApply spotlessCheck test` 成功。
- `./gradlew :apps:music-api:bootJar` 成功。
- 新 JAR HTTP smoke 成功：`MUSIC_PROVIDER=suno` 启动后，确认出歌返回 HTTP 409，作品详情为 `FAILED / FAILED / MUSIC_GENERATION_FAILED / retryable=true`，`available_actions` 包含 `RETRY_MUSIC`。
- 新 JAR HTTP smoke 成功：调用 `POST /api/v1/works/{work_id}/music/retry` 且请求体为 `{"music_provider":"mock"}` 后，作品恢复到 `GENERATED / PACKAGE_READY`，发布包文件写入 `build/local-object-storage/yanyun-works-local/packages/{work_id}.json`。
- PostgreSQL 抽查成功：同一作品先有 `SONG_PRODUCTION|FAILED|FAILED|MUSIC_GENERATION_FAILED`，后有 `SONG_PRODUCTION|SUCCEEDED|PACKAGE_READY`；权益流水为 `LOCK_GENERATE -> RELEASE_GENERATE -> LOCK_GENERATE -> COMMIT_GENERATE`。

## 第 2 批重试稳定性增强验证结果

- 新增 Flyway migration `V202606050520__add_music_retry_count.sql`，为 `works` 增加 `music_retry_count` 和非负约束。
- 音乐重试上限固定为 2 次；作品详情的 `failure` 新增 `retry_count`、`retry_limit`、`remaining_retry_count` 和 `recommended_action`。
- `retryMusic` 在启动 Workflow 前会用 `works.version` 和条件更新预约本次重试，只允许当前版本、当前用户、音乐类失败、`retryable=true` 且次数未耗尽的作品进入 `GENERATING / QUOTA_LOCKING`。
- `WorkStateMachine` 已把剩余重试次数纳入 `RETRY_MUSIC` 暴露条件；次数耗尽后不再返回 `RETRY_MUSIC`，改由 `CONTACT_SUPPORT` / `RETURN_TO_EDIT` 承接。
- `MockSongProductionWorkflow` 已支持“本次失败后是否仍可重试”的输入；最后一次音乐重试失败会写入 `retryable=false`。
- `./gradlew spotlessApply spotlessCheck test :apps:music-api:bootJar` 成功。
- Flyway 启动验证成功：本地 PostgreSQL 从版本 `202606050245` 迁移到 `202606050520`。
- HTTP smoke 成功：`MUSIC_PROVIDER=suno` 下，初始确认失败返回 `retry_count=0 / retry_limit=2 / remaining_retry_count=2`，第一次重试失败后剩余 1 次，第二次重试失败后 `remaining_retry_count=0`、`retryable=false` 且不再返回 `RETRY_MUSIC`。
- HTTP smoke 成功：次数耗尽后即使请求 `{"music_provider":"mock"}`，`POST /api/v1/works/{work_id}/music/retry` 仍返回 HTTP 409。
- HTTP smoke 成功：另一条作品在初始 `suno` 失败后请求 `{"music_provider":"mock"}`，仍可恢复到 `GENERATED / PACKAGE_READY` 并写出本地发布包。
- HTTP smoke 成功：非法 `music_provider` 返回 HTTP 400，作品保持 `FAILED / MUSIC_GENERATION_FAILED`，`music_retry_count` 不增加，随后仍可用 `mock` 重试恢复成功。
- PostgreSQL 抽查成功：耗尽作品为 `FAILED / MUSIC_GENERATION_FAILED / retryable=false / music_retry_count=2`；恢复作品为 `GENERATED / PACKAGE_READY / music_retry_count=1`；权益流水符合失败释放、成功提交口径。

## 第 2 批 Provider 调用记录与接入前置结果

- 已启用已有 `provider_calls` 表：`MockSongProductionWorkflow` 每次调用音乐 Provider 后记录 `provider`、`operation`、`provider_trace_id`、`status`、`latency_ms`、`request_hash`、`prompt_hash`、`error_code` 和 `error_message`。
- `MockSongProductionWorkflowTest` 已验证成功路径写入 `MOCK / MUSIC_GENERATION / SUCCEEDED`，失败路径写入 `MOCK / MUSIC_GENERATION / FAILED` 和 provider 原始失败码。
- `./gradlew spotlessApply spotlessCheck test :apps:music-api:bootJar` 成功。
- HTTP smoke 成功：`MUSIC_PROVIDER=suno` 初始失败后，切换 `mock` 重试恢复成功。
- PostgreSQL 抽查成功：同一作品写入两条 provider call，分别为 `SUNO|MUSIC_GENERATION|FAILED|PROVIDER_EXCEPTION` 和 `MOCK|MUSIC_GENERATION|SUCCEEDED`，`request_hash` 与 `prompt_hash` 均为 64 位 SHA-256 hex。
- 新增 `docs/integrations/suno-minimax-preintegration-notes.md`，记录 Suno / MiniMax 的 DreamMaker run/status 接口、鉴权方式、请求字段、状态字段、输出文件字段和待确认项。
- 飞书参考资料已通过 `lark-cli docs +fetch` 成功读取；后续用户补充确认 DreamMaker 使用 AccessKey/SecretKey 生成 HS256 JWT，并以 `Authorization: Bearer <jwt>` 调用。
- 新增 `docs/specs/dreammaker-music-provider-v0.1.md`，作为 DreamMaker / Suno / MiniMax 接入骨架规格，明确不提交密钥、自动化不调用真实 Provider、供应商音频 URL 必须先导入对象存储。
- 新增 `modules:dreammaker`，定义 DreamMaker client、run/status request、submit/status response、任务状态、输出文件和失败映射。
- `SunoMusicProvider` 已实现 DreamMaker 参数构造、任务提交、状态轮询、成功音频输出提取、`PROVIDER_TIMEOUT` / `RATE_LIMITED` / `MUSIC_QUALITY_FAILED` 等内部失败码映射。
- `MiniMaxMusicProvider` 已实现 DreamMaker 参数构造、任务提交、状态轮询、成功音频输出提取；确认歌词存在时默认 `lyrics_optimizer=false`，避免改写用户已确认歌词。
- `DreamMakerHttpClient` 已接入 `DREAMMAKER_API_BASE_URL`、`DREAMMAKER_ACCESS_KEY`、`DREAMMAKER_SECRET_KEY`、可选 `DREAMMAKER_USER_ACCESS_TOKEN`、轮询次数、轮询间隔、请求超时和模型配置；客户端会按请求生成 HS256 JWT。
- 新增 `RemoteObjectImporter` 和 `HttpRemoteObjectImporter`，支持把 Provider 返回的 HTTP(S) 音频 URL 下载并写入 `ObjectStorageClient`，拒绝非 HTTP(S) URL 并限制最大下载大小。
- `MockSongProductionWorkflow` 已支持 Provider 音频源 URL 导入；导入失败按 `PACKAGE_BUILD_FAILED` 收口并释放权益，避免作品半生成。
- 新增 `docs/specs/reliable-song-production-orchestration-v0.1.md`，定义 Outbox v0.1 的功能要求、验收口径、边界和非目标。
- 新增 Flyway migration `V202606052300__add_workflow_outbox.sql`，落库 `workflow_outbox`，支持事件状态、尝试次数、锁、失败信息和 processed 状态。
- 新增 `WorkflowDispatchProperties`，支持 `MUSIC_WORKFLOW_DISPATCH_MODE=sync|outbox` 和 outbox dispatcher 配置。
- `WorkService.confirmWork` 在默认 sync 模式保持请求内执行；在 outbox 模式会先将作品置为 `GENERATING / QUOTA_LOCKING`，插入 `SONG_PRODUCTION` job 和 outbox event 后立即返回 202。
- `WorkService.retryMusic` 在 outbox 模式下沿用 `works.version` 和重试次数抢占逻辑，随后写入 outbox event 异步执行。
- `MockSongProductionWorkflow` 已支持复用既有 `generation_jobs.id`，dispatcher 执行时不会重复创建 job。
- 新增 `WorkflowOutboxDispatcher`，可 claim due outbox events，执行 `SongProductionWorkflow`，并将事件标记为 `SUCCEEDED` / `FAILED` / `SKIPPED`；已处理终态作品不重复执行的问题。
- 新增 `docs/specs/temporal-song-production-orchestration-v0.1.md`，定义第 4 批 Temporal 真实编排基础规格、验收口径和非目标。
- 新增 `modules:production`，把 `WorkRepository` 与当前 `MockSongProductionWorkflow` 业务委托从 API 应用中抽出，供 API local 模式和 worker activity 复用。
- 新增 `modules:workflow`，定义 Temporal workflow/activity 合约、deterministic workflow id 和 `TemporalSongProductionWorkflowImpl`。
- `WorkflowOutboxDispatcher` 已改为依赖 `SongProductionWorkflowStarter`，可按配置走 local starter 或 Temporal starter。
- `TemporalSongProductionWorkflowStarter` 已用 `work_id + job_id` 生成 deterministic workflow id；重复启动命中 `AlreadyStarted` 时按幂等成功处理。
- `music-worker` 已注册 Temporal workflow 与 activity；activity 通过 `SongProductionActivityAdapter` 委托当前生产链路，worker 生命周期会启动并关闭 `WorkerFactory` 和 Temporal stubs。
- `music-worker` 已补齐本地 Mock 生产链路所需 Spring bean：Mock 权益、审核、发布、对象存储、远程对象导入和音乐 Provider 选择。
- `DreamMakerHttpClient` 与 `DreamMakerProperties` 已从 API 应用移动到共享 `modules:dreammaker`，API sync/local 和 Temporal worker 路径共用同一套 JWT、配置和安全检查。
- 新增 `DREAMMAKER_REAL_CALLS_ENABLED`，默认 false；未显式打开时，即使请求级 `music_provider=suno|minimax` 也会在外部 HTTP 请求前失败。
- 新增 `docs/specs/deepseek-knowledge-lyrics-v0.1.md`，定义第 6 批 DeepSeek / 知识库写词润色 Mock 链路规格、非目标、持久化口径和验收标准。
- 新增 `modules:knowledge`，定义 `KnowledgeService`、检索请求/结果、知识库引用和 `MockKnowledgeService`。
- 新增 `modules:prompt`，定义 `PromptTemplateService`、Prompt 渲染请求/结果和 `MockPromptTemplateService`。
- 新增 `modules:deepseek`，定义 `DeepSeekLyricsClient`、写词请求/响应和 `MockDeepSeekLyricsClient`。
- 新增 `modules:lyrics`，定义 `LyricsGenerationService`、写词请求/结果、操作类型和 `DefaultLyricsGenerationService`，负责知识库检索、Prompt 渲染、模型生成和低质量自动重写。
- `WorkService` 已接入统一写词链路，灵感成歌、填词成歌、AI 润色、AI 续写都通过 `LyricsGenerationService` 产出歌词草案。
- `lyrics_drafts` 已启用既有 `cover_prompt_seed`、`quality_score`、`knowledge_base_version` 和 `prompt_template_versions` 字段，用于后续封面链路、审计和回放。
- AI 润色和 AI 续写已共享 2 次用户侧编辑上限；第三次 AI 编辑返回业务冲突。
- 修复幂等层包裹写操作时业务 4xx 被事务提交异常覆盖成 500 的问题；相关写方法已声明 `noRollbackFor = ResponseStatusException.class`。
- `Idempotency-Key` 缺失或过短现在由业务校验返回 400，不再被 Spring 缺失 header 异常兜底成 500。
- `music-worker` 已注册 DreamMaker client、Suno Provider 和 MiniMax Provider；Temporal 模式真实联调不会因为 worker 仅有 Mock Provider 而失败。
- `MusicGenerationResult` 已增加 `modelName`，`provider_calls.model_name` 可写入 `suno:music-gen:{model}`、`music-minimax:text-to-music:{model}` 或 `mock`。
- 供应商失败消息进入 `provider_calls`、作品失败状态和用户响应前会脱敏 Bearer token、JWT、key/token 字段并截断。
- 新增第 5 批操作文档：受控真实联调 runbook、验收清单、凭据与日志规则、开放问题跟踪、公司交接说明。
- 新增 `modules:image2`，定义封面生成请求、结果、服务接口和 `MockCoverGenerationService`；当前返回 deterministic 16:9 Mock 封面资产描述，不调用真实 Image 2。
- 新增 `modules:media`，定义媒体资产描述、视频渲染请求、结果、服务接口和 `MockVideoRenderService`；当前返回 deterministic 16:9 Mock 视频与时间轴资产描述，不调用真实外部渲染服务。
- `MockSongProductionWorkflow` 已通过 `CoverGenerationService` / `VideoRenderService` 生成并写入 `COVER`、`VIDEO`、`TIMELINE` 资产，发布包 JSON 不再写死封面、视频和 timeline 路径。
- `MockSongProductionWorkflowTest` 已覆盖成功路径四类资产写入、封面/视频 1920x1080、发布包引用媒体 object key，以及视频渲染失败时释放权益并阻止发布包生成。
- `apps/render-worker` 已新增 `LyricVideo16x9`、样例输入和 `render:sample` 脚本，可用 Remotion/FFmpeg 渲染本地 MP4。

## 第 2 批 DreamMaker Provider 接入骨架验证结果

- `./gradlew spotlessApply` 成功。
- `./gradlew spotlessCheck test :apps:music-api:bootJar` 成功。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/openapi-v0.1.yaml')"` 成功，OpenAPI YAML 可解析。
- `DreamMakerTaskStatusTest` 成功：DreamMaker `queued` / `running` / `success` / `failed` 状态可映射。
- `SunoMusicProviderTest` 成功：Fake DreamMaker 成功响应可返回 `SUCCEEDED` 和音频源 URL；长期 `running` 会映射为 `PROVIDER_TIMEOUT`。
- `MiniMaxMusicProviderTest` 成功：Fake DreamMaker 成功响应可返回 `SUCCEEDED` 和音频源 URL；失败状态可映射为 `MUSIC_QUALITY_FAILED`。
- `DreamMakerHttpClientTest` 成功：本地 HTTP server 模拟 DreamMaker run/status，可解析 task id、状态、相对音频 URL 和 duration；缺失 `DREAMMAKER_ACCESS_KEY` / `DREAMMAKER_SECRET_KEY` 会在发请求前失败。
- `HttpRemoteObjectImporterTest` 成功：本地 HTTP 音频可导入 `ObjectStorageClient`；非 HTTP(S) source URL 会被拒绝。
- `MockSongProductionWorkflowTest` 成功：已有 object key 不触发下载；Provider 音频源 URL 会先导入对象存储再写入 `AUDIO` 媒体资产。
- 本地 API JAR smoke 成功：默认 `MUSIC_PROVIDER=mock` 下，`/health` 返回 `OK`，作品可从填词创建推进到 `GENERATED / PACKAGE_READY`，发布包 URL 存在。
- 本地 API JAR smoke 成功：请求级 `music_provider=suno` 且未配置 `DREAMMAKER_ACCESS_KEY` / `DREAMMAKER_SECRET_KEY` 时，确认出歌返回 HTTP 409，作品持久化为 `FAILED / MUSIC_GENERATION_FAILED / retryable=true`，`available_actions` 包含 `RETRY_MUSIC`。
- smoke 后已停止 `music-api`，`8080` 未残留监听进程。

## 第 3 批 Outbox 可靠编排基础验证结果

- `./gradlew :apps:music-api:test` 成功。
- `./gradlew spotlessApply` 成功。
- `./gradlew spotlessCheck test :apps:music-api:bootJar` 成功。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/openapi-v0.1.yaml')"` 成功，OpenAPI YAML 可解析。
- `WorkServiceWorkflowDispatchTest` 成功：默认 sync 模式仍调用 workflow inline；outbox 模式会写 generation job、enqueue outbox，并返回 `GENERATING / QUOTA_LOCKING`。
- `WorkflowOutboxDispatcherTest` 成功：pending event 可执行 workflow 并标记成功；workflow 抛异常会标记 `WORKFLOW_DISPATCH_FAILED` 并安排重试；已终态 `GENERATED` 的作品不会重复执行 workflow。
- Flyway 启动验证成功：本地 PostgreSQL 从版本 `202606050520` 迁移到 `202606052300`，`workflow_outbox` 已落库。
- 默认 sync HTTP smoke 成功：确认出歌仍直接返回 `GENERATED / PACKAGE_READY`，发布包 URL 存在。
- outbox HTTP smoke 成功：确认出歌先返回 `GENERATING / QUOTA_LOCKING`，随后 dispatcher 自动推进到 `GENERATED / PACKAGE_READY`。
- PostgreSQL 抽查成功：outbox smoke 作品对应 `workflow_outbox` 为 `SUCCEEDED / SONG_PRODUCTION_REQUESTED / attempt_count=0 / processed_at=true`；`generation_jobs` 为 `SUCCEEDED / PACKAGE_READY / completed_at=true`。
- smoke 后已停止 `music-api`，`8080` 未残留监听进程。

## 第 3 批 DreamMaker JWT 鉴权补齐验证结果

- `./gradlew :modules:dreammaker:test --tests com.yanyun.music.dreammaker.DreamMakerHttpClientTest` 成功：本地 fake DreamMaker server 收到有效 `Authorization: Bearer <jwt>`；测试验证了 JWT 三段结构、`alg=HS256`、`typ=JWT`、`iss=<access_key>`、`exp=now+1800s`、`nbf=now-5s` 和 HMAC 签名。
- `DreamMakerHttpClientTest` 同时验证可选 `DREAMMAKER_USER_ACCESS_TOKEN` 会透传为 `X-Access-Token`。
- `DreamMakerHttpClientTest` 同时验证缺失 `DREAMMAKER_ACCESS_KEY` / `DREAMMAKER_SECRET_KEY` 会在 HTTP 请求前失败，并映射为 `MUSIC_GENERATION_FAILED`。
- `./gradlew spotlessApply` 成功。
- `./gradlew spotlessCheck test :apps:music-api:bootJar` 成功。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/openapi-v0.1.yaml')"` 成功，OpenAPI YAML 可解析。
- `env_auditor.py` 密钥扫描成功：0 findings。
- 已额外扫描用户提供过的真实 DreamMaker AccessKey/SecretKey 原文，仓库内无匹配。

## 第 4 批 Temporal 真实编排基础验证结果

- `./gradlew :modules:production:compileJava :apps:music-api:compileJava :apps:music-worker:compileJava` 成功。
- `./gradlew :modules:workflow:test :modules:production:compileJava` 成功。
- `./gradlew :apps:music-api:test --tests com.yanyun.music.api.workflow.WorkflowOutboxDispatcherTest` 成功。
- `./gradlew :apps:music-worker:test` 成功。
- `./gradlew spotlessApply` 成功。
- `./gradlew spotlessCheck test :apps:music-api:bootJar :apps:music-worker:bootJar` 成功。
- 本地 Docker 基础设施仍健康运行：PostgreSQL、Redis、Temporal、Temporal UI、MinIO、OpenSearch、Prometheus、Grafana 均为 up/healthy。
- outbox local JAR smoke 成功：`WORKFLOW_OUTBOX_DISPATCH_TARGET=local` 下，填词创建作品、确认出歌、dispatcher 本地委托执行后，作品推进到 `GENERATED / PACKAGE_READY`，发布包 URL 存在。
- Temporal JAR smoke 成功：先启动 `music-worker` 注册 `song-production-local` task queue，再启动 API 的 `WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal`，填词创建作品并确认出歌后，worker activity 将作品推进到 `GENERATED / PACKAGE_READY`。
- PostgreSQL 抽查成功：Temporal smoke 作品对应 outbox 为 `SUCCEEDED / attempt_count=0`，generation job 为 `SUCCEEDED / PACKAGE_READY`，work 为 `GENERATED / PACKAGE_READY / PACKAGE_READY`。
- 修复并验证了一个 Temporal starter 装配问题：`TemporalSongProductionWorkflowStarter` 不再作为 `@Service` 被无参实例化，而是在 Temporal dispatch 配置类中显式创建。
- smoke 后已停止 `music-api` 和 `music-worker`，未留下占用 `8080` / `8081` 的应用进程。

## 第 5 批 DreamMaker 受控真实联调准备验证结果

- `./gradlew :modules:dreammaker:test :apps:music-api:compileJava :apps:music-worker:compileJava` 成功。
- `./gradlew :modules:dreammaker:test :modules:suno:test :modules:minimax:test :apps:music-api:test --tests com.yanyun.music.production.MockSongProductionWorkflowTest :apps:music-worker:test` 成功。
- `./gradlew spotlessApply` 成功。
- `./gradlew spotlessCheck test :apps:music-api:bootJar :apps:music-worker:bootJar` 成功。
- `DreamMakerHttpClientTest` 成功：未打开 `DREAMMAKER_REAL_CALLS_ENABLED` 时会在外部 HTTP 请求前失败；fake server 模式可验证 JWT；非 2xx 响应只保留脱敏短消息。
- `DreamMakerTaskStatusTest` 成功：未知供应商状态映射为 `UNKNOWN`，Provider 会继续轮询直到终态或超时。
- `SunoMusicProviderTest` / `MiniMaxMusicProviderTest` 成功：返回结果携带真实模型标识，用于写入 `provider_calls.model_name`。
- `MockSongProductionWorkflowTest` 成功：`provider_calls.model_name` 写入正确，供应商失败信息进入 provider call、work failure 和 job failure 前会脱敏。
- JAR HTTP guard smoke 成功：API 在 `DREAMMAKER_ACCESS_KEY=fake`、`DREAMMAKER_SECRET_KEY=fake`、`DREAMMAKER_REAL_CALLS_ENABLED=false` 下启动；请求 `music_provider=suno` 确认出歌返回本地保护失败，作品为 `FAILED / MUSIC_GENERATION_FAILED`，`provider_calls` 记录 `SUNO|suno:music-gen:chirp-crow|FAILED`，没有真实外部调用。
- smoke 后已停止 `music-api`，`8080` 未残留监听进程。

## 第 6 批 DeepSeek / 知识库写词润色 Mock 链路验证结果

- `./gradlew :modules:lyrics:test :apps:music-api:compileJava :apps:music-api:compileTestJava` 成功。
- `./gradlew spotlessApply spotlessCheck test :apps:music-api:bootJar :apps:music-worker:bootJar` 成功。
- `DefaultLyricsGenerationServiceTest` 成功：写词结果携带知识库版本、燕云引用、Prompt 模板版本和质量分；低质量 Fake 结果会触发一次内部重写。
- `WorkServiceTransactionPolicyTest` 成功：被幂等层包裹的主要写方法均声明 `noRollbackFor = ResponseStatusException.class`，避免业务 4xx 被事务提交异常覆盖成 500。
- JAR HTTP smoke 成功：缺失 `Idempotency-Key` 的 `POST /works/inspiration` 返回 HTTP 400。
- JAR HTTP smoke 成功：`POST /works/inspiration` 通过 Mock 写词链路创建作品，状态为 `LYRICS_READY / WAITING_CONFIRM`，作品详情返回燕云引用 `Yanyun frontier imagery` 和 `Yanyun ensemble motifs`。
- JAR HTTP smoke 成功：AI 润色后剩余编辑次数从 2 变 1，AI 续写后剩余编辑次数从 1 变 0。
- JAR HTTP smoke 成功：第三次 AI 润色返回 HTTP 409，不再被事务异常覆盖为 500。
- PostgreSQL 抽查成功：最新 smoke 作品的 `lyrics_drafts` 已持久化 `knowledge_base_version=mock-yanyun-kb-v0`、`prompt_template_versions={"lyrics.continue.v1": 1}`，且 `cover_prompt_seed`、`quality_score` 均非空。
- smoke 后已停止 `music-api`，未留下占用 `8080` 的 API 进程。

## 第 7 批封面与 Remotion/FFmpeg MP4 成片基础验证结果

- 新增 `docs/specs/cover-video-rendering-v0.1.md`，明确本批只做 Mock/Fake 封面与视频生成边界、16:9 MP4 成片工具链验证、发布包引用更新，不调用真实 Image 2 或真实外部渲染服务。
- `./gradlew :modules:image2:spotlessCheck :modules:media:spotlessCheck :modules:production:spotlessCheck :apps:music-api:test --tests com.yanyun.music.production.MockSongProductionWorkflowTest` 成功。
- `./gradlew spotlessApply spotlessCheck test :apps:music-api:bootJar :apps:music-worker:bootJar` 成功。
- `cd apps/render-worker && npm run build` 成功。
- `cd apps/render-worker && npm test` 成功：`LyricVideo16x9` 输出设置为 1920x1080、30fps、240 frames，样例歌词覆盖完整 composition 时长。
- `cd apps/render-worker && npm run render:sample` 成功：Remotion/FFmpeg 生成 `apps/render-worker/out/sample.mp4`。
- `ffprobe` 检查成功：样例 MP4 包含 `h264` 视频流、`aac` 音频流，分辨率为 1920x1080，时长约 8 秒，文件约 973KB。
- JAR HTTP smoke 成功：`MUSIC_PROVIDER=mock` 下，填词创建作品、确认出歌后进入 `GENERATED / PACKAGE_READY`。
- JAR HTTP smoke 成功：作品详情 `media_assets` 返回 audio、cover、video URL，video duration 为 180000，video file size 为 12000000。
- PostgreSQL 抽查成功：最新 smoke 作品写入 `AUDIO`、`COVER`、`TIMELINE`、`VIDEO` 四类 `media_assets`；`COVER` 和 `VIDEO` 均为 1920x1080；metadata 分别包含 `mock-image2` 和 `mock-remotion-ffmpeg`。
- 发布包 smoke 成功：`GET /api/v1/works/{work_id}/publish-package` 返回 `PACKAGE_READY`，`video.url`、`cover.url`、`lyrics.timeline_url` 均由对应媒体资产 object key 推导。
- 本地文件 smoke 成功：`build/local-object-storage/yanyun-works-local/packages/{work_id}.json` 已真实写出。
- smoke 后已停止 `music-api`，未留下占用 `8080` 的 API 进程。
- 第 7 批完成时 Java workflow 尚未直接调用 render-worker 生成真实业务 MP4；该缺口已在第 11 批通过 `RENDER_WORKER_MODE=local-process` 本地进程边界补上，但默认仍保持 Mock，长视频真实成片仍需手动 smoke 和后续生产级 render service 方案确认。

## 第 8 批 MinIO/S3 发布包强化验证结果

- 新增 `docs/specs/minio-s3-publish-package-storage-v0.1.md`，明确本批只强化发布包 JSON 对象存储、URL 签发/刷新和本地 MinIO smoke，不把真实媒体文件上传、真实云账号或公司对象存储纳入完成条件。
- `./gradlew :modules:storage:test :apps:music-api:compileJava :apps:music-api:compileTestJava :apps:music-worker:compileJava` 成功。
- `./gradlew spotlessApply :modules:storage:test :modules:publish:test :apps:music-api:test --tests com.yanyun.music.api.work.WorkServiceWorkflowDispatchTest --tests com.yanyun.music.production.MockSongProductionWorkflowTest` 成功。
- `./gradlew spotlessCheck test :apps:music-api:bootJar :apps:music-worker:bootJar` 成功。
- S3/MinIO JAR smoke 成功：`OBJECT_STORAGE_PROVIDER=s3`、`S3_ENDPOINT=http://localhost:9000`、`S3_BUCKET_YANYUN_WORKS=yanyun-works-local` 下，填词创建作品并确认出歌后进入 `PACKAGE_READY`。
- MinIO 对象抽查成功：发布包写入 `yanyun-ai-music/local/2026/06/06/{work_id}/package/publish-package.json`，`mc stat` 可见对象，下载后的 package JSON `work_id` 与作品一致。
- 发布包刷新 smoke 成功：`POST /api/v1/works/{work_id}/publish-package/refresh-url` 使用数据库中的 `package_object_key` 重新签发 URL，返回 URL 包含 S3 签名参数。
- 默认 local JAR smoke 成功：`OBJECT_STORAGE_PROVIDER=local` 下，确认出歌后发布包 JSON 写入 `build/local-object-storage/yanyun-works-local/yanyun-ai-music/local/2026/06/06/{work_id}/package/publish-package.json`，作品为 `GENERATED / PACKAGE_READY`。
- smoke 后已停止 `music-api`，未留下占用 `8080` 的 API 进程。
- 当前 Java workflow 的 `AUDIO`、`COVER`、`VIDEO`、`TIMELINE` 仍是 Mock asset object key/URL 口径；本批只保证发布包 JSON 自身可写入 local 或 MinIO/S3。

## 第 10 批公司 Adapter 接入与部署交接准备验证结果

- 新增 `docs/specs/company-adapter-deployment-handoff-v0.1.md`，明确本批只做公司 Adapter 替换边界、部署变量、内部 readiness 报告和交接文档，不接真实公司系统。
- 新增 `docs/handover/company-adapter-deployment-handoff-v0.1.md`，覆盖账号、审核、权益、发布、分享五类公司系统替换清单、接口口径、部署变量、smoke 步骤和禁止事项。
- `modules:config-center` 新增 `CompanyIntegrationProperties`、`IntegrationReadinessService`、`IntegrationReadinessReport` 和组件状态模型。
- `music-api` 新增内部接口 `GET /internal/integration-readiness`，默认本地配置下返回 `READY_FOR_LOCAL`，但公司账号、审核、权益、发布、分享组件均会标记 Mock/待替换状态。
- `./gradlew :modules:config-center:test :apps:music-api:test --tests com.yanyun.music.api.IntegrationReadinessControllerTest :apps:music-api:compileJava` 成功。
- `./gradlew spotlessApply spotlessCheck test :apps:music-api:bootJar :apps:music-worker:bootJar` 成功。
- JAR HTTP smoke 成功：默认本地配置下访问 `/internal/integration-readiness` 返回 `environment=local`、`overall_status=READY_FOR_LOCAL`、`component_count=10`、`company_account.status=MOCK_ONLY`、`company_quota.blocks_company_deployment=true`；新增 `render_worker` 组件默认显示 `MOCK_ONLY`。
- readiness 敏感信息 smoke 成功：返回内容未命中 `sk-`、`Bearer` 或测试 secret 形态；接口只列环境变量名，不输出密钥值。
- smoke 后已停止 `music-api`，未留下占用 `8080` 的 API 进程。
- 当前仍没有真实公司 Adapter 实现；本批完成的是交接准备、静态 readiness 和替换清单。

## 第 11 批 render-worker 本地进程调用边界验证结果

- 新增 `docs/specs/render-worker-local-process-integration-v0.1.md`，明确本批只做 Java 到 render-worker CLI 的本地进程边界，默认仍为 mock，不接真实公司系统。
- `apps/render-worker` 新增 `render:job` CLI，输入 render job JSON，输出 MP4 文件、timeline JSON 和结果 JSON；`LyricVideo16x9` 已支持按 `duration_ms` 动态计算 `durationInFrames`。
- 修复短字幕片段真实渲染时 `interpolate` 输入区间倒序问题；1 秒、2 行歌词 smoke 可正常渲染。
- `modules:production` 新增 `LocalProcessVideoRenderService`，通过外部进程调用 render-worker，限制输出文件必须位于临时目录内，再把 MP4 和 timeline JSON 写入 `ObjectStorageClient`。
- API / worker 配置新增 `yanyun.render-worker.*`，`.env.example` 新增 `RENDER_WORKER_MODE`、命令、参数、超时和对象 key 前缀变量。
- `/internal/integration-readiness` 新增 `render_worker` 组件；默认 mock 标记 `MOCK_ONLY`，`local-process` 标记 `READY_FOR_LOCAL` 但仍阻塞公司部署确认。
- `cd apps/render-worker && npm run build` 成功。
- `cd apps/render-worker && npm test` 成功，4 个 Node smoke 测试覆盖 16:9 输出、样例 timeline、180 秒动态时长和空歌词兜底。
- `cd apps/render-worker && npm run render:job` 的 1 秒真实渲染 smoke 成功，输出 `duration_ms=1000`、`duration_in_frames=30`、`width=1920`、`height=1080`、`fps=30`。
- `./gradlew :modules:production:test :modules:config-center:test :apps:music-api:test --tests com.yanyun.music.api.IntegrationReadinessControllerTest :apps:music-api:compileJava :apps:music-worker:compileJava` 成功。
- 当前本批仍不做 2-4 分钟长视频自动化渲染，不做音频 mux 完整校验，不改变默认 Mock 主链路；长视频和生产级 render service 需要后续手动 smoke 与部署方案确认。

## 第 9 批前端初审与联调支撑验证结果

- 新建长期 Goal：前端已交付后，优先推进前端原型验收修复任务包、后端短链路联调支撑、前后端本地 smoke、项目进度和阶段快照。
- `prototypes/Claude-web-v1` 初审已执行：`npm test`、`npm run typecheck`、`npm run build` 均通过。
- `prototypes/Claude-web-v1` Playwright smoke 已执行：390px 宽度可在 `?mock=1` 演示模式下完成灵感成歌到成品页，1440px 首页无横向溢出；控制台未发现业务错误。
- 前端仍未严格通过验收：缺少“我的作品”列表和 `GET /works`、成品页发布交接信息不足、`PACKAGE_BLOCKED` 映射错误、润色请求体不符合 OpenAPI、`RETRY_COVER` / `RERENDER_VIDEO` 未实现、错误态未展示 `request_id`、详情页缺少关键状态信息。
- 已新增 `docs/frontend/claude-web-v1-acceptance-fix-task-package.md`，要求前端实现者只修改 `prototypes/Claude-web-v1` 并补齐上述问题。
- `MockMusicProvider` 已支持可配置模拟音频时长；新增单元测试覆盖自定义时长和非法时长拒绝。
- `./gradlew spotlessCheck :modules:music-provider:test :apps:music-api:compileJava :apps:music-worker:compileJava` 在显式设置 `JAVA_HOME=/opt/homebrew/opt/openjdk@21` 后成功。
- 首次 Gradle 命令未设置 `JAVA_HOME` 时失败，原因是当前 shell 找不到 Java Runtime；已确认 `/opt/homebrew/opt/openjdk@21` 可用，运行手册已保留该环境设置说明。
- 同步 Mock 后端主链路 smoke 成功：基础设施已运行且健康，API 使用 `MOCK_MUSIC_DURATION_MS=1000 MUSIC_PROVIDER=mock MUSIC_WORKFLOW_DISPATCH_MODE=sync WORKFLOW_OUTBOX_DISPATCHER_ENABLED=false` 启动。
- HTTP smoke 成功：`GET /health` 和 `GET /actuator/health` 正常；`POST /api/v1/works/lyrics` 创建作品 `0932c4d4-f1e1-40a7-a3d9-00e45ca623b1`，进入 `LYRICS_READY / WAITING_CONFIRM`。
- HTTP smoke 成功：`POST /api/v1/works/{work_id}/confirm` 后作品进入 `GENERATED / PACKAGE_READY`，详情中的 `media_assets.video_duration_ms=1000`，证明 `MOCK_MUSIC_DURATION_MS` 生效。
- HTTP smoke 成功：`GET /publish-package` 返回 `PACKAGE_READY`、发布包 URL、视频 URL、封面 URL、timeline URL 和歌词；`refresh-url` 可刷新过期时间；`mark-fetched` 后详情为 `PACKAGE_FETCHED`。
- 数据库抽查成功：`works` / `publish_packages` 为 `GENERATED / PACKAGE_READY / PACKAGE_FETCHED`，发布包 object key 已持久化，`provider_calls` 记录 `MOCK / MUSIC_GENERATION / SUCCEEDED`。
- 媒体资产抽查成功：`AUDIO`、`VIDEO`、`TIMELINE` 的 `duration_ms` 均为 1000；`COVER` 正常写入。
- 本地发布包 JSON 抽查成功：`bootRun` 下实际文件位于 `apps/music-api/build/local-object-storage/.../publish-package.json`；运行手册已补充相对路径说明。
- render-worker local-process smoke 初次使用相对 `RENDER_WORKER_WORKING_DIRECTORY=apps/render-worker` 时失败，原因是 `bootRun` 工作目录为 `apps/music-api`；已修复 `LocalProcessVideoRenderService` 的相对工作目录解析，并补回归测试。
- `cd apps/render-worker && npm run build && npm test` 成功，4 个 render-worker Node smoke 测试通过。
- `./gradlew :modules:production:test spotlessCheck :apps:music-api:compileJava :apps:music-worker:compileJava` 成功，覆盖相对工作目录向上查找回归测试。
- render-worker local-process API smoke 成功：使用默认相对 `RENDER_WORKER_WORKING_DIRECTORY=apps/render-worker`、`MOCK_MUSIC_DURATION_MS=1000` 确认出歌后，作品 `4ef3dad4-6a3e-4614-b01b-b6106a5827f1` 进入 `GENERATED / PACKAGE_READY`。
- MP4 文件抽查成功：`ffprobe` 显示生成视频为 H.264、1920x1080、30fps、1.000000 秒，文件大小 137159 bytes；数据库 `VIDEO` 和 `TIMELINE` 资产 metadata 标记 `source_mode=local-process`。
- 新增 `scripts/smoke/api-main-flow.sh`，默认 smoke 在同步 Mock API 下成功，作品 `ec36246c-303a-4116-a7e6-10b1b33e14da` 跑通创建、确认、发布包、刷新、标记交接、数据库和本地对象文件抽查。
- 同一脚本的 local-process 分支成功，作品 `8576f4f3-c619-4ed8-b38f-82b7600fb7a3` 跑通并通过本地 MP4 `ffprobe` 校验。

## 待确认事项

- `prototypes/Claude-web-v1` 需由前端实现者按 `docs/frontend/claude-web-v1-acceptance-fix-task-package.md` 修复后，再进入真实后端模式 UI 联调。
- 公司账号、审核、权益、发布、分享系统真实协议仍待公司开发确认；当前已提供 readiness 报告和交接说明，但真实 Adapter 仍需公司开发替换 Mock。
- Suno 和 MiniMax 的 DreamMaker run/status 接入骨架、JWT 鉴权、真实调用硬开关和受控联调文档已实现；非零错误码样本、失败任务响应样本、限流/轮询策略、音频 URL 过期规则和计费口径仍待真实联调确认。
- DeepSeek 真实 API 协议、模型参数、失败码、限流策略、计费口径、日志脱敏和受控联调窗口仍待确认。
- Image 2 API 细节、公司对象存储规范、日志与数据留存规范仍待确认；本地 Mock 封面边界已完成，真实 Image 2 接入尚未开始。
- Outbox v0.1 与 Temporal v0.1 已落地并可本地验证；后续进入真实模型链路前，还需要把当前单一 activity 委托拆成更细粒度、可幂等重试的音乐生成、封面、视频、发布包等活动。
- 当前发布包 JSON 已可写入本地文件存储和 MinIO/S3 兼容对象存储；`RENDER_WORKER_MODE=local-process` 可把真实 MP4 和 timeline 写入对象存储，但默认仍是 Mock asset 描述和 URL 口径，长视频和生产级 render worker 方案尚未最终确认。
- 当前音乐重试已有次数上限和状态抢占，DreamMaker 失败码也已有保守映射；真实联调后仍需根据具体非零 code 和 failed payload 精细化 retryable / non-retryable 规则。
- 运营侧模型降级策略仍待定义：例如 Suno 连续失败是否自动推荐 MiniMax，哪些模型对用户可见，哪些仅作为后台兜底。

## 下一步建议

1. 将 `docs/frontend/claude-web-v1-acceptance-fix-task-package.md` 交给前端实现者修复，修复后重跑 `npm test`、`npm run typecheck`、`npm run build` 和 390px / 1440px Playwright smoke。
2. 执行后端同步 Mock 主链路 smoke：`MOCK_MUSIC_DURATION_MS=1000 MUSIC_WORKFLOW_DISPATCH_MODE=sync WORKFLOW_OUTBOX_DISPATCHER_ENABLED=false MUSIC_PROVIDER=mock`，覆盖创建作品、确认出歌、发布包获取、刷新、标记交接和 readiness。
3. 针对 `RENDER_WORKER_MODE=local-process` 做一次端到端手动 smoke：确认出歌后由 Java 调用 render-worker，检查 MP4、timeline、发布包 URL 和对象存储文件；再评估是否需要独立 HTTP/队列化 render service。
4. 前端修复后执行真实后端模式 UI 联调，不加 `?mock=1`，验证灵感成歌、填词成歌、润色/续写限制、失败重试、成品页和发布交接。
5. 公司开发确认真实账号、审核、权益、发布、分享协议后，按 `docs/handover/company-adapter-deployment-handoff-v0.1.md` 替换 Mock Adapter，并用 `/internal/integration-readiness` 做部署前检查。
6. 在明确联调窗口和止损规则后，按 `docs/runbook/dreammaker-controlled-real-integration.md` 手动执行 Suno / MiniMax 各 1 次真实成功路径；不得把密钥、JWT 或用户 token 写入仓库、日志或测试。
7. 根据真实联调样本更新 `docs/integrations/dreammaker-open-questions-tracker.md` 和失败码 retryable 规则。

## 工作日志

| 时间 | 事项 | 结果 |
|---|---|---|
| 2026-06-05 00:43 CST | Git 初始化 | 已创建本地 Git 仓库，分支为 `main` |
| 2026-06-05 00:43 CST | 创建项目进度文档 | 新增 `docs/project-progress.md` |
| 2026-06-05 00:43 CST | 固化进度记录规则 | 新增项目级 `AGENTS.md`，并在本文件加入阶段性任务完成后必须更新进度的规则 |
| 2026-06-05 00:48 CST | 整理第 1 批仓库初始化任务说明 | 新增 `docs/codex-batch-01-repository-initialization.md`，明确第 1 批范围、目录、禁区、验收标准和执行顺序 |
| 2026-06-05 00:48 CST | 整理技术方案需补项 | 新增 `docs/tech-design-required-supplements.md`，列出前端工程、OpenAPI、权益扣减、审核落点、并发控制等需补齐事项 |
| 2026-06-05 00:53 CST | 固化 commit 提醒规则 | 更新 `AGENTS.md`，要求阶段性任务完成且适合形成快照时提醒用户是否 commit |
| 2026-06-05 00:53 CST | 执行第一次 commit | 将项目基线文档、进度记录规则、第 1 批任务说明和技术方案补项纳入 Git 初始提交 |
| 2026-06-05 01:19 CST | 输出 PRD v0.3 | 新增 `yanyun-ai-music-platform-prd-v0.3.md`，明确商用级目标、多端用户侧、外部系统边界、用户体验和量化验收 |
| 2026-06-05 01:19 CST | 输出技术方案 v0.2 | 新增 `yanyun-ai-music-platform-tech-design-v0.2.md`，明确完整商用级技术栈、`apps/web`、权益扣减、发布包审核、并发控制和 OpenAPI 覆盖范围 |
| 2026-06-05 01:19 CST | 固化架构决策 | 新增 `docs/adr/0001-user-web-scope.md` 和 `docs/adr/0002-commercial-grade-stack.md` |
| 2026-06-05 01:19 CST | 更新执行辅助文档 | 更新 `docs/tech-design-required-supplements.md`、`docs/codex-batch-01-repository-initialization.md` 和 `AGENTS.md`，同步新基线和第 1 批要求 |
| 2026-06-05 01:26 CST | 升级项目 Agent 规则 | 重写 `AGENTS.md`，固化项目身份、Source of Truth、固定技术决策、外部系统边界、前后端执行规则、验证规则、进度记录、Git 和安全要求 |
| 2026-06-05 01:28 CST | 补充前端协作和图片资产规则 | 更新 `AGENTS.md`，明确图片资产优先用 Image 生成，前端视觉实现优先产出 Gemini 任务包，本 Agent 负责需求、接口、状态、验收和 review |
| 2026-06-05 01:30 CST | 提交文档基线 | 用户要求执行 commit；提交前已检查 Git 状态、敏感信息和 Markdown diff 格式，本轮文档基线已纳入 Git 快照 |
| 2026-06-05 02:39 CST | 输出 OpenAPI v0.1 | 新增 `docs/api/openapi-v0.1.yaml`，覆盖作品状态、生成阶段、权益提示、失败动作和发布包交接 |
| 2026-06-05 02:39 CST | 初始化商用级工程骨架 | 新增 Gradle 多模块后端、`apps/web`、`apps/render-worker`、数据库/知识库/运行手册目录 |
| 2026-06-05 02:39 CST | 搭建本地基础设施 | 新增 Docker Compose，PostgreSQL、Redis、Temporal、MinIO、OpenSearch、Prometheus、Grafana 已通过本地启动验证 |
| 2026-06-05 02:39 CST | 完成基础构建和 smoke 验证 | Gradle、Web、Render Worker、Docker Compose、API health、Worker Temporal 连接验证均通过 |
| 2026-06-05 02:43 CST | 提交第 1 批工程快照 | 已提交 `992762a chore: initialize commercial-grade project scaffold` |
| 2026-06-05 03:20 CST | 落地第 2 批数据库 migration | 新增作品域、歌词草案、任务、媒体、发布包、权益、Provider 调用、知识库、Prompt 和配置等核心表 |
| 2026-06-05 03:20 CST | 实现 Work 状态机与 Mock Adapter 边界 | 账号、权益、审核、发布包交接均可通过 Mock 实现跑通，真实公司系统后续替换 Adapter |
| 2026-06-05 03:20 CST | 实现 OpenAPI 主路径 Mock API | 灵感成歌、填词成歌、作品查询、改词、确认出歌、重生/重渲、发布包获取/刷新/交接接口已可运行 |
| 2026-06-05 03:20 CST | 完成本地后端 Mock 业务 smoke | 作品从 `LYRICS_READY` 推进到 `GENERATED/PACKAGE_READY`，发布包标记后进入 `PACKAGE_FETCHED` |
| 2026-06-05 03:20 CST | 输出 Gemini 前端任务包 | 新增 `docs/frontend/gemini-batch-02-mock-workflow-task-package.md`，供后续前端实现使用 |
| 2026-06-05 03:20 CST | 记录 Suno + MiniMax 双模型要求 | 音乐生成后续需同时接入 Suno 与 MiniMax，并通过配置/运营策略选择对用户开放的模型；飞书资料待可读后补细节 |
| 2026-06-05 03:34 CST | 补齐基础幂等语义 | `Idempotency-Key` 成功响应可重放，同 key 不同请求返回 `IDEMPOTENCY_CONFLICT`，已通过单元测试和 HTTP smoke |
| 2026-06-05 03:43 CST | 预置音乐 Provider 工程边界 | 新增统一 `MusicProvider` 合约、`MockMusicProvider`、`SunoMusicProvider`、`MiniMaxMusicProvider` 和配置变量，真实 API 调用仍保持关闭 |
| 2026-06-05 03:50 CST | 接入 MockMusicProvider 到出歌流程 | `confirmWork` 已通过 Provider 结果写入音频媒体资产，主链路 smoke 通过 |
| 2026-06-05 04:19 CST | 拆出 MockSongProductionWorkflow | 出歌编排从 `WorkService` 下沉到 Workflow，补齐 job 成功/失败收口和失败释放权益，单元测试、构建、HTTP smoke、DB 抽查均通过 |
| 2026-06-05 04:29 CST | 接入 Mock 对象存储写入 | 新增 `ObjectStorageClient` 与本地文件实现，发布包 JSON 已可写入 `build/local-object-storage`，单元测试、构建、HTTP smoke、本地文件检查和 DB 抽查均通过 |
| 2026-06-05 04:50 CST | 补 Provider 配置选择 | `MUSIC_PROVIDER=mock|suno|minimax` 已接入 Workflow；默认 mock 成功，suno 未实现边界会持久化失败并释放权益，测试、构建、HTTP smoke 和 DB 抽查均通过 |
| 2026-06-05 05:12 CST | 补失败恢复与重试闭环 | 新增音乐重试接口与请求级 Provider 覆盖；`suno` 失败后可用 `mock` 重试恢复到 `PACKAGE_READY`，测试、构建、HTTP smoke 和 DB 抽查均通过 |
| 2026-06-05 10:07 CST | 增强重试稳定性 | 新增 `music_retry_count`、2 次音乐重试上限、状态抢占和失败推荐动作；次数耗尽、恢复成功、Flyway、HTTP smoke 和 DB 抽查均通过 |
| 2026-06-05 10:22 CST | 启用 Provider 调用记录 | 出歌流程已写入 `provider_calls`，Suno 失败与 Mock 成功均可追踪；新增 Suno/MiniMax 真实接入前置说明 |
| 2026-06-05 22:18 CST | 读取飞书模型接入资料 | 已通过 `lark-cli` 授权读取文档，整理 DreamMaker Suno / MiniMax run/status 接口、字段限制和剩余待确认项 |
| 2026-06-05 22:52 CST | 实现 DreamMaker Provider 接入骨架 | 新增 `modules:dreammaker`、Suno/MiniMax submit+poll、DreamMaker HTTP client、远程音频导入对象存储和规格/运行文档；测试、bootJar、OpenAPI 解析和本地 API smoke 均通过 |
| 2026-06-05 23:55 CST | 实现 Outbox 可靠编排基础 | 新增 `workflow_outbox`、可切换 sync/outbox 模式、本地 dispatcher、确认出歌/音乐重试异步启动边界和规格/运行文档；测试、bootJar、Flyway、sync smoke、outbox smoke 和 DB 抽查均通过 |
| 2026-06-06 00:12 CST | 补齐 DreamMaker JWT 鉴权 | 根据用户补充资料确认 AK/SK 生成 HS256 JWT；`DreamMakerHttpClient` 已改为 `Authorization: Bearer <jwt>`，可选透传 `X-Access-Token`；测试、bootJar、OpenAPI 解析和密钥扫描均通过 |
| 2026-06-06 01:35 CST | 完成 Temporal 真实编排基础 | 新增 `modules:production`、`modules:workflow`、API local/temporal starter、worker workflow/activity 注册和生命周期；local outbox smoke、Temporal worker smoke、全量 Gradle 验证和 DB 抽查均通过 |
| 2026-06-06 02:20 CST | 完成 DreamMaker 受控真实联调准备 | 新增真实调用硬开关、共享 DreamMaker client、worker Suno/MiniMax 注册、provider 模型标识、失败信息脱敏和第 5 批联调/安全/验收/交接文档；全量测试、bootJar 和硬开关 HTTP smoke 均通过 |
| 2026-06-06 03:01 CST | 完成 DeepSeek / 知识库写词润色 Mock 链路 | 新增 Knowledge/Prompt/DeepSeek/Lyrics 模块、统一写词服务、低质量内部重写、AI 编辑次数共享、持久化写词元数据和事务 4xx 修复；全量测试、bootJar、HTTP smoke 和 DB 抽查均通过 |
| 2026-06-06 03:44 CST | 完成封面与 MP4 成片基础 | 新增 Image2/Media 边界、Cover/Video Mock 服务、发布包媒体引用更新和 `LyricVideo16x9` Remotion 样例；全量 Gradle、render-worker build/test/render、ffprobe、HTTP smoke 和 DB 抽查均通过 |
| 2026-06-06 04:36 CST | 完成 MinIO/S3 发布包强化 | 新增 S3/MinIO 对象存储客户端、结构化发布包 object key、presigned URL 签发/刷新和对象存储运行手册；全量 Gradle、MinIO smoke、local smoke 和 8080 清理检查均通过 |
| 2026-06-06 04:40 CST | Gemini 前端原型废弃 | 子 Agent 调用 Gemini 生成的 `prototypes/gemini-web-v1` 因视觉质量不达标已删除；后续第 9 批需重新整理更强约束的高保真前端任务包 |
| 2026-06-06 12:49 CST | 完成公司 Adapter 接入与部署交接准备 | 新增公司 Adapter readiness 服务、内部 `/internal/integration-readiness`、第 10 批规格和公司交接说明；全量 Gradle、JAR HTTP smoke、敏感信息 smoke 和 8080 清理检查均通过 |
| 2026-06-06 13:32 CST | 完成 render-worker 本地进程调用边界 | 新增 `render:job` CLI、动态时长 Remotion composition、Java `LocalProcessVideoRenderService`、render-worker readiness 组件和运行手册；Node build/test、1 秒真实 render smoke、targeted Gradle 测试均通过 |
| 2026-06-06 14:15 CST | 新建前端交付后的长期 Goal | Goal 已改为前端原型验收修复、后端短链路联调支撑、前后端本地 smoke、进度文档和阶段快照 |
| 2026-06-06 14:15 CST | 完成 Claude Web v1 前端初审 | `prototypes/Claude-web-v1` 已通过测试、typecheck、build 和 390px / 1440px smoke；发现 OpenAPI/验收缺口并整理为修复任务包 |
| 2026-06-06 14:15 CST | 补齐 Mock 音频时长配置 | 新增 `MOCK_MUSIC_DURATION_MS` 和 `MockMusicProvider` 配置测试，后端 targeted Gradle 验证通过 |
| 2026-06-06 14:31 CST | 完成同步 Mock 后端主链路 smoke | API 以 1 秒 Mock 音频时长跑通创建作品、确认出歌、发布包获取、刷新、标记交接、readiness、数据库和本地发布包 JSON 抽查 |
| 2026-06-06 14:45 CST | 完成 render-worker local-process API smoke | 修复相对工作目录解析后，默认 `apps/render-worker` 可在 `bootRun` 下生成真实 1 秒 MP4；ffprobe 验证 H.264、1920x1080、30fps |
| 2026-06-06 14:55 CST | 脚本化本地主链路 smoke | 新增 `scripts/smoke/api-main-flow.sh`，同步 Mock 与 render-worker local-process 两种 API 启动模式均验证通过 |
