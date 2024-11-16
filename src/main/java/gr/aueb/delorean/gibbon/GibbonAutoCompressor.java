package gr.aueb.delorean.gibbon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.iki.yak.ts.compression.gorilla.BitOutput;

public class GibbonAutoCompressor {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private int storedVal = 0;
    private boolean first = true;
    private int size;
    private int cases[];
    private List<Integer> leading;
    private List<Integer> trailing;
    private Map<Integer, Integer> exponents;
    private float trailingDiff;
    private float leadingDiff;
    private int bufferCounter = 0;
    private List<Integer> bufferCounters = new LinkedList<>();
    private float max = -1000000;
    private float min = Float.MAX_VALUE;
    private float bufferValue;

    private BitOutput out;
    private int logOfError;
	private double epsilon;
	private int mode;
//	private int counter = 0;
    private int spacePowers[] = new int[32];

    public GibbonAutoCompressor(BitOutput output, double epsilon, int mode) {
        this.out = output;
        this.size = 0;
        this.epsilon = epsilon;
        this.mode = mode;
        for (int power = 30; power > -30; power--) {
        	if (Math.pow(2, power) < epsilon) {
        		this.logOfError = power;
        		break;
        	}
        }
        this.cases = new int[]{0, 0, 0};
        this.leading = new LinkedList<>();
        this.trailing = new LinkedList<>();
        this.exponents = new HashMap<>();
        this.trailingDiff = 0;
        this.leadingDiff = 0;
        for (int i=0; i<spacePowers.length; i++) {
            this.spacePowers[i] = (int) Math.pow(2, i) - 1;
        }
    }

    public void addValue(float value) {
//    	addValueLine(value);
    	addValueInternal(value);
//    	counter++;
    }

   public void addValueLine(float value) {
   	if (value > this.max) this.max = value;
       if (value < this.min) this.min = value;
       if (this.max - this.min <= this.epsilon) {
       	this.bufferValue = this.max - ((this.max - this.min) / 2);
       	this.bufferCounter++;
       } else {
    	this.bufferCounters.add(bufferCounter);
       	for (int i = 0; i < this.bufferCounter; i++) {
       		addValueInternal(this.bufferValue);
       	}
       	this.max = value;
       	this.min = value;
       	this.bufferValue = value;
       	this.bufferCounter = 1;
       }
   }

   private void clearBuffer() {
//	   if (!this.bufferCounters.isEmpty()) {
//		   System.out.println("AVERAGE: " + this.bufferCounters
//		            .stream()
//		            .mapToDouble(a -> a)
//		            .average().getAsDouble() + ", E: " + this.epsilon);
//	   }
   	if (this.bufferCounter > 0) {
       	float segmentValue = this.max - ((this.max - this.min) / 2);
   		for (int i = 0; i < this.bufferCounter; i++) {
       		addValueInternal(segmentValue);
       	}
   		this.bufferCounter = 0;
   	}
	}

	/**
     * Adds a new double value to the series. Note, values must be inserted in order.
     *
     * @param timestamp Timestamp which is inside the allowed time block (default 24 hours with millisecond precision)
     * @param value next floating point value in the series
     */
    public void addValueInternal(float value) {
        if(first) {
            writeMode(this.mode);
            writeFirst(Float.floatToRawIntBits(value));
        } else {
        	compressValue(Float.floatToRawIntBits(value));
        }
    }

    private void writeFirst(int value) {
    	first = false;
        storedVal = value;
        out.writeBits(storedVal, 32);
        size += 32;
    }

    private void writeMode(int mode) {
		switch (mode) {
		case 0:
			this.out.skipBit();
			break;
		case 1:
			this.out.writeBit();
			break;
		}
	}

    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    public void close() {
    	addValue(Float.NaN);
//    	clearBuffer();
        out.skipBit();
        out.flush();
    }

    private void compressValueLine(int value) {
        // TODO Fix already compiled into a big method
        if(value == storedVal) {
            // Write 0
        	cases[0] += 1;
        	writeCaseEqual(mode);
        } else {
        	int integerDigits = (value << 1 >>> 24) - 127;
//        	int integerDigits = ((value >> 23) & 0xff) - 127;
//        	this.exponents.put(integerDigits, this.exponents.getOrDefault(integerDigits, 0) + 1);
        	int space = 23 + this.logOfError - integerDigits;
        	space = space > 23 ? 23 : space;
        	if (space > 0) {
        		value = value >> space << space;
            	value = value | (storedVal & spacePowers[space]);
        	}
        	int xor = storedVal ^ value;

        		int leadingZeros = Integer.numberOfLeadingZeros(xor);
                int trailingZeros = Integer.numberOfTrailingZeros(xor);

                // TODO check if needed
//                if (leadingZeros + trailingZeros > 32) {
//                	trailingZeros = 32 - leadingZeros;
//                }

                // Check overflow of leading? Can't be 32!
                if(leadingZeros >= 16) {
                    leadingZeros = 15;
                }

                // Store bit '1'
//                out.writeBit();
//                size += 1;

                if(leadingZeros >= storedLeadingZeros && trailingZeros >= storedTrailingZeros
                		&& leadingZeros + trailingZeros < storedLeadingZeros + storedTrailingZeros + 6) {
                	cases[1] += 1;
                	this.trailingDiff += trailingZeros - storedTrailingZeros;
                	this.leadingDiff += leadingZeros - storedLeadingZeros;
                    writeExistingLeading(xor);
                } else {
                	cases[2] += 2;
                    writeNewLeading(xor, leadingZeros, trailingZeros);
                }

        }

        storedVal = value;
    }


    private void compressValueXorEq(int value) {
        // TODO Fix already compiled into a big method
        if(value == storedVal) {
            // Write 0
        	cases[0] += 1;
        	writeCaseEqual(mode);
//            out.skipBit();
//            size += 1;
        } else {
        	int integerDigits = (value << 1 >>> 24) - 127;
//        	if (integerDigits < -6 ) {
//        		integerDigits = -6;
//        	}
//        	int integerDigits = ((value >> 23) & 0xff) - 127;
        	this.exponents.put(integerDigits, this.exponents.getOrDefault(integerDigits, 0) + 1);
        	int space = 23 + this.logOfError - integerDigits;
        	space = space > 23 ? 23 : space;
        	if (space > 0) {
        		value = value >> space << space;
            	value = value | (storedVal & ((int) Math.pow(2, space) - 1));
        	}
        	int xor = storedVal ^ value;

        	if (xor == 0) {
        		cases[0] += 1;
            	writeCaseEqual(mode);
        	} else {
        		int leadingZeros = Integer.numberOfLeadingZeros(xor);
                int trailingZeros = Integer.numberOfTrailingZeros(xor);

                // Check overflow of leading? Can't be 32!
                if(leadingZeros >= 16) {
                    leadingZeros = 15;
                }

                // Store bit '1'
//                out.writeBit();
//                size += 1;

                if(leadingZeros >= storedLeadingZeros && trailingZeros >= storedTrailingZeros
                		&& leadingZeros + trailingZeros < storedLeadingZeros + storedTrailingZeros + 6) {
                	cases[1] += 1;
                	this.trailingDiff += trailingZeros - storedTrailingZeros;
                	this.leadingDiff += leadingZeros - storedLeadingZeros;
                    writeExistingLeading(xor);
                } else {
                	cases[2] += 2;
                    writeNewLeading(xor, leadingZeros, trailingZeros);
                }

        	}

        }

        storedVal = value;
    }

    private void compressValue(int value) {
    	// if values is within error wrt the previous value, use the previous value
    	if (Math.abs(Float.intBitsToFloat(value) - Float.intBitsToFloat(storedVal)) < epsilon) {
    		// Write 0
        	cases[0] += 1;
        	writeCaseEqual(mode);
            return;
    	}

        // TODO Fix already compiled into a big method
    	int integerDigits = (value << 1 >>> 24) - 127;
//    	if (integerDigits < -6 ) {
//    		integerDigits = -6;
//    	}
//    	int integerDigits = ((value >> 23) & 0xff) - 127;
//    	this.exponents.put(integerDigits, this.exponents.getOrDefault(integerDigits, 0) + 1);
    	int space = Math.min(23, 23 + this.logOfError - integerDigits);
//    	space = space > 23 ? 23 : space;
//    	System.out.println(value + " " + Float.intBitsToFloat(value));
    	if (space > 0) {
    		value = value >> space << space;
        	value = value | (storedVal & spacePowers[space]);
    	}
    	int xor = storedVal ^ value;

        int leadingZeros = Integer.numberOfLeadingZeros(xor);
        int trailingZeros = Integer.numberOfTrailingZeros(xor);
        // Check overflow of leading? Can't be 32!
        if(leadingZeros >= 16) {
            leadingZeros = 15;
        }
//        leadingZeros = leadingZeros % 2 == 0 ? leadingZeros : (leadingZeros - 1);

        if((leadingZeros >= storedLeadingZeros && trailingZeros >= storedTrailingZeros)
        		&& leadingZeros + trailingZeros < storedLeadingZeros + storedTrailingZeros + 9 + mode) {
        	cases[1] += 1;
//        	this.trailingDiff += trailingZeros - storedTrailingZeros;
//        	this.leadingDiff += leadingZeros - storedLeadingZeros;
//        	this.leading.add(leadingZeros);
//        	this.trailing.add(trailingZeros);
            writeCaseExistingLeading(mode);
            int significantBits = 32 - storedLeadingZeros - storedTrailingZeros;
            out.writeBits(xor >>> storedTrailingZeros, significantBits);
            size += significantBits;
        } else {
        	cases[2] += 1;
            writeCaseNewLeading(mode);
            out.writeBits(leadingZeros, 4); // Number of leading zeros in the next 4 bits
            int significantBits = 32 - leadingZeros - trailingZeros;

            out.writeBits(significantBits, 5); // Length of meaningful bits in the next 5 bits
            out.writeBits(xor >>> trailingZeros, significantBits); // Store the meaningful bits of XOR

            storedLeadingZeros = leadingZeros;
            storedTrailingZeros = trailingZeros;
            size += 4 + 5 + significantBits;
        }

        storedVal = value;
    }

    private void writeCaseEqual(int mode) {
    	if (mode == 0) {
            out.skipBit();
            size++;
    	} else {
    		out.writeBit();
            out.skipBit();
            size += 2;
    	}
	}

    private void writeCaseExistingLeading(int mode) {
    	if (mode == 0) {
    		out.writeBit();
            out.skipBit();
            size += 2;
    	} else {
            out.skipBit();
            size ++;
    	}
	}

    private void writeCaseNewLeading(int mode) {
		out.writeBit();
        out.writeBit();
        size += 2;
	}


	/**
     * If there at least as many leading zeros and as many trailing zeros as previous value, control bit = 0 (type a)
     * store the meaningful XORed value
     *
     * @param xor XOR between previous value and current
     */
    private void writeExistingLeading(int xor) {
        writeCaseExistingLeading(mode);
        int significantBits = 32 - storedLeadingZeros - storedTrailingZeros;
        out.writeBits(xor >>> storedTrailingZeros, significantBits);
        size += significantBits;
    }

    /**
     * store the length of the number of leading zeros in the next 5 bits
     * store length of the meaningful XORed value in the next 6 bits,
     * store the meaningful bits of the XORed value
     * (type b)
     *
     * @param xor XOR between previous value and current
     * @param leadingZeros New leading zeros
     * @param trailingZeros New trailing zeros
     */
    private void writeNewLeading(int xor, int leadingZeros, int trailingZeros) {
    	writeCaseNewLeading(mode);
        out.writeBits(leadingZeros, 4); // Number of leading zeros in the next 4 bits
//        out.writeBits(leadingZeros / 2, 3); // Number of leading zeros in the next 4 bits
        int significantBits = 32 - leadingZeros - trailingZeros;

        // TODO Check if needed
//        if (significantBits == 32) {
//        	out.writeBits(0, 5); // Length of meaningful bits in the next 5 bits
//        } else {
//        	System.out.println("Wrote SB: " + significantBits);
        	out.writeBits(significantBits, 5); // Length of meaningful bits in the next 5 bits
//        }
        out.writeBits(xor >>> trailingZeros, significantBits); // Store the meaningful bits of XOR

        storedLeadingZeros = leadingZeros;
        storedTrailingZeros = trailingZeros;
//        System.out.println("LEADING: " + leadingZeros + " TRAILING: " + trailingZeros + " SIGNIFICANT: " + significantBits);
        size += 4 + 5 + significantBits;
//        size += 3 + 5 + significantBits;
    }

//    public int getSize() {
//    	return size;
//    }

    public float getLeadingDiff() {
		return leadingDiff;
	}

    public float getTrailingDiff() {
		return trailingDiff;
	}

    public int[] getCases() {
		return cases;
	}

    public int getBestMode() {
    	return cases[0] > cases[1] ? 0 : 1;
    }

    public void printLeadingAndTrailing() {
    	if (this.leading.size() < 1 || this.trailing.size() < 1) {
    		return;
    	}
    	double leading = this.leading.stream().mapToDouble(e -> e).average().getAsDouble();
    	double trailing = this.trailing.stream().mapToDouble(e -> e).average().getAsDouble();
    	System.out.println(leading + "\t" + this.leading.size() + "\t" + trailing + "\t" + this.trailing.size());
    }

    public void printExponents() {
    	System.out.println(this.exponents);
    }
}
