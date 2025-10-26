package gr.aueb.delorean.serf.qt;

import java.io.IOException;

import gr.aueb.delorean.Decompressor;
import gr.aueb.delorean.serf.InputBitStream;
import gr.aueb.delorean.serf.Value;
import gr.aueb.delorean.serf.ZigZagCodec;
import gr.aueb.delorean.serf.EliasGammaCodec;

public class SerfQtDecompressor implements Decompressor {

	private long storedVal = 0;
	private boolean first = true;
	private final double epsilon;

	private final InputBitStream in;

	public SerfQtDecompressor(byte[] encoded, double epsilon) {
		in = new InputBitStream(encoded);
		this.epsilon = epsilon * 0.999;
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
		long zzq = EliasGammaCodec.decode(in) - 1;
		long q = ZigZagCodec.decode(zzq);
		double recoverValue = (Double.longBitsToDouble(storedVal) + 2 * epsilon * q);
		storedVal = Double.doubleToRawLongBits(recoverValue);
	}

}
