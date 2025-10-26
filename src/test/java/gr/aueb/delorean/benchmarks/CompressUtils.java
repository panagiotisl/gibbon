package gr.aueb.delorean.benchmarks;

import gr.aueb.delorean.Compressor;
import gr.aueb.delorean.Decompressor;
import gr.aueb.delorean.gibbon.GibbonCompressor;
import gr.aueb.delorean.gibbon.GibbonDecompressor;
import gr.aueb.delorean.serf.qt.SerfQtDecompressor;
import gr.aueb.delorean.util.Point;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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

    public static void init() {
        compressionTime = 0L;
        decompressionTime = 0L;
        error = 0D;
        squareError = 0D;
    }

    public static long[] compress(Compressor compressor, Collection<Point> ts, double epsilon, Class<? extends Decompressor> decompressorClass) throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Iterator<Point> iterator = ts.iterator();
        long start = System.nanoTime();
        while (iterator.hasNext()) {
            compressor.addValue((float) iterator.next().getValue());
        }
        byte[] compressed = compressor.close();
        long end = System.nanoTime();
        compressionTime += end - start;
        long compressedSize = compressed.length;
        Constructor<? extends Decompressor> ctor;
        Decompressor d;
        if (decompressorClass == GibbonDecompressor.class || decompressorClass == SerfQtDecompressor.class) {
            ctor = decompressorClass.getConstructor(byte[].class, double.class);
            d = ctor.newInstance(compressed, epsilon);
        } else {
            ctor = decompressorClass.getConstructor(byte[].class);
            d = ctor.newInstance(compressed);
        }
        float[] decompressedValues = new float[ts.size()];
        start = System.nanoTime();
        for (int i=0; i<ts.size(); i++) {
            decompressedValues[i] = d.readValue();
        }
        end = System.nanoTime();
        decompressionTime += end - start;
        int i = 0;
        for (Point point : ts) {
            float decompressedValue = decompressedValues[i++];
            error += Math.abs((float) point.getValue() - decompressedValue);
            squareError += error * error;
            double factor = 1.01 * epsilon;
            if (decompressorClass == SerfQtDecompressor.class) { factor = epsilon * 2D; } // needed for SerfQt
            assertEquals(
                    (float) point.getValue(),
                    decompressedValue,
                    factor,
                    "Value did not match for timestamp " + point.getTimestamp()
            );
        }
        if (compressor instanceof GibbonCompressor) {
            return new long[] {compressedSize, ((GibbonCompressor) compressor).getK(), ((GibbonCompressor) compressor).getBestMode()};
        } else {
            return new long[] {compressedSize};
        }
    }

}
