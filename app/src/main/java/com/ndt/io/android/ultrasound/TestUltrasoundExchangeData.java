package com.ndt.io.android.ultrasound;


import com.ndt.io.androidultrasound.*;

/**
 * Created by Thien.
 * An example of how to use ultrasound data exchange class
 */
public class TestUltrasoundExchangeData implements OnUsChannelListener {
    String sendData = "123456789";
    String recvData ;

    void example(){
        byte[] data = sendData.getBytes();
        DataExchange dataExchange = new DataExchange(222,data,this);
        dataExchange.execute();
    }


    @Override
    public void onUsProgress(long channelId, UsChannelState state) {
        // TODO: update progress
        switch (state){
            case CONNECTING:
                break;
            case SENDING_DATA:
                break;
            case RECEIVING_DATA:
                break;
            default:

        }

    }

    @Override
    public void onUsSuccess(long channelId, byte[] receivedData) {
        // get data from the peer
        recvData = new String(receivedData);
    }

    @Override
    public void onUsFailed(long channelId, Exception exception) {
        // TODO: update error (cannot send or receive data)

    }
}
