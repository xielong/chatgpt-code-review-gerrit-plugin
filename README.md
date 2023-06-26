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
   INFO com.google.gerrit.server.plugins.PluginLoader : Loaded plugin chatgpt-code-review-gerrit-plugin, version 1.0.0
   ```

   You can also check the status of the chatgpt-code-review-gerrit-plugin on Gerrit's plugin page as Enabled.

## Configuration Parameters

You have the option to establish global settings, or independently configure specific projects. If you choose
independent configuration, the corresponding project settings will override the global parameters.

### Global Configuration

To configure these parameters, you need to modify your Gerrit configuration file (`gerrit.config`). The file format is
as follows:

```
[plugin "chatgpt-code-review-gerrit-plugin"]
    # Required parameters
    gptToken = {gptToken}
    gerritAuthBaseUrl = {gerritAuthBaseUrl}
    ...

    # Optional parameters
    gptModel = {gptModel}
    gptPrompt = {gptPrompt}
    ...
```

### Project Configuration

To add the following content, please edit the project.config file in refs/meta/config:

```
[plugin "chatgpt-code-review-gerrit-plugin"]
    # Required parameters
    gptToken = {gptToken}
    gerritAuthBaseUrl = {gerritAuthBaseUrl}
    ...

    # Optional parameters
    gptModel = {gptModel}
    gptPrompt = {gptPrompt}
    ...
```

### Required Parameters

- `gptToken`: OpenAI GPT token.
- `gerritAuthBaseUrl`: The URL of your Gerrit instance. Similar to: https://gerrit.local.team/a
- `gerritUserName`: Gerrit username.
- `gerritPassword`: Gerrit password.
- `globalEnable`: Default value is false. The plugin will only review specified repositories. If set to true, the plugin
  will by default review all pull requests.

### Optional Parameters

- `gptModel`: The default model is gpt-3.5-turbo. You can also configure it to gpt-3.5-turbo-16k, gpt-4 or gpt-4-32k.
- `gptPrompt`: The default prompt is "Act as a Code Review Helper, please review this patch set:". You can modify it to
  your preferred prompt.
- `gptTemperature`: The default value is 1. What sampling temperature to use, between 0 and 2. Higher values like 0.8
  will make the output more random, while lower values like 0.2 will make it more focused and deterministic.
- `patchSetReduction`: The default value is false. If set to true, the plugin will attempt to reduce patch content by
  compressing redundant blank lines, tabs, import statements, etc., in order to decrease the token count.
- `maxReviewLines`: The default value is 1000. This sets a limit on the number of lines of code included in the review.
- `enabledProjects (for global configuration only)`:
  The default value is an empty string. If globalEnable is set to false, the plugin will only run in the repositories
  specified here. The value should be a comma-separated list of repository names, for example: "
  project1,project2,project3".
- `isEnabled (for project configuration only)`: The default is false. If set to true, the plugin will review the
  patchSet of this project.

## Testing

- You can run the unit tests in the project to familiarize yourself with the plugin's source code.
- If you want to individually test the Gerrit API or the ChatGPT API, you can refer to the test cases in
  CodeReviewPluginIT.

## License

Apache License 2.0