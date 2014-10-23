package com.yufu.wificar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.yufu.arduino.ArduinoConnector;
import com.yufu.arduino.MessageHandler;
import com.yufu.wificar.util.Utils;

import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {
	private ArduinoConnector arduinoConnector;
	private String carIndexHtmlString = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// read index.html from assets
		try {
			this.carIndexHtmlString = readFile("index.html");
		} catch (IOException e) {
			e.printStackTrace();
		}

		// init arduino connector
		this.arduinoConnector = new ArduinoConnector(this,
				new MessageHandler() {

					@Override
					public void handle(String message) {
						TextView tView = (TextView) findViewById(R.id.arduinoTView);
						tView.setText(message);
					}
				});

		// show ip address
		TextView ipTView = (TextView) findViewById(R.id.ipTView);
		ipTView.setText(Utils.getIPAddress(true));

		// listen to backend service
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.BROADCAST_ACTION);

		LocalBroadcastManager.getInstance(this).registerReceiver(
				new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {

						if (intent.hasExtra(Constants.EXTENDED_DATA_STATUS)) {
							TextView logTView = (TextView) findViewById(R.id.logTView);
							logTView.setText(logTView.getText().toString()
									+ "\n"
									+ intent.getStringExtra(Constants.EXTENDED_DATA_STATUS));
						}
						if (intent.hasExtra(Constants.EXTENDED_DATA_MESSAGE)) {
							String message = intent
									.getStringExtra(Constants.EXTENDED_DATA_MESSAGE);
							TextView tView = (TextView) findViewById(R.id.arduinoTView);
							tView.setText(tView.getText().toString() + "\n"
									+ message);
							arduinoConnector.send2Arduino(message);
						}

					}
				}, filter);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void start(View view) {

		Intent httpIntent = new Intent(this, HttpServerIntentService.class);
		httpIntent.putExtra(HttpServerIntentService.KEY_START_INTENT_MESSAGE,
				this.carIndexHtmlString);
		startService(httpIntent);
	}

	private String readFile(String filename) throws IOException {
		InputStreamReader inputReader = new InputStreamReader(getResources()
				.getAssets().open(filename));
		BufferedReader bufReader = new BufferedReader(inputReader);

		String line = "";
		String Result = "";
		//StringBuilder sb = new StringBuilder();
		while ((line = bufReader.readLine()) != null) {
			Result += line;
		}
		bufReader.close();
		return Result;
	}
}
