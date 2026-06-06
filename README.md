# 燕云 AI 作曲平台

燕云 AI 作曲平台是正式商用级 AI 作曲与视频成片平台。当前目标是在本地先跑通完整创作链路的工程基础：用户输入、AI 写词/润色、歌词确认、出歌、封面、MP4 歌词视频和发布包交接。公司账号、审核、权益、发布、分享等系统通过 Adapter 边界预留，本地阶段默认 Mock。

## Current Stage

当前已完成本地商用级主链路基础：Mock 写词、Mock 音乐 Provider、发布包对象存储、Outbox/Temporal 编排边界、公司 Adapter readiness、render-worker local-process MP4 成片边界均已落地。当前可验收前端位于 `prototypes/Claude-web-v1`，验收缺口已修复，并已完成真实后端模式 UI smoke 脚本化复验。`apps/web` 仍是正式工程 scaffold，是否迁移原型或由公司前端重建需在交付前单独决策。真实 DeepSeek、Suno/MiniMax、Image 2 和公司系统仍默认关闭或 Mock。

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

`MUSIC_PROVIDER=suno|minimax` 会走真实音乐 Provider 边界，但仍受硬开关保护。
当前非公司内网环境下，Suno 可用 `SUNO_BACKEND=yunwu` 走 Yunwu 公网联调路径：
`YUNWU_REAL_CALLS_ENABLED=true`、`YUNWU_API_KEY`、`YUNWU_BASE_URL=https://yunwu.ai`。
DreamMaker 接口不删除，`SUNO_BACKEND=dreammaker` 是正式生产目标切换路径；MiniMax
当前仍通过 DreamMaker 接入。DreamMaker 真实调用需要 `DREAMMAKER_ACCESS_KEY` /
`DREAMMAKER_SECRET_KEY`，客户端会生成 HS256 JWT 并使用 `Authorization: Bearer <jwt>` 请求。
如果后续公司接入方需要按真实用户视角归因，可通过安全注入
`DREAMMAKER_USER_ACCESS_TOKEN` 让客户端透传 `X-Access-Token`。

真实音乐联调必须显式打开对应后端的真实调用开关，并按
[Yunwu Suno 受控真实联调 Runbook](docs/runbook/yunwu-suno-controlled-real-integration.md)
或 [DreamMaker 受控真实联调 Runbook](docs/runbook/dreammaker-controlled-real-integration.md)
执行；运行时已阻止在默认 `sync` 模式下跑真实 Provider。自动化测试不会调用真实供应商，
Yunwu 一键 smoke 使用 `ALLOW_YUNWU_REAL_SMOKE=1 scripts/smoke/yunwu-suno-real-music-stack-smoke.sh`；
DreamMaker smoke 必须额外设置 `ALLOW_DREAMMAKER_REAL_SMOKE=1` 才会调用 DreamMaker。

DeepSeek 写词默认仍使用 Mock；显式设置 `AGENT_REAL_CALLS_ENABLED=true` 和
`DEEPSEEK_REAL_CALLS_ENABLED=true` 后，会使用 OpenAI 兼容的
`RealDeepSeekLyricsClient` 调用 `DEEPSEEK_BASE_URL`，默认模型为 `deepseek-v4-pro`。真实联调按
[DeepSeek 受控真实联调 Runbook](docs/runbook/deepseek-controlled-real-integration.md)、
[DeepSeek 凭据与日志规则](docs/security/deepseek-secret-and-log-handling.md) 和
[DeepSeek 验收清单](docs/checklists/deepseek-real-integration-acceptance.md) 执行；API Key 只允许通过本地 shell、
部署 Secret 或公司配置中心注入，不写入仓库。`/internal/integration-readiness`
已包含 `deepseek_guard`，用于展示 DeepSeek 真实调用开关、模型名和 API Key 配置状态。

Image 2 封面默认仍使用 Mock；显式设置 `IMAGE_PROVIDER=image2`、
`IMAGE_REAL_CALLS_ENABLED=true` 后，当前默认 `IMAGE2_BACKEND=wellapi`，通过
`WELLAPI_BASE_URL=https://wellapi.ai` 和 `WELLAPI_API_KEY` 调用 `gpt-image-2`。供应商返回
URL 时会导入平台对象存储；若只返回 `b64_json`，workflow 会直接写入平台对象存储后再进入发布包。
`IMAGE2_BACKEND=dreammaker` 仍保留为正式生产目标切换路径。真实联调按
[Image 2 受控真实联调 Runbook](docs/runbook/image2-controlled-real-integration.md)、
[Image 2 凭据与日志规则](docs/security/image2-secret-and-log-handling.md) 和
[Image 2 验收清单](docs/checklists/image2-real-integration-acceptance.md) 执行。`/internal/integration-readiness`
已包含 `image2_guard`，用于展示 Image 2 后端、真实调用开关和密钥配置状态。
WellAPI 一键封面 smoke 使用
`ALLOW_WELLAPI_IMAGE2_REAL_SMOKE=1 scripts/smoke/wellapi-image2-real-cover-stack-smoke.sh`；
该脚本只打开真实 Image 2，音乐、DeepSeek、render-worker 和公司 Adapter 仍保持 Mock。

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

如需对拍 OpenAPI v0.1 与当前后端运行时响应：

```bash
scripts/smoke/openapi-contract.sh
```

如果 API 以 `RENDER_WORKER_MODE=local-process` 启动，可额外验证真实 MP4：

```bash
EXPECTED_DURATION_MS=1000 EXPECT_RENDER_WORKER=local-process scripts/smoke/api-main-flow.sh
```

Claude 前端原型也提供真实后端接口 UI smoke。它验证真实后端接口和 Mock 主链路，不代表真实模型成功出歌。先按上面的同步 Mock 方式启动 API，再执行：

```bash
cd prototypes/Claude-web-v1
npx playwright install chromium
npm run smoke:real-backend
```

该脚本会临时启动 Vite，使用 Playwright 跑通灵感成歌、AI 润色/续写、第三次改词 409 友好提示、
确认出歌、发布交接、作品列表，以及 `suno` 受控失败后的前端重试恢复。

## Web Commands

当前可验收前端：

```bash
cd prototypes/Claude-web-v1
npm install
npm test
npm run typecheck
npm run build
npm run dev
```

正式工程 scaffold：

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
运行时契约 smoke 位于 `scripts/smoke/openapi-contract.sh`，要求 API 已在 `http://localhost:8080` 启动，会覆盖静态 OpenAPI path/schema/enum 检查、作品创建、作品详情、作品列表、统一错误、幂等冲突、润色次数、发布包交接、受控音乐失败和 mock 重试恢复。

## Delivery Checklist

本地完整跑通并交给公司开发接入前，按
[`docs/checklists/local-commercial-delivery-acceptance.md`](docs/checklists/local-commercial-delivery-acceptance.md)
走查仓库、后端、前端、真实模型联调准备、公司 Adapter 和安全日志口径。

## Not Implemented Yet

- 真实 DeepSeek、Image 2、MiniMax 手动联调；Suno via DreamMaker 已执行一次单作品真实 smoke，但 DreamMaker 创建任务阶段返回 HTTP 403，初步判断与公司内网/账号权限有关。当前改走 Yunwu 公网联调路径，尚未执行真实成功 smoke。Image 2 当前已具备 WellAPI 脚本化 smoke 入口，但尚未执行真实图片调用。
- 真实歌词生成、真实封面生成、真实长视频生成主链路。
- 正式前端承接决策；当前可验收前端在 `prototypes/Claude-web-v1`，`apps/web` 仍是 scaffold。
- 公司真实账号、审核、权益、发布、分享系统接入。
- 大体积音视频产物或真实生成素材入库。
