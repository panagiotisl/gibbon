package gr.aueb.delorean.benchmarks;

import gr.aueb.delorean.gibbon.InputBitStream;
import gr.aueb.delorean.gibbon.OutputBitStream;
import gr.aueb.delorean.gibbon.GibbonCompressor;
import gr.aueb.delorean.gibbon.GibbonDecompressor;
import gr.aueb.delorean.util.Point;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CompressUtils {

    private static long compressionTime = 0L;
    private static long decompressionTime = 0L;
    private static double error = 0D;
    private static double squareError = 0D;

    public static long getCompressionTime() {
        return compressionTime;
    }

    public static long getDecompressionTime() {
        return decompressionTime;
    }

    public static double getError() {
        return error;
    }

    public static double getSquareError() {
        return squareError;
    }

    public static long[] Gibbon(Collection<Point> ts, double epsilon, int k, int f) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        OutputBitStream output = new OutputBitStream(byteOut);
        GibbonCompressor compressor = new GibbonCompressor(output, epsilon, k, f);
        Iterator<Point> iterator = ts.iterator();
        long start = System.nanoTime();
        while (iterator.hasNext()) {
            compressor.addValue((float) iterator.next().getValue());
        }
        compressor.close();
        long end = System.nanoTime();
        compressionTime += end - start;
        byte[] byteBuffer = byteOut.toByteArray();
        long compressedSize = byteBuffer.length;
        GibbonDecompressor d = new GibbonDecompressor(new InputBitStream(byteBuffer), epsilon);
        float[] decompressedValues = new float[ts.size()];
        start = System.nanoTime();
         for (int i=0; i<ts.size(); i++) {
             decompressedValues[i] = d.readValue().getFloatValue();
         }
        end = System.nanoTime();
        decompressionTime += end - start;
         int i = 0;
        for (Point point : ts) {
             float decompressedValue = decompressedValues[i++];
        //    float decompressedValue = d.readValue().getFloatValue();
            // System.out.println(decompressedValue + " " + (float) point.getValue() + " " + point.getTimestamp());
            error += Math.abs((float) point.getValue() - decompressedValue);
            squareError += error * error;
            assertEquals(
                    (float) point.getValue(),
                    decompressedValue,
                    epsilon*1.0001,
                    "Value did not match for timestamp " + point.getTimestamp()
            );
        }
        return new long[] {compressedSize, compressor.getK(), compressor.getBestMode(), compressor.trailingZerosSum, compressor.trailingZerosCnt, compressor.cases[0], compressor.cases[1], compressor.cases[2], compressor.cases[3]};
    }

    public static void init() {
        compressionTime = 0L;
        decompressionTime = 0L;
        error = 0D;
        squareError = 0D;
    }
}
