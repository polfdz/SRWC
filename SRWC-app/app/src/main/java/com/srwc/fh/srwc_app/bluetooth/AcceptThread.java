package com.srwc.fh.srwc_app.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by Thomas on 18.01.2017.
 */

public class AcceptThread extends Thread implements Serializable {
    private BluetoothServerSocket mServerSocket;
    private BluetoothSocket mSocket;
    private final BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    private final Handler mHandler;

    public AcceptThread(Handler handler) {
        mHandler = handler;
        try {
            mServerSocket = mBtAdapter.listenUsingRfcommWithServiceRecord("Bluetooth Demo", BluetoothController.MY_UUID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            try {
                mSocket = mServerSocket.accept();
                ConnectionThread conThread = new ConnectionThread(mSocket, mHandler);
                mHandler.obtainMessage(BluetoothController.SOCKET_CONNECTED, conThread).sendToTarget();
                conThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}