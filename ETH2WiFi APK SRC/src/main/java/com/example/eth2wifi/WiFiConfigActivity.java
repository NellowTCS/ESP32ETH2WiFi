package com.example.eth2wifi;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class WiFiConfigActivity extends AppCompatActivity implements BluetoothService.BluetoothCallback {
    private BluetoothService bluetoothService;
    private EditText ssidInput;
    private EditText passwordInput;
    private Button sendButton;
    private ProgressBar progressBar;
    private static final int RESPONSE_TIMEOUT = 10000; // 10 seconds timeout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_config);

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("WiFi Configuration");
        }

        // Initialize views
        ssidInput = findViewById(R.id.ssidInput);
        passwordInput = findViewById(R.id.passwordInput);
        sendButton = findViewById(R.id.sendButton);
        progressBar = findViewById(R.id.progressBar);

        // Initialize BluetoothService
        bluetoothService = new BluetoothService(this, null);
        bluetoothService.setCallback(this);

        sendButton.setOnClickListener(v -> sendWiFiCredentials());
    }

    private void sendWiFiCredentials() {
        String ssid = ssidInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Validate inputs
        if (ssid.isEmpty()) {
            ssidInput.setError("SSID cannot be empty");
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password cannot be empty");
            return;
        }

        if (password.length() < 8) {
            passwordInput.setError("Password must be at least 8 characters");
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        sendButton.setEnabled(false);

        // Send command to ESP32
        String command = String.format("SET_WIFI,%s,%s", ssid, password);
        bluetoothService.sendMessage(command);

        // Set timeout for response
        progressBar.postDelayed(() -> {
            if (progressBar.getVisibility() == View.VISIBLE) {
                progressBar.setVisibility(View.GONE);
                sendButton.setEnabled(true);
                Toast.makeText(this, "No response from device. Please try again.", 
                             Toast.LENGTH_LONG).show();
            }
        }, RESPONSE_TIMEOUT);
    }

    @Override
    public void onMessageReceived(String message) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            sendButton.setEnabled(true);

            // Handle different response types
            if (message.contains("WIFI_SUCCESS")) {
                Toast.makeText(this, "WiFi configuration successful!", 
                             Toast.LENGTH_LONG).show();
                finish(); // Return to main activity
            } else if (message.contains("WIFI_FAIL")) {
                Toast.makeText(this, "WiFi configuration failed. Please try again.", 
                             Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Unknown response: " + message, 
                             Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onConnected() {
        runOnUiThread(() -> {
            sendButton.setEnabled(true);
            Toast.makeText(this, "Connected to device", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onConnectionFailed(String error) {
        runOnUiThread(() -> {
            sendButton.setEnabled(false);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Connection error: " + error, 
                         Toast.LENGTH_LONG).show();
            finish(); // Return to main activity as we can't proceed without connection
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) {
            bluetoothService.disconnect();
        }
    }
}
