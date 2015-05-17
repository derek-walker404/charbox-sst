package co.charbox.sst.dw;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import co.charbox.sst.server.SstServer;

import com.tpofof.core.App;

@Component
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class SstApplication extends Application<SstConfiguration> {

	@Autowired private SstServer sstServer;
	
	@Autowired private ConfigResource configResource;
	
	@Autowired private SstQueueSizeHealthCheck sstQueueHealthcheck;
	
	public static void main(String[] args) throws Exception {
		App.getContext().getBean(SstApplication.class).run(args);
	}
	
	@Override
	public String getName() {
		return "server-speed-test";
	}
	
	@Override
	public void initialize(Bootstrap<SstConfiguration> bootstrap) {
		
	}
	
	@Override
	public void run(SstConfiguration configuration, Environment environment) throws Exception {
		new Thread(sstServer).start();

		/* RESOURCES */
		environment.jersey().register(configResource);
		
		/* HEALTH CHECKS */
		environment.healthChecks().register("sst-queue-size", sstQueueHealthcheck);
	}
}
