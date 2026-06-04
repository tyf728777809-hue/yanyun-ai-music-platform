# 项目进度记录

更新时间：2026-06-05 04:50 CST

## 当前阶段

项目第 2 批已启动并完成首个可运行后端 Mock 业务闭环：数据库 migration、Work 领域状态机、Mock Adapter 边界、OpenAPI v0.1 主路径 API 和本地 smoke 已跑通。当前仍不接真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统。

第 2 批后续小阶段已补齐 `Idempotency-Key` 的基础重放语义：同用户、同 operation、同 key、同请求内容会重放第一次成功响应；同 key 不同请求内容返回 `IDEMPOTENCY_CONFLICT`。

音乐生成 Provider 工程边界已根据用户要求预置：统一 `MusicProvider` 合约、`MockMusicProvider`、`SunoMusicProvider` 和 `MiniMaxMusicProvider` 均已建好；当前 Suno/MiniMax 只暴露边界和测试，不调用真实 API。

当前 Mock 出歌流程已接入 `MockMusicProvider`，不再在 `WorkService` 内直接硬编码音频生成结果；后续切换 Suno 或 MiniMax 时已有主链路落点。

`confirmWork` 已进一步拆到 `MockSongProductionWorkflow`：`WorkService` 只负责状态校验和接口返回，出歌编排集中处理权益锁定、音乐 Provider、媒体资产、发布包、发布包审核、权益扣减、失败释放和 generation job 收口。当前仍是同步 Mock Workflow，为后续 Temporal Workflow 接入做准备。

Mock 对象存储边界已落地：发布包 JSON 会通过 `ObjectStorageClient` 写入本地 `build/local-object-storage/yanyun-works-local/packages/{work_id}.json`，接口返回的 `package_url` 与本地写出的对象 key 保持一致。当前仍不写入真实 MinIO/S3，公司对象存储协议待后续确认。

音乐 Provider 选择已从硬编码 `MOCK` 改为配置驱动：`MUSIC_PROVIDER=mock|suno|minimax` 会映射到统一 `MusicProviderSelection`。当前默认 `mock` 可完整成功；`suno` / `minimax` 已注册边界但真实提交仍显式未实现，本地选择它们会进入 `MUSIC_GENERATION_FAILED` 可重试失败，并释放已锁权益，不会调用真实 API。

- `yanyun-ai-music-platform-prd-v0.3.md`：商用级产品范围基线。
- `yanyun-ai-music-platform-tech-design-v0.2.md`：商用级技术方案基线。
- `docs/adr/0001-user-web-scope.md`：用户侧 Web 范围决策。
- `docs/adr/0002-commercial-grade-stack.md`：商用级技术栈决策。
- `docs/api/openapi-v0.1.yaml`：作品、生成阶段、失败动作、权益提示和发布包交接接口契约。
- `database/migrations/V202606050245__init_work_domain.sql`：作品域核心业务表。
- `docs/frontend/gemini-batch-02-mock-workflow-task-package.md`：交给 Gemini 的第 2 批前端任务包。

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
- 根据用户补充，记录音乐生成后续需同时接入 Suno 与 MiniMax，并预留可配置开放策略；飞书资料链接当前需要登录权限，待可读后补真实协议细节。
- 实现 `Idempotency-Key` 基础语义，覆盖当前所有 POST 主路径，成功响应写入 `idempotency_keys`，重复提交可重放响应，参数冲突返回 `IDEMPOTENCY_CONFLICT`。
- 新增 `modules:music-provider`，定义音乐生成统一 Provider 合约、请求、结果、状态、Provider 类型和注册选择器。
- 新增 `modules:suno`，预置 `SunoMusicProvider` 边界；当前真实调用显式未实现，自动化测试验证不会调用真实 Suno API。
- 扩展 `modules:minimax`，预置 `MiniMaxMusicProvider` 边界；当前真实调用显式未实现，自动化测试验证不会调用真实 MiniMax API。
- `.env.example` 增加 `MUSIC_PROVIDER=mock`、`SUNO_API_BASE_URL`、`MINIMAX_API_BASE_URL`、`SUNO_API_KEY` 等变量名，不包含真实凭据。
- `music-api` 已依赖 `modules:music-provider`，通过 Spring 配置注册 `MockMusicProvider` 和 `MusicProviderRegistry`。
- `WorkService.confirmWork` 已通过 `MusicProviderRegistry.require(MOCK)` 生成 Mock 音频结果，再继续封面、视频、发布包链路。
- 新增 `MockSongProductionWorkflow`，将确认出歌后的生成编排从 `WorkService` 中拆出，集中处理权益锁定、Provider 调用、媒体资产、发布包、发布包审核、权益提交和失败释放。
- `generation_jobs` 已增加完成状态更新方法，`SONG_PRODUCTION` job 成功后会收口到 `SUCCEEDED / PACKAGE_READY`，失败后会收口到 `FAILED / FAILED` 并记录失败码。
- 新增 `MockSongProductionWorkflowTest`，覆盖成功出发布包和音乐 Provider 失败时释放已锁权益两个关键路径。
- 新增 `modules:storage` 对象存储合约和 `LocalObjectStorageClient`，本地阶段可把发布包 JSON 写到 `build/local-object-storage/yanyun-works-local`。
- `MockSongProductionWorkflow` 已接入对象存储写入；发布包准备或写入失败会进入 `PACKAGE_BUILD_FAILED`，释放已锁权益，并标记为可重试失败。
- 本地运行手册已补充 package JSON 写入位置和检查方式。
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

## 待确认事项

- 公司账号、审核、权益、发布、分享系统真实协议仍待公司开发确认。
- Suno 和 MiniMax 音乐模型真实接入资料仍待补齐：工程 Provider 边界已预置，但飞书链接需要登录权限，尚无法读取具体鉴权、请求、回调、限流、计费和失败码。
- Image 2 API 细节、公司对象存储规范、日志与数据留存规范仍待确认。
- 当前 `Workflow` 仍是同步 Mock 实现；进入真实模型链路前必须接入 Temporal Workflow 与 Outbox 或等价补偿机制。
- 当前发布包已写入本地 mock 对象存储目录，但尚未写入真实 MinIO/S3。

## 下一步建议

1. 补更完整的失败后可执行动作和重试入口，尤其是 `MUSIC_GENERATION_FAILED` 后的用户侧重试/换模型策略。
2. 按 Gemini 前端任务包实现或外包前端页面，并用本地 API 做联调。
3. 后续增强幂等：补并发冲突处理、过期键清理、更多集成测试。
4. 读取飞书资料后补 `SunoMusicProvider` 和 `MiniMaxMusicProvider` 的真实请求/回调/失败码映射。
5. 进入真实模型链路前接入 Temporal `SongProductionWorkflow` 与 Outbox 或等价补偿机制。

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
