# Local Development Runbook

## Prerequisites

- JDK 21。
- Node.js 22 或更高版本。
- Docker Desktop。
- Gradle Wrapper 使用仓库内 `./gradlew`。

本机如果通过 Homebrew 安装 `openjdk@21`，可在命令前加：

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
```

## Start Infrastructure

```bash
docker compose -f deploy/docker-compose.yml --env-file .env.example up -d
docker compose -f deploy/docker-compose.yml ps
```

## Backend

```bash
./gradlew clean build
./gradlew test
./gradlew spotlessCheck
./gradlew :apps:music-api:bootRun
```

音乐 Provider 默认走本地 Mock：

```bash
MUSIC_PROVIDER=mock ./gradlew :apps:music-api:bootRun
```

默认 Mock 音频时长为 180000ms，对齐 3 分钟成片口径。做本地快速 smoke 时可以临时压短：

```bash
MOCK_MUSIC_DURATION_MS=1000 MUSIC_PROVIDER=mock ./gradlew :apps:music-api:bootRun
```

该变量只影响 `MockMusicProvider` 返回的模拟音频时长，方便 `RENDER_WORKER_MODE=local-process`
快速验证 Java 到 render-worker 的 MP4 成片链路；真实 Suno / MiniMax 返回时长不受它影响。

Workflow dispatch 默认走同步本地模式，方便保持当前 Mock 主链路 smoke：

```bash
MUSIC_WORKFLOW_DISPATCH_MODE=sync WORKFLOW_OUTBOX_DISPATCHER_ENABLED=false \
  ./gradlew :apps:music-api:bootRun
```

如果要验证真实模型链路前的可靠启动边界，可切到 Outbox 本地 dispatcher 模式：

```bash
MUSIC_WORKFLOW_DISPATCH_MODE=outbox \
WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true \
WORKFLOW_OUTBOX_DISPATCH_TARGET=local \
WORKFLOW_OUTBOX_POLL_INTERVAL=1s \
WORKFLOW_OUTBOX_RETRY_DELAY=1s \
./gradlew :apps:music-api:bootRun
```

Outbox 模式下，`confirm` / `music/retry` 会先返回 `GENERATING / QUOTA_LOCKING`，由本地 dispatcher
异步执行 `SongProductionWorkflow`，成功后作品会推进到 `GENERATED / PACKAGE_READY`。

Outbox 状态可用 PostgreSQL 抽查：

```bash
docker exec yanyun-postgres psql -U postgres -d yanyun_music -Atc \
  "select status, event_type, attempt_count, processed_at is not null from workflow_outbox where aggregate_id = '{work_id}' order by created_at"
```

如果要验证 API 与 worker 分进程的 Temporal 编排边界，先启动 worker：

```bash
MUSIC_PROVIDER=mock ./gradlew :apps:music-worker:bootRun
```

再启动 API，并把 outbox dispatcher 目标切到 Temporal：

```bash
MUSIC_PROVIDER=mock \
MUSIC_WORKFLOW_DISPATCH_MODE=outbox \
WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true \
WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal \
WORKFLOW_OUTBOX_POLL_INTERVAL=1s \
./gradlew :apps:music-api:bootRun
```

Temporal v0.1 的工作方式是：API 事务内抢占作品、写入 `generation_jobs` 和
`workflow_outbox`；API dispatcher 只负责启动 deterministic workflow，并在启动成功后把
outbox 标记为 `SUCCEEDED`；`music-worker` 的 activity 复用当前 `SongProductionWorkflow`
业务委托，把作品推进到 `GENERATED / PACKAGE_READY`。本阶段 activity 最大尝试次数固定为 1，
等权益、Provider、媒体和发布包写入幂等性审计完成后，再放开 Temporal activity 自动重试。

worker workflow mode 默认仍是 `legacy`，保持当前 Temporal v0.1 路径不变。只有要受控验证
`generation_job_steps` 写入时，才在启动 `music-worker` 时显式设置：

```bash
TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=stepwise-recording \
MUSIC_PROVIDER=mock ./gradlew :apps:music-worker:bootRun
```

`stepwise-recording` 当前只用于本地 Mock / recording step activity 验证，检查步骤记录边界；
不调用真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统，也不是默认生产路径。该模式当前不会推进
`works`、`generation_jobs` 或发布包到 `GENERATED / PACKAGE_READY`，因此不要用
`scripts/smoke/api-main-flow.sh` 验证它。

`stepwise-recording` 的最小验收口径是：outbox 已启动 Temporal workflow，且 step audit 已写入。
在 worker 和 API 都已按上面方式启动后，可运行：

```bash
scripts/smoke/temporal-stepwise-recording.sh
```

脚本会创建作品、确认出歌、等待 outbox 成功，并检查 `generation_job_steps` 是否写入 13 条
`SUCCEEDED` 记录。也可手工抽查：

```bash
docker exec yanyun-postgres psql -U postgres -d yanyun_music -Atc \
  "select status, event_type, processed_at is not null from workflow_outbox where aggregate_id = '{work_id}' order by created_at desc limit 1"
docker exec yanyun-postgres psql -U postgres -d yanyun_music -Atc \
  "select step_name, status, attempt_count, failure_code from generation_job_steps where job_id = '{job_id}' order by started_at, step_name"
```

预期 `workflow_outbox.status=SUCCEEDED`，`generation_job_steps` 有 13 条 `SUCCEEDED` 记录；作品和 job
仍可能保持 `GENERATING / QUOTA_LOCKING`，这是当前受控验证路径的预期边界。

Temporal 模式完成后可抽查 outbox、job 和 work 状态：

```bash
docker exec yanyun-postgres psql -U postgres -d yanyun_music -Atc \
  "select o.status, o.attempt_count, j.status, j.stage, w.status, w.generation_stage, w.package_status from workflow_outbox o join generation_jobs j on j.work_id = o.aggregate_id join works w on w.id = o.aggregate_id where o.aggregate_id = '{work_id}' order by o.created_at desc limit 1"
```

也可以显式选择 `suno` 或 `minimax` 验证真实 Provider 边界。当前 `suno` 可通过
`SUNO_BACKEND=yunwu|dreammaker` 切换公网联调或正式生产目标后端；`minimax` 当前仍通过
DreamMaker 接入。默认不开真实调用硬开关时，不会发起真实供应商请求。

真实模型联调先从统一总入口查看目标矩阵和执行路线。默认不调用供应商：

```bash
scripts/smoke/real-model-controlled-smoke.sh
TARGET=dreammaker-suno MODE=plan scripts/smoke/real-model-controlled-smoke.sh
TARGET=dreammaker-minimax MODE=plan scripts/smoke/real-model-controlled-smoke.sh
TARGET=yunwu-suno MODE=plan scripts/smoke/real-model-controlled-smoke.sh
TARGET=deepseek MODE=plan scripts/smoke/real-model-controlled-smoke.sh
TARGET=wellapi-image2 MODE=plan scripts/smoke/real-model-controlled-smoke.sh
TARGET=dreammaker-image2 MODE=plan scripts/smoke/real-model-controlled-smoke.sh
```

只读预检可以通过总入口委托执行。它只检查当前 shell 环境变量和可选 API readiness，不调用供应商：

```bash
TARGET=yunwu-suno MODE=preflight scripts/smoke/real-model-controlled-smoke.sh
TARGET=dreammaker-suno MODE=preflight scripts/smoke/real-model-controlled-smoke.sh
TARGET=dreammaker-minimax MODE=preflight scripts/smoke/real-model-controlled-smoke.sh
TARGET=deepseek MODE=preflight scripts/smoke/real-model-controlled-smoke.sh
TARGET=wellapi-image2 MODE=preflight scripts/smoke/real-model-controlled-smoke.sh
TARGET=dreammaker-image2 MODE=preflight scripts/smoke/real-model-controlled-smoke.sh
```

真实执行必须显式使用 `MODE=execute`，同时提供 `ALLOW_REAL_MODEL_SMOKE=1` 和目标脚本自己的
`ALLOW_*` 开关；不要从总入口绕过各 Provider 的 runbook 和安全门。

不触发真实调用的安全门矩阵审计：

```bash
scripts/smoke/real-model-safety-gates-audit.sh
```

DeepSeek 单样本真实写词 smoke 的统一入口是：

```bash
ALLOW_REAL_MODEL_SMOKE=1 ALLOW_DEEPSEEK_REAL_SMOKE=1 \
TARGET=deepseek MODE=execute scripts/smoke/real-model-controlled-smoke.sh
```

该命令只验证真实写词，音乐、封面、DreamMaker、Yunwu、WellAPI 和公司 Adapter 必须保持 Mock 或关闭。DreamMaker 音乐与 DreamMaker Image 2 仍是正式生产目标接口。

真实联调必须使用本地环境变量或生产密钥注入，不要把真实凭据写进仓库、文档、测试或命令日志：

```bash
export MUSIC_PROVIDER=suno
export SUNO_BACKEND=yunwu
export YUNWU_BASE_URL=https://yunwu.ai
export YUNWU_API_KEY=...
export YUNWU_REAL_CALLS_ENABLED=true
export YUNWU_SUNO_MODEL=chirp-v5
```

如需验证正式生产目标 DreamMaker Suno 路径，切换为：

```bash
export MUSIC_PROVIDER=suno
export SUNO_BACKEND=dreammaker
export DREAMMAKER_API_BASE_URL=https://api-all.dreammaker.netease.com
export DREAMMAKER_ACCESS_KEY=...
export DREAMMAKER_SECRET_KEY=...
export DREAMMAKER_REAL_CALLS_ENABLED=true
# Optional: only set when company integration wants DreamMaker to parse a real user identity.
export DREAMMAKER_USER_ACCESS_TOKEN=...
export DREAMMAKER_SUNO_MODEL=chirp-crow
./gradlew :apps:music-api:bootRun
```

上面命令只用于说明变量名。真实 Suno 联调优先阅读
`docs/runbook/yunwu-suno-controlled-real-integration.md`；DreamMaker Suno / MiniMax 联调必须阅读
`docs/runbook/dreammaker-controlled-real-integration.md`。真实联调必须走 outbox + Temporal worker，
不要使用默认 `sync` 模式；否则 API 会返回 HTTP 409，防止误在同步请求线程中触发真实供应商。

如果 API 已用 `DREAMMAKER_REAL_CALLS_ENABLED=true` 和默认 `sync` workflow 模式启动，可先运行非真实 guard smoke：

```bash
scripts/smoke/dreammaker-real-guard-smoke.sh
```

预期确认出歌返回 HTTP 409，作品仍停留在 `LYRICS_READY / WAITING_CONFIRM`，且不会写入 `provider_calls`。

MiniMax 可用：

```bash
export MUSIC_PROVIDER=minimax
export MINIMAX_MODEL=minimax-music-2.6
./gradlew :apps:music-api:bootRun
```

DreamMaker 鉴权已确认：客户端会使用 `DREAMMAKER_ACCESS_KEY` 作为 JWT `iss`，使用
`DREAMMAKER_SECRET_KEY` 做 HS256 签名，`exp=now+1800s`，`nbf=now-5s`，并放入
`Authorization: Bearer <jwt>`。`DREAMMAKER_USER_ACCESS_TOKEN` 只作为可选
`X-Access-Token` 透传，不参与本平台本地 Mock 用户身份。

当真实 Provider 返回音频 URL 后，workflow 会先把音频导入本地对象存储，再写入 `media_assets`；作品详情和发布包仍只暴露平台自己的对象 URL。

失败后可通过音乐重试接口验证恢复边界：

```bash
curl -X POST "http://localhost:8080/api/v1/works/{work_id}/music/retry" \
  -H "Content-Type: application/json" \
  -H "X-Mock-User-Id: local-user" \
  -H "Idempotency-Key: retry-{uuid}" \
  -d '{"music_provider":"mock"}'
```

预期结果是作品从 `FAILED / MUSIC_GENERATION_FAILED` 恢复到 `GENERATED / PACKAGE_READY`。权益流水应体现首次失败释放、重试成功提交：`LOCK_GENERATE -> RELEASE_GENERATE -> LOCK_GENERATE -> COMMIT_GENERATE`。

当前音乐重试上限为 2 次。作品详情的 `failure.retry_count`、`failure.retry_limit`、`failure.remaining_retry_count` 和 `failure.recommended_action` 可用于验证失败页展示；当 `remaining_retry_count = 0` 时，`available_actions` 不再包含 `RETRY_MUSIC`，继续调用重试接口会返回 HTTP 409。

Provider 调用记录可用 PostgreSQL 抽查：

```bash
docker exec yanyun-postgres psql -U postgres -d yanyun_music -Atc \
  "select provider, operation, status, provider_trace_id, error_code from provider_calls where work_id = '{work_id}' order by created_at"
```

健康检查：

```bash
curl http://localhost:8080/health
curl http://localhost:8080/actuator/health
```

公司 Adapter 与部署交接 readiness 检查：

```bash
curl http://localhost:8080/internal/integration-readiness
scripts/smoke/company-adapter-readiness-smoke.sh
```

本地环境允许公司账号、审核、权益、发布、分享边界显示为 `MOCK_ONLY`。公司部署前，应检查
`blocks_company_deployment=true` 的项目，并按 `docs/handover/company-adapter-deployment-handoff-v0.1.md`
替换真实 Adapter 或由公司明确豁免。

`company-adapter-readiness-smoke.sh` 是只读脚本，只检查 readiness 结构、公司 Mock 边界、部署变量名和明显密钥泄漏形态；不会调用真实公司系统、模型供应商、数据库或对象存储。

## Automated Smoke

### Local Commercial Backend Acceptance Stack

如果 Docker Compose 基础设施已启动，且本地 `8080` 空闲，可以用一个后端组合入口复验当前本地商用 Mock 基线：

```bash
scripts/smoke/local-commercial-backend-acceptance-stack.sh
```

该脚本会自动完成两轮 `music-api` 启动和清理：

- 普通 Mock API：执行 `api-main-flow.sh`、`openapi-contract.sh` 和 `company-adapter-readiness-smoke.sh`。
- 发布包审核阻断 API：显式设置 `MOCK_MODERATION_PUBLISH_PACKAGE_BLOCKED_USER_IDS=mock_package_block_smoke`，执行 `api-package-blocked-flow.sh`。

脚本会把 API 日志写到 `build/smoke/local-commercial-backend-acceptance-*`，结束后停止它启动的 API 进程。它只使用 Mock / 受控失败路径，不调用真实 DreamMaker、Yunwu、WellAPI、DeepSeek、Suno、MiniMax、Image 2 或公司系统；同时会通过 readiness smoke 保留并检查 DreamMaker guard 生产目标口径。

该组合入口不替代以下人工或专项验收：

- Claude 前端真实后端 UI smoke。
- `RENDER_WORKER_MODE=local-process` MP4 + `ffprobe` smoke。
- 真实模型受控 smoke。
- 公司 Adapter 真实替换联合验收。

仓库提供 `scripts/smoke/api-main-flow.sh`，用于在 API 已启动后复验主链路。脚本会覆盖：

- `GET /health`
- `POST /api/v1/works/lyrics`
- `GET /api/v1/works/{work_id}`
- `POST /api/v1/works/{work_id}/confirm`
- `GET /api/v1/works/{work_id}/publish-package`
- `POST /api/v1/works/{work_id}/publish-package/refresh-url`
- `POST /api/v1/works/{work_id}/publish-package/mark-fetched`
- `GET /internal/integration-readiness`
- PostgreSQL 状态抽查和本地对象文件抽查

同步 Mock 主链路：

```bash
MOCK_MUSIC_DURATION_MS=1000 \
MUSIC_PROVIDER=mock \
MUSIC_WORKFLOW_DISPATCH_MODE=sync \
WORKFLOW_OUTBOX_DISPATCHER_ENABLED=false \
./gradlew :apps:music-api:bootRun
```

另开一个终端执行：

```bash
EXPECTED_DURATION_MS=1000 scripts/smoke/api-main-flow.sh
```

render-worker local-process 主链路：

```bash
MOCK_MUSIC_DURATION_MS=1000 \
MUSIC_PROVIDER=mock \
MUSIC_WORKFLOW_DISPATCH_MODE=sync \
WORKFLOW_OUTBOX_DISPATCHER_ENABLED=false \
RENDER_WORKER_MODE=local-process \
RENDER_WORKER_WORKING_DIRECTORY=apps/render-worker \
RENDER_WORKER_COMMAND=npm \
RENDER_WORKER_ARGUMENTS=run,render:job,-- \
RENDER_WORKER_TIMEOUT=120s \
./gradlew :apps:music-api:bootRun
```

另开一个终端执行：

```bash
EXPECTED_DURATION_MS=1000 \
EXPECT_RENDER_WORKER=local-process \
scripts/smoke/api-main-flow.sh
```

local-process 分支会额外用 `ffprobe` 验证本地 MP4 为 H.264、1920x1080，并检查时长接近
`EXPECTED_DURATION_MS`。如不希望脚本检查 PostgreSQL 或本地文件，可分别设置
`CHECK_DB=false`、`CHECK_LOCAL_FILES=false`。

### Mock Publish Package Block Smoke

发布包交接前审核阻断可以用 Mock-only 配置复验。该路径只验证本地 `ModerationAdapter.preCheckPublishPackage`
阻断、`PACKAGE_BLOCKED` 状态、权益释放和前端可用动作，不调用真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统。

启动 API：

```bash
MOCK_MODERATION_PUBLISH_PACKAGE_BLOCKED_USER_IDS=mock_package_block_smoke \
MOCK_MUSIC_DURATION_MS=1000 \
MUSIC_PROVIDER=mock \
MUSIC_WORKFLOW_DISPATCH_MODE=sync \
WORKFLOW_OUTBOX_DISPATCHER_ENABLED=false \
RENDER_WORKER_MODE=mock \
./gradlew :apps:music-api:bootRun
```

另开一个终端执行：

```bash
MOCK_USER_ID=mock_package_block_smoke scripts/smoke/api-package-blocked-flow.sh
```

预期确认出歌返回 HTTP 403；随后作品详情为 `FAILED / FAILED / PACKAGE_BLOCKED`，失败码为
`PACKAGE_BLOCKED`，`available_actions` 包含 `CONTACT_SUPPORT` 和 `RETURN_TO_EDIT`，发布包响应不包含
`package_url` 或 `package_json`。

### OpenAPI Contract Smoke

仓库提供 `scripts/smoke/openapi-contract.sh`，用于在 API 已启动后对拍 `docs/api/openapi-v0.1.yaml`
和当前后端运行时响应。推荐仍使用同步 Mock API 启动方式：

```bash
MOCK_MUSIC_DURATION_MS=1000 \
MUSIC_PROVIDER=mock \
MUSIC_WORKFLOW_DISPATCH_MODE=sync \
WORKFLOW_OUTBOX_DISPATCHER_ENABLED=false \
RENDER_WORKER_MODE=mock \
./gradlew :apps:music-api:bootRun
```

另开一个终端执行：

```bash
scripts/smoke/openapi-contract.sh
```

脚本覆盖：

- 静态 OpenAPI path、operationId、schema required 字段和枚举值。
- `GET /api/v1/me`、灵感成歌、填词成歌、作品详情和作品列表。
- AI 润色、AI 续写、第 3 次编辑 409 统一错误响应。
- 确认出歌、封面重生、视频重渲、发布包获取、刷新 URL 和标记已交接。
- 缺失 `Idempotency-Key`、幂等冲突、作品不存在、`suno` 受控音乐失败和 mock 重试恢复。

该脚本只使用本地 Mock / 受控失败路径，不触发真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统。

### Agent Run Audit Smoke

真实模型联调前，Agent 调用审计通过 `agent_runs` 表记录。当前写词链路会先写入 `CreativeBriefAgent` run，
再写入 Mock DeepSeek 的 `LyricsAgent` run；确认出歌后，音乐提示词规划会写入 `MusicPromptAgent` run，音乐 Provider 提交前 Prompt 预检会写入 `ModerationAgent` run，封面提示词规划会写入 `CoverPromptAgent` run，发布包写入前质量门会写入 `QualityEvaluationAgent` run。记录只包含模型名、operation、
Prompt 模板版本、输入/输出 hash、状态、耗时和脱敏失败信息，不保存完整 Prompt 或用户原文。

API 启动后创建任意灵感成歌或填词成歌作品，再用 PostgreSQL 抽查：

```bash
docker exec yanyun-postgres psql -U postgres -d yanyun_music -Atc \
  "SELECT agent_name, agent_version, operation, model_name, status, input_hash IS NOT NULL, output_hash IS NOT NULL FROM agent_runs ORDER BY created_at DESC LIMIT 5;"
```

如果只创建作品，正常应能看到类似：

```text
CreativeBriefAgent|v0.1|INSPIRATION|mock-creative-brief|SUCCEEDED|t|t
LyricsAgent|v0.1|INSPIRATION|mock-deepseek-lyrics|SUCCEEDED|t|t
```

如果继续调用 `POST /api/v1/works/{work_id}/confirm`，正常还能看到类似：

```text
MusicPromptAgent|v0.1|MUSIC_PROMPT|mock-music-prompt|SUCCEEDED|t|t
ModerationAgent|v0.1|MUSIC_PROMPT_PRECHECK|mock-moderation-agent|SUCCEEDED|t|t
CoverPromptAgent|v0.1|COVER_PROMPT|mock-cover-prompt|SUCCEEDED|t|t
QualityEvaluationAgent|v0.1|PACKAGE_QUALITY_GATE|mock-quality-evaluation|SUCCEEDED|t|t
```

### Claude Web v1 UI Smoke

`prototypes/Claude-web-v1` 提供真实后端 UI smoke，用于把手动 Playwright 验收固化成可重复脚本。
脚本要求 API 已在 `http://localhost:8080` 启动，默认会自己启动 Vite 到 `http://127.0.0.1:5274`。

推荐先用同步 Mock 后端启动 API：

```bash
MOCK_MUSIC_DURATION_MS=1000 \
MUSIC_PROVIDER=mock \
MUSIC_WORKFLOW_DISPATCH_MODE=sync \
WORKFLOW_OUTBOX_DISPATCHER_ENABLED=false \
RENDER_WORKER_MODE=mock \
./gradlew :apps:music-api:bootRun
```

另开一个终端执行：

```bash
cd prototypes/Claude-web-v1
npm run smoke:real-backend
```

脚本覆盖：

- 390px 移动端首页创建灵感成歌作品。
- 歌词确认页展示歌词、燕云引用和状态提示。
- AI 润色必填、AI 续写、第 3 次改词 409 友好提示和 `request_id`。
- 确认出歌到成品页，展示媒体、交接下载链接、视频地址、封面地址和歌词正文。
- 刷新下载链接、标记已交接、作品列表展示和 1440px 桌面列表无横向溢出。
- 通过 API 造 `suno` 受控失败作品，前端失败页点击“重新生成”恢复到 `PACKAGE_READY`。

可选变量：

```bash
API_ORIGIN=http://localhost:8080 \
FRONTEND_PORT=5274 \
HEADLESS=true \
npm run smoke:real-backend
```

首次运行若提示缺少 Playwright Chromium，请执行：

```bash
cd prototypes/Claude-web-v1
npx playwright install chromium
```

对象存储默认走本地文件模式：

```text
build/local-object-storage/yanyun-works-local/yanyun-ai-music/local/{yyyy}/{MM}/{dd}/{work_id}/package/publish-package.json
```

注意：`./gradlew :apps:music-api:bootRun` 的运行工作目录是 `apps/music-api`，因此使用相对
`OBJECT_STORAGE_LOCAL_ROOT=build/local-object-storage` 时，实际文件会落在：

```text
apps/music-api/build/local-object-storage/yanyun-works-local/yanyun-ai-music/local/{yyyy}/{MM}/{dd}/{work_id}/package/publish-package.json
```

如需固定到仓库根目录，请把 `OBJECT_STORAGE_LOCAL_ROOT` 设置为绝对路径。

确认出歌后可用 `GET /api/v1/works/{work_id}/publish-package` 查看当前可访问链接和
`package_url_expires_at`，并用本地文件检查 package JSON 内容。刷新链接接口会复用数据库中的
`package_object_key`，不会重新猜测文件路径。

如需验证 Docker Compose 内置 MinIO，可用 S3 兼容模式启动 API：

```bash
OBJECT_STORAGE_PROVIDER=s3 \
S3_ENDPOINT=http://localhost:9000 \
S3_PUBLIC_ENDPOINT=http://localhost:9000 \
S3_ACCESS_KEY=minioadmin \
S3_SECRET_KEY=minioadmin \
S3_BUCKET_YANYUN_WORKS=yanyun-works-local \
S3_PATH_STYLE_ENABLED=true \
S3_AUTO_CREATE_BUCKET=true \
MUSIC_PROVIDER=mock \
DREAMMAKER_REAL_CALLS_ENABLED=false \
./gradlew :apps:music-api:bootRun
```

`minioadmin` 只用于本地 Docker 开发默认账号，不得替代公司或生产对象存储凭据。MinIO 模式生成后可抽查对象：

```bash
docker exec yanyun-minio mc alias set ylocal http://localhost:9000 minioadmin minioadmin
docker exec yanyun-minio mc stat \
  ylocal/yanyun-works-local/yanyun-ai-music/local/{yyyy}/{MM}/{dd}/{work_id}/package/publish-package.json
```

发布包 URL 为 presigned GET URL，过期后调用：

```bash
curl -X POST "http://localhost:8080/api/v1/works/{work_id}/publish-package/refresh-url" \
  -H "Content-Type: application/json" \
  -H "X-Mock-User-Id: local-user" \
  -H "Idempotency-Key: refresh-package-{uuid}"
```

Worker 单独健康检查：

```bash
./gradlew :apps:music-worker:bootRun
```

## Web

`apps/web` 当前只保留官方仓库内的 React + Vite 工程 scaffold，用于后续正式前端接入时承接代码：

```bash
cd apps/web
npm install
npm run build
npm test
npm run dev
```

当前可验收的高保真前端原型位于 `prototypes/Claude-web-v1`，默认通过 Vite proxy 把浏览器侧
`/api/v1` 请求转发到 `http://localhost:8080/api/v1`：

```bash
cd prototypes/Claude-web-v1
npm install
npm test
npm run typecheck
npm run build
npm run dev
```

离线演示可使用纯前端 Mock：

```text
http://localhost:{vite_port}/?mock=1
```

真实本地后端模式不加 `?mock=1`，需要先启动 `music-api`，并确保所有 POST 请求带
`X-Mock-User-Id` 和 `Idempotency-Key`。前端原型验收问题统一记录到
`docs/frontend/claude-web-v1-acceptance-fix-task-package.md`；本 Agent 默认只做任务包、接口、
状态和验收 review，不直接修改该原型实现。

## Render Worker

```bash
cd apps/render-worker
npm install
npm run build
npm test
```

默认后端仍使用 `MockVideoRenderService`，不会在确认出歌时触发真实 Remotion 渲染。
如需验证 Java 主链路可调用本地 render-worker 进程，先确保 `apps/render-worker` 依赖已安装，再启动 API 或 worker 时显式切换：

```bash
RENDER_WORKER_MODE=local-process \
RENDER_WORKER_WORKING_DIRECTORY=apps/render-worker \
RENDER_WORKER_COMMAND=npm \
RENDER_WORKER_ARGUMENTS=run,render:job,-- \
MUSIC_PROVIDER=mock \
./gradlew :apps:music-api:bootRun
```

该模式会由 Java `VideoRenderService` 调用 `npm run render:job -- --input ... --output ... --out-dir ...`，
render-worker 生成 MP4 和 timeline JSON 后，Java 再写入当前 `ObjectStorageClient`，发布包继续引用对象存储 URL。
本地进程服务会优先按当前进程目录解析 `RENDER_WORKER_WORKING_DIRECTORY`；如果相对路径不存在，会向上查找父目录，
因此 Gradle `bootRun` 下的默认 `apps/render-worker` 可解析到仓库根目录下的 render-worker。若将 JAR 放到仓库外运行，
建议把 `RENDER_WORKER_WORKING_DIRECTORY` 设置为绝对路径。

短 smoke 可直接验证 CLI，不需要启动后端：

```bash
cd apps/render-worker
npm run render:job -- \
  --input /path/to/render-input.json \
  --output /path/to/render-output.json \
  --out-dir /path/to/render-assets
```

`render-input.json` 至少包含 `work_id`、`song_title`、`lyrics_text`、`audio_object_key`、`cover_object_key` 和 `duration_ms`。
自动化测试不会渲染 2-4 分钟长视频；长视频真实成片只做手动 smoke。

## Current Boundary

当前自动化测试只验证工程、Mock 业务链路、Outbox/Workflow 启动边界、Temporal worker
注册和 activity 委托、DreamMaker Provider/Adapter 边界、发布包 JSON 的本地文件和 MinIO/S3
写入、render-worker 本地进程调用边界，不调用真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统。
真实 Provider 和长视频成片联调必须手动开启环境变量。
