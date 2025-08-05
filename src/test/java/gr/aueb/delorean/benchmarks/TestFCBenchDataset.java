package gr.aueb.delorean.benchmarks;

import gr.aueb.delorean.util.TimeSeries;
import gr.aueb.delorean.util.TimeSeriesReader;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class TestFCBenchDataset {


    final String[] filenames = {
//            "acs_wht_f32.csv.gz",
            "astro_mhd_f64.csv.gz",
//            "astro_pt_f64.csv.gz",
            "citytemp_f32.csv.gz",
//            "g24_78_usb2_f32.csv.gz",
//            "hdr_night_f32.csv.gz",
//            "hdr_palermo_f32.csv.gz",
//            "hst_wfc3_ir_f32.csv.gz",
//            "hst_wfc3_uvis_f32.csv.gz",
//            "jane_street_f64.csv.gz",
//            "jw_mirimage_f32.csv.gz",
//            "msg_bt_f64.csv.gz",
//            "num_brain_f64.csv.gz",
//            "num_control_f64.csv.gz",
//            "nyc_taxi2015_f64.csv.gz",
//            "phone_gyro_f64.csv.gz",
//            "rsim_f32.csv.gz",
//            "solar_wind_f32.csv.gz",
//            "spain_gas_price_f64.csv.gz",
//            "ts_gas_f32.csv.gz",
//            "turbulence_f32.csv.gz",
//            "wave_f32.csv.gz",
//            "wesad_chest_f64.csv.gz",
    };

    @Test
    public void testCrDecimalsInBlocks() throws IOException {
        double[] epsilons = {0.05, 0.005, 0.0005, 0.00005, 0.000005};
//        double[] epsilons = {0.005};

        String delimiter = ",";
        int blockSize = 1000;

        for (String filename : filenames) {
            long[] result = {0, 3, 1};
            for (double epsilon : epsilons) {
                Map<Integer, Integer> ks = new HashMap<>();
                Map<Integer, Integer> modes = new HashMap<>();
                long totalCompressedSize = 0;
                long totalSize = 0;
                long totalTrailingZerosSum = 0;
                int totalTrailingZerosCnt = 0;
                CompressUtils.init();
//                InputStream inputStream = new FileInputStream(datasetPath + filename);
                InputStream inputStream = TestFCBenchDataset.class.getResourceAsStream("/" + filename);
                InputStream gzipStream = new GZIPInputStream(inputStream);
                Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(decoder);
                TimeSeries series;
                do {
                    series = TimeSeriesReader.getTimeSeriesBlock(bufferedReader, blockSize);
                    totalSize += series.data.size() * 8L;
                    result = CompressUtils.Gibbon(series.data, epsilon, (int) result[1], (int) result[2]);
                    ks.put((int) result[1], ks.getOrDefault((int) result[1], 0) + 1);
                    modes.put(0, modes.getOrDefault(0, 0) + (int) result[5]);
                    modes.put(1, modes.getOrDefault(1, 0) + (int) result[6]);
                    modes.put(2, modes.getOrDefault(2, 0) + (int) result[7]);
                    modes.put(3, modes.getOrDefault(3, 0) + (int) result[8]);
                    totalCompressedSize += result[0];
                    totalTrailingZerosSum += result[3];
                    totalTrailingZerosCnt += result[4];
                } while (!series.data.isEmpty());
                System.out.printf("Gibbon\t%s\tEpsilon: %.6f\tCompression Ratio: %.3f\tCompression Time: %.4f\tDecompression Time: %.4f\tMAE: %.10f\tRMSE: %.10f\tBytes: %d\tCases: %s\tTZ: %.3f\tMode: %s\n",
                        filename, epsilon, (double) totalSize / totalCompressedSize, CompressUtils.getCompressionTime() / (double) totalSize, CompressUtils.getDecompressionTime() / (double) totalSize, CompressUtils.getError() / totalSize, Math.sqrt(CompressUtils.getSquareError() / (totalSize * totalSize)), totalCompressedSize, ks.toString().replace(" ", ""), totalTrailingZerosSum*1.0/totalTrailingZerosCnt, modes.toString().replace(" ", ""));
            }
        }
    }

}
