package gr.aueb.delorean.gibbon;

import java.io.IOException;

/**
 * Decompresses a compressed stream created by the Compressor. Returns pairs of timestamp and floating point value.
 *
 * @author Michael Burman
 */
public class GibbonDecompressor {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private int storedVal = 0;
    private int k;
	private int f;
    private boolean first = true;
    private boolean endOfStream = false;
	private double epsilon;
	private int runSize = 0;

    private final InputBitStream in;

    private final static int NAN_INT = 0x7fc00000;


    public GibbonDecompressor(InputBitStream input, double epsilon) {
        in = input;
		this.epsilon = epsilon;
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public Value readValue() throws IOException {
        next();
        if(endOfStream) {
            return null;
        }
        return new Value(storedVal);
    }

    private void next() throws IOException {
        if (first) {
        	first = false;
			this.k = in.readInt(2) + 2;
			this.f = in.readInt(2);
            storedVal = in.readInt(32);
            if (storedVal == NAN_INT) {
            	endOfStream = true;
            }

        } else {
        	nextValue();
        }
    }

	// private void nextValueSerfQt() throws IOException {
	// 	//		int zzq = in.readGamma();
	// 			int zzq = in.readZeta(k);
	// 			int q = decodeZigZag(zzq);
	// 			float recoverValue = (float) (Float.intBitsToFloat(storedVal) + 2 * epsilon * q);
	// 	//		System.out.println(Float.intBitsToFloat(storedVal) + " " + recoverValue + " " + zzq + " " + q);
	// 			storedVal = Float.floatToRawIntBits(recoverValue);
	// 			return;
	// }

	private void nextValueQt() throws IOException {
		//		int zzq = in.readGamma();
		int zzq = in.readZeta(k);
		if (zzq == 0) {
			runSize = in.readZeta(GibbonCompressor.ZETA_K);
			runSize--;
			return;
		}
		int q = decodeZigZag(zzq);
		float recoverValue = (float) (Float.intBitsToFloat(storedVal) + 2 * epsilon * q);
		//		System.out.println(Float.intBitsToFloat(storedVal) + " " + recoverValue + " " + zzq + " " + q);
		storedVal = Float.floatToRawIntBits(recoverValue);
		return;
	}

	private void nextValueExistingLead() throws IOException {
		// System.out.println("DL1");
		int value = in.readInt(32 - storedLeadingZeros - storedTrailingZeros);
		value <<= storedTrailingZeros;
		value = storedVal ^ value;
		if (value == NAN_INT) {
			endOfStream = true;
		} else {
			storedVal = value;
		}
	}

	private void nextValueNewLead() throws IOException {
		// New leading and trailing zeros
		storedLeadingZeros = in.readInt(4);
		int significantBits = in.readInt(5) ;
		significantBits = significantBits == 0 ? 32 : significantBits;
		storedTrailingZeros = 32 - significantBits - storedLeadingZeros;
		int value = in.readInt(32 - storedLeadingZeros - storedTrailingZeros);
		// System.out.println("DL2 " + storedLeadingZeros + " " + significantBits + " " + value);
		value <<= storedTrailingZeros;
		value = storedVal ^ value;
		if (value == NAN_INT) {
			endOfStream = true;
		} else {
			storedVal = value;
		}
	}

    private void nextValue() throws IOException {
		if (runSize > 0) {
    		runSize--;
    		return;
    	}
		if (in.readBit()==0) { // case 0
			nextValue0(f);
		} else { // case 10
			if (in.readBit()==0) {
				nextValue10(f);
			} else { // case 110
				if (in.readBit()==0) {
					nextValue110(f);
				} else { // case 111
					nextValue111(f);
				}
			}
		}

    }

	private void nextValue0(int f) throws IOException {
		switch (f) {
			case 0:
				break;
			case 1:
				nextValueQt();
				break;
			case 2:
				nextValueExistingLead();
				break;
			case 3:
				nextValueNewLead();
			default:
				break;
		}
	}

	private void nextValue10(int f) throws IOException {
		switch (f) {
			case 0:
				nextValueQt();
				break;
			default:
				break;
		}
	}

	private void nextValue110(int f) throws IOException {
		switch (f) {
			case 2:
				nextValueQt();
				break;
			default:
				nextValueExistingLead();
				break;
		}
	}

	private void nextValue111(int f) throws IOException {
		switch (f) {
			case 3:
				nextValueQt();
				break;
			default:
				nextValueNewLead();
				break;
		}
	}

	// Decode a ZigZag-encoded unsigned int back to a signed int
    public static int decodeZigZag(int encoded) {
        return (encoded >>> 1) ^ -(encoded & 1);
    }

}
