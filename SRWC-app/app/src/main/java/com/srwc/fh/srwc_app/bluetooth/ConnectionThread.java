package com.srwc.fh.srwc_app.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Created by Thomas on 18.01.2017.
 */

public class ConnectionThread extends Thread implements Serializable {
    BluetoothSocket mSocket;
    private final Handler mHandler;
    private InputStream mInStream;
    private OutputStream mOutStream;

    public ConnectionThread(BluetoothSocket socket, Handler handler) {
        super();
        mSocket = socket;
        mHandler = handler;

        try {
            mInStream = mSocket.getInputStream();
            mOutStream = mSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        byte[] buf = new byte[1024];
        int bytes;

        while (true) {
            try {
                bytes = mInStream.read(buf);
                String str = new String(buf, 0 , bytes);
                mHandler.obtainMessage(BluetoothController.DATA_RECEIVED, str).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void write(byte[] data) {
        try {
            mOutStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}