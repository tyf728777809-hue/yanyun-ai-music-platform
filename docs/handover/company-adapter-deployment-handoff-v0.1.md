# 公司 Adapter 接入与部署交接说明 v0.1

更新时间：2026-06-06

## 1. 交接目标

本平台本地阶段负责跑通 AI 作曲与 MP4 成片链路。公司开发接入阶段负责把以下 Mock 边界替换为公司真实系统：

- 账号身份。
- 内容审核。
- 权益锁定、扣减、释放。
- 社区发布交接。
- 分享系统。

本平台不重做公司社区、发布审核流、分享系统、互动和推荐流。

视频成片当前支持两种模式：默认 `MockVideoRenderService`，以及本地 smoke 用的
`LocalProcessVideoRenderService`。公司部署前应在 `/internal/integration-readiness` 中检查
`render_worker` 组件；本地进程模式可证明 Remotion MP4 链路可跑通，但生产是否采用进程模式、独立服务或队列化 render worker 需要公司部署方案确认。

## 2. 当前可检查接口

启动 `music-api` 后访问：

```bash
curl http://localhost:8080/internal/integration-readiness
```

该接口只读取配置和静态边界，不调用真实公司系统、不调用真实模型供应商、不输出密钥值。公司部署前需要重点检查：

- `overall_status`
- 每个 `components[].status`
- 每个 `components[].blocks_company_deployment`
- 每个 `components[].required_env_vars`

本地开发环境允许 `MOCK_ONLY`；公司部署前，所有公司系统相关 `blocks_company_deployment=true` 项必须替换真实实现或由公司明确豁免。

## 3. Adapter 替换清单

| 边界 | 当前接口 | 当前实现 | 公司接入要求 |
|---|---|---|---|
| 账号 | `modules/auth/AccountAdapter` | `MockAccountAdapter` | 用公司账号态或网关注入真实用户，替换 `X-Mock-User-Id` |
| 审核 | `modules/moderation/ModerationAdapter` | `MockModerationAdapter` | 覆盖输入、歌词和发布包交接前审核 |
| 权益 | `modules/quota/QuotaAdapter` | `MockQuotaAdapter` | 实现生成权益 lock / commit / release 幂等语义 |
| 发布交接 | `modules/publish/PublishAdapter` | `MockPublishAdapter` | 接公司发布系统需要的交接元数据；社区发布结果仍由公司系统管理 |
| 分享 | 当前无业务调用点 | `NotImplementedShareBoundary` | 分享入口、分享卡片、传播链路由公司系统接管 |

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

公司部署至少需要确认：

```text
YANYUN_ENV=
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
MUSIC_PROVIDER=suno|minimax
DREAMMAKER_REAL_CALLS_ENABLED=true
DREAMMAKER_API_BASE_URL=
DREAMMAKER_ACCESS_KEY=
DREAMMAKER_SECRET_KEY=
RENDER_WORKER_MODE=local-process
RENDER_WORKER_WORKING_DIRECTORY=
RENDER_WORKER_COMMAND=
RENDER_WORKER_ARGUMENTS=
RENDER_WORKER_TIMEOUT=
COMPANY_ACCOUNT_ADAPTER_MODE=company
COMPANY_MODERATION_ADAPTER_MODE=company
COMPANY_QUOTA_ADAPTER_MODE=company
COMPANY_PUBLISH_ADAPTER_MODE=company
COMPANY_SHARE_ADAPTER_MODE=company
```

真实值必须由公司安全配置系统或部署平台注入，不得写入仓库、镜像、日志或测试快照。

## 9. 公司接入 Smoke

1. 调用 `/internal/integration-readiness`，确认公司 Adapter 不再处于未确认的 Mock 状态。
2. 调用 `/api/v1/me`，确认返回真实公司用户。
3. 灵感成歌创建作品，确认作品归属到真实 `user_id`。
4. 输入一条公司审核应阻断的内容，确认返回可读失败提示。
5. 正常作品确认出歌，确认权益先锁定，发布包可用后扣减。
6. 触发一次音乐失败或模拟失败，确认权益释放或重试口径正确。
7. 作品生成到 `PACKAGE_READY` 后，确认公司发布系统能读取发布包 URL。
8. 调用标记已交接接口，确认公司侧不把它误解为“已发布成功”。

## 10. 禁止事项

- 不在本平台内重做公司社区系统。
- 不把真实 AccessKey、SecretKey、Cookie、JWT、公司接口 Token 写入仓库。
- 不把真实供应商或公司系统完整响应直接写入用户可见错误。
- 不绕过 `available_actions` 自行在前端猜按钮。
- 不在生产环境使用 `X-Mock-User-Id` 作为可信用户身份。
