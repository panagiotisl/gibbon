package gr.aueb.delorean.lossless;

import gr.aueb.delorean.Decompressor;

import java.io.IOException;
import java.nio.ByteBuffer;

import static gr.aueb.delorean.lossless.Patas32Compressor.PREVIOUS_VALUES;

/**
 * Decompresses a compressed stream created by the Compressor. Returns pairs of timestamp and floating point value.
 *
 * @author Michael Burman
 */
public class Patas32Decompressor implements Decompressor {

    private int storedVal = 0;
    private final int[] storedValues;
    private int current = 0;
    private boolean first = true;
    private boolean endOfStream = false;

    private final ByteBufferBitInput in;

    private final static int NAN_INT = 0x7fc00000;

    public Patas32Decompressor(byte[] bs) {
        in = new ByteBufferBitInput(ByteBuffer.wrap(bs));
        this.storedValues = new int[PREVIOUS_VALUES];
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
            	return;
            }

        } else {
        	nextValue();
        }
    }

    private void nextValue() throws IOException {
        // Read 2 bytes
        int x = in.getInt(16);  // read the 16-bit packed value
        int index = (x >> 9) & 0x7F;          // top 7 bits
        int trailingZeros = (x >> 3) & 0x3F;  // middle 6 bits
        int significantBytes = x & 0x07;      // bottom 3 bits

        int significantBits = significantBytes * 8;
        storedVal = storedValues[index];
        if(significantBits != 0) {
            int value = in.getInt(significantBits);
            value <<= trailingZeros;
            value = storedVal ^ value;
            if (value == NAN_INT) {
                endOfStream = true;
                return;
            } else {
                storedVal = value;
            }

        }
        current = (current + 1) % PREVIOUS_VALUES;
        storedValues[current] = storedVal;

    }

}
