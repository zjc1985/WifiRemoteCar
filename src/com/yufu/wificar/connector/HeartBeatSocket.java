package com.yufu.wificar.connector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketAddress;

public class HeartBeatSocket {
	private static Logger logger = new DefaultLogger();

	private boolean isAlive = false;
	private PrintWriter writer;
	private BufferedReader reader;
	private final SocketAddress address;
	private Socket socket;
	private final long interval;

	public HeartBeatSocket(final SocketAddress address, final long interval) {
		this.address = address;
		this.interval = interval;
		initSocket();
	}

	private void initSocket() {

		setAlive(false);

		//try to init socket, will retry in 3s
		while (true) {
			log("try to init socket");
			try {
				this.socket = new Socket();
				this.socket.setKeepAlive(true);
				this.socket.setTcpNoDelay(true);
				this.socket.setPerformancePreferences(0, 2, 1);

				this.socket.connect(this.address, 4000);
				log("connected to " + this.address.toString());
				setAlive(true);
				break;
			}
			catch (final Exception e) {
				log("init socket failed " + e.getMessage());
				try {
					Thread.sleep(3000);
				}
				catch (final InterruptedException e1) {
				}
			}

		}

		if (!this.socket.isClosed() && this.socket.isConnected()) {

			setAlive(true);

			try {
				this.writer = new PrintWriter(this.socket.getOutputStream());
				this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

			}
			catch (final Exception e) {
				log("get writer or reader failed");
				setAlive(false);
			}

			startHeartBeatThread();
		}
		else {
			log("need live socket to init");
		}

	}

	private void startHeartBeatThread() {
		//start thread to send heartbeat
		new Thread(new Runnable() {
			@Override
			public void run() {
				log("start socket heartBeat Thread");
				while (isAlive()) {
					try {
						Thread.sleep(HeartBeatSocket.this.interval);
					}
					catch (final InterruptedException e) {
					}

					try {
						HeartBeatSocket.this.socket.sendUrgentData(0);
					}
					catch (final Exception e) {
						log("dead connection detected,prepare to close");
						setAlive(false);
						try {
							HeartBeatSocket.this.writer.close();
							HeartBeatSocket.this.reader.close();
							HeartBeatSocket.this.socket.close();
						}
						catch (final IOException e1) {
						}
					}
				}

				initSocket();
				log("prepare to exit heartBeat Thread");
			}
		}).start();
	}

	public void writeLine(final String message) {
		if (isAlive()) {
			this.writer.println(message);
			this.writer.flush();
		}
	}

	public String readLine() {
		String returnValue = null;

		if (isAlive()) {
			try {
				returnValue = this.reader.readLine();
			}
			catch (final IOException e) {

			}
		}

		return returnValue;
	}

	public synchronized boolean isAlive() {
		return this.isAlive;
	}

	public synchronized void setAlive(final boolean isAlive) {
		this.isAlive = isAlive;
		log("set isAlive to " + isAlive);
	}

	public static void setLogger(final Logger logger) {
		HeartBeatSocket.logger = logger;
	}

	private void log(final String info) {
		HeartBeatSocket.logger.log("HeartBeatSocket: " + info);
	}

}
