package co.charbox.sst.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import co.charbox.domain.model.SstResults;

import com.tpofof.core.App;
import com.tpofof.core.utils.AuthorizationHeader;
import com.tpofof.core.utils.Config;
import com.tpofof.core.utils.HttpClientProvider;
import com.tpofof.core.utils.json.JsonUtils;

@Component
public class SstChartbotApiClient {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SstChartbotApiClient.class);

	@Autowired private Config config;
	@Autowired private HttpClientProvider httpClientProvider;
	@Autowired private Base64 base64;
	@Autowired private JsonUtils json;
	
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
	
	/**
	 * Adding this method because Elasticsearch SUCKS as a primary data store... Fucking lame if you ask me... 
	 * TODO: move to SQL
	 * 
	 * @param deviceId
	 * @param deviceToken
	 * @return
	 */
	public boolean validateDeviceToken(String deviceId, String deviceToken, String serviceId, int retryCount) {
		for (int i=0;i<retryCount;i++) {
			if (validateDeviceToken(deviceId, deviceToken, serviceId)) {
				return true;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public boolean postSstResult(SstResults results) {
		String serviceApiKey = config.getString("charbot.api.auth.key", "asdf123");
		String serviceId = config.getString("charbot.api.auth.id", "test-sst-0");
		PostMethod post = new PostMethod(baseUrl() + "/sst");
		try {
			post.setRequestEntity(new StringRequestEntity(json.toJson(results), 
					"application/json", 
					"UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		String username = serviceId + "@sst";
		System.out.println(username + ":" + serviceApiKey);
		post.addRequestHeader(new AuthorizationHeader(username, serviceApiKey));
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
		String deviceToken = "b8f2191d-22a8-49ae-ab0e-5861d99138e3";
		String serviceId = "sst";
		SstChartbotApiClient client = App.getContext().getBean(SstChartbotApiClient.class);
		System.out.println(client.validateDeviceToken(deviceId, deviceToken, serviceId));
	}
}
