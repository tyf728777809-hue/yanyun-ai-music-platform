# Image 2 接入开放问题跟踪

更新时间：2026-06-07

## 当前结论

当前项目已完成 `CoverPromptAgent` Mock 合约、`CoverGenerationService` Mock 封面边界、`DreamMakerImage2CoverGenerationService`、`WellApiImage2CoverGenerationService`、封面媒体资产写入、供应商图片导入平台对象存储和发布包引用。当前非公司内网公网联调默认 `IMAGE2_BACKEND=wellapi`，调用 WellAPI `gpt-image-2`；DreamMaker Image 2 接口保留为正式生产目标切换路径。真实 Image 2 调用尚未开始；执行真实联调前仍需安全注入对应后端的凭据并确认成本止损。

## 待确认问题

| 编号 | 问题 | 当前状态 | 影响 |
|---|---|---|---|
| IQ-1 | Image 2 API base URL | 已确认：当前联调 `https://wellapi.ai`；生产目标 DreamMaker `https://api-all.dreammaker.netease.com` 保留 | 按 `IMAGE2_BACKEND` 切换 |
| IQ-2 | 鉴权方式与 header 格式 | 已确认：WellAPI 使用 Bearer API Key；DreamMaker 使用 AK/SK 生成 JWT | 凭据仍只允许安全注入 |
| IQ-3 | 可用模型名与模型参数 | 已确认：`gpt-image-2` | `IMAGE2_SIZE`、quality、format 可通过 env 调整 |
| IQ-4 | 请求 / 响应 schema | 已实现：WellAPI `POST /v1/images/generations`，兼容 URL 与 `b64_json`；DreamMaker task submit + status output URL 保留 | WellAPI 文档响应示例疑似错贴，真实样本后继续校验字段稳定性 |
| IQ-5 | 输出格式与尺寸限制 | 已确认：默认 `2048x1152`，JPEG | 1920x1080 不满足 16 倍数约束时自动使用默认尺寸 |
| IQ-6 | 图片 URL TTL 和下载鉴权 | 待确认 | 决定对象存储导入时机和失败处理 |
| IQ-7 | 限流、并发和 QPS | 待确认 | 决定重试、排队和成本止损 |
| IQ-8 | 内容安全与违规返回 | 待确认 | 决定 negative prompt、默认封面兜底和失败码映射 |
| IQ-9 | 计费口径与成本上限 | 待确认 | 决定单轮联调样本数量和生产保护 |
| IQ-10 | 是否支持 seed / reference image | 待确认 | 影响风格一致性和可复现性 |

## 已有本地边界

- `modules:image2`：`CoverGenerationService`、请求/响应合约、`MockCoverGenerationService`、`DreamMakerImage2CoverGenerationService` 和 `WellApiImage2CoverGenerationService`。
- `modules:creative-agent`：`CoverPromptAgent` Mock 合约。
- `modules:production`：确认出歌后调用 `CoverPromptAgent` 和 `CoverGenerationService`，并写入 `COVER` 媒体资产。
- `media_assets`：封面资产 object key、mime type、宽高、metadata 持久化。
- `docs/runbook/image2-controlled-real-integration.md`：真实联调步骤。
- `docs/security/image2-secret-and-log-handling.md`：凭据与日志规则。
- `docs/checklists/image2-real-integration-acceptance.md`：验收清单。

## 下一步

1. 用户在本地 shell 或 Secret 系统安全注入 WellAPI API Key 后，先做 1 条确认出歌样本，保持音乐 Provider 为 mock。
2. 验证鉴权、生图、对象存储导入、发布包引用和日志脱敏。
3. 再补默认封面兜底、失败样本和 retryable 规则。
4. 根据真实 401 / 403 / 429 / timeout / URL 过期样本细化失败码与回退策略。
