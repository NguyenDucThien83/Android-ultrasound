package com.ndt.io.androidultrasound;

import android.media.AudioFormat;

/**
 * Created by Thien on 11/24/2015.
 */
public class Paras {

    //audio
    public static int BIT_RATE = 100;
    public static final int SAMPLE_RATE = 44100; //Hz
    public static final int[] FREQS       = {16200, 17200};
    public static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    //public static int LENGTH_OF_SYMBOL = SAMPLE_RATE/BIT_RATE;
    static int getLengthOfSymbol(){
        return SAMPLE_RATE/BIT_RATE;
    }

    //public static final double AVERAGE_ENERGY_THRESHOLD = 123456789;//TODO: find the appropriate threshold

    // using for correlation computing method
    public static final int Num_Checked_Bits     = 10;
    public static final int Num_Checked_MaxSamples = 12;
    public static final double MIN_CORRELATION = 0.0005;
    public static final double MIN_RELATIVE_CORRELATION = 20;

    // using for error correction
    // for Reed solomon 256
    static int RS256_A = 20;
    static int RS256_B = 70;
    static int RS256_C = 12;

    public static final int RS256_MAX_SYMBOL_ERROR = 6;// it can correct 4 to 32 error bits

    public static final int RS16_MAX_SYMBOL_ERROR = 3;// it can correct 3 to 12 error bits

    // Errors
    //public static final int TIMEOUT = 5;//second

}
