

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VMCore {
	public static final int MAX_STACK_SIZE = 10000;
	public static final int NUM_LINK_SLOTS = 5;
	public static final Value JUNK_VALUE = new Value();
	
	private Map<Integer, Value> memory;
	private List<Value> register;
	private int pc;
	private List<Instruction> program;

	protected VMCore() {
		memory = new HashMap<Integer, Value>();
		register = new ArrayList<Value>();
	}
	
	protected void error(String msg) {
		dump();
		throw new Error(msg);
	}
	
	protected void warn(String msg) {
		System.out.println(msg);
	}

	public void initialise(List<Instruction> code) {
		program = code;
		pc = 0;
	}
		
	public Value rd(int reg) {
		if (reg <= 0)
			error("Illegal register number: r"+reg);
		Value v = register.get(reg);
		if (v == null) {
			warn("Reading from uninitialised register: r"+reg);
			v = JUNK_VALUE;
			register.set(reg, v);
		}
		return v;
	}
	
	public void wt(int reg, Value v) {
		if (reg <= 0)
			error("Illegal register number: r"+reg);
		while (register.size() <= reg)
			register.add(null);
		register.set(reg, v);
	}

	public IntValue rdNumber(int reg) {
		Value v = rd(reg);
		if (!(v instanceof IntValue))
			error("While reading a number from r"+reg+", found: "+safePrint(v));
		return (IntValue) v;
	}

	public AddrValue rdAddrValue(int reg) {
		Value v = rd(reg);
		if (!(v instanceof AddrValue))
			error("While reading an address from r"+reg+", found: "+safePrint(v));
		return (AddrValue) v;
	}

	public Value ld(int addr) {
		if (addr < 0)
			error("Illegal memory address: "+addr);
		Value v = memory.get(addr);
		if (v == null) {
			warn("Loading from uninitialised memory: "+addr);
			v = JUNK_VALUE;
			memory.put(addr, v);
		}
		return v;
	}
	
	public void st(int addr, Value v) {
		if (addr < 0)
			error("Illegal memory address: "+addr);
		memory.put(addr, v);
	}
	
	public Instruction fetchInsn() {
		return program.get(pc++);
	}

	public void displacePC(int disp) {
		int newPC = pc + disp;
		if (newPC >= program.size())
			error("Illegal jump address: "+ newPC);
		pc = newPC;
	}

	public AddrValue getPC() {
		return new AddrValue(pc);
	}
	
	public void setPC(AddrValue addrValue) {
		pc = addrValue.getAddr();
	}
	
	
	
	/*
	public Value getOperand(int index) {
		if (index >= getNumOperands())
			error("Reading "+index+"-th argument while "+getNumOperands()+" arguments are given");
		return rdOperandNocheck(index);
	}
	*/

	private String safePrint(Value v) {
		if (v == null)
			return "null";
		else if (v == JUNK_VALUE)
			return "junk";
		else if (v instanceof IntValue)
			return v.toStringPP();
		else if (v instanceof StrValue)
			return "\""+v.toStringPP()+"\"";
		else if (v instanceof AddrValue)
			return "EXEC_ADDRESS["+((AddrValue) v).getAddr()+"]";
		else
			return "internal value "+v.toString();
	}
	public void dump() {
	}
}