# Yunwu Suno 受控真实联调 Runbook

## 目标

本 Runbook 用于当前非公司内网环境下的 Suno 公网联调。Yunwu 是临时可测路径；DreamMaker 接口和文档仍保留为正式生产目标。

## 硬性规则

- 默认禁止真实请求：`MUSIC_PROVIDER=mock`、`YUNWU_REAL_CALLS_ENABLED=false`。
- 只有手动联调窗口内才允许设置 `MUSIC_PROVIDER=suno`、`SUNO_BACKEND=yunwu`、`YUNWU_REAL_CALLS_ENABLED=true`。
- 真实音乐调用必须走 `MUSIC_WORKFLOW_DISPATCH_MODE=outbox` 和 `WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal`，不能在 API sync 线程里执行。
- DeepSeek、Image 2 和公司 Adapter 可继续 Mock；若要完整成片，可单独打开 Image 2 runbook。
- 不把 API Key、Bearer header、完整请求/响应、音频 URL 写入仓库、文档、日志或提交信息。

## 环境变量

只在当前 shell 或公司 Secret 系统注入：

```bash
export MUSIC_PROVIDER=suno
export SUNO_BACKEND=yunwu
export YUNWU_BASE_URL=https://yunwu.ai
export YUNWU_API_KEY="<from-secure-channel>"
export YUNWU_REAL_CALLS_ENABLED=true
export YUNWU_SUNO_MODEL=chirp-fenix
export YUNWU_REQUEST_TIMEOUT=300s
export YUNWU_MAX_POLL_ATTEMPTS=180
export YUNWU_POLL_INTERVAL=2s
export MUSIC_WORKFLOW_DISPATCH_MODE=outbox
export WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal
export WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true
```

联调结束：

```bash
unset YUNWU_API_KEY
export YUNWU_REAL_CALLS_ENABLED=false
export MUSIC_PROVIDER=mock
```

## 最小联调步骤

真实调用前先从统一总入口查看目标矩阵和执行路线，再做只读预检。总入口会标明 Yunwu 只是当前公网受控 smoke 路径，不替代 DreamMaker 生产路径；底层 `real-model-readiness-preflight.sh` 只作为总入口调用的实现细节，不建议交接方直接绕过总入口。

```bash
TARGET=yunwu-suno MODE=plan scripts/smoke/real-model-controlled-smoke.sh
TARGET=yunwu-suno MODE=preflight scripts/smoke/real-model-controlled-smoke.sh
```

如果本地 8080/8081 没有已启动服务，优先使用一键 stack smoke。脚本会静默读取缺失的 `YUNWU_API_KEY`、启动 worker/API、执行 1 个 Suno 作品、结束后自动停止它启动的进程：

```bash
ALLOW_REAL_MODEL_SMOKE=1 \
ALLOW_YUNWU_REAL_SMOKE=1 \
scripts/smoke/yunwu-suno-real-music-stack-smoke.sh
```

如果 worker/API 已经手动按本 runbook 启动，使用低层单作品 smoke：

```bash
ALLOW_REAL_MODEL_SMOKE=1 \
ALLOW_YUNWU_REAL_SMOKE=1 \
MUSIC_PROVIDER=suno \
SUNO_BACKEND=yunwu \
YUNWU_REAL_CALLS_ENABLED=true \
scripts/smoke/yunwu-suno-real-music-smoke.sh
```

两个脚本都会真实调用 Yunwu；不要作为普通自动化测试运行。

1. 启动本地基础设施和 Temporal worker，worker 进程必须拿到同一组 `YUNWU_*` 环境变量。
2. 启动 API，确认 `/internal/integration-readiness` 中 `music_provider=suno/yunwu`、`yunwu_suno_guard=real-calls-enabled`。
3. 创建作品，确认歌词后用 `music_provider=suno` 发起确认出歌。
4. 轮询作品状态，成功时应进入 `GENERATED / PACKAGE_READY`；失败时记录脱敏 `failure_code`、`provider_calls.model_name`、`provider_calls.status`。
5. 确认供应商音频 URL 已被导入平台对象存储，发布包不直接暴露供应商原始 URL。

## 当前音乐接口与时间轴状态

- Auth：`Authorization: Bearer <YUNWU_API_KEY>`。
- Submit：`POST /suno/submit/music`。
- Custom request：`mv`、`make_instrumental=false`、`prompt`、`tags`、`title`。
- Poll：`GET /suno/fetch/{task_id}`。
- 响应结构以容错解析为准：提交成功可能是 `data=<task_id>` 或 `data.task_id`；音频 URL 会兼容 `audio_url`、`url`、`data.clips[].audio_url` 等字段。
- 默认音乐模型：`chirp-fenix`（对应 Suno v5.5 公网联调口径）。
- Timestamped lyrics：当前脚本默认尝试 SunoAPI 兼容路径 `POST /api/v1/generate/get-timestamped-lyrics`，只在成功生成音乐后用 `taskId + audioId` 查询。`audioId` 来自 Yunwu 音频结果对象，平台会将它脱敏落入 `AUDIO.metadata_json.provider_audio_id`，同时写入 `provider_task_id` 供本地 smoke 配对。2026-06-11 实测当前 Yunwu base URL 对该路径返回 404，若干 `/suno/*` 探测路径返回 HTML 非 JSON；供应商确认正确 JSON 接口前，不要把该路径写成已可用能力。

## 时间轴歌词验证

如果要判断真实歌曲能否做精确字幕，必须在一条 `chirp-fenix` 音乐样本成功后单独验证 timestamped lyrics。公网完整体验脚本默认不再检查时间轴；如需只测时间轴，可用下面的低层脚本，或显式设置 `CHECK_YUNWU_TIMESTAMPED_LYRICS=true`。当前结论是 `blocked_provider_path`，除非供应商给出新的 base URL / path / request body，否则默认视频验收采用无字幕方案。

```bash
ALLOW_REAL_MODEL_SMOKE=1 \
ALLOW_YUNWU_TIMESTAMPED_LYRICS_SMOKE=1 \
SUNO_BACKEND=yunwu \
YUNWU_REAL_CALLS_ENABLED=true \
YUNWU_API_KEY="<from-current-shell>" \
WORK_ID="<successful-yunwu-work-id>" \
scripts/smoke/yunwu-suno-timestamped-lyrics-smoke.sh
```

安全规则：

- 脚本必须同时满足 `ALLOW_REAL_MODEL_SMOKE=1`、`ALLOW_YUNWU_TIMESTAMPED_LYRICS_SMOKE=1`、`SUNO_BACKEND=yunwu`、`YUNWU_REAL_CALLS_ENABLED=true` 才会外呼。
- `YUNWU_API_KEY` 只能来自当前 shell 或交互式安全输入，不写入文件。
- 脚本不会把 Bearer、原始 provider response、完整时间轴、媒体 URL 或完整 task id 写入 Git 文档。
- 证据只记录脱敏摘要：HTTP 状态、provider code、aligned word count、waveform 是否存在、timestamp 是否存在。
- 如果 `provider_audio_id` 不存在，说明供应商当前响应没有暴露可用于时间轴查询的音频 ID；此时本轮不能判定字幕可同步，应记录为阻塞或改走无字幕/弱字幕方案。

## 失败处理

- HTTP 401 / 403 或权限错误会映射为 `PROVIDER_AUTH_FAILED`，用户侧不可重试，需检查 key、账号权限、模型开通或供应商侧限制。
- HTTP 429 会映射为 `RATE_LIMITED`，停止继续触发新样本。
- 超时会映射为 `PROVIDER_TIMEOUT`，可在剩余重试次数内重试。
- 任何失败都只记录脱敏摘要，不记录完整 provider payload。

## 证据记录

每次 Yunwu 公网 smoke 后同步到统一脱敏日志：`docs/integrations/real-model-smoke-evidence-log.md`。只记录 `work_id`、最终状态、失败码、trace 是否 `<present>`、对象存储导入结果和下一步判断；不要记录完整 task id、音频 URL、请求/响应或 API Key。Yunwu 成功只代表公网路径通过，不代表 DreamMaker 生产目标完成。

2026-06-07 已完成一条 Yunwu Suno 公网成功样本，并完成一条公网完整体验成功样本；详见统一脱敏证据日志。真实音频 URL 会先经平台远程对象导入器进入对象存储，导入器已支持重定向和短重试，发布包只暴露平台对象 URL。
