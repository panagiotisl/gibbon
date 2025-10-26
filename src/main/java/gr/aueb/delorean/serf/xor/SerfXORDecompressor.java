package gr.aueb.delorean.serf.xor;

import java.io.IOException;

import gr.aueb.delorean.Decompressor;
import gr.aueb.delorean.serf.InputBitStream;
import gr.aueb.delorean.serf.Value;

public class SerfXORDecompressor implements Decompressor {

    private long storedVal = Double.doubleToLongBits(2.0);

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = Integer.MAX_VALUE;

    private final int[] leadingRepresentation = {0, 8, 12, 16, 18, 20, 22, 24};
    private final int[] trailingRepresentation = {0, 22, 28, 32, 36, 40, 42, 46};
    private final int leadingBitsPerValue = 3;
    private final int trailingBitsPerValue = 3;

    private final long adjustDigit;

    // branch_less_table from header
    private final int[] branchLessTable = {
        32, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
        11, 12, 13, 14, 15, 16, 17, 18, 19,
        20, 21, 22, 23, 24, 25, 26, 27, 28,
        29, 30, 31
    };

	private boolean first = false;

	private final InputBitStream in;

	public SerfXORDecompressor(byte[] encoded) {
		in = new InputBitStream(encoded);
		this.adjustDigit = 0;
	}

	public Float readValue() throws IOException {
		next();
		return (float) new Value(storedVal).getFloatValue();
	}

	private void next() throws IOException {
		if (first) {
			first = false;
			storedVal = in.readInt(64);
		} else {
			nextValue();
		}
	}

	private void nextValue() throws IOException {
		long value = storedVal;
		int centerBits;
		if (in.readInt(1) == 1) {
			centerBits = 64 - storedLeadingZeros - storedTrailingZeros;
			long read = in.readInt(centerBits);
			value = (read << storedTrailingZeros) ^ storedVal;
		} else if (in.readInt(1) == 0) {
			int leadAndTrail = (int) in.readInt(leadingBitsPerValue + trailingBitsPerValue);
			int trailMask = (trailingBitsPerValue >= 31) ? -1 : ((1 << trailingBitsPerValue) - 1);
			int lead = leadAndTrail >> trailingBitsPerValue;
			int trail = leadAndTrail & trailMask;
			storedLeadingZeros = leadingRepresentation[lead];
			storedTrailingZeros = trailingRepresentation[trail];
			centerBits = 64 - storedLeadingZeros - storedTrailingZeros;
			long read = in.readInt(centerBits);
			value = (read << storedTrailingZeros) ^ storedVal;
		}
		storedVal = value;
	}

}
