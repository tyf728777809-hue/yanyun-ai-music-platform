# OpenAPI v0.1 契约对拍 Smoke 规格

作者：Codex
日期：2026-06-06
状态：Approved
评审者：N/A - 当前为本地交付前自检规格

## Context

当前项目已经具备 `docs/api/openapi-v0.1.yaml`、后端 Mock 主链路、Claude Web v1 前端真实后端 smoke、以及本地 MP4 成片 smoke。交付走查中仍有一项证据不足：OpenAPI v0.1 与当前后端响应是否能被自动对拍。

本规格用于新增一个轻量级运行时契约 smoke。它不替代完整 OpenAPI JSON Schema 测试框架，但必须覆盖公司接入和前端最依赖的接口字段、枚举状态、统一错误响应、幂等冲突、发布包交接和失败重试。脚本作为本地交付前的可重复验证命令，默认不调用真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统。

## Functional Requirements

- FR-1：脚本 MUST 静态解析 `docs/api/openapi-v0.1.yaml`，确认关键 path、operationId、schema required 字段和枚举存在。
- FR-2：脚本 MUST 调用本地 API，验证 `GET /api/v1/me` 返回 `UserProfile` 必填字段。
- FR-3：脚本 MUST 覆盖 `POST /api/v1/works/inspiration` 和 `POST /api/v1/works/lyrics` 的 `CreateWorkResponse` 必填字段。
- FR-4：脚本 MUST 覆盖 `GET /api/v1/works/{work_id}` 的 `WorkDetail` 必填字段、歌词草案、权益提示、发布交接提示、可用动作和状态枚举。
- FR-5：脚本 MUST 覆盖歌词润色、续写和第三次编辑 409 错误响应。
- FR-6：脚本 MUST 覆盖确认出歌、封面重生、视频重渲、作品列表、发布包获取、刷新 URL 和标记已交接。
- FR-7：脚本 MUST 覆盖统一 `ErrorResponse`，至少包含缺失 `Idempotency-Key`、幂等冲突、作品不存在和受控音乐失败。
- FR-8：脚本 MUST 覆盖音乐失败作品详情中的 `failure.failure_code`、重试次数、`recommended_action` 和 `RETRY_MUSIC`，并验证可用 `mock` 重试恢复。
- FR-9：脚本 MUST 仅使用本地 Mock/受控失败路径，不触发真实模型或公司系统。

## Non-Functional Requirements

- NFR-1：脚本 SHOULD 在本地 sync Mock API 下 30 秒内完成。
- NFR-2：脚本 MUST 不写入真实密钥、JWT、Cookie、用户 token 或供应商 payload。
- NFR-3：脚本 MUST 输出明确 PASS/ERROR 和关键 `work_id`，便于写入进度记录。
- NFR-4：脚本 MUST 只依赖常见命令行工具 `curl`、`jq`、`ruby`，不新增生产依赖。

## Acceptance Criteria

- AC-1：Given API 已以 `MUSIC_PROVIDER=mock` 和 sync 模式启动，When 运行 `scripts/smoke/openapi-contract.sh`，Then 静态 OpenAPI 检查和运行时契约检查全部通过。覆盖 FR-1 至 FR-8。
- AC-2：Given POST 请求缺少 `Idempotency-Key`，When 脚本调用创建作品接口，Then 返回 HTTP 400 且响应包含 `error.code`、`error.message`、`error.request_id`、`error.timestamp`。覆盖 FR-7。
- AC-3：Given 同一用户、同一 operation、同一幂等键但请求体不同，When 脚本重复创建作品，Then 返回 HTTP 409 且 `error.code=IDEMPOTENCY_CONFLICT`。覆盖 FR-7。
- AC-4：Given 作品已使用两次 AI 编辑，When 脚本第三次调用润色接口，Then 返回 HTTP 409 统一错误响应。覆盖 FR-5、FR-7。
- AC-5：Given 作品确认出歌完成，When 脚本获取发布包并刷新 URL、标记已交接，Then 响应满足 `PublishPackage` required 字段，最终 `package_status=PACKAGE_FETCHED`。覆盖 FR-6。
- AC-6：Given `music_provider=suno` 在真实调用硬开关关闭时触发受控失败，When 脚本查询作品详情，Then `failure.failure_code=MUSIC_GENERATION_FAILED` 且可用 `RETRY_MUSIC`；When 用 `mock` 重试，Then 恢复到 `GENERATED / PACKAGE_READY`。覆盖 FR-8、FR-9。

## Edge Cases

- EC-1：API 未启动时，脚本 MUST 以可读错误退出。
- EC-2：OpenAPI YAML 不能解析或关键契约缺失时，脚本 MUST 在发起业务请求前失败。
- EC-3：返回字段缺失或枚举值不在 OpenAPI 允许范围内时，脚本 MUST 失败并输出对应响应摘要。
- EC-4：脚本运行结束后不负责停止 API，因为 API 由调用方启动。

## API Contracts

运行时检查覆盖以下 OpenAPI schema：

```ts
type RuntimeContractSchemas =
  | "UserProfile"
  | "CreateWorkResponse"
  | "JobAcceptedResponse"
  | "WorkListResponse"
  | "WorkSummary"
  | "WorkDetail"
  | "LyricsDraft"
  | "MediaAssets"
  | "QuotaHint"
  | "FailureInfo"
  | "PublishHandoffHint"
  | "PublishPackage"
  | "PublishPackageJson"
  | "ErrorResponse";
```

## Data Models

| 实体 | 来源 | 对拍内容 |
|---|---|---|
| Work | `works` API 响应 | `work_id`、`work_code`、`status`、`generation_stage`、`package_status`、可用动作 |
| LyricsDraft | `WorkDetail.lyrics_draft` | 版本号、歌词正文、音乐 prompt、燕云引用 |
| PublishPackage | 发布包接口 | URL、过期时间、视频、封面、歌词、交接状态 |
| FailureInfo | 失败作品详情 | 失败码、失败文案、重试次数、推荐动作 |
| ErrorResponse | 统一错误响应 | code、message、request_id、timestamp、details |

## Out of Scope

- OS-1：不实现完整 JSON Schema validator。
- OS-2：不验证真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统。
- OS-3：不覆盖生产鉴权、限流、安全扫描或性能压测。
- OS-4：不替代 `scripts/smoke/api-main-flow.sh` 的对象存储、数据库和 MP4 文件检查。
