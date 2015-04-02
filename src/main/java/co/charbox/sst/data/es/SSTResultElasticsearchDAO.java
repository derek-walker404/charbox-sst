package co.charbox.sst.data.es;

import org.elasticsearch.client.Client;

import co.charbox.core.data.es.AbstractElasticsearchDAO;
import co.charbox.domain.model.mm.SSTResults;

public class SSTResultElasticsearchDAO extends AbstractElasticsearchDAO<SSTResults> {

	public SSTResultElasticsearchDAO(Client client) {
		super(client, SSTResults.class);
	}

	@Override
	protected String getType() {
		return "sstResults";
	}

}
