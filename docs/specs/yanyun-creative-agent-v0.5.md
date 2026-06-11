# 燕云创作 Agent v0.5 设计与接入规格

更新时间：2026-06-12

## 1. 设计结论

本版本把“歌词内容题材”和“音乐风格偏好”拆开处理：

- 歌词内容必须服务《燕云十六声》创作域。
- 用户音乐风格偏好可以开放表达，包括现实歌手名、流派和歌曲类型。
- 系统内部必须把现实歌手名改写为泛化音乐语言，不向音乐供应商输出仿唱、声线模仿或现实歌手名。
- `CreativeBriefAgent`、`MusicPromptAgent`、`CoverPromptAgent`、`QualityEvaluationAgent` 均可在真实开关下接入 DeepSeek v4Pro；默认测试仍用 Mock。
- `QualityEvaluationAgent` 不做图片视觉审核，不 OCR，不判断封面画面是否好看。

本规格不处理视频模板、封面裁剪、进度条或字幕策略。

## 2. Functional Requirements

- FR-1：`CreativeBriefAgent` MUST 判断用户请求是否属于燕云创作域，并输出 `PASS`、`REWRITE_TO_YANYUN` 或 `REJECT`。
- FR-2：当用户请求明确要求其他 IP 主题歌且未要求转成燕云语境时，系统 MUST 返回可读拒绝，不进入 `LyricsAgent`。
- FR-3：当用户请求可转译为燕云语境时，系统 MUST 保留情绪、结构或风格偏好，但删除其他 IP 专属名词和设定。
- FR-4：`LyricsAgent` MUST 接收创作域判断和转译建议，并强制歌词内容与燕云相关。
- FR-5：`MusicPromptAgent` MUST 输出面向 Suno Custom/Advanced 思路的结构化生成规格，包括 title、lyrics、style prompt、exclude prompt 和 advanced options。
- FR-6：`MusicPromptAgent` MUST 将现实歌手名、仿唱、声线模仿改写为泛化音乐描述。
- FR-7：`CoverPromptAgent` MAY 生成带高质量歌名文字的完整专辑封面 prompt。
- FR-8：`CoverPromptAgent` MUST NOT 要求假歌手名、假版权、假厂牌、乱码、低质小字、UI、水印或排行榜样式。
- FR-9：`QualityEvaluationAgent` MUST 审核创作边界、歌词文本、音乐 prompt、封面 prompt 和发布包元数据。
- FR-10：`QualityEvaluationAgent` MUST NOT 审核图片像素、OCR 图片文字或判断图片美术质量。

## 3. Runtime Contract

```ts
type CreativeDomainDecision = "PASS" | "REWRITE_TO_YANYUN" | "REJECT";

type CreativeBriefResult = {
  domain_decision: CreativeDomainDecision;
  creative_intent: string;
  theme: string;
  mood_tags: string[];
  narrative_viewpoint: string;
  music_direction: string;
  yanyun_references: string[];
  constraints: string[];
  risk_notes: string[];
  user_facing_message?: string;
  yanyun_rewrite_suggestion?: string;
  freeform_opportunities: string[];
};

type MusicGenerationSpec = {
  title: string;
  lyrics_with_structure_tags: string;
  style_prompt: string;
  exclude_prompt: string;
  advanced_options: Record<string, unknown>;
  provider_options: Record<string, unknown>;
};

type CoverPromptSpec = {
  visual_prompt: string;
  text_prompt: string;
  negative_prompt: string;
  width: number;
  height: number;
  style_constraints: string[];
  typography_requirements: string[];
  provider_options: Record<string, unknown>;
};
```

## 4. Decision Rules

- “写一首高达歌”：`REJECT`，返回“当前只支持燕云十六声相关创作”。
- “写一首有高达那种热血孤独感的燕云歌”：`REWRITE_TO_YANYUN` 或 `PASS`，删除高达设定，保留热血孤独感。
- “写一首周杰伦风格的燕云歌”：内容 `PASS`，风格进入理解层，最终 music prompt 不输出“周杰伦/仿唱/声线模仿”。
- “写一首周杰伦风格的高达歌”：内容 `REJECT`，不进入写词。

## 5. Acceptance Criteria

- AC-1：Given 用户输入“写一首高达歌”，When 创建灵感成歌，Then API 返回 `CREATIVE_DOMAIN_REJECTED`，且不调用 `LyricsAgent`。
- AC-2：Given 用户输入包含“燕云”和“周杰伦风格”，When 生成音乐 prompt，Then music prompt 保留国风 R&B、轻说唱等泛化风格，但不含现实歌手名和仿唱指令。
- AC-3：Given `CoverPromptAgent` 输出成品封面 prompt，When 调用 Image2，Then provider options 能携带文字策略，允许直接生成高质量歌名文字。
- AC-4：Given 封面 prompt 要求假歌手、假版权或 UI，When 经过质量门，Then 返回 `REWRITE`。
- AC-5：Given 发布包元数据完整，When 经过质量门，Then 不做图片视觉审核，只检查 audio/cover/video/timeline 元数据。

## 6. Out Of Scope

- 不做独立燕云知识库/RAG。
- 不接视觉审核模型。
- 不做封面裁剪、视频模板、生成进度条或字幕策略修复。
- 不调用真实模型作为自动化测试的一部分。
