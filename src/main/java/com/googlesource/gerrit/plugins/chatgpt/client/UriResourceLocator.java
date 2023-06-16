package com.googlesource.gerrit.plugins.chatgpt.client;

public class UriResourceLocator {

    private UriResourceLocator() {
        throw new IllegalStateException("Utility class");
    }

    public static String gerritPatchSetUri(String changeId) {
        return "/changes/" + changeId + "/revisions/current/patch";
    }

    public static String gerritCommentUri(String changeId) {
        return "/changes/" + changeId + "/revisions/current/review";
    }

    public static String chatCompletionsUri() {
        return "/v1/chat/completions";
    }

}
