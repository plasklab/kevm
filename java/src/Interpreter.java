
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Interpreter extends VMCore {
	static abstract class NativeFunction implements ABI {
		String name;
		int arity;
		NativeFunction(String name, int arity) {
			this.name = name;
			this.arity = arity;
		}
		abstract Value exec(Interpreter interp);
		void invoke(Interpreter interp) {
			Value ret = exec(interp);
			interp.wt(RV_REG, ret);
			interp.setPC(interp.rdAddrValue(RA_REG));
		}
		Value getArg(Interpreter interp, int n) {
			if (n <= ARG_REG_NUM)
				return interp.rd(ARG_REG_BASE + n - 1);
			int sp = interp.rdNumber(SP_REG).intValue();
			return interp.ld(sp - (arity - n));
		}
		String getName() {
			return name;
		}
	}

	static class NativeFunction_get extends NativeFunction {
		NativeFunction_get() {
			super("get", 0);
		}
		@Override
		Value exec(Interpreter interp) {
			try {
				while (true) {
					interp.in.mark(1);
					int x = interp.in.read();
					if (!Character.isSpaceChar(x)) {
						interp.in.reset();
						break;
					}
				}
				int v = 0;
				int sign = 1;
				while (true) {
					interp.in.mark(1);
					int x = interp.in.read();
					if (Character.isDigit(x)) {
						v *= 10;
						v += x - '0';
					} else if (x == '-') {
						sign *= -1;
					} else {
						interp.in.reset();
						break;
					}
				}
				return new IntValue(sign * v);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	static class NativeFunction_put extends NativeFunction {
		NativeFunction_put() {
			super("put", 1);
		}
		@Override
		Value exec(Interpreter interp) {
			Value arg1 = getArg(interp, 1);
			interp.out.print(arg1.toStringPP());
			return new IntValue(0);
		}			
	}
	
	static List<NativeFunction> natives = new ArrayList<NativeFunction>();
		static {
		natives.add(new NativeFunction_get());
		natives.add(new NativeFunction_put());
	}
	
	public BufferedReader in;
	public PrintStream out;

	Interpreter() {
		super();
		in = new BufferedReader(new InputStreamReader(System.in));
		out = System.out;
	}
	
	void execute(ArrayList<Instruction> code) {
		initialise(code);
		interpret();
	}

	private void interpret() {
		while (true) {
			Instruction insn = fetchInsn();
			switch(insn.opcode) {
			
			case Instruction.NUMBER:
				wt(insn.r1, new IntValue(insn.intOperand));
				break;
			case Instruction.STRING:
				wt(insn.r1, new StrValue(insn.strOperand));
				break;

			case Instruction.MOVE: {
				Value v = rd(insn.r2);
				wt(insn.r1, v);
				break;
			}

			case Instruction.LOAD: {
				int addr = rdNumber(insn.r1).intValue();
				Value v = ld(addr);
				wt(insn.r2, v);
				break;
			}
			case Instruction.STORE: {
				int addr = rdNumber(insn.r1).intValue();
				Value v = rd(insn.r2);
				st(addr, v);
				break;
			}
			
			case Instruction.ADD:
			case Instruction.SUB:
			case Instruction.MUL:
			case Instruction.DIV:
			case Instruction.AND:
			case Instruction.OR:
			case Instruction.XOR:
			case Instruction.EQ:
			case Instruction.NE:
			case Instruction.LT:
			case Instruction.GT: {
				IntValue v2 = rdNumber(insn.r2);
				IntValue v3 = rdNumber(insn.r3);
				int n2 = v2.intValue(); 
				int n3 = v3.intValue();
				Value v = JUNK_VALUE;
				switch(insn.opcode) {
				case Instruction.ADD:
					v = new IntValue(n2 + n3); break;
				case Instruction.SUB:
					v = new IntValue(n2 - n3); break;
				case Instruction.MUL:
					v = new IntValue(n2 * n3); break;
				case Instruction.DIV:
					if (n3 == 0) {
						if (n2 > 0)
							v = new IntValue(Integer.MAX_VALUE);
						else if (n2 == 0)
							v = new IntValue(0);
						else
							v = new IntValue(Integer.MIN_VALUE);
					} else
						v = new IntValue(n2 / n3);
					break;
				case Instruction.AND:
					v = new IntValue(n2 & n3); break;
				case Instruction.OR:
					v = new IntValue(n2 | n3); break;
				case Instruction.XOR:
					v = new IntValue(n2 ^ n3); break;
				case Instruction.EQ:
					v = new IntValue(n2 == n3 ? 1 : 0); break;
				case Instruction.NE:
					v = new IntValue(n2 != n3 ? 1 : 0); break;
				case Instruction.LT:
					v = new IntValue(n2 < n3 ? 1 : 0); break;
				case Instruction.GT:
					v = new IntValue(n2 > n3 ? 1 : 0); break;
				default:
					error("Internal error: unknown binary operator");
				}
				wt(insn.r1, v);
				break;
			}
			case Instruction.JMP:
				displacePC(insn.intOperand);
				break;
			case Instruction.JMPT:
			case Instruction.JMPF: {
				IntValue cv = rdNumber(insn.r1);
				int cn = cv.intValue();
				if (insn.opcode == Instruction.JMPT) {
					if (cn != 0)
						displacePC(insn.intOperand);
				} else {
					if (cn == 0)
						displacePC(insn.intOperand);
				}
				break;
			}
			case Instruction.CALL: {
				AddrValue retaddr = getPC();
				wt(insn.r1, retaddr);
				displacePC(insn.intOperand);
				int pc = getPC().addr;
				if (pc < 0) {
					NativeFunction f = natives.get(-1 - pc);
					if (f == null)
						error("calling an unkown function");
					f.invoke(this);
				}
				break;
			}
			case Instruction.RET: {
				AddrValue retaddr = rdAddrValue(insn.r1);
				setPC(retaddr);
				break;
			}
			case Instruction.EXIT:
				return;
			default:
				error("unknown instruction: "+insn.opcode);
				break;
			}
		}
	}
	
	public static void main(String[] args) {
		for (int i = 0; i < natives.size(); i++) {
			NativeFunction f = natives.get(i);
			Loader.registerNative(f.name, -1 - i);
		}
		Loader loader = Loader.load(args);
		Interpreter interp = new Interpreter();
		interp.execute(loader.getInsns());
	}
}
