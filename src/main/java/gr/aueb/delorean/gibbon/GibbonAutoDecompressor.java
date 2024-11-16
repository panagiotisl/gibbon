package gr.aueb.delorean.gibbon;

import fi.iki.yak.ts.compression.gorilla.BitInput;

/**
 * Decompresses a compressed stream created by the Compressor. Returns pairs of timestamp and floating point value.
 *
 * @author Michael Burman
 */
public class GibbonAutoDecompressor {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private int storedVal = 0;
    private boolean mode = false;
    private boolean first = true;
    private boolean endOfStream = false;

    private final BitInput in;

    private final static int NAN_INT = 0x7fc00000;


    public GibbonAutoDecompressor(BitInput input) {
        in = input;
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public Value readValue() {
        next();
        if(endOfStream) {
            return null;
        }
        return new Value(storedVal);
    }

    private void next() {
        if (first) {
        	first = false;
			if (in.readBit()) {
				this.mode = true;
			}
            storedVal = in.getInt(32);
            if (storedVal == NAN_INT) {
            	endOfStream = true;
            }

        } else {
        	nextValue();
        }
    }

    private void nextValue() {
        // Read value
    	if (in.readBit()) {
    		if (in.readBit()) {
				// New leading and trailing zeros
				storedLeadingZeros = in.getInt(4);

				int significantBits = in.getInt(5) ;
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
				}
    		} else {
				if (!mode) {
					int value = in.getInt(32 - storedLeadingZeros - storedTrailingZeros);
					value <<= storedTrailingZeros;
					value = storedVal ^ value;
					if (value == NAN_INT) {
						endOfStream = true;
					} else {
						storedVal = value;
					}
				}
    		}
    	} else {
			if (mode) {
				int value = in.getInt(32 - storedLeadingZeros - storedTrailingZeros);
				value <<= storedTrailingZeros;
				value = storedVal ^ value;
				if (value == NAN_INT) {
					endOfStream = true;
				} else {
					storedVal = value;
				}
			}
    	}
    }

}