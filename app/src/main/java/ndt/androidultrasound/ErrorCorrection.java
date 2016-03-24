package ndt.androidultrasound;


import ndt.androidultrasound.reedsolomon.*;

/**
 * Created by Thien
 */
public class ErrorCorrection {

     /**
     * ReedSolomon code with  Galois Fields 16
     * @param aInt : a int with the value must be smaller than 4096 ( 12 bits)
     * @return encoded data ( 28 bits)
     */

    public static byte[] rs16EncodeAInt(int aInt){

        int[] input = {aInt};
        // convert the int into 12 bits
        byte[] bits = Utils.positiveIntsToBits(input, 12);

        // represent into three ints
        int[] data = Utils.bitsToPositiveInts(bits, 4);

        return rs16Encode(data);
    }

    /**
     * ReedSolomon code with  Galois Fields 16
     * @param bytes : data (in BYTES) to encode
     * @return encoded data in BITS
     */

    public static byte[] rs16Encode(byte[] bytes){
        int extra = 2*Paras.RS16_MAX_SYMBOL_ERROR;
        int[] temp = Utils.bytesToints(bytes);
        // convert the int into 8 bits
        byte[] bits = Utils.positiveIntsToBits(temp, 8);

        // represent into ints with each int for four bits
        int[] data = Utils.bitsToPositiveInts(bits, 4);

        return rs16Encode(data);
    }

    /**
     * ReedSolomon code with  Galois Fields 16
     * @param ints : the value must be smaller than 16 ( 4 bits)
     * @return encoded data in bits
     */

    private static byte[] rs16Encode(int[] ints){
        int extra = 2*Paras.RS16_MAX_SYMBOL_ERROR;

        int[] codeword = new int[ints.length + extra];
        System.arraycopy(ints, 0, codeword, 0, ints.length);
        ReedSolomonEncoder encoder = new ReedSolomonEncoder(GenericGF.DATA_MATRIX_FIELD_16);
        encoder.encode(codeword, extra);

        return  Utils.positiveIntsToBits(codeword, 4);
    }

    /**
     * ReedSolomon code with  Galois Fields 16
     * @param bits data ( must be 28 bits) to decode into a int
     * @return decoded data
     */
    public static int rs16DecodeToaInt(byte[] bits) {
        if(bits.length != (3+Paras.RS16_MAX_SYMBOL_ERROR*2)*4) return -1;
        int extra = 2*Paras.RS16_MAX_SYMBOL_ERROR;

        int[] codeword = Utils.bitsToPositiveInts(bits, 4);
        ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.DATA_MATRIX_FIELD_16);
        try {
            decoder.decode(codeword, extra);
            int[] data = new int[codeword.length - extra];
            System.arraycopy(codeword, 0, data, 0, data.length);

            byte[] decodedBits = Utils.positiveIntsToBits(data,4);// received 12 bit

            int[] results = Utils.bitsToPositiveInts(decodedBits,12);
            if(results!= null && results.length>0) return results[0];
            else return -1;
        } catch (ReedSolomonException e) {
            //e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            //e.printStackTrace();
        } catch (IllegalArgumentException e) {
            //e.printStackTrace();
        }
        return -1;
    }

    /**
     * ReedSolomon code with  Galois Fields 16
     * @param bits data (in bits) to decode
     * @return decoded data (in BYTES)
     */
    public static byte[] rs16Decode(byte[] bits) {
        int extra = 2*Paras.RS16_MAX_SYMBOL_ERROR;
        int[] codeword = Utils.bitsToPositiveInts(bits, 4);
        ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.DATA_MATRIX_FIELD_16);
        try {
            decoder.decode(codeword, extra);
            int[] data = new int[codeword.length - extra];
            System.arraycopy(codeword, 0, data, 0, data.length);

            byte[] decodedBits = Utils.positiveIntsToBits(data,4);

            return Utils.bitsToBytes(decodedBits);
        } catch (ReedSolomonException e) {
            //e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            //e.printStackTrace();
        } catch (IllegalArgumentException e) {
            //e.printStackTrace();
        }
        return null;
    }

    /**
     * ReedSolomon code with  Galois Fields 256
     * @param bytes : data (in BYTES) to encode
     * @return encoded data in BITS
     */

    public static byte[] rs256Encode(byte[] bytes){
        int[] data = Utils.bytesToints(bytes);
        // it can correct t = m*a/100 + (b-m)/c
        int a = Paras.RS256_A;
        int b = Paras.RS256_B;
        int c = Paras.RS256_C;
        int m = data.length;
        //int t = (a*c*m + 100*(b - m))/100/c;
        int t = a*m/100 + Paras.RS256_MAX_SYMBOL_ERROR;
        int extra = 2*t;
        int[] codeword = new int[m + extra];
        System.arraycopy(data, 0, codeword, 0, data.length);

        ReedSolomonEncoder encoder = new ReedSolomonEncoder(GenericGF.DATA_MATRIX_FIELD_256);
        encoder.encode(codeword, extra);
        return  Utils.positiveIntsToBits(codeword, 8);
    }

    /**
     * ReedSolomon code with  Galois Fields 256
     * @param bits data (in bits) to decode
     * @return decoded data (in BYTES)
     */
    public static byte[] rs256Decode(byte[] bits) {
        int[] codeword = Utils.bitsToPositiveInts(bits, 8);
        //t = ((a*c -100)n +100*b)/(2*a*c+ 100*(c-2))
        int a = Paras.RS256_A;
        int b = Paras.RS256_B;
        int c = Paras.RS256_C;
        int n = codeword.length;
        //int t = ((a*c -100)*n +100*b)/(2*a*c +100*(c -2));
        int t = (a*n +100*Paras.RS256_MAX_SYMBOL_ERROR)/(100+2*a);
        int extra = 2*t;
        ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.DATA_MATRIX_FIELD_256);
        try {
            decoder.decode(codeword, extra);
            int[] data = new int[codeword.length - extra];
            System.arraycopy(codeword, 0, data, 0, data.length);

            return Utils.intsTobytes(data);
        } catch (ReedSolomonException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return null;
    }

}
