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

        String delimiter = ",";
        int blockSize = 1000;

        for (String filename : filenames) {
            long[] result = {0, 3, 1};
            for (double epsilon : epsilons) {
                long totalCompressedSize = 0;
                long totalSize = 0;
                long totalTrailingZerosSum = 0;
                int totalTrailingZerosCnt = 0;
                CompressUtils.init();
                InputStream inputStream = TestFCBenchDataset.class.getResourceAsStream("/" + filename);
                InputStream gzipStream = new GZIPInputStream(inputStream);
                Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(decoder);
                TimeSeries series;
                do {
                    series = TimeSeriesReader.getTimeSeriesBlock(bufferedReader, blockSize);
                    totalSize += series.data.size() * 8L;
                    result = CompressUtils.Gibbon(series.data, epsilon, (int) result[1], (int) result[2]);
                    totalCompressedSize += result[0];
                    totalTrailingZerosSum += result[3];
                    totalTrailingZerosCnt += result[4];
                } while (!series.data.isEmpty());
                System.out.printf("Gibbon\t%s\tEpsilon: %.6f\tCompression Ratio: %.3f\tCompression Time: %.4f\tDecompression Time: %.4f\tMAE: %.10f\tRMSE: %.10f\tBytes: %d\n",
                        filename, epsilon, (double) totalSize / totalCompressedSize, CompressUtils.getCompressionTime() / (double) totalSize, CompressUtils.getDecompressionTime() / (double) totalSize, CompressUtils.getError() / totalSize, Math.sqrt(CompressUtils.getSquareError() / (totalSize * totalSize)), totalCompressedSize, totalTrailingZerosSum*1.0/totalTrailingZerosCnt);
            }
        }
    }

}
