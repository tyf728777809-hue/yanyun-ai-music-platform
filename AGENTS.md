# AGENTS.md

## Project

燕云 AI 作曲平台是一个长线开发项目。当前目标是从产品与技术基线出发，逐步完成工程初始化、本地完整链路、公司服务器部署和外部系统接入。

## Progress Tracking

- `docs/project-progress.md` 是项目的持久进度记录。
- 每次完成阶段性任务后，必须在最终回复前更新 `docs/project-progress.md`。
- 阶段性任务包括：需求或方案调整、技术栈变更、架构决策、工程初始化、批次开发完成、测试验收、重要问题修复、外部系统协议确认、部署或联调进展。
- 更新进度文档时，应记录完成事项、关键决策、验证结果、当前风险、待确认事项和下一步。
- 不要覆盖历史工作日志；需要保留历史，并追加新的记录。
- 如果 PRD、技术方案、OpenAPI、数据库设计或部署方案发生变化，应同步更新相关文档，或在进度文档中明确标注“待同步”。

## Git Commit Guidance

- 默认不主动提交 commit，除非用户明确要求。
- 当完成一个清晰阶段、相关检查已运行或已说明无法运行、且当前改动适合形成可回退快照时，应提醒用户可以 commit。
- 提醒 commit 时，应简要说明建议提交的范围和推荐 commit message。
- 如果用户确认提交，先检查 `git status`，再执行 `git add` 和 `git commit`。
- 不要把密钥、临时文件、大体积生成产物或无关改动加入 commit。
- 不 push、不创建远端、不改写历史，除非用户明确要求。

## Current Baseline Docs

- `yanyun-ai-music-platform-prd-v0.2.md`
- `yanyun-ai-music-platform-tech-design-v0.1.md`

## Rules

- 默认用中文沟通。
- 不提交真实密钥、Token、Cookie 或生产凭据。
- 未经明确要求，不提交 Git commit、不推送远端、不重写历史。
- 开始代码实现前，先检查当前文档、Git 状态和既有约束。
- 自动化测试不得调用真实 DeepSeek、MiniMax 或 Image 2 API；使用 fake provider 或 Mock HTTP。
