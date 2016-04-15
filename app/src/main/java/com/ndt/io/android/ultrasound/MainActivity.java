package com.ndt.io.android.ultrasound;

import android.Manifest;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ndt.io.androidultrasound.DataExchange;
import com.ndt.io.androidultrasound.OnUsChannelListener;
import com.ndt.io.androidultrasound.UsChannelState;

public class MainActivity extends AppCompatActivity implements OnUsChannelListener {

    private TextView textView;
    private Button button;
    private EditText editText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        textView = (TextView)findViewById(R.id.textView);
        editText = (EditText) findViewById(R.id.editText);
        button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                testUltrasonicCommunication();
            }
        });

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

    void testUltrasonicCommunication(){
        String sendData = editText.getText().toString();
        byte[] data = sendData.getBytes();
        DataExchange dataExchange = new DataExchange(222,data,this);
        dataExchange.execute();

    }

    @Override
    public void onUsProgress(long channelId, UsChannelState usChannelState) {
        // TODO: update progress
        switch (usChannelState){
            case CONNECTING:
                textView.setText("Connecting...");
                break;
            case SENDING_DATA:
                textView.setText("Sending...");
                break;
            case RECEIVING_DATA:
                textView.setText("Receiving...");
                break;
            default:

        }

    }

    @Override
    public void onUsSuccess(long channelId, byte[] receivedData) {
        // get data from the peer
        String recvData = new String(receivedData);
        textView.setText("Receive:" + recvData);

    }

    @Override
    public void onUsFailed(long channelId, Exception exception) {
        textView.setText("Can not send or receive data!");
    }
}
