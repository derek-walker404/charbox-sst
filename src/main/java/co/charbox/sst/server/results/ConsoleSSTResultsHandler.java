package co.charbox.sst.server.results;

import java.net.Socket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import co.charbox.domain.model.SstResults;

import com.tpofof.core.utils.json.JsonUtils;

@Component
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class ConsoleSstResultsHandler implements SstResultsHandler {

	@Autowired private JsonUtils json;
	
	public boolean handle(SstResults results, Socket client) {
		System.out.println(json.toJson(results));
		System.out.println("\t" + client.getRemoteSocketAddress().toString() + ":" + client.getPort());
		return true;
	}

}
