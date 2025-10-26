package gr.aueb.delorean.serf.xor;

import java.io.IOException;

import gr.aueb.delorean.Compressor;
import gr.aueb.delorean.serf.OutputBitStream;

public class SerfXORCompressor implements Compressor {

    private final int kWindowSize;
    private final double kMaxDiff;
    private final long kAdjustDigit;
    // private long compressedSizeThisBlock;
    // private long compressedSizeLastBlock = 0;
    // private byte[] compressedBytesLastBlock = new byte[0];

    // private long compressedSizeThisWindow = 0;
    // private int numberOfValuesThisWindow = 0;
    // private double compressionRatioLastWindow = 0;

    private long storedVal = Double.doubleToLongBits(2.0);

    // arrays from header
    private final int[] leadingRepresentation = {
        0,0,0,0,0,0,0,0,
        1,1,1,1,2,2,2,2,
        3,3,4,4,5,5,6,6,
        7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7
    };
    private final int[] leadingRound = {
        0,0,0,0,0,0,0,0,
        8,8,8,8,12,12,12,12,
        16,16,18,18,20,20,22,22,
        24,24,24,24,24,24,24,24,
        24,24,24,24,24,24,24,24,
        24,24,24,24,24,24,24,24,
        24,24,24,24,24,24,24,24,
        24,24,24,24,24,24,24,24
    };
    private final int[] trailingRepresentation = {
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,1,1,
        1,1,1,1,2,2,2,2,
        3,3,3,3,4,4,4,4,
        5,5,6,6,6,6,7,7,
        7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7
    };
    private final int[] trailingRound = {
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,22,22,
        22,22,22,22,28,28,28,28,
        32,32,32,32,36,36,36,36,
        40,40,42,42,42,42,46,46,
        46,46,46,46,46,46,46,46,
        46,46,46,46,46,46,46,46
    };

    private int leadingBitsPerValue = 3;
    private int trailingBitsPerValue = 3;
    private final int[] leadDistribution = new int[64];
    private final int[] trailDistribution = new int[64];
    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = Integer.MAX_VALUE;

    private boolean first = false;

    private final OutputBitStream out;

    private final static int DEFAULT_WINDOW_SIZE = 1000;

    public SerfXORCompressor(double epsilon) {
        this.kWindowSize = DEFAULT_WINDOW_SIZE;
        this.kMaxDiff = epsilon;
        this.kAdjustDigit = 0;

        this.out = new OutputBitStream(1024*8);

        int initial = (int) Math.floor(((kWindowSize + 1) * 8 + kWindowSize / 8 + 1) * 1.2);
    }

    @Override
    public void addValue(float value) throws IOException {
        addValueInternal(value);
    }

    public void addValueInternal(double value) throws IOException {
        if(first) {
            writeFirst(Double.doubleToRawLongBits(value));
        } else {
            long thisVal;
            double lastDouble = Double.longBitsToDouble(storedVal);
            if (Math.abs(lastDouble - kAdjustDigit - value) > kMaxDiff) {
                double adjustValue = value + kAdjustDigit;
                thisVal = SerfUtils64.findAppLong(adjustValue - kMaxDiff, adjustValue + kMaxDiff, value, storedVal, kMaxDiff, kAdjustDigit);
            } else {
                thisVal = storedVal;
            }

            compressValue(thisVal);
            storedVal = thisVal;
            // numberOfValuesThisWindow++;
        }
    }

    private void writeFirst(long value) {
        first = false;
        storedVal = value;
        out.writeInt(storedVal, 64);
    }

    public byte[] close() throws IOException {
        return out.toByteArray();
    }

    private void compressValue(long value) throws IOException {

        long xorResult = storedVal ^ value;

        if (xorResult == 0L) {
            out.writeInt(1, 2);
        } else {
            int leadingCount = Long.numberOfLeadingZeros(xorResult);
            int trailingCount = Long.numberOfTrailingZeros(xorResult);
            int leadingZeros = leadingRound[Math.min(leadingCount, leadingRound.length - 1)];
            int trailingZeros = trailingRound[Math.min(trailingCount, trailingRound.length - 1)];
            if (leadingCount >= 0 && leadingCount < leadDistribution.length) leadDistribution[leadingCount]++;
            if (trailingCount >= 0 && trailingCount < trailDistribution.length) trailDistribution[trailingCount]++;

            if (leadingZeros >= storedLeadingZeros && trailingZeros >= storedTrailingZeros &&
                    (leadingZeros - storedLeadingZeros) + (trailingZeros - storedTrailingZeros) <
                            1 + leadingBitsPerValue + trailingBitsPerValue) {
                int centerBits = 64 - storedLeadingZeros - storedTrailingZeros;
                int len = 1 + centerBits;
                if (len > 64) {
                    out.writeInt(1, 1);
                    out.writeInt(xorResult >>> storedTrailingZeros, centerBits);
                } else {
                    long toWrite = (1L << centerBits) | (xorResult >>> storedTrailingZeros);
                    out.writeInt(toWrite, 1 + centerBits);
                }
            } else {
                storedLeadingZeros = leadingZeros;
                storedTrailingZeros = trailingZeros;
                int centerBits = 64 - storedLeadingZeros - storedTrailingZeros;

                int len = 2 + leadingBitsPerValue + trailingBitsPerValue + centerBits;
                if (len > 64) {
                    int leadRep = storedLeadingZeros < leadingRepresentation.length ? leadingRepresentation[storedLeadingZeros] : 0;
                    int trailRep = storedTrailingZeros < trailingRepresentation.length ? trailingRepresentation[storedTrailingZeros] : 0;
                    int header = (leadRep << trailingBitsPerValue) | trailRep;
                    out.writeInt(header, 2 + leadingBitsPerValue + trailingBitsPerValue);
                    out.writeInt(xorResult >>> storedTrailingZeros, centerBits);
                } else {
                    int leadRep = storedLeadingZeros < leadingRepresentation.length ? leadingRepresentation[storedLeadingZeros] : 0;
                    int trailRep = storedTrailingZeros < trailingRepresentation.length ? trailingRepresentation[storedTrailingZeros] : 0;
                    long header = (((long) leadRep << trailingBitsPerValue) | (long) trailRep) << centerBits;
                    long toWrite = header | (xorResult >>> storedTrailingZeros);
                    out.writeInt(toWrite, len);
                }
            }
        }

    }

}
