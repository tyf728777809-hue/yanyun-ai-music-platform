# Image 2 真实联调验收清单

## 联调前

- [ ] `git status --short` 无未记录的无关改动。
- [ ] `IMAGE_PROVIDER=mock` 和 `IMAGE_REAL_CALLS_ENABLED=false` 是默认状态。
- [ ] 真实凭据只通过当前 shell、部署 Secret 或公司配置中心注入。
- [ ] `./gradlew :modules:image2:test :modules:creative-agent:test :apps:music-api:test --tests '*MockSongProductionWorkflow*'` 通过。
- [ ] 本地 Docker 基础设施健康。
- [ ] API 可在全 Mock 状态跑通 `scripts/smoke/api-main-flow.sh`。
- [ ] 真实客户端已有硬开关、timeout、max attempts、失败码映射、默认封面兜底和脱敏日志。

## 封面成功路径

- [ ] `CoverPromptAgent` 输出 1920x1080 visual prompt、negative prompt 和 provider options。
- [ ] `CoverGenerationService` 受控调用真实 Image 2。
- [ ] 供应商封面图被导入平台对象存储。
- [ ] `media_assets` 写入 `COVER` 资产。
- [ ] `COVER.width=1920` 且 `COVER.height=1080`。
- [ ] `COVER.mime_type` 与真实文件一致。
- [ ] `media_assets.metadata_json` 不包含 API Key、鉴权 header 或供应商完整 payload。

## 发布包闭环

- [ ] 作品进入 `GENERATED / PACKAGE_READY`。
- [ ] 发布包 `cover.url` 只暴露平台签名 URL。
- [ ] 成品页可展示封面、音频和视频。
- [ ] `video.url`、`lyrics.timeline_url` 不受真实封面接入影响。
- [ ] 标记交接和刷新 URL 仍可用。

## 失败与兜底

- [ ] 未设置 `IMAGE_REAL_CALLS_ENABLED=true` 时，真实请求被本地保护层拒绝。
- [ ] 缺失 API Key 或 base URL 时，请求不会发出外部 HTTP。
- [ ] 401 / 403 / 429 / timeout 至少能落库为脱敏错误。
- [ ] 供应商内容安全阻断能映射为可读失败码。
- [ ] 供应商 URL 下载失败能映射为可读失败码。
- [ ] 默认封面兜底可进入 `media_assets` 和发布包。
- [ ] 回退 `IMAGE_PROVIDER=mock` 后本地主链路仍能跑通。

## 安全

- [ ] 提交内容不包含真实 API Key、鉴权 header、token、Cookie 或私钥。
- [ ] 日志、截图和联调记录不包含完整图像 Prompt、完整请求或完整响应。
- [ ] 发布包不包含供应商原始图片 URL 或供应商凭据。
- [ ] 终端联调结束后已 `unset` 敏感环境变量。

## 交接

- [ ] 更新 `docs/project-progress.md`。
- [ ] 更新 `docs/integrations/image2-open-questions-tracker.md` 的确认项状态。
- [ ] 若发现新错误码，补充失败码样本、retryable 判断和处理建议。
- [ ] 若发布包媒体字段需要变化，先升级接口契约，再交给前端。
