package com.example.eth2wifi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothService {
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private final TextView messagesView;
    private final Handler mainHandler;
    private boolean isConnected = false;
    private ConnectedThread connectedThread;

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    public interface BluetoothCallback {
        void onConnected();
        void onConnectionFailed(String error);
        void onMessageReceived(String message);
    }
    
    private BluetoothCallback callback;

    public BluetoothService(Context context, TextView messagesView) {
        this.context = context;
        this.messagesView = messagesView;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void setCallback(BluetoothCallback callback) {
        this.callback = callback;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void connectBluetooth(String deviceName) {
        if (bluetoothAdapter == null) {
            showToast("Bluetooth is not available on this device");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            showToast("Please enable Bluetooth");
            return;
        }

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
            showToast("Bluetooth permission not granted");
            return;
        }

        // Run connection in background thread
        new Thread(() -> {
            BluetoothDevice device = findDevice(deviceName);
            if (device == null) {
                mainHandler.post(() -> {
                    showToast("Device " + deviceName + " not found");
                    if (callback != null) callback.onConnectionFailed("Device not found");
                });
                return;
            }

            try {
                connect(device);
            } catch (IOException e) {
                mainHandler.post(() -> {
                    showToast("Connection failed: " + e.getMessage());
                    if (callback != null) callback.onConnectionFailed(e.getMessage());
                });
            }
        }).start();
    }

    private BluetoothDevice findDevice(String deviceName) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals(deviceName)) {
                return device;
            }
        }
        return null;
    }

    private void connect(BluetoothDevice device) throws IOException {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        bluetoothSocket = device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        bluetoothSocket.connect();
        outputStream = bluetoothSocket.getOutputStream();
        inputStream = bluetoothSocket.getInputStream();
        isConnected = true;

        mainHandler.post(() -> {
            showToast("Connected to " + device.getName());
            if (callback != null) callback.onConnected();
        });

        // Start reading messages
        connectedThread = new ConnectedThread();
        connectedThread.start();
    }

    public void disconnect() {
        isConnected = false;
        try {
            if (connectedThread != null) {
                connectedThread.interrupt();
                connectedThread = null;
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        if (!isConnected) {
            showToast("Not connected to device");
            return;
        }

        try {
            outputStream.write((message + "\n").getBytes());
            outputStream.flush();
            appendMessage("Sent: " + message);
        } catch (IOException e) {
            showToast("Error sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void appendMessage(String message) {
        if (messagesView != null) {
            mainHandler.post(() -> messagesView.append(message + "\n"));
        }
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    private class ConnectedThread extends Thread {
        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (isConnected) {
                try {
                    bytes = inputStream.read(buffer);
                    String message = new String(buffer, 0, bytes);
                    appendMessage("Received: " + message);
                    if (callback != null) {
                        mainHandler.post(() -> callback.onMessageReceived(message));
                    }
                } catch (IOException e) {
                    if (isConnected) {
                        isConnected = false;
                        mainHandler.post(() -> {
                            showToast("Connection lost: " + e.getMessage());
                            if (callback != null) callback.onConnectionFailed("Connection lost");
                        });
                    }
                    break;
                }
            }
        }
    }
}
