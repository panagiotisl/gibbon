package gr.aueb.delorean.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class TimeSeriesReader {
    public static TimeSeries getTimeSeries(InputStream inputStream, String delimiter) {
        ArrayList<Point> ts = new ArrayList<>();
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;

        try {
            InputStream gzipStream = new GZIPInputStream(inputStream);
            Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(decoder);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] elements = line.split(delimiter);
                long timestamp = Long.parseLong(elements[0]);
                double value = Double.parseDouble(elements[1]);
                ts.add(new Point(timestamp, value));

                max = Math.max(max, value);
                min = Math.min(min, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new TimeSeries(ts, max - min);
    }

    public static TimeSeries getTimeSeriesBlock(BufferedReader bufferedReader, String delimiter, int blockSize) {
        List<Point> ts = new LinkedList<>();
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;

        try {
            String line;
            while ((line = bufferedReader.readLine()) != null && ts.size() < blockSize) {
                String[] elements = line.split(delimiter);
                long timestamp = Long.parseLong(elements[0]);
                double value = Double.parseDouble(elements[1]);
                ts.add(new Point(timestamp, value));

                max = Math.max(max, value);
                min = Math.min(min, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new TimeSeries(ts, max - min);
    }

    public static TimeSeries getTimeSeriesBlock(BufferedReader bufferedReader, int blockSize) {
        List<Point> ts = new LinkedList<>();
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        long timestamp = 0;
        try {
            String line;
            while ((line = bufferedReader.readLine()) != null && ts.size() < blockSize) {
                double value = Double.parseDouble(line.trim());
                ts.add(new Point(timestamp++, value));

                max = Math.max(max, value);
                min = Math.min(min, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new TimeSeries(ts, max - min);
    }

    public static Collection<Double> getSeriesBlock(BufferedReader bufferedReader, int blockSize) {
        List<Double> series = new LinkedList<>();
        try {
            String line;
            while ((line = bufferedReader.readLine()) != null && series.size() < blockSize) {
                double value = Double.parseDouble(line.trim());
                series.add(value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return series;
    }
}
