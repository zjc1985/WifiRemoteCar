package com.yufu.wificar.backendService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

public class CarRemoteHttpServer {

	private ServerSocket socketServer;

	public CarRemoteHttpServer(int port) throws IOException {
		socketServer = new ServerSocket(3000);
	}

	// block method
	public void start() throws IOException {
		Log.i("CarRemoteHttpServer.start", "CarServer Started at port: "
				+ socketServer.getLocalPort() + " :)");
		while (true) {
			final Socket incomingSocket = socketServer.accept();
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						InputStream inStream = incomingSocket.getInputStream();
						OutputStream outStream = incomingSocket
								.getOutputStream();

						BufferedReader in = new BufferedReader(
								new InputStreamReader(inStream));
						PrintWriter out = new PrintWriter(outStream, true);

						String inputLine;
						while ((inputLine = in.readLine()) != null) {
							System.out.println(inputLine);
							if (inputLine.indexOf("POST") == 0
									&& inputLine.indexOf("HTTP") != -1) {
								String returnString = handleMessage(inputLine);
								out.println("HTTP/1.1 200 OK");
								out.println("Access-Control-Allow-Origin: *");
								out.println("");
								out.println(returnString);
								break;
							} else if (inputLine.indexOf("GET") == 0
									&& inputLine.indexOf("HTTP") != -1) {
								String returnString = handleGETMessage(inputLine);
								out.println("HTTP/1.1 200 OK");
								out.println("Content-Type:text/html");
								out.println("Access-Control-Allow-Origin: *");
								out.println("");
								out.println(returnString);
								break;
							}
						}
						incomingSocket.close();
					} catch (Exception e) {
					}

				}
			}).start();
		}
	}

	protected String handleGETMessage(String inputLine) {
		return "";
	}

	private String handleMessage(String inputLine) {
		String result = "unknown car command";
		if (inputLine.indexOf("/car/forward") != -1) {
			carForward();
			result = "car forward";
		} else if (inputLine.indexOf("/car/left") != -1) {
			carLeft();
			result = "car left";
		} else if (inputLine.indexOf("/car/right") != -1) {
			carRight();
			result = "car right";
		} else if (inputLine.indexOf("/car/back") != -1) {
			carBack();
			result = "car back";
		} else if (inputLine.indexOf("/car/stop") != -1) {
			carStop();
			result = "car stop";
		}
		return result;
	}

	protected void carStop() {
		// TODO Auto-generated method stub

	}

	protected void carBack() {
		// TODO Auto-generated method stub

	}

	protected void carRight() {
		// TODO Auto-generated method stub

	}

	protected void carLeft() {
		// TODO Auto-generated method stub

	}

	protected void carForward() {
		// TODO Auto-generated method stub

	}
}
