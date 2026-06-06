# 本地商用闭环交付验收清单

版本：v0.1
更新时间：2026-06-06 16:53 CST
适用范围：本地完整跑通后，交给公司开发替换真实账号、审核、权益、发布、分享系统并部署到公司服务器前的交付检查。

## 使用方式

- 每次进入公司交接、真实模型联调或部署准备前，先按本清单走查。
- 勾选项需要有可追溯证据：命令输出、接口响应、截图、日志摘要或数据库抽查结果。
- 真实密钥、JWT、Cookie、用户 token、供应商原始 payload 不得写入本清单、日志、截图或提交。
- 本清单不替代 `docs/runbook/dreammaker-controlled-real-integration.md`，真实 Suno / MiniMax 联调仍按该 Runbook 执行。

## A. 仓库与文档基线

- [ ] `git status --short` 无未解释的改动；临时截图、`.playwright-mcp/`、构建产物和密钥文件未进入 Git。
- [ ] `README.md`、`docs/project-progress.md`、运行手册和交接文档的当前阶段一致。
- [ ] `docs/api/openapi-v0.1.yaml` 与前端、后端接口实现的字段和状态一致。
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
- [ ] 创建作品、确认出歌、获取发布包、刷新 URL、标记交接、作品列表、失败重试均可复验。
- [ ] `Idempotency-Key` 成功重放和冲突 409 语义通过复验。

## D. 前端创作工作台

- [ ] `cd prototypes/Claude-web-v1 && npm test` 通过。
- [ ] `npm run typecheck` 通过。
- [ ] `npm run build` 通过。
- [ ] `npm run smoke:real-backend` 在不加 `?mock=1` 的真实后端模式通过。
- [ ] 390px 移动端和 1440px 桌面端无横向溢出、按钮重叠和关键内容遮挡。
- [ ] 所有按钮由 `available_actions` 驱动，不由前端猜状态。
- [ ] 第三次 AI 润色/续写 409 显示友好文案，并保留请求编号。
- [ ] 成品页展示媒体、交接下载链接、刷新 URL、标记已交接和已交接状态。
- [ ] 失败页展示失败码、失败信息、推荐动作、重试次数，并能执行可用重试动作。

## E. 真实模型联调准备

- [ ] `DREAMMAKER_REAL_CALLS_ENABLED=false` 是默认状态。
- [ ] Suno / MiniMax 真实调用前已确认联调窗口、成本上限、回滚方式和负责人。
- [ ] DreamMaker AK/SK 只通过当前 shell 或安全配置系统注入，不写入仓库或文档。
- [ ] 真实调用必须使用 outbox + Temporal worker，不在默认同步 API 线程中执行。
- [ ] Suno 成功路径和 MiniMax 成功路径分别按 `docs/checklists/dreammaker-real-integration-acceptance.md` 验收。
- [ ] 真实失败码、限流、超时、音频 URL 过期和计费样本已脱敏记录到集成跟踪文档。

## F. 公司 Adapter 交接

- [ ] `/internal/integration-readiness` 可访问，且每个组件的 `status`、`blocks_company_deployment`、`required_env_vars` 可解释。
- [ ] 公司部署前，账号、审核、权益、发布、分享相关 Mock 阻塞项已替换真实实现或由公司明确豁免。
- [ ] 公司账号接入不再信任 `X-Mock-User-Id`，作品读写按真实 `user_id` 隔离。
- [ ] 审核覆盖用户输入、歌词草案和发布包交接前预检，并能映射到用户可读错误。
- [ ] 权益实现 `lock / commit / release` 幂等语义，并能按 `work_id` 对账。
- [ ] 标记已交接只代表本平台产物交给社区发布流程，不代表社区发布成功。
- [ ] 公司侧发布、分享、互动、推荐流不在本平台重复实现。

## G. 安全与日志

- [ ] 敏感信息扫描未发现真实 AK/SK、JWT、Cookie、私钥、用户 token 或生产配置。
- [ ] 用户可见错误不透出供应商或公司系统原始响应。
- [ ] `provider_calls.error_message`、应用日志和测试快照不包含 token/key 字段原文。
- [ ] 真实联调结束后已清理 shell 中的敏感环境变量。
- [ ] 大体积音视频、构建缓存、浏览器缓存和本地对象存储产物未进入 Git。

## H. 交付判定

- [ ] 本地 Mock 成功链路可一键复验。
- [ ] 本地真实 MP4 成片链路可复验。
- [ ] 前端真实后端模式可脚本化复验。
- [ ] Suno / MiniMax 至少各完成 1 次受控真实成功联调，或明确记录为公司接入前阻塞项。
- [ ] 公司 Adapter 替换清单、环境变量、smoke 步骤和禁止事项已交给公司开发。
- [ ] 本清单中未完成项已归类为阻塞项、风险项或后续项，并记录到 `docs/project-progress.md`。

## 证据记录模板

| 日期 | 检查项 | 结果 | 证据摘要 | 负责人 |
|---|---|---|---|---|
|  |  |  |  |  |
