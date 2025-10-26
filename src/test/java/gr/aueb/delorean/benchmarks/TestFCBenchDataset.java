package gr.aueb.delorean.benchmarks;

import gr.aueb.delorean.lossless.GorillaCompressor;
import gr.aueb.delorean.lossless.GorillaDecompressor;
import gr.aueb.delorean.Compressor;
import gr.aueb.delorean.Decompressor;
import gr.aueb.delorean.lossless.LosslessCompressor;
import gr.aueb.delorean.lossless.ChimpN32Compressor;
import gr.aueb.delorean.lossless.ChimpN32Decompressor;
import gr.aueb.delorean.lossless.Patas32Compressor;
import gr.aueb.delorean.lossless.Patas32Decompressor;
import gr.aueb.delorean.gibbon.GibbonCompressor;
import gr.aueb.delorean.gibbon.GibbonDecompressor;
import gr.aueb.delorean.serf.qt.SerfQtCompressor;
import gr.aueb.delorean.serf.qt.SerfQtDecompressor;
import gr.aueb.delorean.serf.xor.SerfXORCompressor;
import gr.aueb.delorean.serf.xor.SerfXORDecompressor;
import gr.aueb.delorean.util.TimeSeries;
import gr.aueb.delorean.util.TimeSeriesReader;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class TestFCBenchDataset {


    final String[] filenames = {
//            "acs_wht_f32.csv.gz",
//            "astro_mhd_f64.csv.gz",
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
    public void testCrDecimalsInBlocks() throws IOException, ReflectiveOperationException {
        double[] epsilons = {0.00005, 0.0005, 0.005, 0.05};

        int blockSize = 1000;

        List<Class<? extends Compressor>> compressors = List.of(
                GibbonCompressor.class,
                SerfXORCompressor.class,
                SerfQtCompressor.class,
                GorillaCompressor.class,
                ChimpN32Compressor.class,
                Patas32Compressor.class
        );

        List<Class<? extends Decompressor>> decompressors = List.of(
                GibbonDecompressor.class,
                SerfXORDecompressor.class,
                SerfQtDecompressor.class,
                GorillaDecompressor.class,
                ChimpN32Decompressor.class,
                Patas32Decompressor.class
        );

        for (int i = 0; i < compressors.size(); i++) {
            Class<? extends Compressor> compressorClass = compressors.get(i);
            Class<? extends Decompressor> decompressorClass = decompressors.get(i);

            for (String filename : filenames) {
                long[] result = {0, 3, 1};
                for (double epsilon : epsilons) {
                    long totalCompressedSize = 0;
                    long totalSize = 0;
                    long seriesSize = 0;
                    CompressUtils.init();
                    InputStream inputStream = TestFCBenchDataset.class.getResourceAsStream("/" + filename);
                    InputStream gzipStream = new GZIPInputStream(inputStream);
                    Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
                    BufferedReader bufferedReader = new BufferedReader(decoder);
                    TimeSeries series;
                    Compressor compressor;
                    do {
                        series = TimeSeriesReader.getTimeSeriesBlock(bufferedReader, blockSize);
                        totalSize += series.data.size() * 4L;
                        seriesSize += series.data.size();
                        if (result.length == 3) {
                            compressor = createCompressor(compressorClass, epsilon, (int) result[1], (int) result[2]);
                        } else {
                            compressor = createCompressor(compressorClass, epsilon, 0, 0);
                        }
                        result = CompressUtils.compress(compressor, series.data, epsilon, decompressorClass);
                        totalCompressedSize += result[0];
                    } while (!series.data.isEmpty());
                    double cr = (double) totalSize / totalCompressedSize;
                    double compressionRate = 4 * 1000000000D / ((CompressUtils.getCompressionTime() / (double) seriesSize) * 1024 * 1024);
                    double decompressionRate = 4 * 1000000000D / ((CompressUtils.getDecompressionTime() / (double) seriesSize) * 1024 * 1024);
                    long digits = compressor instanceof LosslessCompressor ? 0 : Math.round(Math.log10(1/epsilon));
                    double mae = CompressUtils.getError() / seriesSize;
                    double rmse = Math.sqrt(CompressUtils.getSquareError() / (seriesSize * seriesSize));
                    System.out.printf("%s\t%s\tDecimalDigitsPrecision: %d\tCompressionRatio: %.2f\tCompressionRate: %.4f\tDecompressionRate: %.4f\tMAE: %.10f\tRMSE: %.10f\n",
                            compressor.getClass().getSimpleName(), filename, digits, cr, compressionRate, decompressionRate, mae, rmse);
                    if (compressor instanceof LosslessCompressor) {
                        break;
                    }
                }
            }

        }


    }

    public static Compressor createCompressor(
            Class<? extends Compressor> compressorClass,
            double epsilon,
            int k,
            int f

    ) throws ReflectiveOperationException {

        if (compressorClass == GibbonCompressor.class) {
            return new GibbonCompressor(epsilon, k ,f);
        }
        try {
            Constructor<? extends Compressor> ctor = compressorClass.getConstructor(double.class);
            return ctor.newInstance(epsilon);
        } catch (NoSuchMethodException e) {
            Constructor<? extends Compressor> ctor = compressorClass.getConstructor();
            return ctor.newInstance();
        }
    }


}
