package com.srwc.fh.srwc_app.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;

/**
 * Created by Thomas on 18.01.2017.
 */

public class ConnectThread extends Thread {
    private BluetoothSocket mSocket;
    private final BluetoothDevice mDevice;
    private final BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    private final Handler mHandler;

    public ConnectThread(String deviceId, Handler handler) {
        mDevice = mBtAdapter.getRemoteDevice(deviceId);
        mHandler = handler;

        try {
            mSocket = mDevice.createRfcommSocketToServiceRecord(BluetoothController.MY_UUID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            mSocket.connect();
            ConnectionThread conThread = new ConnectionThread(mSocket, mHandler);
            mHandler.obtainMessage(BluetoothController.SOCKET_CONNECTED, conThread).sendToTarget();
            mHandler.obtainMessage(BluetoothController.ACK_SEND, conThread).sendToTarget();
            conThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}