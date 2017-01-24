package com.srwc.fh.srwc_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bluelinelabs.logansquare.LoganSquare;
import com.peak.salut.Callbacks.SalutCallback;
import com.peak.salut.Callbacks.SalutDataCallback;
import com.peak.salut.Callbacks.SalutDeviceCallback;
import com.peak.salut.Salut;
import com.peak.salut.SalutDataReceiver;
import com.peak.salut.SalutDevice;
import com.peak.salut.SalutServiceData;
import com.srwc.fh.srwc_app.bluetooth.BluetoothController;
import com.srwc.fh.srwc_app.bluetooth.MessageReceiver;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SalutDataCallback {

    private RecyclerView mRecyclerView;
    private Button mButtonSend, mButtonWD;
    private EditText mEditTextMessage;
    private ImageView mImageView;
    private BluetoothController mBtController;
    private ChatMessageAdapter mAdapter;
    private String mOtherMacAddress;

    private Salut network;
    public SalutDataReceiver dataReceiver;
    public SalutServiceData serviceData;
    private  SalutDevice device = null;
    SalutDataCallback callback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOtherMacAddress = getIntent().getStringExtra("MAC_ADDRESS");
        String nameOther = getIntent().getStringExtra("NAME");

        if (nameOther != null) {
            setTitle(nameOther);
        }

        mBtController = BluetoothController.getInstance();

        mBtController.registerMessageReceiver(new MessageReceiver() {
            @Override
            public void messageReceived(String _msg) {
                addMessage(_msg, false);
            }
        });

        //WIFI DIRECT
        dataReceiver = new SalutDataReceiver(this, this);
        serviceData = new SalutServiceData("sas", 50489, nameOther);
        network = new Salut(dataReceiver, serviceData, new SalutCallback() {
            @Override
            public void call() {
                //System.out.println("test");
                Log.e("start salut","Sorry, but this device does not support WiFi Direct.");
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mButtonSend = (Button) findViewById(R.id.btn_send);
        mButtonWD = (Button) findViewById(R.id.btn_wifi);

        mEditTextMessage = (EditText) findViewById(R.id.et_message);
        mImageView = (ImageView) findViewById(R.id.iv_image);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new ChatMessageAdapter(this, new ArrayList<ChatMessage>());
        mRecyclerView.setAdapter(mAdapter);




        mButtonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = mEditTextMessage.getText().toString();
                if (TextUtils.isEmpty(message)) {
                    return;
                }
                //sendMessage(message);
                mBtController.sendMessage(message);
                addMessage(message, true);
                mEditTextMessage.setText("");
            }
        });

        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("click image", "CLICK IMAGE POL");
                setupNetwork();
            }
        });

        if (mOtherMacAddress != null) {
            mBtController.start(mOtherMacAddress);
        }

        //look for devices WIFI DIRECT
        mButtonWD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("WifiDirect", "Connect To Host WD");
                discoverServices();
            }
        });
    }

    private void addMessage(String _message, boolean _isMine) {
        ChatMessage chatMessage = new ChatMessage(_message, _isMine, false);
        mAdapter.add(chatMessage);
        mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupNetwork()
    {
        if(!network.isRunningAsHost)
        {
            network.startNetworkService(new SalutDeviceCallback() {
                @Override
                public void call(SalutDevice salutDevice) {
                    Log.d("HOST", salutDevice.readableName + " has connected!");
                    Message myMessage = new Message();
                    myMessage.description = "See you on the other side!";

                    network.sendToDevice(salutDevice, myMessage, new SalutCallback() {
                        @Override
                        public void call() {
                            Log.e("FAILED", "Oh no! The data failed to send.");
                        }
                    });

                   /* network.sendToHost(myMessage, new SalutCallback() {
                        @Override
                        public void call() {
                            Log.e("FAILED", "Oh no! The data failed to send.");
                        }
                    });*/
                }
            });
        }
        else
        {
            network.stopNetworkService(false);
        }
    }

    private void discoverServices()
    {            Log.i("BEFORE THE IF", "BEFORE THE IF");

        if(!network.isRunningAsHost && !network.isDiscovering)
        {
            Log.i("IN THE IF", "IN THE IF");
            network.discoverNetworkServices(new SalutCallback() {
                @Override
                public void call() {
                    Log.d("Device: ",network.foundDevices.get(0).instanceName + " found.");
                    network.registerWithHost(network.foundDevices.get(0), new SalutCallback() {
                        @Override
                        public void call() {
                            Log.d("CALL", "We're now registered.");
                        }
                    }, new SalutCallback() {
                        @Override
                        public void call() {
                            Log.d("CALL", "We failed to register.");
                        }
                    });
                }
            }, true);
        }
        else
        {
            network.stopServiceDiscovery(true);
        }
    }

    @Override
    public void onDataReceived(Object data) {
        Log.d("DATA", "Received network data.");
        try
        {
            Message newMessage = LoganSquare.parse(String.valueOf(data), Message.class);
            Log.d("DATA", newMessage.description);  //See you on the other side!
            //Do other stuff with data.
        }
        catch (IOException ex)
        {
            Log.e("FAILED", "Failed to parse network data.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(this.network.isRunningAsHost)
            network.stopNetworkService(true);
        /*else
            network.unregisterClient(null);*/
    }

    /*@Override
    public void onBackPressed() {
        Intent backIntent = new Intent(this, MenuActivity.class);
        startActivity(backIntent);
        finish();
    }*/
}