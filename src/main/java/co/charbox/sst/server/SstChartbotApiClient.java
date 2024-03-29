package co.charbox.sst.server;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import co.charbox.domain.model.SstResultsModel;

import com.tpofof.core.utils.AuthorizationHeader;
import com.tpofof.core.utils.Config;
import com.tpofof.core.utils.HttpClientProvider;
import com.tpofof.core.utils.json.JsonUtils;

@Slf4j
@Component
public class SstChartbotApiClient {
	
	@Autowired private Config config;
	@Autowired private HttpClientProvider httpClientProvider;
	@Autowired private Base64 base64;
	@Autowired private JsonUtils json;
	
	protected String baseUrl() {
		return config.getString("charbot.api.url", "http://localhost:8080");
	}
	
	public boolean validateDeviceToken(Serializable deviceId, String deviceToken, String serviceId) {
		GetMethod get = new GetMethod(baseUrl() + "/auth/validate/token");
		get.addRequestHeader(new AuthorizationHeader(deviceId, deviceToken, serviceId));
		try {
			boolean success = 200 == httpClientProvider.get().executeMethod(get);
			if (!success) {
				log.error(get.getResponseBodyAsString());
			}
			return success;
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean postSstResult(SstResultsModel results) {
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
		log.debug(username + ":" + serviceApiKey);
		post.addRequestHeader(new AuthorizationHeader(username, serviceApiKey));
		try {
			boolean success = 200 == httpClientProvider.get().executeMethod(post);
			if (!success) {
				log.error(post.getResponseBodyAsString());
			}
			return success;
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}
