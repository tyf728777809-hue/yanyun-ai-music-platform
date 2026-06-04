# ADR 0002: 商用级技术栈不降级，分批启用

日期：2026-06-05

## 状态

已决策

## 背景

本项目目标不是 Demo 或临时活动页，而是正式商用级 AI 作曲与视频成片平台。当前路线是先在本地跑通完整生成链路，再交给公司开发接入账号、审核、权益、发布、分享等真实系统，并部署到公司服务器。

为了避免后续返工，核心架构能力需要从第一天按商用级边界设计。

## 决策

- 保留 Java 21、Spring Boot 3、PostgreSQL、Redis、Temporal、MinIO/S3、OpenSearch、Remotion、FFmpeg、OpenTelemetry、Prometheus、Grafana。
- 保留 Provider/Adapter 边界，不把公司系统或模型供应商协议写入业务域。
- 第 1 批完成完整商用级组件的工程预置和本地基础设施纳入。
- 业务能力按批次逐步启用，不要求第 1 批实现主链路。
- 不为短期提速移除 Temporal、对象存储、渲染 Worker、语料检索、可观测或 Adapter 边界。

## 影响

- 第 1 批仓库初始化任务必须包含商用级组件目录、配置、Docker Compose 和基础验收。
- 后续批次按数据库/状态机、Mock Adapter、知识库、Provider、渲染、端到端工作流、可观测和压测逐步完成。
- 如果未来需要替换核心技术栈，必须先更新 PRD/技术方案/ADR/进度文档，再执行实现。
