package com.example.eth2wifi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class ETH2WiFi extends AppCompatActivity {
    private static final String TAG = "ETH2WiFi";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // SPP UUID
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private TextView statusTextView;
    private ScrollView scrollView;
    private StringBuilder messageBuffer = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.statusTextView);
        scrollView = findViewById(R.id.scrollView);
        Button configureWiFiButton = findViewById(R.id.configureWiFiButton);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth is not supported on this device.");
            return;
        }

        configureWiFiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configureWiFi();
            }
        });

        registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        connectToBluetoothDevice();
    }

    private void connectToBluetoothDevice() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals("ESP32ETH2WiFi")) { // Replace with your device name
                try {
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    bluetoothSocket.connect();
                    Log.i(TAG, "Connected to Bluetooth device");
                    listenForMessages();
                } catch (IOException e) {
                    Log.e(TAG, "Connection failed", e);
                }
            }
        }
    }

    private void listenForMessages() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream inputStream = bluetoothSocket.getInputStream();
                    byte[] buffer = new byte[1024];
                    int bytes;

                    while ((bytes = inputStream.read(buffer)) != -1) {
                        String message = new String(buffer, 0, bytes);
                        appendMessage(message);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading input", e);
                }
            }
        }).start();
    }

    private void appendMessage(String message) {
        messageBuffer.append(message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusTextView.setText(messageBuffer.toString());
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Handle Bluetooth device discovery results if needed
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket", e);
        }
        unregisterReceiver(bluetoothReceiver);
    }

    private void configureWiFi() {
        // Implement Wi-Fi configuration logic
        // You can launch a new activity or a dialog for user to input SSID and Password
    }
}
