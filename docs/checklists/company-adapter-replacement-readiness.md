# 公司 Adapter 替换 Readiness 清单

更新时间：2026-06-06

## 适用场景

本清单用于公司开发把本地 Mock Adapter 替换成真实公司系统前后走查。它不要求本平台实现公司社区、审核流、分享、互动或推荐流；只要求公司接入点、状态口径、验收证据和禁止事项清楚。

关联文档：

- `docs/handover/company-adapter-deployment-handoff-v0.1.md`
- `docs/specs/company-adapter-deployment-handoff-v0.1.md`
- `docs/checklists/local-commercial-delivery-acceptance.md`

## 1. Readiness 报告先行

启动 `music-api` 后：

```bash
curl -s http://localhost:8080/internal/integration-readiness | jq .
```

公司部署前必须确认：

- [ ] `overall_status` 对公司部署口径可解释。
- [ ] `company_account`、`company_moderation`、`company_quota`、`company_publish`、`company_share` 都不是未解释的本地 Mock 状态，或已有公司明确豁免说明。
- [ ] `company_share` 可以作为“公司系统承接 / 本平台无需实现”的豁免项；豁免说明必须写清分享入口、分享卡片、传播归因和回流统计由公司社区系统负责。
- [ ] `required_env_vars` 只列变量名，不输出任何真实密钥、token、Cookie 或签名 URL。
- [ ] `music_provider`、`workflow_dispatch`、`object_storage`、`render_worker` 的部署模式符合公司部署方案。

## 2. 替换矩阵

| 边界 | 当前接口 / 调用点 | 当前本地实现 | 公司替换验收 |
|---|---|---|---|
| 账号 | `AccountAdapter.getCurrentUser(String userId)` | `MockAccountAdapter` | 真实用户由公司网关、登录态、session 或可信 token 注入；生产不信任 `X-Mock-User-Id` |
| 审核 | `ModerationAdapter.preCheckUserInput` / `preCheckLyrics` / `preCheckPublishPackage` | `MockModerationAdapter` | 覆盖输入、歌词、发布包交接前审核；返回可映射 code 和用户可读 message |
| 权益 | `QuotaAdapter.getHint` / `lockGenerateQuota` / `commitGenerateQuota` / `releaseGenerateQuota` | `MockQuotaAdapter` | `lock / commit / release` 幂等，可按 `work_id` 对账 |
| 发布交接 | `PublishAdapter.preparePackage` / `refreshPackageUrl` | `MockPublishAdapter` + 对象存储 | 公司发布系统能读取发布包 URL；标记已交接不等于社区发布成功 |
| 分享 | 当前无本平台业务调用点 | `NotImplementedShareBoundary` 口径 | 分享入口、卡片、渠道、传播链路由公司社区系统承接 |

## 3. 账号 Adapter 验收

接口：

```java
UserProfile getCurrentUser(String userId);
record UserProfile(String userId, String nickname, String avatarUrl, List<String> roles) {}
```

验收：

- [ ] `/api/v1/me` 返回公司真实用户，而不是 `mock_user_001` / `MockUser`。
- [ ] 作品创建、查询、列表、发布包获取均按真实 `user_id` 隔离。
- [ ] 普通客户端伪造 `X-Mock-User-Id` 不会越权读取他人作品。
- [ ] 后端日志不打印公司登录 token、Cookie 或 session 原文。

证据：

| 项 | 结果 |
|---|---|
| 真实用户 ID 来源 |  |
| `/api/v1/me` 响应摘要 |  |
| 跨用户隔离测试 | 通过 / 失败 |
| Mock Header 是否失效 | 是 / 否 |

## 4. 审核 Adapter 验收

接口：

```java
ModerationDecision preCheckUserInput(String userId, String text);
ModerationDecision preCheckLyrics(String userId, String lyricsText);
ModerationDecision preCheckPublishPackage(String userId, String workId);
record ModerationDecision(boolean allowed, String code, String message) {}
```

验收：

- [ ] 用户输入阻断映射到 `USER_INPUT_BLOCKED`。
- [ ] 歌词审核阻断映射到 `LYRICS_PRECHECK_FAILED`。
- [ ] 发布包交接前阻断映射到 `PACKAGE_BLOCKED`。
- [ ] 用户侧只看到可读提示，不看到公司审核原始响应。
- [ ] 审核失败时，不继续调用下游真实模型或发布交接。

证据：

| 场景 | 预期 | 结果 |
|---|---|---|
| 输入阻断样本 | `USER_INPUT_BLOCKED` |  |
| 歌词阻断样本 | `LYRICS_PRECHECK_FAILED` |  |
| 发布包阻断样本 | `PACKAGE_BLOCKED` |  |
| 原始响应脱敏 | 不透出 |  |

## 5. 权益 Adapter 验收

接口：

```java
QuotaDecision getHint(String userId, int usedPolishCount);
QuotaLock lockGenerateQuota(String userId, String workId);
QuotaCommit commitGenerateQuota(String userId, String lockId);
QuotaRelease releaseGenerateQuota(String userId, String lockId, String reason);
```

验收：

- [ ] 确认出歌时锁定主权益。
- [ ] MP4 发布包进入 `PACKAGE_READY` 后扣减主权益。
- [ ] 音乐、封面、视频、发布包失败时按原因释放或保留可重试权益。
- [ ] `lock / commit / release` 重复调用不会重复扣费或重复释放。
- [ ] 公司权益流水可按 `work_id`、`lock_id` 和 `job_id` 对账。

幂等样本：

| 操作 | 重复调用预期 | 结果 |
|---|---|---|
| `lockGenerateQuota` | 返回同一锁或明确已锁状态 |  |
| `commitGenerateQuota` | 不重复扣减 |  |
| `releaseGenerateQuota` | 不重复释放 |  |

## 6. 发布交接 Adapter 验收

接口：

```java
PublishHandoff preparePackage(String workId);
PublishHandoff refreshPackageUrl(String workId);
record PublishHandoff(String packageObjectKey, String packageUrl, OffsetDateTime expiresAt) {}
```

验收：

- [ ] 发布包 URL 可由公司发布系统读取。
- [ ] URL 有效期符合公司系统拉取窗口。
- [ ] `refreshPackageUrl` 能刷新已过期 URL。
- [ ] `POST /publish-package/mark-fetched` 只表示“已交接给公司发布流程”，不表示社区发布成功。
- [ ] 发布包 JSON 不包含真实 AK/SK、JWT、Cookie、用户 token、供应商临时 URL。

证据：

| 项 | 结果 |
|---|---|
| 公司系统读取 package URL | 通过 / 失败 |
| URL TTL |  |
| 刷新 URL | 通过 / 失败 |
| 标记交接语义确认 | 已确认 / 待确认 |

## 7. 分享系统验收

当前本平台没有分享业务调用点，也不新增分享实现。公司系统需要确认：

- [ ] 分享入口由公司社区或移动端接管。
- [ ] 分享卡片素材使用发布包中的封面、标题、摘要或公司自有素材策略。
- [ ] 分享链路、传播归因、风控和回流统计由公司系统负责。
- [ ] 本平台不承接点赞、评论、收藏、推荐流和分享渠道配置。

判定口径：

- 如果公司确认分享完全由既有社区系统承接，`company_share` 可记录为“豁免 / 公司承接”，不需要本平台新增分享 Adapter。
- 如果公司要求本平台生成分享卡片或回流参数，应先新增接口规格和 PRD/技术方案变更，再进入实现。

## 8. 联合 Smoke

在公司 Adapter 替换后，至少执行：

```bash
scripts/smoke/openapi-contract.sh
EXPECTED_DURATION_MS=1000 scripts/smoke/api-main-flow.sh
cd prototypes/Claude-web-v1 && npm run smoke:real-backend
```

若启用真实 render-worker：

```bash
EXPECTED_DURATION_MS=1000 EXPECT_RENDER_WORKER=local-process scripts/smoke/api-main-flow.sh
```

如果启用真实音乐：

```text
按 docs/checklists/dreammaker-real-music-smoke-10min.md 单独执行，不和公司 Adapter 首次替换 smoke 混在一起。
```

## 9. 禁止事项

- [ ] 不在生产环境信任 `X-Mock-User-Id`。
- [ ] 不把公司真实 token、Cookie、AK/SK、JWT、签名 URL 写入仓库、日志、截图或提交信息。
- [ ] 不把公司审核或供应商原始响应直接展示给用户。
- [ ] 不把“标记已交接”当作“社区发布成功”。
- [ ] 不在本平台重复实现公司社区发布、分享、互动和推荐流。

## 10. 最终判定

| 项 | 状态 | 证据 |
|---|---|---|
| 账号真实接入 | 通过 / 阻塞 / 豁免 |  |
| 审核真实接入 | 通过 / 阻塞 / 豁免 |  |
| 权益真实接入 | 通过 / 阻塞 / 豁免 |  |
| 发布交接接入 | 通过 / 阻塞 / 豁免 |  |
| 分享系统承接 | 通过 / 阻塞 / 豁免 |  |
| readiness 报告 | 通过 / 阻塞 |  |
| smoke | 通过 / 阻塞 |  |
