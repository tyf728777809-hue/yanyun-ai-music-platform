# ADR 0005: 取消独立燕云知识库

日期：2026-06-11

## 状态

已决策

## 背景

项目早期设计中预留了 Markdown 燕云语料库、OpenSearch 检索、知识库版本和检索引用字段，用于辅助 DeepSeek 写词。但当前仓库并没有真实燕云知识库内容，也没有真实 OpenSearch 检索链路；主链路只有 `MockKnowledgeService` 返回少量假引用。

用户已明确确认：燕云知识库取消，不再建设独立语料库/RAG 检索能力。

## 决策

- 不再把独立燕云知识库、Markdown 语料索引或 OpenSearch 检索作为产品能力和交付目标。
- 写词链路继续保留燕云风格，但风格约束来自 Prompt 模板、CreativeBriefAgent、产品文案和模型指令，不来自独立知识库检索。
- 主链路默认使用 no-op knowledge service，返回空引用与 `disabled` 版本标记。
- OpenAPI 中已有 `yanyun_references`、`knowledge_base_version` 等字段暂时保留兼容，默认为空或 `disabled`；后续如要做破坏性接口清理，需要另起版本。
- 早期数据库中的 knowledge 表和 `lyrics_drafts.knowledge_base_version` 暂不做破坏性 migration；后续集中清理历史兼容字段。
- OpenSearch 不再是当前必需核心组件；如后续没有其它搜索用途，应从 Docker Compose、部署文档和验收清单中移除。

## 影响

- `LyricsAgent` 不再依赖燕云知识库检索才能写词。
- 真实 DeepSeek 写词联调不需要准备知识库、索引任务或 OpenSearch。
- 前端不应把“燕云引用”作为必须展示的信息；为空时应隐藏相关区域。
- 文档中“DeepSeek + 燕云知识库 / 语料库 / RAG / OpenSearch 检索”的表述需要逐步替换为“DeepSeek + Prompt 风格约束”。
- 后续工程清理可以删除 `knowledge-indexer` 规划、OpenSearch 基础设施、knowledge tables 和相关验收项。

## 不做

- 本次不删除历史 migration，避免破坏已有本地数据库。
- 本次不直接删除 OpenAPI 字段，避免前端原型和公司接入方契约突然断裂。
- 本次不删除所有旧版 v0.1/v0.2 文档中的历史描述，只在最新 Source of Truth 和进度中标注新决策优先生效。
