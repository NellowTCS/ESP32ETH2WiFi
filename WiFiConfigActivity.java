package com.example.eth2wifi;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class WiFiConfigActivity extends AppCompatActivity implements BluetoothService.BluetoothCallback {
    private BluetoothService bluetoothService;
    private EditText ssidInput;
    private EditText passwordInput;
    private TextView statusView;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_config);

        // Initialize UI components
        ssidInput = findViewById(R.id.ssidInput);
        passwordInput = findViewById(R.id.passwordInput);
        statusView = findViewById(R.id.statusView);
        sendButton = findViewById(R.id.sendButton);

        // Initialize BluetoothService
        bluetoothService = new BluetoothService(this, statusView, this);

        // Button click handlers
        sendButton.setOnClickListener(v -> sendWiFiConfig());

        // Get BluetoothService instance from MainActivity if it exists
        if (getIntent().hasExtra("BLUETOOTH_CONNECTED") && 
            getIntent().getBooleanExtra("BLUETOOTH_CONNECTED", false)) {
            // BluetoothService is already connected
            updateUIState(true);
        } else {
            // Try to connect to ESP32
            connectToESP32();
        }
    }

    private void connectToESP32() {
        updateUIState(false);
        statusView.append("Connecting to ESP32...\n");
        bluetoothService.connectBluetooth("ESP32ETH2WiFi");
    }

    private void sendWiFiConfig() {
        String ssid = ssidInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (ssid.isEmpty()) {
            ssidInput.setError("SSID cannot be empty");
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password cannot be empty");
            return;
        }

        if (!bluetoothService.isConnected()) {
            showToast("Not connected to ESP32");
            connectToESP32();
            return;
        }

        // Send WiFi configuration command
        String command = String.format("SET_WIFI,%s,%s", ssid, password);
        bluetoothService.sendMessage(command);
        showToast("Sending WiFi configuration...");
    }

    private void updateUIState(boolean connected) {
        sendButton.setEnabled(connected);
        if (connected) {
            sendButton.setText("Send WiFi Config");
        } else {
            sendButton.setText("Waiting for Connection...");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // BluetoothCallback implementation
    @Override
    public void onConnected() {
        updateUIState(true);
        showToast("Connected to ESP32");
    }

    @Override
    public void onConnectionFailed(String error) {
        updateUIState(false);
        showToast("Connection failed: " + error);
    }

    @Override
    public void onDataReceived(String message) {
        // Handle responses from ESP32
        if (message.contains("WIFI_SET_OK")) {
            showToast("WiFi configuration successful");
            finish(); // Return to MainActivity
        } else if (message.contains("WIFI_SET_ERROR")) {
            showToast("WiFi configuration failed");
        }
        statusView.append("Received: " + message + "\n");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't disconnect if we're returning to MainActivity
        if (isFinishing()) {
            bluetoothService.disconnect();
        }
    }
}
