package com.srwc.fh.srwc_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.srwc.fh.srwc_app.bluetooth.BluetoothController;
import com.srwc.fh.srwc_app.bluetooth.MessageReceiver;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private Button mButtonSend;
    private EditText mEditTextMessage;
    private ImageView mImageView;
    private BluetoothController mBtController;

    private ChatMessageAdapter mAdapter;

    private String mOtherMacAddress;

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

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mButtonSend = (Button) findViewById(R.id.btn_send);
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
                //sendMessage();
                //mBtController.start(mOtherMacAddress);
            }
        });

        if (mOtherMacAddress != null) {
            mBtController.start(mOtherMacAddress);
        }
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

    /*@Override
    public void onBackPressed() {
        Intent backIntent = new Intent(this, MenuActivity.class);
        startActivity(backIntent);
        finish();
    }*/
}