# DreamMaker 真实联调验收清单

首次手动 smoke 可先按 `docs/checklists/dreammaker-real-music-smoke-10min.md` 执行；若本地未启动 API/worker，优先使用 `scripts/smoke/dreammaker-real-music-stack-smoke.sh` 跑单作品 stack smoke；若 worker/API 已按 runbook 手动启动，则使用 `scripts/smoke/dreammaker-real-music-smoke.sh` 跑单作品脚本化 smoke。本清单用于正式验收 Suno 与 MiniMax 两条成功路径、失败止损和交接记录。

## 联调前

- [ ] `git status --short` 无未记录的无关改动。
- [ ] `DREAMMAKER_REAL_CALLS_ENABLED=false` 是默认状态。
- [ ] 真实凭据只通过当前 shell 或安全密钥系统注入。
- [ ] `./gradlew :modules:dreammaker:test :modules:suno:test :modules:minimax:test` 通过。
- [ ] 本地 Docker 基础设施健康。
- [ ] API 使用 outbox temporal 模式，真实调用由 worker 执行。
- [ ] `/internal/integration-readiness` 中 `dreammaker_guard=READY_FOR_LOCAL`，且缺少 AK/SK 时必须显示 `BLOCKED`。
- [ ] `scripts/smoke/dreammaker-real-guard-smoke.sh` 已验证运行时阻止 `DREAMMAKER_REAL_CALLS_ENABLED=true` 时在 `sync` 模式确认或重试 `suno` / `minimax`。
- [ ] 首次 Suno smoke 优先用 `scripts/smoke/dreammaker-real-music-stack-smoke.sh`，或确认手动 worker/API 启动方式与 runbook 一致。

## Suno 成功路径

- [ ] `music_provider=suno` 能创建真实 DreamMaker task。
- [ ] `provider_calls.provider=SUNO`。
- [ ] `provider_calls.model_name` 含 DreamMaker app/sub_app/model。
- [ ] `provider_calls.provider_trace_id` 只以 `<present>` / `<empty>` 形式记录，完整 task id 不进入文档、截图或提交。
- [ ] 供应商音频 URL 被导入平台对象存储。
- [ ] 作品进入 `GENERATED / PACKAGE_READY`。
- [ ] 发布包可获取，且只暴露平台 URL。

## MiniMax 成功路径

- [ ] `music_provider=minimax` 能创建真实 DreamMaker task。
- [ ] `provider_calls.provider=MINIMAX`。
- [ ] `provider_calls.model_name` 含 DreamMaker app/sub_app/model。
- [ ] `provider_calls.provider_trace_id` 只以 `<present>` / `<empty>` 形式记录，完整 task id 不进入文档、截图或提交。
- [ ] 供应商音频 URL 被导入平台对象存储。
- [ ] 作品进入 `GENERATED / PACKAGE_READY`。
- [ ] 发布包可获取，且只暴露平台 URL。

## 失败与止损

- [ ] 未设置 `DREAMMAKER_REAL_CALLS_ENABLED=true` 时，真实 Provider 请求被本地保护层拒绝。
- [ ] 缺失 AK/SK 时，请求不会发出外部 HTTP。
- [ ] 设置 `DREAMMAKER_REAL_CALLS_ENABLED=true` 但 workflow 不是 `outbox/temporal` 时，API 返回冲突，不启动真实任务。
- [ ] 限流、超时、失败任务至少能落库为脱敏错误。
- [ ] 失败作品保留 `RETRY_MUSIC` 或合理的下一步动作。
- [ ] 回退 `MUSIC_PROVIDER=mock` 后本地主链路仍能跑通。

## 安全

- [ ] 提交内容不包含真实 AK/SK、JWT、用户 token、Cookie 或私钥。
- [ ] 日志、截图和联调记录不包含供应商原始 payload。
- [ ] `provider_calls.error_message` 不包含 Bearer token、JWT 或 key/token 字段原文。
- [ ] 终端联调结束后已 `unset` 敏感环境变量。

## 交接

- [ ] 更新 `docs/project-progress.md`。
- [ ] 更新统一脱敏证据日志 `docs/integrations/real-model-smoke-evidence-log.md`，trace 只记录为 `<present>` / `<empty>`。
- [ ] 更新 `docs/integrations/dreammaker-open-questions-tracker.md` 的确认项状态。
- [ ] 若发现新错误码，补充失败码样本、retryable 判断和处理建议。
- [ ] 将可交接信息同步到 `docs/handover/dreammaker-provider-batch-05-handoff.md`。
