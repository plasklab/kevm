

public class IntValue extends Value {
	int value;
	public IntValue(int v) {
		value = v;
	}
	public int intValue() {
		return value;
	}
	@Override
	public String toStringPP() {
		return String.valueOf(value);
	}
}
