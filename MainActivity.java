package com.example.eth2wifi;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements BluetoothService.BluetoothCallback {
    private BluetoothService bluetoothService;
    private TextView messagesView;
    private Button connectButton;
    private Button wifiConfigButton;

    private final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    };

    private final ActivityResultLauncher<String[]> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
            boolean allGranted = true;
            for (Boolean isGranted : permissions.values()) {
                allGranted &= isGranted;
            }
            if (allGranted) {
                initializeBluetooth();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messagesView = findViewById(R.id.messagesView);
        connectButton = findViewById(R.id.connectButton);
        wifiConfigButton = findViewById(R.id.wifiConfigButton);

        checkPermissions();

        connectButton.setOnClickListener(v -> {
            if (bluetoothService != null) {
                if (!bluetoothService.isConnected()) {
                    bluetoothService.connectBluetooth("ESP32ETH2WiFi");
                } else {
                    bluetoothService.disconnect();
                    updateButtonStates(false);
                }
            }
        });

        wifiConfigButton.setOnClickListener(v -> 
            startActivity(new Intent(MainActivity.this, WiFiConfigActivity.class))
        );

        updateButtonStates(false);
    }

    private void checkPermissions() {
        boolean allPermissionsGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            permissionLauncher.launch(REQUIRED_PERMISSIONS);
        } else {
            initializeBluetooth();
        }
    }

    private void initializeBluetooth() {
        bluetoothService = new BluetoothService(this, messagesView);
        bluetoothService.setCallback(this);
    }

    private void updateButtonStates(boolean isConnected) {
        connectButton.setText(isConnected ? R.string.disconnect : R.string.connect_to_bluetooth);
        wifiConfigButton.setEnabled(isConnected);
    }

    @Override
    public void onConnected() {
        updateButtonStates(true);
    }

    @Override
    public void onConnectionFailed(String error) {
        updateButtonStates(false);
        messagesView.append("Connection error: " + error + "\n");
    }

    @Override
    public void onMessageReceived(String message) {
        // Handle specific message types if needed
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) {
            bluetoothService.disconnect();
        }
    }
}
