package gr.aueb.delorean.gibbon;

public class ZetaInputBitStream {

	private int current;
	protected byte[] buffer;
	protected int fill;
	protected int pos;
	protected int avail;
	protected long position;

	public ZetaInputBitStream(final byte[] a) {
		buffer = a;
		avail = a.length;
	}

	private int read()  {
		if (avail == 0) {
			position += pos;
			pos = 0;
		}

		avail--;
		return buffer[pos++] & 0xFF;
	}

	private void refill() {
		if (avail > 1) {
			// If there is a byte in the buffer, we use it directly.
			avail -= 2;
			current = current << 16 | (buffer[pos++] & 0xFF) << 8 | buffer[pos++] & 0xFF;
			fill += 16;
			return;
		}
		current = (current << 8) | read();
		fill += 8;
		current = (current << 8) | read();
		fill += 8;
	}

	private int readFromCurrent(final int len) {
		if (fill == 0) {
			current = read();
			fill = 8;
		}

		return current >>> (fill -= len) & (1 << len) - 1;
	}

	public int readBit() {
		return readFromCurrent(1);
	}

	public int readInt(int len) {
		if (fill < 16) refill();
		if (len <= fill) return readFromCurrent(len);
		len -= fill;
		int x = readFromCurrent(fill);

		int i = len >> 3;
		while(i-- != 0) x = x << 8 | read();

		len &= 7;

		return (x << len) | readFromCurrent(len);

	}

	public int readUnary() {
		if (fill < 16) refill();
		int x = Integer.numberOfLeadingZeros(current << (32 - fill));
		int x1 = x + 1;
		if (x < fill) { // This works also when fill = 0
			fill -= x1;
			return x;
		}

		x = fill;
		while((current = read()) == 0) x += 8;
		x += 7 - (fill = 31 - Integer.numberOfLeadingZeros(current));
		return x;
	}

	public int readZeta(final int k) {
		final int h = readUnary();
		final int hk = h * k;
		final int left = 1 << hk;
		final int m = readInt(hk + k - 1);
		if (m < left) return m + left - 1;
		return (m << 1) + readBit() - 1;
	}

}
