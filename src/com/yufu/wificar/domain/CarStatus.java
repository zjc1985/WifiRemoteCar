package com.yufu.wificar.domain;

import org.json.JSONException;
import org.json.JSONObject;

public class CarStatus extends JSONObject {
	private static final String key_wifi_strength = "wifi";
	private static final String key_battery = "battery";
	private static final String key_is_streaming = "isStreaming";
	private static final String key_rtspUrl = "rtspUrl";

	public CarStatus() {
		setBatteryLevel(0);
		setWifiStrength(-200);
		setIsStreaming(false);
		setRtspUrl("unknown");
	}

	public String getRtspUrl() {
		try {
			return getString(key_rtspUrl);
		}
		catch (final JSONException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public void setRtspUrl(final String url) {
		try {
			put(key_rtspUrl, url);
		}
		catch (final JSONException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public boolean isStreaming() {
		try {
			return getBoolean(key_is_streaming);
		}
		catch (final JSONException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public void setIsStreaming(final boolean isStreaming) {
		try {
			put(key_is_streaming, isStreaming);
		}
		catch (final JSONException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public int getBatteryLevel() {
		try {
			return getInt(key_battery);
		}
		catch (final JSONException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public void setBatteryLevel(final int batteryLevel) {
		try {
			put(key_battery, batteryLevel);
		}
		catch (final JSONException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public int getWifiStrength() {
		try {
			return getInt(key_wifi_strength);
		}
		catch (final JSONException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public void setWifiStrength(final int wifiStrength) {
		try {
			put(key_wifi_strength, wifiStrength);
		}
		catch (final JSONException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
}
