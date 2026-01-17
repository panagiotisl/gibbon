package gr.aueb.delorean.serf;

public class Value {
    private final long value;

    public Value(long value) {
        this.value = value;
    }

    public double getFloatValue() {
        return Double.longBitsToDouble(value);
    }

}
