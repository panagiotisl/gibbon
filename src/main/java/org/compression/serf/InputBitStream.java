package gr.aueb.delorean.serf;

public class InputBitStream {
    private final byte[] buffer;
    private int bytePos = 0;
    private int bitPos = 0;

    public InputBitStream(byte[] buffer) {
        this.buffer = buffer;
    }

    public boolean readBit() {
        boolean bit = (buffer[bytePos] & (1 << (7 - bitPos))) != 0;
        bitPos++;
        if (bitPos == 8) {
            bitPos = 0;
            bytePos++;
        }
        return bit;
    }

    public long readInt(int bits) {
        long value = 0;
        for (int i = 0; i < bits; i++) {
            value = (value << 1) | (readBit() ? 1 : 0);
        }
        return value;
    }
}
