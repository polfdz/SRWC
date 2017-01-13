package com.srwc.fh.srwc_app;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Pol on 13/01/2017.
 */

public class MenuActivity extends AppCompatActivity  implements NfcAdapter.CreateNdefMessageCallback {

    private final static String MIME_TYPE = "application/vnd.at.fh-ooe.srwc";

    private NfcAdapter mNfcAdapter;
    private PendingIntent mNfcPendingIntent;
    private Tag mTag;

    private Button mBtnWrite;
    private ListView mLvLog;
    private EditText tName;
    private String name1 = "", name2 = null;
    private List<String> mLogList = new ArrayList<String>();
    private ArrayAdapter<String> mLogAdapter;
    private boolean beamed = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Button btnClear = (Button) findViewById(R.id.btnClearLog);
        btnClear.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            mLogAdapter.clear();
                                        }
                                    }
        );

        mBtnWrite = (Button) findViewById(R.id.btnWrite);
        mLvLog = (ListView) findViewById(R.id.lvLog);
        mLogAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mLogList);
        mLvLog.setAdapter(mLogAdapter);
        tName = (EditText) findViewById(R.id.tName);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available!", Toast.LENGTH_LONG)
                    .show();
            finish();
        } else if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(this, "Please switch on NFC", Toast.LENGTH_LONG)
                    .show();
            startActivity(new Intent(
                    android.provider.Settings.ACTION_NFC_SETTINGS));
        }
        mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),0);

        mBtnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeName();
            }
        });

        mNfcAdapter.setNdefPushMessageCallback(this, this);
        mNfcAdapter.setNdefPushMessage(createNdefMessage(null), this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        beamed = true;

        if(beamed == true && name2 != null){
            Toast.makeText(this, "PAIR", Toast.LENGTH_LONG)
                    .show();
            Intent connected = new Intent(this, MainActivity.class);
            startActivity(connected);
        }
        //mNfcAdapter.setNdefPushMessage(createNdefMessage(null), this);
    }

    public void log(String msg) {
        mLogList.add(msg);
        mLogAdapter.notifyDataSetChanged();
        mLvLog.setSelection(mLogAdapter.getCount() - 1);
    }

    private void writeName(){
        name1 = tName.getText().toString();
    }

    private void writeTag() {
        //enter here edittext
        /*String text = "This tag was written by " + android.os.Build.MODEL
                + " at " + DateFormat.getDateTimeInstance().format(new Date());*/
        String text = tName.getText().toString();
        String[] textarray = text.split(",");
        NdefRecord[] records = new NdefRecord[textarray.length];
        for(int i = 0; i < textarray.length; i++) {
            records[i] = NdefRecord.createMime(MIME_TYPE, textarray[i].getBytes());
        }
        NdefMessage msg = new NdefMessage(records);
        if(mTag != null){ //check if there's a tag
            Ndef ndef = Ndef.get(mTag); //msg to overide

            if(ndef != null){ //previous msg
                try {
                    ndef.connect(); //connect with the card
                    ndef.writeNdefMessage(msg);
                    //ndef.writeNdefMessage(msg2);

                    ndef.close();
                    log("Write successful");
                } catch (FormatException e) {
                    e.printStackTrace();
                    log("Wrong format");
                } catch (IOException e) {
                    e.printStackTrace();
                    log("Tag missing");
                }
            }else{
                NdefFormatable format = NdefFormatable.get(mTag);

                try {
                    format.connect();
                    format.format(msg);
                    format.close();
                    log("Format + Write successful");
                } catch (IOException e) {
                    e.printStackTrace();
                    log("Tag missing");
                } catch (FormatException e) {
                    e.printStackTrace();
                    log("Wrong format");
                }
            }
        } else{
            log("Please read a tag first.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter tagDiscovered = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter[] tagFilters = new IntentFilter[]{ tagDiscovered};
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, tagFilters, null);

        Intent intent = getIntent();
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())){
            mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            if(messages != null){
                //log("Found" + messages.length + " NDEF messages");
                for(int i = 0; i < messages.length; i++){
                    NdefMessage msg = (NdefMessage) messages[i];
                    for(NdefRecord record: msg.getRecords()){
                        log("User1: " + tName.getText() + "\n User2 " + new String(record.getPayload()));
                        name1 = tName.getText().toString();
                        name2 = new String(record.getPayload());
                    }
                }
                if(beamed == true && name2 != null){
                    Toast.makeText(this, "PAIR", Toast.LENGTH_LONG)
                            .show();
                    Intent connected = new Intent(this, MainActivity.class);
                    startActivity(connected);
                }
            }else{
                log("Found empty tag");
            }
        }
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        /*String text = "Hello from " + android.os.Build.MODEL
                + " at " + DateFormat.getDateTimeInstance().format(new Date()) +""+tName.getText().toString();*/
        String text = "" + tName.getText().toString();
        NdefMessage msg = new NdefMessage(
                new NdefRecord[]{NdefRecord.createMime(MIME_TYPE, text.getBytes())}
        );
        return msg;
    }
}