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

也可以显式选择 `suno` 或 `minimax` 验证失败边界。当前两者真实提交尚未实现，确认出歌会返回 HTTP 409，并将作品持久化为可重试的 `MUSIC_GENERATION_FAILED`，不会调用真实 API。

失败后可通过音乐重试接口验证恢复边界：

```bash
curl -X POST "http://localhost:8080/api/v1/works/{work_id}/music/retry" \
  -H "Content-Type: application/json" \
  -H "X-Mock-User-Id: local-user" \
  -H "Idempotency-Key: retry-{uuid}" \
  -d '{"music_provider":"mock"}'
```

预期结果是作品从 `FAILED / MUSIC_GENERATION_FAILED` 恢复到 `GENERATED / PACKAGE_READY`。权益流水应体现首次失败释放、重试成功提交：`LOCK_GENERATE -> RELEASE_GENERATE -> LOCK_GENERATE -> COMMIT_GENERATE`。

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

当前本地阶段只验证工程、Mock 业务链路、Provider/Adapter 边界和本地发布包文件写入，不调用真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统。
