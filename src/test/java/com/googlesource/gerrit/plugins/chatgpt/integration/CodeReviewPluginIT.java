package com.googlesource.gerrit.plugins.chatgpt.integration;

import com.googlesource.gerrit.plugins.chatgpt.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.client.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.client.OpenAiClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

//@Ignore("This test suite is designed for integration testing and is not intended to be executed during the regular build process")
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class CodeReviewPluginIT {
    @Mock
    private Configuration configuration;

    @InjectMocks
    private GerritClient gerritClient;

    @InjectMocks
    private OpenAiClient openAiClient;

    @Test
    public void sayHelloToGPT() throws IOException, InterruptedException {
        when(configuration.getGptDomain()).thenReturn(Configuration.OPENAI_DOMAIN);
        when(configuration.getGptToken()).thenReturn("Your GPT token");
        when(configuration.getGptModel()).thenReturn(Configuration.DEFAULT_GPT_MODEL);
        when(configuration.getGptPrompt()).thenReturn(Configuration.DEFAULT_GPT_PROMPT);

        String answer = openAiClient.ask("hello");
        log.info("answer: {}", answer);
        assertNotNull(answer);
    }

    @Test
    public void getPatchSet() throws IOException, InterruptedException {
        when(configuration.getGerritAuthBaseUrl()).thenReturn("Your Gerrit URL");
        when(configuration.getGerritUserName()).thenReturn("Your Gerrit username");
        when(configuration.getGerritPassword()).thenReturn("Your Gerrit password");

        String patchSet = gerritClient.getPatchSet("${changeId}");
        log.info("patchSet: {}", patchSet);
        assertNotNull(patchSet);
    }

    @Test
    public void postComment() throws IOException, InterruptedException {
        when(configuration.getGerritAuthBaseUrl()).thenReturn("Your Gerrit URL");
        when(configuration.getGerritUserName()).thenReturn("Your Gerrit username");
        when(configuration.getGerritPassword()).thenReturn("Your Gerrit password");

        gerritClient.postComment("Your changeId", "message");
    }
}
