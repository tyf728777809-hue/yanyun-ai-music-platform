# 本地商用闭环交付走查记录

日期：2026-06-06 17:37 CST
状态：本地 Mock / 前端 / MP4 成片链路已复验通过；真实模型与真实公司系统仍为交付前阻塞或外部接入项。

## 本轮已复验证据

| 检查项 | 结果 | 证据摘要 |
|---|---|---|
| Git 工作区 | 通过 | `git status --short` 为空 |
| Docker 基础设施 | 通过 | PostgreSQL、Redis、Temporal、MinIO、OpenSearch、Prometheus、Grafana 均为 running/healthy；Temporal UI running |
| 后端测试与构建 | 通过 | `./gradlew test spotlessCheck :apps:music-api:bootJar` 成功 |
| 前端测试与构建 | 通过 | `prototypes/Claude-web-v1`：8 个测试文件、28 个测试通过；`typecheck` 和 `build` 成功 |
| render-worker 测试与构建 | 通过 | `npm run build && npm test` 成功；4 个 Node smoke 测试通过 |
| 同步 Mock API smoke | 通过 | `EXPECTED_DURATION_MS=1000 scripts/smoke/api-main-flow.sh` 成功，作品 `029130c1-feea-4c46-9287-7277df733402` |
| OpenAPI 契约 smoke | 通过 | `scripts/smoke/openapi-contract.sh` 成功，作品 `1ed4a2dd-8cb7-4bb1-849f-fd188df8fea8`，受控失败/重试作品 `f65f1ead-f4e8-4e3b-85b0-dcd88d8d8aa0` |
| 真实后端 UI smoke | 通过 | `npm run smoke:real-backend` 成功；主流程作品 `dd4e9304-f0e6-424f-a0ff-3528ddef0fc6`，失败重试作品 `9e3e7bd9-3181-4578-a154-ab9dd77ba291` |
| local-process MP4 smoke | 通过 | `EXPECTED_DURATION_MS=1000 EXPECT_RENDER_WORKER=local-process scripts/smoke/api-main-flow.sh` 成功，作品 `b16565b0-f93a-4d1d-8dfc-29f53f29f299`；脚本已执行本地对象文件与 `ffprobe` 验证 |
| 进程清理 | 通过 | 验证结束后 `8080`、`5274` 无监听进程 |

## 仍不能判定完成的项

| 项目 | 当前状态 | 原因 |
|---|---|---|
| Suno / MiniMax 真实成功路径 | 阻塞 | 需要真实联调窗口、成本止损、真实调用授权和脱敏记录 |
| DeepSeek 真实写词 | 阻塞 | 真实 API 协议、失败码、限流和计费口径未确认 |
| Image 2 真实封面 | 阻塞 | 真实 API、素材规范和对象存储规范未确认 |
| 公司账号 Adapter | 外部接入项 | 需要公司开发替换 `MockAccountAdapter`，生产不得信任 `X-Mock-User-Id` |
| 公司审核 Adapter | 外部接入项 | 需要公司审核协议覆盖输入、歌词、发布包交接前审核 |
| 公司权益 Adapter | 外部接入项 | 需要公司权益系统实现 `lock / commit / release` 幂等语义 |
| 公司发布/分享系统 | 外部接入项 | 本平台只交付发布包和交接状态，不实现社区发布、分享、互动、推荐流 |
| 正式 `apps/web` 承接策略 | 待决策 | 当前可验收前端在 `prototypes/Claude-web-v1`；是否迁移进正式工程需定 |

## 下一步建议

1. 准备真实模型联调窗口：Suno / MiniMax 各 1 次成功路径，DeepSeek / Image 2 分别建立硬开关、runbook 和验收清单。
2. 把 `prototypes/Claude-web-v1` 的去向定下来：保留为原型、迁移到 `apps/web`，或交给公司前端按它重做。
3. 公司开发进入前，按 `docs/handover/company-adapter-deployment-handoff-v0.1.md` 确认账号、审核、权益、发布、分享五类 Adapter 替换方案。
