package com.yufu.wificar.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import org.apache.http.conn.util.InetAddressUtils;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;


public class Utils {

	/**
	 * Convert byte array to hex string
	 * @param bytes
	 * @return
	 */
	public static String bytesToHex(final byte[] bytes) {
		final StringBuilder sbuf = new StringBuilder();
		for (final byte b : bytes) {
			final int intVal = b & 0xff;
			if (intVal < 0x10) {
				sbuf.append("0");
			}
			sbuf.append(Integer.toHexString(intVal).toUpperCase());
		}
		return sbuf.toString();
	}

	/**
	 * Get utf8 byte array.
	 * @param str
	 * @return  array of NULL if error was found
	 */
	public static byte[] getUTF8Bytes(final String str) {
		try {
			return str.getBytes("UTF-8");
		}
		catch (final Exception ex) {
			return null;
		}
	}

	/**
	 * Load UTF8withBOM or any ansi text file.
	 * @param filename
	 * @return  
	 * @throws java.io.IOException
	 */
	public static String loadFileAsString(final String filename) throws java.io.IOException {
		final int BUFLEN = 1024;
		final BufferedInputStream is = new BufferedInputStream(new FileInputStream(filename), BUFLEN);
		try {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFLEN);
			final byte[] bytes = new byte[BUFLEN];
			boolean isUTF8 = false;
			int read, count = 0;
			while ((read = is.read(bytes)) != -1) {
				if ((count == 0) && (bytes[0] == (byte) 0xEF) && (bytes[1] == (byte) 0xBB) && (bytes[2] == (byte) 0xBF)) {
					isUTF8 = true;
					baos.write(bytes, 3, read - 3); // drop UTF8 bom marker
				}
				else {
					baos.write(bytes, 0, read);
				}
				count += read;
			}
			return isUTF8 ? new String(baos.toByteArray(), "UTF-8") : new String(baos.toByteArray());
		}
		finally {
			try {
				is.close();
			}
			catch (final Exception ex) {
			}
		}
	}

	/**
	 * Get IP address from first non-localhost interface
	 * @param ipv4  true=return ipv4, false=return ipv6
	 * @return  address or empty string
	 */
	public static String getIPAddress(final boolean useIPv4) {
		try {
			final List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (final NetworkInterface intf : interfaces) {
				final List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
				for (final InetAddress addr : addrs) {
					if (!addr.isLoopbackAddress()) {
						final String sAddr = addr.getHostAddress().toUpperCase();
						final boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
						if (useIPv4) {
							if (isIPv4) {
								return sAddr;
							}
						}
						else {
							if (!isIPv4) {
								final int delim = sAddr.indexOf('%'); // drop ip6 port suffix
								return delim < 0 ? sAddr : sAddr.substring(0, delim);
							}
						}
					}
				}
			}
		}
		catch (final Exception ex) {
		} // for now eat exceptions
		return "";
	}

	public static void sendBackLogMessage(final String message, final Context context) {
		final Intent localIntent = new Intent(Constants.BROADCAST_ACTION);
		localIntent.putExtra(Constants.EXTENDED_DATA_STATUS, message);
		LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
	}
}
