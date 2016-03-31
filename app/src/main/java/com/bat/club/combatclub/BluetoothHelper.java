package com.bat.club.combatclub;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Noyloy on 02-Jan-16.
 */
public class BluetoothHelper {
    public final String SUCCESS_RESULT = "BT-SUCCESS";
    public final String NO_SUPPORT = "BT-NO-SUPPORT";
    public final String NOT_ENABLED = "BT-NOT-ENABLED";
    public final String DEVICE_NOT_FOUND = "BT-DEV-NOT-FOUND";
    public final String COMM_FAIL_RESULT = "BT-COMM-FAIL";
    public final String FAIL_RESULT = "BT-COMM-FAIL";

    final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;

    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;

    ArrayList<BluetoothDataListener> mListeners = new ArrayList<BluetoothDataListener>();

    public void registerOnNewBluetoothDataListener(BluetoothDataListener listener){
        mListeners.add(listener);
    }

    public void unregisterOnNewBluetoothDataListener(BluetoothDataListener listener){
        mListeners.remove(listener);
    }

    public String findBluetoothDevice(String deviceName){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(mBluetoothAdapter == null) return NO_SUPPORT;
        if(!mBluetoothAdapter.isEnabled()) return NOT_ENABLED;

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals(deviceName))
                {
                    mmDevice = device;
                    return SUCCESS_RESULT;
                }
            }
        }
        return DEVICE_NOT_FOUND;
    }

    public String openBluetoothCommunication(){
        try {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        }catch (IOException ex){
            return COMM_FAIL_RESULT;
        }
        mBluetoothAdapter.cancelDiscovery();
        try {
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();
        } catch (IOException connectException) {
            try {
                mmSocket.close();
                return COMM_FAIL_RESULT;
            } catch (IOException closeException) {
                return COMM_FAIL_RESULT;
            }
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                    beginListenForData();
                }
            }).start();

        return SUCCESS_RESULT;
    }

    private void beginListenForData(){
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII").trim();
                                    readBufferPosition = 0;

                                     for (BluetoothDataListener listener : mListeners){
                                        listener.onNewData(data);
                                    }
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    public String closeBT() {
        try {
            stopWorker = true;
            mmOutputStream.close();
            mmInputStream.close();
            mmSocket.close();
        }catch (IOException ex){
            return FAIL_RESULT;
        }
        return SUCCESS_RESULT;
    }

    public String sendData(char data){
        try {
            mmOutputStream.write(data);
        }catch (IOException ex){
            return FAIL_RESULT;
        }
        return SUCCESS_RESULT;
    }

}
