package com.yufu.wificar.backendService;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import android.content.Intent;

public class HttpServerIntentService extends BaseWifiCarIntentService {
	public static String KEY_START_INTENT_MESSAGE = "com.yufu.httpServerIntentService.message";

	private CarRemoteHttpServer server;
	private String indexHtml;

	private static ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(100);

	public HttpServerIntentService() {
		super("HttpServerIntentService");
	}

	@Override
	protected void onHandleIntent(final Intent intent) {
		this.indexHtml = intent.getStringExtra(KEY_START_INTENT_MESSAGE);
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
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}).start();

		//starting httpServer
		try {
			this.server = new CarRemoteHttpServer(3000) {

				@Override
				protected String handleGETMessage(final String inputLine) {
					if (inputLine.indexOf("car/index.html") != -1) {
						return HttpServerIntentService.this.indexHtml;
					}
					else {
						return "";
					}
				}

				@Override
				protected void carStop() {
					try {
						queue.put("stop");
					}
					catch (final InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					// sendBackMessage("stop");
				}

				@Override
				protected void carBack() {
					try {
						queue.put("back");
					}
					catch (final InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// sendBackMessage("back");
				}

				@Override
				protected void carRight() {
					try {
						queue.put("right");
					}
					catch (final InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// sendBackMessage("right");
				}

				@Override
				protected void carLeft() {
					try {
						queue.put("left");
					}
					catch (final InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// sendBackMessage("left");
				}

				@Override
				protected void carForward() {
					try {
						queue.put("go");
					}
					catch (final InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// sendBackMessage("go");
				}

			};
		}
		catch (final IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		sendBackLogMessage("server started at port:" + 3000);
		try {
			this.server.start();
		}
		catch (final IOException e) {
			sendBackLogMessage("error message:" + e.getMessage());
		}
	}
}
