package com.ndt.io.androidultrasound;

/**
 * Created by Thien on 11/24/2015.
 * This class will convert digital signal (in bits) to audio signal.
 */
class Modulator {
    /* Object-specific variables */

    protected int sampleRate;
    protected int bitRate;
    protected int[] freqs;

    protected double[][] symbols;
    protected int symbolLength;
    protected int numCheckedBits = Paras.Num_Checked_Bits;
    protected int numCheckedSamples = Paras.Num_Checked_MaxSamples;


    /* Constructors */
    protected Modulator(int sampleRate, int bitRate, int[] freqs) {
        this.sampleRate    = sampleRate;
        this.bitRate = bitRate;
        this.freqs         = freqs;
        this.symbols = new double[2][];
        symbolLength = sampleRate/bitRate;

        symbols[0] = generateDownSymbol(sampleRate, symbolLength, freqs);
        symbols[1] = generateUpSymbol(sampleRate, symbolLength, freqs);

    }

    public Modulator() {
        this(Paras.SAMPLE_RATE, Paras.BIT_RATE, Paras.FREQS);
    }

    /* modulate methods */

    /**
     * modulate data ( bit-array) into audio signal
     * @param msgBits message (in BITS) to modulate
     * @return audio signal
     */
    public short[] modulate(byte[] msgBits) {
        double[] result = new double[symbolLength * msgBits.length];
        int pos = 0;
        for (int i = 0; i < msgBits.length; i++) {
            System.arraycopy((msgBits[i] == 1 ? symbols[1] : symbols[0]), 0, result, pos, symbolLength);
            pos += symbolLength;
        }
        return Utils.audioDoubleToShorts(result);
    }

    protected static double[] generateUpSymbol(int sampleRate, int  symbolLength, int[] freqs){

        // s(t) = cos(2*Pi*f0 + mu*t/2)t)
        int T = symbolLength;
        double mu = 2* Math.PI* (freqs[1] - freqs[0])/T/4;
        double[] signal = new double[T];
        //int d = T/3;
        int d = T/2;

        // Head
        for (int i =0; i< d; i++){
            signal[i] = Math.cos((2 * Math.PI * freqs[0] + mu * i) * i / sampleRate);
            //smooth
            signal[i] =  signal[i]* i / d;
        }

        // middle
        for (int i = d; i < T - d; i++){
            signal[i] = Math.cos((2 * Math.PI * freqs[0] + mu * i) * i / sampleRate);
        }

        // Tail
        for (int i = T-d; i < T; i++){
            signal[i] = Math.cos((2 * Math.PI * freqs[0] + mu * i) * i / sampleRate);
            signal[i] =  signal[i]* (T-i) / d;
        }
        return signal;

    }

    protected static double[] generateDownSymbol(int sampleRate, int  symbolLength, int[] freqs){

        // s(t) = cos(2*Pi*f1 - mu*t/2)t)
        int T = symbolLength;
        double mu = 2* Math.PI* (freqs[1] - freqs[0])/T/4;
        double[] signal = new double[T];
        //int d = T/3;
        int d = T/2;

        // Head
        for (int i =0; i< d; i++){
            signal[i] = Math.cos((2 * Math.PI * freqs[1] - mu * i) * i / sampleRate);
            //smooth
            signal[i] =  signal[i]* i / d;
        }

        // middle
        for (int i = d; i < T - d; i++){
            signal[i] = Math.cos((2 * Math.PI * freqs[1] - mu * i) * i / sampleRate);
        }

        // Tail
        for (int i = T-d; i < T; i++){
            signal[i] = Math.cos((2 * Math.PI * freqs[1] - mu * i) * i / sampleRate);
            signal[i] =  signal[i]* (T-i) / d;
        }
        return signal;

    }


    protected static double[] generateUpSymbol1(int sampleRate, int  symbolLength, int[] freqs){

        // s(t) = cos(2*Pi*f0 + mu*t/2)t)
        int T = symbolLength;
        double mu = 2* Math.PI* (freqs[1] - freqs[0])/T;
        double[] signal = new double[T];
        //int d = T/3;
        int d = T/2;

        // Head
        for (int i =0; i< d; i++){
            signal[i] = Math.cos((2 * Math.PI * freqs[0] + mu * i) * i / sampleRate);
            //smooth
            signal[i] =  signal[i]* i / d;
        }

        // middle
        for (int i = d; i < T - d; i++){
            signal[i] = Math.cos((2 * Math.PI * freqs[0] + mu * i) * i / sampleRate);
        }

        // Tail
        for (int i = T-d; i < T; i++){
            signal[i] = Math.cos((2 * Math.PI * freqs[0] + mu * i) * i / sampleRate);
            signal[i] =  signal[i]* (T-i) / d;
        }
        return signal;

    }

    protected static double[] generateDownSymbol1(int sampleRate, int  symbolLength, int[] freqs){

        // s(t) = cos(2*Pi*f1 - mu*t/2)t)
        int T = symbolLength;
        double mu = 2* Math.PI* (freqs[1] - freqs[0])/T;
        double[] signal = new double[T];
        //int d = T/3;
        int d = T/2;

        // Head
        for (int i =0; i< d; i++){
            signal[i] = Math.cos((2 * Math.PI * freqs[1] - mu * i) * i / sampleRate);
            //smooth
            signal[i] =  signal[i]* i / d;
        }

        // middle
        for (int i = d; i < T - d; i++){
            signal[i] = Math.cos((2 * Math.PI * freqs[1] - mu * i) * i / sampleRate);
        }

        // Tail
        for (int i = T-d; i < T; i++){
            signal[i] = Math.cos((2 * Math.PI * freqs[1] - mu * i) * i / sampleRate);
            signal[i] =  signal[i]* (T-i) / d;
        }
        return signal;

    }
}
