# 燕云 AI 作曲平台

燕云 AI 作曲平台是正式商用级 AI 作曲与视频成片平台。当前目标是在本地先跑通完整创作链路的工程基础：用户输入、AI 写词/润色、歌词确认、出歌、封面、MP4 歌词视频和发布包交接。公司账号、审核、权益、发布、分享等系统通过 Adapter 边界预留，本地阶段默认 Mock。

## Current Stage

当前已完成本地商用级主链路基础：Mock 写词、Mock 音乐 Provider、发布包对象存储、Outbox/Temporal 编排边界、公司 Adapter readiness、render-worker local-process MP4 成片边界均已落地。前端原型位于 `prototypes/Claude-web-v1`，已完成初审但仍需按 `docs/frontend/claude-web-v1-acceptance-fix-task-package.md` 修复契约缺口。真实 DeepSeek、Suno/MiniMax、Image 2 和公司系统仍默认关闭或 Mock。

## Stack

- Backend: Java 21, Spring Boot 3, Gradle Kotlin DSL。
- Workflow: Temporal。
- Storage and data: PostgreSQL 16, Redis 7, MinIO/S3, OpenSearch。
- Web: React, Vite, TypeScript，移动端优先，兼容 PC Web。
- Render worker: Node.js 22, TypeScript, Remotion，后续接 FFmpeg/FFprobe。
- Observability: Spring Actuator, Prometheus, Grafana。

## Local Prerequisites

- JDK 21。
- Node.js 22 或更高版本。
- Docker Desktop。

本机如果使用 Homebrew `openjdk@21`：

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
```

## Infrastructure

```bash
docker compose -f deploy/docker-compose.yml --env-file .env.example up -d
docker compose -f deploy/docker-compose.yml ps
```

本地服务默认端口：

- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- Temporal: `localhost:7233`
- Temporal UI: `http://localhost:8233`
- MinIO: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`
- OpenSearch: `http://localhost:9200`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

## Backend Commands

```bash
./gradlew clean build
./gradlew test
./gradlew spotlessApply
./gradlew spotlessCheck
./gradlew :apps:music-api:bootRun
./gradlew :apps:music-worker:bootRun
```

音乐 Provider 默认使用本地 Mock：

```bash
MUSIC_PROVIDER=mock ./gradlew :apps:music-api:bootRun
```

`MUSIC_PROVIDER=suno|minimax` 会走 DreamMaker 接入骨架。真实调用需要本地环境变量
`DREAMMAKER_ACCESS_KEY` / `DREAMMAKER_SECRET_KEY`；客户端会按 DreamMaker 要求生成
HS256 JWT，并使用 `Authorization: Bearer <jwt>` 请求。自动化测试不会调用真实供应商。
如果后续公司接入方需要按真实用户视角归因，可通过安全注入
`DREAMMAKER_USER_ACCESS_TOKEN` 让客户端透传 `X-Access-Token`。

真实 Suno/MiniMax 联调必须显式打开 `DREAMMAKER_REAL_CALLS_ENABLED=true`，并按
[DreamMaker 受控真实联调 Runbook](docs/runbook/dreammaker-controlled-real-integration.md)
执行；不要在默认 `sync` 模式下跑真实 Provider。

生成编排默认保持同步 Mock 模式；要验证 Outbox local 异步启动边界：

```bash
MUSIC_WORKFLOW_DISPATCH_MODE=outbox WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true \
WORKFLOW_OUTBOX_DISPATCH_TARGET=local \
  ./gradlew :apps:music-api:bootRun
```

要验证 API 与 worker 分进程的 Temporal 编排边界，先启动 worker，再启动 API：

```bash
MUSIC_PROVIDER=mock ./gradlew :apps:music-worker:bootRun
MUSIC_PROVIDER=mock MUSIC_WORKFLOW_DISPATCH_MODE=outbox \
WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal \
  ./gradlew :apps:music-api:bootRun
```

健康检查：

```bash
curl http://localhost:8080/health
curl http://localhost:8080/actuator/health
```

API 启动后可运行本地主链路 smoke：

```bash
EXPECTED_DURATION_MS=1000 scripts/smoke/api-main-flow.sh
```

如果 API 以 `RENDER_WORKER_MODE=local-process` 启动，可额外验证真实 MP4：

```bash
EXPECTED_DURATION_MS=1000 EXPECT_RENDER_WORKER=local-process scripts/smoke/api-main-flow.sh
```

Claude 前端原型也提供真实后端 UI smoke。先按上面的同步 Mock 方式启动 API，再执行：

```bash
cd prototypes/Claude-web-v1
npm run smoke:real-backend
```

该脚本会临时启动 Vite，使用 Playwright 跑通灵感成歌、AI 润色/续写、第三次改词 409 友好提示、
确认出歌、发布交接、作品列表，以及 `suno` 受控失败后的前端重试恢复。

## Web Commands

```bash
cd apps/web
npm install
npm run build
npm test
npm run dev
```

## Render Worker Commands

```bash
cd apps/render-worker
npm install
npm run build
npm test
```

## API Contract

OpenAPI v0.1 位于 `docs/api/openapi-v0.1.yaml`，覆盖作品状态、生成阶段、剩余改词次数、权益提示、失败可执行动作和发布包交接状态。

## Not Implemented Yet

- 真实 DeepSeek、Image 2 调用。
- 真实 Suno/MiniMax 手动联调。
- 真实歌词生成、封面生成、视频生成主链路。
- 用户侧完整创作页面。
- 公司真实账号、审核、权益、发布、分享系统接入。
- 大体积音视频产物或真实生成素材入库。
