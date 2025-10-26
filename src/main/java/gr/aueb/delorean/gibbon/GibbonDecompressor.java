package gr.aueb.delorean.gibbon;

import gr.aueb.delorean.Decompressor;

import java.io.IOException;

public class GibbonDecompressor implements Decompressor {

    private int storedTrailingZeros = 0;
	private int totalStoredZeros = 0;
    private int storedVal = 0;
	private float floatStoredVal = 0;
    private int k;
	private int f;
    private boolean first = true;
    private boolean endOfStream = false;
    private final double twoEpsilon;
	private int runSize = 0;

    private final ZetaInputBitStream in;

    private final static int NAN_INT = 0x7fc00000;

	public static final int MAX_ENCODED = 16384; // inclusive upper bound
    public static final int[] DECODE = new int[MAX_ENCODED];

    static {
        for (int i = 0; i < MAX_ENCODED; i++) {
            DECODE[i] = (i >>> 1) ^ -(i & 1);
        }
    }


    public GibbonDecompressor(byte[] byteBuffer, double epsilon) {
        this.in = new ZetaInputBitStream(byteBuffer);
        this.twoEpsilon = 2 * epsilon;
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
	@Override
    public Float readValue() throws IOException {
        if (first) {
        	first = false;
			this.k = in.readInt(2) + 2;
			this.f = in.readInt(2);
            storedVal = in.readInt(32);
			floatStoredVal = Float.intBitsToFloat(storedVal);
            if (storedVal == NAN_INT) {
            	endOfStream = true;
            }

        } else {
        	if (runSize > 0) {
    			runSize--;
				return floatStoredVal;
			}
			if (in.readBit()==0) { // case 0
				switch (f) {
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
			} else { // case 10
				if (in.readBit()==0) {
					if (f == 0 ) nextValueQt();
				} else { // case 110
					if (in.readBit()==0) {
						if (f == 2) nextValueQt();
						else nextValueExistingLead();
					} else { // case 111
						if (f == 3) nextValueQt();
						else nextValueNewLead();
					}
				}
			}

        }
        if(endOfStream) {
            return null;
        }
		return floatStoredVal;
    }

	private void nextValueQt() throws IOException {
		int zzq = in.readZeta(k);
		if (zzq == 0) {
			runSize = in.readZeta(GibbonCompressor.ZETA_K);
			runSize--;
			return;
		}
		int q = decodeZigZag(zzq);
		float recoverValue = (float) (Float.intBitsToFloat(storedVal) + twoEpsilon * q);
		floatStoredVal = recoverValue;
		storedVal = Float.floatToRawIntBits(recoverValue);
		return;
	}

	private void nextValueExistingLead() throws IOException {
		int value = in.readInt(32 - totalStoredZeros);
		value <<= storedTrailingZeros;
		value = storedVal ^ value;
		if (value == NAN_INT) {
			endOfStream = true;
		} else {
			storedVal = value;
			floatStoredVal = Float.intBitsToFloat(storedVal);
		}
	}

	private void nextValueNewLead() throws IOException {
		// New leading and trailing zeros
        int storedLeadingZeros = in.readInt(4);
		int significantBits = in.readInt(5);

		if (significantBits == 0) significantBits = 32;
		storedTrailingZeros = 32 - significantBits - storedLeadingZeros;
		totalStoredZeros = storedLeadingZeros + storedTrailingZeros;
		int value = in.readInt(32 - totalStoredZeros);
		value <<= storedTrailingZeros;
		value = storedVal ^ value;
		if (value == NAN_INT) {
			endOfStream = true;
		} else {
			storedVal = value;
			floatStoredVal = Float.intBitsToFloat(storedVal);
		}
	}

	// Decode a ZigZag-encoded unsigned int back to a signed int
    public static int decodeZigZag(int encoded) {
		if (encoded < MAX_ENCODED) return DECODE[encoded];
        return (encoded >>> 1) ^ -(encoded & 1);
    }

}
