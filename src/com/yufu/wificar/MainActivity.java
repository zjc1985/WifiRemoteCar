package com.yufu.wificar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.yufu.arduino.ArduinoConnector;
import com.yufu.arduino.MessageHandler;
import com.yufu.wificar.backendService.ServerListener;
import com.yufu.wificar.connector.WifiCarClient;
import com.yufu.wificar.domain.CarStatus;
import com.yufu.wificar.util.Constants;
import com.yufu.wificar.util.Utils;

public class MainActivity extends Activity {
	private ArduinoConnector arduinoConnector;
	private String carIndexHtmlString = "";
	private static CarStatus carStatus = new CarStatus();

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//init switch
		final Switch serverSwitch = (Switch) findViewById(R.id.switch1);
		serverSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
				if (isChecked) {
					switchChecked();
				}
				else {
					switchNotChecked();
				}
			}

		});

		//init logTextView
		final TextView logTView = (TextView) findViewById(R.id.logTextView);
		logTView.setMovementMethod(new ScrollingMovementMethod());

		//init logTextView
		final TextView logTView2 = (TextView) findViewById(R.id.logTextView2);
		logTView2.setMovementMethod(new ScrollingMovementMethod());

		// read index.html from assets
		try {
			this.carIndexHtmlString = readFile("index.html");
		}
		catch (final IOException e) {
			e.printStackTrace();
		}

		// init arduino connector
		this.arduinoConnector = new ArduinoConnector(this, new MessageHandler() {
			@Override
			public void handle(final String message) {
				//handle arduino message here;
				//logInBox(message);
			}
		});

		// show ip address
		final RadioButton radioButton = (RadioButton) findViewById(R.id.radio0);
		radioButton.setText("This Device itself will act as a server. at " + Utils.getIPAddress(true));

		//local broadcast receiver
		final IntentFilter localFilter = new IntentFilter();
		localFilter.addAction(Constants.BROADCAST_ACTION);
		localFilter.addAction(Intent.ACTION_BATTERY_CHANGED);

		LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(final Context context, final Intent intent) {
				if (intent.hasExtra(Constants.EXTENDED_DATA_STATUS)) {
					logInBox(intent.getStringExtra(Constants.EXTENDED_DATA_STATUS));
				}
				//will send to arduino
				if (intent.hasExtra(Constants.EXTENDED_DATA_MESSAGE)) {
					final String message = intent.getStringExtra(Constants.EXTENDED_DATA_MESSAGE);
					logInBox2("send command: " + message + " to arduino");
					MainActivity.this.arduinoConnector.send2Arduino(message);
				}

			}
		}, localFilter);

		//
		final IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);

		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(final Context context, final Intent intent) {
				if (intent.hasExtra(BatteryManager.EXTRA_LEVEL)) {
					carStatus.setBatteryLevel(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0));
				}
			}
		}, filter);

	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		final int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void start(final View view) {

		/*
		System.out.println("prepare to send message");

		if (WifiCarClient.getInstance() != null) {
			try {
				WifiCarClient.getInstance().send("server message");
			}
			catch (final InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		*/
		logInBox("wifi strength:" + getWifiStrength());

	}

	private void switchNotChecked() {
		final RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
		radioGroup.setEnabled(true);
	}

	private void switchChecked() {
		final Switch s = (Switch) findViewById(R.id.switch1);
		s.setEnabled(false);

		final RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
		radioGroup.setEnabled(false);

		final Intent startServerListenerIntent = new Intent(this, ServerListener.class);
		final String hostName = ((EditText) findViewById(R.id.hostEditText)).getText().toString();
		final String port = ((EditText) findViewById(R.id.portEditText)).getText().toString();

		startServerListenerIntent.putExtra(ServerListener.KEY_HOST, hostName);
		startServerListenerIntent.putExtra(ServerListener.KEY_PORT, port);

		startService(startServerListenerIntent);

		//init car status sender
		final Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				final WifiCarClient client = WifiCarClient.getInstance();
				if ((client != null) && client.getHeartBeatSocket().isAlive()) {
					carStatus.setWifiStrength(getWifiStrength());
					try {
						System.out.println(carStatus.toJSONString());
						client.send(carStatus.toJSONString());
					}
					catch (final InterruptedException e) {

					}
				}

			}
		}, 1000, 5000);
	}

	protected void logInBox(final String message) {
		final TextView tView = (TextView) findViewById(R.id.logTextView);
		if (tView.getLineCount() > 100) {
			tView.setText("");
		}
		tView.setText(message + "\n" + tView.getText().toString());
	}

	protected void logInBox2(final String message) {
		final TextView tView = (TextView) findViewById(R.id.logTextView2);
		if (tView.getLineCount() > 100) {
			tView.setText("");
		}
		tView.setText(message + "\n" + tView.getText().toString());
	}

	private String readFile(final String filename) throws IOException {
		final InputStreamReader inputReader = new InputStreamReader(getResources().getAssets().open(filename));
		final BufferedReader bufReader = new BufferedReader(inputReader);

		String line = "";
		String Result = "";
		//StringBuilder sb = new StringBuilder();
		while ((line = bufReader.readLine()) != null) {
			Result += line;
		}
		bufReader.close();
		return Result;
	}

	protected int getWifiStrength() {
		final WifiManager wifi_service = (WifiManager) getSystemService(WIFI_SERVICE);
		final WifiInfo wifiInfo = wifi_service.getConnectionInfo();
		return wifiInfo.getRssi();
	}

	public void sendBackLogMessage(final String message) {
		final Intent localIntent = new Intent(Constants.BROADCAST_ACTION);
		localIntent.putExtra(Constants.EXTENDED_DATA_STATUS, message);
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	}
}
