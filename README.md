# ChatGPT Code Review Gerrit Plugin

[中文版本](README.zh.md)

This plugin allows you to use ChatGPT for code review in Gerrit conveniently. After submitting a patch, OpenAI will
provide review feedback in the form of comments.

## Getting Started

1. **Build:** Requires JDK11 or higher, Maven 3.0 or higher.

   ```bash
   mvn -U clean package
    ```

2. **Install:** Upload the compiled jar file to the $gerrit_site/plugins directory, then refer to [configuration
   parameters](#configuration-parameters) for settings, and restart Gerrit.

3. **Verify:** After installing the plugin, you can see the following information in Gerrit's logs:

   ```bash
   INFO com.google.gerrit.server.plugins.PluginLoader : Loaded plugin chatgpt-code-review-gerrit-plugin, version 3.3.0
   ```

   You can also check the status of the chatgpt-code-review-gerrit-plugin on Gerrit's plugin page as Enabled.

## Configuration Parameters

To configure these parameters, you need to modify your Gerrit configuration file (`gerrit.config`). The file format is
as follows:

```
[plugin "chatgpt-code-review-gerrit-plugin"]

    # Required parameters
    gptToken = {gptToken}
    gerritAuthBaseUrl = {gerritAuthBaseUrl}
    gerritUserName = {gerritUserName}
    gerritPassword = {gerritPassword}

    # Optional parameters
    gptModel = {gptModel}
    gptPrompt = {gptPrompt}
    gptMaxTokens = {gptMaxTokens}
    patchSetReduction = {patchSetReduction}
    maxReviewLines = {maxReviewLines}
```

### Required Parameters

- `gptToken`: OpenAI GPT token.
- `gerritAuthBaseUrl`: The URL of your Gerrit instance. Similar to: https://gerrit.local.team/a
- `gerritUserName`: Gerrit username.
- `gerritPassword`: Gerrit password.

### Optional Parameters

- `gptModel`: The default model is gpt-3.5-turbo. You can also configure it to gpt-4 or gpt-4-32k.
- `gptPrompt`: The default prompt is "Act as a Code Review Helper, please review this patch set:". You can modify it to
  your preferred prompt.
- `gptMaxTokens`: The default value is 4096 tokens. This determines the maximum dialogue length of ChatGPT. [Click
  here](https://platform.openai.com/tokenizer) to check the content token count.
- `patchSetReduction`: The default value is false. If set to true, the plugin will attempt to reduce patch content by
  compressing redundant blank lines, tabs, import statements, etc., in order to decrease the token count.
- `maxReviewLines`: The default value is 1000. This sets a limit on the number of lines of code included in the review.

## Testing

- You can run the unit tests in the project to familiarize yourself with the plugin's source code.
- If you want to individually test the Gerrit API or the ChatGPT API, you can refer to the test cases in
  CodeReviewPluginIT.

## License

Apache License 2.0