package co.charbox.sst.server;

import java.util.List;

import org.elasticsearch.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.charbot.sst.results.ConsoleSstResultsHandler;
import co.charbot.sst.results.SstResultsHandler;
import co.charbox.sst.server.results.CharbotDataApiSstResultsHandler;

@Configuration
public class SstServerBeanConfiguration {

	@Autowired private ConsoleSstResultsHandler consoleSstResultsHandler;
	@Autowired private CharbotDataApiSstResultsHandler charbotSstResultsHandler;
	
	@Bean
	public List<SstResultsHandler> resultsHandlerMasterList() {
		List<SstResultsHandler> handlers = Lists.newArrayList();
		handlers.add(consoleSstResultsHandler);
		handlers.add(charbotSstResultsHandler);
		return handlers;
	}
}
