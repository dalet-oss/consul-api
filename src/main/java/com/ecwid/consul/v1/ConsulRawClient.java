package com.ecwid.consul.v1;

import com.ecwid.consul.SingleUrlParameters;
import com.ecwid.consul.UrlParameters;
import com.ecwid.consul.Utils;
import com.ecwid.consul.transport.*;
import org.apache.http.client.HttpClient;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Vasily Vasilkov (vgv@ecwid.com)
 */
public class ConsulRawClient {

	public static final String DEFAULT_HOST = "localhost";
	public static final int DEFAULT_PORT = 8500;
	public static final String DEFAULT_PATH = "";

	// one real HTTP client for all instances
	private static final HttpTransport DEFAULT_HTTP_TRANSPORT = new DefaultHttpTransport();

	private final HttpTransport httpTransport;
	private final String agentAddress;

	public static final class Builder {
		private String agentHost;
		private int agentPort;
		private String agentPath;
		private HttpTransport httpTransport;

		public static ConsulRawClient.Builder builder() {
			return new ConsulRawClient.Builder();
		}

		private Builder() {
			this.agentHost = DEFAULT_HOST;
			this.agentPort = DEFAULT_PORT;
			this.agentPath = DEFAULT_PATH;
			this.httpTransport = DEFAULT_HTTP_TRANSPORT;
		}

		public Builder setHost(String host) {
			this.agentHost = host;
			return this;
		}

		public Builder setPort(int port) {
			this.agentPort = port;
			return this;
		}

		public Builder setPath(String path) {
			this.agentPath = path;
			return this;
		}

		public Builder setTlsConfig(TLSConfig tlsConfig) {
			this.httpTransport = new DefaultHttpsTransport(tlsConfig);
			return this;
		}

		public Builder setHttpClient(HttpClient httpClient) {
			this.httpTransport = new DefaultHttpTransport(httpClient);
			return this;
		}

		public ConsulRawClient build() {
			return new ConsulRawClient(httpTransport, agentHost, agentPort, agentPath);
		}
	}

	public ConsulRawClient() {
		this(DEFAULT_HOST);
	}

	public ConsulRawClient(TLSConfig tlsConfig) {
		this(DEFAULT_HOST, tlsConfig);
	}

	public ConsulRawClient(String agentHost) {
		this(agentHost, DEFAULT_PORT);
	}

	public ConsulRawClient(String agentHost, TLSConfig tlsConfig) {
		this(agentHost, DEFAULT_PORT, tlsConfig);
	}

	public ConsulRawClient(String agentHost, int agentPort) {
		this(DEFAULT_HTTP_TRANSPORT, agentHost, agentPort, DEFAULT_PATH);
	}

	public ConsulRawClient(HttpClient httpClient) {
		this(DEFAULT_HOST, httpClient);
	}

	public ConsulRawClient(String agentHost, HttpClient httpClient) {
		this(new DefaultHttpTransport(httpClient), agentHost, DEFAULT_PORT, DEFAULT_PATH);
	}

	public ConsulRawClient(String agentHost, int agentPort, HttpClient httpClient) {
		this(new DefaultHttpTransport(httpClient), agentHost, agentPort, DEFAULT_PATH);
	}

	public ConsulRawClient(String agentHost, int agentPort, TLSConfig tlsConfig) {
		this(new DefaultHttpsTransport(tlsConfig), agentHost, agentPort, DEFAULT_PATH);
	}

	public ConsulRawClient(HttpClient httpClient, String host, int port, String path) {
		this(new DefaultHttpTransport(httpClient), host, port, path);
	}

	// hidden constructor, for tests
	ConsulRawClient(HttpTransport httpTransport, String agentHost, int agentPort, String path) {
		this.httpTransport = httpTransport;

		// check that agentHost has scheme or not
		String agentHostLowercase = agentHost.toLowerCase();
		if (!agentHostLowercase.startsWith("https://") && !agentHostLowercase.startsWith("http://")) {
			// no scheme in host, use default 'http'
			agentHost = "http://" + agentHost;
		}

		this.agentAddress = Utils.assembleAgentAddress(agentHost, agentPort, path);
	}

	public HttpResponse makeGetRequest(String endpoint, UrlParameters... urlParams) {
		return makeGetRequest(endpoint, Arrays.asList(urlParams));
	}

	public HttpResponse makeGetRequest(String endpoint, List<UrlParameters> urlParams) {
		HttpRequest httpRequest = httpRequestBuilder(endpoint, urlParams).build();

		return httpTransport.makeGetRequest(httpRequest);
	}

	public HttpResponse makeGetRequest(Request request) {
		HttpRequest httpRequest = httpRequestBuilder(request).build();

		return httpTransport.makeGetRequest(httpRequest);
	}

	public HttpResponse makePutRequest(String endpoint, String content, UrlParameters... urlParams) {
		HttpRequest httpRequest = httpRequestBuilder(endpoint, Arrays.asList(urlParams))
			.setContent(content)
			.build();

		return httpTransport.makePutRequest(httpRequest);
	}

	public HttpResponse makePutRequest(Request request) {
		HttpRequest httpRequest = httpRequestBuilder(request)
			.setBinaryContent(request.getBinaryContent())
			.build();

		return httpTransport.makePutRequest(httpRequest);
	}

	public HttpResponse makeDeleteRequest(Request request) {
		HttpRequest httpRequest = httpRequestBuilder(request).build();

		return httpTransport.makeDeleteRequest(httpRequest);
	}

	private String prepareUrl(String url) {
		if (url.contains(" ")) {
			// temp hack for old clients who did manual encoding and just use %20
			// TODO: Remove it in 2.0
			return Utils.encodeUrl(url);
		} else {
			return url;
		}
	}

	private HttpRequest.Builder httpRequestBuilder(Request request) {
		HttpRequest.Builder requestBuilder = httpRequestBuilder(request.getEndpoint(), request.getUrlParameters());

		// If a token is provided in both places, then the one in URL parameters will be overridden by the one in the
		// `token` field.
		Optional.ofNullable(request.getToken())
			.ifPresent(token -> requestBuilder.addHeader("X-Consul-Token", token));

		return requestBuilder;
	}

	private HttpRequest.Builder httpRequestBuilder(String endpoint, List<UrlParameters> urlParams) {
		String baseUrl = prepareUrl(agentAddress + endpoint);
        List<UrlParameters> finalUrlParams = new ArrayList<>(urlParams);

		HttpRequest.Builder requestBuilder = HttpRequest.Builder.newBuilder();

        if (urlParams != null) {
            for (UrlParameters urlParam : urlParams) {
                if (urlParam instanceof SingleUrlParameters singleUrlParameters) {
                    String token = extractTokenParam(singleUrlParameters);

                    if (token != null) {
                        requestBuilder.addHeader("X-Consul-Token", token);
                        finalUrlParams = urlParams.stream()
                            .filter(p -> !singleUrlParameters.equals(p))
                            .toList();

                        break;
                    }
                }
            }
        }

		return requestBuilder.setUrl(Utils.generateUrl(baseUrl, finalUrlParams));
	}

    public static String extractTokenParam(SingleUrlParameters singleUrlParameters) {
        try {
            Field keyField = SingleUrlParameters.class.getDeclaredField("key");
            keyField.setAccessible(true);
            String key = (String) keyField.get(singleUrlParameters);

            if ("token".equals(key)) {
                Field valueField = SingleUrlParameters.class.getDeclaredField("value");
                valueField.setAccessible(true);

                return (String) valueField.get(singleUrlParameters);
            }
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to extract token parameter", e);
        }

        return null;
    }

}
