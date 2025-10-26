package gr.aueb.delorean;

import java.io.IOException;

public interface Decompressor {
    Float readValue() throws IOException;
}
