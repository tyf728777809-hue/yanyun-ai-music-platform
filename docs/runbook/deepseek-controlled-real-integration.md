# DeepSeek 需求理解与写词受控真实联调 Runbook

## 目标

本 Runbook 用于后续手动联调真实 DeepSeek 链路。目标是在只打开 DeepSeek 的前提下，验证用户需求理解、写词、润色和续写可以受控调用真实模型，并且不影响音乐、封面、视频和公司系统的 Mock 边界。

当前代码状态：截至 2026-06-06，`DeepSeekLyricsClient` 默认注册 `MockDeepSeekLyricsClient`；当 `DEEPSEEK_REAL_CALLS_ENABLED=true` 时注册 `RealDeepSeekLyricsClient`。真实客户端使用 OpenAI 兼容 `POST /chat/completions`，默认 `DEEPSEEK_BASE_URL=https://api.deepseek.com`、`DEEPSEEK_MODEL_NAME=deepseek-v4-pro`，并要求 JSON object 输出。

## 硬性规则

- 默认禁止真实 DeepSeek 请求：`DEEPSEEK_REAL_CALLS_ENABLED=false`。
- 只有手动联调窗口内才允许设置 `AGENT_REAL_CALLS_ENABLED=true` 和 `DEEPSEEK_REAL_CALLS_ENABLED=true`。
- 单次联调只打开 DeepSeek；`MUSIC_PROVIDER=mock`、`DREAMMAKER_REAL_CALLS_ENABLED=false`、`IMAGE_PROVIDER=mock`、公司 Adapter 继续 Mock。
- 不把 API Key、Bearer token、鉴权 header、完整 Prompt、完整模型请求/响应、用户敏感输入写入仓库、文档、测试、截图、日志或提交信息。
- 自动化测试仍只允许 Mock/Fake，不调用真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统。
- 每轮真实样本必须有数量上限、成本止损、回滚命令和证据记录。

## 前置检查

```bash
git status --short
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :modules:deepseek:test :modules:lyrics:test :modules:creative-agent:test
docker compose -f deploy/docker-compose.yml --env-file .env.example up -d
docker compose -f deploy/docker-compose.yml ps
```

确认 PostgreSQL、Redis、Temporal、MinIO 等本地服务健康后再继续。

## 凭据注入

只在当前终端会话或公司 Secret 系统注入，禁止写入 `.env.example` 或任何提交文件：

```bash
export AGENT_REAL_CALLS_ENABLED=true
export DEEPSEEK_REAL_CALLS_ENABLED=true
export DEEPSEEK_BASE_URL="https://api.deepseek.com"
export DEEPSEEK_API_KEY="<from-secure-channel>"
export DEEPSEEK_MODEL_NAME="deepseek-v4-pro"
export DEEPSEEK_TIMEOUT_MS=30000
export DEEPSEEK_MAX_ATTEMPTS=1
export DEEPSEEK_RESPONSE_MAX_TOKENS=1800
export DEEPSEEK_TEMPERATURE=0.7
```

联调结束后执行：

```bash
unset DEEPSEEK_BASE_URL DEEPSEEK_API_KEY DEEPSEEK_MODEL_NAME
unset AGENT_REAL_CALLS_ENABLED DEEPSEEK_REAL_CALLS_ENABLED
unset DEEPSEEK_TIMEOUT_MS DEEPSEEK_MAX_ATTEMPTS DEEPSEEK_RESPONSE_MAX_TOKENS DEEPSEEK_TEMPERATURE
```

## 推荐启动方式

DeepSeek 写词当前发生在 API 进程的创建、润色和续写请求中，因此 API 进程必须拿到 DeepSeek 环境变量。音乐、封面、视频和公司系统继续保持 Mock：

真实调用前先从统一总入口查看计划，再做只读预检。以下命令只检查当前 shell 变量，不调用 DeepSeek、DreamMaker、Yunwu、WellAPI 或公司系统：

```bash
TARGET=deepseek MODE=plan scripts/smoke/real-model-controlled-smoke.sh
TARGET=deepseek MODE=preflight scripts/smoke/real-model-controlled-smoke.sh
```

```bash
AGENT_REAL_CALLS_ENABLED=true \
DEEPSEEK_REAL_CALLS_ENABLED=true \
MUSIC_PROVIDER=mock \
DREAMMAKER_REAL_CALLS_ENABLED=false \
RENDER_WORKER_MODE=mock \
COMPANY_ACCOUNT_ADAPTER_MODE=mock \
COMPANY_MODERATION_ADAPTER_MODE=mock \
COMPANY_QUOTA_ADAPTER_MODE=mock \
COMPANY_PUBLISH_ADAPTER_MODE=mock \
COMPANY_SHARE_ADAPTER_MODE=mock \
  ./gradlew :apps:music-api:bootRun
```

如果后续把 DeepSeek Agent 拆到独立 worker 或 Temporal activity，真实环境变量必须注入到实际发起模型请求的进程，而不是只注入 API。

## 脚本化单样本 smoke

API 已按上面方式启动并通过只读预检后，可执行 1 条真实写词样本。该脚本只调用 DeepSeek 写词链路，不确认出歌，不生成音乐、封面、视频或发布包，也不调用 DreamMaker、Yunwu、WellAPI 或公司系统：

```bash
ALLOW_REAL_MODEL_SMOKE=1 \
ALLOW_DEEPSEEK_REAL_SMOKE=1 \
TARGET=deepseek \
MODE=execute \
scripts/smoke/real-model-controlled-smoke.sh
```

低层入口为：

```bash
ALLOW_DEEPSEEK_REAL_SMOKE=1 scripts/smoke/deepseek-real-lyrics-smoke.sh
```

优先使用统一总入口，因为它会先跑严格只读预检。两个入口都要求音乐、封面、DreamMaker、Yunwu、WellAPI 和公司 Adapter 保持 Mock 或关闭。DreamMaker 音乐与 DreamMaker Image 2 仍是正式生产目标接口，本 smoke 不替代它们。

## 联调步骤

### 1. 基础健康检查

```bash
curl http://localhost:8080/health
curl http://localhost:8080/internal/integration-readiness
```

期望：

- API 可用。
- readiness 能看出 DeepSeek 真实调用开关状态。
- DreamMaker、Image 2、公司 Adapter 仍处于 Mock 或关闭状态。

### 2. 灵感成歌

1. 调用 `POST /api/v1/works/inspiration`，请求头包含 `X-Mock-User-Id` 和唯一 `Idempotency-Key`。
2. 查询 `GET /api/v1/works/{work_id}`。
3. 确认作品进入 `LYRICS_READY / WAITING_CONFIRM`，并有标题、摘要、歌词、music prompt、燕云引用。
4. 抽查 `agent_runs`：

```bash
docker exec yanyun-postgres psql -U postgres -d yanyun_music -Atc \
  "select agent_name, agent_version, operation, model_name, status, input_hash is not null, output_hash is not null, error_code from agent_runs where work_id = '{work_id}' order by started_at"
```

期望能看到真实模型名，且只记录 hash、版本、状态和脱敏失败信息。

### 3. 填词成歌

重复灵感成歌步骤，但调用 `POST /api/v1/works/lyrics`。期望用户输入歌词仍是主要歌词文本，真实模型只补齐标题、摘要、music prompt、封面 seed 和必要修饰。

### 4. AI 润色与续写

1. 对同一作品调用 `POST /api/v1/works/{work_id}/lyrics/polish`。
2. 再调用 `POST /api/v1/works/{work_id}/lyrics/continue`。
3. 第 3 次改词请求应仍返回 HTTP 409，并显示友好错误。
4. 抽查 `polish_remaining_count` 与 `agent_runs`。

期望用户侧 2 次改词限制不变；真实模型失败不得静默消耗次数。

### 5. 确认出歌到发布包

调用 `POST /api/v1/works/{work_id}/confirm`，保持 `music_provider=mock` 或不传 Provider。轮询作品直到 `GENERATED / PACKAGE_READY`，再调用 `GET /api/v1/works/{work_id}/publish-package`。

期望：

- DeepSeek 只影响歌词和相关提示词。
- 音频、封面、视频、发布包仍由本地 Mock 或本地 render-worker 边界生成。
- 发布包 URL 不包含 DeepSeek 凭据或供应商原始响应。

## 失败处理

- `DEEPSEEK_REAL_CALLS_ENABLED=false` 导致失败：这是保护机制，确认是否真的进入手动联调窗口。
- 缺失 `DEEPSEEK_API_KEY`、模型名或总开关未打开：必须在外部 HTTP 请求前失败，不得发出真实请求。
- HTTP 401 / 403：停止继续联调，确认密钥权限，不要重复试错。
- HTTP 429 或限流：停止触发新样本，记录脱敏错误码和时间窗口。
- 超时：记录 operation、模型名、timeout 配置和 `work_id`，回退 Mock。
- 模型输出无法解析：记录模板版本、输出 hash、错误码，不记录完整输出。
- 任何异常都不要粘贴完整请求、完整响应、鉴权 header 或用户敏感输入。

## 快速回滚

```bash
unset DEEPSEEK_BASE_URL DEEPSEEK_API_KEY DEEPSEEK_MODEL_NAME
unset AGENT_REAL_CALLS_ENABLED DEEPSEEK_REAL_CALLS_ENABLED
export MUSIC_PROVIDER=mock
export DREAMMAKER_REAL_CALLS_ENABLED=false
```

必要时停止 API 进程，确认本地主链路回到 Mock：

```bash
EXPECTED_DURATION_MS=1000 scripts/smoke/api-main-flow.sh
```

## 证据记录

每次联调只记录以下字段，不记录真实密钥、鉴权 header、完整 Prompt、完整模型请求/响应或用户敏感输入：

| Field | Value |
|---|---|
| 时间 |  |
| DeepSeek 模型 |  |
| Operation | `INSPIRATION` / `LYRICS` / `POLISH` / `CONTINUE` |
| `work_id` |  |
| Prompt 模板版本 |  |
| 输入 hash |  |
| 输出 hash |  |
| `agent_runs.status` |  |
| 耗时 |  |
| 失败码 |  |
| 是否回退 Mock | 是 / 否 |
| 结论 | 通过 / 待修复 |

## 进入真实联调前仍需确认

- DeepSeek API Key 或公司 Secret 注入方式。
- 本轮联调样本数量、成本止损和回滚窗口。
- 限流、超时、重试、计费和内容安全要求。
- 失败码样本和 retryable / non-retryable 口径。
