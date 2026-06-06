# 本地商用闭环交付走查记录

日期：2026-06-06 17:37 CST
最新更新：2026-06-07 00:10 CST
状态：本地 Mock / 前端 / MP4 成片链路已复验通过；用户已可用 `prototypes/Claude-web-v1 + music-api` 做本地完整链路实测。真实 AI 出歌尚未完成供应商真实调用 smoke，真实公司系统仍为外部接入项。

## 当前可测范围

| 范围 | 当前判定 | 说明 |
|---|---|---|
| 本地用户创作链路 | 可测 | 用 Claude Web v1 前端连接真实后端，走灵感成歌、填词成歌、润色/续写限制、确认出歌、生成中、成品页、发布交接和失败重试。 |
| 本地 MP4 成片链路 | 可测 | `RENDER_WORKER_MODE=local-process` 下已跑通 Java 调用 render-worker 并用 `ffprobe` 验证 MP4。 |
| 真实 Suno / MiniMax 音乐 | 准备好进入首次受控 smoke | DreamMaker Provider、JWT 鉴权、安全硬开关、runtime sync 保护、readiness AK/SK 配置检查、runbook、10 分钟 smoke 清单和脚本化单作品 smoke 已准备；尚未真正向 DreamMaker 发起真实生成请求。 |
| 真实 DeepSeek 写词 | 准备好进入首次受控 smoke | OpenAI 兼容 `RealDeepSeekLyricsClient`、双开关、runbook、验收清单和 readiness guard 已准备；尚未真正向 DeepSeek 发起真实请求。 |
| 真实 Image 2 封面 | 准备好进入首次受控 smoke | DreamMaker `gpt-image-2` 封面客户端、对象存储导入、runbook、验收清单和 readiness guard 已准备；尚未真正向 DreamMaker Image2 发起真实请求。 |
| 公司账号/审核/权益/发布/分享 | 外部接入项 | 本平台只提供 Adapter 边界、readiness 报告和交接清单，真实接入由公司开发替换。 |

## 交付阻塞矩阵

| 项 | 状态 | 责任方 | 是否可豁免 | 下一步 |
|---|---|---|---|---|
| DreamMaker / Suno 真实音乐 | 待首次受控 smoke | 本项目 + 用户提供联调窗口和凭据 | 不建议豁免；真实 AI 出歌必须验证 | 按 `docs/checklists/dreammaker-real-music-smoke-10min.md` 或 `scripts/smoke/dreammaker-real-music-smoke.sh` 单作品执行 |
| DreamMaker / MiniMax 真实音乐 | 待首次受控 smoke | 本项目 + 用户提供联调窗口和凭据 | 不建议豁免；若首发只开放一个 Provider，可记录产品豁免 | Suno 成功后再跑或反向先跑 MiniMax；同样使用受控 smoke 清单或脚本 |
| DeepSeek 真实写词 | 待首次受控 smoke | 本项目 + 用户安全注入 API Key 并确认联调窗口 | 可阶段性豁免，用 Mock 写词先测音乐 | 按 DeepSeek runbook 单样本联调 |
| Image 2 真实封面 | 待首次受控 smoke | 本项目 + 用户安全注入 DreamMaker AK/SK 并确认联调窗口 | 可阶段性豁免，用 Mock 封面先测音乐 | 按 Image 2 runbook 单样本联调 |
| render-worker 生产形态 | 待公司部署确认 | 公司开发 / 运维 | 不可长期豁免 | 确认本地进程、独立服务或队列化 worker |
| `prototypes/Claude-web-v1` 前端承接 | 待决策 | 用户 / 公司前端 / 本项目 | 不可长期豁免 | 决定保留原型、迁移到 `apps/web` 或公司重建 |
| 公司账号 Adapter | 外部接入 | 公司开发 | 生产不可豁免 | 替换 `MockAccountAdapter`，禁用 `X-Mock-User-Id` 信任 |
| 公司审核 Adapter | 外部接入 | 公司开发 | 生产不可豁免 | 覆盖输入、歌词、发布包交接前审核 |
| 公司权益 Adapter | 外部接入 | 公司开发 | 生产不可豁免 | 实现 `lock / commit / release` 幂等和对账 |
| 公司发布交接 | 外部接入 | 公司开发 | 生产不可豁免 | 验证公司系统可读取发布包 URL |
| 公司分享系统 | 公司系统承接 | 公司开发 / 社区团队 | 本平台实现可豁免 | 只需确认分享由公司系统承接，本平台不新增分享 Adapter |

## 本轮已复验证据

| 检查项 | 结果 | 证据摘要 |
|---|---|---|
| Git 工作区 | 通过 | `git status --short` 为空 |
| Docker 基础设施 | 通过 | PostgreSQL、Redis、Temporal、MinIO、OpenSearch、Prometheus、Grafana 均为 running/healthy；Temporal UI running |
| 后端测试与构建 | 通过 | `./gradlew test spotlessCheck :apps:music-api:bootJar` 成功 |
| 前端测试与构建 | 通过 | `prototypes/Claude-web-v1`：8 个测试文件、29 个测试通过；`typecheck` 和 `build` 成功 |
| render-worker 测试与构建 | 通过 | `npm run build && npm test` 成功；4 个 Node smoke 测试通过 |
| 同步 Mock API smoke | 通过 | `EXPECTED_DURATION_MS=1000 scripts/smoke/api-main-flow.sh` 成功，最近复验作品 `92703111-50d0-453e-a9b8-f247aa2c6914` |
| OpenAPI 契约 smoke | 通过 | `scripts/smoke/openapi-contract.sh` 成功，最近复验作品 `e1bc6b0b-340a-4899-9923-15b016147881`，受控失败/重试作品 `e86add7b-0b3b-4d5c-af6b-b05b8b66131a` |
| 真实后端 UI smoke | 通过 | `npm run smoke:real-backend` 成功；最近复验主流程作品 `8b2cdd23-f073-433b-b0cb-1fb8dab76736`，失败重试作品 `b8cfce76-de20-4a65-8f8e-e712648ab671` |
| local-process MP4 smoke | 通过 | `EXPECTED_DURATION_MS=1000 EXPECT_RENDER_WORKER=local-process scripts/smoke/api-main-flow.sh` 成功，作品 `b16565b0-f93a-4d1d-8dfc-29f53f29f299`；脚本已执行本地对象文件与 `ffprobe` 验证 |
| 公司 Adapter 交接资料 | 通过 | 已新增 `docs/checklists/company-adapter-replacement-readiness.md`，覆盖账号、审核、权益、发布、分享替换证据和禁止事项 |
| DreamMaker 真实联调保护 | 通过 | 已补 runtime guard：`DREAMMAKER_REAL_CALLS_ENABLED=true` 且 Provider 为 `suno` / `minimax` 时，确认/重试必须走 `outbox + temporal`；readiness 会在缺 AK/SK 时显示 `BLOCKED` |
| DreamMaker 脚本化 smoke | 待人工执行 | 新增 `scripts/smoke/dreammaker-real-music-smoke.sh`；脚本需要 `ALLOW_DREAMMAKER_REAL_SMOKE=1`，本轮未执行真实供应商调用 |
| 进程清理 | 通过 | 验证结束后 `8080`、`5274` 无监听进程 |

## 仍不能判定完成的项

| 项目 | 当前状态 | 原因 |
|---|---|---|
| Suno / MiniMax 真实成功路径 | 待首次受控 smoke | DreamMaker Provider 最小真实调用实现、硬开关、runtime guard、readiness AK/SK 检查、runbook、10 分钟 smoke 清单和脚本化 smoke 已准备；尚未用真实凭据发起供应商请求 |
| DeepSeek 真实写词 | 准备中 / 阻塞 | 受控联调 Runbook、安全规则、验收清单已补齐；真实客户端、API 协议、失败码、限流和计费口径仍待确认 |
| Image 2 真实封面 | 准备中 / 阻塞 | 受控联调 Runbook、安全规则、验收清单已补齐；真实客户端、API 协议、素材规范和对象存储规范仍待确认 |
| 公司账号 Adapter | 外部接入项 | 需要公司开发替换 `MockAccountAdapter`，生产不得信任 `X-Mock-User-Id` |
| 公司审核 Adapter | 外部接入项 | 需要公司审核协议覆盖输入、歌词、发布包交接前审核 |
| 公司权益 Adapter | 外部接入项 | 需要公司权益系统实现 `lock / commit / release` 幂等语义 |
| 公司发布/分享系统 | 外部接入项 | 本平台只交付发布包和交接状态，不实现社区发布、分享、互动、推荐流 |
| 正式前端承接策略 | 待决策 | 当前可验收前端在 `prototypes/Claude-web-v1`；`apps/web` 仍是 scaffold，是否迁移或重建需定 |

## 下一步建议

1. 若用户要测真实 AI 出歌，优先只开 DreamMaker 音乐一环：按 `docs/checklists/dreammaker-real-music-smoke-10min.md`，或在确认 worker/API 已按 outbox+Temporal 启动后用 `scripts/smoke/dreammaker-real-music-smoke.sh`，先跑 Suno 或 MiniMax 其中一个 Provider 的单作品受控 smoke。
2. 把 `prototypes/Claude-web-v1` 的去向定下来：保留为原型、迁移到 `apps/web`，或交给公司前端按它重做。
3. 公司开发进入前，按 `docs/handover/company-adapter-deployment-handoff-v0.1.md` 确认账号、审核、权益、发布、分享五类 Adapter 替换方案。
