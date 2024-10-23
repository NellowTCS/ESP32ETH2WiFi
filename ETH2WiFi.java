package com.example.eth2wifi;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.statusTextView);
        scrollView = findViewById(R.id.scrollView);
        Button configureWiFiButton = findViewById(R.id.configureWiFiButton);
        Button connectBluetoothButton = findViewById(R.id.connectBluetoothButton);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth is not supported on this device.");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_ADMIN
            }, REQUEST_BLUETOOTH_PERMISSIONS);
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        configureWiFiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configureWiFi();
            }
        });

        connectBluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToBluetoothDevice();
            }
        });

        registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    private void connectToBluetoothDevice() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().contains("ETH2WiFi")) { // Replace with your device name
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Configure Wi-Fi");

        // Set up input fields for SSID and Password
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText ssidInput = new EditText(this);
        ssidInput.setHint("Enter Wi-Fi SSID");
        layout.addView(ssidInput);

        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter Wi-Fi Password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordInput);

        builder.setView(layout);

        // Add "OK" button to confirm
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String ssid = ssidInput.getText().toString();
                String password = passwordInput.getText().toString();
                // Pass the credentials to ESP32 via Bluetooth
                sendWiFiCredentials(ssid, password);
            }
        });

        // Add "Cancel" button
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Show the dialog
        builder.show();
    }

    private void sendWiFiCredentials(String ssid, String password) {
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            try {
                OutputStream outputStream = bluetoothSocket.getOutputStream();
                outputStream.write((ssid + "," + password + "\n").getBytes());
                outputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error sending Wi-Fi credentials", e);
            }
        } else {
            Log.e(TAG, "Bluetooth socket is not connected");
        }
    }
}
