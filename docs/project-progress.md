# 项目进度记录

更新时间：2026-06-07 03:53 CST

## 当前阶段

项目已完成第 8 批 MinIO/S3 发布包强化，并已按用户要求在前端并行开发期间推进第 10 批公司 Adapter 接入与部署交接准备和第 11 批 render-worker 本地进程调用边界：数据库 migration、Work 领域状态机、Mock Adapter 边界、OpenAPI v0.1 主路径 API、本地 Mock 作曲与发布包、DreamMaker / Suno / MiniMax 受控真实调用实现、Outbox v0.1、API outbox 到独立 Temporal worker 的编排边界、真实音乐 Provider 运行时硬保护、DeepSeek / Knowledge / Prompt / Lyrics 写词边界、OpenAI 兼容 `RealDeepSeekLyricsClient`、CoverGenerationService / VideoRenderService 媒体生成边界、DreamMaker Image2 与 WellAPI Image2 封面真实客户端、render-worker 本地 16:9 MP4 样例渲染、Java 可配置调用 render-worker CLI 的本地进程模式、发布包 JSON 的 local / S3-MinIO 可切换对象存储，以及内部公司接入 readiness 报告均已落地。2026-06-07 已执行一次真实 Suno 单作品 smoke，链路触达 DreamMaker 创建任务阶段但返回 HTTP 403；用户判断当前不在公司内网，DreamMaker 可能只支持内网环境。本地编排、失败收口、权益释放、provider_call 记录和进程清理正常。当前已保留 DreamMaker 为正式生产目标，同时新增 Yunwu Suno 公网联调后端与 WellAPI gpt-image-2 公网联调后端。真实 MiniMax、真实 DeepSeek、真实 Image 2 或公司系统调用仍未执行；所有真实外部调用仍需显式开关和安全注入凭据。

2026-06-07 03:53 CST 已补齐 DreamMaker Image2 生产目标单作品真实封面受控 smoke 入口：新增规格 `docs/specs/dreammaker-image2-real-cover-stack-smoke-v0.1.md`、一键脚本 `scripts/smoke/dreammaker-image2-real-cover-stack-smoke.sh` 和低层脚本 `scripts/smoke/dreammaker-image2-real-cover-smoke.sh`，并接入 `TARGET=dreammaker-image2 MODE=execute scripts/smoke/real-model-controlled-smoke.sh`。该入口必须同时设置 `ALLOW_REAL_MODEL_SMOKE=1` 和 `ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE=1`，只打开真实 DreamMaker Image2 封面，音乐、DeepSeek、Yunwu、render-worker 和公司 Adapter 仍保持 Mock 或关闭；低层脚本会验证 `image2_guard=real-calls-enabled/dreammaker`、`dreammaker_guard=READY_FOR_LOCAL`、封面 `provider=dreammaker-image2`、对象存储导入和 metadata 不保留供应商原始 URL/base64。README、Image2 runbook、验收清单、状态说明、公司交接包和只读审计脚本已同步。本轮尚未执行真实 DreamMaker Image2 调用。

2026-06-07 03:42 CST 已补齐 DeepSeek v4Pro 单样本真实写词受控 smoke 入口：新增规格 `docs/specs/deepseek-real-lyrics-smoke-v0.1.md` 和脚本 `scripts/smoke/deepseek-real-lyrics-smoke.sh`，并接入 `TARGET=deepseek MODE=execute scripts/smoke/real-model-controlled-smoke.sh`。该脚本只验证 `POST /api/v1/works/inspiration` 到 `LYRICS_READY / WAITING_CONFIRM` 和 `agent_runs` 中 `LyricsAgent` 非 Mock 模型摘要，不确认出歌、不生成音乐/封面/视频/发布包、不调用 DreamMaker/Yunwu/WellAPI/公司系统；音乐、封面、DreamMaker、Yunwu、WellAPI 和公司 Adapter 必须保持 Mock 或关闭。README、DeepSeek runbook、总体验收清单、状态说明、公司交接包和只读审计脚本已同步。DreamMaker 音乐与 DreamMaker Image 2 仍是正式生产目标接口，Yunwu / WellAPI 仅用于当前公网受控联调。本轮尚未执行真实 DeepSeek 调用。

2026-06-07 01:57 CST 已根据用户提供的新供应商资料完成真实模型联调后端切换准备：Suno 新增 `SUNO_BACKEND=yunwu|dreammaker`，默认当前公网联调用 `YunwuSunoMusicProvider`，通过 `YUNWU_BASE_URL=https://yunwu.ai`、`YUNWU_API_KEY`、`YUNWU_REAL_CALLS_ENABLED=true` 受控调用，DreamMaker `SunoMusicProvider` 仍保留为正式生产目标；Image 2 新增 `IMAGE2_BACKEND=wellapi|dreammaker`，默认当前公网联调用 `WellApiImage2CoverGenerationService`，通过 `WELLAPI_BASE_URL=https://wellapi.ai`、`WELLAPI_API_KEY`、`IMAGE_REAL_CALLS_ENABLED=true` 调用 `gpt-image-2`，DreamMaker Image2 仍保留为正式生产目标。`PROVIDER_AUTH_FAILED` 已进入后端领域状态机、OpenAPI 和前端类型，401/403/权限/签名/模型未开通类错误不再允许用户侧音乐重试。WellAPI 图片返回 URL 时会远程导入对象存储；若只返回 `b64_json`，workflow 会直接写对象存储并从 metadata 移除 base64 原文。已新增 `docs/runbook/yunwu-suno-controlled-real-integration.md`，并同步 README、Image2 runbook、安全规则、验收清单、开放问题和 DreamMaker 403 跟踪。阶段验证已通过 `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew spotlessApply :modules:suno:test :modules:image2:test :modules:work-domain:test :modules:config-center:test :apps:music-api:test --tests '*WorkServiceWorkflowDispatchTest' --tests '*MockSongProductionWorkflowTest' --tests '*IntegrationReadinessControllerTest' --tests '*DreamMakerHttpClientTest'`。

2026-06-07 02:08 CST 已补齐 Yunwu Suno 真实出歌受控 smoke 执行入口：新增规格 `docs/specs/yunwu-suno-real-music-stack-smoke-v0.1.md`、一键脚本 `scripts/smoke/yunwu-suno-real-music-stack-smoke.sh` 和低层脚本 `scripts/smoke/yunwu-suno-real-music-smoke.sh`。一键脚本会在 `ALLOW_YUNWU_REAL_SMOKE=1` 时静默读取或使用当前 shell 的 `YUNWU_API_KEY`，启动 `music-worker` 与 `music-api`，强制 `SUNO_BACKEND=yunwu`、`YUNWU_REAL_CALLS_ENABLED=true`、`outbox + temporal`，创建 1 个作品并确认 `music_provider=suno`；结束后自动停止它启动的进程。低层脚本用于 API/worker 已手动启动的场景。两个脚本默认拒绝真实调用，不打印 API Key、Bearer、供应商原始 payload、供应商音频 URL 或真实 task id；数据库摘要只显示 trace 是否存在。当前尚未执行真实 Yunwu 调用，下一步实测时由操作者在 shell 中安全注入 `YUNWU_API_KEY` 后运行。

2026-06-07 02:11 CST 用户明确确认 DreamMaker 接口必须继续保留，正式生产会切回 DreamMaker。本轮已把该规则固化到项目 Agent 规则、`.env.example` 注释和 Yunwu smoke 规格：Yunwu / WellAPI 仅作为当前非公司内网环境下的公网联调后端，DreamMaker 音乐和 DreamMaker Image 2 仍是必须保留的正式生产目标路径，后续实现、重构、子 Agent 或前端/后端交接文档不得删除或弱化 DreamMaker。

2026-06-07 02:14 CST 已完成 Yunwu smoke 脚本和 DreamMaker 保留规则的安全验证：`bash -n scripts/smoke/yunwu-suno-real-music-smoke.sh scripts/smoke/yunwu-suno-real-music-stack-smoke.sh` 通过；两个脚本在未设置 `ALLOW_YUNWU_REAL_SMOKE=1` 时都会提前拒绝；一键脚本在显式允许但缺少 `YUNWU_API_KEY` 且非交互输入时会在启动服务前退出；`git diff --check` 通过；diff 敏感信息扫描未发现 `sk-...` 或长 Bearer token 形态。本轮未执行真实 Yunwu、DreamMaker、DeepSeek、Image 2 或公司系统调用。

2026-06-07 02:20 CST 已补齐 WellAPI Image2 真实封面受控 smoke 执行入口：新增规格 `docs/specs/wellapi-image2-real-cover-stack-smoke-v0.1.md`、一键脚本 `scripts/smoke/wellapi-image2-real-cover-stack-smoke.sh` 和低层脚本 `scripts/smoke/wellapi-image2-real-cover-smoke.sh`。一键脚本会在 `ALLOW_WELLAPI_IMAGE2_REAL_SMOKE=1` 时静默读取或使用当前 shell 的 `WELLAPI_API_KEY`，只启动 `music-api`，强制 `IMAGE_PROVIDER=image2`、`IMAGE2_BACKEND=wellapi`、`IMAGE_REAL_CALLS_ENABLED=true`，同时保持 `MUSIC_PROVIDER=mock`、DeepSeek 真实调用关闭、render-worker mock、公司 Adapter mock。低层脚本会检查 `image2_guard=READY_FOR_LOCAL / real-calls-enabled/wellapi` 和 `music_provider=mock`，创建 1 个作品并确认出歌，随后验证 `COVER` 资产来自 `wellapi-image2`、已导入平台对象存储、尺寸为 workflow 的 1920x1080，并且 metadata 未保留供应商原始 URL 或 inline base64。同步修正 Image2 runbook 和验收清单中“默认 2048x1152”的偏差：该值只是客户端兜底尺寸，当前完整 workflow 验收尺寸是 1920x1080。本轮未执行真实 WellAPI、DreamMaker、DeepSeek、Suno、MiniMax 或公司系统调用。

2026-06-07 02:24 CST 已完成 WellAPI Image2 smoke 脚本的非真实安全验证：`bash -n scripts/smoke/wellapi-image2-real-cover-smoke.sh scripts/smoke/wellapi-image2-real-cover-stack-smoke.sh` 通过；两个脚本在未设置 `ALLOW_WELLAPI_IMAGE2_REAL_SMOKE=1` 时都会提前拒绝；一键脚本在显式允许但缺少 `WELLAPI_API_KEY` 且非交互输入时会在启动 API 前退出；`git diff --check` 通过；diff 敏感信息扫描未发现 `sk-...` 或长 Bearer token 形态。本轮没有运行 Gradle 或前端构建，因为只新增脚本和文档，未改 Java/TypeScript 生产代码。

2026-06-07 02:29 CST 已补齐公司 Adapter readiness 只读 smoke 执行入口：新增规格 `docs/specs/company-adapter-readiness-smoke-v0.1.md` 和脚本 `scripts/smoke/company-adapter-readiness-smoke.sh`。该脚本只调用 `/health` 与 `/internal/integration-readiness`，默认本地模式下验证 `company_account`、`company_moderation`、`company_quota`、`company_publish`、`company_share` 均为 `MOCK_ONLY` 且 `blocks_company_deployment=true`，同时确认 `music_provider`、`render_worker`、`object_storage`、`workflow_dispatch`、`deepseek_guard`、`image2_guard`、`yunwu_suno_guard`、`dreammaker_guard` 等部署支撑组件存在，`required_env_vars` 只包含变量名，并扫描 readiness 响应中是否出现明显 `sk-...` 或长 Bearer token。同步 README、本地运行手册、公司 Adapter 替换清单和公司交接说明，把该脚本作为公司交接时的脱敏摘要证据入口。

2026-06-07 02:39 CST 已按用户最新确认继续强化 DreamMaker 生产保留规则：`scripts/smoke/company-adapter-readiness-smoke.sh` 现在会断言 `dreammaker_guard` 使用 `DreamMakerHttpClient`，并保留 `DREAMMAKER_REAL_CALLS_ENABLED`、`DREAMMAKER_API_BASE_URL`、`DREAMMAKER_ACCESS_KEY`、`DREAMMAKER_SECRET_KEY` 四个生产切换变量名；公司 Adapter 替换清单和交接说明同步写明 Yunwu / WellAPI 只是公网联调后端，正式生产切回 DreamMaker，后续不得删除 DreamMaker 音乐或 Image 2 接口边界。本轮验证通过 `bash -n scripts/smoke/company-adapter-readiness-smoke.sh`、`scripts/smoke/company-adapter-readiness-smoke.sh`（Mock API，只读调用 `/health` 与 `/internal/integration-readiness`）、`git diff --check` 和 diff 敏感信息扫描；验证结束后 `8080` / `8081` 无残留监听，未调用真实 DreamMaker、Yunwu、WellAPI、DeepSeek、Suno、MiniMax 或公司系统。

2026-06-07 02:49 CST 已补齐稳定交付状态说明：新增规格 `docs/specs/local-commercial-delivery-status-handoff-v0.1.md` 和交接页 `docs/handover/local-commercial-delivery-status-v0.1.md`，把当前能力按 `READY_LOCAL`、`PREPARED_SMOKE`、`PREPARED_HANDOFF`、`BLOCKED_EXTERNAL`、`DECISION_REQUIRED`、`NOT_STARTED` 分类。该状态页明确区分本地 Mock 闭环、OpenAPI 契约、Claude 前端原型、local-process MP4、公司 Adapter readiness 等已本地验证项；真实 DeepSeek / Image2 / Yunwu / DreamMaker 供应商入口已准备但真实成功样本仍待受控 smoke；公司账号、审核、权益、发布、分享和生产部署形态仍由公司接入或决策。同步 README、总体验收清单和公司 Adapter 交接说明，避免把本地 Mock 或公网临时联调路径过度描述为真实生产完成。本轮未调用真实外部模型或公司系统。

2026-06-07 03:01 CST 已补齐真实模型联调前只读预检入口：新增规格 `docs/specs/real-model-readiness-preflight-v0.1.md` 和脚本 `scripts/smoke/real-model-readiness-preflight.sh`。脚本支持 `TARGET=all|yunwu-suno|dreammaker-suno|dreammaker-minimax|deepseek|wellapi-image2|dreammaker-image2`，默认只输出本地环境摘要；`STRICT=true` 时会在目标缺少必要变量、真实音乐未配置 `outbox + temporal + dispatcher`、真实调用开关未打开或后端选择错误时失败。脚本不会启动服务、不会创建作品、不会访问数据库、不会调用 DreamMaker / Yunwu / WellAPI / DeepSeek / 公司系统，也不会打印真实密钥值；可选 `CHECK_API=true` 只读调用 `/health` 与 `/internal/integration-readiness` 并扫描明显密钥泄漏。README、DeepSeek/Yunwu/Image2/DreamMaker runbook、本地运行手册、总体验收清单和状态说明已接入该预检入口。本轮验证使用默认环境和假占位 key 运行，未触发真实外部调用。

2026-06-07 03:10 CST 已补齐真实模型受控 smoke 总入口：新增规格 `docs/specs/real-model-controlled-smoke-index-v0.1.md` 和脚本 `scripts/smoke/real-model-controlled-smoke.sh`。脚本支持 `MODE=list|plan|preflight|execute` 和 `TARGET=yunwu-suno|dreammaker-suno|dreammaker-minimax|deepseek|wellapi-image2|dreammaker-image2`；默认只列目标矩阵或打印执行计划，不调用供应商。`MODE=preflight` 会委托 `real-model-readiness-preflight.sh` 做严格只读预检；`MODE=execute` 必须显式设置 `ALLOW_REAL_MODEL_SMOKE=1`，会先自动跑严格只读预检，并且仍依赖各目标脚本自己的 `ALLOW_YUNWU_REAL_SMOKE`、`ALLOW_DREAMMAKER_REAL_SMOKE`、`ALLOW_DEEPSEEK_REAL_SMOKE`、`ALLOW_WELLAPI_IMAGE2_REAL_SMOKE` 或 `ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE`。DreamMaker Image 2 后续已接入单作品封面执行脚本；DeepSeek 后续已接入单样本写词执行脚本。README、本地运行手册、总体验收清单和交付状态说明已同步，继续强调 DreamMaker 是正式生产目标，Yunwu / WellAPI 只是公网受控联调路径。本轮未执行真实外部调用。

2026-06-07 03:22 CST 已补齐本地商用交付证据只读审计入口：新增规格 `docs/specs/local-delivery-evidence-audit-v0.1.md` 和脚本 `scripts/smoke/local-delivery-evidence-audit.sh`。脚本不启动 API、Docker、浏览器、worker，不访问数据库、对象存储、真实供应商或公司系统；只检查关键交付文档存在、关键 smoke 脚本可执行、DreamMaker 正式生产目标保留口径、Yunwu / WellAPI 公网联调口径、状态矩阵分类、真实模型受控 smoke 总入口、tracked 文件明显密钥形态和大体积 tracked 文件。默认工作区 dirty 只警告，正式交接前可用 `STRICT_GIT_CLEAN=true` 作为硬 gate。首次运行发现 README 的 WellAPI 公网联调口径不够显式，以及两个测试假 Bearer 文本会被仓库级密钥扫描命中；本轮已修正 README 文案并把测试假 token 改成不匹配真实密钥形态。审计默认模式已通过，本轮未执行真实外部调用。

2026-06-07 03:31 CST 已补齐公司开发交接包索引：新增规格 `docs/specs/company-handoff-package-index-v0.1.md`、公司第一阅读入口 `docs/handover/company-delivery-package-v0.1.md` 和只读脚本 `scripts/smoke/company-handoff-package-audit.sh`。交接包把本地可测能力、公司系统必须替换或承接的账号/审核/权益/发布/分享边界、`X-Mock-User-Id` 禁止生产信任、`mark-fetched` 语义、DreamMaker 正式生产目标、Yunwu / WellAPI 公网受控 smoke 路径、真实模型总入口和推荐证据命令集中到一页。公司交接包审计已通过，且已纳入 README、总体验收清单、公司 Adapter handoff 和本地交付状态页；本轮未启动服务、未调用真实供应商或公司系统。

第 9 批前端原型已由用户侧交付到 `prototypes/Claude-web-v1`，并在用户明确要求后由本 Agent 直接修复验收缺口：已补齐“我的作品”列表、发布交接信息、`PACKAGE_BLOCKED`、润色必填、`RETRY_COVER` / `RERENDER_VIDEO`、错误 `request_id`、关键状态展示和基础可访问性问题。该原型已通过 `npm test`、`npm run typecheck`、`npm run build`，并完成 390px / 1440px Playwright smoke 和真实后端模式 UI 联调：灵感成歌、润色/续写次数、409 友好提示、确认出歌、成品交接、作品列表、失败页重试恢复均已跑通。

真实后端 UI smoke 已脚本化到 `prototypes/Claude-web-v1/scripts/smoke-real-backend.mjs`，可通过 `npm run smoke:real-backend` 复验。脚本默认要求 API 已在 `8080` 启动，会自行启动 Vite 到 `5274`，并使用 Playwright Chromium 覆盖主成功链路和 `suno` 受控失败后的前端重试恢复。

为后续前后端联调提速，Mock 音乐 Provider 已新增可配置模拟音频时长：默认仍为 180000ms，保持 3 分钟商用口径；本地 smoke 可通过 `MOCK_MUSIC_DURATION_MS=1000` 临时压短，用于快速验证 Java 到 render-worker 的 MP4 成片链路。

本地主链路 smoke 已脚本化：新增 `scripts/smoke/api-main-flow.sh`，在 API 已启动后可一键验证健康检查、填词成歌、确认出歌、发布包获取/刷新/标记交接、readiness、PostgreSQL 状态、本地对象文件；local-process 模式下还会使用 `ffprobe` 验证 MP4。

本轮已补齐本地商用闭环交付验收清单 `docs/checklists/local-commercial-delivery-acceptance.md`，并修正 README 和 Claude Web v1 前端验收任务包中的过时状态；公司 Adapter 交接文档已增加总体验收清单入口。仓库已忽略 Playwright 本地缓存和验收截图，避免后续 `git status` 被临时产物污染。

2026-06-06 17:14 CST 已完成一次当前态本地商用闭环走查：后端 Gradle 测试/格式/bootJar、Claude Web v1 前端测试/typecheck/build、render-worker build/test、同步 Mock API smoke、真实后端 UI smoke、local-process MP4 smoke 均通过；验证结束后 `8080` 和 `5274` 无残留监听。走查证据已记录到 `docs/checklists/local-commercial-delivery-audit-2026-06-06.md`。

2026-06-06 17:25 CST 已根据用户确认的方向补充 AI 多 Agent / Worker 创作编排设计：后续真实模型接入不采用自由聊天式多 Agent，而采用确定性的 `SongProductionWorkflow` / Temporal 编排，加上专业 Agent Worker 与 Provider Adapter。Agent 负责创意理解、写词、提示词规划、质量评估和风险预检；Adapter 负责 Suno / MiniMax、Image 2、公司审核、对象存储和发布交接等确定性副作用。

2026-06-06 17:37 CST 已补齐 OpenAPI v0.1 运行时契约对拍 smoke：新增规格 `docs/specs/openapi-contract-smoke-v0.1.md` 和脚本 `scripts/smoke/openapi-contract.sh`，覆盖静态 OpenAPI path/schema/enum、用户信息、作品创建、作品详情、作品列表、润色/续写次数、确认出歌、封面重生、视频重渲、发布包获取/刷新/交接、统一错误、幂等冲突、作品不存在、`suno` 受控音乐失败和 mock 重试恢复。同步 Mock API 下已通过，作品 `1ed4a2dd-8cb7-4bb1-849f-fd188df8fea8`，受控失败/重试作品 `f65f1ead-f4e8-4e3b-85b0-dcd88d8d8aa0`。

2026-06-06 17:57 CST 已启动真实模型受控联调准备的 Agent Runtime 基础：新增 `modules:agent-runtime`、`agent_runs` 数据库表、`JdbcAgentRunRecorder` 和写词链路 `LyricsAgent` 审计记录。当前仍使用 Mock DeepSeek，不调用真实模型；每次 `DeepSeekLyricsClient.generate` 会记录 agent 名称、版本、operation、模型名、Prompt 模板版本、输入/输出 hash、状态、耗时和脱敏失败信息。HTTP smoke 已确认作品 `1ac8eab6-91d1-42af-9104-c3979842eca5` 写入 `LyricsAgent|v0.1|INSPIRATION|mock-deepseek-lyrics|SUCCEEDED`。

2026-06-06 18:21 CST 已按用户要求补充多 Agent 工程编排设计文档 `docs/specs/ai-agent-orchestration-engineering-design-v0.1.md`，明确后续按“确定性 Workflow + 专业 Agent Worker + Provider Adapter”推进；同时完成 `MusicPromptAgent` v0.1 首个 Mock 合约落点，把确认出歌后的音乐提示词规划接入 `SongProductionWorkflow` 和 `agent_runs` 审计。当前仍不调用真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统。HTTP smoke 已确认作品 `eec23007-95f0-4054-a75f-5cd372993d8f` 写入 `MusicPromptAgent|v0.1|MUSIC_PROMPT|mock-music-prompt|SUCCEEDED|true|true`。

2026-06-06 18:47 CST 已完成 `CreativeBriefAgent` v0.1 Mock 合约落点：新增规格 `docs/specs/creative-brief-agent-v0.1.md`，在写词链路调用 DeepSeek 前生成结构化创作简报并注入 Prompt 上下文，同时写入 `agent_runs` 审计。当前仍不调用真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统。HTTP smoke 已确认作品 `667bb236-e453-423e-b781-bc6b81ec26fe` 依次写入 `CreativeBriefAgent|v0.1|INSPIRATION|mock-creative-brief|SUCCEEDED`、`LyricsAgent|v0.1|INSPIRATION|mock-deepseek-lyrics|SUCCEEDED`、`MusicPromptAgent|v0.1|MUSIC_PROMPT|mock-music-prompt|SUCCEEDED`，并推进到 `GENERATED / PACKAGE_READY`。

2026-06-06 19:21 CST 已完成 `CoverPromptAgent` v0.1 Mock 合约落点：新增规格 `docs/specs/cover-prompt-agent-v0.1.md`，在生成封面资产前把歌曲标题、摘要、歌词、music prompt 和 cover prompt seed 转换为封面 visual prompt，并把 Agent 输出传给 `CoverGenerationService`。当前仍不调用真实 Image 2。HTTP smoke 已确认作品 `6ccd242f-4d24-4036-8644-c4fc842812b6` 写入 `CreativeBriefAgent`、`LyricsAgent`、`MusicPromptAgent`、`CoverPromptAgent` 四类审计，封面 metadata 包含 `CoverPromptAgent`、`visual_prompt`、`negative_prompt` 和 `cover.prompt.v1`。

2026-06-06 19:24 CST 已按用户确认的方向补充 Agent 架构方向基线 `docs/specs/ai-agent-architecture-direction-v0.1.md`：后续不做自由聊天式多 Agent，而采用确定性 Workflow + 专业 Agent Worker + Provider Adapter。该文档明确 Agent 清单、职责边界、质量门、审核门、真实模型分阶段接入、前端不可感知 Agent 内部状态、以及新增 Agent 和真实模型联调前必须满足的硬规则。

2026-06-06 19:42 CST 已完成 `QualityEvaluationAgent` v0.1 Mock 合约落点：新增规格 `docs/specs/quality-evaluation-agent-v0.1.md`，在发布包准备、发布包审核和发布包写入前执行发布包质量门，检查音频、封面、视频和 timeline 基础可用性。当前仍不调用真实质量评估模型或公司系统；质量门不通过时 Workflow 按 `PACKAGE_BUILD_FAILED` 收口并释放已锁权益。API smoke 已确认作品 `9c692850-da4f-4ffb-aa09-aeb89b2645c9` 进入 `GENERATED / PACKAGE_READY`，数据库 `agent_runs` 包含 `QualityEvaluationAgent|v0.1|PACKAGE_QUALITY_GATE|mock-quality-evaluation|SUCCEEDED|true|true`。

2026-06-06 20:02 CST 已完成 `ModerationAgent` v0.1 Mock 合约落点：新增规格 `docs/specs/moderation-agent-v0.1.md`，在 `MusicPromptAgent` 输出音乐 prompt 后、`MusicProvider.submit` 前执行 AI 预检。当前仍不调用真实审核模型或公司系统，也不替代公司 `ModerationAdapter`；Mock 预检命中 `[BLOCK]` 时会阻断音乐 Provider 调用，按 `MUSIC_GENERATION_FAILED` 收口并释放已锁权益。API smoke 已确认作品 `e736c1b7-fee8-4f5f-973f-6bb1c0eef384` 进入 `GENERATED / PACKAGE_READY`，数据库 `agent_runs` 包含 `ModerationAgent|v0.1|MUSIC_PROMPT_PRECHECK|mock-moderation-agent|SUCCEEDED|true|true`。

2026-06-06 20:24 CST 已完成 DeepSeek 真实受控联调文档准备：新增 `docs/runbook/deepseek-controlled-real-integration.md`、`docs/security/deepseek-secret-and-log-handling.md`、`docs/checklists/deepseek-real-integration-acceptance.md` 和 `docs/integrations/deepseek-open-questions-tracker.md`，并同步 README、`.env.example`、本地交付清单和第 6 批 DeepSeek 规格。`/internal/integration-readiness` 已新增 `deepseek_guard`，可展示真实 DeepSeek 开关仍处于 Mock 或真实客户端待实现阻塞状态。当前仍未实现 `RealDeepSeekLyricsClient`，也未调用真实 DeepSeek；等进入真实客户端实现与单样本联调阶段时，再向用户索取 URL、API Key 和协议细节。验证已通过 `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew spotlessApply :modules:config-center:test :apps:music-api:test --tests '*IntegrationReadiness*'`、`JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew spotlessCheck :apps:music-api:bootJar`、`git diff --check` 和本轮变更文件敏感信息扫描。

2026-06-06 20:48 CST 已完成 Image 2 真实受控联调文档准备：新增 `docs/runbook/image2-controlled-real-integration.md`、`docs/security/image2-secret-and-log-handling.md`、`docs/checklists/image2-real-integration-acceptance.md` 和 `docs/integrations/image2-open-questions-tracker.md`，并同步 README、`.env.example`、本地交付清单、第 7 批封面/视频规格和 `CoverPromptAgent` 规格。`/internal/integration-readiness` 已新增 `image2_guard`，可展示真实 Image 2 开关仍处于 Mock 或真实客户端待实现阻塞状态。当前仍未实现 `RealImage2CoverGenerationService`，也未调用真实 Image 2；等进入真实客户端实现与单样本联调阶段时，再向用户索取 URL、API Key 和协议细节。验证已通过 `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew spotlessApply :modules:config-center:test :apps:music-api:test --tests '*IntegrationReadiness*'` 和 `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew spotlessCheck :apps:music-api:bootJar`。

2026-06-06 21:05 CST 已补充真实模型阶段前的 Temporal activity 细化设计 `docs/specs/temporal-activity-decomposition-v0.1.md`，明确当前单一 `SongProductionActivities.produce` 兼容路径保留，但后续应按 `LockQuota`、`GenerateMusicPrompt`、`SubmitMusic`、`PollMusic`、`ImportAudio`、`GenerateCoverPrompt`、`GenerateCover`、`RenderVideo`、`EvaluatePackage`、`PreCheckPublishPackage`、`AssemblePublishPackage`、`CommitQuota` / `ReleaseQuota` 等步骤拆成可重试、可幂等、可定位失败的 activity。该规格不调用真实模型、不改变 OpenAPI，只作为后续拆分实现基线。

2026-06-06 21:18 CST 已落地 Temporal activity 细化 Phase 1 合约骨架：`modules:workflow` 新增 `SongProductionStepActivities`、`SongProductionActivityContext`、`SongProductionStepName` 和 `SongProductionStepResult`，先定义分步 activity、稳定幂等键和通用步骤结果，不接入现有 worker 执行路径。当前 `SongProductionActivities.produce` 单 activity 兼容路径仍保留，用户侧 OpenAPI 和本地主链路行为不变。`SongProductionStepActivitiesContractTest` 已覆盖幂等键、从 workflow input 派生 context、缺失 job id 拒绝、结果 references 防御性复制和 14 个计划 activity 方法。验证已通过 `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew spotlessApply :modules:workflow:test` 和 `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew :modules:workflow:test :apps:music-api:compileJava :apps:music-worker:compileJava spotlessCheck :apps:music-api:bootJar :apps:music-worker:bootJar`。

2026-06-06 21:32 CST 已新增 Temporal step audit 数据库基础：新增 Flyway migration `database/migrations/V202606062118__add_generation_job_steps.sql`，创建 `generation_job_steps` 表，记录 `job_id`、`work_id`、`step_name`、`idempotency_key`、`status`、`attempt_count`、`external_trace_id`、失败码、失败信息和起止时间，并用 `(job_id, step_name, idempotency_key)` 唯一约束保护 activity 重试幂等。`WorkRepository` 新增 `upsertGenerationJobStep` 与 `findGenerationJobSteps` 最小 API。当前仍未接入主 workflow 执行路径，不改变用户侧行为。验证已通过 `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew spotlessApply :modules:production:compileJava :apps:music-api:bootJar :apps:music-worker:bootJar`、`JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew spotlessCheck :modules:production:compileJava`，并确认新 migration 已进入 API / worker 构建资源。

2026-06-06 21:45 CST 已新增记录型 Mock step activity：`RecordingSongProductionStepActivities` 实现 `SongProductionStepActivities`，每个分步 activity 会生成稳定幂等键并写入 `generation_job_steps`，失败记录会复用 `AgentRunSanitizer` 脱敏失败信息并保留外部 trace id。当前仍未注册到 worker，也不替换现有 `SongProductionActivities.produce` 单 activity 路径，因此用户侧 OpenAPI、本地 Mock 主链路和当前 Temporal 执行行为不变。验证已通过 `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew spotlessApply :modules:production:test` 和强制重跑的 `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew --rerun-tasks :modules:production:test :apps:music-api:compileJava :apps:music-worker:compileJava spotlessCheck :apps:music-api:bootJar :apps:music-worker:bootJar`。

2026-06-06 22:00 CST 已新增受控分步 workflow 验证路径：`StepwiseSongProductionWorkflow` 会按 `LOCK_QUOTA -> GENERATE_MUSIC_PROMPT -> PRE_CHECK_MUSIC_PROMPT -> SUBMIT_MUSIC -> POLL_MUSIC -> IMPORT_AUDIO -> GENERATE_COVER_PROMPT -> GENERATE_COVER -> RENDER_VIDEO -> EVALUATE_PACKAGE -> PRE_CHECK_PUBLISH_PACKAGE -> ASSEMBLE_PUBLISH_PACKAGE -> COMMIT_QUOTA` 顺序调用 step activities；锁权益成功后的后续失败会调用 `RELEASE_QUOTA` 并返回原始失败码，发布预检阻断会映射为 `PACKAGE_BLOCKED`。该 workflow 当前只在测试中使用，`TemporalWorkerConfig` 仍注册旧的 `TemporalSongProductionWorkflowImpl` + `SongProductionActivities.produce`，因此不改变当前用户侧主链路。验证已通过强制重跑 `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew --rerun-tasks :modules:workflow:test :modules:production:test :apps:music-api:compileJava :apps:music-worker:compileJava spotlessCheck :apps:music-api:bootJar :apps:music-worker:bootJar`。

2026-06-06 22:15 CST 已新增 worker 侧可开关 stepwise 注册边界：`TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy|stepwise-recording` 默认保持 `legacy`，继续注册旧的 `TemporalSongProductionWorkflowImpl` 与 `SongProductionActivities.produce`；只有显式设置 `stepwise-recording` 时才注册 `StepwiseSongProductionWorkflow` 与 `RecordingSongProductionStepActivities`。本轮使用子 Agent 并行审查后采纳了“legacy 不应硬依赖 stepwise bean”的建议，已把 step activity 改为 `ObjectProvider` 懒获取，legacy 分支不会向 Spring 请求 step activity 实例。`.env.example` 和本地 runbook 已补充该开关及验收口径：该模式当前只验证 outbox 启动 Temporal workflow 与 `generation_job_steps` 写入 13 条成功记录，不推进作品到 `GENERATED / PACKAGE_READY`，不得用主链路发布包 smoke 断言。验证已通过 `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew spotlessApply :apps:music-worker:test :modules:workflow:test :modules:production:test :apps:music-worker:compileJava` 和强制重跑的 `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew --rerun-tasks :apps:music-worker:test :modules:workflow:test :modules:production:test :apps:music-api:compileJava :apps:music-worker:compileJava spotlessCheck :apps:music-api:bootJar :apps:music-worker:bootJar`。

2026-06-06 22:30 CST 已完成 `stepwise-recording` 本地 Temporal smoke，并修复启动中暴露的配置绑定回归：`TemporalWorkerProperties` record 增加便利构造器后，Spring Boot `@ConfigurationProperties` 绑定会寻找无参构造器导致 worker 启动失败；现已移除便利构造器，保留 canonical constructor 中的 `null -> legacy` 归一化，并新增 Binder 回归测试。smoke 使用 worker `TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=stepwise-recording` 与 API `MUSIC_WORKFLOW_DISPATCH_MODE=outbox / WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal`，作品 `577e5867-0a9a-419f-94ae-c236a0333b34`、job `eebd7ad7-0e24-488b-9f39-c8dd068299f3` 已确认 `workflow_outbox.status=SUCCEEDED`、`processed_at=true`、`generation_job_steps` 写入 13 条 `SUCCEEDED`；作品和 job 保持 `GENERATING / QUOTA_LOCKING / PACKAGE_NOT_READY`，符合当前受控验证边界。验证结束后 `8080` 和 `8081` 无监听进程。

2026-06-06 22:45 CST 已把 `stepwise-recording` 本地 Temporal smoke 脚本化：新增 `scripts/smoke/temporal-stepwise-recording.sh`，在 worker/API 已启动的前提下自动检查 API health、创建填词作品、outbox 确认出歌、等待 `workflow_outbox` 成功、验证 `generation_job_steps` 13 条 `SUCCEEDED`，并确认 work/job 保持当前受控边界 `RUNNING / QUOTA_LOCKING / GENERATING / QUOTA_LOCKING / PACKAGE_NOT_READY`。`docs/runbook/local-development.md` 已补充脚本入口，明确不要用主链路 `api-main-flow.sh` 验证 stepwise 模式。脚本化 smoke 已通过，作品 `5121f214-1c58-4a17-96fb-ec5be3ade020`、job `03b6d954-39ce-44ba-b441-9de49136347c`，验证结束后 `8080` 和 `8081` 无监听进程。

2026-06-06 22:50 CST 已根据当前 stepwise 录步模式和后续真实模型联调风险补充 `docs/specs/stepwise-temporal-production-state-advancement-v0.1.md`：明确 `sync + mock` 仍是用户当前实测主路径，`stepwise-recording` 只验证 Temporal 分步顺序和 step audit，不得误判为发布包完成链路；后续若要替代单 activity，应新增独立 `stepwise-production` 模式，分步推进 `works`、`generation_jobs`、`media_assets`、`publish_packages`、`quota_transactions` 等生产状态，并保持真实外部调用硬开关、幂等、脱敏和可恢复边界。并行只读侦察同时确认：`prototypes/Claude-web-v1 + music-api 同步 Mock` 已适合用户本地实测；Suno/MiniMax via DreamMaker 已具备最小真实调用实现和安全开关，但真实音乐 smoke 尚未执行，仍需用户明确进入真实联调时通过环境变量安全注入凭据。

2026-06-06 22:55 CST 已根据前端契约只读侦察结果修复用户实测前的 P1 问题：`prototypes/Claude-web-v1` 的作品列表分页契约从旧的 `total / has_more` 改为 OpenAPI 与后端实际返回的 `total_items / total_pages`，并新增分页回归测试；失败页不再向普通用户展示 `MUSIC_GENERATION_FAILED` 等内部失败码，真实后端 UI smoke 改为断言用户友好失败提示；后端 `publish_handoff_hint`、`blocked_reason` 和交接冲突错误改为中文用户文案；歌词确认页只在交接真正就绪时展示交接提示；成品页按钮优先消费 `publish-package.available_actions`，避免只依赖作品详情动作列表。验证已通过 `npm test`、`npm run typecheck`、`npm run build`、`node --check prototypes/Claude-web-v1/scripts/smoke-real-backend.mjs`、`JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew :apps:music-api:test --tests '*WorkService*' :apps:music-api:compileJava`、`JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew spotlessCheck :apps:music-api:bootJar`、关键旧字段/技术文案搜索和 `git diff --check`。

2026-06-06 22:58 CST 已完成推荐用户实测路径复验：本地基础设施保持运行，使用 `MOCK_MUSIC_DURATION_MS=1000 MUSIC_PROVIDER=mock MUSIC_WORKFLOW_DISPATCH_MODE=sync WORKFLOW_OUTBOX_DISPATCHER_ENABLED=false RENDER_WORKER_MODE=mock` 启动 `music-api` 后，`EXPECTED_DURATION_MS=1000 scripts/smoke/api-main-flow.sh`、`scripts/smoke/openapi-contract.sh`、`cd prototypes/Claude-web-v1 && npm run smoke:real-backend` 均通过。API 主链路作品 `92703111-50d0-453e-a9b8-f247aa2c6914`，OpenAPI contract 作品 `e1bc6b0b-340a-4899-9923-15b016147881` / 受控失败恢复作品 `e86add7b-0b3b-4d5c-af6b-b05b8b66131a`，前端真实后端 UI smoke 主链路作品 `8b2cdd23-f073-433b-b0cb-1fb8dab76736` / 失败重试作品 `b8cfce76-de20-4a65-8f8e-e712648ab671`。验证结束后 `8080` 和 `5274` 无监听进程。当前结论：用户可以按 `prototypes/Claude-web-v1 + music-api 同步 Mock` 路径进行首轮本地实测；真实 Suno/MiniMax 出歌仍需单独开启 DreamMaker 硬开关并安全注入凭据。

2026-06-06 23:05 CST 已补齐真实音乐受控联调最短执行资料：新增 `docs/checklists/dreammaker-real-music-smoke-10min.md`，把 Suno / MiniMax via DreamMaker 的 1 个 Provider、1 个作品手动 smoke 拆成前置检查、安全注入 AK/SK、启动 worker/API、创建作品、确认出歌、轮询状态、数据库判断是否真的打到 DreamMaker、回滚清理和脱敏记录模板。同步更新 `docs/runbook/dreammaker-controlled-real-integration.md`、`docs/checklists/dreammaker-real-integration-acceptance.md`、`docs/checklists/local-commercial-delivery-acceptance.md` 和 `docs/handover/dreammaker-provider-batch-05-handoff.md`，并修正总体验收清单中过时的“失败页展示失败码”口径为普通用户友好失败说明。当前仍未发起真实 DreamMaker / Suno / MiniMax 请求。

2026-06-06 23:12 CST 已强化公司 Adapter 部署交接资料：新增 `docs/checklists/company-adapter-replacement-readiness.md`，按实际 Java 接口列出 `AccountAdapter`、`ModerationAdapter`、`QuotaAdapter`、`PublishAdapter` 和分享系统承接的替换矩阵、方法签名、幂等/隔离/审核/交接验收项、联合 smoke 和禁止事项；同步更新公司交接文档和本地商用闭环总体验收清单，要求公司开发替换账号、审核、权益、发布、分享边界时留下逐项证据。当前仍未接入真实公司系统。

2026-06-06 23:20 CST 已更新当前态本地商用闭环走查记录 `docs/checklists/local-commercial-delivery-audit-2026-06-06.md`：明确用户现在可测的是 `prototypes/Claude-web-v1 + music-api` 的本地完整链路和 local-process MP4 成片链路；真实 AI 出歌尚未完成供应商真实调用 smoke，下一步若用户要测真实 AI 出歌，应优先按 `docs/checklists/dreammaker-real-music-smoke-10min.md` 只打开 DreamMaker 音乐一环，先跑 Suno 或 MiniMax 单作品受控 smoke。

2026-06-07 00:45 CST 已执行首次真实 Suno 单作品 stack smoke：本地 Docker 基础设施健康，8080/8081 空闲；`scripts/smoke/dreammaker-real-music-stack-smoke.sh` 启动 worker/API、创建作品 `598a0380-0de5-4536-9f84-9c5481bca685`、确认出歌并触达 DreamMaker Suno 创建任务。供应商返回 HTTP 403，作品收口为 `FAILED / MUSIC_GENERATION_FAILED / PACKAGE_NOT_READY`，`provider_calls` 记录 `SUNO | MUSIC_GENERATION | suno:music-gen:chirp-crow | FAILED`，trace id 为空。API/worker 已自动停止，8080/8081 无残留监听；日志敏感词扫描未发现 AK/SK、JWT、Bearer 或 `X-Access-Token`。当前判断：本地编排和失败收口正常，下一步需确认 DreamMaker AK/SK 是否开通 Suno 权限、是否强制 `X-Access-Token`、以及 `app/sub_app/model` 是否与供应商文档和账号权限一致。

2026-06-06 23:28 CST 已根据只读交付审计补齐前端交付轨道和阻塞矩阵：新增 `docs/adr/0003-frontend-delivery-track.md`，冻结 `prototypes/Claude-web-v1` 为当前用户实测与前后端联调验收对象，`apps/web` 仍为正式工程 scaffold，是否迁移或公司前端重建需交付前单独决策；同步 README、AGENTS、总体验收清单和当前态走查记录，避免公司接手时误解前端交付物。同时在公司 Adapter readiness 中明确 `company_share` 可作为“公司系统承接 / 本平台豁免实现”记录，不需要本平台新增分享 Adapter。

2026-06-06 23:45 CST 已按真实音乐联调只读审计结果补齐运行时安全收口：`WorkService.confirmWork` 与 `retryMusic` 在 `DREAMMAKER_REAL_CALLS_ENABLED=true` 且有效 Provider 为 `suno` / `minimax` 时，强制要求 `MUSIC_WORKFLOW_DISPATCH_MODE=outbox` 且 `WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal`，否则直接返回冲突，避免在同步 API 线程中误触真实供应商；`/internal/integration-readiness` 的 `dreammaker_guard` 现在会在真实开关打开但 AK/SK 未配置时显示 `BLOCKED`。新增脚本 `scripts/smoke/dreammaker-real-music-smoke.sh`，用于手动单作品 DreamMaker 真实音乐 smoke，必须设置 `ALLOW_DREAMMAKER_REAL_SMOKE=1` 才会运行。本轮只做安全保护和脚本准备，未发起真实 DreamMaker 请求。

2026-06-07 00:10 CST 已根据用户指定的真实模型方向补齐下一阶段受控客户端：DeepSeek 使用 OpenAI 兼容格式，默认 `DEEPSEEK_BASE_URL=https://api.deepseek.com`、`DEEPSEEK_MODEL_NAME=deepseek-v4-pro`，新增 `RealDeepSeekLyricsClient`，要求 `AGENT_REAL_CALLS_ENABLED=true` 与 `DEEPSEEK_REAL_CALLS_ENABLED=true` 双开关，并用 `response_format=json_object` 解析歌词结构；Image2 通过 DreamMaker 接入，新增 `DreamMakerImage2CoverGenerationService`，默认 `gpt-image-2`、`IMAGE2_SIZE=2048x1152`，供应商图片 URL 只作为内存导入源，导入平台对象存储后从 metadata 移除，不进入发布包。`/internal/integration-readiness` 已更新 DeepSeek / Image2 的真实客户端与缺 key 阻塞口径；新增并通过 `scripts/smoke/dreammaker-real-guard-smoke.sh` 非真实 guard smoke，作品 `d8b4f269-a8eb-4a1a-b971-3c772aad2515` 在 `DREAMMAKER_REAL_CALLS_ENABLED=true + sync` 下确认 `suno` 返回 409，状态仍为 `LYRICS_READY / WAITING_CONFIRM / PACKAGE_NOT_READY`，`provider_calls=0`。该小阶段本身未执行真实 DeepSeek、Image2 或 Suno 调用；后续 00:45 已完成 Suno 首跑并记录 HTTP 403 样本。

第 2 批后续小阶段已补齐 `Idempotency-Key` 的基础重放语义：同用户、同 operation、同 key、同请求内容会重放第一次成功响应；同 key 不同请求内容返回 `IDEMPOTENCY_CONFLICT`。

音乐生成 Provider 工程边界已根据用户要求预置：统一 `MusicProvider` 合约、`MockMusicProvider`、`SunoMusicProvider` 和 `MiniMaxMusicProvider` 均已建好；当前 Suno/MiniMax 已通过 DreamMaker client 具备受控真实 submit/status 调用实现，但默认 `DREAMMAKER_REAL_CALLS_ENABLED=false`，自动化和本地默认路径不会调用真实 API。

当前 Mock 出歌流程已接入 `MockMusicProvider`，不再在 `WorkService` 内直接硬编码音频生成结果；后续切换 Suno 或 MiniMax 时已有主链路落点。

`confirmWork` 已进一步拆到 `MockSongProductionWorkflow`：`WorkService` 只负责状态校验和接口返回，出歌编排集中处理权益锁定、音乐 Provider、媒体资产、发布包、发布包审核、权益扣减、失败释放和 generation job 收口。当前仍是同步 Mock Workflow，为后续 Temporal Workflow 接入做准备。

对象存储边界已升级：发布包 JSON 会通过 `ObjectStorageClient` 写入本地文件存储或 S3/MinIO 兼容对象存储，object key 使用 `yanyun-ai-music/{env}/{yyyy}/{MM}/{dd}/{work_id}/package/publish-package.json` 分层结构；`package_url` 由对象存储客户端按持久化 `package_object_key` 签发或刷新，不再在业务代码中硬编码本地 URL。

音乐 Provider 选择已从硬编码 `MOCK` 改为配置驱动：`MUSIC_PROVIDER=mock|suno|minimax` 会映射到统一 `MusicProviderSelection`。当前默认 `mock` 可完整成功；`suno` / `minimax` 已接入 DreamMaker submit + poll 骨架，但缺少 `DREAMMAKER_ACCESS_KEY` / `DREAMMAKER_SECRET_KEY` 时会在发起外部请求前进入 `MUSIC_GENERATION_FAILED` 可重试失败，并释放已锁权益。

DreamMaker / Suno / MiniMax 接入骨架已落地：新增共享 `modules:dreammaker`，`SunoMusicProvider` 和 `MiniMaxMusicProvider` 已可按 DreamMaker run/status 协议构造请求、提交任务、轮询状态、映射失败码，并在成功时返回供应商音频源 URL。Workflow 已补远程音频导入边界，会先把供应商音频 URL 导入本地对象存储，再写入 `AUDIO` 媒体资产。默认仍走 `mock`，自动化测试不调用真实供应商；真实联调需要安全配置 `DREAMMAKER_ACCESS_KEY` / `DREAMMAKER_SECRET_KEY`。

DreamMaker 鉴权口径已根据用户补充资料从待确认项改为已决策：每次请求使用 AccessKey 作为 JWT `iss`、SecretKey 做 HS256 签名，`exp=now+1800s`、`nbf=now-5s`，并以 `Authorization: Bearer <jwt>` 发起请求；`DREAMMAKER_USER_ACCESS_TOKEN` 仅作为可选 `X-Access-Token` 透传。

第 3 批可靠编排基础已启动并完成 Outbox v0.1：新增 `workflow_outbox` 表、`MUSIC_WORKFLOW_DISPATCH_MODE=sync|outbox`、本地 outbox dispatcher 和确认出歌/音乐重试的异步启动边界。默认 `sync` 保持现有 Mock 主链路兼容；显式 `outbox` 模式会在 API 事务内抢占作品状态、写入 generation job 和 outbox event，随后由 dispatcher 异步推进到 `GENERATED / PACKAGE_READY`。

第 4 批 Temporal 真实编排基础已完成：`WORKFLOW_OUTBOX_DISPATCH_TARGET=local|temporal` 可切换本地委托和 Temporal 启动；Temporal 模式下 API dispatcher 只负责按 `work_id + job_id` deterministic workflow id 启动 workflow，独立 `music-worker` 注册 workflow/activity 并复用当前生产委托推进作品到 `GENERATED / PACKAGE_READY`。

第 5 批 DreamMaker 受控真实联调准备已完成：`DREAMMAKER_REAL_CALLS_ENABLED=false` 作为默认硬开关，API 和 worker 共享 `modules:dreammaker` 中的 HTTP client 与 properties；Temporal worker 已注册 Suno/MiniMax Provider；联调需按 `docs/runbook/dreammaker-controlled-real-integration.md` 手动开启。

第 6 批 DeepSeek / 知识库写词润色 Mock 链路已完成：`WorkService` 不再硬编码灵感成歌、填词成歌、润色和续写文本，而是通过 `LyricsGenerationService` 编排 `KnowledgeService`、`PromptTemplateService` 和 `DeepSeekLyricsClient`；当前默认均为 Mock/Fake，不调用真实 DeepSeek。AI 润色和 AI 续写共享 2 次用户侧编辑次数，低质量结果的内部自动重写不消耗用户次数。

第 7 批封面与视频成片基础已完成：`MockSongProductionWorkflow` 不再直接硬编码封面、视频和时间轴路径，而是通过 `CoverGenerationService` 与 `VideoRenderService` 生成 `COVER`、`VIDEO`、`TIMELINE` 资产描述，并连同 `AUDIO` 统一写入 `media_assets`；发布包 JSON 已按媒体资产 object key 组装 `video.url`、`cover.url` 和 `lyrics.timeline_url`。`apps/render-worker` 已新增 `LyricVideo16x9` composition，并可本地渲染 H.264/AAC、1920x1080、约 8 秒的 MP4 样例。

第 8 批 MinIO/S3 发布包强化已完成：新增 S3/MinIO 兼容 `ObjectStorageClient`、统一对象存储配置、结构化发布包 object key、presigned GET URL 签发、`refresh-url` 按数据库 `package_object_key` 重新签发，以及本地文件模式 TTL 口径。Docker Compose 内置 MinIO 已完成发布包写入、对象存在性、URL 下载和刷新签名 smoke；默认 local 模式仍可跑通。

第 10 批公司 Adapter 接入与部署交接准备已并行完成：新增 `CompanyIntegrationProperties`、`IntegrationReadinessService` 和内部接口 `GET /internal/integration-readiness`，可结构化展示账号、审核、权益、发布、分享、音乐 Provider、对象存储、Workflow dispatch 和 DreamMaker 硬开关的当前模式、实现、阻塞状态和所需环境变量；新增公司交接文档，明确真实公司系统仍由公司开发替换 Adapter 实现。

第 11 批 render-worker 本地进程调用边界已完成：`apps/render-worker` 新增 `render:job` CLI，支持按输入 `duration_ms` 动态生成 Remotion composition 时长和句级 timeline；`modules:production` 新增 `LocalProcessVideoRenderService`，可在 `RENDER_WORKER_MODE=local-process` 时调用 Node render-worker，把 MP4 与 timeline JSON 写入当前对象存储并返回真实媒体资产描述。默认仍为 `mock`，自动化测试不会触发真实长视频渲染。

render-worker 本地进程模式已完成首次 API 端到端 smoke，并修复工作目录解析问题：Gradle `bootRun` 下 Java 进程工作目录是 `apps/music-api`，原先默认 `apps/render-worker` 会解析失败；现在本地进程服务会在相对路径不存在时向上查找父目录，因此默认 `apps/render-worker` 可在仓库内启动时正确解析。若 JAR 放到仓库外运行，仍建议显式设置绝对 `RENDER_WORKER_WORKING_DIRECTORY`。

- `yanyun-ai-music-platform-prd-v0.3.md`：商用级产品范围基线。
- `yanyun-ai-music-platform-tech-design-v0.2.md`：商用级技术方案基线。
- `docs/adr/0001-user-web-scope.md`：用户侧 Web 范围决策。
- `docs/adr/0002-commercial-grade-stack.md`：商用级技术栈决策。
- `docs/adr/0003-frontend-delivery-track.md`：前端交付轨道决策，明确当前验收对象是 `prototypes/Claude-web-v1`，`apps/web` 是正式工程 scaffold。
- `docs/api/openapi-v0.1.yaml`：作品、生成阶段、失败动作、权益提示和发布包交接接口契约。
- `docs/specs/openapi-contract-smoke-v0.1.md`：OpenAPI v0.1 与后端运行时响应的契约对拍 smoke 规格。
- `scripts/smoke/openapi-contract.sh`：OpenAPI v0.1 运行时契约 smoke 脚本。
- `database/migrations/V202606050245__init_work_domain.sql`：作品域核心业务表。
- `database/migrations/V202606061740__add_agent_runs.sql`：Agent run 审计表。
- `docs/specs/agent-runtime-audit-v0.1.md`：Agent Runtime 与 `agent_runs` 审计边界规格。
- `docs/specs/ai-agent-architecture-direction-v0.1.md`：Agent 架构方向基线，后续真实模型和 Agent 实现按此分层推进。
- `docs/specs/ai-agent-orchestration-engineering-design-v0.1.md`：多 Agent 工程编排设计基线，后续按此方向实现。
- `docs/specs/creative-brief-agent-v0.1.md`：创作简报 Agent 边界规格，真实 DeepSeek 接入前的用户需求理解落点。
- `docs/specs/cover-prompt-agent-v0.1.md`：封面提示词 Agent 边界规格，真实 Image 2 接入前的视觉 prompt 规划落点。
- `docs/specs/quality-evaluation-agent-v0.1.md`：发布包质量评估 Agent 边界规格，发布包准备和审核前的质量门落点。
- `docs/specs/moderation-agent-v0.1.md`：AI 预检 Agent 边界规格，音乐 Provider 调用前的音乐 prompt 风险预检落点。
- `docs/frontend/gemini-batch-02-mock-workflow-task-package.md`：交给 Gemini 的第 2 批前端任务包。
- `docs/specs/deepseek-knowledge-lyrics-v0.1.md`：第 6 批 DeepSeek / 知识库写词润色 Mock 链路规格。
- `docs/specs/ai-multi-agent-creative-pipeline-v0.1.md`：真实模型阶段的 AI 多 Agent / Worker 创作编排设计。
- `docs/specs/cover-video-rendering-v0.1.md`：第 7 批封面与 Remotion/FFmpeg MP4 成片基础规格。
- `docs/specs/minio-s3-publish-package-storage-v0.1.md`：第 8 批 MinIO/S3 发布包强化规格。
- `docs/specs/company-adapter-deployment-handoff-v0.1.md`：第 10 批公司 Adapter 接入与部署交接准备规格。
- `docs/specs/render-worker-local-process-integration-v0.1.md`：第 11 批 Java 到 render-worker 本地进程调用边界规格。
- `docs/specs/stepwise-temporal-production-state-advancement-v0.1.md`：分步 Temporal 从录步模式演进到生产态推进的规格基线。
- `docs/checklists/dreammaker-real-music-smoke-10min.md`：真实 Suno / MiniMax 音乐 10 分钟手动 smoke 清单。
- `scripts/smoke/dreammaker-real-music-smoke.sh`：真实 Suno / MiniMax 单作品脚本化 smoke，需人工显式授权后才会调用 DreamMaker。
- `docs/runbook/yunwu-suno-controlled-real-integration.md`：当前非公司内网环境下的 Yunwu Suno 公网受控联调 Runbook，明确 DreamMaker 仍是正式生产目标。
- `docs/specs/yunwu-suno-real-music-stack-smoke-v0.1.md`：Yunwu Suno 单作品真实 smoke 的脚本化规格。
- `scripts/smoke/yunwu-suno-real-music-stack-smoke.sh`：Yunwu Suno 一键受控真实 smoke，需 `ALLOW_YUNWU_REAL_SMOKE=1` 才会启动 worker/API 并真实调用 Yunwu。
- `scripts/smoke/yunwu-suno-real-music-smoke.sh`：Yunwu Suno 低层单作品真实 smoke，用于 API/worker 已手动启动的场景。
- `docs/specs/wellapi-image2-real-cover-stack-smoke-v0.1.md`：WellAPI Image2 单作品真实封面 smoke 的脚本化规格，明确 DreamMaker Image2 仍是正式生产目标。
- `scripts/smoke/wellapi-image2-real-cover-stack-smoke.sh`：WellAPI Image2 一键受控真实封面 smoke，需 `ALLOW_WELLAPI_IMAGE2_REAL_SMOKE=1` 才会启动 API 并真实调用 WellAPI。
- `scripts/smoke/wellapi-image2-real-cover-smoke.sh`：WellAPI Image2 低层单作品真实封面 smoke，用于 API 已手动启动的场景。
- `docs/specs/dreammaker-image2-real-cover-stack-smoke-v0.1.md`：DreamMaker Image2 生产目标单作品真实封面 smoke 的脚本化规格。
- `scripts/smoke/dreammaker-image2-real-cover-stack-smoke.sh`：DreamMaker Image2 一键受控真实封面 smoke，需 `ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE=1` 才会启动 API 并真实调用 DreamMaker。
- `scripts/smoke/dreammaker-image2-real-cover-smoke.sh`：DreamMaker Image2 低层单作品真实封面 smoke，用于 API 已手动启动的场景。
- `docs/specs/company-adapter-readiness-smoke-v0.1.md`：公司 Adapter readiness 只读 smoke 规格，用于验证交接报告结构、Mock 边界和脱敏安全。
- `scripts/smoke/company-adapter-readiness-smoke.sh`：公司 Adapter readiness 只读 smoke，API 已启动后可生成安全组件摘要，不调用真实公司系统或供应商。
- `docs/specs/local-commercial-delivery-status-handoff-v0.1.md`：本地商用交付状态说明规格，用于防止把 Mock / 受控 smoke 准备项过度描述为真实生产完成。
- `docs/handover/local-commercial-delivery-status-v0.1.md`：当前交付状态矩阵，按 READY_LOCAL / PREPARED_SMOKE / PREPARED_HANDOFF / BLOCKED_EXTERNAL / DECISION_REQUIRED 分类给公司开发和用户阅读。
- `docs/specs/real-model-readiness-preflight-v0.1.md`：真实模型联调前只读预检规格，定义目标、严格模式、密钥脱敏和不调用供应商的边界。
- `scripts/smoke/real-model-readiness-preflight.sh`：真实模型联调前只读预检脚本，检查本地环境变量和可选 API readiness，不启动服务、不调用真实供应商。
- `docs/specs/real-model-controlled-smoke-index-v0.1.md`：真实模型受控 smoke 总入口规格，区分公网联调路径与 DreamMaker 正式生产目标。
- `scripts/smoke/real-model-controlled-smoke.sh`：真实模型 smoke 目标矩阵、执行计划、只读预检和受控委托总入口。
- `docs/specs/local-delivery-evidence-audit-v0.1.md`：本地商用交付证据只读审计规格。
- `scripts/smoke/local-delivery-evidence-audit.sh`：交付前只读证据审计脚本，检查文档、脚本、安全口径、密钥形态和大文件。
- `docs/specs/company-handoff-package-index-v0.1.md`：公司开发交接包索引规格。
- `docs/handover/company-delivery-package-v0.1.md`：公司开发第一阅读入口，汇总交付物、命令、边界、真实模型规则和禁止事项。
- `scripts/smoke/company-handoff-package-audit.sh`：公司交接包只读审计脚本。
- `docs/checklists/company-adapter-replacement-readiness.md`：公司 Adapter 替换 readiness 清单。
- `docs/handover/company-adapter-deployment-handoff-v0.1.md`：公司开发替换 Mock Adapter 与部署交接说明。
- `docs/checklists/local-commercial-delivery-acceptance.md`：本地商用闭环交付总体验收清单。
- `docs/checklists/local-commercial-delivery-audit-2026-06-06.md`：2026-06-06 当前态本地商用闭环走查记录。
- `docs/frontend/claude-web-v1-acceptance-fix-task-package.md`：Claude Web v1 前端原型验收修复任务包。

## 进度记录规则

- 本文件是项目的持久进度记录，用于避免长线开发中因上下文压缩、换会话或换技术方案导致信息丢失。
- 每次完成阶段性任务后，必须在最终回复前更新本文件。
- 阶段性任务包括：需求或方案调整、技术栈变更、架构决策、工程初始化、批次开发完成、测试验收、重要问题修复、外部系统协议确认、部署或联调进展。
- 更新内容至少包括：完成事项、关键决策、验证结果、当前风险、待确认事项和下一步。
- 不覆盖历史工作日志；新的阶段性进展追加到“工作日志”，必要时同步调整“当前阶段”“待确认事项”和“下一步建议”。
- 如果 PRD、技术方案、OpenAPI、数据库设计或部署方案发生变化，应同步更新相关文档，或在本文件中明确标注“待同步”。

## 已完成

- 初始化 Git 仓库，默认分支为 `main`。
- 初步阅读 PRD v0.2 和技术方案 v0.1。
- 确认产品目标是正式商用级 AI 作曲与视频成片平台，不是一次性活动页。
- 确认技术主方向为模块化单体、Temporal Worker、独立 Remotion 渲染 Worker、Provider/Adapter 边界。
- 整理 Codex 第 1 批仓库初始化任务说明。
- 整理技术方案进入工程初始化前需要补齐的事项。
- 固化 Git commit 提醒规则：默认不主动提交；阶段性任务完成且适合形成快照时提醒用户是否 commit。
- 输出 PRD v0.3，明确商用级目标、本地完整跑通、外部系统职责边界、移动端优先兼容 PC Web、权益扣减和量化验收。
- 输出技术方案 v0.2，明确完整商用级技术栈、第 1 批工程预置、`apps/web`、发布包审核落点、`works.version` 和 OpenAPI 覆盖范围。
- 新增两份 ADR，固化用户侧 Web 范围和商用级技术栈不降级的决策。
- 升级项目级 `AGENTS.md`，将项目身份、固定决策、外部系统边界、前后端规则、验证规则、进度记录、Git 和安全规则固化为后续 Agent 执行手册。
- 补充前端和图片资产协作规则：需要图片资产时优先用 Image 生成；前端视觉实现优先整理任务包交给 Gemini，本 Agent 负责需求拆解、接口、状态、验收和 review。
- 输出 OpenAPI v0.1 接口契约，覆盖用户信息、作品创建、作品状态、AI 改词/续写、确认出歌、封面重生、视频重渲、发布包获取/刷新/标记交接。
- 初始化 Java 21 + Spring Boot 3 + Gradle Kotlin DSL 多模块工程，包含 `apps/music-api`、`apps/music-worker` 和 `modules/*` 商用级边界。
- 初始化 `apps/web` React + Vite + TypeScript scaffold，移动端优先、兼容 PC Web，只做工程验证页。
- 初始化 `apps/render-worker` Node.js 22 + TypeScript + Remotion scaffold，保留最小 16:9 composition 和 smoke test。
- 新增本地 Docker Compose 基础设施：PostgreSQL 16、Redis 7、Temporal、Temporal UI、MinIO、OpenSearch、Prometheus、Grafana。
- 新增 `.gitignore`、`.dockerignore`、`.env.example`、README、本地运行手册、数据库/知识库预留目录。
- 为 `music-worker` 增加 Temporal 启动连接探测，启动时会验证 `localhost:7233` 可连接，失败时输出明确 target/namespace/taskQueue。
- 已提交第 1 批工程初始化快照：`992762a chore: initialize commercial-grade project scaffold`。
- 新增 Flyway migration，落库 `works`、`work_inputs`、`lyrics_drafts`、`generation_jobs`、`media_assets`、`publish_packages`、`provider_calls`、`quota_transactions`、知识库、Prompt 模板、系统配置和幂等键等表。
- 实现 `work-domain` 领域枚举与状态机，覆盖 OpenAPI v0.1 的作品状态、生成阶段、发布包状态、失败码和可执行动作。
- 实现本地 Mock Adapter 边界：账号、权益、审核、发布包交接均通过接口隔离，真实公司系统后续替换实现即可。
- 实现 `music-api` 主路径接口：`/api/v1/me`、灵感成歌、填词成歌、作品列表/详情、润色/续写、确认出歌、封面重生、视频重渲、发布包获取/刷新/标记交接。
- 实现 Mock 作品生成闭环：创建作品后得到歌词草案；确认出歌后生成 Mock 媒体资源、发布包 JSON、下载 URL，并将状态推进到 `GENERATED` / `PACKAGE_READY`；标记交接后进入 `PACKAGE_FETCHED`。
- 新增第 2 批 Gemini 前端任务包，明确移动端优先 + PC Web 的页面、接口、状态、错误和验收范围。
- 根据用户补充，记录音乐生成后续需同时接入 Suno 与 MiniMax，并预留可配置开放策略；飞书资料已通过 `lark-cli` 授权读取，真实 run/status 接口要点已整理到集成说明。
- 实现 `Idempotency-Key` 基础语义，覆盖当前所有 POST 主路径，成功响应写入 `idempotency_keys`，重复提交可重放响应，参数冲突返回 `IDEMPOTENCY_CONFLICT`。
- 新增 `modules:music-provider`，定义音乐生成统一 Provider 合约、请求、结果、状态、Provider 类型和注册选择器。
- 新增 `modules:suno`，预置 `SunoMusicProvider` 边界；后续已升级为 DreamMaker submit + poll 骨架，自动化测试仍不会调用真实 Suno API。
- 扩展 `modules:minimax`，预置 `MiniMaxMusicProvider` 边界；后续已升级为 DreamMaker submit + poll 骨架，自动化测试仍不会调用真实 MiniMax API。
- `.env.example` 增加 `MUSIC_PROVIDER=mock` 和 DreamMaker 统一接入变量名，不包含真实凭据。
- `music-api` 已依赖 `modules:music-provider`，通过 Spring 配置注册 `MockMusicProvider` 和 `MusicProviderRegistry`。
- `WorkService.confirmWork` 已通过 `MusicProviderRegistry.require(MOCK)` 生成 Mock 音频结果，再继续封面、视频、发布包链路。
- 新增 `MockSongProductionWorkflow`，将确认出歌后的生成编排从 `WorkService` 中拆出，集中处理权益锁定、Provider 调用、媒体资产、发布包、发布包审核、权益提交和失败释放。
- `generation_jobs` 已增加完成状态更新方法，`SONG_PRODUCTION` job 成功后会收口到 `SUCCEEDED / PACKAGE_READY`，失败后会收口到 `FAILED / FAILED` 并记录失败码。
- 新增 `MockSongProductionWorkflowTest`，覆盖成功出发布包和音乐 Provider 失败时释放已锁权益两个关键路径。
- 新增 `modules:storage` 对象存储合约和 `LocalObjectStorageClient`，本地阶段可把发布包 JSON 写到 `build/local-object-storage/yanyun-works-local`。
- `MockSongProductionWorkflow` 已接入对象存储写入；发布包准备或写入失败会进入 `PACKAGE_BUILD_FAILED`，释放已锁权益，并标记为可重试失败。
- 本地运行手册已补充 package JSON 写入位置和检查方式。
- 新增 S3/MinIO 兼容对象存储实现，支持 endpoint override、bucket、region、path-style、自动创建本地 bucket 和 presigned GET URL。
- 发布包 object key 已改为 `yanyun-ai-music/{env}/{yyyy}/{MM}/{dd}/{work_id}/package/publish-package.json`；`refresh-url` 已改为使用持久化 `package_object_key` 重新签发 URL。
- `.env.example` 和 API/worker 配置已补齐 `OBJECT_STORAGE_PROVIDER=local|s3`、`S3_ENDPOINT`、`S3_PUBLIC_ENDPOINT`、bucket、region、TTL 等本地对象存储变量。
- 新增公司接入 readiness 边界：`modules:config-center` 提供 `IntegrationReadinessService`，`music-api` 暴露内部 `GET /internal/integration-readiness`，用于交接和部署前检查 Mock/真实接入状态。
- 新增公司 Adapter 交接说明，覆盖账号、审核、权益、发布、分享的替换接口、部署变量、smoke 步骤和禁止事项。
- 新增 render-worker 本地进程调用边界：`RENDER_WORKER_MODE=local-process` 时 Java `VideoRenderService` 可调用 `apps/render-worker` 的 `render:job` CLI，把生成的 MP4 和 timeline JSON 写入对象存储；默认 `mock` 不触发真实渲染。
- 新增 `MOCK_MUSIC_DURATION_MS`，允许本地 smoke 临时压短 Mock 音频时长；默认仍为 180000ms。
- 新增 AI 多 Agent / Worker 创作编排设计，明确真实模型阶段采用确定性 Workflow + 专业 Agent Worker + Provider Adapter，而不是自由聊天式多 Agent。
- 新增多 Agent 工程编排设计文档，固化 Agent、Adapter、Workflow、Service 的落地边界、流程、状态映射、审计、失败重试、配置开关、测试策略和分阶段实施计划。
- 新增 Agent 架构方向基线，明确后续采用确定性 Workflow + 专业 Agent Worker + Provider Adapter，不做自由聊天式多 Agent。
- 新增 OpenAPI v0.1 运行时契约 smoke，覆盖静态契约、成功主链路、统一错误、幂等冲突、作品列表、发布包、受控音乐失败和重试恢复。
- 新增 Agent Runtime / `agent_runs` 审计基础，写词链路的 Mock DeepSeek 调用已通过 `LyricsAgent` 记录输入/输出 hash、模型名、Prompt 模板版本、耗时和状态。
- 新增 `CreativeBriefAgent` v0.1 Mock 合约，写词链路会在 DeepSeek 前生成结构化创作简报、注入 Prompt 上下文并写入 `agent_runs`。
- 新增 `modules:creative-agent` 和 `MusicPromptAgent` v0.1 Mock 合约，确认出歌时先由 Agent 生成音乐提示词和 provider options，再调用 `MusicProvider`；成功和失败均接入 `agent_runs` 审计。
- 新增 `CoverPromptAgent` v0.1 Mock 合约，封面生成前先规划 visual prompt、negative prompt、尺寸和 provider options，再调用 `CoverGenerationService`。
- 新增 `QualityEvaluationAgent` v0.1 Mock 合约，发布包准备、审核和写入前先执行发布包质量门，成功和失败均接入 `agent_runs` 审计。
- 完成 `prototypes/Claude-web-v1` 前端原型初审：测试、typecheck、build 和 390px / 1440px Playwright smoke 已通过，但仍有契约和验收缺口。
- 新增 `docs/frontend/claude-web-v1-acceptance-fix-task-package.md`，把前端缺口收敛成可交给前端实现者的修复任务包。
- 按用户明确要求直接修复 `prototypes/Claude-web-v1` 前端验收缺口，补齐列表页、发布交接、状态派生、失败动作、润色请求、错误编号展示、可访问性和关键测试。
- 新增 `MusicProviderSelection`，解析 `MUSIC_PROVIDER=mock|suno|minimax`；`music-api` 已注册 Mock、Suno、MiniMax 三类 Provider bean。
- `MockSongProductionWorkflow` 已按配置选择音乐 Provider；Provider 未实现或抛异常时会进入 `MUSIC_GENERATION_FAILED`，释放权益并关闭 job。
- 修正 `IdempotencyService` 外层事务边界：`ResponseStatusException` 不再回滚业务失败状态，避免配置到未实现 Provider 时作品仍停留在 `LYRICS_READY`。
- 新增 `ModerationAgent` v0.1 Mock 合约，音乐 Provider 提交前先做音乐 prompt AI 预检，成功和阻断均接入 `agent_runs` 审计。
- 新增 DeepSeek 真实受控联调 Runbook、安全日志规则、验收清单和开放问题跟踪；`.env.example` 已预留 DeepSeek 真实调用开关、base URL、模型名、超时、最大尝试次数和输入长度上限，默认全部关闭或为空；`/internal/integration-readiness` 新增 `deepseek_guard`，避免真实客户端未实现时误判为可用。
- 新增 Image 2 真实受控联调 Runbook、安全日志规则、验收清单和开放问题跟踪；`.env.example` 已预留 Image 2 Provider、真实调用开关、base URL、模型名、超时、最大尝试次数和目标尺寸，默认全部关闭或为空；`/internal/integration-readiness` 新增 `image2_guard`，避免真实客户端未实现时误判为可用。
- 新增 Temporal activity 细化设计规格，真实模型阶段前不再只依赖单一 `produce` activity，而是有明确的分步 activity、幂等键、失败码、step audit 和分阶段实施建议。
- 新增 `SongProductionStepActivities` Phase 1 合约骨架，先把未来拆分 activity 的接口、步骤名、上下文和通用结果落到 `modules:workflow`，但不改变当前 Temporal worker 的单 activity 运行路径。
- 新增 `generation_job_steps` step audit 表和 repository 最小 API，为后续分步 activity 记录状态、幂等键、外部 trace 和失败码提供数据库落点。
- 新增 `RecordingSongProductionStepActivities`，为分步 activity 的 Mock/记录模式提供可测试实现：成功步骤写入 `SUCCEEDED` step audit，失败步骤写入脱敏后的 `FAILED` 记录；当前未接入主 workflow。
- 新增 `StepwiseSongProductionWorkflow` 受控验证路径，覆盖计划 activity 顺序、锁权益失败不释放、后续失败释放权益、发布预检阻断映射和 `RecordingSongProductionStepActivities` 写出 13 条成功 step audit；当前未注册到生产 worker。
- 新增 worker 侧 `TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE` 开关，默认 `legacy` 保持旧单 activity 主链路；显式 `stepwise-recording` 时才注册 stepwise workflow 和 recording step activity，并用懒获取避免 legacy 启动依赖 stepwise bean。
- 完成 `stepwise-recording` 本地 Temporal smoke，验证 outbox 可启动 stepwise worker 且 `generation_job_steps` 写入 13 条成功记录；同时补 `TemporalWorkerProperties` 配置绑定回归测试。
- 新增 `scripts/smoke/temporal-stepwise-recording.sh`，把 stepwise outbox + step audit 验收脚本化，避免误用主链路发布包 smoke。

## 当前关键判断

- PRD v0.3 和技术方案 v0.2 作为新的项目启动基线。
- 项目按正式商用级平台建设，不按 Demo 或临时活动页设计。
- 本地阶段必须跑通完整生成链路；公司账号、审核、权益、发布、分享等真实接入由公司开发后续替换 Mock Adapter。
- 用户侧由本项目提供 `apps/web`，移动端优先，同时兼容 PC Web。
- 前端视觉和页面实现优先整理成可转交 Gemini / 外部前端实现者的任务包；除非用户明确要求，本 Agent 不默认承担最终高保真前端实现。
- 后续如需要图片资产，优先使用 Image 生成，并在提交前确认用途、尺寸、来源和是否纳入仓库。
- 当前前端验收对象为 `prototypes/Claude-web-v1`；`apps/web` 仍是正式工程 scaffold。`prototypes/Claude-web-v1` 本轮已完成契约验收修复，后续仍建议由外部前端实现者继续负责高保真视觉迭代，本 Agent 负责接口状态 review、联调验收和必要修复。
- `MOCK_MUSIC_DURATION_MS` 只用于本地 Mock 和联调提速，不代表真实音乐模型时长策略。
- Temporal v0.1 先证明 API outbox 到独立 worker 的可靠启动边界；activity 自动重试固定为 1 次，等权益、Provider、媒体和发布包写入幂等性审计完成后再放开。
- 真实模型阶段的多 Agent 方向已定：`SongProductionWorkflow` / Temporal 持有状态和副作用，Agent 只返回结构化候选结果、评分、风险提示和建议动作；Suno / MiniMax、Image 2、对象存储和公司系统必须作为 Adapter 接入。
- Agent 架构方向已作为独立基线文档固化：后续新增 Agent、真实模型受控联调、质量门、审核门和 Temporal activity 拆分都应先对照 `docs/specs/ai-agent-architecture-direction-v0.1.md`。
- 多 Agent 工程设计已进一步固化为实现基线：后续新增 Agent 必须先定义输入/输出、失败码、Mock、审计字段和硬开关；前端不暴露 Agent 概念，只消费 OpenAPI v0.1 状态和 `available_actions`。
- Suno/MiniMax 真实调用必须同时满足 AK/SK 安全注入和 `DREAMMAKER_REAL_CALLS_ENABLED=true`；默认关闭，自动化测试不得真实调用。
- 当前用户实测路径已明确：默认先测同步 Mock 主链路和 local-process MP4；真实 AI 出歌的最短下一步是 DreamMaker 音乐单 Provider 受控 smoke，而不是一次性打开 DeepSeek、Suno/MiniMax、Image 2 和公司系统。
- `/internal/integration-readiness` 只用于内部交接/部署前检查，不是用户侧 API；它只读取配置和静态边界，不调用真实公司系统或真实供应商。
- 第 1 批代码应搭建完整商用级工程边界和本地基础设施，不实现业务主链路。
- Boot 应用只产出可执行 jar，不提交 plain jar、构建缓存、node_modules、大体积媒体或真实密钥。

## 上一轮文档基线验证结果

- 已做文档一致性搜索，PRD、技术方案、补项文档、第 1 批任务说明、ADR 和 `AGENTS.md` 已同步到 PRD v0.3 / 技术方案 v0.2 基线。
- 已检查关键新增口径：`apps/web`、React + Vite + TypeScript、移动端优先兼容 PC Web、`PACKAGE_BLOCKED`、`ModerationAdapter.preCheckPublishPackage`、`works.version`、OpenAPI v0.1 覆盖范围。
- 旧版 `v0.2` / `v0.1` 引用仅保留在历史记录、版本记录或“下一步交付物”语境中。
- 已做敏感信息搜索，未发现真实密钥、Token、私钥或生产凭据。
- 已检查 `AGENTS.md` 覆盖项目身份、Source of Truth、固定技术决策、Mock Adapter 边界、前后端规则、图片资产规则、Gemini 前端任务包规则、验证规则、进度记录和 Git commit 指引。
- 本轮只修改 Markdown 文档，未运行代码构建或测试。
- 用户已要求执行 commit，本轮文档基线已纳入 Git 快照。

## 第 1 批工程初始化验证结果

- `docs/api/openapi-v0.1.yaml` 已通过 Ruby YAML 语法解析检查。
- `./gradlew clean build` 成功。
- `./gradlew test` 成功。
- `./gradlew spotlessCheck` 成功。
- `cd apps/web && npm run build && npm test` 成功，Vitest 1 个测试通过。
- `cd apps/render-worker && npm run build && npm test` 成功，Node smoke test 1 个测试通过。
- `docker compose -f deploy/docker-compose.yml --env-file .env.example config` 成功。
- `docker compose -f deploy/docker-compose.yml --env-file .env.example up -d` 成功；PostgreSQL、Redis、Temporal、MinIO、OpenSearch、Prometheus、Grafana 均已启动，主要服务 healthcheck 为 healthy，Temporal UI 已启动。
- `music-api` 可执行 jar smoke 成功：`/health` 返回 `status=OK`，`/actuator/health` 返回 `UP`。
- `music-worker` 可执行 jar smoke 成功：`/actuator/health` 返回 `UP`，启动日志确认 `Temporal connection verified. target=localhost:7233, namespace=default, taskQueue=song-production-local`。
- 敏感信息扫描未发现真实密钥、Token、私钥或 Cookie；命中的 `task-queue` 为技术方案/配置字段，不是敏感凭据。
- 大文件扫描未发现需要提交的大体积产物。
- 本机环境变更：已通过 Homebrew 安装 `openjdk@21` 和 `gradle`；Homebrew 为 Gradle 额外安装了 `openjdk` 依赖。项目验证命令均显式使用 `/opt/homebrew/opt/openjdk@21`。

## 第 2 批首个后端 Mock 闭环验证结果

- `./gradlew test` 成功。
- `./gradlew spotlessCheck` 成功。
- `./gradlew :apps:music-api:bootJar` 成功。
- `./gradlew :apps:music-api:bootJar spotlessCheck test` 成功。
- `music-api` 启动成功，Flyway 连接本地 PostgreSQL 并应用 `V202606050245__init_work_domain.sql`；二次启动时验证 schema 已 up-to-date。
- HTTP smoke 成功：`POST /api/v1/works/inspiration` 创建作品，返回 `LYRICS_READY` 和 `WAITING_CONFIRM`。
- HTTP smoke 成功：`GET /api/v1/works/{work_id}` 返回歌词草案、权益提示、可执行动作和发布交接提示。
- HTTP smoke 成功：`POST /api/v1/works/{work_id}/confirm` 将作品推进到 `GENERATED` / `PACKAGE_READY`。
- HTTP smoke 成功：`GET /api/v1/works/{work_id}/publish-package` 返回 `PACKAGE_READY`、Mock MP4、封面、歌词、时间轴和发布包 URL。
- HTTP smoke 成功：`POST /api/v1/works/{work_id}/publish-package/mark-fetched` 将发布包推进到 `PACKAGE_FETCHED`。
- HTTP smoke 成功：`POST /api/v1/works/lyrics` 的 `work_code` 已验证为 `YYM-YYYYMMDD-XXXXXX` 格式。
- 本轮未启动真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统请求。

## 第 2 批幂等语义验证结果

- `./gradlew :apps:music-api:test spotlessCheck test` 成功。
- `./gradlew :apps:music-api:bootJar` 成功。
- HTTP smoke 成功：同一 `Idempotency-Key`、同一请求内容重复调用 `POST /api/v1/works/lyrics`，返回同一个 `work_id` 和同一份成功响应。
- HTTP smoke 成功：同一 `Idempotency-Key`、不同请求内容调用 `POST /api/v1/works/lyrics`，返回 HTTP 409，错误码为 `IDEMPOTENCY_CONFLICT`。
- `music-api` 已在 smoke 后停止，未留下占用 `8080` 的 API 进程。

## 第 2 批音乐 Provider 边界验证结果

- `./gradlew spotlessApply spotlessCheck test` 成功。
- `MockMusicProviderTest` 成功：Mock Provider 返回 `SUCCEEDED`、音频对象 key 和模拟时长。
- `MiniMaxMusicProviderTest` 成功：Provider 类型为 `MINIMAX`，本地阶段调用真实提交方法会抛出未实现异常，避免自动化误触真实 API。
- `SunoMusicProviderTest` 成功：Provider 类型为 `SUNO`，本地阶段调用真实提交方法会抛出未实现异常，避免自动化误触真实 API。

## 第 2 批 MockMusicProvider 接入验证结果

- `./gradlew spotlessCheck test` 成功。
- `./gradlew :apps:music-api:bootJar` 成功。
- HTTP smoke 成功：`POST /api/v1/works/lyrics` 创建作品后，`POST /api/v1/works/{work_id}/confirm` 通过 `MockMusicProvider` 推进到 `GENERATED` / `PACKAGE_READY`。
- HTTP smoke 成功：作品详情中的 `media_assets.audio_url` 使用 `MockMusicProvider` 返回的 `audio/{work_id}.mp3` 对象 key，`video_duration_ms` 为 180000。
- `music-api` 已在 smoke 后停止，未留下占用 `8080` 的 API 进程。

## 第 2 批 Mock Workflow 编排验证结果

- `./gradlew spotlessApply spotlessCheck test` 成功。
- `MockSongProductionWorkflowTest` 成功：成功路径会写入 4 类媒体资产、发布包 JSON、提交权益，并将 `SONG_PRODUCTION` job 更新为 `SUCCEEDED / PACKAGE_READY`。
- `MockSongProductionWorkflowTest` 成功：音乐 Provider 失败路径会释放已锁权益、记录 `MUSIC_GENERATION_FAILED`、标记作品失败，并将 job 更新为 `FAILED / FAILED`。
- `./gradlew :apps:music-api:bootJar` 成功。
- 新 JAR HTTP smoke 成功：`POST /api/v1/works/lyrics` 创建作品后，`POST /api/v1/works/{work_id}/confirm` 通过 `MockSongProductionWorkflow` 推进到 `GENERATED` / `PACKAGE_READY`。
- 新 JAR HTTP smoke 成功：`GET /api/v1/works/{work_id}/publish-package` 返回发布包 URL、Mock MP4 URL、封面、歌词和时间轴。
- PostgreSQL 抽查成功：最新 smoke 作品的 `generation_jobs` 中 `SONG_PRODUCTION|SUCCEEDED|PACKAGE_READY|completed_at=true`，没有残留运行中的生成 job。

## 第 2 批 Mock 对象存储验证结果

- `./gradlew spotlessApply spotlessCheck test` 成功。
- `LocalObjectStorageClientTest` 成功：可写入本地对象文件并返回公开 URL；路径穿越 key 会被拒绝。
- `MockSongProductionWorkflowTest` 成功：成功路径会调用 `ObjectStorageClient.putObject` 写入 package JSON；storage 写入失败会释放权益、记录 `PACKAGE_BUILD_FAILED`、标记作品失败，并将 job 更新为 `FAILED / FAILED`。
- `./gradlew :apps:music-api:bootJar` 成功。
- 新 JAR HTTP smoke 成功：作品确认出歌后返回 `PACKAGE_READY` 和 `package_url=http://localhost:9000/yanyun-works-local/packages/{work_id}.json`。
- 本地文件 smoke 成功：`build/local-object-storage/yanyun-works-local/packages/{work_id}.json` 已真实写出，文件内 `work_id` 和 `video.url` 与接口返回一致。
- PostgreSQL 抽查成功：最新 smoke 作品的 `SONG_PRODUCTION` job 为 `SUCCEEDED / PACKAGE_READY`，且 `completed_at` 非空。

## 第 2 批 Provider 配置选择验证结果

- `./gradlew spotlessApply spotlessCheck test :apps:music-api:bootJar` 成功。
- `MusicProviderSelectionTest` 成功：空配置默认 `MOCK`，`mock` / `suno` / `MINIMAX` 可解析，未知 Provider 会拒绝。
- `MockSongProductionWorkflowTest` 成功：配置选择 `MINIMAX` 时会调用对应 Provider；配置选择 `SUNO` 且 Provider 抛未实现异常时，会释放权益、标记 `MUSIC_GENERATION_FAILED`，并关闭 job。
- 默认 `MUSIC_PROVIDER=mock` HTTP smoke 成功：确认出歌后进入 `GENERATED / PACKAGE_READY`，发布包文件已写出。
- `MUSIC_PROVIDER=suno` HTTP smoke 成功验证失败边界：确认出歌返回 HTTP 409，作品详情持久化为 `FAILED / FAILED / MUSIC_GENERATION_FAILED`，`retryable=true`，`package_status=PACKAGE_NOT_READY`。
- PostgreSQL 抽查成功：`suno` 失败作品的 `SONG_PRODUCTION` job 为 `FAILED / FAILED / MUSIC_GENERATION_FAILED / completed_at=true`，权益流水包含 `LOCK_GENERATE|LOCKED` 和 `RELEASE_GENERATE|RELEASED`。

## 第 2 批失败恢复与重试闭环验证结果

- 新增 `POST /api/v1/works/{work_id}/music/retry`，仅允许 `FAILED`、`retryable=true` 且失败码属于音乐生成类失败的作品重试。
- `ConfirmWorkRequest` 与 `RetryMusicRequest` 均支持可选 `music_provider`，用于本地和联调阶段按请求选择 `mock` / `suno` / `minimax`。
- `MockSongProductionWorkflow` 已支持请求级 Provider 覆盖服务端 `MUSIC_PROVIDER` 配置；默认配置 `suno` 失败后，可在重试请求中切回 `mock` 完成恢复。
- `WorkStateMachineTest` 成功：`MUSIC_GENERATION_FAILED` 可重试，`PACKAGE_BUILD_FAILED` 不开放音乐重试。
- `MockSongProductionWorkflowTest` 成功：请求级 `music_provider=mock` 可覆盖服务端 `MUSIC_PROVIDER=suno`，并完成出歌链路。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/openapi-v0.1.yaml')"` 成功，OpenAPI v0.1 YAML 可解析。
- `./gradlew spotlessApply spotlessCheck test` 成功。
- `./gradlew :apps:music-api:bootJar` 成功。
- 新 JAR HTTP smoke 成功：`MUSIC_PROVIDER=suno` 启动后，确认出歌返回 HTTP 409，作品详情为 `FAILED / FAILED / MUSIC_GENERATION_FAILED / retryable=true`，`available_actions` 包含 `RETRY_MUSIC`。
- 新 JAR HTTP smoke 成功：调用 `POST /api/v1/works/{work_id}/music/retry` 且请求体为 `{"music_provider":"mock"}` 后，作品恢复到 `GENERATED / PACKAGE_READY`，发布包文件写入 `build/local-object-storage/yanyun-works-local/packages/{work_id}.json`。
- PostgreSQL 抽查成功：同一作品先有 `SONG_PRODUCTION|FAILED|FAILED|MUSIC_GENERATION_FAILED`，后有 `SONG_PRODUCTION|SUCCEEDED|PACKAGE_READY`；权益流水为 `LOCK_GENERATE -> RELEASE_GENERATE -> LOCK_GENERATE -> COMMIT_GENERATE`。

## 第 2 批重试稳定性增强验证结果

- 新增 Flyway migration `V202606050520__add_music_retry_count.sql`，为 `works` 增加 `music_retry_count` 和非负约束。
- 音乐重试上限固定为 2 次；作品详情的 `failure` 新增 `retry_count`、`retry_limit`、`remaining_retry_count` 和 `recommended_action`。
- `retryMusic` 在启动 Workflow 前会用 `works.version` 和条件更新预约本次重试，只允许当前版本、当前用户、音乐类失败、`retryable=true` 且次数未耗尽的作品进入 `GENERATING / QUOTA_LOCKING`。
- `WorkStateMachine` 已把剩余重试次数纳入 `RETRY_MUSIC` 暴露条件；次数耗尽后不再返回 `RETRY_MUSIC`，改由 `CONTACT_SUPPORT` / `RETURN_TO_EDIT` 承接。
- `MockSongProductionWorkflow` 已支持“本次失败后是否仍可重试”的输入；最后一次音乐重试失败会写入 `retryable=false`。
- `./gradlew spotlessApply spotlessCheck test :apps:music-api:bootJar` 成功。
- Flyway 启动验证成功：本地 PostgreSQL 从版本 `202606050245` 迁移到 `202606050520`。
- HTTP smoke 成功：`MUSIC_PROVIDER=suno` 下，初始确认失败返回 `retry_count=0 / retry_limit=2 / remaining_retry_count=2`，第一次重试失败后剩余 1 次，第二次重试失败后 `remaining_retry_count=0`、`retryable=false` 且不再返回 `RETRY_MUSIC`。
- HTTP smoke 成功：次数耗尽后即使请求 `{"music_provider":"mock"}`，`POST /api/v1/works/{work_id}/music/retry` 仍返回 HTTP 409。
- HTTP smoke 成功：另一条作品在初始 `suno` 失败后请求 `{"music_provider":"mock"}`，仍可恢复到 `GENERATED / PACKAGE_READY` 并写出本地发布包。
- HTTP smoke 成功：非法 `music_provider` 返回 HTTP 400，作品保持 `FAILED / MUSIC_GENERATION_FAILED`，`music_retry_count` 不增加，随后仍可用 `mock` 重试恢复成功。
- PostgreSQL 抽查成功：耗尽作品为 `FAILED / MUSIC_GENERATION_FAILED / retryable=false / music_retry_count=2`；恢复作品为 `GENERATED / PACKAGE_READY / music_retry_count=1`；权益流水符合失败释放、成功提交口径。

## 第 2 批 Provider 调用记录与接入前置结果

- 已启用已有 `provider_calls` 表：`MockSongProductionWorkflow` 每次调用音乐 Provider 后记录 `provider`、`operation`、`provider_trace_id`、`status`、`latency_ms`、`request_hash`、`prompt_hash`、`error_code` 和 `error_message`。
- `MockSongProductionWorkflowTest` 已验证成功路径写入 `MOCK / MUSIC_GENERATION / SUCCEEDED`，失败路径写入 `MOCK / MUSIC_GENERATION / FAILED` 和 provider 原始失败码。
- `./gradlew spotlessApply spotlessCheck test :apps:music-api:bootJar` 成功。
- HTTP smoke 成功：`MUSIC_PROVIDER=suno` 初始失败后，切换 `mock` 重试恢复成功。
- PostgreSQL 抽查成功：同一作品写入两条 provider call，分别为 `SUNO|MUSIC_GENERATION|FAILED|PROVIDER_EXCEPTION` 和 `MOCK|MUSIC_GENERATION|SUCCEEDED`，`request_hash` 与 `prompt_hash` 均为 64 位 SHA-256 hex。
- 新增 `docs/integrations/suno-minimax-preintegration-notes.md`，记录 Suno / MiniMax 的 DreamMaker run/status 接口、鉴权方式、请求字段、状态字段、输出文件字段和待确认项。
- 飞书参考资料已通过 `lark-cli docs +fetch` 成功读取；后续用户补充确认 DreamMaker 使用 AccessKey/SecretKey 生成 HS256 JWT，并以 `Authorization: Bearer <jwt>` 调用。
- 新增 `docs/specs/dreammaker-music-provider-v0.1.md`，作为 DreamMaker / Suno / MiniMax 接入骨架规格，明确不提交密钥、自动化不调用真实 Provider、供应商音频 URL 必须先导入对象存储。
- 新增 `modules:dreammaker`，定义 DreamMaker client、run/status request、submit/status response、任务状态、输出文件和失败映射。
- `SunoMusicProvider` 已实现 DreamMaker 参数构造、任务提交、状态轮询、成功音频输出提取、`PROVIDER_TIMEOUT` / `RATE_LIMITED` / `MUSIC_QUALITY_FAILED` 等内部失败码映射。
- `MiniMaxMusicProvider` 已实现 DreamMaker 参数构造、任务提交、状态轮询、成功音频输出提取；确认歌词存在时默认 `lyrics_optimizer=false`，避免改写用户已确认歌词。
- `DreamMakerHttpClient` 已接入 `DREAMMAKER_API_BASE_URL`、`DREAMMAKER_ACCESS_KEY`、`DREAMMAKER_SECRET_KEY`、可选 `DREAMMAKER_USER_ACCESS_TOKEN`、轮询次数、轮询间隔、请求超时和模型配置；客户端会按请求生成 HS256 JWT。
- 新增 `RemoteObjectImporter` 和 `HttpRemoteObjectImporter`，支持把 Provider 返回的 HTTP(S) 音频 URL 下载并写入 `ObjectStorageClient`，拒绝非 HTTP(S) URL 并限制最大下载大小。
- `MockSongProductionWorkflow` 已支持 Provider 音频源 URL 导入；导入失败按 `PACKAGE_BUILD_FAILED` 收口并释放权益，避免作品半生成。
- 新增 `docs/specs/reliable-song-production-orchestration-v0.1.md`，定义 Outbox v0.1 的功能要求、验收口径、边界和非目标。
- 新增 Flyway migration `V202606052300__add_workflow_outbox.sql`，落库 `workflow_outbox`，支持事件状态、尝试次数、锁、失败信息和 processed 状态。
- 新增 `WorkflowDispatchProperties`，支持 `MUSIC_WORKFLOW_DISPATCH_MODE=sync|outbox` 和 outbox dispatcher 配置。
- `WorkService.confirmWork` 在默认 sync 模式保持请求内执行；在 outbox 模式会先将作品置为 `GENERATING / QUOTA_LOCKING`，插入 `SONG_PRODUCTION` job 和 outbox event 后立即返回 202。
- `WorkService.retryMusic` 在 outbox 模式下沿用 `works.version` 和重试次数抢占逻辑，随后写入 outbox event 异步执行。
- `MockSongProductionWorkflow` 已支持复用既有 `generation_jobs.id`，dispatcher 执行时不会重复创建 job。
- 新增 `WorkflowOutboxDispatcher`，可 claim due outbox events，执行 `SongProductionWorkflow`，并将事件标记为 `SUCCEEDED` / `FAILED` / `SKIPPED`；已处理终态作品不重复执行的问题。
- 新增 `docs/specs/temporal-song-production-orchestration-v0.1.md`，定义第 4 批 Temporal 真实编排基础规格、验收口径和非目标。
- 新增 `modules:production`，把 `WorkRepository` 与当前 `MockSongProductionWorkflow` 业务委托从 API 应用中抽出，供 API local 模式和 worker activity 复用。
- 新增 `modules:workflow`，定义 Temporal workflow/activity 合约、deterministic workflow id 和 `TemporalSongProductionWorkflowImpl`。
- `WorkflowOutboxDispatcher` 已改为依赖 `SongProductionWorkflowStarter`，可按配置走 local starter 或 Temporal starter。
- `TemporalSongProductionWorkflowStarter` 已用 `work_id + job_id` 生成 deterministic workflow id；重复启动命中 `AlreadyStarted` 时按幂等成功处理。
- `music-worker` 已注册 Temporal workflow 与 activity；activity 通过 `SongProductionActivityAdapter` 委托当前生产链路，worker 生命周期会启动并关闭 `WorkerFactory` 和 Temporal stubs。
- `music-worker` 已补齐本地 Mock 生产链路所需 Spring bean：Mock 权益、审核、发布、对象存储、远程对象导入和音乐 Provider 选择。
- `DreamMakerHttpClient` 与 `DreamMakerProperties` 已从 API 应用移动到共享 `modules:dreammaker`，API sync/local 和 Temporal worker 路径共用同一套 JWT、配置和安全检查。
- 新增 `DREAMMAKER_REAL_CALLS_ENABLED`，默认 false；未显式打开时，即使请求级 `music_provider=suno|minimax` 也会在外部 HTTP 请求前失败。
- 新增 `docs/specs/deepseek-knowledge-lyrics-v0.1.md`，定义第 6 批 DeepSeek / 知识库写词润色 Mock 链路规格、非目标、持久化口径和验收标准。
- 新增 `modules:knowledge`，定义 `KnowledgeService`、检索请求/结果、知识库引用和 `MockKnowledgeService`。
- 新增 `modules:prompt`，定义 `PromptTemplateService`、Prompt 渲染请求/结果和 `MockPromptTemplateService`。
- 新增 `modules:deepseek`，定义 `DeepSeekLyricsClient`、写词请求/响应和 `MockDeepSeekLyricsClient`。
- 新增 `modules:lyrics`，定义 `LyricsGenerationService`、写词请求/结果、操作类型和 `DefaultLyricsGenerationService`，负责知识库检索、Prompt 渲染、模型生成和低质量自动重写。
- `WorkService` 已接入统一写词链路，灵感成歌、填词成歌、AI 润色、AI 续写都通过 `LyricsGenerationService` 产出歌词草案。
- `lyrics_drafts` 已启用既有 `cover_prompt_seed`、`quality_score`、`knowledge_base_version` 和 `prompt_template_versions` 字段，用于后续封面链路、审计和回放。
- AI 润色和 AI 续写已共享 2 次用户侧编辑上限；第三次 AI 编辑返回业务冲突。
- 修复幂等层包裹写操作时业务 4xx 被事务提交异常覆盖成 500 的问题；相关写方法已声明 `noRollbackFor = ResponseStatusException.class`。
- `Idempotency-Key` 缺失或过短现在由业务校验返回 400，不再被 Spring 缺失 header 异常兜底成 500。
- `music-worker` 已注册 DreamMaker client、Suno Provider 和 MiniMax Provider；Temporal 模式真实联调不会因为 worker 仅有 Mock Provider 而失败。
- `MusicGenerationResult` 已增加 `modelName`，`provider_calls.model_name` 可写入 `suno:music-gen:{model}`、`music-minimax:text-to-music:{model}` 或 `mock`。
- 供应商失败消息进入 `provider_calls`、作品失败状态和用户响应前会脱敏 Bearer token、JWT、key/token 字段并截断。
- 新增第 5 批操作文档：受控真实联调 runbook、验收清单、凭据与日志规则、开放问题跟踪、公司交接说明。
- 新增 `modules:image2`，定义封面生成请求、结果、服务接口和 `MockCoverGenerationService`；当前返回 deterministic 16:9 Mock 封面资产描述，不调用真实 Image 2。
- 新增 `modules:media`，定义媒体资产描述、视频渲染请求、结果、服务接口和 `MockVideoRenderService`；当前返回 deterministic 16:9 Mock 视频与时间轴资产描述，不调用真实外部渲染服务。
- `MockSongProductionWorkflow` 已通过 `CoverGenerationService` / `VideoRenderService` 生成并写入 `COVER`、`VIDEO`、`TIMELINE` 资产，发布包 JSON 不再写死封面、视频和 timeline 路径。
- `MockSongProductionWorkflowTest` 已覆盖成功路径四类资产写入、封面/视频 1920x1080、发布包引用媒体 object key，以及视频渲染失败时释放权益并阻止发布包生成。
- `apps/render-worker` 已新增 `LyricVideo16x9`、样例输入和 `render:sample` 脚本，可用 Remotion/FFmpeg 渲染本地 MP4。

## 第 2 批 DreamMaker Provider 接入骨架验证结果

- `./gradlew spotlessApply` 成功。
- `./gradlew spotlessCheck test :apps:music-api:bootJar` 成功。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/openapi-v0.1.yaml')"` 成功，OpenAPI YAML 可解析。
- `DreamMakerTaskStatusTest` 成功：DreamMaker `queued` / `running` / `success` / `failed` 状态可映射。
- `SunoMusicProviderTest` 成功：Fake DreamMaker 成功响应可返回 `SUCCEEDED` 和音频源 URL；长期 `running` 会映射为 `PROVIDER_TIMEOUT`。
- `MiniMaxMusicProviderTest` 成功：Fake DreamMaker 成功响应可返回 `SUCCEEDED` 和音频源 URL；失败状态可映射为 `MUSIC_QUALITY_FAILED`。
- `DreamMakerHttpClientTest` 成功：本地 HTTP server 模拟 DreamMaker run/status，可解析 task id、状态、相对音频 URL 和 duration；缺失 `DREAMMAKER_ACCESS_KEY` / `DREAMMAKER_SECRET_KEY` 会在发请求前失败。
- `HttpRemoteObjectImporterTest` 成功：本地 HTTP 音频可导入 `ObjectStorageClient`；非 HTTP(S) source URL 会被拒绝。
- `MockSongProductionWorkflowTest` 成功：已有 object key 不触发下载；Provider 音频源 URL 会先导入对象存储再写入 `AUDIO` 媒体资产。
- 本地 API JAR smoke 成功：默认 `MUSIC_PROVIDER=mock` 下，`/health` 返回 `OK`，作品可从填词创建推进到 `GENERATED / PACKAGE_READY`，发布包 URL 存在。
- 本地 API JAR smoke 成功：请求级 `music_provider=suno` 且未配置 `DREAMMAKER_ACCESS_KEY` / `DREAMMAKER_SECRET_KEY` 时，确认出歌返回 HTTP 409，作品持久化为 `FAILED / MUSIC_GENERATION_FAILED / retryable=true`，`available_actions` 包含 `RETRY_MUSIC`。
- smoke 后已停止 `music-api`，`8080` 未残留监听进程。

## 第 3 批 Outbox 可靠编排基础验证结果

- `./gradlew :apps:music-api:test` 成功。
- `./gradlew spotlessApply` 成功。
- `./gradlew spotlessCheck test :apps:music-api:bootJar` 成功。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/openapi-v0.1.yaml')"` 成功，OpenAPI YAML 可解析。
- `WorkServiceWorkflowDispatchTest` 成功：默认 sync 模式仍调用 workflow inline；outbox 模式会写 generation job、enqueue outbox，并返回 `GENERATING / QUOTA_LOCKING`。
- `WorkflowOutboxDispatcherTest` 成功：pending event 可执行 workflow 并标记成功；workflow 抛异常会标记 `WORKFLOW_DISPATCH_FAILED` 并安排重试；已终态 `GENERATED` 的作品不会重复执行 workflow。
- Flyway 启动验证成功：本地 PostgreSQL 从版本 `202606050520` 迁移到 `202606052300`，`workflow_outbox` 已落库。
- 默认 sync HTTP smoke 成功：确认出歌仍直接返回 `GENERATED / PACKAGE_READY`，发布包 URL 存在。
- outbox HTTP smoke 成功：确认出歌先返回 `GENERATING / QUOTA_LOCKING`，随后 dispatcher 自动推进到 `GENERATED / PACKAGE_READY`。
- PostgreSQL 抽查成功：outbox smoke 作品对应 `workflow_outbox` 为 `SUCCEEDED / SONG_PRODUCTION_REQUESTED / attempt_count=0 / processed_at=true`；`generation_jobs` 为 `SUCCEEDED / PACKAGE_READY / completed_at=true`。
- smoke 后已停止 `music-api`，`8080` 未残留监听进程。

## 第 3 批 DreamMaker JWT 鉴权补齐验证结果

- `./gradlew :modules:dreammaker:test --tests com.yanyun.music.dreammaker.DreamMakerHttpClientTest` 成功：本地 fake DreamMaker server 收到有效 `Authorization: Bearer <jwt>`；测试验证了 JWT 三段结构、`alg=HS256`、`typ=JWT`、`iss=<access_key>`、`exp=now+1800s`、`nbf=now-5s` 和 HMAC 签名。
- `DreamMakerHttpClientTest` 同时验证可选 `DREAMMAKER_USER_ACCESS_TOKEN` 会透传为 `X-Access-Token`。
- `DreamMakerHttpClientTest` 同时验证缺失 `DREAMMAKER_ACCESS_KEY` / `DREAMMAKER_SECRET_KEY` 会在 HTTP 请求前失败，并映射为 `MUSIC_GENERATION_FAILED`。
- `./gradlew spotlessApply` 成功。
- `./gradlew spotlessCheck test :apps:music-api:bootJar` 成功。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/openapi-v0.1.yaml')"` 成功，OpenAPI YAML 可解析。
- `env_auditor.py` 密钥扫描成功：0 findings。
- 已额外扫描用户提供过的真实 DreamMaker AccessKey/SecretKey 原文，仓库内无匹配。

## 第 4 批 Temporal 真实编排基础验证结果

- `./gradlew :modules:production:compileJava :apps:music-api:compileJava :apps:music-worker:compileJava` 成功。
- `./gradlew :modules:workflow:test :modules:production:compileJava` 成功。
- `./gradlew :apps:music-api:test --tests com.yanyun.music.api.workflow.WorkflowOutboxDispatcherTest` 成功。
- `./gradlew :apps:music-worker:test` 成功。
- `./gradlew spotlessApply` 成功。
- `./gradlew spotlessCheck test :apps:music-api:bootJar :apps:music-worker:bootJar` 成功。
- 本地 Docker 基础设施仍健康运行：PostgreSQL、Redis、Temporal、Temporal UI、MinIO、OpenSearch、Prometheus、Grafana 均为 up/healthy。
- outbox local JAR smoke 成功：`WORKFLOW_OUTBOX_DISPATCH_TARGET=local` 下，填词创建作品、确认出歌、dispatcher 本地委托执行后，作品推进到 `GENERATED / PACKAGE_READY`，发布包 URL 存在。
- Temporal JAR smoke 成功：先启动 `music-worker` 注册 `song-production-local` task queue，再启动 API 的 `WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal`，填词创建作品并确认出歌后，worker activity 将作品推进到 `GENERATED / PACKAGE_READY`。
- PostgreSQL 抽查成功：Temporal smoke 作品对应 outbox 为 `SUCCEEDED / attempt_count=0`，generation job 为 `SUCCEEDED / PACKAGE_READY`，work 为 `GENERATED / PACKAGE_READY / PACKAGE_READY`。
- 修复并验证了一个 Temporal starter 装配问题：`TemporalSongProductionWorkflowStarter` 不再作为 `@Service` 被无参实例化，而是在 Temporal dispatch 配置类中显式创建。
- smoke 后已停止 `music-api` 和 `music-worker`，未留下占用 `8080` / `8081` 的应用进程。

## 第 5 批 DreamMaker 受控真实联调准备验证结果

- `./gradlew :modules:dreammaker:test :apps:music-api:compileJava :apps:music-worker:compileJava` 成功。
- `./gradlew :modules:dreammaker:test :modules:suno:test :modules:minimax:test :apps:music-api:test --tests com.yanyun.music.production.MockSongProductionWorkflowTest :apps:music-worker:test` 成功。
- `./gradlew spotlessApply` 成功。
- `./gradlew spotlessCheck test :apps:music-api:bootJar :apps:music-worker:bootJar` 成功。
- `DreamMakerHttpClientTest` 成功：未打开 `DREAMMAKER_REAL_CALLS_ENABLED` 时会在外部 HTTP 请求前失败；fake server 模式可验证 JWT；非 2xx 响应只保留脱敏短消息。
- `DreamMakerTaskStatusTest` 成功：未知供应商状态映射为 `UNKNOWN`，Provider 会继续轮询直到终态或超时。
- `SunoMusicProviderTest` / `MiniMaxMusicProviderTest` 成功：返回结果携带真实模型标识，用于写入 `provider_calls.model_name`。
- `MockSongProductionWorkflowTest` 成功：`provider_calls.model_name` 写入正确，供应商失败信息进入 provider call、work failure 和 job failure 前会脱敏。
- JAR HTTP guard smoke 成功：API 在 `DREAMMAKER_ACCESS_KEY=fake`、`DREAMMAKER_SECRET_KEY=fake`、`DREAMMAKER_REAL_CALLS_ENABLED=false` 下启动；请求 `music_provider=suno` 确认出歌返回本地保护失败，作品为 `FAILED / MUSIC_GENERATION_FAILED`，`provider_calls` 记录 `SUNO|suno:music-gen:chirp-crow|FAILED`，没有真实外部调用。
- smoke 后已停止 `music-api`，`8080` 未残留监听进程。

## 第 6 批 DeepSeek / 知识库写词润色 Mock 链路验证结果

- `./gradlew :modules:lyrics:test :apps:music-api:compileJava :apps:music-api:compileTestJava` 成功。
- `./gradlew spotlessApply spotlessCheck test :apps:music-api:bootJar :apps:music-worker:bootJar` 成功。
- `DefaultLyricsGenerationServiceTest` 成功：写词结果携带知识库版本、燕云引用、Prompt 模板版本和质量分；低质量 Fake 结果会触发一次内部重写。
- `WorkServiceTransactionPolicyTest` 成功：被幂等层包裹的主要写方法均声明 `noRollbackFor = ResponseStatusException.class`，避免业务 4xx 被事务提交异常覆盖成 500。
- JAR HTTP smoke 成功：缺失 `Idempotency-Key` 的 `POST /works/inspiration` 返回 HTTP 400。
- JAR HTTP smoke 成功：`POST /works/inspiration` 通过 Mock 写词链路创建作品，状态为 `LYRICS_READY / WAITING_CONFIRM`，作品详情返回燕云引用 `Yanyun frontier imagery` 和 `Yanyun ensemble motifs`。
- JAR HTTP smoke 成功：AI 润色后剩余编辑次数从 2 变 1，AI 续写后剩余编辑次数从 1 变 0。
- JAR HTTP smoke 成功：第三次 AI 润色返回 HTTP 409，不再被事务异常覆盖为 500。
- PostgreSQL 抽查成功：最新 smoke 作品的 `lyrics_drafts` 已持久化 `knowledge_base_version=mock-yanyun-kb-v0`、`prompt_template_versions={"lyrics.continue.v1": 1}`，且 `cover_prompt_seed`、`quality_score` 均非空。
- smoke 后已停止 `music-api`，未留下占用 `8080` 的 API 进程。

## 第 7 批封面与 Remotion/FFmpeg MP4 成片基础验证结果

- 新增 `docs/specs/cover-video-rendering-v0.1.md`，明确本批只做 Mock/Fake 封面与视频生成边界、16:9 MP4 成片工具链验证、发布包引用更新，不调用真实 Image 2 或真实外部渲染服务。
- `./gradlew :modules:image2:spotlessCheck :modules:media:spotlessCheck :modules:production:spotlessCheck :apps:music-api:test --tests com.yanyun.music.production.MockSongProductionWorkflowTest` 成功。
- `./gradlew spotlessApply spotlessCheck test :apps:music-api:bootJar :apps:music-worker:bootJar` 成功。
- `cd apps/render-worker && npm run build` 成功。
- `cd apps/render-worker && npm test` 成功：`LyricVideo16x9` 输出设置为 1920x1080、30fps、240 frames，样例歌词覆盖完整 composition 时长。
- `cd apps/render-worker && npm run render:sample` 成功：Remotion/FFmpeg 生成 `apps/render-worker/out/sample.mp4`。
- `ffprobe` 检查成功：样例 MP4 包含 `h264` 视频流、`aac` 音频流，分辨率为 1920x1080，时长约 8 秒，文件约 973KB。
- JAR HTTP smoke 成功：`MUSIC_PROVIDER=mock` 下，填词创建作品、确认出歌后进入 `GENERATED / PACKAGE_READY`。
- JAR HTTP smoke 成功：作品详情 `media_assets` 返回 audio、cover、video URL，video duration 为 180000，video file size 为 12000000。
- PostgreSQL 抽查成功：最新 smoke 作品写入 `AUDIO`、`COVER`、`TIMELINE`、`VIDEO` 四类 `media_assets`；`COVER` 和 `VIDEO` 均为 1920x1080；metadata 分别包含 `mock-image2` 和 `mock-remotion-ffmpeg`。
- 发布包 smoke 成功：`GET /api/v1/works/{work_id}/publish-package` 返回 `PACKAGE_READY`，`video.url`、`cover.url`、`lyrics.timeline_url` 均由对应媒体资产 object key 推导。
- 本地文件 smoke 成功：`build/local-object-storage/yanyun-works-local/packages/{work_id}.json` 已真实写出。
- smoke 后已停止 `music-api`，未留下占用 `8080` 的 API 进程。
- 第 7 批完成时 Java workflow 尚未直接调用 render-worker 生成真实业务 MP4；该缺口已在第 11 批通过 `RENDER_WORKER_MODE=local-process` 本地进程边界补上，但默认仍保持 Mock，长视频真实成片仍需手动 smoke 和后续生产级 render service 方案确认。

## 第 8 批 MinIO/S3 发布包强化验证结果

- 新增 `docs/specs/minio-s3-publish-package-storage-v0.1.md`，明确本批只强化发布包 JSON 对象存储、URL 签发/刷新和本地 MinIO smoke，不把真实媒体文件上传、真实云账号或公司对象存储纳入完成条件。
- `./gradlew :modules:storage:test :apps:music-api:compileJava :apps:music-api:compileTestJava :apps:music-worker:compileJava` 成功。
- `./gradlew spotlessApply :modules:storage:test :modules:publish:test :apps:music-api:test --tests com.yanyun.music.api.work.WorkServiceWorkflowDispatchTest --tests com.yanyun.music.production.MockSongProductionWorkflowTest` 成功。
- `./gradlew spotlessCheck test :apps:music-api:bootJar :apps:music-worker:bootJar` 成功。
- S3/MinIO JAR smoke 成功：`OBJECT_STORAGE_PROVIDER=s3`、`S3_ENDPOINT=http://localhost:9000`、`S3_BUCKET_YANYUN_WORKS=yanyun-works-local` 下，填词创建作品并确认出歌后进入 `PACKAGE_READY`。
- MinIO 对象抽查成功：发布包写入 `yanyun-ai-music/local/2026/06/06/{work_id}/package/publish-package.json`，`mc stat` 可见对象，下载后的 package JSON `work_id` 与作品一致。
- 发布包刷新 smoke 成功：`POST /api/v1/works/{work_id}/publish-package/refresh-url` 使用数据库中的 `package_object_key` 重新签发 URL，返回 URL 包含 S3 签名参数。
- 默认 local JAR smoke 成功：`OBJECT_STORAGE_PROVIDER=local` 下，确认出歌后发布包 JSON 写入 `build/local-object-storage/yanyun-works-local/yanyun-ai-music/local/2026/06/06/{work_id}/package/publish-package.json`，作品为 `GENERATED / PACKAGE_READY`。
- smoke 后已停止 `music-api`，未留下占用 `8080` 的 API 进程。
- 当前 Java workflow 的 `AUDIO`、`COVER`、`VIDEO`、`TIMELINE` 仍是 Mock asset object key/URL 口径；本批只保证发布包 JSON 自身可写入 local 或 MinIO/S3。

## 第 10 批公司 Adapter 接入与部署交接准备验证结果

- 新增 `docs/specs/company-adapter-deployment-handoff-v0.1.md`，明确本批只做公司 Adapter 替换边界、部署变量、内部 readiness 报告和交接文档，不接真实公司系统。
- 新增 `docs/handover/company-adapter-deployment-handoff-v0.1.md`，覆盖账号、审核、权益、发布、分享五类公司系统替换清单、接口口径、部署变量、smoke 步骤和禁止事项。
- `modules:config-center` 新增 `CompanyIntegrationProperties`、`IntegrationReadinessService`、`IntegrationReadinessReport` 和组件状态模型。
- `music-api` 新增内部接口 `GET /internal/integration-readiness`，默认本地配置下返回 `READY_FOR_LOCAL`，但公司账号、审核、权益、发布、分享组件均会标记 Mock/待替换状态。
- `./gradlew :modules:config-center:test :apps:music-api:test --tests com.yanyun.music.api.IntegrationReadinessControllerTest :apps:music-api:compileJava` 成功。
- `./gradlew spotlessApply spotlessCheck test :apps:music-api:bootJar :apps:music-worker:bootJar` 成功。
- JAR HTTP smoke 成功：默认本地配置下访问 `/internal/integration-readiness` 返回 `environment=local`、`overall_status=READY_FOR_LOCAL`、`component_count=10`、`company_account.status=MOCK_ONLY`、`company_quota.blocks_company_deployment=true`；新增 `render_worker` 组件默认显示 `MOCK_ONLY`。
- readiness 敏感信息 smoke 成功：返回内容未命中 `sk-`、`Bearer` 或测试 secret 形态；接口只列环境变量名，不输出密钥值。
- smoke 后已停止 `music-api`，未留下占用 `8080` 的 API 进程。
- 当前仍没有真实公司 Adapter 实现；本批完成的是交接准备、静态 readiness 和替换清单。

## 第 11 批 render-worker 本地进程调用边界验证结果

- 新增 `docs/specs/render-worker-local-process-integration-v0.1.md`，明确本批只做 Java 到 render-worker CLI 的本地进程边界，默认仍为 mock，不接真实公司系统。
- `apps/render-worker` 新增 `render:job` CLI，输入 render job JSON，输出 MP4 文件、timeline JSON 和结果 JSON；`LyricVideo16x9` 已支持按 `duration_ms` 动态计算 `durationInFrames`。
- 修复短字幕片段真实渲染时 `interpolate` 输入区间倒序问题；1 秒、2 行歌词 smoke 可正常渲染。
- `modules:production` 新增 `LocalProcessVideoRenderService`，通过外部进程调用 render-worker，限制输出文件必须位于临时目录内，再把 MP4 和 timeline JSON 写入 `ObjectStorageClient`。
- API / worker 配置新增 `yanyun.render-worker.*`，`.env.example` 新增 `RENDER_WORKER_MODE`、命令、参数、超时和对象 key 前缀变量。
- `/internal/integration-readiness` 新增 `render_worker` 组件；默认 mock 标记 `MOCK_ONLY`，`local-process` 标记 `READY_FOR_LOCAL` 但仍阻塞公司部署确认。
- `cd apps/render-worker && npm run build` 成功。
- `cd apps/render-worker && npm test` 成功，4 个 Node smoke 测试覆盖 16:9 输出、样例 timeline、180 秒动态时长和空歌词兜底。
- `cd apps/render-worker && npm run render:job` 的 1 秒真实渲染 smoke 成功，输出 `duration_ms=1000`、`duration_in_frames=30`、`width=1920`、`height=1080`、`fps=30`。
- `./gradlew :modules:production:test :modules:config-center:test :apps:music-api:test --tests com.yanyun.music.api.IntegrationReadinessControllerTest :apps:music-api:compileJava :apps:music-worker:compileJava` 成功。
- 当前本批仍不做 2-4 分钟长视频自动化渲染，不做音频 mux 完整校验，不改变默认 Mock 主链路；长视频和生产级 render service 需要后续手动 smoke 与部署方案确认。

## 第 9 批前端初审与联调支撑验证结果

- 新建长期 Goal：前端已交付后，优先推进前端原型验收修复任务包、后端短链路联调支撑、前后端本地 smoke、项目进度和阶段快照。
- `prototypes/Claude-web-v1` 初审已执行：`npm test`、`npm run typecheck`、`npm run build` 均通过。
- `prototypes/Claude-web-v1` Playwright smoke 已执行：390px 宽度可在 `?mock=1` 演示模式下完成灵感成歌到成品页，1440px 首页无横向溢出；控制台未发现业务错误。
- 初审时前端尚未严格通过验收：缺少“我的作品”列表和 `GET /works`、成品页发布交接信息不足、`PACKAGE_BLOCKED` 映射错误、润色请求体不符合 OpenAPI、`RETRY_COVER` / `RERENDER_VIDEO` 未实现、错误态未展示 `request_id`、详情页缺少关键状态信息。
- 已新增 `docs/frontend/claude-web-v1-acceptance-fix-task-package.md`，要求前端实现者只修改 `prototypes/Claude-web-v1` 并补齐上述问题；这些缺口已在后续“第 9 批前端验收修复验证结果”和“真实 UI Smoke 脚本化验证结果”中完成修复和复验。
- `MockMusicProvider` 已支持可配置模拟音频时长；新增单元测试覆盖自定义时长和非法时长拒绝。
- `./gradlew spotlessCheck :modules:music-provider:test :apps:music-api:compileJava :apps:music-worker:compileJava` 在显式设置 `JAVA_HOME=/opt/homebrew/opt/openjdk@21` 后成功。
- 首次 Gradle 命令未设置 `JAVA_HOME` 时失败，原因是当前 shell 找不到 Java Runtime；已确认 `/opt/homebrew/opt/openjdk@21` 可用，运行手册已保留该环境设置说明。
- 同步 Mock 后端主链路 smoke 成功：基础设施已运行且健康，API 使用 `MOCK_MUSIC_DURATION_MS=1000 MUSIC_PROVIDER=mock MUSIC_WORKFLOW_DISPATCH_MODE=sync WORKFLOW_OUTBOX_DISPATCHER_ENABLED=false` 启动。
- HTTP smoke 成功：`GET /health` 和 `GET /actuator/health` 正常；`POST /api/v1/works/lyrics` 创建作品 `0932c4d4-f1e1-40a7-a3d9-00e45ca623b1`，进入 `LYRICS_READY / WAITING_CONFIRM`。
- HTTP smoke 成功：`POST /api/v1/works/{work_id}/confirm` 后作品进入 `GENERATED / PACKAGE_READY`，详情中的 `media_assets.video_duration_ms=1000`，证明 `MOCK_MUSIC_DURATION_MS` 生效。
- HTTP smoke 成功：`GET /publish-package` 返回 `PACKAGE_READY`、发布包 URL、视频 URL、封面 URL、timeline URL 和歌词；`refresh-url` 可刷新过期时间；`mark-fetched` 后详情为 `PACKAGE_FETCHED`。
- 数据库抽查成功：`works` / `publish_packages` 为 `GENERATED / PACKAGE_READY / PACKAGE_FETCHED`，发布包 object key 已持久化，`provider_calls` 记录 `MOCK / MUSIC_GENERATION / SUCCEEDED`。
- 媒体资产抽查成功：`AUDIO`、`VIDEO`、`TIMELINE` 的 `duration_ms` 均为 1000；`COVER` 正常写入。
- 本地发布包 JSON 抽查成功：`bootRun` 下实际文件位于 `apps/music-api/build/local-object-storage/.../publish-package.json`；运行手册已补充相对路径说明。
- render-worker local-process smoke 初次使用相对 `RENDER_WORKER_WORKING_DIRECTORY=apps/render-worker` 时失败，原因是 `bootRun` 工作目录为 `apps/music-api`；已修复 `LocalProcessVideoRenderService` 的相对工作目录解析，并补回归测试。
- `cd apps/render-worker && npm run build && npm test` 成功，4 个 render-worker Node smoke 测试通过。
- `./gradlew :modules:production:test spotlessCheck :apps:music-api:compileJava :apps:music-worker:compileJava` 成功，覆盖相对工作目录向上查找回归测试。
- render-worker local-process API smoke 成功：使用默认相对 `RENDER_WORKER_WORKING_DIRECTORY=apps/render-worker`、`MOCK_MUSIC_DURATION_MS=1000` 确认出歌后，作品 `4ef3dad4-6a3e-4614-b01b-b6106a5827f1` 进入 `GENERATED / PACKAGE_READY`。
- MP4 文件抽查成功：`ffprobe` 显示生成视频为 H.264、1920x1080、30fps、1.000000 秒，文件大小 137159 bytes；数据库 `VIDEO` 和 `TIMELINE` 资产 metadata 标记 `source_mode=local-process`。
- 新增 `scripts/smoke/api-main-flow.sh`，默认 smoke 在同步 Mock API 下成功，作品 `ec36246c-303a-4116-a7e6-10b1b33e14da` 跑通创建、确认、发布包、刷新、标记交接、数据库和本地对象文件抽查。
- 同一脚本的 local-process 分支成功，作品 `8576f4f3-c619-4ed8-b38f-82b7600fb7a3` 跑通并通过本地 MP4 `ffprobe` 校验。

## 第 9 批前端验收修复验证结果

- 用户明确要求本 Agent 直接修复 `prototypes/Claude-web-v1`，本轮未修改 `apps/web`、后端 Java 模块、数据库或部署配置。
- 新增 `#/works` “我的作品”页面，接入 `GET /works` 演示服务和 API 类型，支持 loading、empty、error、分页加载、作品卡片和点击进入详情。
- 成品页已补齐发布交接展示：`package_url`、过期时间、`package_json.video.url`、`package_json.cover.url`、`package_json.lyrics.text`，并保留 `media_assets` 优先展示音频、封面、视频。
- `PACKAGE_BLOCKED` 不再被派生为生成中，改为成品/交接页阻断态；页面展示 `blocked_reason` 和 `publish_handoff_hint.message`，并避免继续展示“标记已交接”。
- `LyricsPolishRequest.instruction` 已改为必填；润色弹窗空内容禁用提交，删除“不填也可以”的用户文案，演示后端对空润色返回 400。
- 补齐 `RETRY_COVER` 与 `RERENDER_VIDEO` 前端 action 和 API 调用；失败页按 `available_actions` 渲染对应按钮。
- 错误 UI 已补 `request_id` 展示，Toast / Banner / 异常文案不再只显示通用错误。
- 作品详情页已展示 `status`、`generation_stage`、`package_status`、权益提示、发布交接提示、失败码、失败消息、推荐动作和剩余重试次数。
- 基础可访问性修复已完成：Modal 支持焦点陷阱与关闭后焦点恢复；伪 tab/radio 控件改为按钮状态语义；弱文本颜色已调亮。
- 真实后端 UI smoke 期间发现确认页移动端 action bar 与详情卡片重叠；已取消 sticky action bar，改为普通流式布局并复测通过。
- 第 3 次润色 409 提示原先只通过短暂 Toast 展示；已在编辑弹窗内持久展示友好错误和 `request_id`，并补单元测试。
- `npm test` 成功：8 个测试文件、27 个测试通过。
- `npm run typecheck` 成功。
- `npm run build` 成功。
- Playwright smoke 成功：`http://127.0.0.1:5274/?mock=1` 的 390px 首页、1440px 首页、390px 作品列表空态、1440px 作品列表空态均无明显横向溢出或重叠；console 仅有 React DevTools 开发提示。
- Playwright 生成的本地截图 `claude-web-v1-*.png` 属于烟测产物，不应提交到仓库。

## 第 9 批前后端真实 UI 联调验证结果

- 启动方式：后端使用 `MUSIC_PROVIDER=mock MOCK_MUSIC_DURATION_MS=1000 MUSIC_WORKFLOW_DISPATCH_MODE=sync WORKFLOW_OUTBOX_DISPATCHER_ENABLED=false RENDER_WORKER_MODE=mock ./gradlew :apps:music-api:bootRun`；前端使用 `npm run dev -- --host 127.0.0.1 --port 5274`，不加 `?mock=1`。
- Docker 基础设施健康：PostgreSQL、Redis、Temporal、MinIO、OpenSearch、Prometheus、Grafana 均保持运行；后端 Flyway schema 已 up-to-date。
- 真实后端 UI smoke 成功：灵感成歌创建作品 `5b75751a-69fc-48bf-b295-8029892298bc`，进入歌词确认页，展示标题、摘要、歌词、music prompt、燕云引用、权益提示和发布交接提示。
- 润色必填 UI 成功：空润色时“开始润色”按钮禁用；填入润色方向后真实后端返回第 2 版歌词，`polish_remaining_count=1`。
- 续写 UI 成功：第 2 次编辑后真实后端返回第 3 版歌词，`polish_remaining_count=0`。
- 第 3 次润色 UI 成功：真实后端返回 409，弹窗内稳定显示“改词次数已用完，本次未生效”和 `request_id=37a457ea-b526-4804-a3e8-729fa0ef300f`；浏览器 console 记录的 409 network error 为预期触发项，不是未处理异常。
- 确认出歌 UI 成功：作品 `5b75751a-69fc-48bf-b295-8029892298bc` 进入 `GENERATED / PACKAGE_READY`，成品页展示 audio、cover、video、`package_url`、视频地址、封面地址和歌词正文；`media_assets.video_duration_ms=1000`。
- 发布交接 UI 成功：刷新下载链接成功；标记已交接后真实 API 返回 `PACKAGE_FETCHED`，页面状态显示“已交接”，不再展示“标记已交接”按钮，仅保留刷新下载链接。
- 作品列表 UI 成功：`#/works` 从真实后端列表展示最新作品 `YYM-20260606-CDEC89`，状态为“已完成 / 成品就绪 / 已交接”，点击后可回到作品详情。
- 失败页重试 UI 成功：通过 API 创建作品 `c909c925-2bb7-4e0d-9c7b-c82af9992511` 并用 `music_provider=suno` 触发受控失败，前端失败页展示 `MUSIC_GENERATION_FAILED`、失败消息、剩余重试次数和建议操作；点击“重新生成”后前端调用 mock 重试，真实 API 恢复到 `GENERATED / PACKAGE_READY`。
- 本轮启动的 API 与前端 dev server 已停止，`8080` 和 `5274` 无残留监听。

## 第 9 批真实 UI Smoke 脚本化验证结果

- 新增 `prototypes/Claude-web-v1/scripts/smoke-real-backend.mjs` 和 `npm run smoke:real-backend`，脚本默认使用 `API_ORIGIN=http://localhost:8080`、`FRONTEND_PORT=5274`、`MOCK_USER_ID=mock_user_001`。
- 新增前端 devDependency `playwright@1.57.0`；本机已执行 `npx playwright install chromium` 下载 Chromium/headless shell 到本地 Playwright cache。
- 脚本会自行启动并停止 Vite；API 仍由调用方按 runbook 提前启动。
- 脚本化 smoke 成功：主流程作品 `e48e33dc-8c72-467f-93f3-68549185ae5a` 跑通灵感成歌、润色、续写、第三次润色 409、确认出歌、发布交接、作品列表和移动/桌面无横向溢出检查。
- 脚本化 smoke 成功：失败重试作品 `e612f8c9-5818-4848-96e5-88ba5cc0c396` 通过 `music_provider=suno` 受控失败进入失败页，前端点击“重新生成”后恢复到 `GENERATED / PACKAGE_READY`。
- 脚本首次运行暴露出成品页收口边界：标记交接后本地 `pkg.package_status` 已是 `PACKAGE_FETCHED`，但 `work` props refresh 尚未完成时按钮仍可能短暂存在；已修复为同时参考 `pkg.package_status`，并把 `PACKAGE_FETCHED` 用户文案固定为“作品已交接给社区发布流程。”。
- `npm test` 成功：8 个测试文件、28 个测试通过。
- `npm run typecheck` 成功。
- `npm run build` 成功。
- `npm run smoke:real-backend` 成功。

## 本地商用闭环交付清单补齐结果

- 新增 `docs/checklists/local-commercial-delivery-acceptance.md`，覆盖仓库与文档基线、本地基础设施、后端主链路、前端创作工作台、真实模型联调准备、公司 Adapter 交接、安全日志和交付判定。
- 更新 `README.md` 当前阶段：Claude Web v1 前端验收缺口已修复，并已完成真实后端模式 UI smoke 脚本化复验；新增总体验收清单入口和 Playwright Chromium 首次安装提示。
- 更新 `docs/frontend/claude-web-v1-acceptance-fix-task-package.md` 状态为“已完成修复并通过复验”，保留原任务包作为历史验收依据。
- 更新 `docs/handover/company-adapter-deployment-handoff-v0.1.md`，把基础运行变量、公司 Adapter 替换变量、真实模型与 render-worker 选型变量拆开，并增加总体验收清单入口和公司交接证据要求。
- 更新 `.gitignore`，忽略 `.playwright-mcp/`、Playwright 报告、测试结果目录和本地验收截图，避免临时产物污染 Git 状态。

## 当前态本地商用闭环走查结果

- Docker 基础设施检查通过：PostgreSQL、Redis、Temporal、MinIO、OpenSearch、Prometheus、Grafana 均为 running/healthy；Temporal UI running。
- `./gradlew test spotlessCheck :apps:music-api:bootJar` 成功。
- `cd prototypes/Claude-web-v1 && npm test && npm run typecheck && npm run build` 成功，8 个测试文件、28 个测试通过。
- `cd apps/render-worker && npm run build && npm test` 成功，4 个 Node smoke 测试通过。
- 同步 Mock API smoke 成功：`EXPECTED_DURATION_MS=1000 scripts/smoke/api-main-flow.sh`，作品 `029130c1-feea-4c46-9287-7277df733402`。
- 真实后端 UI smoke 成功：`npm run smoke:real-backend`，主流程作品 `dd4e9304-f0e6-424f-a0ff-3528ddef0fc6`，失败重试作品 `9e3e7bd9-3181-4578-a154-ab9dd77ba291`。
- local-process MP4 smoke 成功：`EXPECTED_DURATION_MS=1000 EXPECT_RENDER_WORKER=local-process scripts/smoke/api-main-flow.sh`，作品 `b16565b0-f93a-4d1d-8dfc-29f53f29f299`；脚本已检查本地对象文件和 `ffprobe`。
- 验证结束后 `8080`、`5274` 无残留监听，`git status --short` 为空。
- 仍未完成项已归类到 `docs/checklists/local-commercial-delivery-audit-2026-06-06.md`：真实 Suno/MiniMax、真实 DeepSeek、真实 Image 2、公司五类 Adapter 替换、正式 `apps/web` 承接策略。

## AI 多 Agent 创作编排设计结果

- 新增 `docs/specs/ai-multi-agent-creative-pipeline-v0.1.md`，作为后续真实模型接入的设计基线。
- 新增 `docs/specs/ai-agent-orchestration-engineering-design-v0.1.md`，作为工程实现基线，明确 Agent / Adapter / Workflow / Service 分层、核心流程、状态映射、审计、失败收口、配置开关、测试策略和阶段计划。
- 核心决策已固化：不采用自由聊天式多 Agent；采用确定性的 `SongProductionWorkflow` / Temporal 编排，加上专业 Agent Worker 与 Provider Adapter。
- Agent 边界已明确：`CreativeBriefAgent`、`LyricsAgent`、`LyricsEditAgent`、`MusicPromptAgent`、`QualityEvaluationAgent`、`CoverPromptAgent`、可选 `VideoPlanAgent`、`ModerationAgent`。
- Adapter 边界已明确：`MusicProviderAdapter`、`ImageProviderAdapter`、`ModerationAdapter`、`AudioImportAdapter`、对象存储、公司发布/分享/审核/权益系统。
- 状态与副作用边界已明确：Agent 不直接更新作品终态、不扣权益、不写发布包、不调用公司发布；Workflow / Service 负责状态、权益、幂等、发布包和交接。
- 真实模型接入顺序已建议：先 DeepSeek 需求理解与写词，再音乐提示词和 Suno / MiniMax，再封面提示词与 Image 2，最后补质量评估、审核和公司 Adapter。
- Phase 1 已落六块代码：新增 `agent_runs` 与 `AgentRunRecorder`，接入写词链路的 `CreativeBriefAgent` / `LyricsAgent` Mock 审计；新增 `MusicPromptAgent` v0.1 Mock 合约并在确认出歌前接入 `SongProductionWorkflow`；新增 `ModerationAgent` v0.1 Mock 合约并在音乐 Provider 提交前做 Prompt 预检；新增 `CoverPromptAgent` v0.1 Mock 合约并在封面生成前规划 visual prompt；新增 `QualityEvaluationAgent` v0.1 Mock 合约并在发布包写入前执行质量门。

## OpenAPI 契约对拍 Smoke 结果

- 新增 `docs/specs/openapi-contract-smoke-v0.1.md`，明确 contract smoke 的功能要求、非功能要求、验收标准、边界情况和不覆盖范围。
- 新增 `scripts/smoke/openapi-contract.sh`，要求 API 已启动，默认使用 `API_BASE_URL=http://localhost:8080/api/v1` 和独立 `X-Mock-User-Id`。
- 静态检查覆盖 OpenAPI path、operationId、关键 schema required 字段和枚举值。
- 运行时检查覆盖 `/me`、灵感成歌、填词成歌、作品详情、作品列表、润色、续写、第三次编辑 409、确认出歌、封面重生、视频重渲、发布包获取、刷新 URL、标记交接、缺失幂等键、幂等冲突、作品不存在、`suno` 受控失败和 mock 重试恢复。
- 验证命令已通过：`scripts/smoke/openapi-contract.sh`。
- 验证环境：`MUSIC_PROVIDER=mock MOCK_MUSIC_DURATION_MS=1000 MUSIC_WORKFLOW_DISPATCH_MODE=sync WORKFLOW_OUTBOX_DISPATCHER_ENABLED=false RENDER_WORKER_MODE=mock ./gradlew :apps:music-api:bootRun`。
- 验证作品：成功主链路 `1ed4a2dd-8cb7-4bb1-849f-fd188df8fea8`；受控失败/重试链路 `f65f1ead-f4e8-4e3b-85b0-dcd88d8d8aa0`。
- 验证结束后已停止 API，`8080` 无监听进程。

## Agent Runtime 审计基础结果

- 新增 `docs/specs/agent-runtime-audit-v0.1.md`，明确 Agent run 审计的功能要求、非功能要求、验收标准、边界情况和不覆盖范围。
- 新增 `modules:agent-runtime`，提供 `AgentRunRecord`、`AgentRunRecorder`、`AgentRunStatus`、`NoopAgentRunRecorder`、hash helper 和失败信息脱敏工具。
- 新增 Flyway migration `V202606061740__add_agent_runs.sql`，落库 `agent_runs`，记录 agent、operation、model、Prompt 模板版本、输入/输出 hash、status、latency、token/cost 预留和失败信息。
- 新增 `JdbcAgentRunRecorder`，API 侧通过 Spring bean 接入；`DefaultLyricsGenerationService` 每次调用 `DeepSeekLyricsClient.generate` 都记录一条 `LyricsAgent` run。
- 低质量内部重写会记录两条 Agent run；DeepSeek client 异常会记录 `FAILED / DEEPSEEK_LYRICS_FAILED` 并脱敏 token/key 字段。
- 本批仍不调用真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统。
- Targeted 验证通过：`./gradlew :modules:agent-runtime:test :modules:lyrics:test :apps:music-api:compileJava :apps:music-worker:compileJava`。
- 格式验证通过：`./gradlew spotlessCheck`。
- 构建验证通过：`./gradlew :apps:music-api:bootJar :apps:music-worker:bootJar`。
- API smoke 成功：Flyway 应用 `agent_runs` migration；`POST /api/v1/works/inspiration` 创建作品 `1ac8eab6-91d1-42af-9104-c3979842eca5` 后，数据库 `agent_runs` 记录 `LyricsAgent|v0.1|INSPIRATION|mock-deepseek-lyrics|SUCCEEDED|true|true`。
- 验证结束后已停止 API，`8080` 无监听进程。

## MusicPromptAgent Mock 合约结果

- 新增 `docs/specs/music-prompt-agent-v0.1.md`，明确确认出歌后、调用 Suno / MiniMax / MockMusicProvider 前的音乐提示词 Agent 边界。
- 新增 `modules:creative-agent`，提供 `MusicPromptAgent`、`MusicPromptRequest`、`MusicPromptResult` 和 `MockMusicPromptAgent`。
- `MockSongProductionWorkflow` 已在 `MusicProvider.submit` 前调用 `MusicPromptAgent`，Provider 收到的是 Agent 输出的 music prompt 和 provider options，而不是直接透传歌词草案里的 seed。
- `MockMusicPromptAgent` 通过 `AgentRunRecorder` 写入 `MusicPromptAgent / v0.1 / MUSIC_PROMPT / mock-music-prompt` 审计记录；只保存输入/输出 hash 和元数据，不保存完整歌词、Prompt、密钥或供应商 payload。
- Agent 失败时，Workflow 会收口为 `MUSIC_GENERATION_FAILED`，释放已锁权益，Provider 不会被调用。
- Targeted 验证通过：`./gradlew :modules:creative-agent:test :apps:music-api:test --tests com.yanyun.music.production.MockSongProductionWorkflowTest :apps:music-api:compileJava :apps:music-worker:compileJava`。
- 格式与构建验证通过：`./gradlew spotlessCheck :apps:music-api:bootJar :apps:music-worker:bootJar`。
- API smoke 成功：`POST /api/v1/works/lyrics` 创建作品 `eec23007-95f0-4054-a75f-5cd372993d8f`，确认出歌后数据库 `agent_runs` 记录 `MusicPromptAgent|v0.1|MUSIC_PROMPT|mock-music-prompt|SUCCEEDED|true|true`。
- 验证结束后已停止 API，`8080` 无监听进程。

## CreativeBriefAgent / ModerationAgent / CoverPromptAgent / QualityEvaluationAgent Mock 合约结果

- 新增 `docs/specs/creative-brief-agent-v0.1.md`，明确灵感成歌、填词成歌、润色和续写进入写词模型前的用户需求理解边界。
- `modules:creative-agent` 新增 `CreativeBriefAgent`、`CreativeBriefRequest`、`CreativeBriefResult` 和 `MockCreativeBriefAgent`。
- `DefaultLyricsGenerationService` 已在知识库检索后、Prompt 渲染和 `DeepSeekLyricsClient.generate` 前调用 `CreativeBriefAgent`。
- `CreativeBriefAgent` 输出的用户意图、主题、情绪标签、叙事视角、音乐方向、燕云引用和约束会注入写词 Prompt 上下文；用户侧 OpenAPI 响应结构不变。
- `MockCreativeBriefAgent` 通过 `AgentRunRecorder` 写入 `CreativeBriefAgent / v0.1 / <LyricsOperation> / mock-creative-brief` 审计记录；只保存输入/输出 hash 和元数据，不保存完整歌词、Prompt、密钥或供应商 payload。
- Agent 失败时，写词链路会在 DeepSeek 前停止，记录 `FAILED / CREATIVE_BRIEF_AGENT_FAILED`，并沿原异常路径收口。
- Targeted 验证通过：`./gradlew :modules:creative-agent:test :modules:lyrics:test :apps:music-api:compileJava`。
- 格式与构建验证通过：`./gradlew :modules:creative-agent:test :modules:lyrics:test :apps:music-api:compileJava spotlessCheck :apps:music-api:bootJar :apps:music-worker:bootJar`。
- API smoke 成功：`POST /api/v1/works/inspiration` 创建作品 `667bb236-e453-423e-b781-bc6b81ec26fe` 并确认出歌后，数据库 `agent_runs` 记录 `CreativeBriefAgent`、`LyricsAgent`、`MusicPromptAgent` 三条成功审计，作品进入 `GENERATED / PACKAGE_READY`。
- 验证结束后已停止 API，`8080` 无监听进程。

- 新增 `docs/specs/cover-prompt-agent-v0.1.md`，明确确认出歌后、调用 Image 2 / MockCoverGenerationService 前的封面视觉提示词规划边界。
- `modules:creative-agent` 新增 `CoverPromptAgent`、`CoverPromptRequest`、`CoverPromptResult` 和 `MockCoverPromptAgent`。
- `SongProductionWorkflowInput` 新增内部字段 `coverPromptSeed`，并保留旧构造器兼容旧测试和 outbox payload。
- `MockSongProductionWorkflow` 已在 `CoverGenerationService.generateCover` 前调用 `CoverPromptAgent`，封面服务收到的是 Agent 输出的 visual prompt、negative prompt、尺寸和 provider options。
- `MockCoverGenerationService` 已把 `CoverPromptAgent` 输出写入 `COVER` 媒体资产 metadata，便于后续 Image 2 替换和 smoke 查询。
- `MockCoverPromptAgent` 通过 `AgentRunRecorder` 写入 `CoverPromptAgent / v0.1 / COVER_PROMPT / mock-cover-prompt` 审计记录；只保存输入/输出 hash 和元数据，不保存完整歌词、Prompt、密钥或供应商 payload。
- Agent 失败时，Workflow 会在调用封面生成服务前停止，按当前媒体生成失败路径收口为 `PACKAGE_BUILD_FAILED`，释放已锁权益。
- Targeted 验证通过：`./gradlew :modules:creative-agent:test :modules:image2:test :apps:music-api:test --tests com.yanyun.music.production.MockSongProductionWorkflowTest :apps:music-api:compileJava :apps:music-worker:compileJava :modules:workflow:test`。
- 格式与构建验证通过：`./gradlew :modules:creative-agent:test :modules:image2:test :apps:music-api:test --tests com.yanyun.music.production.MockSongProductionWorkflowTest :apps:music-api:compileJava :apps:music-worker:compileJava :modules:workflow:test spotlessCheck :apps:music-api:bootJar :apps:music-worker:bootJar`。
- 全量测试通过：`./gradlew test`。
- API smoke 成功：`POST /api/v1/works/inspiration` 创建作品 `6ccd242f-4d24-4036-8644-c4fc842812b6` 并确认出歌后，数据库 `agent_runs` 记录 `CreativeBriefAgent`、`LyricsAgent`、`MusicPromptAgent`、`CoverPromptAgent` 四条成功审计；封面 metadata 包含 `CoverPromptAgent` 和 `visual_prompt`。
- 验证结束后已停止 API，`8080` 无监听进程。

- 新增 `docs/specs/quality-evaluation-agent-v0.1.md`，明确发布包写入前的质量评估 Agent 边界。
- `modules:creative-agent` 新增 `QualityEvaluationAgent`、`QualityEvaluationRequest`、`QualityEvaluationResult`、`QualityGate`、`QualityDecision` 和 `MockQualityEvaluationAgent`。
- `MockSongProductionWorkflow` 已在发布包准备、发布包审核和发布包写入前调用 `QualityEvaluationAgent`，检查音频、封面、视频和 timeline 基础可用性。
- `MockQualityEvaluationAgent` 通过 `AgentRunRecorder` 写入 `QualityEvaluationAgent / v0.1 / PACKAGE_QUALITY_GATE / mock-quality-evaluation` 审计记录；只保存输入/输出 hash 和元数据，不保存完整歌词、Prompt、签名 URL、密钥或供应商 payload。
- 质量门返回非 `PASS` 或抛异常时，Workflow 会按 `PACKAGE_BUILD_FAILED` 收口，释放已锁权益，不进入发布包准备、审核、对象存储写入或 `PACKAGE_READY`。
- Targeted 验证通过：`./gradlew :modules:creative-agent:test :apps:music-api:test --tests com.yanyun.music.production.MockSongProductionWorkflowTest :apps:music-api:compileJava :apps:music-worker:compileJava`。
- 格式、构建和全量测试通过：`./gradlew spotlessCheck :apps:music-api:bootJar :apps:music-worker:bootJar test`。
- API smoke 成功：`POST /api/v1/works/inspiration` 创建作品 `9c692850-da4f-4ffb-aa09-aeb89b2645c9` 并确认出歌后，数据库 `agent_runs` 记录 `CreativeBriefAgent`、`LyricsAgent`、`MusicPromptAgent`、`CoverPromptAgent`、`QualityEvaluationAgent` 五类成功审计；作品进入 `GENERATED / PACKAGE_READY`。
- 验证结束后已停止 API，`8080` 无监听进程。

- 新增 `docs/specs/moderation-agent-v0.1.md`，明确音乐 Provider 调用前的音乐 Prompt AI 预检边界。
- `modules:creative-agent` 新增 `ModerationAgent`、`ModerationAgentRequest`、`ModerationAgentResult`、`ModerationTarget`、`ModerationAgentDecision` 和 `MockModerationAgent`。
- `MockSongProductionWorkflow` 已在 `MusicPromptAgent` 输出 music prompt 后、`MusicProvider.submit` 前调用 `ModerationAgent`。
- `MockModerationAgent` 通过 `AgentRunRecorder` 写入 `ModerationAgent / v0.1 / MUSIC_PROMPT_PRECHECK / mock-moderation-agent` 审计记录；只保存输入/输出 hash 和元数据，不保存完整歌词、Prompt、密钥或供应商 payload。
- Mock 预检命中 `[BLOCK]` 时，Workflow 会在音乐 Provider 调用前停止，按 `MUSIC_GENERATION_FAILED` 收口，释放已锁权益，并不给用户开放音乐重试；仍可返回编辑。
- Targeted 验证通过：`./gradlew :modules:creative-agent:test :apps:music-api:test --tests com.yanyun.music.production.MockSongProductionWorkflowTest :apps:music-api:compileJava :apps:music-worker:compileJava`。
- 格式、构建和全量测试通过：`./gradlew spotlessCheck :apps:music-api:bootJar :apps:music-worker:bootJar test`。
- API smoke 成功：`POST /api/v1/works/inspiration` 创建作品 `e736c1b7-fee8-4f5f-973f-6bb1c0eef384` 并确认出歌后，数据库 `agent_runs` 记录 `CreativeBriefAgent`、`LyricsAgent`、`MusicPromptAgent`、`ModerationAgent`、`CoverPromptAgent`、`QualityEvaluationAgent` 六类成功审计；作品进入 `GENERATED / PACKAGE_READY`。
- 验证结束后已停止 API，`8080` 无监听进程。

## 待确认事项

- `prototypes/Claude-web-v1` 已完成当前验收修复、真实后端模式 UI smoke 和脚本化复验；后续需决定是否把该原型合并/迁移到正式 `apps/web`，还是继续作为独立原型交给外部前端实现者迭代。
- 公司账号、审核、权益、发布、分享系统真实协议仍待公司开发确认；当前已提供 readiness 报告和交接说明，但真实 Adapter 仍需公司开发替换 Mock。
- Suno 和 MiniMax 的 DreamMaker run/status 接入骨架、JWT 鉴权、真实调用硬开关和受控联调文档已实现；Suno 首次真实 DreamMaker stack smoke 已触达创建任务阶段但返回 HTTP 403，需确认 AK/SK 权限、`X-Access-Token` 要求和 `app/sub_app/model` 配置；当前非内网公网 Suno 已补 Yunwu 后端和脚本化 smoke，但尚未执行真实 Yunwu 成功样本；MiniMax 尚未真实调用。非零错误码样本、失败任务响应样本、限流/轮询策略、音频 URL 过期规则和计费口径仍待真实联调确认。
- DeepSeek 真实客户端已实现并默认关闭；真实 API Key、失败码、限流策略、计费口径、内容安全要求和联调窗口仍待确认。
- Image 2 DreamMaker 生产目标客户端与 WellAPI 当前公网客户端均已实现并默认关闭；WellAPI Image2 与 DreamMaker Image2 均已补一键 stack smoke 和低层单作品 smoke，但尚未执行真实图片调用。真实图片 URL TTL、内容安全返回、默认封面兜底策略、失败码、计费口径和联调窗口仍待确认。
- Suno/MiniMax 真实音乐已补充 DreamMaker 一键 stack smoke，Yunwu Suno 已补当前公网一键 stack smoke；当前仍需用户明确执行真实调用窗口后再产生真实 Provider 成功样本。
- AI 多 Agent v0.1 已确定方向，`agent_runs` 表、写词链路 `CreativeBriefAgent` / `LyricsAgent` 审计、`MusicPromptAgent`、`ModerationAgent`、`CoverPromptAgent` 和 `QualityEvaluationAgent` 发布包质量门 Mock 合约已落地；后续可按真实模型联调需要扩展输入/歌词/封面/发布包更细预检点，歌词/音乐更细粒度质量门可在 `QualityEvaluationAgent` v0.2 扩展。
- Outbox v0.1 与 Temporal v0.1 已落地并可本地验证；真实模型链路前的 activity 细化设计、step audit 表、记录型 Mock step activity、受控分步 workflow 验证路径、worker 显式开关和本地 stepwise smoke 已补齐。`stepwise-recording` 当前只验证 Temporal step audit，不产出发布包；主链路验收仍使用 legacy 或同步 Mock 模式。
- 当前发布包 JSON 已可写入本地文件存储和 MinIO/S3 兼容对象存储；`RENDER_WORKER_MODE=local-process` 可把真实 MP4 和 timeline 写入对象存储，但默认仍是 Mock asset 描述和 URL 口径，长视频和生产级 render worker 方案尚未最终确认。
- 当前音乐重试已有次数上限和状态抢占，DreamMaker 失败码也已有保守映射；真实联调后仍需根据具体非零 code 和 failed payload 精细化 retryable / non-retryable 规则。
- 运营侧模型降级策略仍待定义：例如 Suno 连续失败是否自动推荐 MiniMax，哪些模型对用户可见，哪些仅作为后台兜底。

## 下一步建议

1. 决定 `prototypes/Claude-web-v1` 的工程去向：继续独立原型、并入正式 `apps/web`，或作为外部前端实现者的参考版本。
2. 若用户要测真实 AI 出歌，先用 `TARGET=yunwu-suno MODE=plan scripts/smoke/real-model-controlled-smoke.sh` 和 `TARGET=yunwu-suno MODE=preflight scripts/smoke/real-model-controlled-smoke.sh` 确认路线与环境；当前公网再用双重开关执行 Yunwu 单作品 Suno stack smoke。进入公司内网或生产目标路径时，改用 `TARGET=dreammaker-suno MODE=plan/preflight` 后验证 DreamMaker。不得在默认 `sync` 模式或无成本止损时运行真实音乐。
3. DeepSeek 真实写词联调先用 `TARGET=deepseek MODE=plan scripts/smoke/real-model-controlled-smoke.sh` 和 `TARGET=deepseek MODE=preflight scripts/smoke/real-model-controlled-smoke.sh` 确认双开关与密钥注入；随后按 `docs/runbook/deepseek-controlled-real-integration.md` 先跑 1 条灵感成歌样本。
4. Image2 真实封面联调当前公网先用 `TARGET=wellapi-image2 MODE=plan/preflight scripts/smoke/real-model-controlled-smoke.sh` 确认路线，保持 `MUSIC_PROVIDER=mock` 后再用双重开关执行 WellAPI 单作品封面 smoke；生产目标 DreamMaker Image2 路径用 `TARGET=dreammaker-image2 MODE=plan/preflight scripts/smoke/real-model-controlled-smoke.sh` 确认路线，再用 `ALLOW_REAL_MODEL_SMOKE=1 ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE=1 TARGET=dreammaker-image2 MODE=execute scripts/smoke/real-model-controlled-smoke.sh` 验证。
5. 按 `docs/specs/temporal-activity-decomposition-v0.1.md` 继续推进分步 activity：下一步让分步 activity 逐步接入真实音乐、封面、视频和发布包状态推进；`stepwise-recording` 的 outbox + step audit 验收已可用专用脚本复验。
6. 按 `docs/checklists/local-commercial-delivery-audit-2026-06-06.md` 的未完成项逐项推进或交给公司确认。
7. 公司开发确认真实账号、审核、权益、发布、分享协议后，按 `docs/handover/company-adapter-deployment-handoff-v0.1.md` 替换 Mock Adapter，并用 `/internal/integration-readiness` 做部署前检查。
8. 根据真实联调样本更新 `docs/integrations/dreammaker-open-questions-tracker.md` 和失败码 retryable 规则。

## 工作日志

| 时间 | 事项 | 结果 |
|---|---|---|
| 2026-06-05 00:43 CST | Git 初始化 | 已创建本地 Git 仓库，分支为 `main` |
| 2026-06-05 00:43 CST | 创建项目进度文档 | 新增 `docs/project-progress.md` |
| 2026-06-05 00:43 CST | 固化进度记录规则 | 新增项目级 `AGENTS.md`，并在本文件加入阶段性任务完成后必须更新进度的规则 |
| 2026-06-05 00:48 CST | 整理第 1 批仓库初始化任务说明 | 新增 `docs/codex-batch-01-repository-initialization.md`，明确第 1 批范围、目录、禁区、验收标准和执行顺序 |
| 2026-06-05 00:48 CST | 整理技术方案需补项 | 新增 `docs/tech-design-required-supplements.md`，列出前端工程、OpenAPI、权益扣减、审核落点、并发控制等需补齐事项 |
| 2026-06-05 00:53 CST | 固化 commit 提醒规则 | 更新 `AGENTS.md`，要求阶段性任务完成且适合形成快照时提醒用户是否 commit |
| 2026-06-05 00:53 CST | 执行第一次 commit | 将项目基线文档、进度记录规则、第 1 批任务说明和技术方案补项纳入 Git 初始提交 |
| 2026-06-05 01:19 CST | 输出 PRD v0.3 | 新增 `yanyun-ai-music-platform-prd-v0.3.md`，明确商用级目标、多端用户侧、外部系统边界、用户体验和量化验收 |
| 2026-06-05 01:19 CST | 输出技术方案 v0.2 | 新增 `yanyun-ai-music-platform-tech-design-v0.2.md`，明确完整商用级技术栈、`apps/web`、权益扣减、发布包审核、并发控制和 OpenAPI 覆盖范围 |
| 2026-06-05 01:19 CST | 固化架构决策 | 新增 `docs/adr/0001-user-web-scope.md` 和 `docs/adr/0002-commercial-grade-stack.md` |
| 2026-06-05 01:19 CST | 更新执行辅助文档 | 更新 `docs/tech-design-required-supplements.md`、`docs/codex-batch-01-repository-initialization.md` 和 `AGENTS.md`，同步新基线和第 1 批要求 |
| 2026-06-05 01:26 CST | 升级项目 Agent 规则 | 重写 `AGENTS.md`，固化项目身份、Source of Truth、固定技术决策、外部系统边界、前后端执行规则、验证规则、进度记录、Git 和安全要求 |
| 2026-06-05 01:28 CST | 补充前端协作和图片资产规则 | 更新 `AGENTS.md`，明确图片资产优先用 Image 生成，前端视觉实现优先产出 Gemini 任务包，本 Agent 负责需求、接口、状态、验收和 review |
| 2026-06-05 01:30 CST | 提交文档基线 | 用户要求执行 commit；提交前已检查 Git 状态、敏感信息和 Markdown diff 格式，本轮文档基线已纳入 Git 快照 |
| 2026-06-05 02:39 CST | 输出 OpenAPI v0.1 | 新增 `docs/api/openapi-v0.1.yaml`，覆盖作品状态、生成阶段、权益提示、失败动作和发布包交接 |
| 2026-06-05 02:39 CST | 初始化商用级工程骨架 | 新增 Gradle 多模块后端、`apps/web`、`apps/render-worker`、数据库/知识库/运行手册目录 |
| 2026-06-05 02:39 CST | 搭建本地基础设施 | 新增 Docker Compose，PostgreSQL、Redis、Temporal、MinIO、OpenSearch、Prometheus、Grafana 已通过本地启动验证 |
| 2026-06-05 02:39 CST | 完成基础构建和 smoke 验证 | Gradle、Web、Render Worker、Docker Compose、API health、Worker Temporal 连接验证均通过 |
| 2026-06-05 02:43 CST | 提交第 1 批工程快照 | 已提交 `992762a chore: initialize commercial-grade project scaffold` |
| 2026-06-05 03:20 CST | 落地第 2 批数据库 migration | 新增作品域、歌词草案、任务、媒体、发布包、权益、Provider 调用、知识库、Prompt 和配置等核心表 |
| 2026-06-05 03:20 CST | 实现 Work 状态机与 Mock Adapter 边界 | 账号、权益、审核、发布包交接均可通过 Mock 实现跑通，真实公司系统后续替换 Adapter |
| 2026-06-05 03:20 CST | 实现 OpenAPI 主路径 Mock API | 灵感成歌、填词成歌、作品查询、改词、确认出歌、重生/重渲、发布包获取/刷新/交接接口已可运行 |
| 2026-06-05 03:20 CST | 完成本地后端 Mock 业务 smoke | 作品从 `LYRICS_READY` 推进到 `GENERATED/PACKAGE_READY`，发布包标记后进入 `PACKAGE_FETCHED` |
| 2026-06-05 03:20 CST | 输出 Gemini 前端任务包 | 新增 `docs/frontend/gemini-batch-02-mock-workflow-task-package.md`，供后续前端实现使用 |
| 2026-06-05 03:20 CST | 记录 Suno + MiniMax 双模型要求 | 音乐生成后续需同时接入 Suno 与 MiniMax，并通过配置/运营策略选择对用户开放的模型；飞书资料待可读后补细节 |
| 2026-06-05 03:34 CST | 补齐基础幂等语义 | `Idempotency-Key` 成功响应可重放，同 key 不同请求返回 `IDEMPOTENCY_CONFLICT`，已通过单元测试和 HTTP smoke |
| 2026-06-05 03:43 CST | 预置音乐 Provider 工程边界 | 新增统一 `MusicProvider` 合约、`MockMusicProvider`、`SunoMusicProvider`、`MiniMaxMusicProvider` 和配置变量，真实 API 调用仍保持关闭 |
| 2026-06-05 03:50 CST | 接入 MockMusicProvider 到出歌流程 | `confirmWork` 已通过 Provider 结果写入音频媒体资产，主链路 smoke 通过 |
| 2026-06-05 04:19 CST | 拆出 MockSongProductionWorkflow | 出歌编排从 `WorkService` 下沉到 Workflow，补齐 job 成功/失败收口和失败释放权益，单元测试、构建、HTTP smoke、DB 抽查均通过 |
| 2026-06-05 04:29 CST | 接入 Mock 对象存储写入 | 新增 `ObjectStorageClient` 与本地文件实现，发布包 JSON 已可写入 `build/local-object-storage`，单元测试、构建、HTTP smoke、本地文件检查和 DB 抽查均通过 |
| 2026-06-05 04:50 CST | 补 Provider 配置选择 | `MUSIC_PROVIDER=mock|suno|minimax` 已接入 Workflow；默认 mock 成功，suno 未实现边界会持久化失败并释放权益，测试、构建、HTTP smoke 和 DB 抽查均通过 |
| 2026-06-05 05:12 CST | 补失败恢复与重试闭环 | 新增音乐重试接口与请求级 Provider 覆盖；`suno` 失败后可用 `mock` 重试恢复到 `PACKAGE_READY`，测试、构建、HTTP smoke 和 DB 抽查均通过 |
| 2026-06-05 10:07 CST | 增强重试稳定性 | 新增 `music_retry_count`、2 次音乐重试上限、状态抢占和失败推荐动作；次数耗尽、恢复成功、Flyway、HTTP smoke 和 DB 抽查均通过 |
| 2026-06-05 10:22 CST | 启用 Provider 调用记录 | 出歌流程已写入 `provider_calls`，Suno 失败与 Mock 成功均可追踪；新增 Suno/MiniMax 真实接入前置说明 |
| 2026-06-05 22:18 CST | 读取飞书模型接入资料 | 已通过 `lark-cli` 授权读取文档，整理 DreamMaker Suno / MiniMax run/status 接口、字段限制和剩余待确认项 |
| 2026-06-05 22:52 CST | 实现 DreamMaker Provider 接入骨架 | 新增 `modules:dreammaker`、Suno/MiniMax submit+poll、DreamMaker HTTP client、远程音频导入对象存储和规格/运行文档；测试、bootJar、OpenAPI 解析和本地 API smoke 均通过 |
| 2026-06-05 23:55 CST | 实现 Outbox 可靠编排基础 | 新增 `workflow_outbox`、可切换 sync/outbox 模式、本地 dispatcher、确认出歌/音乐重试异步启动边界和规格/运行文档；测试、bootJar、Flyway、sync smoke、outbox smoke 和 DB 抽查均通过 |
| 2026-06-06 00:12 CST | 补齐 DreamMaker JWT 鉴权 | 根据用户补充资料确认 AK/SK 生成 HS256 JWT；`DreamMakerHttpClient` 已改为 `Authorization: Bearer <jwt>`，可选透传 `X-Access-Token`；测试、bootJar、OpenAPI 解析和密钥扫描均通过 |
| 2026-06-06 01:35 CST | 完成 Temporal 真实编排基础 | 新增 `modules:production`、`modules:workflow`、API local/temporal starter、worker workflow/activity 注册和生命周期；local outbox smoke、Temporal worker smoke、全量 Gradle 验证和 DB 抽查均通过 |
| 2026-06-06 02:20 CST | 完成 DreamMaker 受控真实联调准备 | 新增真实调用硬开关、共享 DreamMaker client、worker Suno/MiniMax 注册、provider 模型标识、失败信息脱敏和第 5 批联调/安全/验收/交接文档；全量测试、bootJar 和硬开关 HTTP smoke 均通过 |
| 2026-06-06 03:01 CST | 完成 DeepSeek / 知识库写词润色 Mock 链路 | 新增 Knowledge/Prompt/DeepSeek/Lyrics 模块、统一写词服务、低质量内部重写、AI 编辑次数共享、持久化写词元数据和事务 4xx 修复；全量测试、bootJar、HTTP smoke 和 DB 抽查均通过 |
| 2026-06-06 03:44 CST | 完成封面与 MP4 成片基础 | 新增 Image2/Media 边界、Cover/Video Mock 服务、发布包媒体引用更新和 `LyricVideo16x9` Remotion 样例；全量 Gradle、render-worker build/test/render、ffprobe、HTTP smoke 和 DB 抽查均通过 |
| 2026-06-06 04:36 CST | 完成 MinIO/S3 发布包强化 | 新增 S3/MinIO 对象存储客户端、结构化发布包 object key、presigned URL 签发/刷新和对象存储运行手册；全量 Gradle、MinIO smoke、local smoke 和 8080 清理检查均通过 |
| 2026-06-06 04:40 CST | Gemini 前端原型废弃 | 子 Agent 调用 Gemini 生成的 `prototypes/gemini-web-v1` 因视觉质量不达标已删除；后续第 9 批需重新整理更强约束的高保真前端任务包 |
| 2026-06-06 12:49 CST | 完成公司 Adapter 接入与部署交接准备 | 新增公司 Adapter readiness 服务、内部 `/internal/integration-readiness`、第 10 批规格和公司交接说明；全量 Gradle、JAR HTTP smoke、敏感信息 smoke 和 8080 清理检查均通过 |
| 2026-06-06 13:32 CST | 完成 render-worker 本地进程调用边界 | 新增 `render:job` CLI、动态时长 Remotion composition、Java `LocalProcessVideoRenderService`、render-worker readiness 组件和运行手册；Node build/test、1 秒真实 render smoke、targeted Gradle 测试均通过 |
| 2026-06-06 14:15 CST | 新建前端交付后的长期 Goal | Goal 已改为前端原型验收修复、后端短链路联调支撑、前后端本地 smoke、进度文档和阶段快照 |
| 2026-06-06 14:15 CST | 完成 Claude Web v1 前端初审 | `prototypes/Claude-web-v1` 已通过测试、typecheck、build 和 390px / 1440px smoke；发现 OpenAPI/验收缺口并整理为修复任务包 |
| 2026-06-06 14:15 CST | 补齐 Mock 音频时长配置 | 新增 `MOCK_MUSIC_DURATION_MS` 和 `MockMusicProvider` 配置测试，后端 targeted Gradle 验证通过 |
| 2026-06-06 14:31 CST | 完成同步 Mock 后端主链路 smoke | API 以 1 秒 Mock 音频时长跑通创建作品、确认出歌、发布包获取、刷新、标记交接、readiness、数据库和本地发布包 JSON 抽查 |
| 2026-06-06 14:45 CST | 完成 render-worker local-process API smoke | 修复相对工作目录解析后，默认 `apps/render-worker` 可在 `bootRun` 下生成真实 1 秒 MP4；ffprobe 验证 H.264、1920x1080、30fps |
| 2026-06-06 14:55 CST | 脚本化本地主链路 smoke | 新增 `scripts/smoke/api-main-flow.sh`，同步 Mock 与 render-worker local-process 两种 API 启动模式均验证通过 |
| 2026-06-06 15:38 CST | 完成 Claude Web v1 前端验收修复 | 本 Agent 按用户要求直接修复 `prototypes/Claude-web-v1`，补齐列表、发布交接、状态派生、失败动作、错误编号和可访问性；前端 test/typecheck/build 与 390px/1440px smoke 通过 |
| 2026-06-06 16:23 CST | 完成真实后端 UI 联调 smoke | `prototypes/Claude-web-v1` 不加 `?mock=1` 跑通灵感成歌、润色/续写、409 友好提示、确认出歌、成品交接、作品列表和失败页重试恢复；同时修复移动端按钮重叠和弹窗错误持久展示 |
| 2026-06-06 16:44 CST | 脚本化真实后端 UI smoke | 新增 `npm run smoke:real-backend`，Playwright 脚本可复验主成功链路和失败重试链路；修复标记交接后的按钮收口与已交接文案 |
| 2026-06-06 16:53 CST | 补齐本地商用闭环交付清单 | 新增总体验收 checklist，修正 README/前端任务包过时状态，拆分公司部署变量口径，并忽略本地 Playwright 缓存和验收截图 |
| 2026-06-06 17:14 CST | 完成当前态本地商用闭环走查 | Gradle、Claude Web v1、render-worker、同步 Mock API smoke、真实后端 UI smoke、local-process MP4 smoke 均通过；新增走查记录并归类剩余交付项 |
| 2026-06-06 17:25 CST | 补充 AI 多 Agent 创作编排设计 | 新增真实模型阶段设计文档，明确确定性 Workflow + 专业 Agent Worker + Provider Adapter 的后续方向，并更新项目进度 |
| 2026-06-06 17:37 CST | 补齐 OpenAPI 契约对拍 smoke | 新增 contract smoke 规格和脚本，覆盖静态 OpenAPI 与运行时主响应、错误、幂等、发布包、受控失败和重试恢复；同步 Mock API 下验证通过 |
| 2026-06-06 17:57 CST | 落地 Agent Runtime 审计基础 | 新增 `modules:agent-runtime`、`agent_runs` migration、JDBC recorder 和写词链路 `LyricsAgent` 审计；targeted tests、Spotless、bootJar、Flyway/API/DB smoke 均通过 |
| 2026-06-06 18:21 CST | 补充多 Agent 工程设计并落地 MusicPromptAgent | 新增工程编排设计基线；新增 `modules:creative-agent` 和 `MusicPromptAgent` v0.1 Mock 合约，确认出歌前生成音乐提示词并写入 `agent_runs`；targeted tests、Spotless、bootJar 和 API/DB smoke 均通过 |
| 2026-06-06 18:47 CST | 落地 CreativeBriefAgent Mock 合约 | 新增创作简报 Agent 规格和 Mock 实现，写词前生成结构化简报并注入 Prompt，上报 `agent_runs`；targeted tests、Spotless、bootJar 和 API/DB smoke 均通过 |
| 2026-06-06 19:21 CST | 落地 CoverPromptAgent Mock 合约 | 新增封面提示词 Agent 规格和 Mock 实现，封面生成前规划 visual prompt 并写入封面 metadata 和 `agent_runs`；targeted tests、Spotless、bootJar、全量测试和 API/DB smoke 均通过 |
| 2026-06-06 19:42 CST | 落地 QualityEvaluationAgent 发布包质量门 | 新增质量评估 Agent 规格和 Mock 实现，发布包准备/审核/写入前执行质量门并写入 `agent_runs`；targeted tests、Spotless、bootJar、全量测试和 API/DB smoke 均通过 |
| 2026-06-06 20:02 CST | 落地 ModerationAgent 音乐 Prompt 预检 | 新增 AI 预检 Agent 规格和 Mock 实现，音乐 Provider 调用前执行 Prompt 预检并写入 `agent_runs`；targeted tests、Spotless、bootJar、全量测试和 API/DB smoke 均通过 |
| 2026-06-06 20:24 CST | 补齐 DeepSeek 真实联调准备文档 | 新增 DeepSeek runbook、安全规则、验收清单、开放问题跟踪和 `deepseek_guard` readiness；targeted tests、Spotless、bootJar、diff check 和敏感信息扫描通过；当前仍不调用真实 DeepSeek |
| 2026-06-06 20:48 CST | 补齐 Image 2 真实联调准备文档 | 新增 Image 2 runbook、安全规则、验收清单、开放问题跟踪和 `image2_guard` readiness；targeted tests、Spotless 和 bootJar 通过；当前仍不调用真实 Image 2 |
| 2026-06-06 21:05 CST | 补充 Temporal activity 细化设计 | 新增 activity 拆分规格，定义真实模型阶段前的分步 activity、幂等键、失败收口、数据模型和实施顺序；当前未改业务代码 |
| 2026-06-06 21:18 CST | 落地 Temporal activity Phase 1 合约骨架 | 新增分步 activity 接口、上下文、步骤枚举和通用结果；workflow tests、API/worker compile、Spotless 和 bootJar 通过；当前不改变现有单 activity 执行路径 |
| 2026-06-06 21:32 CST | 新增 Temporal step audit 数据库基础 | 新增 `generation_job_steps` migration 和 `WorkRepository` 最小 API；production compile、API/worker bootJar、Spotless 通过；当前不接入主 workflow |
| 2026-06-06 21:45 CST | 新增记录型 Mock step activity | 新增 `RecordingSongProductionStepActivities` 和成功/失败记录测试；production test、API/worker compile、Spotless 和 bootJar 通过；当前不替换现有单 activity 执行路径 |
| 2026-06-06 22:00 CST | 新增受控分步 workflow 验证路径 | 新增 `StepwiseSongProductionWorkflow` 和顺序/失败/释放测试，并验证 recording activity 成功路径写出 13 条 step audit；workflow/production test、API/worker compile、Spotless 和 bootJar 通过；当前不注册到生产 worker |
| 2026-06-06 22:15 CST | 新增 worker stepwise 显式开关 | 新增 `TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy|stepwise-recording`；默认 legacy 不变，stepwise 才注册 recording activity；采纳子 Agent 审查改为懒获取 step activity；worker/workflow/production tests、API/worker compile、Spotless 和 bootJar 通过 |
| 2026-06-06 22:30 CST | 完成 stepwise-recording 本地 smoke | 修复 `TemporalWorkerProperties` 配置绑定回归并补 Binder 测试；本地 worker/API smoke 验证 outbox `SUCCEEDED`、13 条 step audit 写入、作品保持预期未完成边界；`8080/8081` 已清理 |
| 2026-06-06 22:45 CST | 脚本化 stepwise-recording smoke | 新增 `scripts/smoke/temporal-stepwise-recording.sh` 并更新 runbook；脚本化 smoke 通过，验证 outbox 成功、13 条 step audit 和预期未完成状态；`8080/8081` 已清理 |
| 2026-06-07 00:10 CST | 补齐 DeepSeek v4Pro 与 DreamMaker Image2 受控客户端 | 新增 OpenAI 兼容 `RealDeepSeekLyricsClient`、DreamMaker `gpt-image-2` 封面客户端、远程封面导入对象存储、DeepSeek/Image2 readiness 真实客户端口径、DreamMaker 非真实 guard smoke 和联调文档更新；targeted Gradle 测试、bootJar、diff check、敏感信息扫描和非真实 guard smoke 通过，当前仍未调用真实外部模型 |
| 2026-06-07 00:32 CST | 补齐 Suno/MiniMax 一键受控真实 smoke | 新增 `scripts/smoke/dreammaker-real-music-stack-smoke.sh` 和规格文档，默认 `REAL_PROVIDER=suno`，必须显式 `ALLOW_DREAMMAKER_REAL_SMOKE=1`，静默读取 AK/SK，自动启动/清理 worker 与 API，并复用单作品真实 Provider smoke；本轮只验证保护和脚本，不调用真实 DreamMaker |
| 2026-06-07 00:45 CST | 执行首次真实 Suno 单作品 smoke | Stack smoke 创建作品 `598a0380-0de5-4536-9f84-9c5481bca685`，确认出歌后 DreamMaker 创建任务阶段返回 HTTP 403；作品失败收口、provider_call 脱敏记录、重试动作、权益释放和进程清理均正常；需供应商/公司确认 Suno 权限、用户 token 要求和模型配置 |
