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
    private int mode = 0;
    private boolean first = true;
    private boolean endOfStream = false;

    private BitInput in;

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
            storedVal = (int) in.getLong(32);
            if (in.readBit()) {
            	this.mode = 1;
            }
            if (storedVal == NAN_INT) {
            	endOfStream = true;
            	return;
            }

        } else {
        	nextValue();
        }
    }

    private void nextValue() {
        // Read value
    	if (in.readBit()) {
    		if (in.readBit()) {
    			decodeValueWithFlag(2);
    		} else {
    			decodeValueWithFlag(1);
    		}
    	} else {
    		decodeValueWithFlag(0);        
    	}
    }

	private void decodeValueWithFlag(int flag) {
		if (flag == 0) {
			if (mode == 0) {
			} else {
				int value = (int) in.getLong(32 - storedLeadingZeros - storedTrailingZeros);
	            value <<= storedTrailingZeros;
	            value = storedVal ^ value;
	            if (value == NAN_INT) {
	            	endOfStream = true;
	            	return;
	            } else {
	            	storedVal = value;
	            }	
			}
		} else if (flag == 1) {
			if (mode == 0) {
				int value = (int) in.getLong(32 - storedLeadingZeros - storedTrailingZeros);
	            value <<= storedTrailingZeros;
	            value = storedVal ^ value;
	            if (value == NAN_INT) {
	            	endOfStream = true;
	            	return;
	            } else {
	            	storedVal = value;
	            }
			} else { }
		} else if (flag == 2) {
			// New leading and trailing zeros
            storedLeadingZeros = (int) in.getLong(4);

            byte significantBits = (byte) in.getLong(5);
            if(significantBits == 0) {
                significantBits = 32;
            }
            storedTrailingZeros = 32 - significantBits - storedLeadingZeros;
    		int value = (int) in.getLong(32 - storedLeadingZeros - storedTrailingZeros);
            value <<= storedTrailingZeros;
            value = storedVal ^ value;
            if (value == NAN_INT) {
            	endOfStream = true;
            	return;
            } else {
            	storedVal = value;
            }
		}
		
	}

}