package com.bat.club.combatclub;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

public class BluetoothReceiverExample extends Activity implements BluetoothDataListener
{
    TextView myLabel;
    EditText myTextbox;
    EditText health;

    BluetoothHelper mBTHelper;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mBTHelper = new BluetoothHelper();
        mBTHelper.registerOnNewBluetoothDataListener(this);

        Button openButton = (Button)findViewById(R.id.open);
        Button sendButton = (Button)findViewById(R.id.send);
        Button closeButton = (Button)findViewById(R.id.close);
        myLabel = (TextView)findViewById(R.id.label);
        myTextbox = (EditText)findViewById(R.id.entry);
        health = (EditText)findViewById(R.id.entry1);

        //Open Button
        openButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {

                String bt_res = mBTHelper.findBluetoothDevice("HC-06");

                if (bt_res.equals(mBTHelper.NOT_ENABLED)) Toast.makeText(BluetoothReceiverExample.this,"Try to Enable Your Bluetooth first",Toast.LENGTH_SHORT).show();
                else if (bt_res.equals(mBTHelper.NO_SUPPORT)) Toast.makeText(BluetoothReceiverExample.this,"Your Phone Doesn't support Bluetooth",Toast.LENGTH_SHORT).show();
                else if (bt_res.equals(mBTHelper.DEVICE_NOT_FOUND)) Toast.makeText(BluetoothReceiverExample.this,"Device Not Found",Toast.LENGTH_SHORT).show();
                else mBTHelper.openBluetoothCommunication();

            }
        });

        //Send Button
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBTHelper.sendData('*');
            }
        });

        //Close button
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBTHelper.closeBT();
            }
        });
    }



    @Override
    public void onNewData(final String data) {
        final String[] dataParsed = data.split(",");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch(dataParsed[0]){
                    case "a":
                        myTextbox.setText("Magazine: "+dataParsed[1]);
                        break;
                    case "h":
                        health.setText("health: "+dataParsed[1]+"%");
                        break;
                    case "r":
                        Toast.makeText(BluetoothReceiverExample.this,"Reloading",Toast.LENGTH_SHORT).show();
                        break;
                }
                myLabel.setText(data);
            }
        });
    }
}
