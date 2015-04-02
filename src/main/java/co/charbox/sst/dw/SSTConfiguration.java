package co.charbox.sst.dw;

import io.dropwizard.Configuration;

import java.util.List;

import com.google.common.collect.Lists;

public class SSTConfiguration extends Configuration {
	
	private List<ElasticsearchConfiguration> esConfigs = Lists.newArrayList();

	public List<ElasticsearchConfiguration> getEsConfigs() {
		return esConfigs;
	}

	public SSTConfiguration setEsConfigs(List<ElasticsearchConfiguration> esConfigs) {
		this.esConfigs = esConfigs;
		return this;
	}
}
