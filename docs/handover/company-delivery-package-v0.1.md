# 公司开发交接包 v0.1

更新时间：2026-06-07 04:28 CST
状态：本地 Mock 闭环可实测；真实模型和公司系统仍按受控联调 / 公司接入推进。

## 1. 先读结论

本项目交给公司开发时，应按“本平台负责 AI 创作与成片，公司系统负责账号、审核、权益、发布、分享和社区互动”的边界接手。

当前本地可验证的是完整 Mock 创作链路、OpenAPI v0.1、Claude 前端原型真实后端模式、render-worker 本地 MP4 成片、公司 Adapter readiness 和交付证据审计。不能把这些说成真实公司系统已接入，也不能说真实 AI 出歌已经完整成功。

DreamMaker 音乐和 DreamMaker Image 2 是必须保留的正式生产目标路径。Yunwu Suno 和 WellAPI Image 2 只用于当前非公司内网环境下的公网受控 smoke，不是替代 DreamMaker 的理由。

## 2. 第一阅读顺序

| 顺序 | 文档 | 用途 |
|---|---|---|
| 1 | `docs/handover/local-commercial-delivery-status-v0.1.md` | 看清 `READY_LOCAL`、`PREPARED_SMOKE`、`PREPARED_HANDOFF`、`BLOCKED_EXTERNAL`、`DECISION_REQUIRED` 分类。 |
| 2 | `docs/checklists/local-commercial-delivery-acceptance.md` | 作为最终交付 gate，逐项留证据。 |
| 3 | `docs/handover/company-adapter-deployment-handoff-v0.1.md` | 看公司账号、审核、权益、发布、分享 Adapter 接入边界。 |
| 4 | `docs/checklists/company-adapter-replacement-readiness.md` | 公司替换 Mock Adapter 时逐项验收。 |
| 5 | `docs/api/openapi-v0.1.yaml` | 前后端和公司发布交接的接口契约。 |
| 6 | `docs/runbook/local-development.md` | 本地启动、smoke、真实模型受控联调入口。 |
| 7 | `docs/adr/0003-frontend-delivery-track.md` | 前端交付轨道：当前验收对象是 `prototypes/Claude-web-v1`，`apps/web` 仍是 scaffold。 |
| 8 | `docs/adr/0004-production-provider-targets.md` | 供应商定位：DreamMaker 是正式生产目标，Yunwu / WellAPI 只是公网受控联调路径。 |

## 3. 可先跑的只读检查

这些命令不调用真实模型供应商，也不访问真实公司系统：

```bash
scripts/smoke/local-delivery-evidence-audit.sh
scripts/smoke/real-model-controlled-smoke.sh
scripts/smoke/real-model-safety-gates-audit.sh
```

API 启动后再跑：

```bash
scripts/smoke/company-adapter-readiness-smoke.sh
scripts/smoke/openapi-contract.sh
```

公司交接前建议用严格模式先证明工作区干净：

```bash
STRICT_GIT_CLEAN=true scripts/smoke/local-delivery-evidence-audit.sh
```

## 4. 本地主链路复验

默认使用 Mock Provider，不调用 DeepSeek、Suno、MiniMax、Image 2 或公司系统：

```bash
EXPECTED_DURATION_MS=1000 scripts/smoke/api-main-flow.sh
```

如果要验证本地 Remotion MP4 成片边界：

```bash
RENDER_WORKER_MODE=local-process EXPECT_RENDER_WORKER=local-process \
EXPECTED_DURATION_MS=1000 scripts/smoke/api-main-flow.sh
```

前端当前验收对象：

```bash
cd prototypes/Claude-web-v1
npm run smoke:real-backend
```

`apps/web` 仍是正式工程 scaffold。是否迁移 `prototypes/Claude-web-v1`、公司前端重建，或保留原型，需要交付前单独决策。

## 5. 公司系统必须替换或承接

| 系统 | 当前本地边界 | 公司接入要求 |
|---|---|---|
| 账号 | `MockAccountAdapter`、本地 `X-Mock-User-Id` | 生产必须由公司网关、登录态、session 或可信 token 注入真实用户；不得信任普通客户端传入 `X-Mock-User-Id`。 |
| 审核 | `MockModerationAdapter` | 覆盖用户输入、歌词草案、发布包交接前预检；只返回可映射 code 和用户可读文案。 |
| 权益 | `MockQuotaAdapter` | 实现 `lock / commit / release` 幂等；按 `work_id`、`job_id`、`lock_id` 可对账。 |
| 发布 | `MockPublishAdapter` + 对象存储发布包 | 公司发布系统读取发布包 URL；本平台只负责素材和交接状态。 |
| 分享 | `NotImplementedShareBoundary` 口径 | 分享入口、分享卡片、传播归因、回流统计由公司社区系统承接。 |

`POST /publish-package/mark-fetched` 只表示“发布素材已交接给公司发布流程”，不表示社区发布成功。

## 6. 真实模型接入顺序

真实模型调用必须先看总入口，不要直接跳到供应商脚本：

```bash
scripts/smoke/real-model-controlled-smoke.sh
TARGET=yunwu-suno MODE=plan scripts/smoke/real-model-controlled-smoke.sh
TARGET=dreammaker-suno MODE=plan scripts/smoke/real-model-controlled-smoke.sh
TARGET=dreammaker-minimax MODE=plan scripts/smoke/real-model-controlled-smoke.sh
TARGET=deepseek MODE=plan scripts/smoke/real-model-controlled-smoke.sh
TARGET=wellapi-image2 MODE=plan scripts/smoke/real-model-controlled-smoke.sh
TARGET=dreammaker-image2 MODE=plan scripts/smoke/real-model-controlled-smoke.sh
```

首次真实模型联调建议一次只打开一个外部成本点：

1. 真实音乐：当前公网可先测 `yunwu-suno`；公司内网或生产目标切回 `dreammaker-suno` / `dreammaker-minimax`。
2. 真实写词：单独按 DeepSeek runbook 打开 `AGENT_REAL_CALLS_ENABLED=true` 和 `DEEPSEEK_REAL_CALLS_ENABLED=true`，再通过 `ALLOW_REAL_MODEL_SMOKE=1 ALLOW_DEEPSEEK_REAL_SMOKE=1 TARGET=deepseek MODE=execute scripts/smoke/real-model-controlled-smoke.sh` 跑 1 条写词样本。
3. 真实封面：当前公网可先测 `wellapi-image2`；生产目标通过 `ALLOW_REAL_MODEL_SMOKE=1 ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE=1 TARGET=dreammaker-image2 MODE=execute scripts/smoke/real-model-controlled-smoke.sh` 验证 `dreammaker-image2`。

真实执行必须显式走 `MODE=execute`，同时设置 `ALLOW_REAL_MODEL_SMOKE=1` 和目标脚本自己的 `ALLOW_*` 开关。真实密钥只允许通过 shell、部署 Secret 或公司配置中心注入，不能写入仓库、镜像、文档、截图、日志或提交信息。

## 7. 生产部署前不能混淆的状态

| 状态 | 交接含义 |
|---|---|
| `READY_LOCAL` | 本地 Mock 或本地进程模式有可复验证据。 |
| `PREPARED_SMOKE` | 真实联调代码、脚本、runbook 已准备，但尚未代表真实成功。 |
| `PREPARED_HANDOFF` | 交接资料、替换矩阵已准备，真实替换仍待公司实施。 |
| `BLOCKED_EXTERNAL` | 需要公司内网、真实凭据、外部账号权限或公司系统接入。 |
| `DECISION_REQUIRED` | 需要用户、公司前端、公司运维或产品负责人确认策略。 |

## 8. 禁止事项

- 不在生产环境信任 `X-Mock-User-Id`。
- 不在本平台内重做公司社区发布、发布审核流、分享系统、点赞、评论、收藏或推荐流。
- 不删除 DreamMaker 音乐或 DreamMaker Image 2 接口边界。
- 不把 Yunwu 或 WellAPI 公网 smoke 成功写成 DreamMaker 生产路径已完成。
- 不把 `mark-fetched` 写成社区发布成功。
- 不把真实 AK/SK、API Key、JWT、Cookie、用户 token、签名 URL 写入仓库、文档、测试、截图、日志或提交信息。
- 不把供应商或公司系统原始响应直接展示给用户。

## 9. 交接时建议留下的证据

| 证据 | 推荐入口 |
|---|---|
| 本地交付证据审计 | `STRICT_GIT_CLEAN=true scripts/smoke/local-delivery-evidence-audit.sh` |
| 真实模型安全门矩阵审计 | `scripts/smoke/real-model-safety-gates-audit.sh` |
| 公司 Adapter readiness | `scripts/smoke/company-adapter-readiness-smoke.sh` |
| OpenAPI 运行时契约 | `scripts/smoke/openapi-contract.sh` |
| 本地主链路 | `EXPECTED_DURATION_MS=1000 scripts/smoke/api-main-flow.sh` |
| 本地 MP4 成片 | `RENDER_WORKER_MODE=local-process EXPECT_RENDER_WORKER=local-process EXPECTED_DURATION_MS=1000 scripts/smoke/api-main-flow.sh` |
| 前端真实后端模式 | `cd prototypes/Claude-web-v1 && npm run smoke:real-backend` |
| 真实模型计划 / 预检 | `TARGET=<target> MODE=plan/preflight scripts/smoke/real-model-controlled-smoke.sh` |
| DeepSeek 单样本写词 smoke | `ALLOW_REAL_MODEL_SMOKE=1 ALLOW_DEEPSEEK_REAL_SMOKE=1 TARGET=deepseek MODE=execute scripts/smoke/real-model-controlled-smoke.sh` |
| DreamMaker Image 2 单作品封面 smoke | `ALLOW_REAL_MODEL_SMOKE=1 ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE=1 TARGET=dreammaker-image2 MODE=execute scripts/smoke/real-model-controlled-smoke.sh` |

## 10. 当前仍需公司或后续阶段完成

- 真实账号、审核、权益、发布、分享系统接入。
- 公司服务器部署方案，包括对象存储、Temporal、render-worker、监控和日志策略。
- 正式前端承接策略。
- Suno / MiniMax / DeepSeek / Image 2 的真实成功样本和失败码精细化。
- 生产权限、成本上限、密钥轮换和事故回滚流程。
