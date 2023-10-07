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

   此外，你还可以直接从我们的 GitHub 存储库的 'Releases' 页面下载预先构建的包。在该页面上，你将找到发布的版本，
   并可以下载相应的JAR文件。请确保选择与所需版本相对应的适当的JAR文件。

2. **安装：** 将编译好的 jar 文件上传到 $gerrit_site/plugins 目录中，然后参考 [配置参数](#配置参数) 进行配置，再重启
   Gerrit。

3. **配置：** 在 `$gerrit_site/etc/gerrit.config` 文件中进行基本配置：
- `gptToken`：OpenAI GPT token。
- `gerritAuthBaseUrl`：Gerrit 实例的 URL。类似于：https://gerrit.local.team/a。
- `gerritUserName`：Gerrit 用户名。
- `gerritPassword`：Gerrit 密码。
- `globalEnable`: 默认值为 false。插件将只会审查指定的仓库，如果设为 true，插件默认会审查所有 review 请求。

  为了增强安全性，建议将敏感信息，如 gptToken 和 gerritPassword，存储在一个安全的位置或文件中。本文档后续部分将提供详细的操作指南。

4. **确认：** 安装好插件后，你可以在 Gerrit 的日志中看到以下信息：

   ```bash
      INFO com.google.gerrit.server.plugins.PluginLoader : Loaded plugin chatgpt-code-review-gerrit-plugin, version 1.0.0
    ```
   并可以在 Gerrit 的插件页面中看到 chatgpt-code-review-gerrit-plugin 插件的状态为 Enabled。

## 代码审查示例

提交以下代码进行审查:

```java
public static int stringToInt(String bar) throws Exception {
    if (bar == null || bar.length() == 0) {
        throw new Exception("Invalid string input");
    }

    int x = 0;
    boolean y = false;
    int z = 0;

    if (bar.charAt(bar.length() - 1) == '-') {
        y = true;
    }

    for (int i = z; i < bar.length(); i++) {
        char w = bar.charAt(i);

        if (w == ' ') {
            continue;
        }

        x = x * 10 + (w - '0');
    }

    return -x;
}
```

经过审查，以下是对所提供补丁的建议和可能存在的问题：

**负数处理不当：**从代码中可以看出，只有当字符串的最后一个字符为"-"时，函数才会改变输出的正负号。但通常，"-"应该出现在字符串的开头来表示负数。
**非数字字符处理不足：**该函数未能有效识别非数字字符。它会计算除空格外的任何字符的ASCII值，这意味着如果输入了不应存在的字符，可能会得到意外的输出。
**变量命名不清晰：**建议使用更有描述性的变量名，而不是简单的x、y、z和w。
**尾部空格处理不当：**当前函数在遇到空格时会直接进入下一个循环，这导致它无法正确处理字符串末尾的空格。
**存在未使用的变量：**虽然定义了变量'y'，但代码中并未使用到。
**总是返回负值：**这似乎是一个逻辑错误。通常，将字符串转换为整数并不总是得到负数。
综上所述，建议加强字符串到整数的异常处理，并修正负数处理的逻辑。


### 优化代码

```
@@gpt 请 review 并优化此代码。我希望继续手动解析这个字符串。
```

当前代码可以将代表数字的字符串转为整数，但存在一些遗漏。例如，它未能处理字符串中除数字、空格或末尾的'-'以外的字符。通常，'-'应该出现在字符串开头来表示负数，而不是末尾。此外，该代码始终返回负数。以下是代码的优化版本，其中改进了逻辑和错误处理。

```java
public static int stringToInt(String inputString) throws IllegalArgumentException {
    if (inputString == null || inputString.trim().isEmpty()) {
        throw new IllegalArgumentException("Invalid string input");
    }

    inputString = inputString.trim();
    boolean isNegative = false;
    String numStr = inputString;

    if (inputString.charAt(0)=='-') {
        isNegative = true;
        numStr = inputString.substring(1);
    }

    int result = 0;

    for (int i = 0; i < numStr.length(); i++) {
        char c = numStr.charAt(i);

        if (!Character.isDigit(c)) {
            throw new IllegalArgumentException("Invalid string input: non-digit character encountered.");
        }

        result *= 10;
        result += c - '0';
    }

    return isNegative ? -result : result;
}

```

此次优化确保了字符串仅包含合法字符。当字符串为空、无任何字符或含有非数字字符时，会抛出 IllegalArgumentException 异常。不同于原始代码仅检查字符串是否以'-'结尾，现在如果字符串以'-'开头，处理前会先修剪'inputString'，去除其前后的空格。

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