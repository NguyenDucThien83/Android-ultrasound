package ndt.androidultrasound;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Created by Thien on 12/4/2015.
 */
class Utils {
    /* Helpers */

    /**
     *
     * @param bits data to convert
     * @param block number of bits converted into one int
     * @return ints represent the bits
     */
    static int[] bitsToPositiveInts(byte[] bits, int block) {
        int[] ints = new int[bits.length / block];
        int[] bases = new int[block];
        bases[block-1] =1;
        for(int i = block-2;i>=0; i--){
            bases[i] = bases[i+1]*2;
        }

        for (int i = 0; i < ints.length; i++) {
            ints[i] = 0;
            int start = i*block;
            for (int j = 0; j < block; j++) {
                ints[i] += bits[start + j] * bases[j];
            }
        }
        return ints;
    }

    /**
     * convert each element in the ints into block bits
     * @param ints data to convert
     * @param block number of bits converted from one int
     * @return bits represent the ints
     */
    static byte[] positiveIntsToBits(int[] ints, int block) {
        byte[] bits = new byte[block * ints.length];
        for (int i = 0; i < ints.length; i++) {
            int start = i*block;
            if (ints[i] > Math.pow(2, block)-1 || ints[i] < 0){
                return null;
            }
            int j = block-1;
            int remainder = ints[i]%2;
            int quotient = ints[i]/2;
            bits[start+j] = (byte) remainder;
            while (quotient>0){
                remainder = quotient%2;
                quotient = quotient/2;
                j -= 1;
                bits[start+ j] =  (byte) remainder;
            }

            for (int k = 0; k<j; k++){
                bits[start+k] = 0;
            }

        }
        return bits;

    }

    static byte[] bitsToBytes(byte[] bits) {
        byte[] bytes = new byte[bits.length / 8];
        for (int i = 0; i < bytes.length; i++) {
            for (int j = 0; j < 8; j++) {
                bytes[i] |= (((bits[8 * i + j] & 0x01) << (7 - j)));
            }
        }
        return bytes;
    }

    static byte[] bytesToBits(byte[] bytes) {
        byte[] bits = new byte[8 * bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            for (int j = 0; j < 8; j++) {
                bits[8 * i + j] = (byte) ((bytes[i] >> (7 - j)) & 0x01);
            }
        }
        return bits;
    }

    static byte[] stringToBits(String msg) {
        return bytesToBits(msg.getBytes());
    }

    static String bitsToString(byte[] bits) {
        return new String(bitsToBytes(bits));
    }

    static byte[] generateRandomBits(int len) {
        byte[] bits = new byte[len];
        Random random = new Random();
        random.nextBytes(bits);
        for (int i = 0; i < bits.length; i++) {
            bits[i] &= 0x01;
        }
        return bits;

    }
    static double[] audioBytesToDoubles(byte[] bytes) {
        double[] doubles = new double[bytes.length/2];
        for (int i = 0; i < doubles.length; i++) {
            doubles[i] = (double)(bytes[2*i+0] | (bytes[2*i+1]<<8)) / Short.MAX_VALUE;
        }
        return doubles;
    }

    static byte[] audioDoubleToBytes(double[] samples) {
        byte[] bytes = new byte[samples.length*2];
        for (int i = 0; i < samples.length; i++) {
            short scaled = (short) (samples[i] * Short.MAX_VALUE);
            bytes[2*i+0] = (byte)((scaled & 0x00ff) >> 0);
            bytes[2*i+1] = (byte)((scaled & 0xff00) >> 8);
        }
        return bytes;
    }

    static double[] audioShortsToDouble(short[] samples) {
        double[] doubles = new double[samples.length];
        for (int i = 0; i < samples.length; i++) {
            doubles[i] = (double) samples[i]/ Short.MAX_VALUE;
        }
        return doubles;
    }
    static void audioShortsToDouble(short[] in, double[] out) {
        for (int i = 0; i < in.length; i++) {
            out[i] = (double) in[i]/ Short.MAX_VALUE;
        }
    }

    public static short[] audioDoubleToShorts(double[] samples) {
        short[] shorts = new short[samples.length];
        for (int i = 0; i < samples.length; i++) {
            shorts[i] = (short) (samples[i] * Short.MAX_VALUE);
        }
        return shorts;
    }
    /**
     * Value 0 - 255
     * @param input
     * @return
     */
    static int[] bytesToints(byte[] input){
        if(input == null || input.length <1){
            return null;
        }
        int[] out = new int[input.length];
        for(int i = 0; i < input.length; i++){
            out[i] = input[i];
            if(out[i] < 0){
                out[i] += 256;
            }
        }

        return out;
    }
    /**
     * Value 0 -255
     * @param input
     * @return
     */
    static byte[] intsTobytes(int[] input){
        if(input == null || input.length <1){
            return null;
        }
        byte[] out = new byte[input.length];
        for(int i = 0; i < input.length; i++){
            out[i] = (byte) input[i];
        }
        return out;
    }


    static int findSample(byte[] haystack, byte[] needle, int start){
        return findSample(haystack,needle,start, haystack.length);
    }

    static int findSample(byte[] haystack, byte[] needle, int start, int length){
        int position = start;
        int minDiff  = length;
        for (int i = start; i < (length-needle.length)+1; i++){
            int diff = 0;
            for (int j = 0; j < needle.length; j++){
                if (haystack[i+j] != needle[j]){
                    diff++;
                }
            }
            if (diff < minDiff){
                minDiff  = diff;
                position = i;
            }

        }
        Log.v("Audio Utils", "Found position: " + position + " min diff: " + minDiff);
        return position;
    }

    static void writeCSV(String fileName, double[] data, int length){
        String dir = "sdcard/OmniShare/test";
        new File(dir).mkdirs();
        File csvFile = new File(dir +"/" + fileName);
        try{
            FileOutputStream fos = new FileOutputStream(csvFile);
            DataOutputStream dos = new DataOutputStream(fos);
            for(int i =0; i< length; i++){
                double aDouble = data[i];
                String s = "" + aDouble+ ",\n";
                //Log.v("writeCSV", " value= " + s);
                //dos.writeDouble(aDouble);
                dos.writeBytes(s);
            }

        }catch (FileNotFoundException e){
            e.printStackTrace();

        }catch (IOException e){
            e.printStackTrace();
        }

    }
    static void writeLog(byte[] data, String name){
        if(data == null) return;
        String s = "";
        for(int i =0; i < data.length; i++){
            s = s + data[i];
        }
        Log.v("Audio Utils", name + ": " + s);
    }
    static int check(byte[]send, byte[] recv){
        int count = 0;
        if(send.length != recv.length) count = -1;
        else for(int i = 0; i< send.length; i++)
            if(send[i] != recv[i]) count++;
        Log.v("Audio Utils", " diff  send and recv:  " + count);
        return count;
    }
}
