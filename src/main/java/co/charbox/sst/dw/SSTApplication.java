package co.charbox.sst.dw;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.List;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import co.charbox.sst.data.es.SSTResultElasticsearchDAO;
import co.charbox.sst.server.SSTServer;
import co.charbox.sst.server.results.ConsoleSSTResultsHandler;
import co.charbox.sst.server.results.ElasticsearchSSTResultsHandler;
import co.charbox.sst.server.results.SSTResultsHandler;

import com.google.common.collect.Lists;

public class SSTApplication extends Application<SSTConfiguration> {

	public static void main(String[] args) throws Exception {
		new SSTApplication().run(args);
	}
	
	@Override
	public String getName() {
		return "server-speed-test";
	}
	
	@Override
	public void initialize(Bootstrap<SSTConfiguration> bootstrap) {
		
	}
	
	@Override
	public void run(SSTConfiguration configuration, Environment environment) throws Exception {
		/** DATA */
		/* ELASTICSEARCH */
		List<ElasticsearchConfiguration> esConfigs = configuration.getEsConfigs();
		TransportClient esClient = new TransportClient();
		for (ElasticsearchConfiguration c : esConfigs) {
        	esClient.addTransportAddress(new InetSocketTransportAddress(c.getHost(), c.getPort()));
		}
		/* ES DAO */
		final SSTResultElasticsearchDAO resultsDao = new SSTResultElasticsearchDAO(esClient);
		
		/** SPEED TEST SERVER */
		final List<SSTResultsHandler> handlers = Lists.newArrayList();
		handlers.add(new ConsoleSSTResultsHandler());
		handlers.add(new ElasticsearchSSTResultsHandler(resultsDao));
		final SSTServer sst = new SSTServer(handlers);
		new Thread(sst).start();

		/** SST DROP WIZARD APP */
		/* RESOURCES */
		final ConfigResource configResource = new ConfigResource();
		environment.jersey().register(configResource);
		/* HEALTH CHECKS */
		final SSTQueueSizeHealthCheck queueHealthCheck = new SSTQueueSizeHealthCheck(sst);
		environment.healthChecks().register("sst-queue-size", queueHealthCheck);
	}
}
