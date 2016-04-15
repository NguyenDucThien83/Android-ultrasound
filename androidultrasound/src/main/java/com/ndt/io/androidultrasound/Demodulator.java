package com.ndt.io.androidultrasound;

/**
 * Created by Thien on 1/5/2016.
 * This class will convert audio signal to digital signal (in bits).
 */
class Demodulator extends Modulator {
    /* Decoding methods */

    /**
     * demodulate audio signal to message in bits
     * @param signal audio signal
     * @return message in bits
     */
    public byte[] demodulate(short[] signal){

        return demodulate(Utils.audioShortsToDouble(signal));
    }

    /**
     * demodulate audio signal to message in bits
     * @param signal audio signal
     * @return message in bits
     */
    public byte[] demodulate(double[] signal){
        int[]  firstPos = findTheFirstPositionOf_a_Window(signal, symbolLength, numCheckedBits);
        return demodulate(signal, 0,signal.length, firstPos);

    }
    /**
     * demodulate audio signal to message in bits
     * @param signal audio signal
     * @param firstPos start position of signal
     * @return message in bits
     */
    public byte[] demodulate(double[] signal, int start, int length, int[] firstPos){
        int numBits = (length - (firstPos[0] + firstPos[1])/2)/symbolLength;
        byte[] bits = new byte[numBits];

        start = start/symbolLength;
        start = start*symbolLength;// make it is multiple of symbolLength

        int startCheckZero = start + firstPos[0] - numCheckedSamples/2 +1;
        int startCheckOne = start+ firstPos[1] - numCheckedSamples/2 +1;
        if(startCheckZero<0) startCheckZero = 0;
        if(startCheckOne<0) startCheckOne = 0;


        for (int i = 0; i < bits.length; i++){
            double zero_max =  getMaxStrengthOfWindow(signal, symbolLength, startCheckZero, numCheckedSamples, 0);
            double one_max  =  getMaxStrengthOfWindow(signal, symbolLength, startCheckOne, numCheckedSamples, 1);
            //Log.v("demodulate", " zero_max: " + zero_max + " one_max: " + one_max);
            if(zero_max < Paras.MIN_CORRELATION && one_max < Paras.MIN_CORRELATION){
                if(one_max > Paras.MIN_RELATIVE_CORRELATION*zero_max){
                    bits[i] = 1;
                }else{
                    // it is not ultrasound signal
                    bits[i] = 0;
                    //Log.v("demodulate", " it is not like ultrasound signal");
                }

            }else{
                bits[i] =  zero_max > one_max ? (byte)0 : (byte)1;
            }


            startCheckZero += symbolLength;
            startCheckOne += symbolLength;
        }
        return bits;
    }

    /**
     * demodulate audio signal to message in bits
     * @param signal audio signal
     * @param firstPos start position of signal
     * @param numBits number of bits needed to demodulate
     * @return message in bits
     */
    public byte[] demodulate2(double[] signal, int start, int firstPos, int numBits){
        byte[] bits = new byte[numBits];
        firstPos = firstPos - numCheckedSamples/2 +1;
        if(firstPos<0) firstPos = 0;
        for (int i = 0; i < bits.length; i++){
            int startCheck = start + i*symbolLength + firstPos;
            double zero_max =  getMaxStrengthOfWindow(signal, symbolLength, startCheck, numCheckedSamples, 0);
            double one_max  =  getMaxStrengthOfWindow(signal, symbolLength, startCheck, numCheckedSamples, 1);
            bits[i] =  zero_max > one_max ? (byte)0 : (byte)1;
        }
        return bits;
    }

    /**
     *
     * @param signal audio signal
     * @return the first position to start demodulating
     */

    public int[] findTheFisrtPosition(double[] signal, int start, int length) {
        return findTheFirstPositionOf_a_Window(signal, start, length, symbolLength, numCheckedBits);
    }

    private int[] findTheFirstPositionOf_a_Window(double[] signal, int symbolLength, int numCheckBits){
        int[] firstPoints = new int[2];
        int maxBits = signal.length/symbolLength;
        if (numCheckBits > maxBits){
            numCheckBits = maxBits;
        }
        int checkLen = numCheckBits*symbolLength;
        int start    = (signal.length - checkLen) / 2;
        double[] corrsZero = getCorrelations(signal, symbolLength, start, checkLen, 0);
        double[] corrsOne  = getCorrelations(signal, symbolLength, start, checkLen, 1);

        double maxZero = 0;
        double maxOne = 0;
        for (int i = 0; i <  symbolLength; i++){
            double sumZero = 0;
            double sumOne = 0;
            for(int j = 0; j< numCheckBits; j++){
                sumZero += corrsZero[j*symbolLength + i] ;
                sumOne += corrsOne[j*symbolLength + i];
            }
            if(maxZero < sumZero){
                maxZero  = sumZero;
                firstPoints[0] = i;
            }
            if(maxOne < sumOne){
                maxOne  = sumOne;
                firstPoints[1] = i;
            }
        }

        firstPoints[0] = (start + firstPoints[0])%symbolLength;
        firstPoints[1] = (start + firstPoints[1])%symbolLength;
        return firstPoints;
    }

    private int[] findTheFirstPositionOf_a_Window(double[] signal, int start, int length, int symbolLength, int numCheckBits){
        int[] firstPoints = new int[2];
        int maxBits = length/symbolLength;
        if (numCheckBits > maxBits){
            numCheckBits = maxBits;
        }
        int checkLen = numCheckBits*symbolLength;
        int startCheck    = start + (length - checkLen) / 2;
        double[] corrsZero = getCorrelations(signal, symbolLength, startCheck, checkLen, 0);
        double[] corrsOne  = getCorrelations(signal, symbolLength, startCheck, checkLen, 1);

        double maxZero = 0;
        double maxOne = 0;
        for (int i = 0; i <  symbolLength; i++){
            double sumZero = 0;
            double sumOne = 0;
            for(int j = 0; j< numCheckBits; j++){
                sumZero += corrsZero[j*symbolLength + i] ;
                sumOne += corrsOne[j*symbolLength + i];
            }
            if(maxZero < sumZero){
                maxZero  = sumZero;
                firstPoints[0] = i;
            }
            if(maxOne < sumOne){
                maxOne  = sumOne;
                firstPoints[1] = i;
            }
        }

        firstPoints[0] = (start + firstPoints[0])%symbolLength;
        firstPoints[1] = (start + firstPoints[1])%symbolLength;
        //Log.i(this.toString(), "1)firstPoints[0]: " + firstPoints[0] + "; firstPoints[1]:" + firstPoints[1]);
        if(Math.abs(firstPoints[0] - firstPoints[1]) > symbolLength -30){
            firstPoints[0] = firstPoints[1] = 0;
        }
        //Log.i(this.toString(), "2) firstPoints[0]: " + firstPoints[0] + "; firstPoints[1]:" + firstPoints[1]);
        return firstPoints;
    }

    private double getMaxStrengthOfWindow(double[] signal, int symbolLength, int start, int len, int bit){
        double[] correlations = getCorrelations(signal, symbolLength, start, len, bit);
        return sum(correlations);
    }

    private double[] getCorrelations(double[] signal, int symbolLength, int start, int len, int bit ){
        double[] result = new double[len];
        int offset = Math.max(0, start - symbolLength);
        for (int i = 0; i < len; i++){
            result[i] = getCorrelation(signal, symbolLength, offset+i, bit);
        }
        return result;
    }

    private double getCorrelation(double[] signal, int symbolLength, int start, int bit){
        double corr = 0;
        for (int i = 0; i < Math.min(symbolLength, signal.length - start); i++){
            double first  = signal[start+i];
            double second = symbols[bit][i];
            corr += first*second;
        }
        return corr*corr;
    }

    protected double sum(double[] array) {
        double sum = 0;
        for (double x : array)
            sum += x;
        return sum;
    }
}
