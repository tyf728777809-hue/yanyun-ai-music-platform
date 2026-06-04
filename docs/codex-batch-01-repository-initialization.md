# Codex 第 1 批仓库初始化任务说明

文档日期：2026-06-05

## 1. 目标

搭建燕云 AI 作曲平台的工程骨架和本地基础设施，不实现业务主链路。

第 1 批完成后，仓库应具备：

- Java 21 + Spring Boot 3 多模块工程。
- React + Vite + TypeScript 用户侧 Web scaffold，移动端优先，兼容 PC Web。
- API 应用、Worker 应用、渲染 Worker 的基础启动能力。
- 本地 Docker Compose 基础依赖。
- 统一构建、测试、格式化、健康检查和项目说明。
- 为后续数据库、领域状态机、Provider/Adapter、Workflow、前端页面开发留好目录边界。

## 2. 输入基线

- `yanyun-ai-music-platform-prd-v0.3.md`
- `yanyun-ai-music-platform-tech-design-v0.2.md`
- `AGENTS.md`
- `docs/project-progress.md`
- `docs/adr/0001-user-web-scope.md`
- `docs/adr/0002-commercial-grade-stack.md`

## 3. 明确不做

第 1 批只做工程初始化，不实现以下内容：

- 不实现真实 DeepSeek、MiniMax、Image 2 调用。
- 不实现歌词生成、出歌、封面生成、视频生成主链路。
- 不实现数据库业务表迁移。
- 不实现 Work 状态机。
- 不实现用户侧完整页面。
- 不实现完整前端业务交互，只创建 `apps/web` scaffold、基础路由和构建能力。
- 不接入网易大神账号、审核、发布、任务权益等真实公司系统。
- 不提交真实 API Key、Token、Cookie、生产配置或密钥。
- 不引入未在任务说明中列出的生产依赖，除非说明原因并得到确认。

## 4. 推荐仓库结构

```text
yanyun-ai-music-platform/
  apps/
    music-api/
    music-worker/
    render-worker/
    web/                         # React + Vite + TypeScript，移动端优先，兼容 PC Web

  modules/
    common/
    auth/
    quota/
    moderation/
    publish/
    work-domain/
    lyrics/
    knowledge/
    prompt/
    minimax/
    deepseek/
    image2/
    media/
    storage/
    workflow/
    config-center/
    observability/

  database/
    migrations/
    jooq/

  deploy/
    docker-compose.yml
    docker/
      music-api.Dockerfile
      music-worker.Dockerfile
      render-worker.Dockerfile

  docs/
    api/
    adr/
    runbook/

  knowledge-base/
    README.md
    worldview.md
    locations.md
    characters.md
    plot_events.md
    lyric_style_guide.md
    forbidden_claims.md

  AGENTS.md
  README.md
```

说明：`apps/web` 已在 PRD v0.3 和技术方案 v0.2 中确认。第 1 批必须创建 scaffold，但不实现完整业务页面。

## 5. Java 工程要求

### 5.1 技术栈

- Java 21。
- Spring Boot 3.x。
- Gradle Kotlin DSL。
- JUnit 5。
- Testcontainers 预留。
- Flyway 预留。
- jOOQ 预留。
- springdoc-openapi 预留。
- Spotless 格式化。

### 5.2 应用模块

`apps/music-api`：

- Spring Boot Web 应用。
- 提供 `/health` 或 Spring Actuator health endpoint。
- 不暴露业务 API。
- 使用本地 profile 启动。

`apps/music-worker`：

- Spring Boot Worker 应用。
- 可连接 Temporal 本地服务。
- 暂不注册真实业务 Workflow。
- 启动失败时应能清晰提示 Temporal 连接问题。

### 5.3 业务模块

`modules/*` 本批只需建立模块、基础包名和最小编译单元。

要求：

- 模块之间不要提前制造复杂依赖。
- Provider 模块不泄露供应商 HTTP 请求体给领域模块。
- Adapter 模块只定义未来边界，不在本批实现真实公司系统。
- 不创建空泛的大量类；能用 README 或 package marker 说明边界时，不强行写代码。

## 6. render-worker 要求

`apps/render-worker`：

- Node.js 22。
- TypeScript。
- Remotion scaffold。
- 提供 `npm run build`。
- 提供最小测试或 smoke check。
- 暂不实现真实歌词视频模板。
- 可保留一个最小 composition，证明 Remotion 工程能编译。

注意：

- 不提交未授权字体。
- 不生成大体积视频产物进入 Git。
- 后续 MP4 生成、FFmpeg 参数、字幕模板在第 8 批处理。

## 7. web 要求

`apps/web`：

- React。
- Vite。
- TypeScript。
- 移动端优先，兼容 PC Web。
- 提供 `npm run build`。
- 提供最小测试或 smoke check。
- 可保留最小首页和健康/占位页面，证明工程能启动和构建。
- 不实现完整创作、确认、生成中、成品页业务流程。

注意：

- 不直接访问数据库、对象存储或 Provider。
- 只通过 `music-api` 获取数据。
- 不实现社区发布、分享、点赞、评论、推荐流。

## 8. Docker Compose 要求

本批提供本地基础依赖：

- PostgreSQL 16。
- Redis 7。
- Temporal local/auto-setup。
- MinIO。
- OpenSearch。
- Prometheus。
- Grafana。

要求：

- `docker compose up` 能启动基础依赖。
- 默认账号密码只能用于本地开发。
- 不包含真实 Provider API Key。
- `.env.example` 可提供变量名和本地默认值，不提供真实密钥。

## 9. README 要求

新增或更新 `README.md`，至少包含：

- 项目简介。
- 当前阶段。
- 技术栈。
- 本地依赖。
- 构建命令。
- 测试命令。
- 格式化命令。
- 启动基础依赖命令。
- 启动 `music-api` 和 `music-worker` 的命令。
- 启动/构建 `web` 的命令。
- 启动/构建 `render-worker` 的命令。
- 当前未实现范围。

## 10. AGENTS.md 要求

保留现有进度记录规则，并补充工程命令：

```text
./gradlew clean build
./gradlew test
./gradlew spotlessApply
./gradlew spotlessCheck

cd apps/render-worker
npm install
npm run build
npm test

cd apps/web
npm install
npm run build
npm test
```

如命令尚未可用，应在 README 和进度文档中说明原因。

## 11. 验收标准

必须满足：

- `git status` 中不包含无意生成的大文件、密钥或临时文件。
- `./gradlew clean build` 成功。
- `./gradlew test` 成功，至少包含最小测试。
- `./gradlew spotlessCheck` 成功。
- `cd apps/web && npm run build` 成功。
- `cd apps/web && npm test` 成功，或提供等价 smoke check。
- `cd apps/render-worker && npm run build` 成功。
- `cd apps/render-worker && npm test` 成功，或提供等价 smoke check。
- `docker compose up` 可启动 PostgreSQL、Redis、Temporal、MinIO、OpenSearch、Prometheus、Grafana。
- `music-api` 启动后健康检查返回 OK。
- `music-worker` 可连接 Temporal。
- `docs/project-progress.md` 已追加本批执行结果和验证结果。

## 12. 建议执行顺序

1. 创建根 Gradle 工程和 Java 模块。
2. 创建 `music-api` 最小 Spring Boot 应用和健康检查。
3. 创建 `music-worker` 最小 Spring Boot 应用和 Temporal 连接配置。
4. 配置 Spotless、JUnit 5、基础测试。
5. 配置 Flyway、jOOQ、Testcontainers、springdoc-openapi 的依赖或预留目录。
6. 创建 `render-worker` TypeScript + Remotion 最小工程。
7. 创建 `apps/web` React + Vite + TypeScript 最小工程。
8. 创建 Docker Compose 基础依赖。
9. 创建 `.gitignore`、`.env.example`、README。
10. 更新 `AGENTS.md` 工程命令。
11. 运行验收命令。
12. 更新 `docs/project-progress.md`。

## 13. 交付物

- 工程骨架文件。
- 本地基础设施配置。
- README。
- 更新后的 `AGENTS.md`。
- 更新后的 `docs/project-progress.md`。
- 验收命令输出摘要。
