package com.ecwid.consul;

import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Request;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ConsulRawClientTest {

    private static final String ENDPOINT = "/any/endpoint";
    private static final QueryParams EMPTY_QUERY_PARAMS = QueryParams.Builder.builder().build();
    private static final String HOST = "host";
    private static final int PORT = 8888;
    private static final String PATH = "path";
    private static final String EXPECTED_AGENT_ADDRESS_NO_PATH = "http://" + HOST + ":" + PORT + ENDPOINT;
    private static final String EXPECTED_AGENT_ADDRESS = "http://" + HOST + ":" + PORT + "/" + PATH + ENDPOINT;

    private final ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
    private final HttpClient httpClient = mock(HttpClient.class);
    ;
    private ConsulRawClient client;

    private static final SingleUrlParameters TOKEN_PARAM = new SingleUrlParameters("token", "CONFIDENTIAL");
    List<UrlParameters> TOKEN_PARAMS = List.of(TOKEN_PARAM);


    @BeforeEach
    void setup() {
        // Given
        client = ConsulRawClient.Builder.builder()
            .setHttpClient(httpClient)
            .setHost(HOST)
            .setPort(PORT)
            .setPath(PATH)
            .build();
    }

    @Test
    public void verifyDefaultUrl() throws Exception {
        // Given
        client = ConsulRawClient.Builder.builder()
            .setHttpClient(httpClient)
            .setHost(HOST)
            .setPort(PORT)
            .build();

        // When
        client.makeGetRequest(ENDPOINT, EMPTY_QUERY_PARAMS);

        // Then
        ArgumentCaptor<HttpUriRequest> calledUri = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient).execute(calledUri.capture(), any(ResponseHandler.class));
        assertEquals(EXPECTED_AGENT_ADDRESS_NO_PATH, calledUri.getValue().getURI().toString());
    }

    @Test
    public void verifyUrlWithPath() throws Exception {
        // Given done by setup()
        // When
        client.makeGetRequest(ENDPOINT, EMPTY_QUERY_PARAMS);

        // Then
        ArgumentCaptor<HttpUriRequest> calledUri = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient).execute(calledUri.capture(), any(ResponseHandler.class));
        assertEquals(EXPECTED_AGENT_ADDRESS, calledUri.getValue().getURI().toString());
    }

    @Disabled("not implemented yet")
    @Test
    public void verifyNoTokenInGets() throws Exception {

        client.makeGetRequest(ENDPOINT, TOKEN_PARAM);
        verify(httpClient).execute(captor.capture(), any(ResponseHandler.class));
        checkTokenExtraction();

        Mockito.reset(httpClient);

        client.makeGetRequest(ENDPOINT, TOKEN_PARAMS);
        verify(httpClient).execute(captor.capture(), any(ResponseHandler.class));
        checkTokenExtraction();

        Mockito.reset(httpClient);

        client.makeGetRequest(Request.Builder.newBuilder().setEndpoint(ENDPOINT).setToken("CONFIDENTIAL").build());
        verify(httpClient).execute(captor.capture(), any(ResponseHandler.class));
        checkTokenExtraction();

        Mockito.reset(httpClient);

        client.makeGetRequest(Request.Builder.newBuilder().setEndpoint(ENDPOINT).addUrlParameters(TOKEN_PARAMS).build());
        verify(httpClient).execute(captor.capture(), any(ResponseHandler.class));
        checkTokenExtraction();
    }

    @Disabled("not implemented yet")
    @Test
    public void verifyNoTokenInPuts() throws Exception {

        client.makePutRequest(ENDPOINT, "content", TOKEN_PARAM);
        verify(httpClient).execute(captor.capture(), any(ResponseHandler.class));
        checkTokenExtraction();

        Mockito.reset(httpClient);

        client.makePutRequest(Request.Builder.newBuilder().setEndpoint(ENDPOINT).setContent("content").setToken("CONFIDENTIAL").build());
        verify(httpClient).execute(captor.capture(), any(ResponseHandler.class));
        checkTokenExtraction();

        Mockito.reset(httpClient);

        client.makePutRequest(Request.Builder.newBuilder().setEndpoint(ENDPOINT).setContent("content").addUrlParameters(TOKEN_PARAMS).build());
        verify(httpClient).execute(captor.capture(), any(ResponseHandler.class));
        checkTokenExtraction();
    }

    @Disabled("not implemented yet")
    @Test
    public void verifyNoTokenInDeletes() throws Exception {

        client.makeDeleteRequest(Request.Builder.newBuilder().setEndpoint(ENDPOINT).setContent("content").setToken("CONFIDENTIAL").build());
        verify(httpClient).execute(captor.capture(), any(ResponseHandler.class));
        checkTokenExtraction();

        Mockito.reset(httpClient);

        client.makeDeleteRequest(Request.Builder.newBuilder().setEndpoint(ENDPOINT).setContent("content").addUrlParameters(TOKEN_PARAMS).build());
        verify(httpClient).execute(captor.capture(), any(ResponseHandler.class));
        checkTokenExtraction();
    }

    private void checkTokenExtraction() {
        String targetUri = captor.getValue().getURI().toString();

        assertThat(targetUri).isEqualTo(EXPECTED_AGENT_ADDRESS)
            .doesNotContain("token", "CONFIDENTIAL");
        Header[] headers = captor.getValue().getAllHeaders();
        assertThat(headers).hasSize(1);
        assertThat(headers[0].getName()).isEqualTo("X-Consul-Token");
        assertThat(headers[0].getValue()).isEqualTo("CONFIDENTIAL");
    }

}
