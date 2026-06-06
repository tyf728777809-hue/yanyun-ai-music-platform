# DreamMaker 开放问题跟踪

| ID | 问题 | 状态 | 阻塞级别 | 责任方 | 备注 |
|---|---|---|---|---|---|
| DM-001 | 生产密钥管理、轮换、过期策略 | 待确认 | 高 | 公司运维/安全 | 本地只允许 shell 注入 |
| DM-002 | 非零 `code` 列表及 retryable 判断 | 待确认 | 高 | DreamMaker/公司后端 | 当前保守映射 |
| DM-003 | `failed` 任务 payload schema | 待确认 | 高 | DreamMaker | 需要样本，不记录敏感字段 |
| DM-004 | 限流阈值、轮询间隔、最长轮询时间 | 待确认 | 高 | DreamMaker | 当前默认 60 次，每 2 秒 |
| DM-005 | 输出 `url` 是否一定相对同域或绝对 HTTP(S) | 待确认 | 中 | DreamMaker | 当前 client 支持相对 URL 解析 |
| DM-006 | 输出音频 URL 有效期 | 待确认 | 高 | DreamMaker | 影响对象存储导入重试窗口 |
| DM-007 | 是否需要专门下载 API，而非直接下载 URL | 待确认 | 中 | DreamMaker | 当前按 HTTP(S) 直接导入 |
| DM-008 | Suno/MiniMax 单次任务计费口径 | 待确认 | 中 | 公司产品/财务 | 影响权益成本归因 |
| DM-009 | 内容安全/审核失败错误码 | 待确认 | 高 | DreamMaker/公司审核 | 影响 retryable 与用户提示 |
| DM-010 | 是否强制传 `X-Access-Token` | 待确认 | 中 | 公司后端/DreamMaker | 当前为可选透传 |
| DM-011 | Suno 创建任务阶段 HTTP 403 | 阻塞中 | 高 | DreamMaker/公司后端 | 2026-06-07 首次真实 Suno stack smoke 触达创建任务阶段后返回 403，`provider_trace_id` 为空；用户判断当前不在公司内网，DreamMaker 可能只支持内网环境。仍需确认 AK/SK 是否具备 Suno 应用权限、是否必须提供 `X-Access-Token`、以及 `app=suno` / `sub_app=music-gen` / `model=chirp-crow` 是否匹配账号开通项。当前公网联调临时切到 Yunwu，DreamMaker 接口作为正式生产目标保留 |

## 更新规则

- 每次真实联调后更新状态、样本结论和下一步。
- 只记录脱敏摘要，不记录完整 provider payload。
- 若问题影响上线或公司接入，标记为高阻塞并同步到 `docs/project-progress.md`。
