# Company Adapter Deployment Handoff v0.1

更新时间：2026-06-06

## 1. 标题与元数据

- 标题：第 10 批公司 Adapter 接入与部署交接准备
- 作者：Codex
- 状态：Approved for local readiness implementation
- 适用范围：公司账号、审核、权益、发布、分享 Adapter 替换边界；部署变量；内部 readiness 报告；交接文档
- 评审依据：PRD v0.3、技术方案 v0.2、OpenAPI v0.1、`AGENTS.md`、第 8 批对象存储规格

## 2. 背景

当前本地链路已经可以通过 Mock Adapter 跑通写词、确认出歌、音乐生成、封面/视频 Mock 资产、发布包 JSON 和 MinIO/S3 存储。项目目标不是在本平台内重做公司社区系统，而是在本地跑通完整生成链路后，把账号、审核、权益、发布、分享等边界交给公司开发替换真实系统。

公司接入前最容易出问题的点不是业务代码是否能跑，而是 Mock 与真实系统边界不清：哪些接口必须替换、哪些字段需要对齐、哪些环境变量属于本地默认值、哪些状态仍然只是本平台内部状态。本批目标是把这些边界固化成规格、运行文档和一个可查询的内部 readiness 报告。

本批不接真实公司接口，不实现真实登录、审核、权益扣减、社区发布、分享或推荐流。所有真实协议仍由公司开发确认并替换 Adapter 实现。本批只让“当前是否仍为 Mock、上线前还缺什么、哪些变量必须配置”可见、可测试、可交接。

## 3. 功能需求

- FR-1：系统 MUST 明确公司系统 Adapter 边界，至少覆盖账号、审核、权益、发布交接、分享五类。
- FR-2：系统 MUST 提供内部 readiness 报告，列出每类 Adapter 的配置模式、当前实现、readiness 状态、是否阻塞公司部署、替换说明和所需环境变量。
- FR-3：readiness 报告 MUST 不暴露真实密钥、Token、Cookie、JWT、用户身份凭据或对象存储签名 URL。
- FR-4：本地默认配置 MUST 继续使用 Mock Adapter，不破坏当前本地 Mock 生成链路。
- FR-5：当公司 Adapter 配置为非 Mock 但真实实现尚未接入时，readiness 报告 MUST 明确标记为 `BLOCKED`，不得伪装为已接入。
- FR-6：readiness 报告 MUST 同时展示生成链路关键部署依赖，包括音乐 Provider、对象存储 Provider、Workflow dispatch mode、Temporal target 和 DreamMaker 真实调用开关。
- FR-7：公司交接文档 MUST 给出替换清单、接口契约、字段映射、部署变量、验收 smoke 和禁止事项。
- FR-8：`.env.example` MUST 只包含本地占位或空值，不得加入真实公司凭据。

## 4. 非功能需求

- NFR-1：内部 readiness 接口 SHOULD 在无外部依赖调用的情况下返回，避免部署前检查触发真实公司系统或真实模型供应商。
- NFR-2：readiness 报告 SHOULD 在 200ms 内完成，因为只读取配置和静态边界。
- NFR-3：自动化测试 MUST NOT 调用真实公司系统、真实 DeepSeek、真实 Suno/MiniMax、真实 Image 2 或生产对象存储。
- NFR-4：配置和文档 MUST 使用稳定环境变量名，便于公司开发替换和部署审计。
- NFR-5：readiness 输出 MUST 使用结构化 JSON，便于部署脚本或人工检查。

## 5. 验收标准

- AC-1：Given 默认 `.env.example` 配置，When 调用内部 readiness 报告，Then 账号、审核、权益、发布、分享均显示 Mock 或待接入状态，且本地运行不被阻塞。覆盖 FR-1、FR-2、FR-4。
- AC-2：Given 任一公司 Adapter 模式配置为非 Mock，When 真实实现未提供，Then readiness 报告将该项标记为 `BLOCKED` 并说明需要公司开发替换实现。覆盖 FR-5。
- AC-3：Given readiness 报告返回，When 检查 JSON，Then 不包含 SecretKey、API key、JWT、Bearer token、Cookie 或签名 URL。覆盖 FR-3、NFR-3。
- AC-4：Given 本地 API 构建测试，When 运行相关单元测试，Then readiness 服务和内部 controller 测试通过。覆盖 FR-2、NFR-5。
- AC-5：Given 交接文档，When 公司开发阅读，Then 能看到账号、审核、权益、发布、分享各自的替换接口、输入输出、状态口径、环境变量和 smoke 步骤。覆盖 FR-7、FR-8。

## 6. 边界情况

- EC-1：分享系统当前没有业务调用点；readiness 中仍必须列出，因为 PRD 边界要求分享由公司既有系统承接。
- EC-2：账号 Adapter 当前通过 `X-Mock-User-Id` 模拟用户；正式接入后必须由公司账号态或网关注入用户身份，不得信任普通用户自传 Header。
- EC-3：权益 Adapter 当前只做本地 Mock 锁定/扣减/释放；正式接入后必须保证 lock/commit/release 幂等，并与公司权益流水对账。
- EC-4：审核 Adapter 当前只做简单 Mock 预检；正式接入后必须覆盖输入歌词、歌词草案、发布包交接前审核，且返回可映射的阻断原因。
- EC-5：发布/分享不由本平台真实执行；本平台只提供发布包和交接状态，社区发布结果由公司系统管理。

## 7. API Contracts

本批新增内部接口，不进入用户侧 OpenAPI v0.1：

```ts
type IntegrationReadinessReport = {
  environment: string;
  service: "music-api";
  generated_at: string;
  overall_status: "READY_FOR_LOCAL" | "BLOCKED_FOR_COMPANY_DEPLOYMENT";
  components: IntegrationComponentReadiness[];
  notes: string[];
};

type IntegrationComponentReadiness = {
  component: string;
  configured_mode: string;
  implementation: string;
  status: "READY_FOR_LOCAL" | "MOCK_ONLY" | "BLOCKED";
  blocks_company_deployment: boolean;
  required_env_vars: string[];
  handoff_note: string;
};
```

路径：

```http
GET /internal/integration-readiness
```

## 8. 数据模型

| 实体 | 字段 | 类型 | 约束 |
|---|---|---|---|
| `IntegrationReadinessReport` | `environment` | string | 来自 `YANYUN_ENV` |
| `IntegrationReadinessReport` | `overall_status` | enum | 由 components 是否阻塞公司部署推导 |
| `IntegrationComponentReadiness` | `component` | string | 稳定组件名 |
| `IntegrationComponentReadiness` | `configured_mode` | string | 环境变量配置值，不含密钥 |
| `IntegrationComponentReadiness` | `implementation` | string | 当前实现名称 |
| `IntegrationComponentReadiness` | `status` | enum | 本地/Mock/阻塞 |
| `IntegrationComponentReadiness` | `required_env_vars` | string[] | 只列变量名 |

本批不新增数据库表。

## 9. 非目标

- OS-1：不实现真实公司账号登录。
- OS-2：不实现真实公司审核策略。
- OS-3：不实现真实公司权益扣减或权益流水同步。
- OS-4：不实现社区发布、分享、互动、推荐流。
- OS-5：不将真实公司接口地址、密钥、Cookie、JWT 或生产配置写入仓库。
- OS-6：不改变用户侧 OpenAPI v0.1 主路径契约。
