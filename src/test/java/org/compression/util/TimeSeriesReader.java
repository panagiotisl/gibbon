package gr.aueb.delorean.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class TimeSeriesReader {

    public static TimeSeries getTimeSeriesBlock(BufferedReader bufferedReader, int blockSize) {
        List<Point> ts = new LinkedList<>();
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        long timestamp = 0;
        try {
            String line;
            while (ts.size() < blockSize && (line = bufferedReader.readLine()) != null) {
                if (line.trim().equalsIgnoreCase("nan")) {
                    continue;
                }
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

}
