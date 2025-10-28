package com.ecwid.consul.v1.health;

import com.ecwid.consul.ConsulRequest;
import com.ecwid.consul.SingleUrlParameters;
import com.ecwid.consul.v1.TagsParameters;
import com.ecwid.consul.UrlParameters;
import com.ecwid.consul.v1.NodeMetaParameters;
import com.ecwid.consul.v1.QueryParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class HealthServicesRequest implements ConsulRequest {

	private final String datacenter;
	private final String near;
	private final String[] tags;
	private final Map<String, String> nodeMeta;
	private final String filter;
	private final boolean passing;
	private final QueryParams queryParams;
	private final String token;

	private HealthServicesRequest(String datacenter, String near, String[] tags, Map<String, String> nodeMeta,
			String filter, boolean passing, QueryParams queryParams, String token) {
		this.datacenter = datacenter;
		this.near = near;
		this.tags = tags;
		this.nodeMeta = nodeMeta;
		this.filter = filter;
		this.passing = passing;
		this.queryParams = queryParams;
		this.token = token;
	}

	public String getDatacenter() {
		return datacenter;
	}

	public String getNear() {
		return near;
	}

	public String getTag() {
		return tags != null && tags.length > 0 ? tags[0] : null;
	}

	public String[] getTags() {
		return tags;
	}

	public Map<String, String> getNodeMeta() {
		return nodeMeta;
	}

	public String getFilter() {
		return filter;
	}

	public boolean isPassing() {
		return passing;
	}

	public QueryParams getQueryParams() {
		return queryParams;
	}

	public String getToken() {
		return token;
	}

	public static class Builder {
		private String datacenter;
		private String near;
		private String[] tags;
		private Map<String, String> nodeMeta;
		private String filter;
		private boolean passing;
		private QueryParams queryParams;
		private String token;

		private Builder() {
		}

		public Builder setDatacenter(String datacenter) {
			this.datacenter = datacenter;
			return this;
		}

		public Builder setNear(String near) {
			this.near = near;
			return this;
		}

		/**
		 * Sets the service tag to filter results by.
		 *
		 * @see <a href="https://developer.hashicorp.com/consul/api-docs/health#query-parameters-2">https://developer.hashicorp.com/consul/api-docs/health#query-parameters-2</a>
		 * @param tag The service tag to filter by.
		 * @return This {@link Builder} instance for method chaining.
		 */
		@Deprecated
		public Builder setTag(String tag) {
			this.tags = new String[]{tag};
			return this;
		}

		/**
		 * Sets the service tags to filter results by.
		 *
		 * @deprecated Use filter with the Service.Tags selector instead.
		 * @see <a href="https://developer.hashicorp.com/consul/api-docs/health#query-parameters-2">https://developer.hashicorp.com/consul/api-docs/health#query-parameters-2</a>
		 * @param tags The service tags to filter by.
		 * @return This {@link Builder} instance for method chaining.
		 */
		@Deprecated
		public Builder setTags(String[] tags) {
			this.tags = tags;
			return this;
		}

		/**
		 * Sets the node metadata to filter results by.
		 *
		 * @deprecated Use filter with the Node.Meta selector instead.
		 * @see <a href="https://developer.hashicorp.com/consul/api-docs/health#query-parameters-2">https://developer.hashicorp.com/consul/api-docs/health#query-parameters-2</a>
		 * @param nodeMeta The node metadata to filter by.
		 * @return This {@link Builder} instance for method chaining.
		 */
		@Deprecated
		public Builder setNodeMeta(Map<String, String> nodeMeta) {
			this.nodeMeta = nodeMeta != null ? Collections.unmodifiableMap(nodeMeta) : null;
			return this;
		}

		/**
		 * Set the expression used to filter the queries results prior to returning the
		 * data.
		 *
		 * @param filter The filter expression.
		 * @return This {@link Builder} instance for method chaining.
		 */
		public Builder setFilter(String filter) {
			this.filter = filter;
			return this;
		}

		public Builder setPassing(boolean passing) {
			this.passing = passing;
			return this;
		}

		public Builder setQueryParams(QueryParams queryParams) {
			this.queryParams = queryParams;
			return this;
		}

		public Builder setToken(String token) {
			this.token = token;
			return this;
		}

		public HealthServicesRequest build() {
			return new HealthServicesRequest(datacenter, near, tags, nodeMeta, filter, passing, queryParams, token);
		}
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	@Override
	public List<UrlParameters> asUrlParameters() {
		List<UrlParameters> params = new ArrayList<>();

		if (datacenter != null) {
			params.add(new SingleUrlParameters("dc", datacenter));
		}

		if (near != null) {
			params.add(new SingleUrlParameters("near", near));
		}

		if (tags != null) {
			params.add(new TagsParameters(tags));
		}

		if (nodeMeta != null) {
			params.add(new NodeMetaParameters(nodeMeta));
		}

		// Allow a very basic form of filtering for now
		if (filter != null) {
			params.add(new SingleUrlParameters("filter", filter));
		}

		params.add(new SingleUrlParameters("passing", String.valueOf(passing)));

		if (queryParams != null) {
			params.add(queryParams);
		}

		if (token != null) {
			params.add(new SingleUrlParameters("token", token));
		}

		return params;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof HealthServicesRequest)) {
			return false;
		}
		HealthServicesRequest that = (HealthServicesRequest) o;
		return passing == that.passing &&
			Objects.equals(datacenter, that.datacenter) &&
			Objects.equals(near, that.near) &&
			Arrays.equals(tags, that.tags) &&
			Objects.equals(nodeMeta, that.nodeMeta) &&
			Objects.equals(queryParams, that.queryParams) &&
			Objects.equals(token, that.token);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(datacenter, near, nodeMeta, passing, queryParams, token);
		result = 31 * result + Arrays.hashCode(tags);
		return result;
	}
}
