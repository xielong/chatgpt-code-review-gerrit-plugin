package com.googlesource.gerrit.plugins.chatgpt.integration;

import com.googlesource.gerrit.plugins.chatgpt.client.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.client.OpenAiClient;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Mockito.when;

@Ignore("This test suite is designed to demonstrate how to test the Gerrit and GPT interfaces in a real environment. " +
        "It is not intended to be executed during the regular build process")
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class CodeReviewPluginIT {
    @Mock
    private Configuration config;

    @InjectMocks
    private GerritClient gerritClient;

    @InjectMocks
    private OpenAiClient openAiClient;

    @Test
    public void sayHelloToGPT() throws Exception {
        when(config.getGptDomain()).thenReturn(Configuration.OPENAI_DOMAIN);
        when(config.getGptToken()).thenReturn("Your GPT token");
        when(config.getGptModel()).thenReturn(Configuration.DEFAULT_GPT_MODEL);
        when(config.getGptPrompt()).thenReturn(Configuration.DEFAULT_GPT_PROMPT);

        String answer = openAiClient.ask(config, "hello");
        log.info("answer: {}", answer);
        assertNotNull(answer);
    }

    @Test
    public void getPatchSet() throws Exception {
        when(config.getGerritAuthBaseUrl()).thenReturn("Your Gerrit URL");
        when(config.getGerritUserName()).thenReturn("Your Gerrit username");
        when(config.getGerritPassword()).thenReturn("Your Gerrit password");

        String patchSet = gerritClient.getPatchSet(config, "${changeId}");
        log.info("patchSet: {}", patchSet);
        assertNotNull(patchSet);
    }

    @Test
    public void postComment() throws Exception {
        when(config.getGerritAuthBaseUrl()).thenReturn("Your Gerrit URL");
        when(config.getGerritUserName()).thenReturn("Your Gerrit username");
        when(config.getGerritPassword()).thenReturn("Your Gerrit password");

        gerritClient.postComment(config, "Your changeId", "message");
    }
}
