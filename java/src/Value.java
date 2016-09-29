

public class Value {
	public static final int VMINTERNAL = 100;
	public static final int NUMBER = 1;
	public static final int STRING = 2;
	
	public String toStringPP() {
		throw new Error("internal error: printing an internal value: "+toString());
	}
}
