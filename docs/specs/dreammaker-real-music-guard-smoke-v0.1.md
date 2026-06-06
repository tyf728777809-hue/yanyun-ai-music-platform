# DreamMaker Real Music Guard Smoke v0.1

日期：2026-06-06
状态：已决策

## 1. 背景

真实 Suno / MiniMax 通过 DreamMaker 接入后，系统允许用户在确认出歌或音乐重试时选择 `music_provider=suno|minimax`。真实音乐任务可能轮询数分钟，并会消耗供应商额度，因此真实调用只能在受控联调窗口走 `outbox + Temporal worker`。

当前已经有 `scripts/smoke/dreammaker-real-music-smoke.sh` 用于真实单作品联调，但它会真实调用 DreamMaker。还需要一个不会触发外部请求的 smoke，用来证明运行时 guard 生效：当 `DREAMMAKER_REAL_CALLS_ENABLED=true` 且 API 仍是默认 `sync` 模式时，`suno|minimax` 确认或重试必须在进入 Provider 前被拒绝。

## 2. 功能要求

- FR-1：Guard smoke MUST 要求 API 已启动并可访问 `/health`。
- FR-2：Guard smoke MUST 读取 `/internal/integration-readiness`，并确认 `dreammaker_guard` 不是 `real-calls-disabled`。
- FR-3：Guard smoke MUST 读取 `/internal/integration-readiness`，并确认 `workflow_dispatch` 是 `sync/*`，不能在 `outbox/temporal` 下运行。
- FR-4：Guard smoke MUST 创建 1 个歌词作品，并用 `music_provider=suno` 或 `music_provider=minimax` 确认出歌。
- FR-5：Guard smoke MUST 断言确认出歌返回 HTTP 409，且错误提示包含 `outbox + Temporal worker`。
- FR-6：Guard smoke SHOULD 在 Docker PostgreSQL 可用时确认该作品没有写入 `provider_calls`，且作品仍保持 `LYRICS_READY / WAITING_CONFIRM`。
- FR-7：Guard smoke MUST NOT 设置真实 AK/SK、JWT、用户 token，也 MUST NOT 调用 DreamMaker 外部 HTTP。

## 3. 非功能要求

- NFR-1：脚本失败时必须输出明确原因。
- NFR-2：脚本不得打印任何真实凭据字段值。
- NFR-3：脚本默认只测 1 个 Provider，避免误创建多个作品。

## 4. 验收标准

- AC-1：Given API 以 `DREAMMAKER_REAL_CALLS_ENABLED=true` 和 `MUSIC_WORKFLOW_DISPATCH_MODE=sync` 启动，When 执行 guard smoke，Then 确认出歌返回 HTTP 409，覆盖 FR-1 到 FR-5。
- AC-2：Given Docker PostgreSQL 可访问，When 执行 guard smoke，Then `provider_calls` 对该作品为 0，作品仍是 `LYRICS_READY / WAITING_CONFIRM`，覆盖 FR-6。
- AC-3：Given API 未打开 DreamMaker 真实开关，When 执行 guard smoke，Then 脚本拒绝继续，覆盖 FR-2 和 FR-7。
- AC-4：Given API 是 `outbox/temporal`，When 执行 guard smoke，Then 脚本拒绝继续，覆盖 FR-3 和 FR-7。

## 5. Out of Scope

- OS-1：不验证真实 Suno / MiniMax 成功路径；真实成功路径由 `scripts/smoke/dreammaker-real-music-smoke.sh` 覆盖。
- OS-2：不启动 API、worker、Docker 或前端；脚本只验证已启动 API 的运行时行为。
- OS-3：不验证 DeepSeek、Image 2 或公司 Adapter。
