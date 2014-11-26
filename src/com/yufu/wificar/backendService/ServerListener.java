package com.yufu.wificar.backendService;

import java.util.concurrent.ArrayBlockingQueue;

import android.content.Intent;

import com.yufu.wificar.connector.Logger;
import com.yufu.wificar.connector.WifiCarClient;

public class ServerListener extends BaseWifiCarIntentService implements Logger {
	public static String KEY_HOST = "com.yufu.wificar.wifiCarConnectorIntent.host";
	public static String KEY_PORT = "com.yufu.wificar.WifiCarConnectorIntent.port";

	private static ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(100);

	WifiCarClient client;

	public ServerListener() {
		super("WifiCarConnectorIntent");
	}

	@Override
	protected void onHandleIntent(final Intent intent) {
		//starting to listen to queue
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					final String message = queue.poll();
					if (message == null) {
						sendBackCommand("stop");
					}
					else {
						sendBackCommand(message);
					}
					;
					try {
						Thread.sleep(200);
					}
					catch (final InterruptedException e) {
					}
				}
			}
		}).start();

		try {
			final String hostName = intent.getStringExtra(KEY_HOST);
			final int port = Integer.valueOf(intent.getStringExtra(KEY_PORT));

			WifiCarClient.setLogger(this);
			WifiCarClient.init(hostName, port);

			this.client = WifiCarClient.getInstance();

			Thread.sleep(1000);

			while (true) {
				System.out.println("isAlive:" + this.client.getHeartBeatSocket().isAlive());

				final String receivedMessage = this.client.readline();
				if (receivedMessage != null) {
					queue.put(receivedMessage);
				}
			}

		}
		catch (final Exception e) {
			System.err.println(e);
		}

	}

	@Override
	public void log(final String info) {
		sendBackLogMessage(info);
	}

}
