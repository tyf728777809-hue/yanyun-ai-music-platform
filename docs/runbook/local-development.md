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

健康检查：

```bash
curl http://localhost:8080/health
curl http://localhost:8080/actuator/health
```

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

第 1 批只验证工程启动、构建、测试和本地依赖边界，不调用真实 DeepSeek、MiniMax、Image 2 或公司系统。
