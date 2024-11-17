package gr.aueb.delorean.benchmarks;

import fi.iki.yak.ts.compression.gorilla.ByteBufferBitInput;
import fi.iki.yak.ts.compression.gorilla.ByteBufferBitOutput;
import gr.aueb.delorean.gibbon.GibbonAutoCompressor;
import gr.aueb.delorean.gibbon.GibbonAutoDecompressor;
import gr.aueb.delorean.util.Point;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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



    public static long[] GibbonAuto(Collection<Double> series, double epsilon, int mode) {
        ByteBufferBitOutput output = new ByteBufferBitOutput();
        GibbonAutoCompressor compressor = new GibbonAutoCompressor(output, epsilon, mode);
        long start = System.nanoTime();
        for (double value : series) {
            compressor.addValue((float) value);
        }
        compressor.close();
        long end = System.nanoTime();
        compressionTime = end - start;
        //long compressedSize = compressor.getSize() / 8;

        ByteBuffer byteBuffer = output.getByteBuffer();
        long compressedSize = byteBuffer.position();
        byteBuffer.flip();
        ByteBufferBitInput input = new ByteBufferBitInput(byteBuffer);
        GibbonAutoDecompressor d = new GibbonAutoDecompressor(input);
        start = System.nanoTime();
        long timestamp = 0;
        for (double value : series) {
            timestamp++;
            float decompressedValue = d.readValue().getFloatValue();
//            float roundedDecompressedValue = (float) (Math.round(round * decompressedValue) / round);
//            float originalValue = (float) (Math.round(round * point.getValue()) / round);
//            System.out.println(((float) point.getValue()) + " " + originalValue + " " + roundedDecompressedValue + " " + decompressedValue + " " + epsilon);
//            assertEquals(originalValue, roundedDecompressedValue, 0.00001D);
            error += Math.abs((float) value - decompressedValue);
            squareError += error * error;
            assertEquals(
                    (float) value,
                    decompressedValue,
                    epsilon,
                    "Value did not match for timestamp " + timestamp
            );
        }
        end = System.nanoTime();
        decompressionTime += end - start;
        return new long[] {compressedSize, compressor.getBestMode()};
    }


    public static long[] GibbonAuto(List<Point> ts, double epsilon, int mode) {
        ByteBufferBitOutput output = new ByteBufferBitOutput();
        GibbonAutoCompressor compressor = new GibbonAutoCompressor(output, epsilon, mode);
        Iterator<Point> iterator = ts.iterator();
        long start = System.nanoTime();
        while (iterator.hasNext()) {
            compressor.addValue((float) iterator.next().getValue());
        }
        compressor.close();
        long end = System.nanoTime();
        compressionTime = end - start;
        //long compressedSize = compressor.getSize() / 8;

        ByteBuffer byteBuffer = output.getByteBuffer();
        long compressedSize = byteBuffer.position();
        byteBuffer.flip();
        ByteBufferBitInput input = new ByteBufferBitInput(byteBuffer);
        GibbonAutoDecompressor d = new GibbonAutoDecompressor(input);
        start = System.nanoTime();
        for (Point point : ts) {
            float decompressedValue = d.readValue().getFloatValue();
//            float roundedDecompressedValue = (float) (Math.round(round * decompressedValue) / round);
//            float originalValue = (float) (Math.round(round * point.getValue()) / round);
//            System.out.println(((float) point.getValue()) + " " + originalValue + " " + roundedDecompressedValue + " " + decompressedValue + " " + epsilon);
//            assertEquals(originalValue, roundedDecompressedValue, 0.00001D);
            error += Math.abs((float) point.getValue() - decompressedValue);
            squareError += error * error;
            assertEquals(
                    (float) point.getValue(),
                    decompressedValue,
                    epsilon,
                    "Value did not match for timestamp " + point.getTimestamp()
            );
        }
        end = System.nanoTime();
        decompressionTime += end - start;
        return new long[] {compressedSize, compressor.getBestMode()};
    }

    public static void init() {
        compressionTime = 0L;
        decompressionTime = 0L;
        error = 0D;
        squareError = 0D;
    }
}
