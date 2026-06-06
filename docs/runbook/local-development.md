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

Temporal 模式完成后可抽查 outbox、job 和 work 状态：

```bash
docker exec yanyun-postgres psql -U postgres -d yanyun_music -Atc \
  "select o.status, o.attempt_count, j.status, j.stage, w.status, w.generation_stage, w.package_status from workflow_outbox o join generation_jobs j on j.work_id = o.aggregate_id join works w on w.id = o.aggregate_id where o.aggregate_id = '{work_id}' order by o.created_at desc limit 1"
```

也可以显式选择 `suno` 或 `minimax` 验证 DreamMaker Provider 边界。默认不配置
`DREAMMAKER_ACCESS_KEY` / `DREAMMAKER_SECRET_KEY` 时，确认出歌会返回 HTTP 409，并将作品持久化为可重试的
`MUSIC_GENERATION_FAILED`，不会发起真实供应商请求。

真实联调必须使用本地环境变量或生产密钥注入，不要把真实凭据写进仓库、文档、测试或命令日志：

```bash
export MUSIC_PROVIDER=suno
export DREAMMAKER_API_BASE_URL=https://api-all.dreammaker.netease.com
export DREAMMAKER_ACCESS_KEY=...
export DREAMMAKER_SECRET_KEY=...
export DREAMMAKER_REAL_CALLS_ENABLED=true
# Optional: only set when company integration wants DreamMaker to parse a real user identity.
export DREAMMAKER_USER_ACCESS_TOKEN=...
export SUNO_MODEL=chirp-crow
./gradlew :apps:music-api:bootRun
```

上面命令只用于说明变量名。真实 Suno / MiniMax 联调必须先阅读并执行
`docs/runbook/dreammaker-controlled-real-integration.md`；默认 `DREAMMAKER_REAL_CALLS_ENABLED=false`
会在外部 HTTP 请求前拒绝调用，防止误触真实供应商。真实联调推荐走 outbox + Temporal worker，
不要使用默认 `sync` 模式。

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
```

本地环境允许公司账号、审核、权益、发布、分享边界显示为 `MOCK_ONLY`。公司部署前，应检查
`blocks_company_deployment=true` 的项目，并按 `docs/handover/company-adapter-deployment-handoff-v0.1.md`
替换真实 Adapter 或由公司明确豁免。

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
