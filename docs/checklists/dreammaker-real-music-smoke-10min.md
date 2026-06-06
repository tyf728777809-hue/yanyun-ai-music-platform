# DreamMaker 真实音乐 10 分钟 Smoke 清单

更新时间：2026-06-06

## 适用场景

本清单用于手动验证“真实 Suno / MiniMax 出歌是否能通过 DreamMaker 打通”。它只验证音乐生成真实调用，其他系统仍保持本地 Mock / Adapter 边界：账号、权益、审核、发布、分享、社区系统不做真实接入。

正式规则仍以 `docs/runbook/dreammaker-controlled-real-integration.md` 和 `docs/security/dreammaker-secret-and-log-handling.md` 为准。本清单只提供最短可执行路径。

## 0. 先确认边界

- [ ] 本次只测 1 个 Provider、1 个作品，避免无意识消耗额度。
- [ ] 默认 `MUSIC_PROVIDER=mock` 不改；只在 `confirm` 请求体里显式传 `music_provider=suno` 或 `music_provider=minimax`。
- [ ] 必须走 `outbox + Temporal worker`，不要用 API `sync` 模式跑真实音乐。
- [ ] 运行时会阻止 `DREAMMAKER_REAL_CALLS_ENABLED=true` 时在 `sync` 模式下使用 `suno` / `minimax`，如遇 409 需先检查 workflow dispatch 配置。
- [ ] 不把 AK、SK、JWT、`X-Access-Token`、供应商原始 payload 写进文档、截图、日志、测试或 commit。
- [ ] 联调结束后必须清理 shell 环境变量。

## 1. 准备本地环境

```bash
git status --short
command -v curl
command -v jq

export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew :modules:dreammaker:test :modules:suno:test :modules:minimax:test :apps:music-api:bootJar :apps:music-worker:bootJar

docker compose -f deploy/docker-compose.yml --env-file .env.example up -d
docker compose -f deploy/docker-compose.yml --env-file .env.example ps
```

确认 PostgreSQL、Temporal、MinIO 至少是 `healthy` 或 `Up`。

## 2. 安全注入凭据

只在当前终端读入，不要把真实值写入命令历史或文件：

```bash
export DREAMMAKER_API_BASE_URL="https://api-all.dreammaker.netease.com"
export DREAMMAKER_REAL_CALLS_ENABLED=true

read -r -s -p "DREAMMAKER_ACCESS_KEY: " DREAMMAKER_ACCESS_KEY; echo
export DREAMMAKER_ACCESS_KEY

read -r -s -p "DREAMMAKER_SECRET_KEY: " DREAMMAKER_SECRET_KEY; echo
export DREAMMAKER_SECRET_KEY

# 可选：只有需要 DreamMaker 按真实用户 token 归因时才设置。
# read -r -s -p "DREAMMAKER_USER_ACCESS_TOKEN: " DREAMMAKER_USER_ACCESS_TOKEN; echo
# export DREAMMAKER_USER_ACCESS_TOKEN
```

## 3. 启动 worker

在注入凭据的终端启动 worker。真实 Provider 调用发生在 worker 里，所以 worker 必须拿到 DreamMaker 环境变量。

```bash
MUSIC_PROVIDER=mock \
TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy \
./gradlew :apps:music-worker:bootRun
```

看到 worker 注册到 `song-production-local` task queue 后，保留该终端运行。

## 4. 启动 API

另开一个终端，并重复执行第 2 步的安全注入。然后启动 API：

```bash
MUSIC_PROVIDER=mock \
MUSIC_WORKFLOW_DISPATCH_MODE=outbox \
WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true \
WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal \
WORKFLOW_OUTBOX_POLL_INTERVAL=1s \
RENDER_WORKER_MODE=mock \
./gradlew :apps:music-api:bootRun
```

确认：

```bash
curl -s http://localhost:8080/health
```

可选：如果不想手动执行第 5-8 步，可在完成 worker/API 启动后运行脚本化 smoke。该脚本会真实调用 DreamMaker，必须显式确认：

```bash
ALLOW_DREAMMAKER_REAL_SMOKE=1 \
REAL_PROVIDER=suno \
DREAMMAKER_REAL_CALLS_ENABLED=true \
scripts/smoke/dreammaker-real-music-smoke.sh
```

或把 `REAL_PROVIDER` 改为 `minimax`。脚本只创建 1 个作品，不会同时测试两个 Provider。

## 5. 创建作品

```bash
export API_BASE="http://localhost:8080/api/v1"
export MOCK_USER="mock_user_001"
export IDEMPOTENCY_PREFIX="real-music-$(date +%s)"

CREATE_RESPONSE="$(
  curl -sS -X POST "$API_BASE/works/lyrics" \
    -H "Content-Type: application/json" \
    -H "X-Mock-User-Id: $MOCK_USER" \
    -H "Idempotency-Key: $IDEMPOTENCY_PREFIX-create" \
    -d '{
      "song_title": "真实音乐联调测试",
      "lyrics_input": "雁门风起过长街，灯影照见旧山河。故人踏月归来晚，一曲清歌入燕云。",
      "music_style": "国风民谣，古筝，笛子，女声，温柔叙事"
    }'
)"

echo "$CREATE_RESPONSE" | jq .
export WORK_ID="$(echo "$CREATE_RESPONSE" | jq -r '.work_id')"
```

等待歌词草稿就绪：

```bash
while true; do
  DETAIL_RESPONSE="$(curl -sS "$API_BASE/works/$WORK_ID" -H "X-Mock-User-Id: $MOCK_USER")"
  STATUS="$(echo "$DETAIL_RESPONSE" | jq -r '.status')"
  LYRICS_DRAFT_ID="$(echo "$DETAIL_RESPONSE" | jq -r '.lyrics_draft.lyrics_draft_id // empty')"
  echo "$(date '+%H:%M:%S') status=$STATUS lyrics_draft_id=${LYRICS_DRAFT_ID:-empty}"

  if [ -n "$LYRICS_DRAFT_ID" ]; then
    echo "$DETAIL_RESPONSE" | jq .
    export LYRICS_DRAFT_ID
    break
  fi
  if [ "$STATUS" = "LYRICS_FAILED" ] || [ "$STATUS" = "FAILED" ]; then
    echo "$DETAIL_RESPONSE" | jq .
    exit 1
  fi
  sleep 2
done
```

## 6. 确认出歌

选择一个 Provider。先测一个，不要同时测两个：

```bash
export REAL_PROVIDER="suno"
# 或：
# export REAL_PROVIDER="minimax"
```

确认出歌：

```bash
curl -sS -X POST "$API_BASE/works/$WORK_ID/confirm" \
  -H "Content-Type: application/json" \
  -H "X-Mock-User-Id: $MOCK_USER" \
  -H "Idempotency-Key: $IDEMPOTENCY_PREFIX-confirm-$REAL_PROVIDER" \
  -d "{
    \"lyrics_draft_id\": \"$LYRICS_DRAFT_ID\",
    \"music_provider\": \"$REAL_PROVIDER\"
  }" | jq .
```

预期：API 很快返回 `GENERATING / QUOTA_LOCKING` 或相近生成中状态；真实调用在 worker 中继续执行。

## 7. 轮询结果

```bash
while true; do
  DETAIL_RESPONSE="$(curl -sS "$API_BASE/works/$WORK_ID" -H "X-Mock-User-Id: $MOCK_USER")"
  STATUS="$(echo "$DETAIL_RESPONSE" | jq -r '.status')"
  STAGE="$(echo "$DETAIL_RESPONSE" | jq -r '.generation_stage')"
  PACKAGE_STATUS="$(echo "$DETAIL_RESPONSE" | jq -r '.package_status')"
  FAILURE_CODE="$(echo "$DETAIL_RESPONSE" | jq -r '.failure.failure_code // empty')"
  echo "$(date '+%H:%M:%S') status=$STATUS stage=$STAGE package=$PACKAGE_STATUS failure=$FAILURE_CODE"

  if [ "$STATUS" = "GENERATED" ] || [ "$STATUS" = "FAILED" ] || [ "$STATUS" = "LYRICS_FAILED" ]; then
    echo "$DETAIL_RESPONSE" | jq .
    break
  fi
  sleep 5
done
```

成功时获取发布包：

```bash
curl -sS "$API_BASE/works/$WORK_ID/publish-package" \
  -H "X-Mock-User-Id: $MOCK_USER" | jq .
```

## 8. 判断是否真的打到 DreamMaker

数据库抽查：

```bash
docker exec yanyun-postgres psql -U postgres -d yanyun_music -Atc \
  "select provider, model_name, provider_trace_id, status, error_code from provider_calls where work_id = '$WORK_ID' order by created_at;"
```

成功证据：

- [ ] `provider` 为 `SUNO` 或 `MINIMAX`，不是 `MOCK`。
- [ ] `provider_trace_id` 有真实 task id。
- [ ] `works.status=GENERATED`。
- [ ] `works.package_status=PACKAGE_READY`。
- [ ] 作品详情里的 `media_assets.audio_url` 是平台 URL，不是供应商原始 URL。
- [ ] 发布包可获取，且不包含 AK/SK、JWT、用户 token 或供应商临时 URL。

失败但仍算有效联调样本：

- [ ] `provider_calls` 有真实 Provider 和 task id。
- [ ] 失败码已脱敏，例如 `PROVIDER_TIMEOUT`、`RATE_LIMITED`、`MUSIC_GENERATION_FAILED`。
- [ ] 作品失败页保留 `RETRY_MUSIC` 或合理下一步动作。
- [ ] 没有重复确认同一个作品导致第二个真实任务。

## 9. 回滚与清理

停止 API 和 worker 后执行：

```bash
unset DREAMMAKER_ACCESS_KEY DREAMMAKER_SECRET_KEY DREAMMAKER_USER_ACCESS_TOKEN
export DREAMMAKER_REAL_CALLS_ENABLED=false
export MUSIC_PROVIDER=mock
```

复验 Mock 路径：

```bash
MOCK_MUSIC_DURATION_MS=1000 \
MUSIC_PROVIDER=mock \
MUSIC_WORKFLOW_DISPATCH_MODE=sync \
WORKFLOW_OUTBOX_DISPATCHER_ENABLED=false \
RENDER_WORKER_MODE=mock \
./gradlew :apps:music-api:bootRun
```

另开终端：

```bash
EXPECTED_DURATION_MS=1000 scripts/smoke/api-main-flow.sh
```

## 10. 记录模板

只记录脱敏字段：

| 字段 | 值 |
|---|---|
| 时间 |  |
| Provider | `suno` / `minimax` |
| `work_id` |  |
| `job_id` |  |
| DreamMaker task id |  |
| 最终状态 |  |
| 发布包状态 |  |
| 是否导入平台对象存储 |  |
| 失败码 |  |
| 是否需要更新错误码映射 |  |
| 结论 | 通过 / 待修复 |
