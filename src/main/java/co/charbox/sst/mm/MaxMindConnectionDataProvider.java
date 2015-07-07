package co.charbox.sst.mm;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.methods.GetMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import co.charbox.domain.model.mm.ConnectionInfoModel;
import co.charbox.domain.model.mm.LocationModel;
import co.charbox.domain.model.mm.SimpleLocationModel;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.maxmind.geoip2.exception.HttpException;
import com.tpofof.core.utils.Config;
import com.tpofof.core.utils.HttpClientProvider;
import com.tpofof.core.utils.json.JsonUtils;

@Component
@Scope(BeanDefinition.SCOPE_SINGLETON)
public final class MaxMindConnectionDataProvider {

	@Autowired private HttpClientProvider httpClientProvider;
	@Autowired private JsonUtils json;
	private String maxMindUrl;
	private Cache<String, ConnectionInfoModel> cache;
	
	@Autowired
	public MaxMindConnectionDataProvider(Config config) {
		maxMindUrl = config.getString("mm.server.url") + "mm/";
		cache = CacheBuilder.newBuilder()
							.expireAfterAccess(3, TimeUnit.MINUTES)
							.expireAfterWrite(30, TimeUnit.MINUTES)
							.recordStats()
							.build();
	}
	
	public ConnectionInfoModel getConnectionInfo(String ip) {
		ConnectionInfoModel info = cache.getIfPresent(ip);
		if (info == null) {
			try {
				GetMethod gm = new GetMethod(maxMindUrl + ip);
				if (httpClientProvider.get().executeMethod(gm) == 200) {
					info = json.fromJsonResponse(gm.getResponseBodyAsString(), ConnectionInfoModel.class);
					cache.put(ip, info);
				}
				gm.releaseConnection();
			} catch (HttpException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return info;
	}

	public SimpleLocationModel getCurrentServerLocation() {
		ConnectionInfoModel info = getConnectionInfo("self");
		if (info != null) {
			LocationModel loc = info.getLocation();
			return SimpleLocationModel.builder()
					.ip(info.getConnection().getIp())
					.lat(loc.getLat())
					.lon(loc.getLon())
					.build();
		}
		return null;
	}
}
