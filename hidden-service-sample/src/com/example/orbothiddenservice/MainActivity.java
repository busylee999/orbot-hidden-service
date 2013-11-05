package com.example.orbothiddenservice;

import info.guardianproject.onionkit.ui.OrbotHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {

	final static String HOST_NAME = "hs_host";
	final static int HS_PORT = 9062;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		startTorService();
	}

	protected void setMyTorDomain(String hsHostName) {
		((TextView) findViewById(R.id.tvTorDomain)).setText(hsHostName);
	}

	protected void startServerSocket(int port) throws IOException {
		ServerSocket ssc = new ServerSocket();
		ssc.bind(new InetSocketAddress(port));
		Socket socket = ssc.accept();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				socket.getOutputStream()));
		System.out.println("ip: " + socket.getInetAddress());
		while (socket.isConnected()) {
			String line = null;
			while ((line = in.readLine()) != null) {
				out.write(line);
			}
		}

		// ServerSocketChannel ssc = ServerSocketChannel.open();
		// ssc.socket().setReuseAddress(true);
		// ssc.socket().bind(new InetSocketAddress(port));
		// ssc.configureBlocking(false);
	}

	protected void startTorService() {
		OrbotHelper oh = new OrbotHelper(this);
		oh.requestOrbotStart(this);
	}

	protected void openHS(int port) {
		OrbotHelper oh = new OrbotHelper(this);
		oh.requestHiddenServiceOnPort(this, port);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null) {
			String hs_host = data.getStringExtra(HOST_NAME);
			setMyTorDomain(hs_host != null ? hs_host : "not defined");
			Log.i("HOST_NAME", hs_host != null ? hs_host : "null");
			(new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						startServerSocket(HS_PORT);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			})).start();

		}

		if (requestCode == 1) {
			openHS(HS_PORT);

			return;
		}

	}
}