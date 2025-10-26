package gr.aueb.delorean.gibbon;

import gr.aueb.delorean.Compressor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static gr.aueb.delorean.gibbon.ZetaOutputBitStream.ZETA_THRESHOLD;
import static gr.aueb.delorean.gibbon.ZetaOutputBitStream.zetaKLength;

public class GibbonCompressor implements Compressor {

    public static final int ZETA_K = 2;

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private int totalStoredZeros = 0;
    private int storedVal = 0;
    private float floatStoredVal = 0;
    private boolean first = true;
    private int runSize;
    private final List<Integer> zqqs;
    public final int[] cases;

    private final ByteArrayOutputStream byteOut;
    private final ZetaOutputBitStream out;
    private int positionOfError;
	private final double epsilon;
    private final double twoEpsilon;
    private final double invTwoEpsilon;

	private final int k;
    private final int f;
    private final int[] spacePowers = new int[32];

    public GibbonCompressor(double epsilon, int k, int f) {
        this.byteOut = new ByteArrayOutputStream();
        this.out = new ZetaOutputBitStream(byteOut);
        this.epsilon = epsilon;
        this.twoEpsilon = 2 * epsilon;
        this.invTwoEpsilon = 1 / twoEpsilon;
        this.k = k;
        this.f = f;

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
        this.zqqs = new ArrayList<>(1000);
    }

    @Override
    public void addValue(float value) throws IOException {
        if(first) {
            this.out.writeInt(this.k-2, 2);
            this.out.writeInt(f, 2);
            first = false;
            storedVal = Float.floatToIntBits(value);
            floatStoredVal = value;
            this.out.writeInt(storedVal, 32);
        } else {
        	compressValue(value);
        }    }

    @Override
    public byte[] close() throws IOException {
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
        out.writeBit(false);
        out.flush();
        return byteOut.toByteArray();
    }

    private void compressValueQtOrXor(float floatValue) throws IOException {
        int q =  (int) Math.round((floatValue - floatStoredVal) * invTwoEpsilon);
        int zzq = encodeZigZag(q);
        float recoverValue = (float) (floatStoredVal + twoEpsilon * q);

        int value = Float.floatToRawIntBits(floatValue);
        int exponent = Math.getExponent(floatValue);
        int space = Math.min(23, this.positionOfError - exponent);

    	if (space > 0) {
    		value = value >> space << space;
        	value = value | (storedVal & spacePowers[space]);
    	}
    	int xor = storedVal ^ value;

        int leadingZeros = Integer.numberOfLeadingZeros(xor);
        int trailingZeros = Integer.numberOfTrailingZeros(xor);
        if(leadingZeros >= 16) {
            leadingZeros = 15;
        }
        int totalZeros = leadingZeros + trailingZeros;

        if((leadingZeros >= storedLeadingZeros && trailingZeros >= storedTrailingZeros)
        		&& totalZeros < totalStoredZeros + 9) {

            int significantBits = 32 - totalStoredZeros;
            if ((zzq >= 0) && (zzq < ZETA_THRESHOLD) && zetaKLength(zzq, k) + 2 < significantBits && !((Math.abs(recoverValue - floatValue) >= epsilon))) {
                cases[1] += 1;
                writeQuantizedValue(f);
                out.writeZeta(zzq, k);
                floatStoredVal = recoverValue;
                storedVal = Float.floatToIntBits(floatStoredVal);
                this.zqqs.add(zzq);
                return;
            }
            cases[2] += 1;
            writeExistingLead(f);
            int temp = xor >>> storedTrailingZeros;
            out.writeInt(temp, significantBits);
        } else {

            int significantBits = 32 - totalZeros;
            if ((zzq >= 0) && (zzq < ZETA_THRESHOLD) &&  (zetaKLength(zzq, k)  + 2 < 9 + significantBits) && !((Math.abs(recoverValue - floatValue) >= epsilon))) {
                cases[1] += 1;
                writeQuantizedValue(f);
                out.writeZeta(zzq, k);
                floatStoredVal = recoverValue;
                storedVal = Float.floatToIntBits(floatStoredVal);
                this.zqqs.add(zzq);
                return;
            }
            cases[3] += 1;
            writeNewLead(f);
            out.writeInt(leadingZeros, 4); // Number of leading zeros in the next 4 bits
            out.writeInt(significantBits == 32 ? 0 : significantBits, 5); // Length of meaningful bits in the next 5 bits
            int temp = xor >>> trailingZeros;
            out.writeInt(temp, significantBits); // Store the meaningful bits of XOR
            storedLeadingZeros = leadingZeros;
            storedTrailingZeros = trailingZeros;
            totalStoredZeros = storedLeadingZeros + storedTrailingZeros;
        }
        storedVal = value;
        floatStoredVal = Float.intBitsToFloat(storedVal);
    }

    private void compressValue(float value) throws IOException {
    	// if values is within error wrt the previous value, use the previous value
    	if (Math.abs(value - floatStoredVal) < epsilon) {
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
    }

    private void writeSameValue(int f) throws IOException {
        if (f == 0) writeCase0();
        else writeCase10();
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
        if (f == 2) writeCase0();
        else writeCase110();
    }

    private void writeNewLead(int f) throws IOException {
        if (f == 3) writeCase0();
        else writeCase111();
    }

    private void writeCase0() throws IOException {
        out.writeBit(false);
    }

    private void writeCase10() throws IOException {
        out.writeInt(2, 2);
    }

    private void writeCase110() throws IOException {
        out.writeInt(6, 3);
    }

    private void writeCase111() throws IOException {
        out.writeInt(7, 3);
    }

    public int getK() {
        double median = findMedian(zqqs);
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

    public static int encodeZigZag(int value) {
        return (value << 1) ^ (value >> 31);
    }

    public static double findMedian(List<Integer> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return 0;
        }
        Collections.sort(numbers);

        int size = numbers.size();
        int middle = size / 2;

        if (size % 2 != 0) {
            return numbers.get(middle);
        }

        return (numbers.get(middle - 1) + numbers.get(middle)) / 2.0;
    }

}
