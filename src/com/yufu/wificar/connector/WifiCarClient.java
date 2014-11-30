package com.yufu.wificar.connector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;

import com.yufu.wificar.util.Constants;

public class WifiCarClient {
	private static ArrayBlockingQueue<String> sendQueue = new ArrayBlockingQueue<String>(100);
	private final HeartBeatSocket heartBeatSocket;
	private final SocketAddress address;
	private static WifiCarClient instance;
	private static Logger logger = new DefaultLogger();

	private WifiCarClient(final String hostName, final int port) throws UnknownHostException, IOException {
		this.address = new InetSocketAddress(hostName, port);

		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						final String message = sendQueue.take();
						WifiCarClient.this.heartBeatSocket.writeLine(message);
					}
					catch (final InterruptedException e) {

					}
				}
			}
		}).start();

		this.heartBeatSocket = new HeartBeatSocket(this.address, Constants.HEART_BEART_INTERVAL);
	}

	public static void init(final String hostName, final int port) throws UnknownHostException, IOException {
		if (instance == null) {
			synchronized (WifiCarClient.class) {
				if (instance == null) {
					instance = new WifiCarClient(hostName, port);
				}
			}
		}
	}

	public static WifiCarClient getInstance() {
		return instance;
	}

	public synchronized void send(final String message) throws InterruptedException {
		sendQueue.put(message);
	}

	public String readline() {
		return this.heartBeatSocket.readLine();
	}

	public HeartBeatSocket getHeartBeatSocket() {
		return this.heartBeatSocket;
	}

	public static void setLogger(final Logger logger) {
		WifiCarClient.logger = logger;
		HeartBeatSocket.setLogger(logger);
	}

	public static Logger getLogger() {
		return WifiCarClient.logger;
	}
}
