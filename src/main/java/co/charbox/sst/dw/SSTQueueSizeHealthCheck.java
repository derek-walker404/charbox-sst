package co.charbox.sst.dw;

import co.charbox.core.utils.Config;
import co.charbox.sst.server.SSTServer;

import com.codahale.metrics.health.HealthCheck;

public class SSTQueueSizeHealthCheck extends HealthCheck {

	private final SSTServer sst;
	private final int maxQueueSize = Config.get().getInt("sst.queue.maxsize", 20);
	
	public SSTQueueSizeHealthCheck(SSTServer sst) {
		this.sst = sst;
	}
	
	@Override
	protected Result check() throws Exception {
		return sst.getQueueSize() > maxQueueSize
				? Result.unhealthy("Queue size too large: " + sst.getQueueSize())
				: Result.healthy("Queue size: " + sst.getQueueSize());
	}

}
