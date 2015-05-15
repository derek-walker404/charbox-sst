package co.charbox.sst.dw;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import co.charbox.sst.server.SstServer;

import com.codahale.metrics.health.HealthCheck;
import com.tpofof.core.utils.Config;

@Component
public class SstQueueSizeHealthCheck extends HealthCheck {

	@Autowired private SstServer sst;
	@Autowired Config config;
	
	@Override
	protected Result check() throws Exception {
		return sst.getQueueSize() > config.getInt("sst.queue.maxsize", 20)
				? Result.unhealthy("Queue size too large: " + sst.getQueueSize())
				: Result.healthy("Queue size: " + sst.getQueueSize());
	}

}
