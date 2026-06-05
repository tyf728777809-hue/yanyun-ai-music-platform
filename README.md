# 燕云 AI 作曲平台

燕云 AI 作曲平台是正式商用级 AI 作曲与视频成片平台。当前目标是在本地先跑通完整创作链路的工程基础：用户输入、AI 写词/润色、歌词确认、出歌、封面、MP4 歌词视频和发布包交接。公司账号、审核、权益、发布、分享等系统通过 Adapter 边界预留，本地阶段默认 Mock。

## Current Stage

第 2 批后端 Mock 业务闭环与音乐 Provider 接入准备：本地默认 `mock` 可跑通作品到发布包，Suno/MiniMax 已通过 DreamMaker 接入骨架预置，真实联调需本地安全配置密钥。

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
`DREAMMAKER_API_KEY`；自动化测试不会调用真实供应商。

健康检查：

```bash
curl http://localhost:8080/health
curl http://localhost:8080/actuator/health
```

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
- 真实 Suno/MiniMax 联调和 AccessKey/SecretKey 鉴权口径确认。
- 真实歌词生成、封面生成、视频生成主链路。
- 用户侧完整创作页面。
- 公司真实账号、审核、权益、发布、分享系统接入。
- 大体积音视频产物或真实生成素材入库。
