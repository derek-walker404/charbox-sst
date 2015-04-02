package co.charbox.sst.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.joda.time.DateTime;

import co.charbox.core.utils.Config;
import co.charbox.sst.server.results.ConsoleSSTResultsHandler;
import co.charbox.sst.server.results.ElasticsearchSSTResultsHandler;
import co.charbox.sst.server.results.SSTResultsHandler;

import com.google.common.collect.Lists;

public class SSTServer implements Runnable {

	private final ServerSocket sock;
	private final int initialSize;
	private final int maxSize;
	private final ThreadPoolExecutor es;
	private final List<SSTResultsHandler> handlers;
	private DateTime lastClientTime;
	
	public SSTServer(List<SSTResultsHandler> handlers) throws IOException {
		Config config = Config.get();
		sock = new ServerSocket(config.getInt("sst.socket.port"));
		this.initialSize = config.getInt("sst.initialSize");
		this.maxSize = config.getInt("sst.maxSize");
		this.es = (ThreadPoolExecutor) Executors.newFixedThreadPool(config.getInt("sst.executor.threadCount"));
		this.handlers = handlers;
	}
	
	@Override
	public String toString() {
		return "SSTServer [intialSize=" + initialSize + ", maxSize=" + maxSize
				+ "]";
	}
	
	public DateTime getLastClientConnectTime() {
		return lastClientTime;
	}
	
	public int getActiveTests() {
		return es.getActiveCount();
	}
	
	public int getQueueSize() {
		return es.getQueue().size();
	}

	public void run() {
		while (true) {
			try {
				Socket client = sock.accept();
				lastClientTime = new DateTime();
				System.out.println("Just connected to "
		                  + client.getRemoteSocketAddress());
				
	            es.execute(new ServerTestRunner(client, initialSize, maxSize, handlers));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		List<SSTResultsHandler> handlers = Lists.newArrayList();
		
		handlers.add(new ConsoleSSTResultsHandler());
		handlers.add(new ElasticsearchSSTResultsHandler(null));
		
		new SSTServer(handlers).run();
	}
}
