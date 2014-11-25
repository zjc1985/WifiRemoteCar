package com.yufu.wificar.domain;

public class CarStatus {
	private int batteryLevel;
	private int wifiStrength;

	public CarStatus() {
		this.batteryLevel = 0;
		this.wifiStrength = -200;
	}

	public int getBatteryLevel() {
		return this.batteryLevel;
	}

	public void setBatteryLevel(final int batteryLevel) {
		this.batteryLevel = batteryLevel;
	}

	public int getWifiStrength() {
		return this.wifiStrength;
	}

	public void setWifiStrength(final int wifiStrength) {
		this.wifiStrength = wifiStrength;
	}

	public String toJSONString() {
		return "{\"wifi\":" + this.wifiStrength + ",\"battery\":" + this.batteryLevel + "}";
	}
}
