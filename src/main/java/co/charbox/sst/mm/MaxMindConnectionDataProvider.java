package co.charbox.sst.mm;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.methods.GetMethod;

import co.charbox.core.utils.Config;
import co.charbox.core.utils.HttpClientProvider;
import co.charbox.core.utils.JsonUtils;
import co.charbox.domain.model.MyLocation;
import co.charbox.domain.model.mm.ConnectionInfoModel;
import co.charbox.domain.model.mm.MyCharboxLocation;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.maxmind.geoip2.exception.HttpException;

public final class MaxMindConnectionDataProvider {

	private static final String maxMindUrl = Config.get().getString("mm.server.url") + "mm/";
	private static final Cache<String, ConnectionInfoModel> cache = CacheBuilder.newBuilder()
						.expireAfterAccess(3, TimeUnit.MINUTES)
						.expireAfterWrite(30, TimeUnit.MINUTES)
						.recordStats()
						.build();
	
	private MaxMindConnectionDataProvider() { }
	
	public static ConnectionInfoModel getConnectionInfo(String ip) {
		ConnectionInfoModel info = cache.getIfPresent(ip);
		if (info == null) {
			try {
				GetMethod gm = new GetMethod(maxMindUrl + ip);
				if (HttpClientProvider.get().executeMethod(gm) == 200) {
					info = JsonUtils.fromJsonResponse(gm.getResponseBodyAsString(), ConnectionInfoModel.class);
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

	public static MyLocation getCurrentServerLocation() {
		ConnectionInfoModel info = getConnectionInfo("self");
		if (info != null) {
			MyCharboxLocation loc = info.getLocation();
			return new MyLocation()
				.setIp(info.getConnection().getIp())
				.setLocation(loc.getLocation());
		}
		return null;
	}
}
