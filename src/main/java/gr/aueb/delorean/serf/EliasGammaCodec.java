package gr.aueb.delorean.serf;

import java.util.Random;

public class EliasGammaCodec {

    private static final double[] LOG2_TABLE = {
        Double.NaN, 0.0, 1.0, 1.584962500721156, 2.0, 2.321928094887362,
        2.584962500721156, 2.807354922057604, 3.0, 3.169925001442312,
        3.321928094887362, 3.459431618637297, 3.584962500721156,
        3.700439718141092, 3.807354922057604, 3.906890595608519,
        4.0
    };

    public static void encode(long number, OutputBitStream out) {
        int n;
        if (number <= 16) {
            n = (int) Math.floor(LOG2_TABLE[(int) number]);
        } else {
            n = (int) Math.floor(Math.log(number) / Math.log(2));
        }
        // write n zeros
        for (int i = 0; i < n; i++) out.writeBit(false);
        // write a 1
        out.writeBit(true);
        // write lower n bits of number
        out.writeInt(number, n);
    }

    public static long decode(InputBitStream in) {
        long n = 0;
        while (!in.readBit()) n++;
        long lower = (n == 0) ? 0 : in.readInt((int) n);
        return (1 << n) | lower;
    }

    public static void main(String[] args) {
        Random rand = new Random();
        int[] numbers = new int[50];
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = Math.abs(rand.nextInt()) + 1; // random int from 1 to 1000
        }
        // Encode
        OutputBitStream out = new OutputBitStream(50*8);
        for (int num : numbers) encode(num, out);
        byte[] encoded = out.toByteArray();

        // Decode
        InputBitStream in = new InputBitStream(encoded);
        for (int expected : numbers) {
            int decoded = (int) decode(in);
            System.out.println("Original: " + expected + " -> Decoded: " + decoded +
                               (expected == decoded ? "Yes" : "No"));
        }
    }
}
