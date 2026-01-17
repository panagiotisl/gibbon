package gr.aueb.delorean.lossless;

import gr.aueb.delorean.Decompressor;

import java.io.IOException;
import java.nio.ByteBuffer;

public class GorillaDecompressor implements Decompressor {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private long storedVal = 0;
    private boolean first = true;
    private boolean endOfStream = false;

    private final ByteBufferBitInput in;

    private final static long END_SIGN = Double.doubleToLongBits(Double.NaN);

    public GorillaDecompressor(byte[] bs) {
        in = new ByteBufferBitInput(ByteBuffer.wrap(bs));
    }

    public ByteBufferBitInput getInputStream() {
        return in;
    }


    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    @Override
    public Float readValue() {
        try {
            next();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        if(endOfStream) {
            return null;
        }
        return (float) new Value(storedVal).getDoubleValue();
    }

    private void next() throws IOException {
        if (first) {
            first = false;
            storedVal = in.getLong(64);
            if (storedVal == END_SIGN) {
                endOfStream = true;
            }

        } else {
            nextValue();
        }
    }

    private void nextValue() throws IOException {
        // Read value
        if (in.readBit()) {
            // else -> same value as before
            if (in.readBit()) {
                // New leading and trailing zeros
                storedLeadingZeros = in.getInt(5);

                int significantBits = in.getInt(6);
                if(significantBits == 0) {
                    significantBits = 64;
                }
                storedTrailingZeros = 64 - significantBits - storedLeadingZeros;
            }
            long value = in.getLong(64 - storedLeadingZeros - storedTrailingZeros);
            value <<= storedTrailingZeros;
            value = storedVal ^ value;
            if (value == END_SIGN) {
                endOfStream = true;
            } else {
                storedVal = value;
            }

        }
    }
}
