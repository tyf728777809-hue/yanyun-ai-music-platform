# 本地商用交付状态说明 v0.1

更新时间：2026-06-07 15:20 CST
状态：本地 Mock 闭环与公网真实完整体验均可交付实测；DreamMaker 生产验证和公司系统仍按受控联调 / 外部接入推进。

## 1. 阅读结论

当前项目已经具备“本地商用级闭环验证”和“公网真实 AI 产品体验”的主要证据：后端主链路、OpenAPI 契约、Claude 前端原型真实后端模式、render-worker 本地 MP4 成片、公司 Adapter readiness 报告、只读 smoke、DeepSeek / Yunwu / WellAPI 单项 smoke，以及 DeepSeek + Yunwu Suno + WellAPI Image 2 + local-process MP4 + Claude Web v1 组合 smoke 都有可复验入口。

当前项目可以宣称“公网真实完整产品体验已成功跑通一条样本”：作品 `0dd48e52-2477-4c89-ad7a-43d18a976657` 已通过真实写词、真实出歌、真实封面、本地 MP4、发布素材包和前端交接动作。当前项目仍不能宣称“DreamMaker 生产路径已验证”或“公司系统已接入”：真实 Suno via DreamMaker 曾触达创建任务但返回 HTTP 403，MiniMax / DreamMaker Image 2 尚未完成真实成功样本；公司账号、审核、权益、社区发布、分享仍是公司系统外部接入项。

DreamMaker 是正式生产目标接口，必须持续保留音乐和 Image 2 两条 DreamMaker Adapter / smoke / runbook 路径。Yunwu / WellAPI 只是当前非公司内网环境下的公网联调后端，不替代 DreamMaker 音乐或 DreamMaker Image 2 路径。公司交付验收、用户实测和生产部署 smoke 必须保持 `TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy`，直到 `stepwise-production` 有专用 production activity 和独立 smoke 证据；`stepwise-recording` 只允许作为 step audit 受控验证。

## 2. 状态口径

| 状态 | 含义 |
|---|---|
| `READY_LOCAL` | 本地 Mock 或本地进程模式已有脚本化复验证据，可给用户做本地链路实测。 |
| `PREPARED_SMOKE` | 真实联调代码、runbook、脚本或 guard 已准备，但尚未完成真实成功样本。 |
| `PREPARED_HANDOFF` | 交接文档、替换矩阵或验收口径已准备，但真实替换仍由公司或后续阶段完成。 |
| `BLOCKED_EXTERNAL` | 需要公司系统、公司内网、外部账号权限、真实凭据或公司开发接入。 |
| `DECISION_REQUIRED` | 需要用户、公司前端、公司运维或产品负责人确认策略。 |
| `NOT_STARTED` | 尚未进入实现或验收。 |

## 3. 当前状态矩阵

| 事项 | 当前状态 | 责任方 | 证据 / 入口 | 不能过度宣称 |
|---|---|---|---|---|
| 仓库与文档基线 | `READY_LOCAL` | 本项目 | `README.md`、`AGENTS.md`、`docs/project-progress.md`、`docs/checklists/local-commercial-delivery-acceptance.md` | 只能说当前口径已对齐；不能说后续前端、生产部署或公司接入决策已冻结。 |
| 本地基础设施与编排边界 | `READY_LOCAL` | 本项目 | `deploy/docker-compose.yml`、`docs/runbook/local-development.md`、`docs/checklists/local-commercial-delivery-audit-2026-06-06.md` | 只能说本地 Docker / Outbox / Temporal / 对象存储边界可验证；不能等同于公司服务器部署方案。 |
| 本地 Mock 作曲主链路 | `READY_LOCAL` | 本项目 | `scripts/smoke/api-main-flow.sh`、`docs/checklists/local-commercial-delivery-audit-2026-06-06.md` | 只证明 Mock Provider 链路，不证明真实供应商成功。 |
| OpenAPI v0.1 运行时契约 | `READY_LOCAL` | 本项目 | `scripts/smoke/openapi-contract.sh`、`docs/specs/openapi-contract-smoke-v0.1.md` | 只证明当前后端契约对拍，不证明公司系统已接。 |
| 后端本地商用组合验收 | `READY_LOCAL` | 本项目 | `scripts/smoke/local-commercial-backend-acceptance-stack.sh`、`docs/specs/local-commercial-backend-acceptance-stack-smoke-v0.1.md` | 只组合复验本地 Mock 主链路、OpenAPI、readiness 和发布包审核阻断；不证明真实模型、真实 MP4 local-process 或公司系统接入。 |
| 本地商用完整验收栈 | `READY_LOCAL` | 本项目 | `scripts/smoke/local-commercial-full-acceptance-stack.sh`、`docs/specs/local-commercial-full-acceptance-stack-smoke-v0.1.md`、`docs/checklists/local-commercial-delivery-audit-2026-06-06.md` | 2026-06-07 06:51 CST 复跑通过，组合复验后端 Mock 基线、local-process MP4 和 Claude 前端真实后端 UI；仍不证明真实模型或公司系统接入。 |
| Claude 前端原型真实后端模式 | `READY_LOCAL` | 本项目 / 前端原型提供方 | `prototypes/Claude-web-v1/scripts/smoke-real-backend.mjs`、`docs/adr/0003-frontend-delivery-track.md` | “真实后端模式”不等于真实模型成功出歌。 |
| Claude 前端动作矩阵 | `READY_LOCAL` | 本项目 / 前端原型提供方 | `docs/specs/frontend-action-matrix-smoke-v0.1.md`、`prototypes/Claude-web-v1/src/pages/work/FailedView.test.tsx`、`prototypes/Claude-web-v1/src/pages/work/FinishedView.test.tsx` | 组件测试证明 `available_actions` 驱动和异常动作渲染；后端 `PACKAGE_BLOCKED` 自然触发样本见下一行。 |
| Mock 发布包审核阻断 | `READY_LOCAL` | 本项目 | `docs/specs/mock-publish-package-block-smoke-v0.1.md`、`scripts/smoke/api-package-blocked-flow.sh` | 只证明 Mock 审核 Adapter 的发布包预检阻断和状态收口；不证明公司真实审核系统已接入。 |
| 正式前端承接目录 | `DECISION_REQUIRED` | 用户 / 公司前端 | `docs/adr/0003-frontend-delivery-track.md` | `apps/web` 仍是 scaffold，当前可验收对象是 `prototypes/Claude-web-v1`。 |
| 本地 MP4 成片 | `READY_LOCAL` | 本项目 | `RENDER_WORKER_MODE=local-process EXPECT_RENDER_WORKER=local-process scripts/smoke/api-main-flow.sh`、`docs/specs/render-worker-local-process-integration-v0.1.md` | 本地进程模式可跑通，不等于生产 render-worker 部署形态已定。 |
| 公司 Adapter readiness | `READY_LOCAL` | 本项目 | `scripts/smoke/company-adapter-readiness-smoke.sh`、`docs/specs/company-adapter-readiness-smoke-v0.1.md` | 只证明边界和 Mock 状态清楚，不证明公司真实系统已实现。 |
| 交付证据只读审计 | `READY_LOCAL` | 本项目 | `scripts/smoke/local-delivery-evidence-audit.sh`、`docs/specs/local-delivery-evidence-audit-v0.1.md` | 只检查交付证据、脚本权限、安全口径和密钥形态，不替代运行时 smoke 或人工验收清单。 |
| 公司交接包索引 | `PREPARED_HANDOFF` | 本项目 | `docs/handover/company-delivery-package-v0.1.md`、`scripts/smoke/company-handoff-package-audit.sh` | 只提供公司开发第一阅读入口和交接顺序，不代表公司真实 Adapter 已替换。 |
| 公司 Adapter 替换资料 | `PREPARED_HANDOFF` | 本项目 | `docs/handover/company-adapter-deployment-handoff-v0.1.md`、`docs/checklists/company-adapter-replacement-readiness.md` | 只能说交接资料和替换矩阵齐了；不能说五类真实 Adapter 已替换。 |
| 公司账号、审核、权益、发布、分享 | `BLOCKED_EXTERNAL` | 公司开发 / 公司社区系统 | `docs/handover/company-adapter-deployment-handoff-v0.1.md`、`docs/checklists/company-adapter-replacement-readiness.md` | 本平台不重做公司社区、分享、互动或推荐流。 |
| 真实音乐成功样本 | `READY_LOCAL` | 本项目 + 用户安全注入凭据 | `docs/integrations/real-model-smoke-evidence-log.md`、`docs/project-progress.md` | Yunwu Suno 公网样本已成功；DreamMaker Suno / MiniMax 生产目标仍未成功验证。 |
| DreamMaker Suno 真实音乐 | `PREPARED_SMOKE` | 本项目 + 用户 / 公司内网与凭据 | `docs/runbook/dreammaker-controlled-real-integration.md`、`scripts/smoke/dreammaker-real-music-stack-smoke.sh` | 曾收到 HTTP 403，不能宣称真实成功；生产仍保留 DreamMaker。 |
| Yunwu Suno 公网联调 | `READY_LOCAL` | 本项目 + 用户安全注入凭据 | `docs/runbook/yunwu-suno-controlled-real-integration.md`、`scripts/smoke/yunwu-suno-real-music-stack-smoke.sh`、`docs/integrations/real-model-smoke-evidence-log.md` | 只作为当前公网联调，不替代 DreamMaker 生产路径。 |
| Yunwu timestamped lyrics 字幕时间轴 | `PREPARED_SMOKE` | 本项目 + 用户安全注入凭据 | `scripts/smoke/yunwu-suno-timestamped-lyrics-smoke.sh`、`docs/runbook/yunwu-suno-controlled-real-integration.md`、`docs/integrations/real-model-smoke-evidence-log.md` | 下一条 `chirp-fenix` 真实音乐成功后验证；未通过前不能承诺精确字幕同步。 |
| DreamMaker MiniMax 真实音乐 | `PREPARED_SMOKE` | 本项目 + 用户 / 公司内网与凭据 | `docs/checklists/dreammaker-real-integration-acceptance.md`、`docs/runbook/dreammaker-controlled-real-integration.md` | 仍未完成真实 MiniMax 成功样本。 |
| DeepSeek v4Pro 真实写词 | `READY_LOCAL` | 本项目 + 用户安全注入凭据 | `modules/deepseek/src/main/java/com/yanyun/music/deepseek/RealDeepSeekLyricsClient.java`、`docs/runbook/deepseek-controlled-real-integration.md`、`scripts/smoke/deepseek-real-lyrics-stack-smoke.sh`、`scripts/smoke/deepseek-real-lyrics-smoke.sh`、`docs/specs/deepseek-real-lyrics-stack-smoke-v0.1.md`、`docs/integrations/real-model-smoke-evidence-log.md` | 公网单样本已成功；默认仍可用 Mock 写词。 |
| Image 2 真实封面 | `READY_LOCAL` | 本项目 + 用户安全注入 WellAPI 或 DreamMaker 凭据 | `scripts/smoke/wellapi-image2-real-cover-stack-smoke.sh`、`scripts/smoke/dreammaker-image2-real-cover-stack-smoke.sh`、`docs/runbook/image2-controlled-real-integration.md`、`docs/integrations/real-model-smoke-evidence-log.md` | WellAPI 公网样本已成功；DreamMaker Image 2 是生产目标且尚未成功验证。 |
| 真实模型联调预检 | `READY_LOCAL` | 本项目 | `scripts/smoke/real-model-readiness-preflight.sh`、`docs/specs/real-model-readiness-preflight-v0.1.md` | 只检查本地变量和可选 readiness，不证明真实供应商成功。 |
| 真实模型受控 smoke 总入口 | `READY_LOCAL` | 本项目 | `scripts/smoke/real-model-controlled-smoke.sh`、`docs/specs/real-model-controlled-smoke-index-v0.1.md` | 默认只列矩阵/计划；`execute` 仍需要双重显式开关，不代表真实模型已成功。 |
| 真实模型安全门矩阵审计 | `READY_LOCAL` | 本项目 | `scripts/smoke/real-model-safety-gates-audit.sh`、`docs/specs/real-model-safety-gates-audit-v0.1.md` | 只证明所有真实模型 target 的全局 gate 和目标 `ALLOW_*` gate 未被绕过，不证明真实供应商成功。 |
| 真实模型 smoke 脱敏证据日志 | `READY_LOCAL` | 本项目 | `docs/integrations/real-model-smoke-evidence-log.md`、`scripts/smoke/real-model-evidence-log-audit.sh` | 只证明证据留痕格式、脱敏规则和 DreamMaker 生产保留口径齐全，不证明真实供应商成功。 |
| 公网真实完整体验 smoke | `READY_LOCAL` | 本项目 + 用户安全注入凭据 | `docs/specs/public-real-full-experience-smoke-v0.1.md`、`scripts/smoke/public-real-full-experience-stack.sh`、`docs/integrations/real-model-smoke-evidence-log.md` | 组合 DeepSeek、Yunwu Suno、WellAPI Image 2、local-process MP4 和 Claude Web v1 已成功一条样本；仅用于公网首测，不能替代 DreamMaker 生产验证。 |
| 生产 DreamMaker 默认约束 | `READY_LOCAL` | 本项目 | `deploy/env.production.example`、`scripts/smoke/production-provider-defaults-audit.sh`、`docs/specs/production-dreammaker-provider-defaults-v0.1.md` | 只证明生产 profile / fallback / 交接口径默认 DreamMaker；不代表真实 DreamMaker 已在当前公网环境成功调用。 |
| 公司部署 readiness 静态审计 | `READY_LOCAL` | 本项目 | `scripts/smoke/company-deployment-readiness-audit.sh`、`docs/specs/company-deployment-readiness-audit-v0.1.md` | 只证明本地基础设施 compose、应用 Dockerfile、监控配置和部署交接文档齐全；不代表公司生产拓扑已确定或已部署。 |
| Stepwise recording Temporal 验证 | `READY_LOCAL` | 本项目 | `scripts/smoke/temporal-stepwise-recording.sh`、`docs/specs/stepwise-temporal-production-state-advancement-v0.1.md` | 只证明 outbox 可启动分步 workflow 且 step audit 写入；不推进作品到 `GENERATED / PACKAGE_READY`，不能作为用户实测或生产发布包完成证据。 |
| Stepwise production 分步生产态 | `PREPARED_HANDOFF` | 本项目 | `docs/handover/stepwise-production-implementation-task-package-v0.1.md`、`scripts/smoke/stepwise-production-boundary-audit.sh` | 当前只是任务包和边界审计已准备；`stepwise-production` 尚未实现，不能标为 `READY_LOCAL`。 |
| 生产部署形态 | `DECISION_REQUIRED` | 公司开发 / 运维 | `docs/handover/company-adapter-deployment-handoff-v0.1.md` | 本地 Docker/进程验证不等于公司服务器部署方案已确定。 |

## 4. 建议下一步执行顺序

1. 用户本地实测先用 `prototypes/Claude-web-v1 + music-api`，保持 `MUSIC_PROVIDER=mock`，验证完整产品体验和发布交接文案。
2. 若要测真实 AI 出歌，先运行 `TARGET=yunwu-suno MODE=plan scripts/smoke/real-model-controlled-smoke.sh` 和 `TARGET=yunwu-suno MODE=preflight scripts/smoke/real-model-controlled-smoke.sh`，再跑单作品 Yunwu Suno 公网受控 smoke；成功后用 `scripts/smoke/yunwu-suno-timestamped-lyrics-smoke.sh` 验证时间轴歌词。进入公司内网或生产联调时再切回 DreamMaker Suno / MiniMax。
3. 若要测真实写词，先运行 `TARGET=deepseek MODE=plan scripts/smoke/real-model-controlled-smoke.sh` 和 `TARGET=deepseek MODE=preflight scripts/smoke/real-model-controlled-smoke.sh`，再用 `ALLOW_REAL_MODEL_SMOKE=1 ALLOW_DEEPSEEK_REAL_SMOKE=1 TARGET=deepseek MODE=execute scripts/smoke/real-model-controlled-smoke.sh` 跑一条样本，不同时打开真实音乐和真实 Image 2。
4. 若要测真实封面，当前公网可先运行 `TARGET=wellapi-image2 MODE=plan/preflight scripts/smoke/real-model-controlled-smoke.sh`，再跑 WellAPI Image2 单作品受控 smoke；生产路径运行 `TARGET=dreammaker-image2 MODE=plan/preflight scripts/smoke/real-model-controlled-smoke.sh`，再用 `ALLOW_REAL_MODEL_SMOKE=1 ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE=1 TARGET=dreammaker-image2 MODE=execute scripts/smoke/real-model-controlled-smoke.sh` 验证 DreamMaker Image 2。
5. 公网完整体验首测时，先保证 DeepSeek、Yunwu Suno、WellAPI Image 2 的计划/预检均可解释，再运行 `TARGET=public-real-full-experience MODE=plan/preflight scripts/smoke/real-model-controlled-smoke.sh`；执行单作品组合样本时使用 `ALLOW_REAL_MODEL_SMOKE=1 ALLOW_PUBLIC_REAL_FULL_EXPERIENCE=1 TARGET=public-real-full-experience MODE=execute scripts/smoke/real-model-controlled-smoke.sh`，凭据只能来自当前 shell 或交互式静默输入。
6. 前端交付前必须决定 `prototypes/Claude-web-v1` 的去向：保留原型、迁移到 `apps/web`，或由公司前端按原型重建。
7. 公司开发接入前，按公司 Adapter handoff 文档替换账号、审核、权益、发布、分享边界，并复跑 readiness、OpenAPI、主链路和前端 smoke。

## 5. 交付给公司前必须说清楚

- 本平台交付的是 AI 创作、作词、出歌、封面、视频、预览、发布包和交接状态。
- 公司系统负责真实账号、审核、权益、社区发布、分享、互动和推荐流。
- `mark-fetched` 只表示“发布素材已交接给公司发布流程”，不表示社区发布成功。
- 真实凭据只允许通过 shell、部署 Secret 或公司配置中心注入，不得写入仓库、文档、截图、测试或日志。
- 自动化测试默认不得调用真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统。

## 6. 关联验收入口

- 完整验收清单：`docs/checklists/local-commercial-delivery-acceptance.md`
- 最新走查记录：`docs/checklists/local-commercial-delivery-audit-2026-06-06.md`
- 公司 Adapter 交接：`docs/handover/company-adapter-deployment-handoff-v0.1.md`
- 公司交接包索引：`docs/handover/company-delivery-package-v0.1.md`
- 公司 Adapter 替换清单：`docs/checklists/company-adapter-replacement-readiness.md`
- 真实模型 smoke 总入口：`scripts/smoke/real-model-controlled-smoke.sh`
- 真实模型 smoke 脱敏证据日志：`docs/integrations/real-model-smoke-evidence-log.md`
- 真实模型 smoke 脱敏证据审计：`scripts/smoke/real-model-evidence-log-audit.sh`
- 公网真实完整体验规格：`docs/specs/public-real-full-experience-smoke-v0.1.md`
- 公网真实完整体验脚本：`scripts/smoke/public-real-full-experience-stack.sh`
- 当前长期 Goal 完成度审计：`docs/checklists/local-commercial-goal-completion-audit-2026-06-07.md`
- Mock 发布包审核阻断 smoke：`scripts/smoke/api-package-blocked-flow.sh`
- 后端本地商用组合验收：`scripts/smoke/local-commercial-backend-acceptance-stack.sh`
- 本地商用完整验收栈：`scripts/smoke/local-commercial-full-acceptance-stack.sh`
- 交付证据只读审计：`scripts/smoke/local-delivery-evidence-audit.sh`
- 生产 DreamMaker 默认审计：`scripts/smoke/production-provider-defaults-audit.sh`
- 公司部署 readiness 静态审计：`scripts/smoke/company-deployment-readiness-audit.sh`
- 公司交接包审计：`scripts/smoke/company-handoff-package-audit.sh`
- Stepwise production 边界审计：`scripts/smoke/stepwise-production-boundary-audit.sh`
- 项目进度记录：`docs/project-progress.md`
