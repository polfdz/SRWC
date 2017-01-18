package com.srwc.fh.srwc_app.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by christoph on 17.01.17.
 */

public class BluetoothController implements Serializable {
    public static final UUID MY_UUID = UUID
            .fromString("f895eaf0-867f-11e3-baa7-0800200c9a66");
    public static final int SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    public static final int DATA_RECEIVED = 3;
    public static final int SOCKET_CONNECTED = 4;
    public static final int ACK_SEND = 5;

    private BluetoothAdapter mBtAdapter = null;
    private ConnectionThread mBtConnection = null;
    private AcceptThread mAcceptThread = null;
    private MessageReceiver mMsgReceiver;
    private ConnectionReceiver mConnectionReceiver;

    private String mNameMine;

    private static BluetoothController instance = null;

    public static BluetoothController getInstance() {
        if (instance == null) {
            instance = new BluetoothController();
        }
        return instance;
    }

    private BluetoothController() {

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBtAdapter == null) {
            //Toast.makeText(_context, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            //finish();
            return;
        } else {
            int btState = mBtAdapter.getState();

            if (btState == BluetoothAdapter.STATE_OFF) {
                //mTvStatus.setText("Bluetooth is off.");
                if (!mBtAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            } else if (btState == BluetoothAdapter.STATE_ON) {
                //initializeBluetooth();
                mAcceptThread = new AcceptThread(mHandler);
                mAcceptThread.start();
            }
        }
    }

    public void registerMessageReceiver(MessageReceiver _msgReceiver) {
        mMsgReceiver = _msgReceiver;
    }

    public void registerConnectionReceiver(ConnectionReceiver _connectionReceiver) {
        mConnectionReceiver = _connectionReceiver;
    }

    public void setNameMine(String _myName) {
        mNameMine = _myName;
    }

    public void start(String _otherMacAddress) {
        new ConnectThread(_otherMacAddress, mHandler).start();
    }

    public void sendMessage(String _msg) {
        mBtConnection.write(_msg.getBytes());
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case DATA_RECEIVED:
                    String str = (String) msg.obj;
                    //addNewMessage(mBtConnection.mSocket.getRemoteDevice().getName(), str);

                    if (mMsgReceiver != null) {
                        mMsgReceiver.messageReceived(str);
                        Log.i("data_received", mBtConnection.mSocket.getRemoteDevice().getName() + " | " + str);
                    } else {
                        String[] strArray = str.split(";");
                        if (strArray[0].equals("ACK")) {
                            mConnectionReceiver.connectionReceived(strArray[1]);
                        }
                    }
                    break;

                case SOCKET_CONNECTED:
                    mBtConnection = (ConnectionThread) msg.obj;
                    //String msgString = "Hello from " + mBtAdapter.getName();
                    //mBtConnection.write(msgString.getBytes());
                    //Log.i("socket_connected", mBtAdapter.getName() + " | " + msgString);
                    break;

                case ACK_SEND:
                    mBtConnection = (ConnectionThread) msg.obj;
                    mBtConnection.write(("ACK;" + mNameMine).getBytes());
                    break;
                default:
                    break;
            }
            return true;
        }
    });
}
