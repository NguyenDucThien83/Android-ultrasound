package com.ndt.io.androidultrasound;

import android.os.AsyncTask;
import android.util.Log;

import java.util.Random;

/**
 * The DataExchange class establish the ultrasound connection and exchange data
 * Created by Thien on 12/21/2015.
 */

public class DataExchange extends AsyncTask<Void,UsChannelState,Boolean> {
    static final String TAG = "DataExchange";

    enum Action{
        SEND_SYN,
        RECEIVE_SYN_OR_DATA,
        RECEIVE_DATA,
        SEND_DATA
    }
    Action action;

    private OnUsChannelListener listener;

    private long id;
    private Exception exception;

    private UsSender sender;
    private UsReceiver receiver;
    private Message synMsg;
    private Message sendMsg;
    private Message recvMsg;


    /**
     * The DataExchange class establish the ultrasound connection and exchange data
     * @param sendData      Data to send
     * @param listener  Listener to receive callbacks while handshake or exchange data.
     */

    public DataExchange(long id, byte[] sendData, OnUsChannelListener listener){
        Log.v(TAG, "construct");
        this.id = id;
        this.listener = listener;
        sendMsg = Message.getInstanceData(sendData);
        synMsg = Message.getInstanceSyn();

        sender = new UsSender(12345, onUsSendListener);
        receiver = new UsReceiver(12345, onUsReceiveListener);

        exception = new Exception("Error in exchange data via ultrasound");

    }


    private int getTimeForNextSyn(int timeToSendASyn, int countSendSyn){
        int tmp = timeToSendASyn/Message.NUM_REPEAT_TYPE_SYN;
        int time = 200 + (Message.NUM_REPEAT_TYPE_SYN +2 )* tmp + new Random().nextInt(8)*tmp/2;
        return time;
    }
    boolean hasReceivedData;
    boolean hasAnError;

    @Override
    public Boolean doInBackground(Void... voids) {
        Log.v(TAG, "doInBackground");

        action = Action.SEND_SYN;
        hasReceivedData = false;
        hasAnError = false;
        int timeToSendASyn = synMsg.getTimeToSend();
        int countSendSyn = 0;
        publishProgress(UsChannelState.CONNECTING);
        while (true){
            if(isCancelled()) break;
            if(hasAnError) return false;
            if(countSendSyn>10){
                exception = new Exception("Timeout: Cannot connect to another device.");
                return false;
            }
            switch (action){
                case SEND_SYN:
                    Log.v(TAG, " start sending SYN");
                    //publishProgress("Sending SYN");

                    sender.send(synMsg);
                    action = Action.RECEIVE_SYN_OR_DATA;
                    countSendSyn++;
                    Log.v(TAG, " countSendSyn: " + countSendSyn);
                    break;
                case RECEIVE_SYN_OR_DATA:
                    Log.v(TAG, "waite for receiving SYN or DATA");
                    //publishProgress("Receiving");
                    int timeOut = getTimeForNextSyn(timeToSendASyn, countSendSyn);
                    Log.i(TAG, "recording Time: " + timeOut);
                    if(receiver.receive(timeOut) != null) action = Action.SEND_DATA;
                    else action = Action.SEND_SYN;
                    break;
                case SEND_DATA:

                    Log.v(TAG, "start sending DATA");
                    //publishProgress("Sending Data");
                    publishProgress(UsChannelState.SENDING_DATA);
                    sender.send(sendMsg);
                    if(!hasReceivedData){
                        action= Action.RECEIVE_DATA;
                    } else{
                        // check if the peer also receives data successfully
                        Message expectSyn =  receiver.receive(3000);
                        if(expectSyn != null && expectSyn.getType() == Message.MessageType.SYN){
                            return true;
                        }
                        return false;
                    }
                    break;
                case RECEIVE_DATA:
                    Log.v(TAG, "start receiving DATA");
                    int timeout = sendMsg.getTimeToSend();
                    Message message =  receiver.receive(timeout);
                    if (message != null && message.getType() == Message.MessageType.DATA){
                        //confirm success
                        sender.send(synMsg);
                        return true;
                    } else{
                        return false;
                    }
            }

        }
        return null;
    }

    @Override
    protected void onProgressUpdate(UsChannelState... progress) {
        listener.onUsProgress(id, progress[0]);
    }

    @Override
    protected void onPostExecute(Boolean status){
        if(listener != null)
            if(status){
                Log.v(TAG, "onUsSuccess");
                listener.onUsSuccess(id, recvMsg.getBody());
            }else{
                Log.v(TAG, "onUsFailed");
                listener.onUsFailed(id, exception);
            }
    }

    @Override
    protected void onCancelled(){
        listener.onUsFailed(id, new Exception("process was cancelled"));
        Log.v(TAG, "onCancelled");
    }


    OnUsSendListener onUsSendListener = new OnUsSendListener() {
        @Override
        public void onSendProgress(long sendId, int percentDone) {
        }

        @Override
        public void onSendFinish(long sendId) {
            //publishProgress("finish send");
            Log.v(TAG, "finish send");
        }

        @Override
        public void onSendFailed(long sendId, Exception exp) {
            //publishProgress("Cannot send Data");
            exception = exp;
            hasAnError = true;
        }
    };

    OnUsReceiveListener onUsReceiveListener = new OnUsReceiveListener() {
        @Override
        public void onReceiveProgress(long receiveId, int percentDone, String info, UsChannelState state) {
            //publishProgress(info);
            if(state == UsChannelState.RECEIVING_DATA){
                publishProgress(state);
            }
        }

        @Override
        public void onReceiveSuccess(long receiveId, Message message) {
            if(message.getType() == Message.MessageType.DATA){
                recvMsg = message;
                hasReceivedData = true;
                //publishProgress("Received Data");
                Log.v(TAG, "Received Data");
            }else if(message.getType() == Message.MessageType.SYN){
                //publishProgress("Received SYN");
                Log.v(TAG, "Received SYN");
            } else{
                //publishProgress("Received unknown");
                Log.v(TAG, "Received unknown");
            }
        }

        @Override
        public void onReceiveFailed(long receiveId, Exception exp) {
            //publishProgress("Cannot receive Data");
            exception = exp;
            hasAnError = true;
        }

        @Override
        public void onReceiveTimeout(long receiveId) {
            //publishProgress("TimeOut: cannot receive message");
            exception = new Exception("TimeOut: cannot receive message");
        }
        @Override
        public void onReceiveCancel(long receiveId){

        }

    };


}
