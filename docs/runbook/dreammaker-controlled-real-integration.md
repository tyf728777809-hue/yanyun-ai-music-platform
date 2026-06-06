# DreamMaker Suno / MiniMax 受控真实联调 Runbook

## 目标

本 Runbook 用于第 5 批手动联调 Suno 与 MiniMax 的真实 DreamMaker 链路。目标是证明真实 Provider 可以在本地完整走到平台对象存储、作品状态和发布包交接；同时避免误触真实请求、泄露密钥或让同步 API 线程长时间阻塞。

如果只需要一次最短路径 smoke，先按 `docs/checklists/dreammaker-real-music-smoke-10min.md` 执行；也可以用 `scripts/smoke/dreammaker-real-music-stack-smoke.sh` 一次性启动 worker/API、执行单作品 smoke 并自动清理进程。若 worker/API 已手动启动，则用 `scripts/smoke/dreammaker-real-music-smoke.sh` 执行脚本化单作品 smoke。真实调用前，可先用 `scripts/smoke/dreammaker-real-guard-smoke.sh` 验证默认 `sync` 模式会被运行时 guard 拒绝，且不会触发 DreamMaker。本 Runbook 负责解释完整风险、回滚和证据记录口径。

## 硬性规则

- 默认禁止真实外部请求：`DREAMMAKER_REAL_CALLS_ENABLED=false`。
- 只有手动联调窗口内才允许设置 `DREAMMAKER_REAL_CALLS_ENABLED=true`。
- 真实联调优先使用 `MUSIC_WORKFLOW_DISPATCH_MODE=outbox` + `WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal`。
- 不使用默认 `sync` 模式联调真实 Provider；Suno/MiniMax 轮询可能持续数分钟，会阻塞 API 请求线程。
- 运行时已加硬保护：当 `DREAMMAKER_REAL_CALLS_ENABLED=true` 且 Provider 为 `suno` / `minimax` 时，API 只允许 `outbox + temporal`，否则确认出歌或重试会直接返回冲突。
- 不把 AccessKey、SecretKey、JWT、`X-Access-Token`、供应商原始 payload 写入仓库、文档、测试、截图或提交信息。
- 自动化测试仍只允许 fake/mock，不调用真实 DreamMaker。

## 前置检查

```bash
git status --short
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :modules:dreammaker:test :modules:suno:test :modules:minimax:test
docker compose -f deploy/docker-compose.yml --env-file .env.example up -d
docker compose -f deploy/docker-compose.yml ps
```

确认 PostgreSQL、Temporal、MinIO 等本地服务健康后再继续。

## 凭据注入

只在当前终端会话注入，禁止写入 `.env.example` 或任何提交文件：

```bash
export DREAMMAKER_API_BASE_URL=https://api-all.dreammaker.netease.com
export DREAMMAKER_ACCESS_KEY="<from-secure-channel>"
export DREAMMAKER_SECRET_KEY="<from-secure-channel>"
export DREAMMAKER_REAL_CALLS_ENABLED=true
# 可选，只有公司需要按真实用户归因时设置：
export DREAMMAKER_USER_ACCESS_TOKEN="<from-secure-channel>"
```

联调结束后执行：

```bash
unset DREAMMAKER_ACCESS_KEY DREAMMAKER_SECRET_KEY DREAMMAKER_USER_ACCESS_TOKEN
export DREAMMAKER_REAL_CALLS_ENABLED=false
```

## 推荐启动方式

真实调用前先做只读预检。该命令只检查当前 shell 变量，不调用 DreamMaker、Yunwu、WellAPI、DeepSeek 或公司系统：

```bash
TARGET=dreammaker-suno STRICT=true \
scripts/smoke/real-model-readiness-preflight.sh
```

如需测试 MiniMax，把 `TARGET` 改成 `dreammaker-minimax`。预检通过不代表真实供应商成功，只代表当前环境变量和真实音乐 dispatch 口径适合进入受控 smoke。

### 一键 Stack Smoke

首次只跑 1 个 Suno 作品时，优先使用 stack smoke。脚本会静默读取 AK/SK、拒绝占用的 8080/8081 端口、启动 worker/API、执行单作品真实 Provider smoke，并在成功、失败或中断时停止它自己启动的进程。

```bash
ALLOW_DREAMMAKER_REAL_SMOKE=1 \
REAL_PROVIDER=suno \
scripts/smoke/dreammaker-real-music-stack-smoke.sh
```

如需测试 MiniMax，把 `REAL_PROVIDER` 改为 `minimax`。该脚本会真实调用 DreamMaker，只能在手动联调窗口内运行；如果本地已经手动启动了 API 或 worker，则改用下方手动启动方式和单作品 smoke。

### 手动启动

先启动 worker。真实 Provider 调用发生在 worker 进程里，因此 worker 必须拿到 DreamMaker 环境变量：

```bash
MUSIC_PROVIDER=mock \
DREAMMAKER_REAL_CALLS_ENABLED=true \
java -jar apps/music-worker/build/libs/music-worker-0.1.0-SNAPSHOT.jar
```

再启动 API，使用 Temporal 目标：

```bash
MUSIC_PROVIDER=mock \
MUSIC_WORKFLOW_DISPATCH_MODE=outbox \
WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true \
WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal \
WORKFLOW_OUTBOX_POLL_INTERVAL=1s \
DREAMMAKER_REAL_CALLS_ENABLED=true \
java -jar apps/music-api/build/libs/music-api-0.1.0-SNAPSHOT.jar
```

`MUSIC_PROVIDER=mock` 保持默认安全；每次联调用请求体中的 `music_provider` 显式选择 `suno` 或 `minimax`。

## Suno 联调步骤

1. 创建填词作品，拿到 `work_id`。
2. 查询作品详情，拿到 `lyrics_draft.lyrics_draft_id`。
3. 确认出歌，请求体使用：

```json
{
  "lyrics_draft_id": "<lyrics_draft_id>",
  "music_provider": "suno"
}
```

4. 轮询 `GET /api/v1/works/{work_id}`，直到 `status=GENERATED` 且 `package_status=PACKAGE_READY`，或进入失败状态。
5. 查询发布包：`GET /api/v1/works/{work_id}/publish-package`。
6. 抽查数据库：

```bash
docker exec yanyun-postgres psql -U postgres -d yanyun_music -Atc \
  "select provider, model_name, provider_trace_id, status, error_code from provider_calls where work_id = '{work_id}' order by created_at"
```

## 脚本化单作品 Smoke

### 非真实 Guard Smoke

在 API 已用 `DREAMMAKER_REAL_CALLS_ENABLED=true` 且默认 `sync` workflow 模式启动时，先执行：

```bash
scripts/smoke/dreammaker-real-guard-smoke.sh
```

该脚本期望确认出歌返回 HTTP 409，并验证作品仍停留在 `LYRICS_READY / WAITING_CONFIRM`；它不会设置 AK/SK，也不调用 DreamMaker。

### 真实 Provider Smoke

在完成凭据注入、worker 启动和 API 启动后，可选择执行脚本：

```bash
ALLOW_DREAMMAKER_REAL_SMOKE=1 \
REAL_PROVIDER=suno \
DREAMMAKER_REAL_CALLS_ENABLED=true \
scripts/smoke/dreammaker-real-music-smoke.sh
```

或：

```bash
ALLOW_DREAMMAKER_REAL_SMOKE=1 \
REAL_PROVIDER=minimax \
DREAMMAKER_REAL_CALLS_ENABLED=true \
scripts/smoke/dreammaker-real-music-smoke.sh
```

脚本会检查 `/internal/integration-readiness` 中 `dreammaker_guard=READY_FOR_LOCAL` 且 `workflow_dispatch=outbox/temporal`，再创建 1 个作品并确认出歌。不要在普通 CI、自动化测试或无联调窗口时运行。

## MiniMax 联调步骤

重复 Suno 步骤，但确认出歌请求体使用：

```json
{
  "lyrics_draft_id": "<lyrics_draft_id>",
  "music_provider": "minimax"
}
```

## 成功判定

- `provider_calls.provider` 分别出现 `SUNO` / `MINIMAX`。
- `provider_calls.model_name` 能区分实际 DreamMaker app/sub_app/model。
- `provider_calls.provider_trace_id` 有真实 task id。
- `works.status=GENERATED`。
- `works.package_status=PACKAGE_READY`。
- `generation_jobs.status=SUCCEEDED` 且 `stage=PACKAGE_READY`。
- 作品详情中的音频 URL 是平台对象 URL，不是供应商原始 URL。
- 发布包 URL 可获取，且不包含供应商凭据或供应商临时 URL。

## 失败处理

- `DREAMMAKER_REAL_CALLS_ENABLED=false` 导致失败：这是保护机制，确认联调窗口是否真的打开。
- HTTP 429 或限流：停止继续触发同 Provider，记录 `work_id`、provider、task id、错误码，回退到 mock。
- 长时间 running / queued：等待本次轮询结束，不重复确认同一作品；必要时停 API dispatcher，避免继续启动新任务。
- 音频导入失败：保留 `work_id`、provider task id 和平台错误码，确认供应商 URL 是否过期或不可下载。
- 任何异常都不要粘贴完整供应商响应；只记录脱敏后的 `error_code` / `error_message`。

## 快速回滚

```bash
unset DREAMMAKER_ACCESS_KEY DREAMMAKER_SECRET_KEY DREAMMAKER_USER_ACCESS_TOKEN
export DREAMMAKER_REAL_CALLS_ENABLED=false
export MUSIC_PROVIDER=mock
```

如仍有 outbox 事件继续启动真实请求，停止 API 和 worker，确认 `provider_calls` 后再重启 mock 路径。

## 证据记录

每次联调在本地记录以下字段，不记录真实密钥、JWT、用户 token 或原始 payload：

| Field | Value |
|---|---|
| 时间 |  |
| Provider | `suno` / `minimax` |
| 模型 |  |
| `work_id` |  |
| `job_id` |  |
| DreamMaker task id |  |
| 最终作品状态 |  |
| 发布包状态 |  |
| 对象存储导入 | 成功 / 失败 |
| 失败码 |  |
| 结论 | 通过 / 待修复 |
