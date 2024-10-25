package com.example.esp32eth2wifi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private BluetoothService bluetoothService;
    private TextView messagesView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messagesView = findViewById(R.id.messagesView);
        bluetoothService = new BluetoothService(messagesView);

        Button connectButton = findViewById(R.id.connectButton);
        connectButton.setOnClickListener(v -> bluetoothService.connectBluetooth("ESP32ETH2WiFi"));

        Button wifiConfigButton = findViewById(R.id.wifiConfigButton);
        wifiConfigButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, WiFiConfigActivity.class)));
    }
}
