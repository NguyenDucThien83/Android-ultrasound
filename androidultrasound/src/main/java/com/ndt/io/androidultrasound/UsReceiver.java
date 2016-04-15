package com.ndt.io.androidultrasound;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by Thien
 */
public class UsReceiver {
    static final String TAG = "UsReceiver";

    private enum ProcessStep{
        // excellent
        CHECK_SIGNAL,
        CHECK_SYN_AND_LENGTH,
        GET_DATA
    }

    private enum FinishType{
        SUCCESS,
        TIMEOUT,
        CANCEL,
        FAIL
    }

    Modulator modulator;
    Demodulator demodulator;
    Filter filter;

    private ProcessStep procStep;
    double[] buffer = new double[15*Paras.SAMPLE_RATE];// enough space for 15 seconds
    int countBuffer = 0;
    private final int CHUNK = (Message.LENGTH_TYPE*2) * Paras.getLengthOfSymbol();


    private final int REQ_LENGTH_CHECK_SYN_AND_LENGTH = (Message.NUM_REPEAT_TYPE_DATA +2)*Message.LENGTH_TYPE
                                                + Message.LENGTH_SYN + Message.LENGTH_INFO_LENGTH;
    private int req_length_getData = 0;

    //recorder
    private AudioRecord recorder;

    private boolean isStopImmediately;

    private OnUsReceiveListener listener;
    private Exception exception;
    long receiverID;

    Message receivedMsg = null;




    public UsReceiver(long receiverID, OnUsReceiveListener onUsReceiveListener){
        Log.v(TAG, "construct");
        this.receiverID = receiverID;
        listener = onUsReceiveListener;
        initRecorder();
        exception = new Exception("Error in receiving audio message");
        modulator = new Modulator();
        demodulator = new Demodulator();
        filter = new Filter();
    }

    /**
     * Start receiving audio signal.
     * It will be stopped automatically if the task is done or the stop() method is called or timeout.
     */

    public Message receive( int timeout){
        Log.v(TAG, "receive");
        procStep = ProcessStep.CHECK_SIGNAL;
        FinishType result =  execute(timeout);
        onPostExecute(result);
        return receivedMsg;
    }

    /**
     * Stop receiving immediately.
     */

    public void stop(){
        isStopImmediately = true;
    }

    byte[] receiveBits = new byte[1000];
    int countBits = 0;
    void putReceiveBits(byte[] bits){
        for(int i = 0; i < bits.length; i++){
            receiveBits[countBits] = bits[i];
            countBits++;
        }
    }

    // the first position of ultrasound signal in audio data.
    int firstPos[] = null;

    /**
     * This is a magic function!!!
     * @return success or false
     */
static boolean TESTDECODING = false;
    protected FinishType execute(int timeout) {
        if(recorder.getState() != AudioRecord.STATE_INITIALIZED){
            exception = new Exception("Cannot initialize audio recorder");
            return FinishType.FAIL;
        }
        Log.v(TAG, "execute");
        long maxRead = timeout*Paras.SAMPLE_RATE/1000;
        isStopImmediately = false;
        int totalRead = 0;
        int lengthOfEncBody = -1;
        int bodyPos = -1;
        byte[] infoLength = null;

        Log.v(TAG, "startRecording");
        recorder.startRecording();


        double[] chunks = new double[CHUNK];
        short[] shortBuffer = new short[chunks.length];
        boolean checkFirstPosAgain = true;


        while (true) {
            if(isStopImmediately){
                return FinishType.CANCEL;
            }
            //1. read audio data
            totalRead += recorder.read(shortBuffer, 0, shortBuffer.length);
            for (int i = 0; i < shortBuffer.length; i++) {
                chunks[i]  = (double) shortBuffer[i] / Short.MAX_VALUE;
            }
            //2. process
            switch (procStep){

                case CHECK_SIGNAL:
                    Log.v(TAG, "CHECK_SIGNAL");
                    if(totalRead>maxRead){
                        Log.v(TAG, "TIMEOUT");
                        return FinishType.TIMEOUT;
                    }
                    Message.MessageType type = checkSignal(chunks);
                    if(type== null){
                        continue;
                    }else if(type == Message.MessageType.SYN){
                        // Create SYN message and finish
                        receivedMsg = Message.getInstanceSyn();
                        // finish
                        return FinishType.SUCCESS;
                    }else {
                        // Data message, move to next step.
                        onProgressUpdate(0, "Receiving data", UsChannelState.RECEIVING_DATA);
                        procStep = ProcessStep.CHECK_SYN_AND_LENGTH;
                        //TESTDECODING
                        TESTDECODING = true;


                    }
                    break;
                case CHECK_SYN_AND_LENGTH:
                    if(checkFirstPosAgain){
                        int newPos[] = demodulator.findTheFisrtPosition(chunks,0, chunks.length);
                        if(Math.abs(firstPos[0] - firstPos[1]) > Math.abs(newPos[0] - newPos[1])){
                            firstPos = newPos;
                        }
                        checkFirstPosAgain = false;
                    }

                    Log.v(TAG, "CHECK_SYN_AND_LENGTH");
                    addSignal(chunks);
                    if(countBits >= REQ_LENGTH_CHECK_SYN_AND_LENGTH){

                        int synPos = Utils.findSample(receiveBits, Message.SYN, 0, countBits);
                        int numTry = 0;
                        while (numTry < 3){
                            bodyPos = synPos+ Message.LENGTH_SYN + Message.LENGTH_INFO_LENGTH;
                            infoLength = Arrays.copyOfRange(receiveBits,
                                    synPos + Message.LENGTH_SYN, bodyPos);

                            lengthOfEncBody = ErrorCorrection.rs16DecodeToaInt(infoLength);
                            Log.v(TAG, "lengthOfEncBody: " + lengthOfEncBody);
                            if(lengthOfEncBody>0){
                                break;
                            }
                            numTry++;
                            if(numTry == 1) synPos -= 1;// check synPos -1
                            else synPos += 2; // check synPos +1
                        }

                        if(lengthOfEncBody<0){
                            exception = new Exception(" Cannot get length of ultrasound message");
                            return FinishType.FAIL;
                        }

                        // move to next step
                        req_length_getData = REQ_LENGTH_CHECK_SYN_AND_LENGTH + lengthOfEncBody;
                        procStep = ProcessStep.GET_DATA;
                    }
                    break;
                case GET_DATA:
                    Log.v(TAG, "GET_DATA");
                    addSignal(chunks);
                    if (countBits >= req_length_getData){
                        byte[] encBody = Arrays.copyOfRange(receiveBits, bodyPos, bodyPos + lengthOfEncBody);

                        receivedMsg = Message.getInstanceData(encBody, infoLength);
                        // finish
                        if(receivedMsg == null){
                            exception = new Exception(" received message is null");
                            return FinishType.FAIL;
                        }else{
                            return FinishType.SUCCESS;
                        }

                    }
                    break;
                default:
                    break;
            }
        }

    }

    protected void onProgressUpdate(int percentDone, String info, UsChannelState state) {
        if(listener != null ){
            listener.onReceiveProgress(receiverID, percentDone, info, state);
        }

    }



    protected void onPostExecute(FinishType status) {
        reset();
        switch (status){
            case SUCCESS:
                Log.v(TAG, "ReceiveSuccess");
                listener.onReceiveSuccess(receiverID, receivedMsg);
                break;
            case TIMEOUT:
                listener.onReceiveTimeout(receiverID);
                break;
            case CANCEL:
                listener.onReceiveCancel(receiverID);
                break;
            case FAIL:
                Log.v(TAG, "ReceiveFailed");
                listener.onReceiveFailed(receiverID, exception);
                exception.printStackTrace();
                break;
        }
    }

    // Signal processing

    Message.MessageType checkSignal(double[] signal){
        firstPos = demodulator.findTheFisrtPosition(signal,0, signal.length);
        if(Math.abs(firstPos[0] - firstPos[1]) > 50 && Math.abs(firstPos[0] - firstPos[1]) < Paras.getLengthOfSymbol() -50) return null;

        byte[] newBits = demodulator.demodulate(signal,0, signal.length, firstPos);
        putReceiveBits(newBits);
        int startCheck = countBits-newBits.length - Message.LENGTH_TYPE +1;
        if(startCheck <0){
            startCheck =0;
        }
        int length = countBits - startCheck;
        Message.MessageType type = Message.checkType(receiveBits, startCheck, length);

        if(type == Message.MessageType.DATA){
            countBits = 0;
            countBuffer = 0;
            putReceiveBits(newBits);
            for(int i = 0; i < signal.length; i++){
                buffer[countBuffer] = signal[i];
                countBuffer++;
            }

        }
        return type;
    }

   void addSignal(double[] signal){
       //CHECK_SYN_AND_LENGTH || GET_DATA
       for(int i = 0; i < signal.length; i++){
           buffer[countBuffer] = signal[i];
           countBuffer++;
       }
       int newStartDemodulating = countBits*Paras.getLengthOfSymbol();
       byte[] newBits = demodulator.demodulate(buffer, newStartDemodulating,
               countBuffer - newStartDemodulating+1, firstPos);
       putReceiveBits(newBits);

    }

    byte[] checkEncBody(){
        int[] pos = demodulator.findTheFisrtPosition(buffer,0,countBuffer);
        byte[] bits = demodulator.demodulate(buffer, 0, countBuffer, pos);
        int synPos = Utils.findSample(bits, Message.SYN, 0, REQ_LENGTH_CHECK_SYN_AND_LENGTH);
        int bodyPos = synPos+ Message.LENGTH_SYN + Message.LENGTH_INFO_LENGTH;
        byte[] infoLength = Arrays.copyOfRange(bits,
                synPos + Message.LENGTH_SYN, bodyPos);

        int lengthOfEncBody = ErrorCorrection.rs16DecodeToaInt(infoLength);
        Log.v(TAG, "lengthOfEncBody: " + lengthOfEncBody);
        if(lengthOfEncBody<0){
            return null;
        }
        byte[] encBody = Arrays.copyOfRange(bits, bodyPos, bodyPos + lengthOfEncBody);
        return  encBody;
    }


    /* player*/

    protected boolean initRecorder(){

        int source    = MediaRecorder.AudioSource.MIC;
        int minBufferSize = AudioRecord.getMinBufferSize(Paras.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, Paras.AUDIO_ENCODING);
        Log.v(TAG, "minBuffer size: " + minBufferSize);

        // construct recorder
        recorder = new AudioRecord( source, Paras.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                Paras.AUDIO_ENCODING, 2* Paras.SAMPLE_RATE); // 1 seconds
        if(recorder.getState() != AudioRecord.STATE_INITIALIZED){
            Log.v(TAG, "Cannot initialize audio record");
            return  false;
        }
        return true;
    }

    /**
     * Stop recording and prepare recorder for using in the future.
     */
    protected void reset(){
        recorder.stop();
        recorder.release();
        countBuffer = 0;
        countBits = 0;
        initRecorder();
    }

}
