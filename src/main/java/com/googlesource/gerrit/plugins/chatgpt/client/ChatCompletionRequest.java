package com.googlesource.gerrit.plugins.chatgpt.client;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatCompletionRequest {
    private String model;
    private boolean stream;
    private double temperature;
    private List<Message> messages;

    @Data
    @Builder
    public static class Message {
        private String role;
        private String content;
    }
}
