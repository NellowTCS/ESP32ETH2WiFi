package com.example.esp32eth2wifi;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class WiFiConfigActivity extends AppCompatActivity {
    private BluetoothService bluetoothService;
    private EditText ssidInput;
    private EditText passwordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_config);

        ssidInput = findViewById(R.id.ssidInput);
        passwordInput = findViewById(R.id.passwordInput);
        bluetoothService = new BluetoothService(null);  // No TextView needed here

        Button sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(v -> {
            String ssid = ssidInput.getText().toString();
            String password = passwordInput.getText().toString();
            bluetoothService.sendMessage("SET_WIFI," + ssid.trim() + "," + password.trim());
        });
    }
}
