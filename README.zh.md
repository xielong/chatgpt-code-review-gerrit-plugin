# ChatGPT 代码审查 Gerrit 插件

[English Version](README.md)

此插件让你可以方便地在 Gerrit 中使用 ChatGPT 进行代码审查。提交 patch 后， OpenAI 会以评论的方式提供审查意见。

## 入门

1. **构建：** 需要 JDK11 以上版本，Maven 3.0 以上版本。

   ```bash
   mvn -U clean package
   ```

2. **安装：** 将编译好的 jar 文件上传到 $gerrit_site/plugins 目录中，然后参考 [配置参数](#配置参数) 进行配置，再重启
   Gerrit。

3. **确认：** 安装好插件后，你可以在 Gerrit 的日志中看到以下信息：

   ```bash
      INFO com.google.gerrit.server.plugins.PluginLoader : Loaded plugin chatgpt-code-review-gerrit-plugin, version 3.3.0
    ```
   并可以在 Gerrit 的插件页面中看到 chatgpt-code-review-gerrit-plugin 插件的状态为 Enabled。

## 配置参数

要配置这些参数，你需要修改你的 Gerrit 配置文件（`gerrit.config`）。文件格式如下：

```
[plugin "chatgpt-code-review-gerrit-plugin"]
    # 必填参数
    gptToken = {gptToken}
    gerritAuthBaseUrl = {gerritAuthBaseUrl}
    gerritUserName = {gerritUserName}
    gerritPassword = {gerritPassword}
   
    # 可选参数
    gptModel = {gptModel}
    gptPrompt = {gptPrompt}
    gptMaxTokens = {gptMaxTokens}
    patchSetReduction = {patchSetReduction}
    maxReviewLines = {maxReviewLines}
```

### 必填参数

- `gptToken`：OpenAI GPT token。
- `gerritAuthBaseUrl`：Gerrit 实例的 URL。类似于：https://gerrit.local.team/a
- `gerritUserName`：Gerrit 用户名。
- `gerritPassword`：Gerrit 密码。

### 可选参数

- `gptModel`：默认模型是 gpt-3.5-turbo。你也可以配置成 gpt-4 或 gpt-4-32k。
- `gptPrompt`：默认提示是 "Act as a Code Review Helper, please review this patch set:"。你可以修改成自己喜欢的 prompt。
- `gptMaxTokens`：默认值是 4096 tokens。这决定了 ChatGPT 最大对话长度。[点击这里](https://platform.openai.com/tokenizer)
  检查内容 token 数量。
- `patchSetReduction`：默认值是 false。如果设置为 true，插件会尝试压缩patch内容，包括但不限于多余的空行、制表符、import语句等，以便减少
  token 数量等。
- `maxReviewLines`：默认值是 1000。这设置了审查中包含的代码行数限制。

## 测试

- 你可以运行项目中的单元测试来熟悉插件的项目源码。
- 如果你想单独测试 Gerrit API 或者 ChatGPT API，你可以参考 CodeReviewPluginIT 中的测试用例。

## License

Apache License 2.0