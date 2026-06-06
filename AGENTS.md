# AGENTS.md

## Project Identity

燕云 AI 作曲平台是一个长期建设的正式商用级 AI 作曲与视频成片平台，不是 Demo、临时活动页或一次性原型。

当前总目标：

1. 先在本地跑通完整链路：用户输入 -> AI 写词/润色 -> 歌词确认 -> 出歌 -> 封面 -> MP4 歌词视频 -> 发布包。
2. 本地阶段通过 Mock Adapter 预留账号、审核、权益、发布、分享等公司系统边界。
3. 本地跑通后交给公司开发替换真实接入，并部署到公司服务器。

## Communication

- 默认用中文沟通。
- 对用户解释技术问题时，用技术小白也能理解的表达，同时保留关键工程判断。
- 用户只要求分析、review、计划或讨论时，不改文件、不执行有状态命令。
- 用户明确要求实现、整理、写文档或执行时，直接推进到可用结果。
- 长任务要给简短阶段性进展；完成后说明改了什么、如何验证、还有什么风险。

## Source Of Truth

优先按以下文档判断项目方向：

1. `yanyun-ai-music-platform-prd-v0.3.md`
2. `yanyun-ai-music-platform-tech-design-v0.2.md`
3. `docs/adr/0001-user-web-scope.md`
4. `docs/adr/0002-commercial-grade-stack.md`
5. `docs/adr/0003-frontend-delivery-track.md`
6. `docs/project-progress.md`
7. `docs/codex-batch-01-repository-initialization.md`
8. `docs/tech-design-required-supplements.md`

如果这些文档之间冲突：

- 先以用户当前明确指令为准。
- 其次以 ADR 和最新版本 PRD/技术方案为准。
- 必要时同步更新冲突文档，或在 `docs/project-progress.md` 标注待同步。

## Fixed Decisions

- 用户侧正式工程承接目录保留为 `apps/web`。
- 当前可验收、可本地实测的前端工作台是 `prototypes/Claude-web-v1`；迁移到 `apps/web` 或交给公司前端重建前，必须先更新 ADR 和项目进度。
- `apps/web` 使用 React + Vite + TypeScript。
- 用户侧移动端 H5/WebView 优先，同时兼容 PC Web。
- 后端主栈为 Java 21 + Spring Boot 3 + Gradle Kotlin DSL。
- 数据库为 PostgreSQL 16，缓存/幂等辅助为 Redis 7。
- 长耗时生成任务使用 Temporal。
- 对象存储使用 S3 兼容接口，本地用 MinIO。
- 燕云语料检索使用 OpenSearch。
- 视频成片使用 Node.js 22 + Remotion + FFmpeg/FFprobe。
- 可观测保留 OpenTelemetry + Prometheus + Grafana。
- DeepSeek、MiniMax、Image 2、公司系统都必须通过 Provider/Adapter 边界接入。
- DreamMaker 音乐和 DreamMaker Image 2 接口必须保留为正式生产目标；Yunwu / WellAPI 只作为当前非公司内网环境下的公网联调后端，不得因为临时联调成功而删除、弱化或绕开 DreamMaker 路径。
- 不为短期提速移除核心商用级组件；业务能力可以分批启用。

核心技术栈或用户侧范围如需改变，必须先新增或更新 ADR、PRD/技术方案和项目进度文档。

## External System Boundary

本平台负责：

- 创作入口。
- AI 写词、润色、续写。
- 歌词确认。
- 出歌任务编排。
- 封面生成。
- MP4 歌词视频生成。
- 作品状态、生成阶段、失败原因、重试动作。
- 发布包生成、存储、状态和交接接口。

公司既有系统负责：

- 真实账号登录与用户身份。
- 真实审核策略。
- 真实权益/任务次数。
- 社区发布。
- 分享系统。
- 点赞、评论、收藏、推荐流和互动。

本地开发阶段只实现 Mock Adapter，不在本平台内重做公司社区系统。

## Product And UX Rules

- 移动端体验优先，PC Web 不能破坏核心链路。
- 普通用户侧文案不要暴露过强技术词，例如主按钮不直接写“获取发布包”。
- 用户应能理解当前状态：等待输入、歌词待确认、生成中、生成成功、可重试失败、不可重试失败、发布包已准备好或被阻断。
- 失败页必须给出可执行动作：重试、返回修改、稍后再试、联系客服/运营排查。
- MP4 预览要关注播放可用性、字幕安全区、音画同步、文件大小和 URL 有效期。

## Visual Asset Rules

- 后续如前端、视频模板、封面占位、运营页面或演示材料需要图片资产，优先使用 Image 生成。
- 不用低质量占位图、随手画的 SVG 或无授权素材充当正式视觉资产。
- 生成图片前应先明确用途、尺寸、风格、主体、是否需要透明背景、是否会进入 Git。
- 大体积图片、临时生成图和未确认版权/授权的素材不得直接提交。
- 如果图片资产只是用于本地设计验证，应记录来源和用途，并在提交前确认是否需要纳入仓库。

## Backend Rules

- 领域状态必须可追溯，不用散落的字符串临时判断业务状态。
- `works.version` 用于状态流转和并发控制。
- Workflow 启动与数据库状态必须考虑一致性：前期可用状态补偿，进入真实模型链路前必须补可靠 Outbox 或等价机制。
- 权益口径按当前决策执行：确认出歌时锁定；MP4 发布包可用后扣减主权益；失败按原因释放或允许重试。
- 发布包对外可获取前必须经过 `ModerationAdapter.preCheckPublishPackage`。
- Provider 模块不得把供应商 HTTP 请求体泄露到领域模块。
- Adapter 模块只定义公司系统边界，本地默认 Mock。

## Frontend Rules

- `apps/web` 或当前验收前端 `prototypes/Claude-web-v1` 只调用 `music-api`，不直接访问数据库、对象存储、Provider 或公司真实系统。
- 页面优先覆盖：创作首页、歌词确认页、生成中页、失败页、成品页、作品列表。
- 移动端优先设计，PC Web 作为响应式增强。
- 状态展示必须使用后端统一状态、生成阶段、失败码、可执行动作和发布包状态。
- 前端视觉和页面实现优先整理成可转交 Gemini 的任务包，由用户传达给 Gemini 实现。
- 本 Agent 主要负责前端需求拆解、信息架构、接口契约、状态字段、异常分支、验收标准和实现审查。
- 除非用户明确要求，本 Agent 不直接承担最终视觉设计和高保真前端实现。
- 给 Gemini 的前端任务包必须包含：目标页面、用户路径、移动端/PC Web 适配要求、接口字段、状态机、交互状态、错误处理、图片资产需求、验收标准和禁止事项。
- Gemini 返回前端实现后，本 Agent 负责从产品逻辑、接口一致性、响应式、安全边界和可测试性角度 review。
- 当前前端 smoke 和用户实测默认以 `prototypes/Claude-web-v1` 为对象；不要把 `apps/web` scaffold 误写成已完成的正式前端交付。

## Testing And Verification

代码项目尚未初始化前，文档任务至少做：

- 文档一致性搜索。
- 关键决策字段搜索。
- Git 状态检查。
- 敏感信息搜索。

第 1 批工程初始化后，优先使用以下检查：

```bash
./gradlew clean build
./gradlew test
./gradlew spotlessCheck

cd apps/web
npm install
npm run build
npm test

cd ../render-worker
npm install
npm run build
npm test
```

如命令尚未存在或不能运行，必须在最终回复和 `docs/project-progress.md` 说明原因。

自动化测试不得调用真实 DeepSeek、MiniMax、Image 2 或公司生产系统；必须使用 fake provider、Mock HTTP 或本地 Mock Adapter。真实模型联调只能在用户明确同意且环境变量配置完整时进行。

## Progress Tracking

`docs/project-progress.md` 是项目的持久进度记录，用于避免长线开发中因上下文压缩、换会话、换方案或换技术栈导致信息丢失。

每次完成阶段性任务后，必须在最终回复前更新 `docs/project-progress.md`。

阶段性任务包括：

- 需求、PRD、技术方案或架构决策变化。
- OpenAPI、数据库、Workflow、部署方案变化。
- 工程初始化或批次开发完成。
- 测试、验收、联调、部署进展。
- 重要问题修复。
- 外部系统协议确认。
- 技术栈或范围变化。

更新进度文档时至少记录：

- 完成事项。
- 关键决策。
- 验证结果。
- 当前风险。
- 待确认事项。
- 下一步建议。

不要覆盖历史工作日志；只追加新记录，并在需要时更新当前阶段、待确认事项和下一步。

## Git Commit Guidance

- 默认不主动提交 commit，除非用户明确要求。
- 当完成一个清晰阶段、相关检查已运行或已说明无法运行、且当前改动适合形成可回退快照时，必须提醒用户可以 commit。
- 提醒 commit 时，应说明建议提交范围和推荐 commit message。
- 如果用户确认提交，先检查 `git status`，再执行 `git add` 和 `git commit`。
- 不把密钥、临时文件、大体积生成产物、真实模型输出样本或无关改动加入 commit。
- 不 push、不创建远端、不改写历史，除非用户明确要求。

## Security

- 不提交真实密钥、Token、Cookie、私钥、生产凭据或公司敏感配置。
- `.env`、真实账号信息、真实 Provider Key、公司接口凭据必须视为敏感信息。
- 示例配置只能放变量名和本地默认占位值。
- 日志、测试、快照和文档中不得出现真实用户敏感数据。

## Batch 01 Boundary

第 1 批仓库初始化只做工程边界和基础设施，不实现业务主链路。

必须包含：

- 根 Gradle 工程和 Java 模块边界。
- `apps/web` React + Vite + TypeScript scaffold。
- `apps/render-worker` Node.js 22 + Remotion scaffold。
- PostgreSQL、Redis、Temporal、MinIO、OpenSearch、Prometheus、Grafana 的 Docker Compose 基础配置。
- `.gitignore`、`.env.example`、README 和基础测试/构建命令。

不得包含：

- 真实 DeepSeek、MiniMax、Image 2 API Key。
- 真实公司账号、审核、权益、发布、分享系统接入。
- 大体积 MP4、音频、图片生成产物。
- 未授权字体或素材。

## Documentation Hygiene

- 需求变化优先更新 PRD。
- 技术边界变化优先更新技术方案或 ADR。
- 接口契约变化优先更新 OpenAPI 文档。
- 数据库表结构变化优先更新 migration 和数据库设计文档。
- Workflow 或状态机变化必须同步状态、失败码、重试和权益口径。
- 文档新增长期决策时，优先使用 ADR，避免只写在聊天上下文里。
