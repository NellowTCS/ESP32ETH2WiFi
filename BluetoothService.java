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

    // Standard SPP UUID
    private static final UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public interface BluetoothCallback {
        void onConnected();
        void onConnectionFailed(String error);
        void onDataReceived(String message);
    }

    private BluetoothCallback callback;

    public BluetoothService(Context context, TextView messagesView, BluetoothCallback callback) {
        this.context = context;
        this.messagesView = messagesView;
        this.callback = callback;
        this.mainHandler = new Handler(Looper.getMainLooper());
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void connectBluetooth(String deviceName) {
        if (bluetoothAdapter == null) {
            showMessage("Bluetooth is not available on this device");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            showMessage("Please enable Bluetooth");
            return;
        }

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) 
            != PackageManager.PERMISSION_GRANTED) {
            showMessage("Bluetooth permission not granted");
            return;
        }

        // Run connection in background thread
        new Thread(() -> {
            BluetoothDevice device = findDevice(deviceName);
            if (device == null) {
                mainHandler.post(() -> {
                    showMessage("Device " + deviceName + " not found");
                    callback.onConnectionFailed("Device not found");
                });
                return;
            }

            try {
                connectToDevice(device);
            } catch (IOException e) {
                mainHandler.post(() -> {
                    showMessage("Connection failed: " + e.getMessage());
                    callback.onConnectionFailed(e.getMessage());
                });
            }
        }).start();
    }

    private BluetoothDevice findDevice(String deviceName) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) 
            != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (device.getName().equals(deviceName)) {
                return device;
            }
        }
        return null;
    }

    private void connectToDevice(BluetoothDevice device) throws IOException {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) 
            != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Close any existing connection
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Create new connection
        bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_SPP);
        bluetoothSocket.connect();
        outputStream = bluetoothSocket.getOutputStream();
        inputStream = bluetoothSocket.getInputStream();
        isConnected = true;

        // Start listening for messages
        connectedThread = new ConnectedThread();
        connectedThread.start();

        mainHandler.post(() -> {
            showMessage("Connected to " + device.getName());
            callback.onConnected();
        });
    }

    public void sendMessage(String message) {
        if (!isConnected || outputStream == null) {
            showMessage("Not connected");
            return;
        }

        new Thread(() -> {
            try {
                outputStream.write((message + "\n").getBytes());
                outputStream.flush();
                mainHandler.post(() -> showMessage("Sent: " + message));
            } catch (IOException e) {
                mainHandler.post(() -> showMessage("Error sending message: " + e.getMessage()));
            }
        }).start();
    }

    public void disconnect() {
        isConnected = false;
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothSocket = null;
        }
        showMessage("Disconnected");
    }

    private void showMessage(String message) {
        if (messagesView != null) {
            messagesView.append(message + "\n");
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private class ConnectedThread extends Thread {
        private boolean running = true;

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (running) {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        final String message = new String(buffer, 0, bytes);
                        mainHandler.post(() -> {
                            showMessage("Received: " + message);
                            callback.onDataReceived(message);
                        });
                    }
                } catch (IOException e) {
                    if (running) {
                        mainHandler.post(() -> {
                            isConnected = false;
                            showMessage("Connection lost: " + e.getMessage());
                            callback.onConnectionFailed("Connection lost");
                        });
                        break;
                    }
                }
            }
        }

        public void cancel() {
            running = false;
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
