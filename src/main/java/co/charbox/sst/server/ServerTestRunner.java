package co.charbox.sst.server;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;

import org.joda.time.DateTime;

import co.charbox.client.sst.InvalidDeviceTokenException;
import co.charbox.client.sst.SSTProperties;
import co.charbox.client.sst.results.SstResultsHandler;
import co.charbox.client.sst.utils.DataReceiver;
import co.charbox.client.sst.utils.DataSender;
import co.charbox.client.sst.utils.MyIOHAndler;
import co.charbox.core.utils.SpeedUtils;
import co.charbox.domain.model.MyLocation;
import co.charbox.domain.model.SstResults;
import co.charbox.domain.model.mm.ConnectionInfoModel;
import co.charbox.domain.model.mm.MyCharboxConnection;

@Builder
@AllArgsConstructor
public class ServerTestRunner implements Runnable {

	@NonNull private final Socket client;
	@NonNull private Integer initialSize;
	@NonNull private Integer minSendTime;
	@NonNull private List<SstResultsHandler> handlers;
	private SstChartbotApiClient charbotApiClient;
	private SstResults results;

	public void run() {
		try {
			MyIOHAndler io = new MyIOHAndler(client, 4096);
			initResults(io);
			
			calculateDownloadSpeed(io);
			calculateUploadSpeed(io);
			calculatePingSpeed(io);
			
			io.write("F", true);
			
			for (SstResultsHandler handler : handlers) {
				handler.handle(results, client);
			}
			io.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidDeviceTokenException e) {
			e.printStackTrace();
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void initResults(MyIOHAndler io) throws InvalidDeviceTokenException {
		String[] deviceVals = io.read(true).split(":");
		String deviceId = deviceVals[0];
		System.out.println("Read deviceId " + deviceId);
		String deviceToken = deviceVals[1];
		System.out.println("Read deviceToken " + deviceToken);
		this.results = SstResults.builder()
			.deviceId(deviceId)
			.deviceToken(deviceToken)
			.testStartTime(new DateTime())
			.deviceInfo(ConnectionInfoModel.builder()
					.connection(MyCharboxConnection.builder()
							.ip(io.getRemoteIp())
							.build())
					.build())
			.serverLocation(MyLocation.builder()
					.ip("") // TODO
					.build())
			.build();
		try {
			Thread.sleep(500); // Sleep to allow token to appear in ES
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (!charbotApiClient.validateDeviceToken(deviceId, deviceToken, "sst", 5)) {
			throw new InvalidDeviceTokenException(deviceId, deviceToken, "sst");
		}
	}
	
	private void calculateDownloadSpeed(MyIOHAndler io) throws IOException {
		// TODO: cache initial size??
		long currSize = this.initialSize;
		long totalDownloadSize = 0;
		while (this.results.getDownloadDuration() < this.minSendTime) {
			executeDownloadTest(currSize, io);
			totalDownloadSize += currSize;
			if (this.results.getDownloadDuration() >= this.minSendTime) {
				// TODO: Do I need this?
				double speed = this.results.getDownloadSpeed();
				int duration = this.results.getDownloadDuration();
				executeDownloadTest(currSize, io);
				this.results.setDownloadSpeed(avg(this.results.getDownloadSpeed(), speed));
				this.results.setDownloadDuration((int)avg(this.results.getDownloadDuration(), duration));
//				this.results.setDownloadSpeed(this.results.getDownloadSpeed());
//				this.results.setDownloadDuration(this.results.getDownloadDuration());
				break;
			} else {
				currSize *= 2;
			}
		}
		System.out.println("Total Download: " + totalDownloadSize/1024/1024 + "Mbs");
	}
	
	private void executeDownloadTest(long size, MyIOHAndler io) throws IOException {
		System.out.println("Download Test...");
		this.results.setDownloadSize(size);
		io.write("D", true);
		io.write(size, true);
		new DataSender(io, SSTProperties.getDefaultDataChunk(), size).run();
		this.results.setDownloadDuration(io.readInt(true));
		this.results.setDownloadSpeed(SpeedUtils.calcSpeed(results.getDownloadDuration(), size));
	}
	
	private void calculateUploadSpeed(MyIOHAndler io) throws IOException {
		// TODO: cache initial size??
		long currSize = this.initialSize;
		long totalUploadSize = 0;
		while (this.results.getUploadDuration() < this.minSendTime) {
			executeUploadTest(currSize, io);
			totalUploadSize += currSize;
			if (this.results.getUploadDuration() >= this.minSendTime) {
				// TODO: do I need this?
				double speed = this.results.getDownloadSpeed();
				int duration = this.results.getDownloadDuration();
				executeUploadTest(currSize, io);
				this.results.setUploadSpeed(avg(this.results.getUploadSpeed(), speed));
				this.results.setUploadDuration((int)avg(this.results.getUploadDuration(), duration));
//				this.results.setUploadSpeed(this.results.getUploadSpeed());
//				this.results.setUploadDuration(this.results.getUploadDuration());
				break;
			} else {
				currSize *= 2;
			}
		}
		System.out.println("Total Upload: " + totalUploadSize/1024/1024 + "Mb");
	}
	
	private void executeUploadTest(long size, MyIOHAndler io) throws IOException {
		System.out.println("Upload Test...");
		this.results.setUploadSize(size);
		io.write("U", true);
		io.write(size, true);
		DataReceiver dr = new DataReceiver(io, size);
		dr.run();
		this.results.setUploadDuration(dr.getDuration());
		this.results.setUploadSpeed(SpeedUtils.calcSpeed(results.getUploadDuration(), size));
	}
	
	private void calculatePingSpeed(MyIOHAndler io) throws IOException {
		executePingTest(io);
		int duration = this.results.getPingDuration();
		executePingTest(io);
		this.results.setPingDuration((int)avg(this.results.getPingDuration(), duration));
	}
	
	private void executePingTest(MyIOHAndler io) throws IOException {
		System.out.println("Ping Test...");
		io.write("P", true);
		io.write(io.read(false), false);
		this.results.setPingDuration(io.readInt(true));
	}
	
	private double avg(double a, double b) {
		return (double)(a + b) / 2.0;
	}
}
