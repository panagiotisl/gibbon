package gr.aueb.delorean.benchmarks;

import gr.aueb.delorean.util.TimeSeries;
import gr.aueb.delorean.util.TimeSeriesReader;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

public class TestFCBenchDataset {

    final String datasetPath = "/home/panagiotis/workspace/datasets/FCBench/";

    final String[] filenames = {
            "citytemp_f32.csv.gz",
            "astro_mhd_f64.csv.gz",
    };

    @Test
    public void testCrDecimalsInBlocks() throws IOException {
        double[] epsilons = {0.05, 0.005, 0.0005, 0.00005, 0.000005};
//        double[] epsilons = {0.000005};

        String delimiter = ",";
        int blockSize = 10000;

        for (String filename : filenames) {
            long[] result = {0, 0};
            for (double epsilon : epsilons) {
                long totalCompressedSize = 0;
                long totalSize = 0;
                CompressUtils.init();
                InputStream inputStream = new FileInputStream(datasetPath + filename);
                InputStream gzipStream = new GZIPInputStream(inputStream);
                Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(decoder);
                Collection<Double> series;
                do {
                    series = TimeSeriesReader.getSeriesBlock(bufferedReader, blockSize);
                    totalSize += series.size() * 4;
                    result = CompressUtils.GibbonAuto(series, epsilon, (int) result[1]);
                    totalCompressedSize += result[0];
                } while (!series.isEmpty());
                System.out.printf("GibbonAuto\t%s\tEpsilon: %.6f\tCompression Ratio: %.3f\tCompression Time: %.9f\tDecompression Time: %.3f\tMAE: %5f\tRMSE: %5f\n",
                        filename, epsilon, (double) totalSize / totalCompressedSize, CompressUtils.getCompressionTime() / (double) totalSize, CompressUtils.getDecompressionTime() / (double) totalSize, CompressUtils.getError() / totalSize, Math.sqrt(CompressUtils.getSquareError() / (totalSize * totalSize)));
            }
        }
    }

}
