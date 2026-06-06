# ADR 0004: DreamMaker 作为正式生产供应商目标保留

日期：2026-06-07

## 状态

已决策

## 背景

当前本地和公网联调环境无法完全等同公司内网或生产环境。首次 DreamMaker Suno 真实 smoke 已触达供应商创建任务阶段，但返回 HTTP 403；用户判断当前机器不在公司内网，DreamMaker 可能只支持内网环境。

为了继续推进真实模型联调，项目新增了 Yunwu Suno 和 WellAPI Image 2 作为当前公网受控 smoke 后端。但这两个后端是环境受限下的临时联调路径，不代表正式生产供应商目标发生变化。

## 决策

- DreamMaker 音乐接口必须保留为正式生产音乐供应商目标。
- DreamMaker Image 2 接口必须保留为正式生产封面供应商目标。
- Yunwu Suno 只作为当前非公司内网环境下的公网受控音乐 smoke 后端。
- WellAPI Image 2 只作为当前非公司内网环境下的公网受控封面 smoke 后端。
- 生产部署、公司交接和后续重构不得因为 Yunwu / WellAPI 联调成功而删除、弱化、绕开或改名 DreamMaker 路径。
- 真实供应商调用必须继续经过 Provider/Adapter 边界、真实调用硬开关、readiness guard、受控 smoke 总入口和脱敏日志规则。

## 影响

- 配置上继续保留 `SUNO_BACKEND=dreammaker|yunwu` 和 `IMAGE2_BACKEND=dreammaker|wellapi` 的切换边界。
- `prod/production` Spring profile 和 `deploy/env.production.example` 必须默认 `SUNO_BACKEND=dreammaker` 与 `IMAGE2_BACKEND=dreammaker`；`.env.example` 只作为本地 Mock / 公网 smoke 便利配置。
- 公司内网或生产环境应优先验证 `dreammaker-suno`、`dreammaker-minimax` 和 `dreammaker-image2`。
- 当前公网 smoke 可使用 `yunwu-suno` 和 `wellapi-image2` 缩短联调等待，但交付状态必须标注为公网临时路径。
- 交接包、runbook、验收清单和审计脚本必须能证明 DreamMaker 生产目标仍然存在。

## 禁止事项

- 不把 Yunwu / WellAPI 写成正式生产替代方案。
- 不删除 `DreamMakerHttpClient`、`dreammaker_guard`、`DREAMMAKER_*` 配置变量或 DreamMaker smoke/runbook。
- 不让业务域代码直接依赖 Yunwu / WellAPI / DreamMaker 的 HTTP 请求体。
- 不在自动化测试中调用真实 DreamMaker、Yunwu、WellAPI、DeepSeek、Image 2 或公司系统。
