package com.googlesource.gerrit.plugins.chatgpt;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.net.HttpHeaders;
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.googlesource.gerrit.plugins.chatgpt.client.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.client.OpenAiClient;
import com.googlesource.gerrit.plugins.chatgpt.client.UriResourceLocator;
import com.googlesource.gerrit.plugins.chatgpt.config.ConfigCreator;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.listener.EventListenerHandler;
import com.googlesource.gerrit.plugins.chatgpt.listener.GptMentionedCommentListener;
import com.googlesource.gerrit.plugins.chatgpt.listener.PatchSetCreatedListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.googlesource.gerrit.plugins.chatgpt.client.UriResourceLocator.gerritCommentUri;
import static com.googlesource.gerrit.plugins.chatgpt.client.UriResourceLocator.gerritPatchSetUri;
import static com.googlesource.gerrit.plugins.chatgpt.listener.EventListenerHandler.buildFullChangeId;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class ChatGptReviewTests {
    private static final Project.NameKey PROJECT_NAME = Project.NameKey.parse("myProject");
    private static final Change.Key CHANGE_ID = Change.Key.parse("myChangeId");
    private static final BranchNameKey BRANCH_NAME = BranchNameKey.create(PROJECT_NAME, "myBranchName");

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9527);

    private Configuration config;

    @Before
    public void before() {
        initConfig();
        setupMockRequests();
    }

    private void initConfig() {
        config = mock(Configuration.class);
        when(config.getGerritAuthBaseUrl()).thenReturn("http://localhost:9527");
        when(config.getGptDomain()).thenReturn("http://localhost:9527");
        when(config.getGptTemperature()).thenReturn(1.0);
        when(config.getMaxReviewLines()).thenReturn(500);
        when(config.getEnabledProjects()).thenReturn("");
        when(config.isProjectEnable()).thenReturn(true);
    }

    private void setupMockRequests() {
        // Mocks the behavior of the getPatchSet request
        WireMock.stubFor(WireMock.get(gerritPatchSetUri(buildFullChangeId(PROJECT_NAME, BRANCH_NAME, CHANGE_ID)))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody(Base64.getEncoder().encodeToString("myPatch".getBytes()))));

        // Mocks the behavior of the askGpt request
        byte[] gptAnswer = Base64.getDecoder().decode("ZGF0YTogeyJpZCI6ImNoYXRjbXBsLTdSZDVOYVpEOGJNVTRkdnBVV2" +
                "9hM3Q2RG83RkkzIiwib2JqZWN0IjoiY2hhdC5jb21wbGV0aW9uLmNodW5rIiwiY3JlYXRlZCI6MTY4NjgxOTQ1NywibW9kZWw" +
                "iOiJncHQtMy41LXR1cmJvLTAzMDEiLCJjaG9pY2VzIjpbeyJkZWx0YSI6eyJyb2xlIjoiYXNzaXN0YW50In0sImluZGV4Ijow" +
                "LCJmaW5pc2hfcmVhc29uIjpudWxsfV19CgpkYXRhOiB7ImlkIjoiY2hhdGNtcGwtN1JkNU5hWkQ4Yk1VNGR2cFVXb2EzdDZEb" +
                "zdGSTMiLCJvYmplY3QiOiJjaGF0LmNvbXBsZXRpb24uY2h1bmsiLCJjcmVhdGVkIjoxNjg2ODE5NDU3LCJtb2RlbCI6ImdwdC0" +
                "zLjUtdHVyYm8tMDMwMSIsImNob2ljZXMiOlt7ImRlbHRhIjp7ImNvbnRlbnQiOiJIZWxsbyJ9LCJpbmRleCI6MCwiZmluaXNo" +
                "X3JlYXNvbiI6bnVsbH1dfQoKZGF0YTogeyJpZCI6ImNoYXRjbXBsLTdSZDVOYVpEOGJNVTRkdnBVV29hM3Q2RG83RkkzIiwib" +
                "2JqZWN0IjoiY2hhdC5jb21wbGV0aW9uLmNodW5rIiwiY3JlYXRlZCI6MTY4NjgxOTQ1NywibW9kZWwiOiJncHQtMy41LXR1cm" +
                "JvLTAzMDEiLCJjaG9pY2VzIjpbeyJkZWx0YSI6eyJjb250ZW50IjoiISJ9LCJpbmRleCI6MCwiZmluaXNoX3JlYXNvbiI" +
                "6bnVsbH1dfQ==");
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocator.chatCompletionsUri()).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody(new String(gptAnswer))));

        // Mocks the behavior of the postReview request
        WireMock.stubFor(WireMock.post(gerritCommentUri(buildFullChangeId(PROJECT_NAME, BRANCH_NAME, CHANGE_ID)))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)));
    }

    @Test
    public void patchSetCreatedOrUpdated() throws InterruptedException, NoSuchProjectException, ExecutionException {
        GerritClient gerritClient = new GerritClient();
        OpenAiClient openAiClient = new OpenAiClient();
        PatchSetReviewer patchSetReviewer = new PatchSetReviewer(gerritClient, openAiClient);
        ConfigCreator mockConfigCreator = mock(ConfigCreator.class);
        when(mockConfigCreator.createConfig(ArgumentMatchers.any())).thenReturn(config);

        PatchSetCreatedEvent event = mock(PatchSetCreatedEvent.class);
        when(event.getProjectNameKey()).thenReturn(PROJECT_NAME);
        when(event.getBranchNameKey()).thenReturn(BRANCH_NAME);
        when(event.getChangeKey()).thenReturn(CHANGE_ID);
        EventListenerHandler eventListenerHandler = new EventListenerHandler(patchSetReviewer);

        PatchSetCreatedListener patchSetCreatedListener = new PatchSetCreatedListener(mockConfigCreator, eventListenerHandler);
        patchSetCreatedListener.onEvent(event);
        CompletableFuture<Void> future = eventListenerHandler.getLatestFuture();
        future.get();

        RequestPatternBuilder requestPatternBuilder = WireMock.postRequestedFor(
                WireMock.urlEqualTo(gerritCommentUri(buildFullChangeId(PROJECT_NAME, BRANCH_NAME, CHANGE_ID))));
        List<LoggedRequest> loggedRequests = WireMock.findAll(requestPatternBuilder);
        Assert.assertEquals(1, loggedRequests.size());
        String requestBody = loggedRequests.get(0).getBodyAsString();
        Assert.assertEquals("{\"message\":\"Hello!\\n\"}", requestBody);

    }

    @Test
    public void gptMentionedInComment() throws InterruptedException, NoSuchProjectException, ExecutionException {
        GerritClient gerritClient = new GerritClient();
        OpenAiClient openAiClient = new OpenAiClient();
        PatchSetReviewer patchSetReviewer = new PatchSetReviewer(gerritClient, openAiClient);
        ConfigCreator mockConfigCreator = mock(ConfigCreator.class);
        when(config.getGerritUserName()).thenReturn("gpt");
        when(mockConfigCreator.createConfig(ArgumentMatchers.any())).thenReturn(config);

        CommentAddedEvent event = mock(CommentAddedEvent.class);
        when(event.getProjectNameKey()).thenReturn(PROJECT_NAME);
        when(event.getBranchNameKey()).thenReturn(BRANCH_NAME);
        when(event.getChangeKey()).thenReturn(CHANGE_ID);
        EventListenerHandler eventListenerHandler = new EventListenerHandler(patchSetReviewer);

        GptMentionedCommentListener gptMentionedCommentListener = new GptMentionedCommentListener(mockConfigCreator, eventListenerHandler);
        event.comment = "@gpt Hello!";
        gptMentionedCommentListener.onEvent(event);
        CompletableFuture<Void> future = eventListenerHandler.getLatestFuture();
        future.get();

        RequestPatternBuilder requestPatternBuilder = WireMock.postRequestedFor(
                WireMock.urlEqualTo(gerritCommentUri(buildFullChangeId(PROJECT_NAME, BRANCH_NAME, CHANGE_ID))));
        List<LoggedRequest> loggedRequests = WireMock.findAll(requestPatternBuilder);
        Assert.assertEquals(1, loggedRequests.size());
        String requestBody = loggedRequests.get(0).getBodyAsString();
        Assert.assertEquals("{\"message\":\"Hello!\\n\"}", requestBody);

    }

}
