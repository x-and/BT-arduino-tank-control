package andreyanov.carcontrol.tools;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import andreyanov.carcontrol.MainActivity;

public class Bluetooth implements Runnable {

    static final byte delimiter = 10; //This is the ASCII code for a newline character

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;

    Thread workerThread;

    Activity owner;

    List<BluetoothDataListener> listeners = new ArrayList(3);

    public Bluetooth(Activity activity) {
        owner = activity;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.d("BT", "BT instance created");
    }

    public void addListener(BluetoothDataListener b) {
        listeners.add(b);
    }

    public boolean isBluetoothExists() {
        return mBluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    public void tryToEnableBluetooth() {
        Log.d("BT", "tryToEnableBluetooth");
        if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            owner.startActivityForResult(enableBluetooth, 0);
        }
    }

    private void findBT() {
        Log.d("BT", "try to find BT device");
        if (!isBluetoothExists()) {
            Log.d("BT", "No BT adapter find. Exit");
            return;
        }

        if (!isBluetoothEnabled()) {
            tryToEnableBluetooth();
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0) {
            for(BluetoothDevice device : pairedDevices) {
                if(device.getName().equals("HC-06")) {
                    mmDevice = device;
                    break;
                }
            }
        }
        if (mmDevice != null)
            Log.d("BT", "Bluetooth Device Found");

        else
            Log.d("BT", "No Bluetooth Device Found");
    }

    private void openBT() {
        Log.d("BT", "try to openBT");
        if (mmDevice == null) return;

        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        try {
            mmSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();
        }
        catch (Exception e) {
            Log.d("BT", "failed to open BT socket");
            mmSocket = null;
            e.printStackTrace();
            return;
        }

        Log.d("BT", "Bluetooth Opened " + mmSocket.isConnected());
    }

    private void closeBT() {
        if (mmSocket != null && mmSocket.isConnected()) {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (workerThread != null) {
                workerThread.interrupt();
                workerThread = null;
            }
            Log.d("BT", "Bluetooth Closed");
        }
        mmDevice = null;

    }

    @Override
    public void run() {

        int readBufferPosition = 0;
        byte[] readBuffer = new byte[1024];

        while(!Thread.currentThread().isInterrupted()) {
            try  {
                if (mmSocket != null && mmSocket.isConnected()) {
                    int bytesAvailable = mmInputStream.available();
                    if (bytesAvailable > 0) {
                        byte[] packetBytes = new byte[bytesAvailable];
                        mmInputStream.read(packetBytes);
                        for (int i = 0; i < bytesAvailable; i++) {
                            byte b = packetBytes[i];
                            if (b == delimiter) {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "US-ASCII");
                                readBufferPosition = 0;
                                for (BluetoothDataListener listener : listeners) {
                                    listener.dataReady(data);
                                }
                            } else {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                } else if (mmDevice != null) {
                    openBT();
                }
                Thread.sleep(30L);
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public void init() {
            closeBT();
            findBT();
            openBT();

            if (workerThread == null) {
                workerThread = new Thread(this,"Bluetooth Listener");
            }
            if (workerThread.isInterrupted()) {
                workerThread.start();
                Log.d("BT", "thread start");
            }

    }

    public void sendData(String data) {
        if (!data.endsWith("/n")) data += "/n";
        if (mmOutputStream == null || !mmSocket.isConnected()) return;

        try {
            Log.d("BT", "SendData: " + data);
            mmOutputStream.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static interface BluetoothDataListener {
        public void dataReady(String data);
    }

}
