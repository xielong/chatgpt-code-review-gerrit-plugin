package com.googlesource.gerrit.plugins.chatgpt.client;

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static java.net.HttpURLConnection.HTTP_OK;

@Slf4j
@Singleton
public class OpenAiClient {
    private final Gson gson = new Gson();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMinutes(5))
            .build();

    @Inject
    private Configuration configuration;

    public String ask(String patchSet) throws IOException, InterruptedException {
        HttpRequest request = createRequest(getConfiguration().getGptModel(),
                getConfiguration().getGptPrompt(), patchSet);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != HTTP_OK) {
            throw new IOException("Unexpected response " + response);
        }

        String body = response.body();
        if (body == null) {
            throw new IOException("responseBody is null");
        }

        StringBuilder finalContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
            String line;
            while ((line = reader.readLine()) != null) {
                extractContentFromLine(line).ifPresent(finalContent::append);
            }
        }

        return finalContent.toString();
    }

    private HttpRequest createRequest(String model, String prompt, String patchSet) {
        String jsonRequest = createJsonRequest(model, prompt, patchSet);

        return HttpRequest.newBuilder()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getConfiguration().getGptToken())
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                .uri(URI.create(URI.create(Configuration.OPENAI_DOMAIN) + UriResourceLocator.chatCompletionsUri()))
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();
    }

    private String createJsonRequest(String model, String prompt, String patchSet) {
        ChatCompletionRequest.Message systemMessage = ChatCompletionRequest.Message.builder()
                .role("system")
                .content(prompt)
                .build();
        ChatCompletionRequest.Message userMessage = ChatCompletionRequest.Message.builder()
                .role("user")
                .content(patchSet)
                .build();

        List<ChatCompletionRequest.Message> messages = List.of(systemMessage, userMessage);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .stream(true)
                .build();

        return gson.toJson(chatCompletionRequest);
    }

    private Optional<String> extractContentFromLine(String line) {
        String dataPrefix = "data: {\"id\"";

        if (!line.startsWith(dataPrefix)) {
            return Optional.empty();
        }
        ChatCompletionResponse chatCompletionResponse =
                gson.fromJson(line.substring("data: ".length()), ChatCompletionResponse.class);
        String content = chatCompletionResponse.getChoices().get(0).getDelta().getContent();
        return Optional.ofNullable(content);
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

}

