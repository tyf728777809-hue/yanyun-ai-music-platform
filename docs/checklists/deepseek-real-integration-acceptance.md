# DeepSeek 真实联调验收清单

## 联调前

- [ ] `git status --short` 无未记录的无关改动。
- [ ] `DEEPSEEK_REAL_CALLS_ENABLED=false` 是默认状态。
- [ ] 真实凭据只通过当前 shell、部署 Secret 或公司配置中心注入。
- [ ] `AGENT_REAL_CALLS_ENABLED=true` 与 `DEEPSEEK_REAL_CALLS_ENABLED=true` 必须同时打开才允许真实调用。
- [ ] `DEEPSEEK_BASE_URL=https://api.deepseek.com`、`DEEPSEEK_MODEL_NAME=deepseek-v4-pro` 或经批准的等价值已配置。
- [ ] `./gradlew :modules:deepseek:test :modules:lyrics:test :modules:creative-agent:test` 通过。
- [ ] 本地 Docker 基础设施健康。
- [ ] API 可在全 Mock 状态跑通 `scripts/smoke/api-main-flow.sh`。
- [ ] 真实客户端已有硬开关、timeout、max attempts、JSON 输出解析和脱敏错误处理。

## 灵感成歌成功路径

- [ ] `POST /api/v1/works/inspiration` 受控调用真实 DeepSeek。
- [ ] 作品进入 `LYRICS_READY / WAITING_CONFIRM`。
- [ ] 作品详情有标题、摘要、歌词、music prompt 和燕云引用。
- [ ] `agent_runs` 记录 `CreativeBriefAgent` 和 / 或 `LyricsAgent` 的真实模型名。
- [ ] `agent_runs` 只记录 hash、模板版本、耗时、状态和脱敏错误，不记录完整 Prompt。

## 填词成歌成功路径

- [ ] `POST /api/v1/works/lyrics` 受控调用真实 DeepSeek。
- [ ] 用户输入歌词被保留为主要歌词文本。
- [ ] 模型补齐标题、摘要、music prompt、封面 seed 和必要风险提示。
- [ ] `lyrics_drafts` 仍持久化知识库版本、Prompt 模板版本、质量分和燕云引用。

## 润色与续写

- [ ] `POST /api/v1/works/{work_id}/lyrics/polish` 生成新版歌词草案。
- [ ] `POST /api/v1/works/{work_id}/lyrics/continue` 生成新版歌词草案。
- [ ] 两次操作后 `polish_remaining_count=0`。
- [ ] 第三次改词返回 HTTP 409 和友好错误。
- [ ] 模型失败时不静默消耗用户侧改词次数。

## 下游闭环

- [ ] 确认出歌后，音乐仍使用 `MUSIC_PROVIDER=mock` 或显式 mock。
- [ ] 作品可进入 `GENERATED / PACKAGE_READY`。
- [ ] 发布包可获取，且不包含 DeepSeek 原始响应或凭据。
- [ ] `DREAMMAKER_REAL_CALLS_ENABLED=false`、Image 2 和公司 Adapter 仍保持关闭或 Mock。

## 失败与止损

- [ ] 未设置 `DEEPSEEK_REAL_CALLS_ENABLED=true` 时，真实请求被本地保护层拒绝。
- [ ] 缺失 API Key、模型名或总开关时，请求不会发出外部 HTTP。
- [ ] 401 / 403 / 429 / timeout 至少能落库为脱敏错误。
- [ ] 输出解析失败能映射为可读失败码。
- [ ] 回退 Mock 后本地主链路仍能跑通。

## 安全

- [ ] 提交内容不包含真实 API Key、鉴权 header、token、Cookie 或私钥。
- [ ] 日志、截图和联调记录不包含完整 Prompt、完整请求或完整响应。
- [ ] `agent_runs.error_message` 不包含 Bearer token、key/token 字段原文。
- [ ] 终端联调结束后已 `unset` 敏感环境变量。

## 交接

- [ ] 更新 `docs/project-progress.md`。
- [ ] 更新 `docs/integrations/deepseek-open-questions-tracker.md` 的确认项状态。
- [ ] 若发现新错误码，补充失败码样本、retryable 判断和处理建议。
- [ ] 若 OpenAPI 用户可见字段需要变化，先升级接口契约，再交给前端。
