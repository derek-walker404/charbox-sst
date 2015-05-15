package co.charbox.sst.dw;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.tpofof.core.utils.Config;
import com.tpofof.core.utils.json.ObjectMapperProvider;

@Component
@Path("/configs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class ConfigResource {

	@Autowired private Config config;
	@Autowired private ObjectMapperProvider mapperP;
	
	/*
	 * sst.socket.port = 31415
sst.initialSize = 6000
sst.maxSize = 262144000

sst.executor.threadCount = 2
	 */
	
	@Path("/initialSize")
	@GET
	@Timed
	public JsonNode getInitialSize() {
		return mapperP.get().createObjectNode().put("initialSize", config.getInt("sst.initialSize"));
	}
	
	@Path("/maxSize")
	@GET
	@Timed
	public JsonNode getMaxSize() {
		return mapperP.get().createObjectNode().put("maxSize", config.getInt("sst.maxSize"));
	}
	
	@Path("/port")
	@GET
	@Timed
	public JsonNode getPort() {
		return mapperP.get().createObjectNode().put("port", config.getInt("sst.socket.port"));
	}
	
	@Path("/threadCount")
	@GET
	@Timed
	public JsonNode getThreadCount() {
		return mapperP.get().createObjectNode().put("threadCount", config.getInt("sst.executor.threadCount"));
	}

	@GET
	@Timed
	public JsonNode getAll() {
		return mapperP.get().createObjectNode()
				.put("initialSize", config.getInt("sst.initialSize"))
				.put("maxSize", config.getInt("sst.maxSize"))
				.put("port", config.getInt("sst.socket.port"))
				.put("threadCount", config.getInt("sst.executor.threadCount"));
	}
}
