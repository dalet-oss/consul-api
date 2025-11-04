package com.ecwid.consul;

import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Request;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
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
    private static final SingleUrlParameters TOKEN_PARAM = new SingleUrlParameters("token", "CONFIDENTIAL");
    private static final List<UrlParameters> TOKEN_PARAMS = List.of(TOKEN_PARAM);

    private final ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
    private final HttpClient httpClient = mock(HttpClient.class);

    private ConsulRawClient client;

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

    @Test
    public void verifyNoTokenInPuts() throws Exception {

        client.makePutRequest(ENDPOINT, "content", TOKEN_PARAM);
        verify(httpClient).execute(captor.capture(), any(ResponseHandler.class));
        checkTokenExtraction();

        Mockito.reset(httpClient);

        Request request = Request.Builder.newBuilder()
            .setEndpoint(ENDPOINT)
            .setBinaryContent("content".getBytes(StandardCharsets.UTF_8))
            .setToken("CONFIDENTIAL")
            .build();
        client.makePutRequest(request);
        verify(httpClient).execute(captor.capture(), any(ResponseHandler.class));
        checkTokenExtraction();

        Mockito.reset(httpClient);

        request = Request.Builder.newBuilder()
            .setEndpoint(ENDPOINT)
            .setBinaryContent("content".getBytes(StandardCharsets.UTF_8))
            .addUrlParameters(TOKEN_PARAMS)
            .build();
        client.makePutRequest(request);
        verify(httpClient).execute(captor.capture(), any(ResponseHandler.class));
        checkTokenExtraction();
    }

    @Test
    public void verifyNoTokenInDeletes() throws Exception {

        Request request = Request.Builder.newBuilder()
            .setEndpoint(ENDPOINT)
            .setContent("content")
            .setToken("CONFIDENTIAL")
            .build();
        client.makeDeleteRequest(request);
        verify(httpClient).execute(captor.capture(), any(ResponseHandler.class));
        checkTokenExtraction();

        Mockito.reset(httpClient);

        request = Request.Builder.newBuilder()
            .setEndpoint(ENDPOINT)
            .setContent("content")
            .addUrlParameters(TOKEN_PARAMS)
            .build();
        client.makeDeleteRequest(request);
        verify(httpClient).execute(captor.capture(), any(ResponseHandler.class));
        checkTokenExtraction();
    }

    @Test
    void verifyTokenFieldOverridesUrlParams() throws Exception {
        Request request = Request.Builder.newBuilder()
            .setEndpoint(ENDPOINT)
            .setToken("CONFIDENTIAL")
            .addUrlParameters(List.of(new SingleUrlParameters("token", "INVALID")))
            .build();
        client.makeGetRequest(request);
        verify(httpClient).execute(captor.capture(), any(ResponseHandler.class));
        checkTokenExtraction();
    }

    @Test
    void verifyHealthServicesRequestWithFilter() throws Exception {

        HealthServicesRequest healthServicesRequest = HealthServicesRequest.newBuilder()
            .setQueryParams(QueryParams.DEFAULT)
            .setToken("CONFIDENTIAL")
            .setPassing(true)
            .setDatacenter("dc1")
            .setFilter("\"GPU\" in Service.Tags and \"CPU\" in Service.Tags")
            .build();

        client.makeGetRequest("/v1/health/service/the-service", healthServicesRequest.asUrlParameters());
        verify(httpClient).execute(captor.capture(), any(ResponseHandler.class));

        String targetUri = captor.getValue().getURI().toString();
        assertThat(targetUri).isEqualTo("http://host:8888/path/v1/health/service/the-service?dc=dc1&filter=%22GPU%22+in+Service.Tags+and+%22CPU%22+in+Service.Tags&passing=true");
    }

    private void checkTokenExtraction() {
        String targetUri = captor.getValue().getURI().toString();

        assertThat(targetUri).isEqualTo(EXPECTED_AGENT_ADDRESS)
            .doesNotContain("token", "CONFIDENTIAL", "INVALID");
        Header[] headers = captor.getValue().getAllHeaders();
        assertThat(headers).hasSize(1);
        assertThat(headers[0].getName()).isEqualTo("X-Consul-Token");
        assertThat(headers[0].getValue()).isEqualTo("CONFIDENTIAL");
    }

}
