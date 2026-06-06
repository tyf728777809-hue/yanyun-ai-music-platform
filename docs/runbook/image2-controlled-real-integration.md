# Image 2 封面受控真实联调 Runbook

## 目标

本 Runbook 用于后续手动联调真实 Image 2 封面生成链路。目标是在只打开 Image 2 生图 Provider 的前提下，验证 `CoverPromptAgent` 输出的视觉提示词可以生成 16:9 封面，并导入平台对象存储、进入媒体资产和发布包。

当前代码状态：`CoverGenerationService` 默认注册 `MockCoverGenerationService`；当 `IMAGE_PROVIDER=image2` 或 `IMAGE_REAL_CALLS_ENABLED=true` 时按 `IMAGE2_BACKEND=wellapi|dreammaker` 注册真实适配器。当前非内网公网联调默认使用 `WellApiImage2CoverGenerationService`，调用 `POST /v1/images/generations` 的 `gpt-image-2`；DreamMaker Image 2 接口仍保留为正式生产目标切换路径。

## 硬性规则

- 默认禁止真实 Image 2 请求：`IMAGE_PROVIDER=mock` 且 `IMAGE_REAL_CALLS_ENABLED=false`。
- 只有手动联调窗口内才允许设置 `IMAGE_PROVIDER=image2` 和 `IMAGE_REAL_CALLS_ENABLED=true`。
- 单次联调只打开 Image 2；DeepSeek、Suno、MiniMax 和公司 Adapter 继续 Mock 或关闭。若 `IMAGE2_BACKEND=wellapi`，不需要 DreamMaker AK/SK；若 `IMAGE2_BACKEND=dreammaker`，才需要同时打开 DreamMaker 真实开关。
- 不把 API Key、鉴权 header、完整图像生成 Prompt、完整请求/响应、供应商临时图片 URL 写入仓库、文档、测试、截图、日志或提交信息。
- 自动化测试仍只允许 Mock/Fake，不调用真实 Image 2、DeepSeek、Suno、MiniMax 或公司系统。
- Image 2 是 Provider Adapter；`CoverPromptAgent` 只负责规划视觉 prompt，不直接调用生图服务。

## 前置检查

```bash
git status --short
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :modules:image2:test :modules:creative-agent:test :apps:music-api:test --tests '*MockSongProductionWorkflow*'
docker compose -f deploy/docker-compose.yml --env-file .env.example up -d
docker compose -f deploy/docker-compose.yml ps
```

确认 PostgreSQL、MinIO/S3、Redis 等本地服务健康后再继续。

## 凭据注入

只在当前终端会话或公司 Secret 系统注入，禁止写入 `.env.example` 或任何提交文件：

```bash
export IMAGE_PROVIDER=image2
export IMAGE2_BACKEND=wellapi
export IMAGE_REAL_CALLS_ENABLED=true
export WELLAPI_BASE_URL=https://wellapi.ai
export WELLAPI_API_KEY="<from-secure-channel>"
export IMAGE2_MODEL_NAME=gpt-image-2
export IMAGE2_SIZE=2048x1152
export IMAGE2_QUALITY=medium
export IMAGE2_OUTPUT_FORMAT=jpeg
```

联调结束后执行：

```bash
unset WELLAPI_API_KEY
unset IMAGE2_MODEL_NAME IMAGE2_SIZE IMAGE2_QUALITY IMAGE2_OUTPUT_FORMAT
unset IMAGE_REAL_CALLS_ENABLED IMAGE_PROVIDER
```

## 推荐启动方式

Image 2 封面生成发生在确认出歌后的生产 workflow 中。真实联调建议仍保持音乐和 DeepSeek Mock，避免一次打开多个外部成本点：

真实调用前先从统一总入口查看目标矩阵和执行路线，再做只读预检。总入口会标明 WellAPI 只是当前公网受控 smoke 路径，DreamMaker Image 2 仍是生产目标；底层 `real-model-readiness-preflight.sh` 只作为总入口调用的实现细节，不建议交接方直接绕过总入口。

```bash
TARGET=wellapi-image2 MODE=plan scripts/smoke/real-model-controlled-smoke.sh
TARGET=wellapi-image2 MODE=preflight scripts/smoke/real-model-controlled-smoke.sh
TARGET=dreammaker-image2 MODE=plan scripts/smoke/real-model-controlled-smoke.sh
TARGET=dreammaker-image2 MODE=preflight scripts/smoke/real-model-controlled-smoke.sh
```

如果本地 8080 没有已启动 API，优先使用一键 stack smoke。脚本会静默读取缺失的 `WELLAPI_API_KEY`、启动 API、执行 1 个作品、结束后自动停止它启动的进程：

```bash
ALLOW_WELLAPI_IMAGE2_REAL_SMOKE=1 \
scripts/smoke/wellapi-image2-real-cover-stack-smoke.sh
```

如果 API 已经手动按本 runbook 启动，使用低层单作品 smoke：

```bash
ALLOW_WELLAPI_IMAGE2_REAL_SMOKE=1 \
IMAGE_PROVIDER=image2 \
IMAGE2_BACKEND=wellapi \
IMAGE_REAL_CALLS_ENABLED=true \
MUSIC_PROVIDER=mock \
scripts/smoke/wellapi-image2-real-cover-smoke.sh
```

两个脚本都会真实调用 WellAPI；不要作为普通自动化测试运行。当前 workflow 的封面请求尺寸固定为 1920x1080，`IMAGE2_SIZE=2048x1152` 只是 Image 2 客户端在直接请求尺寸无效时使用的兜底值。

若要验证正式生产目标 DreamMaker Image 2 路径，先切换凭据和后端：

```bash
export IMAGE_PROVIDER=image2
export IMAGE2_BACKEND=dreammaker
export IMAGE_REAL_CALLS_ENABLED=true
export DREAMMAKER_REAL_CALLS_ENABLED=true
export DREAMMAKER_API_BASE_URL=https://api-all.dreammaker.netease.com
export DREAMMAKER_ACCESS_KEY="<from-secure-channel>"
export DREAMMAKER_SECRET_KEY="<from-secure-channel>"
export IMAGE2_MODEL_NAME=gpt-image-2
```

只读预检：

```bash
TARGET=dreammaker-image2 MODE=preflight scripts/smoke/real-model-controlled-smoke.sh
```

一键 stack smoke：

```bash
ALLOW_REAL_MODEL_SMOKE=1 \
ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE=1 \
TARGET=dreammaker-image2 \
MODE=execute \
scripts/smoke/real-model-controlled-smoke.sh
```

API 已手动启动时可使用低层脚本：

```bash
ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE=1 \
IMAGE_PROVIDER=image2 \
IMAGE2_BACKEND=dreammaker \
IMAGE_REAL_CALLS_ENABLED=true \
DREAMMAKER_REAL_CALLS_ENABLED=true \
MUSIC_PROVIDER=mock \
scripts/smoke/dreammaker-image2-real-cover-smoke.sh
```

DreamMaker Image 2 脚本会真实调用 DreamMaker；不要作为普通自动化测试运行。Yunwu 和 WellAPI 的公网 smoke 成功不代表 DreamMaker 生产路径完成。

```bash
IMAGE_PROVIDER=image2 \
IMAGE2_BACKEND=wellapi \
IMAGE_REAL_CALLS_ENABLED=true \
WELLAPI_BASE_URL=https://wellapi.ai \
MUSIC_PROVIDER=mock \
DEEPSEEK_REAL_CALLS_ENABLED=false \
COMPANY_ACCOUNT_ADAPTER_MODE=mock \
COMPANY_MODERATION_ADAPTER_MODE=mock \
COMPANY_QUOTA_ADAPTER_MODE=mock \
COMPANY_PUBLISH_ADAPTER_MODE=mock \
COMPANY_SHARE_ADAPTER_MODE=mock \
  ./gradlew :apps:music-api:bootRun
```

如果后续把封面生成拆到 worker 或 Temporal activity，真实环境变量必须注入到实际发起 Image 2 请求的进程。

## 联调步骤

### 1. 基础健康检查

```bash
curl http://localhost:8080/health
curl http://localhost:8080/internal/integration-readiness
```

期望：

- API 可用。
- readiness 能看出 Image 2 真实调用开关状态。
- DeepSeek、音乐 Provider、公司 Adapter 仍处于 Mock 或关闭状态。

### 2. 创建作品并确认出歌

1. 调用 `POST /api/v1/works/lyrics` 或 `POST /api/v1/works/inspiration`，拿到 `work_id`。
2. 查询 `GET /api/v1/works/{work_id}`，拿到 `lyrics_draft_id`。
3. 调用 `POST /api/v1/works/{work_id}/confirm`，保持 `music_provider=mock` 或不传 Provider。
4. 轮询作品直到 `GENERATED / PACKAGE_READY` 或进入失败状态。

### 3. 抽查封面资产

```bash
docker exec yanyun-postgres psql -U postgres -d yanyun_music -Atc \
  "select asset_type, provider, object_key, mime_type, width, height, metadata_json from media_assets where work_id = '{work_id}' and asset_type = 'COVER'"
```

期望：

- `asset_type=COVER`。
- `provider` 或 metadata 能区分 `wellapi-image2` 或 `dreammaker-image2`。
- `width=1920`、`height=1080`。
- `metadata_json.object_storage_imported=true`。
- `object_key` 指向平台对象存储，不是供应商原始 URL。
- metadata 不包含 API Key、鉴权 header、完整供应商 payload、供应商原始 URL 或 inline base64 原文。

### 4. 验证发布包

调用 `GET /api/v1/works/{work_id}/publish-package`，确认：

- `cover.url` 可访问。
- 发布包只暴露平台签名 URL。
- `video.url`、`lyrics.timeline_url` 仍正常。
- 成品页可使用 `media_assets.cover_url` 展示封面。

## 失败处理

- `IMAGE_REAL_CALLS_ENABLED=false` 导致失败：这是保护机制，确认是否真的进入手动联调窗口。
- 缺失 `WELLAPI_API_KEY`、`WELLAPI_BASE_URL` 或未设置 `IMAGE_REAL_CALLS_ENABLED=true`：必须在外部 HTTP 请求前失败，不得发出真实请求。
- 若切 `IMAGE2_BACKEND=dreammaker`，缺失 DreamMaker AK/SK 或未打开 `DREAMMAKER_REAL_CALLS_ENABLED=true` 也必须在外部 HTTP 请求前失败。
- HTTP 401 / 403：停止继续联调，确认密钥权限，不要重复试错。
- HTTP 429 或限流：停止触发新样本，记录脱敏错误码和时间窗口。
- 生成内容违规或被供应商阻断：记录脱敏错误码，确认是否需要调整 `CoverPromptAgent` 的 negative prompt 或安全约束。
- 供应商图片 URL 下载失败：记录 `work_id`、供应商任务 id、平台错误码，确认 URL 是否过期或需要鉴权。
- 供应商只返回 `b64_json`：当前 workflow 会直接写入平台对象存储，metadata 不保留 base64 原文。
- 任何异常都不要粘贴完整请求、完整响应、鉴权 header、供应商原始图片 URL 或用户敏感输入。

## 默认封面兜底

真实 Image 2 阶段仍需补默认封面兜底策略：

- 供应商失败但音乐和视频可继续时，优先使用平台默认 16:9 封面。
- 若默认封面不可用，才按 `PACKAGE_BUILD_FAILED` 收口。
- 兜底封面也必须进入 `media_assets` 和发布包，不得使用前端临时占位图冒充平台资产。

## 快速回滚

```bash
unset WELLAPI_API_KEY
unset DREAMMAKER_ACCESS_KEY DREAMMAKER_SECRET_KEY DREAMMAKER_USER_ACCESS_TOKEN
unset IMAGE2_MODEL_NAME IMAGE2_SIZE IMAGE2_QUALITY IMAGE2_OUTPUT_FORMAT
unset IMAGE_REAL_CALLS_ENABLED IMAGE_PROVIDER
export IMAGE_PROVIDER=mock
```

必要时停止 API 进程，确认本地主链路回到 Mock：

```bash
EXPECTED_DURATION_MS=1000 scripts/smoke/api-main-flow.sh
```

## 证据记录

每次联调只记录以下字段，不记录真实密钥、鉴权 header、完整 Prompt、完整供应商请求/响应或供应商原始图片 URL：

| Field | Value |
|---|---|
| 时间 |  |
| Image 2 模型 |  |
| `work_id` |  |
| 封面尺寸 |  |
| 平台 `cover.object_key` |  |
| 供应商 task id |  |
| 对象存储导入 | 成功 / 失败 |
| `works.status` |  |
| `package_status` |  |
| 失败码 |  |
| 是否使用默认封面兜底 | 是 / 否 |
| 结论 | 通过 / 待修复 |

## 进入真实联调前仍需确认

- WellAPI API Key 或公司 Secret 注入方式；若切回生产目标 DreamMaker，还需 DreamMaker AK/SK 注入方式。
- 本轮联调样本数量、成本止损和回滚窗口。
- 图片尺寸、格式、风格、安全词和输出 URL TTL。
- 限流、超时、重试、计费和内容安全要求。
- 失败码样本和 retryable / non-retryable 口径。
