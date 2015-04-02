package co.charbox.sst.server.results;

import java.net.Socket;

import co.charbox.domain.model.mm.SSTResults;

public interface SSTResultsHandler {

	public boolean handle(SSTResults results, Socket client);
}
