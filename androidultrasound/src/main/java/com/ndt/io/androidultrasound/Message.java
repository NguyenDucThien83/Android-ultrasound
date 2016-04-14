package com.ndt.io.androidultrasound;

import android.util.Log;


/**
 * Created by Thien Nguyen on 12/22/2015.
 * This is the structure of ultrasound message
 * Currently, there are two types of messages: SYN (handshake) and DATA message
 * SYN message structure has only dataOfType part. length: 12xNUM_REPEAT_TYPE_SYN
 * DATA message has all part: type|syn|lengthOfEncodedBody|body|tail
 *                            12xNUM_REPEAT_TYPE_DATA|24|28|-|4
 *
 */
public class Message {
    public static final String TAG = "Message";
    public enum MessageType{
        SYN,
        DATA
    }
    private MessageType type;
    /**
     * the 8-bit dataOfType of the messages
     */
    private byte[] dataOfType = null;
    //repeat sending type of message
    static final int NUM_REPEAT_TYPE_SYN = 4;
    static final int NUM_REPEAT_TYPE_DATA = 10;

    /**
     * contained length of the encoded body
     * length in bits: 36 bits = 12 bits (indicating data) + 24 bits extra (for error correction)
     */
    int lengthOfEncodedBody;

    /**
     * the body (in bytes) of the message
     */
    private byte[] body;

    /**
     * bits of the whole message
     */
    private byte[] msgBits = null ;

    private int timeToSend;// millisecond.


    static final byte[] TYPE_SYN = {1,0,1,0,1,0,1,0,1,0,1,0}; //12bits
    static final byte[] TYPE_DATA ={0,0,1,1,0,0,1,1,0,0,1,1};

    /**
     * syn used for locating the body data in the whole message
     * TYPE_DATA + {0,0,0,0,1,1,1,1,0,0,0,0}
     */
    static final byte[] SYN = {0,0,1,1,0,0,1,1,0,0,1,1, 0,0,0,0,1,1,1,1,0,0,0,0};

    /**
     * The tail is use to prevent the player stopped suddenly.
     */
    static final byte[] TAIL = {0,0,0,0};

    // length (int bit)
    static final int LENGTH_TYPE = TYPE_SYN.length;
    static final int LENGTH_SYN = 24;
    static final int LENGTH_INFO_LENGTH = (3+2*Paras.RS16_MAX_SYMBOL_ERROR)*4;// inform the length of encoded body
    static final int LENGTH_TAIL = 4;

    //For debug
    static byte[] encSendBits = {0};



    /**
     *
     * @return SYN message
     */
    public static Message getInstanceSyn(){
        Message message = new Message();
        message.type = MessageType.SYN;
        message.dataOfType = TYPE_SYN;
        message.msgBits = getRepeatData(message.dataOfType, NUM_REPEAT_TYPE_SYN);
        message.timeToSend = message.msgBits.length*1000/Paras.BIT_RATE;
        return message;
    }

    /**
     * construct message to send
     * @param body body data in bytes
     * @return data message
     */
    public static Message getInstanceData(byte[] body){
        Message message = new Message();
        message.type = MessageType.DATA;
        message.dataOfType = TYPE_DATA;
        // data to send
        message.body = body;
        byte[] encodedBodyInBits = ErrorCorrection.rs256Encode(message.body);
        encSendBits = encodedBodyInBits;
        message.lengthOfEncodedBody = encodedBodyInBits.length;
        byte[] infoLength = ErrorCorrection.rs16EncodeAInt(message.lengthOfEncodedBody);
        Log.v(TAG, "lengthOfEncodedBody: " + message.lengthOfEncodedBody);
        Log.v(TAG, "infoLength: " + infoLength.length);
        byte[][] packet = { getRepeatData(message.dataOfType,NUM_REPEAT_TYPE_DATA), SYN, infoLength, encodedBodyInBits, TAIL};
        message.msgBits = convert2Dto1D(packet);
        message.timeToSend = message.msgBits.length*1000/Paras.BIT_RATE;
        return message;
    }
    /**
     * construct a received message
     * @param encBody encoded body data in bits
     * @param encLength encoded bits represent length of encoded body.
     * @return data message
     */
    public static Message getInstanceData(byte[] encBody,byte[] encLength){
        Message message = new Message();
        message.type = MessageType.DATA;
        message.dataOfType = TYPE_DATA;
        message.lengthOfEncodedBody = encBody.length;
        message.body = ErrorCorrection.rs256Decode(encBody);
        if(message.body == null){
            return null;
        }
        byte[][] packet = { getRepeatData(message.dataOfType,NUM_REPEAT_TYPE_DATA), SYN,encLength, encBody, TAIL};
        message.msgBits = convert2Dto1D(packet);
        message.timeToSend = message.msgBits.length*1000/Paras.BIT_RATE;
        return message;
    }


    public byte[] getBody(){
        return body;
    }

    public  MessageType getType(){
        return type;
    }

    /**
     * This method is used to get the input data to encode into audio signal
     * @return the encoded message in bits.
     */

    byte[] getEncodedMsgBits(){
        return msgBits;
    }
    public int getTimeToSend(){
        return timeToSend;
    }

    public static MessageType checkType(byte[] typeInBits, int start, int length) {

        int maxRS = getMaxSimilarity(TYPE_SYN, typeInBits, start, length);
        int maxData = getMaxSimilarity(TYPE_DATA, typeInBits, start, length);
        Log.v(TAG, " maxRS = " + maxRS);
        Log.v(TAG, " maxData = " + maxData);
        if (maxRS > maxData) {
            if(maxRS<LENGTH_TYPE) return null;
            return MessageType.SYN;
        } else {
            if (maxData < LENGTH_TYPE)return null;
            return MessageType.DATA;
        }

    }

    static int getMaxSimilarity(byte[] sub,byte[] msg,  int start, int length){
        int max = 0;
        for(int i = start; i< start + length-sub.length +1;i++){
            int countSimilar = 0;
            for (int j = 0; j < sub.length;j++){
                if(sub[j] == msg[i+j])  countSimilar++;
            }
            if (max < countSimilar) max = countSimilar;
        }
        return  max;
    }


    private static byte[] convert2Dto1D(byte[][] input){
        int length = 0;
        for (byte[] x : input){
            length += x.length;
        }
        byte[] output = new byte[length];
        int i =0;
        for(byte[] x : input)
            for (byte aByte : x){
                output[i] = aByte;
                i++;
            }
        return output;
    }

    static byte[] getRepeatData(byte[] data, int numRepeat){
        byte[] bits = new byte[numRepeat * data.length];
        int k = 0;
        for(int i = 0; i<numRepeat; i++){
            for(int j = 0; j< data.length; j++){
                bits[k] = data[j];
                k++;
            }
        }
        return bits;
    }



}
