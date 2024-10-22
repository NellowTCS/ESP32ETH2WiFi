package com.example.eth2wifi; // Change to your new package name

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

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "ETH2WiFi";
    private static final String DEVICE_ADDRESS = "00:00:00:00:00:00"; // Replace with your Bluetooth device address
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Replace with your UUID

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;

    private TextView statusTextView;
    private ScrollView scrollView;
    private Button connectButton;
    private Button configureWiFiButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.statusTextView);
        scrollView = findViewById(R.id.scrollView);
        connectButton = findViewById(R.id.connectButton);
        configureWiFiButton = findViewById(R.id.configureWiFiButton);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBluetoothSupport();

        connectButton.setOnClickListener(v -> connectToBluetoothDevice());
        configureWiFiButton.setOnClickListener(v -> configureWiFi());
    }

    private void checkBluetoothSupport() {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth is not supported");
            finish();
        }
    }

    private void connectToBluetoothDevice() {
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            inputStream = bluetoothSocket.getInputStream();
            outputStream = bluetoothSocket.getOutputStream();
            startListeningForMessages();
            statusTextView.setText("Connected to Bluetooth device!");
        } catch (IOException e) {
            Log.e(TAG, "Could not connect to Bluetooth device", e);
            statusTextView.setText("Connection failed!");
        }
    }

    private void startListeningForMessages() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            while (bluetoothSocket != null) {
                try {
                    bytes = inputStream.read(buffer);
                    String message = new String(buffer, 0, bytes);
                    runOnUiThread(() -> {
                        statusTextView.append("\n" + message);
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    });
                } catch (IOException e) {
                    Log.e(TAG, "Error reading input", e);
                    break;
                }
            }
        }).start();
    }

    private void configureWiFi() {
        // Implement a method to configure WiFi via Bluetooth here
        String ssid = "New_SSID"; // Replace with input from the user
        String password = "New_Password"; // Replace with input from the user
        sendWiFiConfigToESP(ssid, password);
    }

    private void sendWiFiConfigToESP(String ssid, String password) {
        String configCommand = "CONFIG_WIFI:" + ssid + "," + password + "\n";
        try {
            outputStream.write(configCommand.getBytes());
            statusTextView.append("\nSent WiFi configuration: " + configCommand);
        } catch (IOException e) {
            Log.e(TAG, "Error sending WiFi configuration", e);
        }
    }

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
    }
}
