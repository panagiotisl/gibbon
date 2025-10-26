package gr.aueb.delorean;

import java.io.IOException;

public interface Compressor {

    void addValue(float value) throws IOException;

    byte[] close() throws IOException;
}
