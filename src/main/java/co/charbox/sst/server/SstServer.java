package co.charbox.sst.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import co.charbox.client.sst.results.SstResultsHandler;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.tpofof.core.App;
import com.tpofof.core.utils.Config;

@Slf4j
@Component
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class SstServer implements Runnable {

	private ServerSocket sock;
	private final int initialSize;
	private final int minSendTime;
	private final int maxExecutionTime;
	private final ThreadPoolExecutor testExecutorPool;
	private final List<SstResultsHandler> handlers;
	private final Map<ServerTestRunner, Future<?>> runnerMap;
	private DateTime lastClientTime;
	@Autowired SstChartbotApiClient charbotApiClient;
	
	@Autowired
	public SstServer(Config config, List<SstResultsHandler> resultsHandlerMasterList) throws IOException {
		sock = new ServerSocket(config.getInt("sst.socket.port")); // TODO: move to bean configuration file
		this.initialSize = config.getInt("sst.initialSize", 6000);
		this.minSendTime = config.getInt("sst.minSendTime", 3000);
		this.maxExecutionTime = config.getInt("sst.maxExecutionTime", 40*1000);
		this.testExecutorPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(config.getInt("sst.executor.threadCount"));
		this.runnerMap = Maps.newHashMap();
		this.handlers = resultsHandlerMasterList;
	}
	
	@Override
	public String toString() {
		return "SSTServer [intialSize=" + initialSize + ", minSendTime=" + minSendTime
				+ "]";
	}
	
	public DateTime getLastClientConnectTime() {
		return lastClientTime;
	}
	
	public int getActiveTests() {
		return testExecutorPool.getActiveCount();
	}
	
	public int getQueueSize() {
		return testExecutorPool.getQueue().size();
	}

	public void run() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Queue<ServerTestRunner> toDelete = new LinkedList<ServerTestRunner>();
				while (true) {
					DateTime currTime = new DateTime();
					for (Entry<ServerTestRunner, Future<?>> e : runnerMap.entrySet()) {
						ServerTestRunner runner = e.getKey();
						if (!runner.isRunning() || (runner.getExpiration() != null && currTime.compareTo(runner.getExpiration()) >= 0)) {
							if (runner.isRunning()) {
								log.warn("Speed test timeout!");
								try {
									runner.getSocket().close();
								} catch (IOException e1) {
									e1.printStackTrace();
								}
							}
							e.getValue().cancel(true);
							toDelete.add(runner);
						}
					}
					while (!toDelete.isEmpty()) {
						runnerMap.remove(toDelete.poll());
					}
				}
			}
		}).start();
		while (true) {
			try {
				Socket client = sock.accept();
				lastClientTime = new DateTime();
				log.debug("Just connected to "
		                  + client.getRemoteSocketAddress());
				
				ServerTestRunner runner = ServerTestRunner.builder()
		        		.client(client)
		        		.initialSize(initialSize)
		        		.minSendTime(minSendTime)
		        		.maxExecutionTime(maxExecutionTime)
		        		.running(new AtomicBoolean(true))
		        		.handlers(handlers)
		        		.charbotApiClient(charbotApiClient)
		        		.build();
	            Future<?> future = testExecutorPool.submit(runner);
	            runnerMap.put(runner, future);
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
