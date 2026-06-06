# Image 2 真实联调验收清单

## 联调前

- [ ] `git status --short` 无未记录的无关改动。
- [ ] `IMAGE_PROVIDER=mock` 和 `IMAGE_REAL_CALLS_ENABLED=false` 是默认状态。
- [ ] 当前公网联调默认 `IMAGE2_BACKEND=wellapi`，`WELLAPI_API_KEY` 只通过当前 shell、部署 Secret 或公司配置中心注入。
- [ ] `IMAGE_PROVIDER=image2`、`IMAGE_REAL_CALLS_ENABLED=true`、`IMAGE2_BACKEND=wellapi`、`WELLAPI_BASE_URL` 和 `WELLAPI_API_KEY` 必须配置完整才允许真实调用。
- [ ] 若切 `IMAGE2_BACKEND=dreammaker`，才额外要求 `DREAMMAKER_REAL_CALLS_ENABLED=true` 和 DreamMaker AK/SK。
- [ ] `./gradlew :modules:image2:test :modules:creative-agent:test :apps:music-api:test --tests '*MockSongProductionWorkflow*'` 通过。
- [ ] 本地 Docker 基础设施健康。
- [ ] API 可在全 Mock 状态跑通 `scripts/smoke/api-main-flow.sh`。
- [ ] 真实客户端已有硬开关、WellAPI URL / `b64_json` 输出处理、对象存储导入和脱敏错误处理；默认封面兜底若未实现，需明确本次联调接受 `PACKAGE_BUILD_FAILED` 收口。

## 封面成功路径

- [ ] `CoverPromptAgent` 输出 visual prompt、negative prompt 和 provider options。
- [ ] `CoverGenerationService` 受控调用真实 Image 2。
- [ ] 供应商封面图被导入平台对象存储。
- [ ] `media_assets` 写入 `COVER` 资产。
- [ ] 当前 workflow 的 `COVER.width` / `COVER.height` 为 1920x1080；`IMAGE2_SIZE=2048x1152` 只是客户端兜底值，不作为本链路验收尺寸。
- [ ] `COVER.mime_type` 与真实文件一致。
- [ ] `media_assets.metadata_json.object_storage_imported=true`。
- [ ] `media_assets.metadata_json` 不包含 API Key、鉴权 header、供应商完整 payload、供应商原始图片 URL 或 inline base64 原文。

## 发布包闭环

- [ ] 作品进入 `GENERATED / PACKAGE_READY`。
- [ ] 发布包 `cover.url` 只暴露平台签名 URL。
- [ ] 成品页可展示封面、音频和视频。
- [ ] `video.url`、`lyrics.timeline_url` 不受真实封面接入影响。
- [ ] 标记交接和刷新 URL 仍可用。

## 失败与兜底

- [ ] 未设置 `IMAGE_REAL_CALLS_ENABLED=true` 时，真实请求被本地保护层拒绝。
- [ ] 缺失 WellAPI API Key、base URL 或真实调用开关时，请求不会发出外部 HTTP。
- [ ] 401 / 403 / 429 / timeout 至少能落库为脱敏错误。
- [ ] 供应商内容安全阻断能映射为可读失败码。
- [ ] 供应商 URL 下载失败能映射为可读失败码。
- [ ] 供应商仅返回 `b64_json` 时，图片会直接写入平台对象存储，metadata 和发布包不保留 base64 原文。
- [ ] 默认封面兜底可进入 `media_assets` 和发布包。
- [ ] 回退 `IMAGE_PROVIDER=mock` 后本地主链路仍能跑通。

## 安全

- [ ] 提交内容不包含真实 API Key、鉴权 header、token、Cookie 或私钥。
- [ ] 日志、截图和联调记录不包含完整图像 Prompt、完整请求或完整响应。
- [ ] 发布包不包含供应商原始图片 URL 或供应商凭据。
- [ ] 终端联调结束后已 `unset` 敏感环境变量。
- [ ] 可选使用 `ALLOW_WELLAPI_IMAGE2_REAL_SMOKE=1 scripts/smoke/wellapi-image2-real-cover-stack-smoke.sh` 执行单作品公网 smoke；若 API 已手动启动，则使用 `scripts/smoke/wellapi-image2-real-cover-smoke.sh`。
- [ ] 生产目标 DreamMaker Image 2 可用 `ALLOW_REAL_MODEL_SMOKE=1 ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE=1 TARGET=dreammaker-image2 MODE=execute scripts/smoke/real-model-controlled-smoke.sh` 执行单作品 smoke；若 API 已手动启动，则使用 `scripts/smoke/dreammaker-image2-real-cover-smoke.sh`。

## 交接

- [ ] 更新 `docs/project-progress.md`。
- [ ] 更新 `docs/integrations/image2-open-questions-tracker.md` 的确认项状态。
- [ ] 若发现新错误码，补充失败码样本、retryable 判断和处理建议。
- [ ] 若发布包媒体字段需要变化，先升级接口契约，再交给前端。
