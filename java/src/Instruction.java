

public class Instruction {
	public static final int MOVE = 1;
	public static final int ADD = 2;
	public static final int SUB = 3;
	public static final int MUL = 4;
	public static final int DIV = 5;
	public static final int AND = 6;
	public static final int OR = 7;
	public static final int XOR = 8;
	public static final int EQ = 9;
	public static final int NE = 10;
	public static final int LT = 11;
	public static final int GT = 12;
	public static final int NUMBER = 30;
	public static final int STRING = 31;
	public static final int JMP = 40;
	public static final int JMPT = 41;
	public static final int JMPF = 42;
	public static final int NATIVE = 50;
	public static final int RET = 51;
	public static final int ENTER = 52;
	public static final int GETA = 53;
	public static final int SETA = 54;
	public static final int PUSH = 60;
	public static final int LOAD = 70;
	public static final int STORE = 71;
	public static final int CALL = 72;
	public static final int EXIT = 73;
	
	public static final int NO = 0;
	public static final int REG = 1;
	public static final int NUM = 2;
	public static final int STR = 3;
	public static final int DISP = 4;
	public static final int PINT = 5;
	public static final int SYM = 6;
	
	public static final int MAX_OPERANDS = 3;

	public static class InsnSpec {
		public String name;
		public int opcode;
		public int[] operands;
		InsnSpec(String name, int opcode, int a1, int a2, int a3) {
			this.name = name;
			this.opcode = opcode;
			this.operands = new int[]{a1, a2, a3};
		}
	}
	public static InsnSpec[] insns = new InsnSpec[] {
		new InsnSpec("move", MOVE, REG, REG, 0),
		new InsnSpec("add",  ADD,  REG, REG, REG),
		new InsnSpec("sub",  SUB,  REG, REG, REG),
		new InsnSpec("mul",  MUL,  REG, REG, REG),
		new InsnSpec("div",  DIV,  REG, REG, REG),
		new InsnSpec("and",  AND,  REG, REG, REG),
		new InsnSpec("or",   OR,   REG, REG, REG),
		new InsnSpec("xor",  XOR,  REG, REG, REG),
		new InsnSpec("eq",   EQ,   REG, REG, REG),
		new InsnSpec("ne",   NE,   REG, REG, REG),
		new InsnSpec("lt",   LT,   REG, REG, REG),
		new InsnSpec("gt",   GT,   REG, REG, REG),
		new InsnSpec("number", NUMBER, REG, NUM, 0),
		new InsnSpec("string", STRING, REG, STR, 0),
		new InsnSpec("jmp",  JMP,  DISP, 0, 0),
		new InsnSpec("jmpt", JMPT, REG, DISP, 0),
		new InsnSpec("jmpf", JMPF, REG, DISP, 0),
		new InsnSpec("ret",  RET,  REG, 0, 0),
		new InsnSpec("enter", ENTER, PINT, 0, 0),
		new InsnSpec("geta", GETA, REG, 0, 0),
		new InsnSpec("seta", SETA, REG, 0, 0),
		new InsnSpec("push", PUSH, REG, 0, 0),
		new InsnSpec("load", LOAD, REG, REG, 0),
		new InsnSpec("store", STORE, REG,REG, 0),
		new InsnSpec("call", CALL, REG, DISP, 0),
		new InsnSpec("exit", EXIT, 0, 0, 0),
	};
	public static InsnSpec lookupInsn(String name) {
		for (InsnSpec i: insns) {
			if (i.name.equals(name))
				return i;
		}
		return null;
	}
	public static InsnSpec lookupInsnByOpcode(int opcode) {
		for (InsnSpec i: insns) {
			if (i.opcode == opcode)
				return i;
		}
		return null;
	}
	
	public int opcode;
	public int r1, r2, r3;
	public int intOperand;
	public String strOperand;
	String unresolvedLabel;
	public Instruction(InsnSpec spec, Object[] operands) {
		opcode = spec.opcode;
		for (int i = 0; i < MAX_OPERANDS; i++) {
			switch(spec.operands[i]) {
			case NO:
				break;
			case REG: {
				int r = ((Integer) operands[i]).intValue();
				switch (i) {
				case 0: r1 = r; break;
				case 1: r2 = r; break;
				case 2: r3 = r; break;
				default: throw new Error("internal error: too many register operands");
				}
				break;
			}
			case NUM:
			case PINT:
				intOperand = ((Integer) operands[i]).intValue();
				break;
			case STR:
			case SYM:
				strOperand = (String) operands[i];
				break;
			case DISP:
				if (operands[i] instanceof String)
					unresolvedLabel = (String) operands[i];
				else
					intOperand = ((Integer) operands[i]).intValue();
				break;
			}
		}
	}
	public String getUnresolvedLabel() {
		return unresolvedLabel;
	}
	public void setLabelDisplacement(int disp) {
		if (unresolvedLabel == null)
			throw new Error("internal error: no unresolved label");
		unresolvedLabel = null;
		intOperand = disp;
	}
	
	@Override
	public String toString() {
		InsnSpec spec = lookupInsnByOpcode(opcode);
		String s = spec.name;
		for (int i = 0; i < MAX_OPERANDS; i++) {
			switch(spec.operands[i]) {
			case NO: break;
			case REG:
				switch(i) {
				case 0: s += " "+r1; break;
				case 1: s += ", "+r2; break;
				case 2: s += ", "+r3; break;
				}
				break;
			case NUM:
			case PINT:
				s += (i >= 1 ? ", " : " ")+intOperand;
				break;
			case STR:
				s += (i >= 1 ? ", \"" : " \"")+strOperand+"\"";
				break;
			case SYM:
				s += (i >= 1 ? ", " : " ")+strOperand;
				break;
			case DISP:
				if (unresolvedLabel == null)
					s += (i >= 1 ? ", " : " ")+intOperand;
				else
					s += (i >= 1 ? ", " : " ")+strOperand;
				break;
			}
		}
		return s;
	}
}
