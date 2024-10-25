package com.example.esp32eth2wifi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.widget.TextView;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService {
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private TextView messagesView;

    private final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public BluetoothService(TextView messagesView) {
        this.messagesView = messagesView;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void connectBluetooth(String deviceName) {
        BluetoothDevice device = null;
        for (BluetoothDevice pairedDevice : bluetoothAdapter.getBondedDevices()) {
            if (pairedDevice.getName().equals(deviceName)) {
                device = pairedDevice;
                break;
            }
        }
        
        if (device != null) {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();
                inputStream = bluetoothSocket.getInputStream();
                
                readMessages();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void readMessages() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    String receivedMessage = new String(buffer, 0, bytes);
                    messagesView.post(() -> messagesView.append(receivedMessage + "\n"));
                } catch (IOException e) {
                    break;
                }
            }
        }).start();
    }

    public void sendMessage(String message) {
        try {
            outputStream.write((message + "\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
