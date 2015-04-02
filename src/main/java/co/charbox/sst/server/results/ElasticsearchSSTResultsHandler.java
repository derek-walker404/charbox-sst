package co.charbox.sst.server.results;

import static co.charbox.sst.mm.MaxMindConnectionDataProvider.getConnectionInfo;
import static co.charbox.sst.mm.MaxMindConnectionDataProvider.getCurrentServerLocation;

import java.net.Socket;

import org.bson.types.ObjectId;

import co.charbox.core.utils.Config;
import co.charbox.core.utils.JsonUtils;
import co.charbox.domain.model.mm.ConnectionInfoModel;
import co.charbox.domain.model.mm.SSTResults;
import co.charbox.sst.data.es.SSTResultElasticsearchDAO;

public class ElasticsearchSSTResultsHandler implements SSTResultsHandler {

	private final SSTResultElasticsearchDAO dao;
	
	public ElasticsearchSSTResultsHandler(SSTResultElasticsearchDAO dao) {
		this.dao = dao;
	}
	
	public boolean handle(SSTResults results, Socket client) {
		String ip = client.getInetAddress().getHostAddress();
		if (client.getInetAddress().isLoopbackAddress()) {
			ip = Config.get().getString("client.ip.override");
		}
		if (ip != null) {
			ConnectionInfoModel connectionInfo = getConnectionInfo(ip);
			if (connectionInfo != null) {
				results.set_id(new ObjectId().toString());
				results.setDeviceInfo(connectionInfo);
				results.setServerLocation(getCurrentServerLocation());
				System.out.println(JsonUtils.toJson(results));
				dao.insert(results);
			}
			return true;
		}
		return false;
	}

}
