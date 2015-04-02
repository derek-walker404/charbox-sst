package co.charbox.sst.dw;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

public class ElasticsearchConfiguration extends Configuration {

	private String host;
	private int port;

	@JsonProperty
	public String getHost() {
		return host;
	}

	@JsonProperty
	public ElasticsearchConfiguration setHost(String host) {
		this.host = host;
		return this;
	}

	@JsonProperty
	public int getPort() {
		return port;
	}

	@JsonProperty
	public ElasticsearchConfiguration setPort(int port) {
		this.port = port;
		return this;
	}
}
