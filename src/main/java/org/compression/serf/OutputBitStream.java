package gr.aueb.delorean.serf;

import java.util.Arrays;

public class OutputBitStream {
    private final byte[] buffer;
    private int bytePos = 0;
    private int bitPos = 0; // 0 = MSB

    public OutputBitStream(int size) {
        buffer = new byte[size];
    }

    // Write a single bit
    public void writeBit(boolean bit) {
        if (bit) {
            buffer[bytePos] |= (1 << (7 - bitPos));
        }
        bitPos++;
        if (bitPos == 8) {
            bitPos = 0;
            bytePos++;
        }
    }

    // Write multiple bits from the lowest bits of value
    public void writeInt(long value, int bits) {
        for (int i = bits - 1; i >= 0; i--) {
            writeBit(((value >> i) & 1) != 0);
        }
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(buffer, bytePos + (bitPos > 0 ? 1 : 0));
    }
}
