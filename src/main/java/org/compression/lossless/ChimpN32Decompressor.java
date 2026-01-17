package gr.aueb.delorean.lossless;

import gr.aueb.delorean.Decompressor;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Decompresses a compressed stream created by the Compressor. Returns pairs of timestamp and floating point value.
 *
 * @author Michael Burman
 */
public class ChimpN32Decompressor implements Decompressor {

    private static final int DEFAULT_PREVIOUS_VALUES = 64;
    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private int storedVal = 0;
    private int storedValues[];
    private int current = 0;
    private boolean first = true;
    private boolean endOfStream = false;

    private final ByteBufferBitInput in;
	private int previousValues;
	private int previousValuesLog2;

	public final static short[] leadingRepresentation = {0, 8, 12, 16, 18, 20, 22, 24};

	private final static int NAN_INT = 0x7fc00000;

    public ChimpN32Decompressor(byte[] bs) {
        this(bs, ChimpN32Compressor.DEFAULT_PREVIOUS_VALUES);
    }

    public ChimpN32Decompressor(byte[] bs, int previousValues) {
        in = new ByteBufferBitInput(ByteBuffer.wrap(bs));
        this.previousValues = previousValues;
        this.previousValuesLog2 =  (int)(Math.log(previousValues) / Math.log(2));
        this.storedValues = new int[previousValues];
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public Float readValue() {
        try {
			next();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
        if(endOfStream) {
            return null;
        }
        return Float.intBitsToFloat(storedVal);
    }

    private void next() throws IOException {
        if (first) {
        	first = false;
            storedVal = in.getInt(32);
            storedValues[current] = storedVal;
            if (storedValues[current] == NAN_INT) {
            	endOfStream = true;
            }
        } else {
        	nextValue();
        }
    }

    private void nextValue() throws IOException {
        if (in.readBit()) {
            if (in.readBit()) {
                // New leading zeros
            	storedLeadingZeros = leadingRepresentation[in.getInt(3)];
            } else {
            }
            int significantBits = 32 - storedLeadingZeros;
            if(significantBits == 0) {
                significantBits = 32;
            }
            int value = in.getInt(32 - storedLeadingZeros);
            value = storedVal ^ value;

            if (value == NAN_INT) {
            	endOfStream = true;
            	return;
            } else {
            	storedVal = value;
            	current = (current + 1) % previousValues;
    			storedValues[current] = storedVal;
            }

        } else if (in.readBit()) {
        	int fill = previousValuesLog2 + 8;
        	int temp = in.getInt(fill);
        	int index = temp >>> (fill -= previousValuesLog2) & (1 << previousValuesLog2) - 1;
        	storedLeadingZeros = leadingRepresentation[temp >>> (fill -= 3) & (1 << 3) - 1];
        	int significantBits = temp >>> (fill -= 5) & (1 << 5) - 1;
        	storedVal = storedValues[index];
        	if(significantBits == 0) {
                significantBits = 32;
            }
            storedTrailingZeros = 32 - significantBits - storedLeadingZeros;
            int value = in.getInt(32 - storedLeadingZeros - storedTrailingZeros);
            value <<= storedTrailingZeros;
            value = storedVal ^ value;
            if (value == NAN_INT) {
            	endOfStream = true;
            } else {
            	storedVal = value;
    			current = (current + 1) % previousValues;
    			storedValues[current] = storedVal;
            }
        } else {
            // else -> same value as before
            int index = in.getInt(previousValuesLog2);
            storedVal = storedValues[index];
            current = (current + 1) % previousValues;
    		storedValues[current] = storedVal;
        }
    }

}
