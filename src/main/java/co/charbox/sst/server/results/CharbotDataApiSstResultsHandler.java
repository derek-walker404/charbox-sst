package co.charbox.sst.server.results;

import java.net.Socket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import co.charbox.domain.model.SstResultsModel;
import co.charbox.sst.results.SstResultsHandler;
import co.charbox.sst.server.SstChartbotApiClient;

@Component
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class CharbotDataApiSstResultsHandler implements SstResultsHandler {

	@Autowired private SstChartbotApiClient charbotClient;
	
	public boolean handle(SstResultsModel results, Socket client) {
		return charbotClient.postSstResult(results);
	}

}
