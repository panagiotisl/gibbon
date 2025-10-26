package gr.aueb.delorean.gibbon;

import java.io.IOException;
import java.io.OutputStream;

public class ZetaOutputBitStream {

	public static final int DEFAULT_BUFFER_SIZE = 16 * 1024;
	protected final OutputStream os;
	private int current;
	protected byte[] buffer;
	protected int free;
	protected int pos;
	protected long position;
	protected int avail;

	public ZetaOutputBitStream(final OutputStream os) {
		this(os, DEFAULT_BUFFER_SIZE);
	}

	public ZetaOutputBitStream(final OutputStream os, final int bufSize) {
		this.os = os;
		if (bufSize != 0) {
			this.buffer = new byte[bufSize];
			avail = bufSize;
		}
		free = 8;
	}

	public void flush() {
		try {
			align();
			if (os != null) {
				if (buffer != null) {
					os.write(buffer, 0, pos);
					position += pos;
					pos = 0;
					avail = buffer.length;
				}
				os.flush();
			}
		} catch (Exception e) {
		}

	}

	private void write(final int b) throws IOException {
		if (avail-- == 0) {
			if (buffer == null) {
				os.write(b);
				position++;
				avail = 0;
				return;
			}
			os.write(buffer);
			position += buffer.length;
			avail = buffer.length - 1;
			pos = 0;
		}

		buffer[pos++] = (byte)b;
	}

	private int writeInCurrent(final int b, final int len) throws IOException {

		current |= (b & ((1 << len) - 1)) << (free -= len);
		if (free == 0) {
			write(current);
			free = 8;
			current = 0;
		}

		return len;
	}

	public int align() throws IOException {
		if (free != 8) return writeInCurrent(0, free);
		else return 0;
	}


	public int writeBit(final boolean bit) {
		try {
			return writeInCurrent(bit ? 1 : 0, 1);
		} catch (IOException e) {
			return -1;
		}
	}

	public int writeInt(int x, final int len) throws IOException {
		if (len <= free) return writeInCurrent(x, len);

		int i = len - free;
		final int queue = i & 7;

		if (free != 0) writeInCurrent(x >>> i, free);

		if (queue != 0) {
			i -= queue;
			writeInCurrent(x, queue);
			x >>>= queue;
		}

		if (i == 32) write(x >>> 24);
		if (i > 23) write(x >>> 16);
		if (i > 15) write(x >>> 8);
		if (i > 7) write(x);

		return len;
	}

	public int writeUnary(int x) throws IOException {
		if (x < free) return writeInCurrent(1, x + 1);

		final int shift = free;
		x -= shift;

		write(current);
		free = 8;
		current = 0;

		int i = x >> 3;

		while(i-- != 0) write(0);

		writeInCurrent(1, (x & 7) + 1);

		return x + shift + 1;
	}

	public int writeZeta(int x, final int k) throws IOException {
		if (x < ZETA_MAX_PRECOMPUTED) return writeInt(ZETA[k][x], ZETA[k][x] >>> 26);

		final int msb = 31 - Integer.numberOfLeadingZeros(++x);
		final int h = msb / k;
		final int l = writeUnary(h);
		final int left = 1 << h * k;
		return l + (x - left < left
				? writeInt(x - left, h * k + k - 1)
						: writeInt(x, h * k + k));
	}

	public static final int ZETA_MAX_PRECOMPUTED = 131072;

	private static final int[][] ZETA = new int[6][ZETA_MAX_PRECOMPUTED];

	static {
		for (int k = 2; k <= 5; k++) {
			for (int x = 0; x < ZETA_MAX_PRECOMPUTED; x++) {
				ZETA[k][x] = getZeta(k, x);
			}
		}
	}

	static final int ZETA_THRESHOLD = 1048576;
	private static final int[][] ZETA_K_LENGTH = new int[6][ZETA_THRESHOLD + 1];

	static {
		for (int k = 2; k <= 5; k++) {
			for (int n = 0; n <= ZETA_THRESHOLD; n++) {
				int l = (int) Math.ceil((Math.log(n + 1) / Math.log(2)) / k);
				ZETA_K_LENGTH[k][n] = (l - 1) * (k + 1) + 1;
			}
		}
	}

	public static int zetaKLength(int n, int k) {
		// if (n <= ZETA_THRESHOLD) {
		return ZETA_K_LENGTH[k][n];
		// }

		// Fallback for larger n or k
		// int l = (int) Math.ceil((Math.log(n + 1) / Math.log(2)) / k);
		// return (l - 1) * (k + 1) + 1;
	}

	private static int getZeta(int k, int n) {
		int val = n + 1;
		int msb = 31 - Integer.numberOfLeadingZeros(val);
		int h = msb / k;

		int unaryBits = 1;
		int unaryLen = h + 1;

		int left = 1 << (h * k);

		int payload, payloadLen;
		if (val - left < left) {
			payload = val - left;
			payloadLen = h * k + k - 1;
		} else {
			payload = val;
			payloadLen = h * k + k;
		}

		int encodedBits = (unaryBits << payloadLen) | payload;
		int totalLen = unaryLen + payloadLen;

		return (totalLen << 26) | (encodedBits & 0x03FFFFFF);
	}

}
