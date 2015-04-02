package co.charbox.sst.server.results;

import java.net.Socket;

import co.charbox.core.utils.JsonUtils;
import co.charbox.domain.model.mm.SSTResults;

public class ConsoleSSTResultsHandler implements SSTResultsHandler {

	public boolean handle(SSTResults results, Socket client) {
		System.out.println(JsonUtils.toJson(results));
		System.out.println("\t" + client.getRemoteSocketAddress().toString() + ":" + client.getPort());
		return true;
	}

}
