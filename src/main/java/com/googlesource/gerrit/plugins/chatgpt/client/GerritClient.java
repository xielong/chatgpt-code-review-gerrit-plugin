package com.googlesource.gerrit.plugins.chatgpt.client;

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_OK;

@Slf4j
@Singleton
public class GerritClient {
    private final Gson gson = new Gson();
    private final HttpClientWithRetry httpClientWithRetry = new HttpClientWithRetry();

    public String getPatchSet(Configuration config, String fullChangeId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .header(HttpHeaders.AUTHORIZATION, generateBasicAuth(config.getGerritUserName(),
                        config.getGerritPassword()))
                .uri(URI.create(config.getGerritAuthBaseUrl()
                        + UriResourceLocator.gerritPatchSetUri(fullChangeId)))
                .build();

        HttpResponse<String> response = httpClientWithRetry.execute(request);

        if (response.statusCode() != HTTP_OK) {
            log.error("Failed to get patch. Response: {}", response);
            throw new IOException("Failed to get patch from Gerrit");
        }

        String responseBody = response.body();
        log.info("Successfully obtained patch. Decoding response body.");
        return new String(Base64.getDecoder().decode(responseBody));
    }

    private String generateBasicAuth(String username, String password) {
        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }

    public void postComment(Configuration config, String fullChangeId, String message) throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("message", message);
        String json = gson.toJson(map);

        HttpRequest request = HttpRequest.newBuilder()
                .header(HttpHeaders.AUTHORIZATION, generateBasicAuth(config.getGerritUserName(),
                        config.getGerritPassword()))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                .uri(URI.create(config.getGerritAuthBaseUrl()
                        + UriResourceLocator.gerritCommentUri(fullChangeId)))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClientWithRetry.execute(request);

        if (response.statusCode() != HTTP_OK) {
            log.error("Review post failed with status code: {}", response.statusCode());
        }
    }

}
