package gr.aueb.delorean.gibbon;

/**
 * Value is an extracted value from the stream
 *
 * @author Michael Burman
 */
public class Value {
    private int value;
    private int diff;
    private float epsilon;

    public Value(int value) {
        this.value = value;
        this.diff = -1;
    }

    public Value(int value, int diff, float epsilon) {
        this.value = value;
        this.diff = diff;
        this.epsilon = epsilon;
    }

    public float getFloatValue() {
        return Float.intBitsToFloat(value);
        // switch (diff) {
        //     case 0:
        //         return Float.intBitsToFloat(value) + epsilon;
        //     case 1:
        //         return Float.intBitsToFloat(value) - epsilon;
        //     case 2:
        //         return Float.intBitsToFloat(value) + 2 * epsilon;
        //     case 3:
        //         return Float.intBitsToFloat(value) - 2 * epsilon;
        //     default:
        //         return Float.intBitsToFloat(value);
        // }
    }

    public int getIntValue() {
        return value;
    }
}
