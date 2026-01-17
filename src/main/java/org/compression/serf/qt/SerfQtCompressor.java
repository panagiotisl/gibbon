package gr.aueb.delorean.serf.qt;

import java.io.IOException;

import gr.aueb.delorean.Compressor;
import gr.aueb.delorean.serf.OutputBitStream;
import gr.aueb.delorean.serf.ZigZagCodec;
import gr.aueb.delorean.serf.EliasGammaCodec;

public class SerfQtCompressor implements Compressor {

    private long storedVal = 0;
    private boolean first = true;

    private final OutputBitStream out;
    private final double epsilon;

    public SerfQtCompressor(double epsilon) {
        this.out = new OutputBitStream(1024*8);
        this.epsilon = epsilon * 0.999;
    }

    @Override
    public void addValue(float value) throws IOException {
        addValueInternal(value);
    }

    public void addValueInternal(double value) throws IOException {
        if(first) {
            writeFirst(Double.doubleToRawLongBits(value));
        } else {
            compressValue(Double.doubleToRawLongBits(value));
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

        long q =  (long) Math.round((Double.longBitsToDouble(value) - Double.longBitsToDouble(storedVal)) / (2 * epsilon));
        long zzq = ZigZagCodec.encode(q);
        double recoverValue = (Double.longBitsToDouble(storedVal) + 2 * epsilon * q);
        EliasGammaCodec.encode(zzq + 1, out);
        storedVal = Double.doubleToRawLongBits(recoverValue);

    }

}
