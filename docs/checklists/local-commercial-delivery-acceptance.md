# 本地商用闭环交付验收清单

版本：v0.1
更新时间：2026-06-07 05:48 CST
适用范围：本地完整跑通后，交给公司开发替换真实账号、审核、权益、发布、分享系统并部署到公司服务器前的交付检查。

## 使用方式

- 每次进入公司交接、真实模型联调或部署准备前，先按本清单走查。
- 勾选项需要有可追溯证据：命令输出、接口响应、截图、日志摘要或数据库抽查结果。
- 真实密钥、JWT、Cookie、用户 token、供应商原始 payload 不得写入本清单、日志、截图或提交。
- 本清单不替代 `docs/runbook/dreammaker-controlled-real-integration.md`、`docs/runbook/deepseek-controlled-real-integration.md` 和 `docs/runbook/image2-controlled-real-integration.md`；真实 Suno / MiniMax / DeepSeek / Image 2 联调仍按对应 Runbook 执行。
- 当前状态汇总见 `docs/handover/local-commercial-delivery-status-v0.1.md`。本清单是最终 gate；状态说明用于区分 `READY_LOCAL`、`PREPARED_SMOKE`、`PREPARED_HANDOFF`、`BLOCKED_EXTERNAL` 和 `DECISION_REQUIRED`，避免把本地 Mock 通过误写成真实生产完成。

## A. 仓库与文档基线

- [ ] `git status --short` 无未解释的改动；临时截图、`.playwright-mcp/`、构建产物和密钥文件未进入 Git。
- [ ] `README.md`、`docs/project-progress.md`、运行手册和交接文档的当前阶段一致。
- [ ] `docs/handover/local-commercial-delivery-status-v0.1.md` 已同步当前阶段，且没有把 Mock / 受控 smoke 准备项过度描述为真实生产完成。
- [ ] `scripts/smoke/local-delivery-evidence-audit.sh` 通过；若用于正式交接前 gate，应设置 `STRICT_GIT_CLEAN=true`。
- [ ] `scripts/smoke/production-provider-defaults-audit.sh` 通过，证明生产 profile、生产样例、Java fallback 和 readiness 默认值都保留 DreamMaker 为生产目标。
- [ ] `scripts/smoke/company-deployment-readiness-audit.sh` 通过，证明本地基础设施 compose、应用 Dockerfile、监控 scrape、生产 env 样例和部署交接文档齐全。
- [ ] `docs/handover/company-delivery-package-v0.1.md` 已作为公司开发第一阅读入口，且 `scripts/smoke/company-handoff-package-audit.sh` 通过。
- [ ] 前端承接口径已按 `docs/adr/0003-frontend-delivery-track.md` 确认：当前验收对象是 `prototypes/Claude-web-v1`，正式 `apps/web` 是否承接需单独决策。
- [ ] `scripts/smoke/local-commercial-backend-acceptance-stack.sh` 通过，证明后端本地 Mock 主链路、OpenAPI 契约、公司 Adapter readiness 和发布包审核阻断可组合复验。
- [ ] `scripts/smoke/local-commercial-full-acceptance-stack.sh` 通过，证明后端基线、local-process MP4 和 Claude 前端真实后端模式可组合复验。
- [ ] `scripts/smoke/openapi-contract.sh` 通过，证明 `docs/api/openapi-v0.1.yaml` 与当前后端主响应字段、状态、错误和发布包契约一致。
- [ ] 阶段性验收完成后已更新 `docs/project-progress.md`。

## B. 本地基础设施

- [ ] Docker Compose 可启动 PostgreSQL、Redis、Temporal、MinIO、OpenSearch、Prometheus、Grafana。
- [ ] PostgreSQL migration 可完整应用，重复启动不报错。
- [ ] MinIO/S3 发布包对象可写入、读取和刷新 URL。
- [ ] Temporal worker 与 API 可分进程启动，并能通过 readiness 或日志证明连接成功。

## C. 后端主链路

- [ ] `./gradlew test` 通过。
- [ ] `./gradlew spotlessCheck` 通过。
- [ ] `./gradlew :apps:music-api:bootJar` 通过。
- [ ] API `/health` 与 `/actuator/health` 返回健康。
- [ ] `scripts/smoke/api-main-flow.sh` 在同步 Mock 模式通过。
- [ ] `scripts/smoke/api-main-flow.sh` 在 `RENDER_WORKER_MODE=local-process` 模式通过，并用 `ffprobe` 验证 MP4。
- [ ] `scripts/smoke/openapi-contract.sh` 在同步 Mock 模式通过。
- [ ] `MOCK_MODERATION_PUBLISH_PACKAGE_BLOCKED_USER_IDS=mock_package_block_smoke` 启动 API 后，`scripts/smoke/api-package-blocked-flow.sh` 通过，证明发布包交接前审核阻断会收口到 `PACKAGE_BLOCKED`、`CONTACT_SUPPORT` 和 `RETURN_TO_EDIT`。
- [ ] `scripts/smoke/local-commercial-backend-acceptance-stack.sh` 在 `8080` 空闲、Docker 基础设施已启动时通过；该脚本必须保持真实 DreamMaker、Yunwu、WellAPI、DeepSeek、Image 2 和公司系统调用关闭。
- [ ] `scripts/smoke/local-commercial-full-acceptance-stack.sh` 在 `8080` 和前端 smoke 端口空闲时通过；该脚本必须覆盖后端组合验收、local-process MP4 和前端真实后端 UI smoke。
- [ ] 创建作品、确认出歌、获取发布包、刷新 URL、标记交接、作品列表、失败重试均可复验。
- [ ] `Idempotency-Key` 成功重放和冲突 409 语义通过复验。

## D. 前端创作工作台

- [ ] `cd prototypes/Claude-web-v1 && npm test` 通过。
- [ ] `npm run typecheck` 通过。
- [ ] `npm run build` 通过。
- [ ] `npm run smoke:real-backend` 在不加 `?mock=1` 的真实后端模式通过。
- [ ] 390px 移动端和 1440px 桌面端无横向溢出、按钮重叠和关键内容遮挡。
- [ ] 所有按钮由 `available_actions` 驱动，不由前端猜状态。
- [ ] 动作矩阵组件测试覆盖 `RETRY_COVER`、`RERENDER_VIDEO`、`RETURN_TO_EDIT`、`CONTACT_SUPPORT`、`PACKAGE_BLOCKED`，且不要求真实模型或公司系统。
- [ ] 第三次 AI 润色/续写 409 显示友好文案，并保留请求编号。
- [ ] 成品页展示媒体、交接下载链接、刷新 URL、标记已交接和已交接状态。
- [ ] 失败页展示用户友好的失败原因、推荐动作、重试次数，并能执行可用重试动作；内部失败码不直接暴露给普通用户。

## E. 真实模型联调准备

- [ ] `DREAMMAKER_REAL_CALLS_ENABLED=false` 是默认状态。
- [ ] Suno / MiniMax 真实调用前已确认联调窗口、成本上限、回滚方式和负责人。
- [ ] DreamMaker AK/SK 只通过当前 shell 或安全配置系统注入，不写入仓库或文档。
- [ ] 真实调用必须使用 outbox + Temporal worker，不在默认同步 API 线程中执行。
- [ ] runtime guard 已验证：`DREAMMAKER_REAL_CALLS_ENABLED=true` 时，`suno` / `minimax` 不允许在 `sync` 模式确认或重试。
- [ ] 真实模型 smoke 前已先运行 `scripts/smoke/real-model-controlled-smoke.sh` 查看目标矩阵/计划，再通过 `MODE=preflight` 做只读预检，且未打印真实密钥。
- [ ] 真实模型安全门矩阵审计 `scripts/smoke/real-model-safety-gates-audit.sh` 通过，证明所有真实模型 target 仍需要全局 gate 和目标 `ALLOW_*` gate。
- [ ] 若使用 `MODE=execute`，已同时设置 `ALLOW_REAL_MODEL_SMOKE=1` 和目标脚本自己的 `ALLOW_*` 开关，并记录目标供应商、执行窗口和回滚方式。
- [ ] `agent_runs` 可记录 Agent 调用摘要，且只包含 hash、模型名、模板版本、状态、耗时和脱敏失败信息，不保存完整 Prompt、用户原文或密钥。
- [ ] DeepSeek 真实写词前已按 `docs/checklists/deepseek-real-integration-acceptance.md` 确认双开关、真实客户端、密钥注入、日志脱敏和回滚方式；首次单样本 smoke 可通过 `ALLOW_REAL_MODEL_SMOKE=1 ALLOW_DEEPSEEK_REAL_SMOKE=1 TARGET=deepseek MODE=execute scripts/smoke/real-model-controlled-smoke.sh` 执行，且音乐、封面、DreamMaker、Yunwu、WellAPI 和公司 Adapter 必须保持 Mock 或关闭。
- [ ] Image 2 真实封面前已按 `docs/checklists/image2-real-integration-acceptance.md` 确认当前后端凭据、真实客户端、对象存储导入、失败收口/兜底策略、日志脱敏和回滚方式；当前公网联调用 WellAPI，正式生产目标 DreamMaker Image 2 路径必须保留。
- [ ] 公司内网或生产环境启用真实供应商前，已使用 `deploy/env.production.example` 或公司配置中心等价变量名，并确认 `SPRING_PROFILES_ACTIVE=prod`、`SUNO_BACKEND=dreammaker`、`IMAGE2_BACKEND=dreammaker`。
- [ ] Suno 成功路径和 MiniMax 成功路径分别按 `docs/checklists/dreammaker-real-integration-acceptance.md` 验收。
- [ ] 首次手动真实音乐 smoke 可先按 `docs/checklists/dreammaker-real-music-smoke-10min.md`、`scripts/smoke/dreammaker-real-music-smoke.sh` 或当前公网 `scripts/smoke/yunwu-suno-real-music-stack-smoke.sh` 执行，确认是真的打到供应商而不是仍在 Mock；DreamMaker 仍是正式生产目标。
- [ ] 首次手动真实封面 smoke 可先按 `ALLOW_WELLAPI_IMAGE2_REAL_SMOKE=1 scripts/smoke/wellapi-image2-real-cover-stack-smoke.sh` 执行公网路径；生产目标 DreamMaker Image 2 按 `ALLOW_REAL_MODEL_SMOKE=1 ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE=1 TARGET=dreammaker-image2 MODE=execute scripts/smoke/real-model-controlled-smoke.sh` 执行，确认只打开 Image 2，音乐、DeepSeek、Yunwu、render-worker 和公司 Adapter 仍保持 Mock。
- [ ] 真实失败码、限流、超时、音频 URL 过期和计费样本已脱敏记录到集成跟踪文档。

## F. 公司 Adapter 交接

- [ ] `/internal/integration-readiness` 可访问，且每个组件的 `status`、`blocks_company_deployment`、`required_env_vars` 可解释。
- [ ] 公司 Adapter 替换按 `docs/checklists/company-adapter-replacement-readiness.md` 留下账号、审核、权益、发布、分享五类证据。
- [ ] 公司部署前，账号、审核、权益、发布、分享相关 Mock 阻塞项已替换真实实现或由公司明确豁免。
- [ ] 公司账号接入不再信任 `X-Mock-User-Id`，作品读写按真实 `user_id` 隔离。
- [ ] 审核覆盖用户输入、歌词草案和发布包交接前预检，并能映射到用户可读错误。
- [ ] 权益实现 `lock / commit / release` 幂等语义，并能按 `work_id` 对账。
- [ ] 标记已交接只代表本平台产物交给社区发布流程，不代表社区发布成功。
- [ ] 公司侧发布、分享、互动、推荐流不在本平台重复实现。
- [ ] 分享若完全由公司社区系统承接，已在 `company_share` readiness 或公司交接记录中标为豁免 / 公司承接，不要求本平台新增分享 Adapter。

## G. 安全与日志

- [ ] 敏感信息扫描未发现真实 AK/SK、JWT、Cookie、私钥、用户 token 或生产配置。
- [ ] 用户可见错误不透出供应商或公司系统原始响应。
- [ ] `provider_calls.error_message`、应用日志和测试快照不包含 token/key 字段原文。
- [ ] 真实联调结束后已清理 shell 中的敏感环境变量。
- [ ] 大体积音视频、构建缓存、浏览器缓存和本地对象存储产物未进入 Git。

## H. 交付判定

- [ ] 本地 Mock 成功链路可一键复验。
- [ ] 后端组合验收入口可一键复验，且没有真实供应商或公司系统调用。
- [ ] 本地完整验收栈可一键复验，且没有真实供应商或公司系统调用。
- [ ] 本地真实 MP4 成片链路可复验。
- [ ] 前端真实后端模式可脚本化复验。
- [ ] Suno / MiniMax 至少各完成 1 次受控真实成功联调，或明确记录为公司接入前阻塞项。
- [ ] 正式前端承接策略已明确：保留 `prototypes/Claude-web-v1`、迁移到 `apps/web`，或公司前端重建。
- [ ] 公司 Adapter 替换清单、环境变量、smoke 步骤和禁止事项已交给公司开发。
- [ ] 本清单中未完成项已归类为阻塞项、风险项或后续项，并记录到 `docs/project-progress.md`。

## 证据记录模板

| 日期 | 检查项 | 结果 | 证据摘要 | 负责人 |
|---|---|---|---|---|
|  |  |  |  |  |
