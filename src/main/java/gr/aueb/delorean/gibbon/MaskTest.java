package gr.aueb.delorean.gibbon;

public class MaskTest {

	public static void main(String[] args) {
		
		int value = 1012439176;
		int storedVal = -1114803260;
		int space = 25;
		System.out.println(Integer.toString(storedVal, 2));
		System.out.println(Integer.toString(value, 2));
		value = value >> space << space;
		System.out.println(Integer.toString(value, 2));
    	value = value | (storedVal & ((int) Math.pow(2, space) - 1));
    	System.out.println(Integer.toString(value, 2));
    	int xor = storedVal ^ value;
		System.out.println(Integer.toString(xor, 2));
		
	}
	
}
