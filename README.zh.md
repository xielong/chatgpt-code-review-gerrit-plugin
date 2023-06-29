# ChatGPT 代码审查 Gerrit 插件

[English Version](README.md)

## 功能

1. 你可以方便地在 Gerrit 中通过 ChatGPT 进行代码审查。在提交 patchSet 后， OpenAI 会以评论的方式提供审查意见。
2. 你可以在评论中 @{gerritUserName} 用户，继续向 ChatGPT 提问，进一步引导 ChatGPT 生成更有针对性的审查意见。

## 入门

1. **构建：** 需要 JDK 11 以上版本，Maven 3.0 以上版本。

   ```bash
   mvn -U clean package
   ```

2. **安装：** 将编译好的 jar 文件上传到 $gerrit_site/plugins 目录中，然后参考 [配置参数](#配置参数) 进行配置，再重启
   Gerrit。

3. **确认：** 安装好插件后，你可以在 Gerrit 的日志中看到以下信息：

   ```bash
      INFO com.google.gerrit.server.plugins.PluginLoader : Loaded plugin chatgpt-code-review-gerrit-plugin, version 1.0.0
    ```
   并可以在 Gerrit 的插件页面中看到 chatgpt-code-review-gerrit-plugin 插件的状态为 Enabled。

## 配置参数

你可以选择设定全局参数，或对特定项目进行独立配置。若进行独立配置，则相应的项目配置将覆盖全局参数。

### 全局配置

编辑 $gerrit_site/etc/`gerrit.config` 文件，添加以下内容：

```
[plugin "chatgpt-code-review-gerrit-plugin"]
    # 必填参数
    gerritUserName = {gerritUserName}
    gerritAuthBaseUrl = {gerritAuthBaseUrl}
    ...

    # 可选参数
    gptModel = {gptModel}
    gptPrompt = {gptPrompt}
    ...
```

#### 安全配置

强烈建议将 `gptToken` 和 `gerritPassword` 这两项敏感信息配置在 `secure.config` 文件中。请在
$gerrit_site/etc/`secure.config` 文件中进行编辑，并添加以下内容：

```
[plugin "chatgpt-code-review-gerrit-plugin"]
    gptToken = {gptToken}
    gerritPassword = {gerritPassword}
```

如果你希望加密 `secure.config` 文件中的信息，可以参考：https://gerrit.googlesource.com/plugins/secure-config

### 项目配置

编辑 `refs/meta/config` 的 `project.config` 文件，添加以下内容：

```
[plugin "chatgpt-code-review-gerrit-plugin"]
    # 必填参数
    gptToken = {gptToken}
    gerritAuthBaseUrl = {gerritAuthBaseUrl}
    ...

    # 可选参数
    gptModel = {gptModel}
    gptPrompt = {gptPrompt}
    ...
```

#### 安全配置

**请确保对 `refs/meta/config` 的访问权限被严格控制**，因为敏感信息（例如 `gptToken` 和 `gerritPassword`
）配置在 `project.config` 文件中。

### 必填参数

- `gptToken`：OpenAI GPT token。
- `gerritAuthBaseUrl`：Gerrit 实例的 URL。类似于：https://gerrit.local.team/a
- `gerritUserName`：Gerrit 用户名。
- `gerritPassword`：Gerrit 密码。
- `globalEnable`: 默认值为 false。插件将只会审查指定的仓库，如果设为 true，插件默认会审查所有 review 请求。

### 可选参数

- `gptModel`：默认模型是 gpt-3.5-turbo。你也可以配置成 gpt-3.5-turbo-16k、gpt-4 或 gpt-4-32k。
- `gptPrompt`：默认提示是 "Act as a Code Review Helper, please review this patch set:"。你可以修改成自己喜欢的 prompt。
- `gptTemperature`: 默认值为 1。范围在 0 到 2 之间。较高的值如 1.8 会使输出结果更具随机性，而较低的值如 0.2 则会让输出更加集中和确定性强。
- `patchSetReduction`：默认值是 false。如果设置为 true，插件会尝试压缩 patch 内容，包括但不限于多余的空行、制表符、import
  语句等，以便减少 token 数量等。
- `maxReviewLines`：默认值是 1000。这设置了审查中包含的代码行数限制。
- `enabledProjects（仅用于全局配置）`:
  默认值为空字符串。如果 globalEnable 被设为 false，插件将仅在这里指定的仓库中运行。值应为仓库名称的逗号分隔列表，例如："
  project1,project2,project3"。
- `isEnabled（仅用于项目配置）`: 默认值为 false。如果设为 true，插件将会 review 这个项目的 patchSet。

## 测试

- 你可以运行项目中的单元测试来熟悉插件的项目源码。
- 如果你想单独测试 Gerrit API 或者 ChatGPT API，你可以参考 CodeReviewPluginIT 中的测试用例。

## License

Apache License 2.0