package gr.aueb.delorean.benchmarks;

import gr.aueb.delorean.gibbon.*;
import gr.aueb.delorean.util.TimeSeries;
import gr.aueb.delorean.pmcmr.PMCMRCompressor;
import gr.aueb.delorean.pmcmr.PMCMRDecompressor;
import gr.aueb.delorean.pmcmr.PMCMREncoder;
import gr.aueb.delorean.pmcmr.PMCMRSegment;
import gr.aueb.delorean.simpiece.SimPiece;
//import gr.aueb.delorean.simpiece.SimPieceCompressor;
//import gr.aueb.delorean.simpiece.SimPieceDecompressor;
//import gr.aueb.delorean.simpiece.SimPieceEncoder;
//import gr.aueb.delorean.simpiece.SimPieceSegment;
import gr.aueb.delorean.util.TimeSeriesReader;
import gr.aueb.delorean.swingfilter.SwingFilterCompressor;
import gr.aueb.delorean.swingfilter.SwingFilterDecompressor;
import gr.aueb.delorean.swingfilter.SwingFilterEncoder;
import gr.aueb.delorean.swingfilter.SwingFilterSegment;
import gr.aueb.delorean.util.Point;
import org.junit.jupiter.api.Test;

import fi.iki.yak.ts.compression.gorilla.ByteBufferBitInput;
import fi.iki.yak.ts.compression.gorilla.ByteBufferBitOutput;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCR {

    private long compressionTime = 0L;
    private long decompressionTime = 0L;
    private double error = 0D;
    private double squareError = 0D;
    final String[] filenames = {
          "/Lightning.csv.gz",
          "/Wafer.csv.gz",
          "/MoteStrain.csv.gz",
          "/Cricket.csv.gz",
          "/FaceFour.csv.gz",
          "/basel-temp.csv.gz",
          "/basel-wind-speed.csv.gz",
          "/Stocks-Germany.csv.gz",
          "/Stocks-UK-sample.csv.gz",
//          "/Stocks-USA-sample.csv.gz",
//          "/NEON_temp-bio-bioTempMean-sample.csv.gz",
//          "/NEON_pressure-air_staPresMean-sample.csv.gz",
//          "/NEON_wind-2d_windDirMean-sample.csv.gz",
//          "/NEON_temp-bio-bioTempMean-sample.csv.gz",

  };


    private long PMCMR(List<Point> ts, double epsilon) {
        List<PMCMRSegment> segments = PMCMRCompressor.filter(ts, epsilon);

        byte[] binary = PMCMREncoder.getBinary(segments);
        long compressedSize = binary.length;
        segments = PMCMREncoder.readBinary(binary);

        PMCMRDecompressor d = new PMCMRDecompressor(segments);
        for (Point point : ts) {
            Double decompressedValue = d.readValue();
            assertEquals(
                    point.getValue(),
                    decompressedValue,
                    1.1 * epsilon,
                    "Value did not match for timestamp " + point.getTimestamp()
            );
        }

        return compressedSize;
    }


    private long Swing(List<Point> ts, double epsilon) {
        List<SwingFilterSegment> segments = SwingFilterCompressor.filter(ts, epsilon);

        byte[] binary = SwingFilterEncoder.getBinary(segments);
        long compressedSize = binary.length;
        segments = SwingFilterEncoder.readBinary(binary);

        SwingFilterDecompressor d = new SwingFilterDecompressor(segments);
        for (Point point : ts) {
            double decompressedValue = d.readValue();
            assertEquals(
                    point.getValue(),
                    decompressedValue,
                    1.1 * epsilon,
                    "Value did not match for timestamp " + point.getTimestamp()
            );
        }

        return compressedSize;
    }

    private long Gibbon(List<Point> ts, double epsilon) {
    	ByteBufferBitOutput output = new ByteBufferBitOutput();
    	GibbonCompressor compressor = new GibbonCompressor(output, epsilon);
    	Iterator<Point> iterator = ts.iterator();
        long start = System.nanoTime();
    	while (iterator.hasNext()) {
    		compressor.addValue((float) iterator.next().getValue());
    	}
        compressor.close();
        long end = System.nanoTime();
        compressionTime += end - start;
        long compressedSize = compressor.getSize() / 8;

        ByteBuffer byteBuffer = output.getByteBuffer();
        byteBuffer.flip();
        ByteBufferBitInput input = new ByteBufferBitInput(byteBuffer);
        GibbonDecompressor d = new GibbonDecompressor(input);

        start = System.nanoTime();
        for (Point point : ts) {

            float decompressedValue = d.readValue().getFloatValue();
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
        return compressedSize;
    }


    private long[] GibbonAuto(List<Point> ts, double epsilon, int mode) {
    	ByteBufferBitOutput output = new ByteBufferBitOutput();
    	GibbonAutoCompressor compressor = new GibbonAutoCompressor(output, epsilon, mode);
    	Iterator<Point> iterator = ts.iterator();
        long start = System.nanoTime();
    	while (iterator.hasNext()) {
    		compressor.addValue((float) iterator.next().getValue());
    	}
        compressor.close();
        long end = System.nanoTime();
        compressionTime += end - start;
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


    private long GibbonRLE(List<Point> ts, double epsilon) {
    	ByteBufferBitOutput output = new ByteBufferBitOutput();
    	RunLengthEncodingLossyCompressor32 compressor = new RunLengthEncodingLossyCompressor32(output, epsilon);
    	Iterator<Point> iterator = ts.iterator();
    	while (iterator.hasNext()) {
    		compressor.addValue((float) iterator.next().getValue());
    	}
        compressor.close();
        long compressedSize = compressor.getSize() / 8;

        ByteBuffer byteBuffer = output.getByteBuffer();
        byteBuffer.flip();
        ByteBufferBitInput input = new ByteBufferBitInput(byteBuffer);
        RunLengthEncodingDecompressor32 d = new RunLengthEncodingDecompressor32(input);
        for (Point point : ts) {
            float decompressedValue = d.readValue().getFloatValue();
            assertEquals(
                    (float) point.getValue(),
                    decompressedValue,
                    1.1 * epsilon,
                    "Value did not match for timestamp " + point.getTimestamp()
            );
        }

        return compressedSize;
    }


    private long SimPiece(List<Point> ts, double epsilon) {
        SimPiece simPiece = new SimPiece(ts, epsilon);
        byte[] binary = simPiece.toByteArray();

        long compressedSize = binary.length;

        simPiece = new SimPiece(binary);
        List<Point> tsDecompressed = simPiece.decompress();
        for (int i = 0; i < ts.size(); i++) {
            assertEquals(
                    ts.get(i).getValue(),
                    tsDecompressed.get(i).getValue(),
                    1.2 * epsilon,
                    "Value did not match for timestamp " + ts.get(i).getTimestamp()
            );
        }

        return compressedSize;
    }

//    private long SimPiece(List<Point> ts, double epsilon) {
//        List<SimPieceSegment> segments = SimPieceCompressor.filter(ts, epsilon);
//        segments = SimPieceCompressor.mergeSegments(segments);
//
//        byte[] binary = SimPieceEncoder.getBinary(epsilon, segments);
//        long compressedSize = binary.length;
//        segments = SimPieceEncoder.readBinary(binary);
//
//        SimPieceDecompressor simPieceDecompressor = new SimPieceDecompressor(segments);
//        for (Point point : ts) {
//            double decompressedValue = simPieceDecompressor.readValue();
//            assertEquals(
//                    point.getValue(),
//                    decompressedValue,
//                    1.1 * epsilon,
//                    "Value did not match for timestamp " + point.getTimestamp()
//            );
//        }
//
//        return compressedSize;
//    }


//    @Test
    public void testCR() {
        double epsilonStart = 0.005;
        double epsilonStep = 0.005;
        double epsilonEnd = 0.05;

        String delimiter = ",";

        for (String filename : filenames) {
            System.out.println(filename);
            TimeSeries ts = TimeSeriesReader.getTimeSeries(getClass().getResourceAsStream(filename), delimiter);

//            System.out.println("PMCMR");
//            for (double epsilonPct = epsilonStart; epsilonPct <= epsilonEnd; epsilonPct += epsilonStep)
//                System.out.printf("Epsilon: %.2f%%\tCompression Ratio: %.3f\tepsilin: %.5f\n",
//                        epsilonPct * 100, (double) ts.size / PMCMR(ts.data, ts.range * epsilonPct), ts.range * epsilonPct);

//            System.out.println("Swing");
//            for (double epsilonPct = epsilonStart; epsilonPct <= epsilonEnd; epsilonPct += epsilonStep)
//                System.out.printf("Epsilon: %.2f%%\tCompression Ratio: %.3f\n",
//                        epsilonPct * 100, (double) ts.size / Swing(ts.data, ts.range * epsilonPct));

            System.out.println("Gibbon");
            for (double epsilonPct = epsilonStart; epsilonPct <= epsilonEnd; epsilonPct += epsilonStep) {
                compressionTime = 0L;
                decompressionTime = 0L;
                System.out.printf("Epsilon: %.2f%%\tCompression Ratio: %.3f\tCompression Time: %.3f\tDeCompression Time: %.3f\n",
                        epsilonPct * 100, (double) ts.size / Gibbon(ts.data, ts.range * epsilonPct), compressionTime / 1000000.0, decompressionTime / 1000000.0);
            }
            compressionTime = 0L;
            decompressionTime = 0L;
//            System.out.println("GibbonRLE");
//            for (double epsilonPct = epsilonStart; epsilonPct <= epsilonEnd; epsilonPct += epsilonStep)
//                System.out.printf("Epsilon: %.2f%%\tCompression Ratio: %.3f\n",
//                        epsilonPct * 100, (double) ts.size / GibbonRLE(ts.data, ts.range * epsilonPct));

//            System.out.println("Sim-Piece");
//            for (double epsilonPct = epsilonStart; epsilonPct <= epsilonEnd; epsilonPct += epsilonStep)
//                System.out.printf("Epsilon: %.2f%%\tCompression Ratio: %.3f\n",
//                        epsilonPct * 100, (double) ts.size / SimPiece(ts.data, ts.range * epsilonPct));

            System.out.println();
        }
    }

    @Test
    public void testCrDecimals() {
    	double[] epsilons = {0.05, 0.005, 0.0005, 0.00005 }; //, 0.000005};

        String delimiter = ",";

        for (String filename : filenames) {
            System.out.println(filename);
            TimeSeries ts = TimeSeriesReader.getTimeSeries(getClass().getResourceAsStream(filename), delimiter);

//            System.out.println("PMCMR");
//            for (double epsilon : epsilons)
//                System.out.printf("Epsilon: %.5f\tCompression Ratio: %.3f\n",
//                        epsilon, (double) ts.size / PMCMR(ts.data, epsilon));
//
//            System.out.println("Swing");
//            for (double epsilon : epsilons)
//                System.out.printf("Epsilon: %.5f\tCompression Ratio: %.3f\n",
//                		epsilon, (double) ts.size / Swing(ts.data, epsilon));
            System.out.println("Gibbon");
            for (double epsilon : epsilons) {
                compressionTime = 0L;
                decompressionTime = 0L;
                error = 0D;
                squareError = 0D;
                System.out.printf("Epsilon: %.5f\tCompression Ratio: %.3f\tCompression Time: %.3f\tDecompression Time: %.3f\tMAE: %5f\tRMSE: %5f\n",
                        epsilon, (double) ts.size / Gibbon(ts.data, epsilon), compressionTime / 1000000.0, decompressionTime / 1000000.0, error / ts.size, Math.sqrt(squareError / (ts.size * ts.size)));
            }
//
//            System.out.println("GibbonRLE");
//            for (double epsilon : epsilons)
//            	System.out.printf("Epsilon: %.5f\tCompression Ratio: %.3f\n",
//                		epsilon, (double) ts.size / GibbonRLE(ts.data, epsilon));

//            System.out.println("Sim-Piece");
//            for (double epsilon : epsilons)
//            	System.out.printf("%.3f\n",
//                		(double) ts.size / SimPiece(ts.data, epsilon));

            System.out.println();
        }
    }


	@Test
	public void testCrDecimalsInBlocks() throws IOException {
		double[] epsilons = {0.05, 0.005, 0.0005, 0.00005, 0.000005};

	    String delimiter = ",";
	    int blockSize = 1000;

	    for (String filename : filenames) {
	        long[] result = {0, 0};
	        for (double epsilon : epsilons) {
	        	long totalCompressedSize = 0;
	        	long totalSize = 0;
                compressionTime = 0L;
                decompressionTime = 0L;
                error = 0D;
                squareError = 0D;
	        	InputStream inputStream = getClass().getResourceAsStream(filename);
	        	InputStream gzipStream = new GZIPInputStream(inputStream);
	            Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
	            BufferedReader bufferedReader = new BufferedReader(decoder);
		        TimeSeries ts;
		        do {
		        	ts = TimeSeriesReader.getTimeSeriesBlock(bufferedReader, delimiter, blockSize);
		        	totalSize += ts.size;
		        	result = GibbonAuto(ts.data, epsilon, (int) result[1]);
		        	totalCompressedSize += result[0];
		        } while (ts.size > 0);
                System.out.printf("GibbonAuto\t%s\tEpsilon: %.5f\tCompression Ratio: %.3f\tCompression Time: %.3f\tDecompression Time: %.3f\tMAE: %5f\tRMSE: %5f\n",
                        filename, epsilon, (double) totalSize / totalCompressedSize, compressionTime / 1000000.0, decompressionTime / 1000000.0, error / totalSize, Math.sqrt(squareError / (totalSize * totalSize)));
	        }
	    }
	}

//    @Test
    public void testFloats() {
        int spacePowers[] = new int[32];
        for (int i=0; i<spacePowers.length; i++) {
            spacePowers[i] = (int) Math.pow(2, i) - 1;
        }

        float value1 = 4.6692F;
        float value2 = 1.618F;

        int logOfError = 0;
        double epsilon = 0.00005;
        for (int power = 30; power > -30; power--) {
            if (Math.pow(2, power) < epsilon) {
                logOfError = power;
                break;
            }
        }

        int storedVal = Float.floatToRawIntBits(value1);
        int value = Float.floatToRawIntBits(value2);
        System.out.println(String.format("%32s", Integer.toBinaryString(value)).replace(' ', '0'));
        // TODO Fix already compiled into a big method
        int integerDigits = (value << 1 >>> 24) - 127;
        System.out.println("Digits:\t" + integerDigits);
        int space = Math.min(23, 23 + logOfError - integerDigits);
        System.out.println("Space:\t" + space);
        if (space > 0) {
            value = value >> space << space;
            value = value | (storedVal & spacePowers[space]);
        }
        int xor = storedVal ^ value;



        System.out.println(String.format("%32s", Integer.toBinaryString(storedVal)).replace(' ', '0'));
        System.out.println(String.format("%32s", Integer.toBinaryString(value)).replace(' ', '0'));
        System.out.println(String.format("%32s", Integer.toBinaryString(xor)).replace(' ', '0'));

    }

    @Test
    public void testZeroingandReplacing() {
        int spacePowers[] = new int[32];
        for (int i=0; i<spacePowers.length; i++) {
            spacePowers[i] = (int) Math.pow(2, i) - 1;
        }

        int logOfError = 0;
        double epsilon = 0.5;
        for (int power = 30; power > -30; power--) {
            if (Math.pow(2, power) < epsilon) {
                logOfError = power;
                break;
            }
        }

        String line = "";
        double totalError = 0D;
        int count = 0;
        Set<Integer> masks = new HashSet<>();
        Set<Integer> numbers = new HashSet<>();
        Random random = new Random();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(TestCR.class.getResourceAsStream("/Cricket-uniq.csv.gz"))))) {
            float storedValueFloat = Float.parseFloat(br.readLine().trim().split(",")[1]);
            while ((line = br.readLine()) != null) {
                float valueFloat = Float.parseFloat(line.trim().split(",")[1]);

                int storedVal = Float.floatToRawIntBits(storedValueFloat);
//                int storedVal = random.nextInt();
                int value = Float.floatToRawIntBits(valueFloat);

                int integerDigits = (value << 1 >>> 24) - 127;
                int space = Math.min(23, 23 + logOfError - integerDigits);
                if (space > 0) {
                    value = value >> space << space;
//                    value = value | spacePowers[space];
                    value = value | (storedVal & spacePowers[space]);
                    masks.add(storedVal & spacePowers[space]);
                    numbers.add(storedVal);
                }

                float approximation = Float.intBitsToFloat(value);
                totalError += Math.abs(valueFloat - approximation);
                count++;
                storedValueFloat = valueFloat;

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Average error is: " + totalError / count);
        System.out.println("Masks: " + masks.size() + ", Numbers: " + numbers.size() + " Count: " + count);

    }
}
