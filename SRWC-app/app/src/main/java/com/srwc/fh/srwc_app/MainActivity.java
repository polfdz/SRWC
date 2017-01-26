package com.srwc.fh.srwc_app;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Base64;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class MainActivity extends AppCompatActivity implements SalutDataCallback {

    private RecyclerView mRecyclerView;
    private EditText mEditTextMessage;
    private BluetoothController mBtController;
    private ChatMessageAdapter mAdapter;
    private String mOtherMacAddress;
    private ImageView mButtonSend, mImageView;

    private Salut network;
    public SalutDataReceiver dataReceiver;
    public SalutServiceData serviceData;
    private SalutDevice device = null;
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
                addMessage(_msg, false, false);
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
        mButtonSend = (ImageView) findViewById(R.id.iv_send);

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
                addMessage(message, true, false);
                mEditTextMessage.setText("");
            }
        });

        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("click image", "CLICK IMAGE POL");
                sendImage();
            }
        });

        if (mOtherMacAddress != null) {
            mBtController.start(mOtherMacAddress);
        }
    }

    private void addMessage(String _message, boolean _isMine, boolean _isImage) {
        ChatMessage chatMessage = new ChatMessage(_message, _isMine, _isImage);
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
        if (id == R.id.action_host) {
            Log.i("WifiDirect", "Host created");
            setupNetwork();
            return true;
        } else if (id == R.id.action_client) {
            Log.i("WifiDirect", "Connect To Host WD");
            discoverServices();
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

                    device = salutDevice;
                    //method to send myMessage to Connected Host:
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
            device = null;
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
                    device = network.foundDevices.get(0);
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
            device = null;
        }
    }

    private void sendImage() {
        //GetImageString to be sent
        String encImage = getImageStringToSend();
        Message myMessage = new Message();
        myMessage.description = encImage;//myMessage.description = "See you on the other side!";

        //method to send myMessage to Connected Client:
        network.sendToDevice(device, myMessage, new SalutCallback() {
            @Override
            public void call() {
                Log.e("FAILED", "Oh no! The data failed to send.");
            }
        });

        addMessage(encImage, true, true);
    }

    @Override
    public void onDataReceived(Object data) {
        Log.d("DATA", "Received network data.");
        try
        {
            Message newMessage = LoganSquare.parse(String.valueOf(data), Message.class);
            //Bitmap imageRecived = decodeDataBitmap(newMessage.description);
            //ivImage.setImageBitmap(imageRecived);
            Log.d("DATAEncodedRecived", newMessage.description);  //See you on the other side!
            //Log.d("DATADecodedRecived", imageRecived.toString());  //See you on the other side!
            //Do other stuff with data

            addMessage(newMessage.description, false, true);
        }
        catch (IOException ex)
        {
            Log.e("FAILED", "Failed to parse network data.");
        }
    }
    //encode image from path to string
    public String getImageStringToSend(){
        //Path of the image
        String dcim = getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
        Log.d("PIC", dcim.toString());
        String encImage = "";
        if (dcim != null) {

            //Bitmap bm = BitmapFactory.decodeStream(fis);
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.img_sample);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.JPEG, 100 , baos);
            byte[] b = baos.toByteArray();
            encImage = Base64.encodeToString(b, Base64.DEFAULT);
            Log.d("IMG", encImage);
        }
        return encImage;
    }
    //decode string to bitmap
    public Bitmap decodeDataBitmap(String message){
        byte[] decodedString = Base64.decode(message, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
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