package co.charbox.sst.server.results;

import java.net.Socket;

import co.charbox.domain.model.SstResults;

public interface SstResultsHandler {

	public boolean handle(SstResults results, Socket client);
}
