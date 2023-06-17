package com.googlesource.gerrit.plugins.chatgpt;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.net.HttpHeaders;
import com.googlesource.gerrit.plugins.chatgpt.client.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.client.OpenAiClient;
import com.googlesource.gerrit.plugins.chatgpt.client.UriResourceLocator;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class PatchSetReviewerTests {

    private static final String CHANGE_ID = "myChangeId";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9527);

    private Configuration configuration;

    private GerritClient gerritClient;

    private OpenAiClient openAiClient;

    private PatchSetReviewer patchSetReviewer;

    private PatchSetCreated patchSetCreated;

    @Before
    public void before() {
        initializePatchSetReviewer();
        setupMockRequests();
    }

    private void initializePatchSetReviewer() {
        configuration = Mockito.mock(Configuration.class);
        when(configuration.getGerritAuthBaseUrl()).thenReturn("http://localhost:9527");
        when(configuration.getGptDomain()).thenReturn("http://localhost:9527");
        when(configuration.getMaxReviewLines()).thenReturn(500);

        gerritClient = Mockito.spy(new GerritClient() {
            @Override
            public Configuration getConfiguration() {
                return configuration;
            }
        });

        openAiClient = Mockito.spy(new OpenAiClient() {
            @Override
            public Configuration getConfiguration() {
                return configuration;
            }
        });

        patchSetReviewer = new PatchSetReviewer(configuration, gerritClient, openAiClient);
        patchSetCreated = new PatchSetCreated(patchSetReviewer);
    }

    private void setupMockRequests() {
        // Mocks the behavior of the getPatchSet request
        stubFor(get(urlEqualTo(URI.create(configuration.getGerritAuthBaseUrl() +
                UriResourceLocator.gerritPatchSetUri(CHANGE_ID)).getPath()))
                .willReturn(aResponse()
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
        stubFor(post(urlEqualTo(URI.create(configuration.getGptDomain()
                + UriResourceLocator.chatCompletionsUri()).getPath()))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody(new String(gptAnswer))));

        // Mocks the behavior of the postReview request
        stubFor(post(urlEqualTo(URI.create(configuration.getGerritAuthBaseUrl()
                + UriResourceLocator.gerritCommentUri(CHANGE_ID)).getPath()))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)));
    }

    @Test
    public void review() throws IOException, InterruptedException {
        patchSetReviewer.review(CHANGE_ID);

        LoggedRequest firstMatchingRequest = findAll(postRequestedFor(urlEqualTo(UriResourceLocator
                .gerritCommentUri(CHANGE_ID)))).get(0);
        String requestBody = firstMatchingRequest.getBodyAsString();
        assertEquals("{\"message\":\"Hello!\\n\"}", requestBody);

    }

}
