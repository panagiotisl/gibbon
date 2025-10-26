package gr.aueb.delorean.lossless;

import java.nio.ByteBuffer;

/**
 * Implements the Patas32Compressor based on the open source Gorilla implementation.
 *
 * @author Panagiotis Liakos
 */
public class Patas32Compressor extends LosslessCompressor {

    static final int PREVIOUS_VALUES = 128;
    private int storedValues[];
    private boolean first = true;
    private int previousValuesLog2;
    private int threshold;

    private final ByteBufferBitOutput out;

	private int setLsb;
	private int[] indices;
	private int index = 0;
	private int current = 0;

    // We should have access to the series?
    public Patas32Compressor() {
        out = new ByteBufferBitOutput();

        this.previousValuesLog2 =  (int)(Math.log(PREVIOUS_VALUES) / Math.log(2));
        this.threshold = 6 + previousValuesLog2;
        this.setLsb = (int) Math.pow(2, threshold + 1) - 1;
        this.indices = new int[(int) Math.pow(2, threshold + 1)];
        this.storedValues = new int[PREVIOUS_VALUES];
    }

    /**
     * Adds a new long value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    public void addValue(int value) {
        if(first) {
            writeFirst(value);
        } else {
            compressValue(value);
        }
    }

    /**
     * Adds a new double value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    @Override
    public void addValue(float value) {
        if(first) {
            writeFirst(Float.floatToRawIntBits(value));
        } else {
            compressValue(Float.floatToRawIntBits(value));
        }
    }

    private void writeFirst(int value) {
    	first = false;
        storedValues[current] = value;
        out.writeBits(storedValues[current], 32);
        indices[value & setLsb] = index;
    }

    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     *
     * @return
     */
    @Override
    public byte[] close() {
    	addValue(Float.NaN);
        out.skipBit();
        out.flush();
        ByteBuffer byteBuffer = out.getByteBuffer();
        byte[] bytes = new byte[byteBuffer.position()];
        byteBuffer.flip();
        byteBuffer.get(bytes);
        return bytes;
    }

    private void compressValue(int value) {
    	int key = value & setLsb;
    	int xor;
    	int previousIndex;
    	int trailingZeros = 0;
    	int currIndex = indices[key];
    	if ((index - currIndex) < PREVIOUS_VALUES) {
    		int tempXor = value ^ storedValues[currIndex % PREVIOUS_VALUES];
            trailingZeros = Integer.numberOfTrailingZeros(tempXor);

    		if (trailingZeros > threshold) {
    			previousIndex = currIndex % PREVIOUS_VALUES;
    			xor = tempXor;
    		} else {
    			previousIndex =  index % PREVIOUS_VALUES;
    			xor = storedValues[previousIndex] ^ value;
                trailingZeros = Integer.numberOfTrailingZeros(xor);
    		}
    	} else {
    		previousIndex =  index % PREVIOUS_VALUES;
    		xor = storedValues[previousIndex] ^ value;
            trailingZeros = Integer.numberOfTrailingZeros(xor);
    	}
        int significantBits = 0;
        int significantBytes = 0;
        if(xor == 0) {
            int x = ((previousIndex & 0x7F) << 9)       // 7 bits shifted to the top
            | ((trailingZeros & 0x3F) << 3) // 6 bits in the middle
            | (significantBytes & 0x07);    // 3 bits at the bottom
            // Write as 16 bits
            out.writeBits(x, 16);
        } else {
            int leadingZeros = Integer.numberOfLeadingZeros(xor);
            significantBits = 32 - leadingZeros - trailingZeros;
            significantBytes = (significantBits >> 3) + ((significantBits & 7) != 0 ? 1 : 0);
            // Build the 16-bit integer
            int x = ((previousIndex & 0x7F) << 9)       // 7 bits shifted to the top
            | ((trailingZeros & 0x3F) << 3) // 6 bits in the middle
            | (significantBytes & 0x07);    // 3 bits at the bottom
            // Write as 16 bits
            out.writeBits(x, 16);
            out.writeBits(xor >>> trailingZeros, significantBytes * 8); // Store the meaningful bits of XOR
    	}
        current = (current + 1) % PREVIOUS_VALUES;
        storedValues[current] = value;
		index++;
		indices[key] = index;
    }

}
