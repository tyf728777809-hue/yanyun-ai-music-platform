# Image 2 凭据与日志处理规则

## 敏感信息范围

以下内容一律视为敏感信息：

- WellAPI API Key、DreamMaker AccessKey / SecretKey，以及后续公司 Secret 系统中的等价凭据。
- DreamMaker JWT、WellAPI / Image 2 鉴权 header、Bearer token、签名、临时 token。
- 私有 API base URL，如果供应商或公司要求不公开。
- 完整图像生成 Prompt、完整请求、完整响应。
- 供应商返回的原始图片 URL、带签名下载 URL 或任务详情 payload。
- 供应商返回的 `b64_json` 原文或其他完整图片二进制编码。
- 用户输入中可能包含的个人信息、账号信息、联系方式或公司内部资料。
- Cookie、私钥、生产配置、公司用户 token。

## 禁止事项

- 不把真实凭据写入 `.env.example`、Markdown、测试、fixture、截图、提交信息或 issue。
- 不把 `Authorization`、API Key、token 或签名参数打印到日志。
- 不提交完整图像 Prompt、完整请求 payload、完整响应 payload。
- 不在自动化测试中调用真实 Image 2。
- 不把 Image 2 失败原文直接写入作品失败信息或用户可见错误。
- 不把供应商原始图片 URL 直接写入发布包；必须先导入平台对象存储。
- 不把 `b64_json` 原文写入 `media_assets.metadata_json`、发布包、日志或文档；只能在内存中解码后写对象存储。

## 允许记录的字段

- `work_id`。
- Provider 名称与模型名。
- 平台对象存储 key。
- 图片宽高、mime type、文件大小、checksum。
- 脱敏后的错误码和短错误消息。
- 耗时、状态、是否成功、是否可重试。
- 是否使用默认封面兜底。

## 代码侧保护

- `IMAGE_PROVIDER=mock` 或 `IMAGE_REAL_CALLS_ENABLED=false` 时，真实 Image 2 客户端必须在外部 HTTP 请求前失败。
- `IMAGE2_BACKEND=wellapi` 时，缺失 `WELLAPI_BASE_URL`、`WELLAPI_API_KEY` 或未设置 `IMAGE_REAL_CALLS_ENABLED=true`，必须在外部 HTTP 请求前失败。
- `IMAGE2_BACKEND=dreammaker` 时，缺失 DreamMaker AK/SK、未设置 `DREAMMAKER_REAL_CALLS_ENABLED=true` 或未设置 `IMAGE_REAL_CALLS_ENABLED=true`，必须在外部 HTTP 请求前失败。
- 真实客户端必须设置超时和最大尝试次数；默认不得无限重试。
- 日志必须禁用请求/响应 body 原文输出。
- 错误文本入库前必须截断，并脱敏 `Authorization`、`Bearer`、`api_key`、`token`、`secret`、`password` 等字段。
- `media_assets.metadata_json` 可以记录模型、尺寸、风格和安全区，但不得记录完整供应商 payload 或凭据。
- 自动化测试必须使用 fake HTTP 或 Mock service，不能依赖真实供应商可用性。

## 凭据注入与清理

只允许使用当前 shell、部署 Secret、CI Secret 或公司配置中心注入真实凭据：

```bash
export IMAGE_PROVIDER=image2
export IMAGE2_BACKEND=wellapi
export IMAGE_REAL_CALLS_ENABLED=true
export WELLAPI_BASE_URL=https://wellapi.ai
export WELLAPI_API_KEY="<from-secure-channel>"
```

联调结束后：

```bash
unset WELLAPI_API_KEY
unset IMAGE2_MODEL_NAME IMAGE2_SIZE IMAGE2_QUALITY IMAGE2_OUTPUT_FORMAT
unset IMAGE_REAL_CALLS_ENABLED IMAGE_PROVIDER
```

提交前至少执行：

```bash
git diff --check
git status --short
```

如使用本机密钥扫描脚本，应只输出命中的文件名，不在终端粘贴真实密钥原文。
