package com.yufu.wificar.backendService;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.yufu.wificar.util.Constants;

public abstract class BaseWifiCarIntentService extends IntentService {

	public BaseWifiCarIntentService(final String name) {
		super(name);
	}

	//this message will be sent back for log or show status
	protected void sendBackLogMessage(final String message) {
		final Intent localIntent = new Intent(Constants.BROADCAST_ACTION);
		localIntent.putExtra(Constants.EXTENDED_DATA_STATUS, message);
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	}

	protected void sendBackCommand(final String message) {
		final Intent localIntent = new Intent(Constants.BROADCAST_ACTION);
		localIntent.putExtra(Constants.EXTENDED_DATA_MESSAGE, message);
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	}

}
