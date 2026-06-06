# Image 2 接入开放问题跟踪

更新时间：2026-06-06

## 当前结论

当前项目已完成 `CoverPromptAgent` Mock 合约、`CoverGenerationService` Mock 封面边界、封面媒体资产写入和发布包引用。真实 Image 2 调用尚未开始；执行真实联调前需要用户提供供应商 URL、API Key 和协议细节，并由代码补齐真实客户端硬开关、默认封面兜底和对象存储导入。

## 待确认问题

| 编号 | 问题 | 当前状态 | 影响 |
|---|---|---|---|
| IQ-1 | Image 2 API base URL | 待用户提供 | 决定真实客户端目标地址 |
| IQ-2 | 鉴权方式与 header 格式 | 待用户提供 | 决定 API Key 注入和请求构造 |
| IQ-3 | 可用模型名与模型参数 | 待用户提供 | 决定 `IMAGE2_MODEL_NAME`、尺寸、风格等参数 |
| IQ-4 | 请求 / 响应 schema | 待用户提供 | 决定真实响应解析和输出校验 |
| IQ-5 | 输出格式与尺寸限制 | 待确认 | 影响 1920x1080、PNG/JPEG 和发布包 mime type |
| IQ-6 | 图片 URL TTL 和下载鉴权 | 待确认 | 决定对象存储导入时机和失败处理 |
| IQ-7 | 限流、并发和 QPS | 待确认 | 决定重试、排队和成本止损 |
| IQ-8 | 内容安全与违规返回 | 待确认 | 决定 negative prompt、默认封面兜底和失败码映射 |
| IQ-9 | 计费口径与成本上限 | 待确认 | 决定单轮联调样本数量和生产保护 |
| IQ-10 | 是否支持 seed / reference image | 待确认 | 影响风格一致性和可复现性 |

## 已有本地边界

- `modules:image2`：`CoverGenerationService`、请求/响应合约和 `MockCoverGenerationService`。
- `modules:creative-agent`：`CoverPromptAgent` Mock 合约。
- `modules:production`：确认出歌后调用 `CoverPromptAgent` 和 `CoverGenerationService`，并写入 `COVER` 媒体资产。
- `media_assets`：封面资产 object key、mime type、宽高、metadata 持久化。
- `docs/runbook/image2-controlled-real-integration.md`：真实联调步骤。
- `docs/security/image2-secret-and-log-handling.md`：凭据与日志规则。
- `docs/checklists/image2-real-integration-acceptance.md`：验收清单。

## 下一步

1. 用户提供 Image 2 URL、API Key 和协议文档后，先补真实客户端规格，不直接进入大批量联调。
2. 实现 `RealImage2CoverGenerationService` 或等价 Provider Adapter，默认保持关闭。
3. 用单条确认出歌样本验证鉴权、生图、对象存储导入、发布包引用和日志脱敏。
4. 再补默认封面兜底、失败样本和 retryable 规则。
