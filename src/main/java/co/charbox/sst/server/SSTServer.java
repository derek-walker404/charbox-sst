package co.charbox.sst.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import co.charbox.sst.server.results.SstResultsHandler;

import com.tpofof.core.App;
import com.tpofof.core.utils.Config;

@Component
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class SstServer implements Runnable {

	private ServerSocket sock;
	private final int initialSize;
	private final int maxSize;
	private final ThreadPoolExecutor es;
	private final List<SstResultsHandler> handlers;
	private DateTime lastClientTime;
	@Autowired SstChartbotApiClient charbotApiClient;
	
	@Autowired
	public SstServer(Config config, List<SstResultsHandler> resultsHandlerMasterList) throws IOException {
		sock = new ServerSocket(config.getInt("sst.socket.port")); // TODO: move to bean configuration file
		this.initialSize = config.getInt("sst.initialSize");
		this.maxSize = config.getInt("sst.maxSize");
		this.es = (ThreadPoolExecutor) Executors.newFixedThreadPool(config.getInt("sst.executor.threadCount"));
		this.handlers = resultsHandlerMasterList;
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
				
	            es.execute(ServerTestRunner.builder()
	            		.client(client)
	            		.initialSize(initialSize)
	            		.maxSize(maxSize)
	            		.handlers(handlers)
	            		.charbotApiClient(charbotApiClient)
	            		.build());
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
		App.getContext().getBean(SstServer.class).run();
	}
}
