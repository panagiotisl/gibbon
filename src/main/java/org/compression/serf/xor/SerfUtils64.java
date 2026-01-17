package gr.aueb.delorean.serf.xor;

// SerfUtils64.java
// Faithful Java translation of utils/serf_utils_64.h and serf_utils_64.cpp

public class SerfUtils64 {
    private static final long[] kBitWeight = new long[65];

    static {
        for (int i = 0; i < 64; i++) {
            kBitWeight[i] = 1L << i;
        }
        kBitWeight[64] = 0x8000000000000000L; // guard bit
    }

    public static long findAppLong(double min, double max, double v, long lastLong, double maxDiff, double adjustDigit) {
        if (min >= 0) {
            // both positive
            return findAppLong(min, max, 0L, v, lastLong, maxDiff, adjustDigit);
        } else if (max <= 0) {
            // both negative
            return findAppLong(-max, -min, 0x8000000000000000L, v, lastLong, maxDiff, adjustDigit);
        } else if ((lastLong >>> 63) == 0) {
            // consider positive part only
            return findAppLong(0, max, 0L, v, lastLong, maxDiff, adjustDigit);
        } else {
            // consider negative part only
            return findAppLong(0, -min, 0x8000000000000000L, v, lastLong, maxDiff, adjustDigit);
        }
    }

    public static long findAppLong(double minDouble, double maxDouble, long sign,
                                   double original, long lastLong, double maxDiff, double adjustDigit) {

        long min = Double.doubleToLongBits(minDouble) & 0x7fffffffffffffffL; // clear sign bit
        long max = Double.doubleToLongBits(maxDouble);
        int leadingZeros = Long.numberOfLeadingZeros(min ^ max);
        long frontMask = -1L << (64 - leadingZeros);
        int shift = 64 - leadingZeros;
        long resultLong;
        double diff;
        long append;

        for (; shift >= 0; --shift) {
            long front = frontMask & min;
            long rear = (~frontMask) & lastLong;
            append = rear | front;

            boolean condition1 = append >= min && append <= max;
            resultLong = condition1 ? (append ^ sign) : 0;

            diff = Double.longBitsToDouble(resultLong) - adjustDigit - original;
            boolean diffSatisfied = diff >= -maxDiff && diff <= maxDiff;
            if (condition1 && diffSatisfied) {
                return resultLong;
            }

            // avoid overflow
            append = (append + kBitWeight[shift]) & 0x7fffffffffffffffL;
            boolean condition2 = append <= max;
            resultLong = condition2 ? (append ^ sign) : 0;

            diff = Double.longBitsToDouble(resultLong) - adjustDigit - original;
            diffSatisfied = diff >= -maxDiff && diff <= maxDiff;
            if (condition2 && diffSatisfied) {
                return resultLong;
            }

            frontMask >>>= 1;
        }

        // fallback
        return Double.doubleToLongBits(original + adjustDigit);
    }

    // === FindAppLongNoPlus ===

    public static long findAppLongNoPlus(double min, double max, double v, long lastLong, double maxDiff, double adjustDigit) {
        if (min >= 0) {
            return findAppLongNoPlus(min, max, 0L, v, lastLong, maxDiff, adjustDigit);
        } else if (max <= 0) {
            return findAppLongNoPlus(-max, -min, 0x8000000000000000L, v, lastLong, maxDiff, adjustDigit);
        } else if ((lastLong >>> 63) == 0) {
            return findAppLongNoPlus(0, max, 0L, v, lastLong, maxDiff, adjustDigit);
        } else {
            return findAppLongNoPlus(0, -min, 0x8000000000000000L, v, lastLong, maxDiff, adjustDigit);
        }
    }

    public static long findAppLongNoPlus(double minDouble, double maxDouble, long sign,
                                         double original, long lastLong, double maxDiff, double adjustDigit) {
        long min = Double.doubleToLongBits(minDouble) & 0x7fffffffffffffffL;
        long max = Double.doubleToLongBits(maxDouble);
        int leadingZeros = Long.numberOfLeadingZeros(min ^ max);
        long frontMask = -1L << (64 - leadingZeros);
        int shift = 64 - leadingZeros;
        long resultLong;
        double diff;
        long append;

        while (shift >= 0) {
            long front = frontMask & min;
            long rear = (~frontMask) & lastLong;
            append = rear | front;
            if (append >= min && append <= max) {
                resultLong = append ^ sign;
                diff = Double.longBitsToDouble(resultLong) - adjustDigit - original;
                if (diff >= -maxDiff && diff <= maxDiff) {
                    return resultLong;
                }
            }
            frontMask >>>= 1;
            --shift;
        }
        return Double.doubleToLongBits(original + adjustDigit);
    }

    // === FindAppLongNoFast ===

    public static long findAppLongNoFast(double min, double max, double v, long lastLong, double maxDiff, double adjustDigit) {
        if (min >= 0) {
            return findAppLongNoFast(min, max, 0L, v, lastLong, maxDiff, adjustDigit);
        } else if (max <= 0) {
            return findAppLongNoFast(-max, -min, 0x8000000000000000L, v, lastLong, maxDiff, adjustDigit);
        } else if ((lastLong >>> 63) == 0) {
            return findAppLongNoFast(0, max, 0L, v, lastLong, maxDiff, adjustDigit);
        } else {
            return findAppLongNoFast(0, -min, 0x8000000000000000L, v, lastLong, maxDiff, adjustDigit);
        }
    }

    public static long findAppLongNoFast(double minDouble, double maxDouble, long sign,
                                         double original, long lastLong, double maxDiff, double adjustDigit) {
        long min = Double.doubleToLongBits(minDouble) & 0x7fffffffffffffffL;
        long max = Double.doubleToLongBits(maxDouble);
        long frontMask = -1L;
        long resultLong;
        double diff;
        long append;

        for (int i = 1; i <= 64; ++i) {
            long mask = frontMask << (64 - i);
            append = (lastLong & ~mask) | (min & mask);

            if (min <= append && append <= max) {
                resultLong = append ^ sign;
                diff = Double.longBitsToDouble(resultLong) - adjustDigit - original;
                if (diff >= -maxDiff && diff <= maxDiff) {
                    return resultLong;
                }
            }

            // may overflow
            append = (append + kBitWeight[64 - i]) & 0x7fffffffffffffffL;
            if (append <= max) {
                resultLong = append ^ sign;
                diff = Double.longBitsToDouble(resultLong) - adjustDigit - original;
                if (diff >= -maxDiff && diff <= maxDiff) {
                    return resultLong;
                }
            }
        }
        return Double.doubleToLongBits(original + adjustDigit);
    }
}
