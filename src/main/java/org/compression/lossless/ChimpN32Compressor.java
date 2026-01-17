package gr.aueb.delorean.lossless;

import java.nio.ByteBuffer;

/**
 * Implements the Chimp128 time series compression. Value compression
 * is for floating points only.
 *
 * @author Panagiotis Liakos
 */
public class ChimpN32Compressor extends LosslessCompressor {

	public static final int DEFAULT_PREVIOUS_VALUES = 64;
	private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedValues[];
    private boolean first = true;
    private int previousValuesLog2;
    private int threshold;

    public final static short[] leadingRepresentation = {0, 0, 0, 0, 0, 0, 0, 0,
			1, 1, 1, 1, 2, 2, 2, 2,
			3, 3, 4, 4, 5, 5, 6, 6,
			7, 7, 7, 7, 7, 7, 7, 7,
			7, 7, 7, 7, 7, 7, 7, 7,
			7, 7, 7, 7, 7, 7, 7, 7,
			7, 7, 7, 7, 7, 7, 7, 7,
			7, 7, 7, 7, 7, 7, 7, 7
		};

    public final static short[] leadingRound = {0, 0, 0, 0, 0, 0, 0, 0,
			8, 8, 8, 8, 12, 12, 12, 12,
			16, 16, 18, 18, 20, 20, 22, 22,
			24, 24, 24, 24, 24, 24, 24, 24,
			24, 24, 24, 24, 24, 24, 24, 24,
			24, 24, 24, 24, 24, 24, 24, 24,
			24, 24, 24, 24, 24, 24, 24, 24,
			24, 24, 24, 24, 24, 24, 24, 24
		};

	private final ByteBufferBitOutput out;
	private int previousValues;

	private int setLsb;
	private int[] indices;
	private int index = 0;
	private int current = 0;

	public ChimpN32Compressor() {
		this(DEFAULT_PREVIOUS_VALUES);
	}

    public ChimpN32Compressor(int previousValues) {
		out = new ByteBufferBitOutput();
        this.previousValues = previousValues;
        this.previousValuesLog2 =  (int)(Math.log(previousValues) / Math.log(2));
        this.threshold = 5 + previousValuesLog2;
        this.setLsb = (int) Math.pow(2, threshold + 1) - 1;
        this.indices = new int[(int) Math.pow(2, threshold + 1)];
        this.storedValues = new int[previousValues];
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
    	int trailingZeros;
    	int currIndex = indices[key];
    	if ((index - currIndex) < previousValues) {
    		int tempXor = value ^ storedValues[currIndex % previousValues];
            trailingZeros = Integer.numberOfTrailingZeros(tempXor);

    		if (trailingZeros > threshold) {
    			previousIndex = currIndex % previousValues;
    			xor = tempXor;
    		} else {
    			previousIndex =  index % previousValues;
    			xor = storedValues[previousIndex] ^ value;
    			trailingZeros = Integer.numberOfTrailingZeros(xor);
    		}
    	} else {
    		previousIndex =  index % previousValues;
    		xor = storedValues[previousIndex] ^ value;
    		trailingZeros = Integer.numberOfTrailingZeros(xor);
    	}

        if(xor == 0) {
            // Write 0
        	out.skipBit();
        	out.skipBit();
            out.writeBits(previousIndex, previousValuesLog2);
            storedLeadingZeros = 33;
        } else {
            int leadingZeros = Integer.numberOfLeadingZeros(xor);

            if (trailingZeros > threshold) {
                int significantBits = 32 - leadingRound[leadingZeros] - trailingZeros;
                out.writeBits(256L * (previousValues + previousIndex) + 32 * leadingRepresentation[leadingZeros] + significantBits, previousValuesLog2 + 10);
                out.writeBits(xor >>> trailingZeros, significantBits); // Store the meaningful bits of XOR
    			storedLeadingZeros = 33;
    		} else if (leadingRound[leadingZeros] == storedLeadingZeros) {
    			out.writeBit();
    			out.skipBit();
    			int significantBits = 32 - leadingRound[leadingZeros];
    			out.writeBits(xor, significantBits);
    		} else {
    			storedLeadingZeros = leadingRound[leadingZeros];
    			int significantBits = 32 - leadingRound[leadingZeros];
    			out.writeBits(16 + 8 + leadingRepresentation[leadingZeros], 5);
    			out.writeBits(xor, significantBits);
    		}
    	}
        current = ((current + 1) % previousValues);
        storedValues[current] = value;
		index++;
		indices[key] = index;

    }

}
