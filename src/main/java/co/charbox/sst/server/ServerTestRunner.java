package co.charbox.sst.server;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import co.charbox.core.utils.SpeedUtils;
import co.charbox.domain.model.mm.SSTResults;
import co.charbox.sst.SSTProperties;
import co.charbox.sst.server.results.SSTResultsHandler;
import co.charbox.sst.utils.DataReceiver;
import co.charbox.sst.utils.DataSender;
import co.charbox.sst.utils.MyIOHAndler;

public class ServerTestRunner implements Runnable {

	private final Socket client;
	private SSTResults results;
	private final int initialSize;
	private final int maxSize;
	private final List<SSTResultsHandler> handlers;

	public ServerTestRunner(Socket client, int initialSize, int maxSize, List<SSTResultsHandler> handlers) {
		this.client = client;
		this.initialSize = initialSize;
		this.maxSize = maxSize;
		this.handlers = handlers;
	}

	public void run() {
		try {
			MyIOHAndler io = new MyIOHAndler(client);
			initResults(io);
			
			calculateDownloadSpeed(io);
			calculateUploadSpeed(io);
			calculatePingSpeed(io);
			
			io.write("F");
			
			for (SSTResultsHandler handler : handlers) {
				handler.handle(results, client);
			}
			
			io.close();
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initResults(MyIOHAndler io) {
		this.results = new SSTResults()
			.setDeviceId(io.read())
			.setDeviceKey(io.read())
			.setTestStartTime(System.currentTimeMillis());
//		System.out.println("Init Results...");
//		System.out.println("\t" + results.getDeviceId() + "\t" + results.getDeviceKey());
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
		this.results.setDownloadDuration(io.readInt())
			.setDownloadSpeed(SpeedUtils.calcSpeed(results.getDownloadDuration(), size));
//		System.out.println("\t" + this.results);
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
		this.results.setUploadDuration(dr.getDuration())
			.setUploadSpeed(SpeedUtils.calcSpeed(results.getUploadDuration(), size));
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
