package com.yufu.wificar.connector;

public class DefaultLogger implements Logger {

	@Override
	public void log(final String info) {
		System.out.println(info);
	}

}
