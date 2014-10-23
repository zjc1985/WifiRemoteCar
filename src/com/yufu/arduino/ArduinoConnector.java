package com.yufu.arduino;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class ArduinoConnector {
	private final static String DATA_RECEIVED_INTENT = "primavera.arduino.intent.action.DATA_RECEIVED";
	private final static String SEND_DATA_INTENT = "primavera.arduino.intent.action.SEND_DATA";
	private final static String DATA_EXTRA = "primavera.arduino.intent.extra.DATA";

	private Activity activity;
	private String receivedString;
	
	public ArduinoConnector(Activity activity,final MessageHandler handler){
		this.activity=activity;
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(DATA_RECEIVED_INTENT);
		this.activity.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final String action = intent.getAction();
				if (DATA_RECEIVED_INTENT.equals(action)) {
					byte[] data = intent.getByteArrayExtra(DATA_EXTRA);
					receivedString+=new String(data);
					if(receivedString.indexOf("\n")!=-1){
						handler.handle(receivedString);
						receivedString="";
					}
					
				}
			}
		}, filter);
		
	}
	
	public void send2Arduino(String message){
		Intent intent=new Intent(SEND_DATA_INTENT);
		intent.putExtra(DATA_EXTRA, message.getBytes());
		activity.sendBroadcast(intent);
	}
}
