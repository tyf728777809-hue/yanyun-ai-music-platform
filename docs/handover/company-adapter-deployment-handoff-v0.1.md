# 公司 Adapter 接入与部署交接说明 v0.1

更新时间：2026-06-07

## 1. 交接目标

本平台本地阶段负责跑通 AI 作曲与 MP4 成片链路。公司开发接入阶段负责把以下 Mock 边界替换为公司真实系统：

- 账号身份。
- 内容审核。
- 权益锁定、扣减、释放。
- 社区发布交接。
- 分享系统。

本平台不重做公司社区、发布审核流、分享系统、互动和推荐流。

进入具体 Adapter 替换前，先阅读 `docs/handover/company-delivery-package-v0.1.md` 和 `docs/handover/local-commercial-delivery-status-v0.1.md`，确认哪些能力已经本地验证、哪些只准备好受控 smoke、哪些必须由公司系统或部署方案补齐。

视频成片当前支持两种模式：默认 `MockVideoRenderService`，以及本地 smoke 用的
`LocalProcessVideoRenderService`。公司部署前应在 `/internal/integration-readiness` 中检查
`render_worker` 组件；本地进程模式可证明 Remotion MP4 链路可跑通，但生产是否采用进程模式、独立服务或队列化 render worker 需要公司部署方案确认。

真实音乐和 Image 2 供应商边界中，DreamMaker 是必须保留的正式生产目标。Yunwu / WellAPI 只用于当前非公司内网环境下的公网受控联调，不作为删除 DreamMaker 接口的理由。
生产或公司内网部署应参考 `deploy/env.production.example`，启用 `SPRING_PROFILES_ACTIVE=prod`；
API 与 worker 的 `prod/production` profile 会把 `SUNO_BACKEND` 和 `IMAGE2_BACKEND` 的默认值切回
`dreammaker`。`.env.example` 是本地 Mock / 公网 smoke 便利配置，不得直接当作生产默认。

## 2. 当前可检查接口

启动 `music-api` 后访问：

```bash
curl http://localhost:8080/internal/integration-readiness
scripts/smoke/company-adapter-readiness-smoke.sh
```

该接口只读取配置和静态边界，不调用真实公司系统、不调用真实模型供应商、不输出密钥值。公司部署前需要重点检查：

- `overall_status`
- 每个 `components[].status`
- 每个 `components[].blocks_company_deployment`
- 每个 `components[].required_env_vars`

本地开发环境允许 `MOCK_ONLY`；公司部署前，所有公司系统相关 `blocks_company_deployment=true` 项必须替换真实实现或由公司明确豁免。

`scripts/smoke/company-adapter-readiness-smoke.sh` 只读检查 readiness 报告结构、公司 Mock 边界、部署变量名和明显密钥泄漏形态，不调用真实公司系统或真实模型供应商；适合作为公司交接时的脱敏摘要证据。

该脚本还会检查 `dreammaker_guard`、`DreamMakerHttpClient` 和 `DREAMMAKER_*` 变量名仍然存在。公司内网或生产环境切回 DreamMaker 时，应通过安全配置系统注入真实值，而不是改代码或写入仓库。

## 3. Adapter 替换清单

| 边界 | 当前接口 | 当前实现 | 公司接入要求 |
|---|---|---|---|
| 账号 | `modules/auth/AccountAdapter` | `MockAccountAdapter` | 用公司账号态或网关注入真实用户，替换 `X-Mock-User-Id` |
| 审核 | `modules/moderation/ModerationAdapter` | `MockModerationAdapter` | 覆盖输入、歌词和发布包交接前审核 |
| 权益 | `modules/quota/QuotaAdapter` | `MockQuotaAdapter` | 实现生成权益 lock / commit / release 幂等语义 |
| 发布交接 | `modules/publish/PublishAdapter` | `MockPublishAdapter` | 接公司发布系统需要的交接元数据；社区发布结果仍由公司系统管理 |
| 分享 | 当前无业务调用点 | `NotImplementedShareBoundary` | 分享入口、分享卡片、传播链路由公司系统接管 |
| DreamMaker | `modules/dreammaker/DreamMakerHttpClient` | 默认真实调用关闭 | 保留正式生产目标，生产通过 `DREAMMAKER_*` 安全注入启用 |

## 4. 账号接入

当前本地方式：

- Header：`X-Mock-User-Id`
- 默认用户：`mock_user_001`
- 当前接口：`AccountAdapter.getCurrentUser(String userId)`

公司接入要求：

- 不信任普通用户自传的 `X-Mock-User-Id`。
- 由公司网关、登录态、服务端 session 或可信 token 解析真实用户。
- 返回稳定 `user_id`、昵称、头像和角色。
- 后端所有作品查询和写操作必须继续按 `user_id` 隔离。

建议环境变量：

```text
COMPANY_ACCOUNT_ADAPTER_MODE=company
COMPANY_ACCOUNT_BASE_URL=
```

## 5. 审核接入

当前接口：

- `preCheckUserInput(userId, text)`
- `preCheckLyrics(userId, lyricsText)`
- `preCheckPublishPackage(userId, workId)`

公司接入要求：

- 审核阻断要返回可映射 code 和用户可读 message。
- 输入审核失败映射到 `USER_INPUT_BLOCKED`。
- 歌词审核失败映射到 `LYRICS_PRECHECK_FAILED`。
- 发布包交接前审核失败映射到 `PACKAGE_BLOCKED`。
- 不把公司审核原始敏感响应直接透出给用户。

建议环境变量：

```text
COMPANY_MODERATION_ADAPTER_MODE=company
COMPANY_MODERATION_BASE_URL=
```

## 6. 权益接入

当前接口：

- `getHint(userId, usedPolishCount)`
- `lockGenerateQuota(userId, workId)`
- `commitGenerateQuota(userId, lockId)`
- `releaseGenerateQuota(userId, lockId, reason)`

当前产品口径：

- 确认出歌时锁定主生成权益。
- MP4 发布包可用后扣减主权益。
- 失败按原因释放权益或允许重试。
- 音乐重试上限当前本地为 2 次。

公司接入要求：

- `lockId` 必须稳定、可追踪、可幂等。
- `commit` 和 `release` 必须能安全重复调用。
- 公司权益流水需能按 `work_id` 对账。
- 权益失败映射到 `QUOTA_LOCK_FAILED` 或可恢复业务失败。

建议环境变量：

```text
COMPANY_QUOTA_ADAPTER_MODE=company
COMPANY_QUOTA_BASE_URL=
```

## 7. 发布与分享接入

本平台提供：

- 作品状态。
- 媒体资产 URL。
- 发布包 JSON。
- 发布包 URL 刷新。
- 标记已交接。

公司系统负责：

- 社区发布表单。
- 发布审核流。
- 分享卡片。
- 分享渠道。
- 点赞、评论、收藏、推荐流。

建议环境变量：

```text
COMPANY_PUBLISH_ADAPTER_MODE=company
COMPANY_PUBLISH_BASE_URL=
COMPANY_SHARE_ADAPTER_MODE=company
COMPANY_SHARE_BASE_URL=
```

## 8. 部署变量检查

公司部署变量分为三类：基础运行必需项、公司 Adapter 替换必需项、以及真实模型和 render-worker 的部署选型项。当前仓库默认仍可使用 Mock Provider 和 Mock render-worker 做本地验证；生产是否启用真实 Suno / MiniMax、是否采用 `local-process` render-worker，需要公司部署方案和联调窗口确认。

生产变量名样例：

```bash
deploy/env.production.example
```

交付前可运行以下只读审计，确认生产 profile、Java fallback、readiness 默认值和交接文档没有把公网联调后端当成生产默认：

```bash
scripts/smoke/production-provider-defaults-audit.sh
```

### 8.1 基础运行必需项

```text
YANYUN_ENV=
SPRING_PROFILES_ACTIVE=prod
POSTGRES_HOST=
POSTGRES_DB=
POSTGRES_USER=
POSTGRES_PASSWORD=
TEMPORAL_TARGET=
TEMPORAL_NAMESPACE=
TEMPORAL_TASK_QUEUE=
OBJECT_STORAGE_PROVIDER=s3
S3_ENDPOINT=
S3_PUBLIC_ENDPOINT=
S3_REGION=
S3_BUCKET_YANYUN_WORKS=
S3_ACCESS_KEY=
S3_SECRET_KEY=
MUSIC_WORKFLOW_DISPATCH_MODE=outbox
WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal
```

### 8.2 公司 Adapter 替换必需项

```text
COMPANY_ACCOUNT_ADAPTER_MODE=company
COMPANY_MODERATION_ADAPTER_MODE=company
COMPANY_QUOTA_ADAPTER_MODE=company
COMPANY_PUBLISH_ADAPTER_MODE=company
COMPANY_SHARE_ADAPTER_MODE=company
```

### 8.3 真实模型与 render-worker 选型项

以下变量不是当前本地 Mock 阶段的默认要求。只有在公司确认真实模型联调和成片部署方案后才启用：

```text
MUSIC_PROVIDER=suno|minimax
DREAMMAKER_REAL_CALLS_ENABLED=true
DREAMMAKER_API_BASE_URL=
DREAMMAKER_ACCESS_KEY=
DREAMMAKER_SECRET_KEY=
SUNO_BACKEND=dreammaker
IMAGE2_BACKEND=dreammaker
RENDER_WORKER_MODE=local-process
RENDER_WORKER_WORKING_DIRECTORY=
RENDER_WORKER_COMMAND=
RENDER_WORKER_ARGUMENTS=
RENDER_WORKER_TIMEOUT=
```

若公司最终采用独立 render service、队列化 worker 或容器化渲染服务，应保留 `VideoRenderService` 调用边界，但替换具体部署变量和实现方式。

当前本地公网联调可使用 `SUNO_BACKEND=yunwu` 或 `IMAGE2_BACKEND=wellapi`，但这只是联调选项；正式生产切回 DreamMaker 时应使用 `SUNO_BACKEND=dreammaker` 和 `IMAGE2_BACKEND=dreammaker`，并确认 `dreammaker_guard` 通过。

真实值必须由公司安全配置系统或部署平台注入，不得写入仓库、镜像、日志或测试快照。

## 9. 公司接入 Smoke

1. 调用 `/internal/integration-readiness`，并运行 `scripts/smoke/company-adapter-readiness-smoke.sh`，确认公司 Adapter 状态可解释且 readiness 响应不泄露密钥。
2. 替换公司 Adapter 前，运行 `scripts/smoke/production-provider-defaults-audit.sh`，确认生产默认仍指向 DreamMaker，而 Yunwu / WellAPI 只保留为公网 smoke 后端。
3. 调用 `/api/v1/me`，确认返回真实公司用户。
4. 运行 `scripts/smoke/api-main-flow.sh`，确认主链路可创建作品、确认出歌、获取发布包、刷新 URL、标记交接。
5. 若启用 `RENDER_WORKER_MODE=local-process`，运行 `EXPECTED_DURATION_MS=1000 EXPECT_RENDER_WORKER=local-process scripts/smoke/api-main-flow.sh`，确认 MP4 与 timeline 写入对象存储，并用 `ffprobe` 验证视频。
6. 运行 `cd prototypes/Claude-web-v1 && npm run smoke:real-backend`，确认真实后端模式下前端创作、改词、409 友好提示、成品交接、作品列表和失败重试恢复。
7. 灵感成歌创建作品，确认作品归属到真实 `user_id`。
8. 输入一条公司审核应阻断的内容，确认返回可读失败提示。
9. 正常作品确认出歌，确认权益先锁定，发布包可用后扣减。
10. 触发一次音乐失败或模拟失败，确认权益释放或重试口径正确。
11. 作品生成到 `PACKAGE_READY` 后，确认公司发布系统能读取发布包 URL。
12. 调用标记已交接接口，确认公司侧不把它误解为“已发布成功”。

## 10. 总体验收清单

本交接文档只覆盖公司 Adapter 替换与部署接入。完整本地商用闭环交付前，还需要按
`docs/checklists/local-commercial-delivery-acceptance.md` 统一走查仓库状态、基础设施、后端主链路、
前端真实后端模式、真实模型联调准备、安全日志和公司 Adapter 接入状态。

公司开发实际替换账号、审核、权益、发布、分享边界时，按
`docs/checklists/company-adapter-replacement-readiness.md` 逐项留下接口、幂等、隔离和 smoke 证据。

其中公司 Adapter 部分至少应留下以下证据：

- `/internal/integration-readiness` 的脱敏响应摘要。
- 账号、审核、权益、发布、分享五类 Adapter 的真实实现或公司豁免说明。
- 权益 `lock / commit / release` 幂等验证结果。
- 公司发布系统读取发布包 URL 与标记交接的 smoke 结果。
- 生产环境不信任 `X-Mock-User-Id` 的验证说明。

## 11. 禁止事项

- 不在本平台内重做公司社区系统。
- 不把真实 AccessKey、SecretKey、Cookie、JWT、公司接口 Token 写入仓库。
- 不把真实供应商或公司系统完整响应直接写入用户可见错误。
- 不绕过 `available_actions` 自行在前端猜按钮。
- 不在生产环境使用 `X-Mock-User-Id` 作为可信用户身份。
- 不删除 DreamMaker 音乐或 Image 2 接口边界；临时公网供应商只能作为联调后端存在。
