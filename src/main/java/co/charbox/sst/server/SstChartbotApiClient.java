package co.charbox.sst.server;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import co.charbox.domain.model.SstResults;

import com.tpofof.core.App;
import com.tpofof.core.utils.AuthorizationHeader;
import com.tpofof.core.utils.Config;
import com.tpofof.core.utils.HttpClientProvider;

@Component
public class SstChartbotApiClient {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SstChartbotApiClient.class);

	@Autowired private Config config;
	@Autowired private HttpClientProvider httpClientProvider;
	@Autowired private Base64 base64;
	
	protected String baseUrl() {
		return config.getString("charbot.api.uri", "http://localhost:8080");
	}
	
	public boolean validateDeviceToken(String deviceId, String deviceToken, String serviceId) {
		GetMethod get = new GetMethod(baseUrl() + "/auth/validate/token");
		get.addRequestHeader(new AuthorizationHeader(deviceId, deviceToken, serviceId));
		try {
			boolean success = 200 == httpClientProvider.get().executeMethod(get);
			if (!success) {
				LOGGER.error(get.getResponseBodyAsString());
			}
			return success;
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean postSstResult(SstResults results) {
		String serviceApiKey = config.getString("charbot.api.auth.key", "asdf123");
		PostMethod post = new PostMethod(baseUrl() + "/sst");
		post.addRequestHeader(new AuthorizationHeader("sst", serviceApiKey));
		try {
			boolean success = 200 == httpClientProvider.get().executeMethod(post);
			if (!success) {
				LOGGER.error(post.getResponseBodyAsString());
			}
			return success;
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static void main(String[] args) {
		String deviceId = "test-dev";
		String deviceToken = "45080c95-01c8-475c-b785-3eca32bb8f2d";
		String serviceId = "sst";
		SstChartbotApiClient client = App.getContext().getBean(SstChartbotApiClient.class);
		System.out.println(client.validateDeviceToken(deviceId, deviceToken, serviceId));
	}
}
