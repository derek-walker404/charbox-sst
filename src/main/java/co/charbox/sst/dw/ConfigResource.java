package co.charbox.sst.dw;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import co.charbox.core.utils.Config;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/configs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigResource {

	private final Config config;
	private final ObjectMapper mapper;
	
	public ConfigResource() {
		this.config = Config.get();
		this.mapper = new ObjectMapper();
	}
	
	
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
		return mapper.createObjectNode().put("initialSize", config.getInt("sst.initialSize"));
	}
	
	@Path("/maxSize")
	@GET
	@Timed
	public JsonNode getMaxSize() {
		return mapper.createObjectNode().put("maxSize", config.getInt("sst.maxSize"));
	}
	
	@Path("/port")
	@GET
	@Timed
	public JsonNode getPort() {
		return mapper.createObjectNode().put("port", config.getInt("sst.socket.port"));
	}
	
	@Path("/threadCount")
	@GET
	@Timed
	public JsonNode getThreadCount() {
		return mapper.createObjectNode().put("threadCount", config.getInt("sst.executor.threadCount"));
	}

	@GET
	@Timed
	public JsonNode getAll() {
		return mapper.createObjectNode()
				.put("initialSize", config.getInt("sst.initialSize"))
				.put("maxSize", config.getInt("sst.maxSize"))
				.put("port", config.getInt("sst.socket.port"))
				.put("threadCount", config.getInt("sst.executor.threadCount"));
	}
}
