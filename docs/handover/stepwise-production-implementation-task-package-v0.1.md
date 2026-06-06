# Stepwise Production Implementation Task Package v0.1

更新时间：2026-06-07 06:35 CST
状态：Implementation gate prepared; `stepwise-production` not implemented.

## 1. 结论

当前 `stepwise-recording` 只用于验证 Temporal workflow 能按步骤写入 `generation_job_steps` 审计记录。它不会推进 `works`、`generation_jobs`、`media_assets`、`publish_packages` 或权益事务到可交付完成态，因此不得作为生产链路、用户实测链路或 MP4 发布包完成证据。

后续要替代单 activity 编排时，必须新增独立的 `stepwise-production` worker 模式。该模式只有在能用 Mock provider 分步推进到 `GENERATED / PACKAGE_READY`、生成音频/封面/视频/timeline/发布包、完成发布包预检和权益提交后，才允许进入用户实测候选。

DreamMaker 音乐和 DreamMaker Image 2 接口必须继续保留为正式生产目标。Yunwu Suno 和 WellAPI Image 2 只用于当前非公司内网环境下的公网受控 smoke，不得替代、删除、弱化或绕过 DreamMaker 生产路径。

## 2. 不得混淆的模式

| 模式 | 当前用途 | 能否生成发布包 | 能否给用户实测 | 真实外部调用 |
|---|---|---:|---:|---:|
| `sync + mock` | 当前本地用户实测主路径 | 能 | 能 | 不能 |
| `legacy` worker | Temporal v0.1 兼容路径，单 activity 委托现有生产 workflow | 能 | 可内部联调 | 默认不能 |
| `stepwise-recording` | 录制 step audit、验证 workflow 顺序 | 不能 | 不能 | 不能 |
| `stepwise-production` | 后续分步生产态推进模式 | 未实现 | 未实现前不能 | 默认不能 |

## 3. 第一批实现切片

### Slice 1：只开放配置测试，不改变默认路径

- 在 `TemporalWorkerProperties` 中新增 `stepwise-production` 配置值。
- 默认仍保持 `legacy`，同步 Mock 用户实测路径不变。
- `stepwise-recording` 继续只绑定 `RecordingSongProductionStepActivities`，不得改成推进生产状态。
- 新增 worker 配置测试，证明 `legacy`、`stepwise-recording`、`stepwise-production` 三种模式分别注册正确 workflow/activity。

验收：没有 `ProductionSongProductionStepActivities` 和独立 smoke 前，不允许把 `stepwise-production` 写进交付状态为 `READY_LOCAL`。

### Slice 2：实现 Mock-only production step activities

- 新增 `ProductionSongProductionStepActivities`，每个 step 既写 `generation_job_steps`，也推进领域状态。
- 第一版只使用 Mock provider、Mock Agent、Mock Adapter 和现有对象存储，不调用真实 DeepSeek、DreamMaker、Yunwu、WellAPI、Suno、MiniMax、Image 2 或公司系统。
- 状态推进必须覆盖 quota lock、music prompt、music precheck、submit/poll/import audio、cover prompt、cover generation、video rendering、package quality、publish package precheck、package assembly、quota commit。
- 失败必须覆盖 quota release、work/job failure、`failure_code`、`recommended_action` 和 `available_actions`。

验收：`stepwise-production + mock` 能最终写出 `AUDIO`、`COVER`、`VIDEO`、`TIMELINE`、`publish_packages`，并让作品进入 `GENERATED / PACKAGE_READY`。

### Slice 3：抽出可复用生产副作用服务

- 从 `MockSongProductionWorkflow` 中逐步抽出可复用服务，避免 step activity 复制业务规则。
- 优先抽出对象存储写入、远程媒体导入、发布包组装、默认封面兜底、失败收口、权益提交/释放、Provider 调用记录。
- 每个服务必须保留幂等键和可恢复信息，不把完整 prompt、完整歌词、供应商原始响应、JWT、API Key 或签名 URL 写入 Temporal history。

验收：同步 Mock 主链路和 `stepwise-production + mock` 共享关键副作用能力，状态和发布包字段口径一致。

### Slice 4：新增独立 smoke

- 新增 `scripts/smoke/temporal-stepwise-production.sh`。
- 该脚本必须在 API + worker 分进程运行下验证 outbox 启动、完整 step audit、作品完成、发布包可获取、URL 可刷新、交接可标记。
- 该脚本必须显式关闭真实 DreamMaker、Yunwu、WellAPI、DeepSeek、Suno、MiniMax、Image 2 和公司系统调用。

验收：通过该 smoke 前，`stepwise-production` 不进入本地交付 `READY_LOCAL`。

### Slice 5：逐项接入真实模型

- 先在 `stepwise-production + mock` 稳定后，再一次只打开一个真实外部成本点。
- 音乐生产目标优先保留 DreamMaker Suno / DreamMaker MiniMax；当前公网可继续用 Yunwu Suno 做受控 smoke。
- 封面生产目标优先保留 DreamMaker Image 2；当前公网可继续用 WellAPI Image 2 做受控 smoke。
- 每次真实 smoke 必须走 `scripts/smoke/real-model-controlled-smoke.sh` 的 `plan / preflight / execute` 路线，并更新脱敏证据日志。

验收：Yunwu / WellAPI 成功样本不能写成 DreamMaker 生产成功；DreamMaker 成功样本必须独立留证。

## 4. 必须保留的安全门

- 自动化测试不得调用真实模型供应商或公司系统。
- 真实模型必须同时满足全局 `ALLOW_REAL_MODEL_SMOKE=1` 和目标级 `ALLOW_*` 开关。
- 真实凭据只允许通过 shell、部署 Secret 或公司配置中心注入。
- Temporal history 不得保存完整 prompt、完整歌词、供应商完整响应、JWT、API Key、用户 token、签名 URL 或大体积媒体 payload。
- 发布包进入 `PACKAGE_READY` 前必须完成发布包质量门和 `ModerationAdapter.preCheckPublishPackage` 或等价 Mock 边界。
- `mark-fetched` 只表示素材已交接给公司发布流程，不表示社区发布成功。

## 5. 当前审计规则

当前阶段只要求通过：

```bash
scripts/smoke/stepwise-production-boundary-audit.sh
```

该审计不启动 API、worker、Docker、浏览器、数据库、对象存储或真实供应商。它只检查：

- `stepwise-recording` 仍被描述为录步模式，不能误写成用户实测或发布包完成链路。
- `stepwise-production` 当前未实现，或若未来实现，必须同时存在 production activity 和独立 smoke。
- 公司交接包、状态页、验收清单和 README 都引用该边界。
- DreamMaker 仍被明确保留为生产目标，Yunwu / WellAPI 仍被标注为公网受控 smoke。

## 6. 非目标

- 本任务包不要求立即实现 `stepwise-production`。
- 本任务包不改变当前 `sync + mock` 用户实测路径。
- 本任务包不调用真实 DeepSeek、DreamMaker、Yunwu、WellAPI、Suno、MiniMax、Image 2 或公司系统。
- 本任务包不决定公司最终部署拓扑。
