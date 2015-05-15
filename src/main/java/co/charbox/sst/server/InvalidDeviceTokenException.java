package co.charbox.sst.server;

import lombok.Getter;

@Getter
public class InvalidDeviceTokenException extends Exception {

	private static final long serialVersionUID = 1L;

	private String deviceId;
	private String deviceToken;
	private String serviceId;
	
	public InvalidDeviceTokenException(String deviceId, String deviceToken, String serviceId) {
		super("Invalid device token " + deviceId + "@" + serviceId + ":" + deviceToken);
		this.deviceId = deviceId;
		this.deviceToken = deviceToken;
		this.serviceId = serviceId;
	}
}
