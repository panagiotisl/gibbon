package gr.aueb.delorean.lossless;

public class Value {
    private final long value;

    public Value(long value) {
        this.value = value;
    }

    public double getDoubleValue() {
        return Double.longBitsToDouble(value);
    }

}
