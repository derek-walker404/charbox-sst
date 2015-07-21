package co.charbox.sst.server;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.joda.time.DateTime;

import co.charbox.core.utils.SpeedUtils;
import co.charbox.domain.model.DeviceModel;
import co.charbox.domain.model.SstResultsModel;
import co.charbox.domain.model.mm.ConnectionInfoModel;
import co.charbox.domain.model.mm.ConnectionModel;
import co.charbox.domain.model.mm.SimpleLocationModel;
import co.charbox.sst.InvalidDeviceTokenException;
import co.charbox.sst.results.SstResultsHandler;
import co.charbox.sst.utils.DataReceiver;
import co.charbox.sst.utils.DataSender;
import co.charbox.sst.utils.MyIOHAndler;

@Slf4j
@Builder
@AllArgsConstructor
public class ServerTestRunner implements Runnable {

	@NonNull private final Socket client;
	@NonNull private Integer initialSize;
	@NonNull private Integer minSendTime;
	@NonNull private Integer maxExecutionTime;
	@NonNull private List<SstResultsHandler> handlers;
	private SstChartbotApiClient charbotApiClient;
	private SstResultsModel results;
	private DateTime expirationDate;
	private AtomicBoolean running = new AtomicBoolean(true);
	
	public DateTime getExpiration() {
		return expirationDate;
	}
	
	public boolean isRunning() {
		return running.get();
	}
	
	public Socket getSocket() {
		return client;
	}

	public void run() {
		expirationDate = new DateTime().plusMillis(maxExecutionTime);
		try {
			MyIOHAndler io = new MyIOHAndler(client, 131072);
			initResults(io);
			
			calculatePingSpeed(io);
			calculateDownloadSpeed(io);
			calculateUploadSpeed(io);
			
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
		running.set(false);
	}

	private void initResults(MyIOHAndler io) throws InvalidDeviceTokenException {
		String[] deviceVals = io.read(true).split(":");
		Integer deviceId = Integer.parseInt(deviceVals[0]);
		log.debug("Read deviceId " + deviceId);
		String deviceToken = deviceVals[1];
		log.debug("Read deviceToken " + deviceToken);
		this.results = SstResultsModel.builder()
				.device(DeviceModel.builder()
						.id(deviceId)
						.build())
				.deviceToken(deviceToken)
				.startTime(new DateTime())
				.deviceInfo(ConnectionInfoModel.builder()
						.connection(ConnectionModel.builder()
								.ip(io.getRemoteIp())
								.build())
						.build())
				.serverLocation(SimpleLocationModel.builder()
						.ip("") // TODO
						.build())
				.build();
		try {
			Thread.sleep(500); // Sleep to allow token to appear in ES
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (!charbotApiClient.validateDeviceToken(deviceId, deviceToken, "sst")) {
			log.error("Invalid token, closing socket...");
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			throw new InvalidDeviceTokenException(deviceId, deviceToken, "sst");
		}
	}
	
	private void calculateDownloadSpeed(MyIOHAndler io) throws IOException {
		// TODO: cache initial size??
		long currSize = this.initialSize;
		long totalDownloadSize = 0;
		while (this.results.getDownloadDuration() < this.minSendTime) {
			totalDownloadSize += currSize;
			if (!executeDownloadTest(currSize, io)) {
				log.warn("Error during download test. Negative duration. currSize: " + currSize);
				continue;
			}
			if (this.results.getDownloadDuration() >= this.minSendTime) {
				// TODO: Do I need this?
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
		log.debug("Total Download: " + totalDownloadSize/1024/1024 + "Mbs");
	}
	
	private boolean executeDownloadTest(long size, MyIOHAndler io) throws IOException {
		log.trace("Download Test...");
		this.results.setDownloadSize(size);
		io.write("D", true);
		io.write(size, true);
		new DataSender(io, size).run();
		int duration = io.readInt(true) - results.getPingDuration();
		if (duration == -1) {
			log.error("negative duration: " + duration);
			return false;
		}
		this.results.setDownloadDuration(duration);
		this.results.setDownloadSpeed(SpeedUtils.calcSpeed(results.getDownloadDuration(), size));
		return true;
	}
	
	private void calculateUploadSpeed(MyIOHAndler io) throws IOException {
		// TODO: cache initial size??
		long currSize = this.initialSize;
		long totalUploadSize = 0;
		while (this.results.getUploadDuration() < this.minSendTime) {
			totalUploadSize += currSize;
			if (!executeUploadTest(currSize, io)) {
				log.warn("Error during upload test. Negative duration. currSize: " + currSize);
				continue;
			}
			if (this.results.getUploadDuration() >= this.minSendTime) {
				// TODO: do I need this?
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
		log.debug("Total Upload: " + totalUploadSize/1024/1024 + "Mb");
	}
	
	private boolean executeUploadTest(long size, MyIOHAndler io) throws IOException {
		log.trace("Upload Test...");
		this.results.setUploadSize(size);
		io.write("U", true);
		io.write(size, true);
		DataReceiver dr = new DataReceiver(io, size);
		dr.run();
		int duration = dr.getDuration() - results.getPingDuration();
		if (duration == -1) {
			log.error("negative duration: " + duration);
			return false;
		}
		this.results.setUploadDuration(duration);
		this.results.setUploadSpeed(SpeedUtils.calcSpeed(results.getUploadDuration(), size));
		return true;
	}
	
	private void calculatePingSpeed(MyIOHAndler io) throws IOException {
		executePingTest(io);
		int duration = this.results.getPingDuration();
		executePingTest(io);
		this.results.setPingDuration((int)avg(this.results.getPingDuration(), duration));
	}
	
	private void executePingTest(MyIOHAndler io) throws IOException {
		log.trace("Ping Test...");
		io.write("P", true);
		io.write(io.read(false), false);
		this.results.setPingDuration(io.readInt(true) / 2);
	}
	
	private double avg(double a, double b) {
		return (double)(a + b) / 2.0;
	}
}
