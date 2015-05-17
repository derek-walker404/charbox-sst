package co.charbox.sst.server;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;

import org.joda.time.DateTime;

import co.charbox.client.sst.SSTProperties;
import co.charbox.client.sst.utils.DataReceiver;
import co.charbox.client.sst.utils.DataSender;
import co.charbox.client.sst.utils.MyIOHAndler;
import co.charbox.core.utils.SpeedUtils;
import co.charbox.domain.model.MyLocation;
import co.charbox.domain.model.SstResults;
import co.charbox.domain.model.mm.ConnectionInfoModel;
import co.charbox.domain.model.mm.MyCharboxConnection;
import co.charbox.sst.server.results.SstResultsHandler;

@Builder
@AllArgsConstructor
public class ServerTestRunner implements Runnable {

	@NonNull private final Socket client;
	@NonNull private Integer initialSize;
	@NonNull private Integer maxSize;
	@NonNull private List<SstResultsHandler> handlers;
	@NonNull private SstChartbotApiClient charbotApiClient;
	private SstResults results;

	public void run() {
		try {
			MyIOHAndler io = new MyIOHAndler(client);
			initResults(io);
			
			calculateDownloadSpeed(io);
			calculateUploadSpeed(io);
			calculatePingSpeed(io);
			
			io.write("F");
			
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
		String deviceId = io.read();
		String deviceToken = io.read();
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
		int currSize = this.initialSize;
		while (this.results.getDownloadDuration() < 1000) {
			executeDownloadTest(currSize, io);
			if (this.results.getDownloadDuration() >= 800 || currSize >= this.maxSize) {
				double speed = this.results.getDownloadSpeed();
				int duration = this.results.getDownloadDuration();
				executeDownloadTest(currSize, io);
				this.results.setDownloadSpeed(avg(this.results.getDownloadSpeed(), speed));
				this.results.setDownloadDuration((int)avg(this.results.getDownloadDuration(), duration));
				break;
			} else {
				currSize *= 2;
			}
		}
	}
	
	private void executeDownloadTest(int size, MyIOHAndler io) throws IOException {
//		System.out.println("Download Test...");
		this.results.setDownloadSize(size);
		io.write("D");
		io.write(size);
		new DataSender(io, SSTProperties.getDefaultDataChunk(), size).run();
		this.results.setDownloadDuration(io.readInt());
		this.results.setDownloadSpeed(SpeedUtils.calcSpeed(results.getDownloadDuration(), size));
	}
	
	private void calculateUploadSpeed(MyIOHAndler io) throws IOException {
		// TODO: cache initial size??
		int currSize = this.initialSize;
		while (this.results.getUploadDuration() < 1000) {
			executeUploadTest(currSize, io);
			if (this.results.getUploadDuration() >= 800 || currSize >= this.maxSize) {
				double speed = this.results.getDownloadSpeed();
				int duration = this.results.getDownloadDuration();
				executeUploadTest(currSize, io);
				this.results.setUploadSpeed(avg(this.results.getUploadSpeed(), speed));
				this.results.setUploadDuration((int)avg(this.results.getUploadDuration(), duration));
				break;
			} else {
				currSize *= 2;
			}
		}
	}
	
	private void executeUploadTest(int size, MyIOHAndler io) throws IOException {
//		System.out.println("Upload Test...");
		this.results.setUploadSize(size);
		io.write("U");
		io.write(size);
		DataReceiver dr = new DataReceiver(io, size);
		dr.run();
		this.results.setUploadDuration(dr.getDuration());
		this.results.setUploadSpeed(SpeedUtils.calcSpeed(results.getUploadDuration(), size));
//		System.out.println("\t" + this.results);
	}
	
	private void calculatePingSpeed(MyIOHAndler io) throws IOException {
		executePingTest(io);
		int duration = this.results.getPingDuration();
		executePingTest(io);
		this.results.setPingDuration((int)avg(this.results.getPingDuration(), duration));
	}
	
	private void executePingTest(MyIOHAndler io) throws IOException {
//		System.out.println("Ping Test...");
		io.write("P");
		io.write(io.read());
		this.results.setPingDuration(io.readInt());
	}
	
	private double avg(double a, double b) {
		return (double)(a + b) / 2.0;
	}
}
