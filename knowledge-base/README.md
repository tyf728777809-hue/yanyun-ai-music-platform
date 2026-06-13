# Knowledge Base

本目录用于沉淀燕云题材相关的世界观、地点、角色、事件、玩法体验、歌词风格与禁写项，供创作知识库导入和写词链路检索使用。

当前主入口是 `commercial-final/`。它按人物、剧情/任务线、地域/场景、势力/门派、玩法体验、创作禁区和 eval case 分域维护，导入时合并为一个 `kb_version`。

`yanyun-official-kb-v1.json` 是历史工程种子，只用于保留早期单文件导入样例，不再作为商用默认入口。

资料治理原则：

- 官方资料优先；攻略站、玩家维基、剧情实录整理和自问自答结果可以进入创作叙事层，来源等级使用 `guide_wiki_synthesis` / `accepted_story_synthesis`。
- 未经整理的原始社区散帖不直接入库。
- 只向 Agent 注入短摘要、情绪弧线、可用意象和禁写项。
- 用户实时写词链路不联网搜索；搜索只用于构建期资料整理。
- 检索失败时降级为空知识上下文，不阻断创作。
- 每次导入商用知识库前必须先跑 `STRICT_COMMERCIAL=1 python3 scripts/knowledge/validate-yanyun-knowledge.py knowledge-base/commercial-final`。

来源登记见 `docs/integrations/yanyun-knowledge-source-registry.md`。
