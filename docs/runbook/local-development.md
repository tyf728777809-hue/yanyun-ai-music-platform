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

Workflow dispatch 默认走同步本地模式，方便保持当前 Mock 主链路 smoke：

```bash
MUSIC_WORKFLOW_DISPATCH_MODE=sync WORKFLOW_OUTBOX_DISPATCHER_ENABLED=false \
  ./gradlew :apps:music-api:bootRun
```

如果要验证真实模型链路前的可靠启动边界，可切到 Outbox 模式：

```bash
MUSIC_WORKFLOW_DISPATCH_MODE=outbox \
WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true \
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

也可以显式选择 `suno` 或 `minimax` 验证 DreamMaker Provider 边界。默认不配置
`DREAMMAKER_API_KEY` 时，确认出歌会返回 HTTP 409，并将作品持久化为可重试的
`MUSIC_GENERATION_FAILED`，不会发起真实供应商请求。

真实联调必须使用本地环境变量或生产密钥注入，不要把真实凭据写进仓库、文档、测试或命令日志：

```bash
export MUSIC_PROVIDER=suno
export DREAMMAKER_API_BASE_URL=https://api-all.dreammaker.netease.com
export DREAMMAKER_API_KEY=...
export SUNO_MODEL=chirp-crow
./gradlew :apps:music-api:bootRun
```

MiniMax 可用：

```bash
export MUSIC_PROVIDER=minimax
export MINIMAX_MODEL=minimax-music-2.6
./gradlew :apps:music-api:bootRun
```

当前工程已预留 `DREAMMAKER_ACCESS_KEY` / `DREAMMAKER_SECRET_KEY` 变量名，但 Feishu 资料只明确
`Authorization: Bearer <key>`。在供应商确认 AccessKey/SecretKey 如何换取 Bearer 或签名之前，不要擅自把
AccessKey/SecretKey 当作请求头使用。

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

本地 Mock 发布包文件会写入：

```text
build/local-object-storage/yanyun-works-local/packages/{work_id}.json
```

确认出歌后可用 `GET /api/v1/works/{work_id}/publish-package` 查看 `package_url`，并用本地文件检查 package JSON 内容。

Worker：

```bash
./gradlew :apps:music-worker:bootRun
```

## Web

```bash
cd apps/web
npm install
npm run build
npm test
npm run dev
```

## Render Worker

```bash
cd apps/render-worker
npm install
npm run build
npm test
```

## Current Boundary

当前自动化测试只验证工程、Mock 业务链路、Outbox/Workflow 启动边界、DreamMaker Provider/Adapter 边界和本地发布包文件写入，不调用真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统。真实 Provider 联调必须手动开启环境变量。
