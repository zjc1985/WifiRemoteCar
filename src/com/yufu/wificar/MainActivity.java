package com.yufu.wificar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspClient;
import net.majorkernelpanic.streaming.video.VideoQuality;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
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

public class MainActivity extends Activity implements SurfaceHolder.Callback, Session.Callback, RtspClient.Callback {
	private ArduinoConnector arduinoConnector;
	private String carIndexHtmlString = "";
	private static CarStatus carStatus = new CarStatus();

	private SurfaceView mSurfaceView;
	private Session mSession;
	private RtspClient mClient;
	private EditText rtspUriEditText;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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
		radioButton.setText(radioButton.getText() + " at " + Utils.getIPAddress(true));

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
				if (intent.hasExtra(Constants.EXTENDED_DATA_MESSAGE)) {
					final String command = intent.getStringExtra(Constants.EXTENDED_DATA_MESSAGE);
					processCommand(command);
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

		this.rtspUriEditText = (EditText) findViewById(R.id.rtspUriEText);
		this.mSurfaceView = (SurfaceView) findViewById(R.id.surface);
		this.mSession = SessionBuilder.getInstance().setContext(getApplicationContext()).setAudioEncoder(SessionBuilder.AUDIO_AAC).setAudioQuality(new AudioQuality(8000, 16000))
		        .setVideoEncoder(SessionBuilder.VIDEO_H264).setSurfaceView(this.mSurfaceView).setPreviewOrientation(0).setCallback(this).setVideoQuality(new VideoQuality(176, 144, 15, 50 * 1000))
		        .build();
		this.mSession.switchCamera();
		this.mClient = new RtspClient();
		this.mClient.setSession(this.mSession);
		this.mClient.setCallback(this);
		this.mSurfaceView.getHolder().addCallback(this);
	}

	private void processCommand(final String command) {
		logInBox2("received command: " + command);
		if (command.equalsIgnoreCase("toggleStream")) {
			toggleStream();
		}
		else {
			MainActivity.this.arduinoConnector.send2Arduino(command);
		}

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
		logInBox("wifi strength:" + getWifiStrength());
	}

	public void toggle(final View view) {
		toggleStream();
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
						System.out.println(carStatus.toString());
						client.send(carStatus.toString());
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

	@Override
	public void onRtspUpdate(final int message, final Exception exception) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBitrateUpdate(final long bitrate) {
		final TextView bitRateView = (TextView) findViewById(R.id.bitRateTView);
		bitRateView.setText("" + (bitrate / 1000) + " kbps");
	}

	@Override
	public void onSessionError(final int reason, final int streamType, final Exception e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPreviewStarted() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSessionConfigured() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSessionStarted() {
		final Button btn = (Button) findViewById(R.id.ToggleStreamBtn);
		btn.setText("Stop Stream");
		carStatus.setIsStreaming(true);
		carStatus.setRtspUrl(this.rtspUriEditText.getText().toString());
	}

	@Override
	public void onSessionStopped() {
		final Button btn = (Button) findViewById(R.id.ToggleStreamBtn);
		btn.setText("Start Stream");
		carStatus.setIsStreaming(false);
		carStatus.setRtspUrl("Unknown");
	}

	@Override
	public void surfaceChanged(final SurfaceHolder arg0, final int arg1, final int arg2, final int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceCreated(final SurfaceHolder arg0) {
		//this.mSession.startPreview();
	}

	@Override
	public void surfaceDestroyed(final SurfaceHolder arg0) {
		this.mClient.stopStream();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.mClient.release();
		this.mSession.release();
		this.mSurfaceView.getHolder().removeCallback(this);
	}

	public void toggleStream() {
		//mProgressBar.setVisibility(View.VISIBLE);
		if (!this.mClient.isStreaming()) {
			String ip, port, path;

			// We save the content user inputs in Shared Preferences
			final SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
			final Editor editor = mPrefs.edit();
			editor.putString("uri", this.rtspUriEditText.getText().toString());

			editor.commit();

			// We parse the URI written in the Editext
			final Pattern uri = Pattern.compile("rtsp://(.+):(\\d*)/(.+)");
			final Matcher m = uri.matcher(this.rtspUriEditText.getText());
			m.find();
			ip = m.group(1);
			port = m.group(2);
			path = m.group(3);

			//this.mClient.setCredentials(this.mEditTextUsername.getText().toString(), this.mEditTextPassword.getText().toString());
			this.mClient.setServerAddress(ip, Integer.parseInt(port));
			this.mClient.setStreamPath("/" + path);
			this.mClient.startStream();

		}
		else {
			// Stops the stream and disconnects from the RTSP server
			this.mClient.stopStream();
		}
	}

}
