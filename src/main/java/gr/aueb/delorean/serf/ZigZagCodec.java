package gr.aueb.delorean.serf;


public class ZigZagCodec {

    public static long encode(long value) {
        return (value << 1) ^ (value >> 63);
    }

    public static long decode(long value) {
        return (value >> 1) ^ -(value & 1);
    }
}

