package gr.aueb.delorean.gibbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Test;

import fi.iki.yak.ts.compression.gorilla.ByteBufferBitInput;
import fi.iki.yak.ts.compression.gorilla.ByteBufferBitOutput;
import gr.aueb.delorean.gibbon.GibbonCompressor;
import gr.aueb.delorean.gibbon.GibbonDecompressor;
import gr.aueb.delorean.gibbon.Value;

/**
 * These are generic tests to test that input matches the output after compression + decompression cycle, using
 * both the timestamp and value compression.
 *
 * @author Michael Burman
 */
public class CompressTest {

	private class TimeseriesFileReader {
		private static final int DEFAULT_BLOCK_SIZE = 1_000;
		private static final String DELIMITER = ",";
		private static final int VALUE_POSITION = 1;
		BufferedReader bufferedReader;
		private int blocksize;

		public TimeseriesFileReader(InputStream inputStream) throws IOException {
			this(inputStream, DEFAULT_BLOCK_SIZE);
		}

		public TimeseriesFileReader(InputStream inputStream, int blocksize) throws IOException {
			InputStream gzipStream = new GZIPInputStream(inputStream);
			Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
			this.bufferedReader = new BufferedReader(decoder);
			this.blocksize = blocksize;
		}

		public Collection<Double> nextBlock() {
			Collection<Double> list = new ArrayList<>();
			String line;
			try {
				while ((line = bufferedReader.readLine()) != null) {
					double value = Double.parseDouble(line.split(DELIMITER)[VALUE_POSITION]);
					list.add(value);
					if (list.size() == blocksize) {
						return list;
					}
				}
			} catch (NumberFormatException | IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}


	@Test
	public void testPrecisionNewLossy32ForBaselTemp() throws IOException {

		for (int logOfError = -10; logOfError < 10; logOfError++) {
			String filename = "/basel-temp.csv.gz";
			TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(this.getClass().getResourceAsStream(filename));
			Collection<Double> values;
			double maxValue = Double.MIN_VALUE;
			double minValue = Double.MAX_VALUE;
			double maxPrecisionError = 0;
			int totalSize = 0;
			float totalBlocks = 0;
			float totalTrailingDiff = 0;
			int totalCases0 = 0;
			int totalCases1 = 0;
			int totalCases2 = 0;
			while ((values = timeseriesFileReader.nextBlock()) != null) {
				ByteBufferBitOutput output = new ByteBufferBitOutput();
				GibbonCompressor compressor = new GibbonCompressor(output, logOfError);
				values.forEach(value -> compressor.addValue(value.floatValue()));
		        compressor.close();
		        totalSize += compressor.getSize();
		        totalBlocks += 1;
		        totalTrailingDiff += compressor.getTrailingDiff();
		        totalCases0 += compressor.getCases()[0];
		        totalCases1 += compressor.getCases()[1];
		        totalCases2 += compressor.getCases()[2];
		        ByteBuffer byteBuffer = output.getByteBuffer();
		        byteBuffer.flip();
		        ByteBufferBitInput input = new ByteBufferBitInput(byteBuffer);
		        GibbonDecompressor d = new GibbonDecompressor(input);
		        for(Double value : values) {
		        	maxValue = value > maxValue ? value : maxValue;
		        	minValue = value < minValue ? value : minValue;
		            Value pair = d.readValue();
		            double precisionError = Math.abs(value.doubleValue() - pair.getFloatValue());
		            maxPrecisionError = (precisionError > maxPrecisionError) ? precisionError : maxPrecisionError;
		            assertEquals(value.doubleValue(), pair.getFloatValue(), Math.pow(2, logOfError), "Value did not match");
		        }
		        assertNull(d.readValue());
			}
			float total = totalCases0 + totalCases1 + totalCases2;
			System.out.println(String.format("NewLossy32 %s - Size : %d, Bits/value: %.2f, error: %f, Range: %.2f, (%.2f%%), Avg. Unexploited Trailing: %.2f, Cases 0: %.2f, 10: %.2f, 11: %.2f",
					filename, totalSize, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), maxPrecisionError, (maxValue - minValue), 100* maxPrecisionError / (maxValue - minValue), totalTrailingDiff / totalCases1, totalCases0 / total, totalCases1 / total, totalCases2 / total));
		}
	}

	@Test
	public void testPrecisionNewLossy32ForBaselWindSpeed() throws IOException {

		for (int logOfError = -10; logOfError < 10; logOfError++) {
			String filename = "/basel-wind-speed.csv.gz";
			TimeseriesFileReader timeseriesFileReader = new TimeseriesFileReader(this.getClass().getResourceAsStream(filename));
			Collection<Double> values;
			double maxValue = Double.MIN_VALUE;
			double minValue = Double.MAX_VALUE;
			double maxPrecisionError = 0;
			int totalSize = 0;
			float totalBlocks = 0;
			float totalTrailingDiff = 0;
			int totalCases0 = 0;
			int totalCases1 = 0;
			int totalCases2 = 0;
			while ((values = timeseriesFileReader.nextBlock()) != null) {
				ByteBufferBitOutput output = new ByteBufferBitOutput();
				GibbonCompressor compressor = new GibbonCompressor(output, logOfError);
				values.forEach(value -> compressor.addValue(value.floatValue()));
		        compressor.close();
		        totalSize += compressor.getSize();
		        totalBlocks += 1;
		        totalTrailingDiff += compressor.getTrailingDiff();
		        totalCases0 += compressor.getCases()[0];
		        totalCases1 += compressor.getCases()[1];
		        totalCases2 += compressor.getCases()[2];
		        ByteBuffer byteBuffer = output.getByteBuffer();
		        byteBuffer.flip();
		        ByteBufferBitInput input = new ByteBufferBitInput(byteBuffer);
		        GibbonDecompressor d = new GibbonDecompressor(input);
		        for(Double value : values) {
		        	maxValue = value > maxValue ? value : maxValue;
		        	minValue = value < minValue ? value : minValue;
		            Value pair = d.readValue();
		            double precisionError = Math.abs(value.doubleValue() - pair.getFloatValue());
		            maxPrecisionError = (precisionError > maxPrecisionError) ? precisionError : maxPrecisionError;
		            assertEquals(value.doubleValue(), pair.getFloatValue(), Math.pow(2, logOfError), "Value did not match");
		        }
		        assertNull(d.readValue());
			}
			float total = totalCases0 + totalCases1 + totalCases2;
			System.out.println(String.format("NewLossy32 %s - Size : %d, Bits/value: %.2f, error: %f, Range: %.2f, (%.2f%%), Avg. Unexploited Trailing: %.2f, Cases 0: %.2f, 10: %.2f, 11: %.2f",
					filename, totalSize, totalSize / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE), maxPrecisionError, (maxValue - minValue), 100* maxPrecisionError / (maxValue - minValue), totalTrailingDiff / totalCases1, totalCases0 / total, totalCases1 / total, totalCases2 / total));
		}
	}



}
