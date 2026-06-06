# DreamMaker Provider 第 5 批交接说明

## 当前能力

- 后端支持 `MUSIC_PROVIDER=mock|suno|minimax`。
- `confirm` / `music/retry` 支持请求级 `music_provider` 覆盖，用于受控联调。
- Suno 与 MiniMax 均通过 DreamMaker run/status 协议提交任务并轮询结果。
- DreamMaker 鉴权使用 HS256 JWT：`iss=AccessKey`，SecretKey 签名，`Authorization: Bearer <jwt>`。
- 可选 `DREAMMAKER_USER_ACCESS_TOKEN` 会透传为 `X-Access-Token`。
- Provider 返回音频 URL 后，平台会先导入对象存储，再写入作品媒体资产和发布包。
- Temporal 模式下真实 Provider 调用发生在 `music-worker`。
- 当 `DREAMMAKER_REAL_CALLS_ENABLED=true` 且 Provider 为 `suno` / `minimax` 时，API 已强制要求 outbox + Temporal worker；sync 模式会直接返回冲突，避免误在 API 线程中触发真实供应商。

## 环境变量

| 变量 | 说明 | 默认 |
|---|---|---|
| `DREAMMAKER_API_BASE_URL` | DreamMaker API 域名 | `https://api-all.dreammaker.netease.com` |
| `DREAMMAKER_ACCESS_KEY` | AccessKey | 空 |
| `DREAMMAKER_SECRET_KEY` | SecretKey | 空 |
| `DREAMMAKER_USER_ACCESS_TOKEN` | 可选用户 token | 空 |
| `DREAMMAKER_REAL_CALLS_ENABLED` | 真实请求硬开关 | `false` |
| `DREAMMAKER_REQUEST_TIMEOUT` | 单次 HTTP timeout | `30s` |
| `DREAMMAKER_MAX_POLL_ATTEMPTS` | 最大轮询次数 | `60` |
| `DREAMMAKER_POLL_INTERVAL` | 轮询间隔 | `2s` |
| `SUNO_MODEL` | Suno 模型 | `chirp-crow` |
| `MINIMAX_MODEL` | MiniMax 模型 | `minimax-music-2.6` |

## 公司接入注意事项

- 生产环境必须使用公司密钥系统注入 AK/SK，不允许写入镜像、Git、日志或静态配置。
- 若公司要求真实用户归因，应由公司系统提供 `DREAMMAKER_USER_ACCESS_TOKEN` 注入策略。
- 真实联调和生产建议走 outbox + Temporal worker，不建议用 API sync 模式调用真实 Provider。
- 公司审核、权益、发布、分享仍通过 Adapter 接入，本平台当前只负责创作、出歌、媒体和发布包。
- 若要对普通用户开放模型选择，需要公司侧产品策略确认；当前请求级 `music_provider` 主要服务受控联调。

## 待公司确认

- 错误码与 retryable 规则。
- 限流、计费和并发额度。
- 音频 URL 有效期与下载授权。
- `X-Access-Token` 是否强制。
- 内容安全失败的错误码和用户提示策略。
- 生产对象存储、日志留存和数据合规要求。

## 推荐交接命令

```bash
./gradlew spotlessCheck test :apps:music-api:bootJar :apps:music-worker:bootJar
python3 /Users/tongyifeng/.codex/skills/env-secrets-manager/scripts/env_auditor.py /Users/tongyifeng/Desktop/ai项目/燕云乐坊
```

首次人工真实音乐 smoke 参考：

- `scripts/smoke/dreammaker-real-music-smoke.sh`
- `docs/checklists/dreammaker-real-music-smoke-10min.md`
- `docs/runbook/dreammaker-controlled-real-integration.md`
- `docs/checklists/dreammaker-real-integration-acceptance.md`
