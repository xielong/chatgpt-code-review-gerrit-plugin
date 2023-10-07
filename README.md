# ChatGPT Code Review Gerrit Plugin

[中文版本](README.zh.md)

## Features

1. This plugin allows you to use ChatGPT for code review in Gerrit conveniently. After submitting a patchSet, OpenAI
   will provide review feedback in the form of comments.
2. You can continue to ask ChatGPT by @{gerritUserName} in the comments to further guide it in generating more
   targeted review comments.

## Getting Started

1. **Build:** Requires JDK 11 or higher, Maven 3.0 or higher.

   ```bash
   mvn -U clean package
    ```

   Alternatively, you can also download the pre-built packages directly from the 'Releases' page on our GitHub
   repository.
   On this page, you will find the released versions and can download the corresponding JAR files. Make sure to select
   the
   appropriate JAR file that corresponds to the desired version.

2. **Install:** Upload the compiled jar file to the `$gerrit_site/plugins` directory.

3. **Configure:** Set up the basic parameters in your `$gerrit_site/etc/gerrit.config`:
- `gptToken`: OpenAI GPT token.
- `gerritAuthBaseUrl`: The URL of your Gerrit instance. Similar to: https://gerrit.local.team/a
- `gerritUserName`: Gerrit username.
- `gerritPassword`: Gerrit password.
- `globalEnable`: Default value is false. The plugin will only review specified repositories. If set to true, the plugin
  will by default review all pull requests.

  For enhanced security, consider storing sensitive information like gptToken and gerritPassword in a secure location or file. Detailed instructions on how to do this will be provided later in this document.

4. **Verify:** After restarting Gerrit, you can see the following information in Gerrit's logs:

   ```bash
   INFO com.google.gerrit.server.plugins.PluginLoader : Loaded plugin chatgpt-code-review-gerrit-plugin, version 1.0.0
   ```

   You can also check the status of the chatgpt-code-review-gerrit-plugin on Gerrit's plugin page as Enabled.

## Code Review Example

Consider the following code snippet:

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

Upon review, here are some suggestions and potential issues for the given patch:

- **Inconsistent handling of negative numbers:** According to the code, the function only reverts the sign of the output if the last char of the string is a "-". This is not a conventional way to define negative numbers in strings. Normally, the "-" sign should be at the start of the string.
- **Error handling of non-number characters:** The function lacks handling of non-numeric characters comprehensively. It takes anything that isn't a space and calculates its ASCII value, which could yield undesired results if any unwanted character apart from numbers and spaces are introduced.
- **Naming variables:** It could be good practice to use more descriptive variable names instead of x, y, z, and w.
- **Handling of trailing spaces:** The function is currently continuing to the next iteration whenever it encounters a space. This would mean it won't handle trailing spaces well, because it won't realize it's reached the end of the valid part of the string.
- **Unused Variable:** Variable 'y' is defined but never used.
- **Always returns negative value:** This could be a logical error. Based on typical use cases, the conversion of a string to an integer would not always yield a negative result.

Overall, it's recommended that the exception handling for the string to int conversion could be improved and the code for negative numbers should be fixed.

### Optimize the code

```
@gpt Please review and optimize this code. I'd like to continue manually parsing the string
```

The existing code converts a string representing a number into its integer form. Nevertheless, it does not take into account certain circumstances such as input strings that contain characters aside from numbers, whitespaces, or a '-' at the last place. A '-' at the last place is not standard for indicating negative numbers; it should be at the first place. Furthermore, in its current state, the code always returns a negative number. Here is a revision of the code that optimizes logic and improves error handling.


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

The above optimization validates that the string only contains valid characters. It throws an IllegalArgumentException in cases where the String is null, a string without characters, or a string containing non-digit characters. The exception in the event that it starts with '-' (unlike the original code, where it only checks if it ends with '-'), the 'inputString' is trimmed prior to processing to eliminate leading and trailing whitespaces.


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

#### Secure Configuration

It is highly recommended to store sensitive information such as `gptToken` and `gerritPassword` in the `secure.config`
file. Please edit the file at $gerrit_site/etc/`secure.config` and include the following details:

```
[plugin "chatgpt-code-review-gerrit-plugin"]
    gptToken = {gptToken}
    gerritPassword = {gerritPassword}
```

If you wish to encrypt the information within the `secure.config` file, you can refer
to: https://gerrit.googlesource.com/plugins/secure-config

### Project Configuration

To add the following content, please edit the `project.config` file in `refs/meta/config`:

```
[plugin "chatgpt-code-review-gerrit-plugin"]
    # Required parameters
    gerritUserName = {gerritUserName}
    gerritAuthBaseUrl = {gerritAuthBaseUrl}
    ...

    # Optional parameters
    gptModel = {gptModel}
    gptPrompt = {gptPrompt}
    ...
```

#### Secure Configuration

Please ensure **strict control over the access permissions of `refs/meta/config`** since sensitive information such as
`gptToken` and `gerritPassword` is configured in the `project.config` file within `refs/meta/config`.

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