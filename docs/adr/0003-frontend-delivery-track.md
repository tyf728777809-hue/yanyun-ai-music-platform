# ADR 0003: Frontend Delivery Track

日期：2026-06-06
状态：已决策

## 背景

技术方案 v0.2 和第 1 批工程初始化已预置 `apps/web`，作为正式商用级用户侧 Web 工程目录。但当前可验收、可本地实测的高保真创作工作台由外部前端实现者交付在 `prototypes/Claude-web-v1`。

如果不冻结交付口径，公司开发接手时会误解“应该接 `apps/web` 还是 `prototypes/Claude-web-v1`”。

## 决策

- `apps/web` 保留为正式工程 scaffold 和未来承接目录。
- `prototypes/Claude-web-v1` 是当前用户实测和前后端联调验收对象。
- 在交给公司开发或进入正式部署前，必须单独做一次前端承接决策：保留原型目录、迁移到 `apps/web`，或由公司前端按原型重建。
- 在承接决策完成前，不把 `apps/web` 描述为已经完成的正式用户侧交付件。
- 后端、OpenAPI、状态机和 smoke 脚本以 `prototypes/Claude-web-v1` 当前验收结果作为前端联调证据。

## 影响

- README、验收清单和项目进度中提到“前端已可测”时，默认指 `prototypes/Claude-web-v1`。
- `apps/web` 的存在不代表公司可直接拿它作为最终用户侧前端。
- 若后续迁移到 `apps/web`，需要新增迁移任务包、重新跑前端测试、真实后端 UI smoke 和 390px / 1440px 响应式验收。

## 非目标

- 本 ADR 不要求立即迁移 `prototypes/Claude-web-v1` 到 `apps/web`。
- 本 ADR 不改变后端 OpenAPI v0.1、公司 Adapter 边界或真实模型联调顺序。
