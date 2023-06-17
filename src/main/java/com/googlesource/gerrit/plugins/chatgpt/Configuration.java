package com.googlesource.gerrit.plugins.chatgpt;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;

@Singleton
@Getter
public class Configuration {

    public static final String OPENAI_DOMAIN = "https://api.openai.com";
    public static final String DEFAULT_GPT_MODEL = "gpt-3.5-turbo";
    public static final String DEFAULT_GPT_PROMPT = "Act as a Code Review Helper, please review this patch set: ";
    public static final String NOT_CONFIGURED_ERROR_MSG = "%s is not configured";

    private final PluginConfig cfg;

    private final String gptDomain;
    private final String gptToken;
    private final String gptModel;
    private final String gptPrompt;
    private final int gptMaxTokens;

    private final String gerritAuthBaseUrl;
    private final String gerritUserName;
    private final String gerritPassword;

    private final boolean patchSetReduction;
    private final int maxReviewLines;

    @Inject
    Configuration(PluginConfigFactory cfgFactory, @PluginName String pluginName) {
        cfg = cfgFactory.getFromGerritConfig(pluginName);
        gptDomain = cfg.getString("gptUrl", OPENAI_DOMAIN);
        gptToken = getValidatedOrThrow("gptToken");
        gptModel = cfg.getString("gptModel", DEFAULT_GPT_MODEL);
        gptPrompt = cfg.getString("gptPrompt", DEFAULT_GPT_PROMPT);
        gptMaxTokens = cfg.getInt("gptMaxTokens", 4096);
        gerritAuthBaseUrl = getValidatedOrThrow("gerritAuthBaseUrl");
        gerritUserName = getValidatedOrThrow("gerritUserName");
        gerritPassword = getValidatedOrThrow("gerritPassword");
        patchSetReduction = cfg.getBoolean("patchSetReduction", false);
        maxReviewLines = cfg.getInt("maxReviewLines", 1000);
    }

    private String getValidatedOrThrow(String key) {
        String value = cfg.getString(key);
        if (value == null) {
            throw new RuntimeException(String.format(NOT_CONFIGURED_ERROR_MSG, key));
        }
        return value;
    }
}

