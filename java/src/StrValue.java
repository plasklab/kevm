

public class StrValue extends Value {
	String value;
	public String getString() {
		return value;
	}
	public StrValue(String v) {
		value = v;
	}
	@Override
	public String toStringPP() {
		return value;
	}
}
