package com.yufu.wificar;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

public class HttpServerIntentService extends IntentService {
	public static String KEY_START_INTENT_MESSAGE = "com.yufu.httpServerIntentService.message";

	private CarRemoteHttpServer server;
	private String indexHtml;

	private static ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(
			100);

	public HttpServerIntentService() {
		super("HttpServerIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		this.indexHtml = intent.getStringExtra(KEY_START_INTENT_MESSAGE);
		//starting to listen to queue
		new Thread(new Runnable() {			
			@Override
			public void run() {
				while(true){
					String message=queue.poll();
					if(message==null){
						sendBackMessage("stop");
					}else{
						sendBackMessage(message);
					};
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}				
			}
		}).start();
		
		//starting httpServer
		try {
			server = new CarRemoteHttpServer(3000) {

				@Override
				protected String handleGETMessage(String inputLine) {
					if (inputLine.indexOf("car/index.html") != -1) {
						return indexHtml;
					} else {
						return "";
					}
				}

				@Override
				protected void carStop() {
					try {
						queue.put("stop");
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					// sendBackMessage("stop");
				}

				@Override
				protected void carBack() {
					try {
						queue.put("back");
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// sendBackMessage("back");
				}

				@Override
				protected void carRight() {
					try {
						queue.put("right");
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// sendBackMessage("right");
				}

				@Override
				protected void carLeft() {
					try {
						queue.put("left");
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// sendBackMessage("left");
				}

				@Override
				protected void carForward() {
					try {
						queue.put("go");
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// sendBackMessage("go");
				}

			};
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		sendBackLogMessage("server started at port:" + 3000);
		try {
			this.server.start();
		} catch (IOException e) {
			sendBackLogMessage("error message:" + e.getMessage());
		}
	}

	private void sendBackLogMessage(String message) {
		Intent localIntent = new Intent(Constants.BROADCAST_ACTION);
		localIntent.putExtra(Constants.EXTENDED_DATA_STATUS, message);
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	}

	private void sendBackMessage(String message) {
		Intent localIntent = new Intent(Constants.BROADCAST_ACTION);
		localIntent.putExtra(Constants.EXTENDED_DATA_MESSAGE, message);
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	}
}
