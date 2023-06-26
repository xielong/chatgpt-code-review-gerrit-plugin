package com.googlesource.gerrit.plugins.chatgpt.client;

import com.github.rholder.retry.*;
import com.google.inject.Singleton;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.net.HttpURLConnection.HTTP_OK;

@Singleton
public class HttpClientWithRetry {
    private final Retryer<HttpResponse<String>> retryer;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMinutes(5))
            .build();

    public HttpClientWithRetry() {
        this.retryer = RetryerBuilder.<HttpResponse<String>>newBuilder()
                .retryIfException()
                .retryIfResult(response -> response.statusCode() != HTTP_OK)
                .withWaitStrategy(WaitStrategies.fixedWait(10, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .build();
    }

    public HttpResponse<String> execute(HttpRequest request) throws ExecutionException, RetryException {
        return retryer.call(() -> httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
    }

}

