package gr.aueb.delorean.benchmarks;

import gr.aueb.delorean.util.TimeSeries;
import gr.aueb.delorean.gibbon.GibbonAutoCompressor;
import gr.aueb.delorean.gibbon.GibbonAutoDecompressor;
import gr.aueb.delorean.gibbon.GibbonCompressor;
import gr.aueb.delorean.gibbon.GibbonDecompressor;
import gr.aueb.delorean.gibbon.RunLengthEncodingDecompressor32;
import gr.aueb.delorean.gibbon.RunLengthEncodingLossyCompressor32;
import gr.aueb.delorean.pmcmr.PMCMRCompressor;
import gr.aueb.delorean.pmcmr.PMCMRDecompressor;
import gr.aueb.delorean.pmcmr.PMCMREncoder;
import gr.aueb.delorean.pmcmr.PMCMRSegment;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCR {
	
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
          "/Stocks-USA-sample.csv.gz",
          "/NEON_temp-bio-bioTempMean-sample.csv.gz",
          "/NEON_pressure-air_staPresMean-sample.csv.gz",
          "/NEON_wind-2d_windDirMean-sample.csv.gz",
          "/NEON_temp-bio-bioTempMean-sample.csv.gz"
          
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
    	while (iterator.hasNext()) {
    		compressor.addValue((float) iterator.next().getValue());
    	}
        compressor.close();
        long compressedSize = compressor.getSize() / 8;
        
        ByteBuffer byteBuffer = output.getByteBuffer();
        byteBuffer.flip();
        ByteBufferBitInput input = new ByteBufferBitInput(byteBuffer);
        GibbonDecompressor d = new GibbonDecompressor(input);
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
    
    
    private long[] GibbonAuto(List<Point> ts, double epsilon, int mode) {
    	ByteBufferBitOutput output = new ByteBufferBitOutput();
    	GibbonAutoCompressor compressor = new GibbonAutoCompressor(output, epsilon, mode);
    	Iterator<Point> iterator = ts.iterator();
    	while (iterator.hasNext()) {
    		compressor.addValue((float) iterator.next().getValue());
    	}
        compressor.close();
        long compressedSize = compressor.getSize() / 8;
        
        ByteBuffer byteBuffer = output.getByteBuffer();
        byteBuffer.flip();
        ByteBufferBitInput input = new ByteBufferBitInput(byteBuffer);
        GibbonAutoDecompressor d = new GibbonAutoDecompressor(input);
        for (Point point : ts) {
            float decompressedValue = d.readValue().getFloatValue();
            assertEquals(
                    (float) point.getValue(),
                    decompressedValue,
                    1.1 * epsilon,
                    "Value did not match for timestamp " + point.getTimestamp()
            );
        }
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


    @Test
    public void testCR() {
        double epsilonStart = 0.005;
        double epsilonStep = 0.005;
        double epsilonEnd = 0.05;

        String delimiter = ",";

        for (String filename : filenames) {
            System.out.println(filename);
            TimeSeries ts = TimeSeriesReader.getTimeSeries(getClass().getResourceAsStream(filename), delimiter);

            System.out.println("PMCMR");
            for (double epsilonPct = epsilonStart; epsilonPct <= epsilonEnd; epsilonPct += epsilonStep)
                System.out.printf("Epsilon: %.2f%%\tCompression Ratio: %.3f\tepsilin: %.5f\n",
                        epsilonPct * 100, (double) ts.size / PMCMR(ts.data, ts.range * epsilonPct), ts.range * epsilonPct);

            System.out.println("Swing");
            for (double epsilonPct = epsilonStart; epsilonPct <= epsilonEnd; epsilonPct += epsilonStep)
                System.out.printf("Epsilon: %.2f%%\tCompression Ratio: %.3f\n",
                        epsilonPct * 100, (double) ts.size / Swing(ts.data, ts.range * epsilonPct));

            System.out.println("Gibbon");
            for (double epsilonPct = epsilonStart; epsilonPct <= epsilonEnd; epsilonPct += epsilonStep)
                System.out.printf("Epsilon: %.2f%%\tCompression Ratio: %.3f\n",
                        epsilonPct * 100, (double) ts.size / Gibbon(ts.data, ts.range * epsilonPct));

            System.out.println("GibbonRLE");
            for (double epsilonPct = epsilonStart; epsilonPct <= epsilonEnd; epsilonPct += epsilonStep)
                System.out.printf("Epsilon: %.2f%%\tCompression Ratio: %.3f\n",
                        epsilonPct * 100, (double) ts.size / GibbonRLE(ts.data, ts.range * epsilonPct));

//            System.out.println("Sim-Piece");
//            for (double epsilonPct = epsilonStart; epsilonPct <= epsilonEnd; epsilonPct += epsilonStep)
//                System.out.printf("Epsilon: %.2f%%\tCompression Ratio: %.3f\n",
//                        epsilonPct * 100, (double) ts.size / SimPiece(ts.data, ts.range * epsilonPct));

            System.out.println();
        }
    }
    
    @Test
    public void testCrDecimals() {
    	double[] epsilons = {0.05, 0.005, 0.0005, 0.00005, 0.000005};

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
            for (double epsilon : epsilons)
            	System.out.printf("Epsilon: %.5f\tCompression Ratio: %.3f\n",
                		epsilon, (double) ts.size / Gibbon(ts.data, epsilon));

            System.out.println("GibbonRLE");
            for (double epsilon : epsilons)
            	System.out.printf("Epsilon: %.5f\tCompression Ratio: %.3f\n",
                		epsilon, (double) ts.size / GibbonRLE(ts.data, epsilon));

//            System.out.println("Sim-Piece");
//            for (double epsilon : epsilons)
//            	System.out.printf("Epsilon: %.5f\tCompression Ratio: %.3f\n",
//                		epsilon, (double) ts.size / SimPiece(ts.data, epsilon));

            System.out.println();
        }
    }


	@Test
	public void testCrDecimalsInBlocks() throws IOException {
		double[] epsilons = {0.05, 0.005, 0.0005, 0.00005, 0.000005};
	
	    String delimiter = ",";
	    int blockSize = 1000;
	
	    for (String filename : filenames) {
	        System.out.println(filename);
	        System.out.println("Gibbon");
	        long[] result = {0, 0};
	        for (double epsilon : epsilons) {
	        	long totalCompressedSize = 0;
	        	long totalSize = 0;
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
		        System.out.printf("%.3f\n",
	            		(double) totalSize / totalCompressedSize);
//		        System.out.printf("Epsilon: %.5f\tCompression Ratio: %.3f - Size: %d, Compressed size: %d\n",
//	            		epsilon, (double) totalSize / totalCompressedSize, totalSize, totalCompressedSize);
	        }
	
	        System.out.println();
	    }
	}
}