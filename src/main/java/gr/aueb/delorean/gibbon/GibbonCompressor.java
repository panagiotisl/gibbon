package gr.aueb.delorean.gibbon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GibbonCompressor {

    public static final int ZETA_K = 2;

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private int storedVal = 0;
    private float floatStoredVal = 0;
    private boolean first = true;
    private int runSize;
    private List<Integer> zqqs;
    public final int[] cases;
    public long trailingZerosSum = 0;
    public int trailingZerosCnt = 0;

    private final OutputBitStream out;
    private int positionOfError;
	private final double epsilon;
	private final int k;
    private final int f;
    private final int[] spacePowers = new int[32];

    public GibbonCompressor(OutputBitStream output, double epsilon, int k, int f) {
        this.out = output;
        this.epsilon = epsilon;
        this.k = k;
        this.f = f;
        trailingZerosSum = 0;
        trailingZerosCnt = 0;

        for (int power = 30; power > -30; power--) {
        	if (Math.pow(2, power) < epsilon) {
        		this.positionOfError = power + 23;
        		break;
        	}
        }
        this.cases = new int[]{0, 0, 0, 0};
        for (int i=0; i<spacePowers.length; i++) {
            this.spacePowers[i] = (int) Math.pow(2, i) - 1;
        }
        this.runSize = 0;
        this.zqqs = new ArrayList<>();
    }

    public void addValue(float value) throws IOException {
    	addValueInternal(value);
    }

	/**
     * Adds a new double value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    public void addValueInternal(float value) throws IOException {
        if(first) {
            // System.out.println(k);
            writeTwoBitInteger(this.k-2);
            writeTwoBitInteger(this.f);
            writeFirst(value);
        } else {
            // compressValueSerfQt(Float.floatToRawIntBits(value));
        	compressValue(value);
        }
    }

    private void writeFirst(float value) {
    	first = false;
        storedVal = Float.floatToIntBits(value);
        floatStoredVal = value;
        out.writeInt(storedVal, 32);
    }

    private void writeTwoBitInteger(int i) throws IOException {
		this.out.writeInt(i, 2);
	}

    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    public void close() throws IOException {
        if (runSize > 11) {
            cases[0] += 1;
            writeQuantizedValue(f);
            out.writeZeta(0, k);
    		out.writeZeta(runSize, ZETA_K);
    	} else {
            cases[0] += runSize;
    		for (int i = 0; i < runSize; i++) {
    			writeSameValue(f);
    		}
    	}
        runSize = 0;
        addValue(Float.NaN);
//    	clearBuffer();
        out.writeBit(0);
        out.flush();
    }


//     private int compressValueQt(float value) throws IOException {
//         int q =  (int) Math.round((value - floatStoredVal) / (2 * epsilon));
//         int zzq = encodeZigZag(q);
//         float recoverValue = (float) (floatStoredVal + 2 * epsilon * q);
//         if (Math.abs(recoverValue - value) >= epsilon){
//             return -1;
//         }
//         out.writeBit(0);
// //        out.writeGamma(zzq);
//         out.writeZeta(zzq, k);
//         floatStoredVal = recoverValue;
//         storedVal = Float.floatToIntBits(floatStoredVal);
//         this.zqqs.add(zzq);
//         return zzq;
//     }

    // private float compressValueXOR(float floatValue) throws IOException {

    //     int value = Float.floatToRawIntBits(floatValue);
    // 	int integerDigits = (value << 1 >>> 24) - 127;
    // 	int space = Math.min(23, this.positionOfError - integerDigits);

    // 	if (space > 0) {
    // 		value = value >> space << space;
    //     	value = value | (storedVal & spacePowers[space]);
    // 	}
    // 	int xor = storedVal ^ value;

    //     int leadingZeros = Integer.numberOfLeadingZeros(xor);
    //     int trailingZeros = Integer.numberOfTrailingZeros(xor);
    //     // Check overflow of leading? Can't be 32!
    //     if(leadingZeros >= 16) {
    //         leadingZeros = 15;
    //     }

    //     if((leadingZeros >= storedLeadingZeros && trailingZeros >= storedTrailingZeros)
    //     		&& leadingZeros + trailingZeros < storedLeadingZeros + storedTrailingZeros + 9 + 0) {
    //     	cases[1] += 1;
    //         int significantBits = 32 - storedLeadingZeros - storedTrailingZeros;
    //         int temp = xor >>> storedTrailingZeros;
    //         writeCaseExistingLeading();
    //         out.writeInt(temp, significantBits);
    //     } else {

    //     	cases[2] += 1;
    //         writeCaseNewLeading();
    //         out.writeInt(leadingZeros, 4); // Number of leading zeros in the next 4 bits
    //         int significantBits = 32 - leadingZeros - trailingZeros;
    //         out.writeInt(significantBits == 32 ? 0 : significantBits, 5); // Length of meaningful bits in the next 5 bits

    //         int temp = xor >>> trailingZeros;
    //         out.writeInt(temp, significantBits); // Store the meaningful bits of XOR
    //         storedLeadingZeros = leadingZeros;
    //         storedTrailingZeros = trailingZeros;
    //     }

    //     storedVal = value;
    //     floatStoredVal = Float.intBitsToFloat(storedVal);
    //     return floatStoredVal;
    // }

    private int compressValueQtOrXor(float floatValue) throws IOException {
        int q =  (int) Math.round((floatValue - floatStoredVal) / (2 * epsilon));
        int zzq = encodeZigZag(q);
        float recoverValue = (float) (floatStoredVal + 2 * epsilon * q);

        int value = Float.floatToRawIntBits(floatValue);
    	int integerDigits = (value << 1 >>> 24) - 127;
    	int space = Math.min(23, this.positionOfError - integerDigits);

    	if (space > 0) {
    		value = value >> space << space;
        	value = value | (storedVal & spacePowers[space]);
    	}
    	int xor = storedVal ^ value;

        int leadingZeros = Integer.numberOfLeadingZeros(xor);
        int trailingZeros = Integer.numberOfTrailingZeros(xor);
        trailingZerosSum += trailingZeros;
        trailingZerosCnt++;
        if(leadingZeros >= 16) {
            leadingZeros = 15;
        }

        if((leadingZeros >= storedLeadingZeros && trailingZeros >= storedTrailingZeros)
        		&& leadingZeros + trailingZeros < storedLeadingZeros + storedTrailingZeros + 9) {

            int significantBits = 32 - storedLeadingZeros - storedTrailingZeros;
            if ((zzq >= 0) && zetaKLength(zzq, k) + 2 < significantBits && !((Math.abs(recoverValue - floatValue) >= epsilon))) {
            // if (zzq < 4096 && !((Math.abs(recoverValue - floatValue) >= epsilon))) {
                cases[1] += 1;
                writeQuantizedValue(f);
                out.writeZeta(zzq, k);
                floatStoredVal = recoverValue;
                storedVal = Float.floatToIntBits(floatStoredVal);
                this.zqqs.add(zzq);
                return -1;
            }
            cases[2] += 1;
            writeExistingLead(f);
            int temp = xor >>> storedTrailingZeros;
            out.writeInt(temp, significantBits);
        } else {

            int significantBits = 32 - leadingZeros - trailingZeros;
            if ((zzq >= 0) && (zetaKLength(zzq, k)  + 2 < 9 + significantBits) && !((Math.abs(recoverValue - floatValue) >= epsilon))) {
            // if ((zzq < 4096) && !((Math.abs(recoverValue - floatValue) >= epsilon))) {
                cases[1] += 1;
                writeQuantizedValue(f);
                out.writeZeta(zzq, k);
                floatStoredVal = recoverValue;
                storedVal = Float.floatToIntBits(floatStoredVal);
                this.zqqs.add(zzq);
                return -1;
            }
            cases[3] += 1;
            writeNewLead(f);
            out.writeInt(leadingZeros, 4); // Number of leading zeros in the next 4 bits
            out.writeInt(significantBits == 32 ? 0 : significantBits, 5); // Length of meaningful bits in the next 5 bits
            int temp = xor >>> trailingZeros;
            out.writeInt(temp, significantBits); // Store the meaningful bits of XOR
            storedLeadingZeros = leadingZeros;
            storedTrailingZeros = trailingZeros;
        }
        storedVal = value;
        floatStoredVal = Float.intBitsToFloat(storedVal);
        return 1;

    }

    private void compressValue(float value) throws IOException {

    	// if values is within error wrt the previous value, use the previous value
    	if (Math.abs(value - floatStoredVal) < epsilon) {
    		// Write 0
        	// out.writeBit(0);
            runSize++;
            return;
    	}

    	if (runSize > 11) {
            cases[0] += 1;
            writeQuantizedValue(f);
            out.writeZeta(0, k);
    		out.writeZeta(runSize, ZETA_K);
    	} else {
            cases[0] += runSize;
    		for (int i = 0; i < runSize; i++) {
                writeSameValue(f);
    		}
    	}
        runSize = 0;

        compressValueQtOrXor(value);

        return;
    }

    private void writeSameValue(int f) throws IOException {
        switch (f) {
            case 0:
                writeCase0();
                break;
            default:
                writeCase10();
                break;
        }
    }

    private void writeQuantizedValue(int f) throws IOException {
        switch (f) {
            case 0:
                writeCase10();
                break;
            case 1:
                writeCase0();
                break;
            case 2:
                writeCase110();
                break;
            case 3:
                writeCase111();
                break;
        }
    }

    private void writeExistingLead(int f) throws IOException {
        switch (f) {
            case 2:
                writeCase0();
                break;
            default:
                writeCase110();
                break;
        }
    }

    private void writeNewLead(int f) throws IOException {
        switch (f) {
            case 3:
                writeCase0();
                break;
            default:
                writeCase111();
                break;
        }
    }

    private void writeCase0() throws IOException {
        out.writeBit(0);
    }

    private void writeCase10() throws IOException {
        out.writeBit(1);
        out.writeBit(0);
    }

    private void writeCase110() throws IOException {
        out.writeBit(1);
        out.writeBit(1);
        out.writeBit(0);
    }

    private void writeCase111() throws IOException {
        out.writeBit(1);
        out.writeBit(1);
        out.writeBit(1);
    }

    public int getK() {
        double median = findMedian(zqqs);
        // System.out.println(median);
        if (median < 16) {
            return 2;
        } else if (median < 64) {
            return 3;
        } else if (median < 512) {
            return 4;
        } else {
            return 5;
        }
    }

    public int getBestMode() {
        int maxIndex = 0; // Assume first element is max
        for (int i = 1; i < cases.length; i++) {
            if (cases[i] > cases[maxIndex]) {
                maxIndex = i; // Update index if a larger value is found
            }
        }
        return maxIndex;
    }


       // Encode a signed int to an unsigned int using ZigZag
    public static int encodeZigZag(int value) {
        return (value << 1) ^ (value >> 31);
    }


    public static int zetaKLength(int n, int k) {
        // if (n <= 0 || k <= 0) {
        //     throw new IllegalArgumentException("Both n and k must be positive integers.");
        // }

        // l = ceil(log2(n + 1) / k)
        int l = (int) Math.ceil((Math.log(n + 1) / Math.log(2)) / k);

        // Total bits: (l - 1) * (k + 1) + 1
        return (l - 1) * (k + 1) + 1;
    }

    public static double findMedian(List<Integer> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return 0;
        }
        // Sort the list
        Collections.sort(numbers);

        int size = numbers.size();
        int middle = size / 2;

        // If the size is odd, return the middle element
        if (size % 2 != 0) {
            return numbers.get(middle);
        }

        // If the size is even, return the average of the two middle elements
        return (numbers.get(middle - 1) + numbers.get(middle)) / 2.0;
    }

}
