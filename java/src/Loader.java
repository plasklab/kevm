

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

public class Loader extends AsmBaseVisitor<Void> {
	public static final int MAX_OPERANDS = 3;
	Object[] operands = new Object[MAX_OPERANDS];
	int operandIndex;
	Instruction.InsnSpec expectedInsn;
	ArrayList<Instruction> insns;
	int maxReg;
	public ArrayList<Instruction> getInsns() {
		return insns;
	}

	static Map<String, Integer> natives = new HashMap<String, Integer>();
	HashMap<String, Integer> labels;
	
	
	void error(ParserRuleContext ctx, String msg) {
		throw new Error("error in line "+ctx.getStart().getLine()+
				": "+msg+": "+ctx.getText());
	}
	void error(String msg) {
		throw new Error(msg);
	}

	void resolveLabels() {
		for (int i = 0; i < insns.size(); i++) {
			Instruction insn = insns.get(i);
			String lbl = insn.getUnresolvedLabel();
			if (lbl != null) {
				Integer loc = labels.get(lbl);
				if (loc == null) {
					loc = natives.get(lbl);
					if (loc == null)
						error("error: undefined label: "+lbl);
				}
				insn.setLabelDisplacement(loc.intValue() - (i + 1));
			}
		}
	}

	@Override
	public Void visitProgram(AsmParser.ProgramContext ctx) {
		insns = new ArrayList<Instruction>();
		labels = new HashMap<String, Integer>();
		visitChildren(ctx);
		resolveLabels();
		return null;
	}

	@Override
	public Void visitInstruction(AsmParser.InstructionContext ctx) {
		String name = ctx.rator().getText();
		operandIndex = 0;
		expectedInsn = Instruction.lookupInsn(name);
		if (expectedInsn == null)
			error(ctx, "unknown opcode");
		visit(ctx.rands());
		Instruction insn = new Instruction(expectedInsn, operands);
		insns.add(insn);
		return null;
	}

	@Override
	public Void visitLabel(AsmParser.LabelContext ctx) {
		String label = ctx.getText().substring(0, ctx.getText().length() - 1);
		if (labels.containsKey(label))
			error(ctx, "double definition of label");
		labels.put(label, new Integer(insns.size()));
		return null;
	}

	@Override
	public Void visitRand(AsmParser.RandContext ctx) {
		int expected = expectedInsn.operands[operandIndex];
		if (operandIndex >= MAX_OPERANDS || expected == Instruction.NO)
			error(ctx, "too many operands");

		if (ctx.r.getType() == AsmParser.INT) {
			Integer val = new Integer(ctx.getText());
			if (expected != Instruction.NUM && expected != Instruction.REG &&
				expected != Instruction.PINT && expected != Instruction.DISP)
				error(ctx, "unexpected integer");
			if (expected == Instruction.PINT) {
				if (val.intValue() < 0)
					error(ctx, "unexpected negative number");
			}
			operands[operandIndex++] = val;
		} else if (ctx.r.getType() == AsmParser.STRING) {
			if (expected != Instruction.STR)
				error(ctx, "unexpected string");
			operands[operandIndex++] = ctx.getText().substring(1, ctx.getText().length() - 1);
		} else if (ctx.r.getType() == AsmParser.SYMBOL) {
			if (expected != Instruction.SYM &&
				expected != Instruction.DISP)
				error(ctx, "unexpected use of label or function name");
			operands[operandIndex++] = ctx.getText();
		} else if (ctx.r.getType() == AsmParser.REGISTER) {
			Integer val = new Integer(ctx.getText().substring(1));
			if (expected == Instruction.REG) {
				if (maxReg < val.intValue())
					maxReg = val.intValue();
				if (val.intValue() < 0)
					error(ctx, "register number should be >= 0");
			} else 
				error(ctx, "unexpected use of register");
			operands[operandIndex++] = val;
		}
		return null;
	}

	public void printInsns() {
		for (Instruction insn: insns)
			System.out.println(insn.toString());
	}
	
	public static void registerNative(String name, int addr) {
		System.out.println(name);
		natives.put(name, addr);
	}

	public static Loader load(InputStream is) {
		ANTLRInputStream input;
		try {
			input = new ANTLRInputStream(is);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		AsmLexer lexer = new AsmLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		AsmParser parser = new AsmParser(tokens);
		ParseTree tree = parser.program();
		Loader loader = new Loader();
		loader.visit(tree);
		loader.printInsns();
		return loader;
	}
	
	@SuppressWarnings("resource")
	public static Loader load(String[] args) {
		InputStream is;
		try {
			if (args.length > 0)
				is = new FileInputStream(args[0]);
			else
				is = System.in;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return load(is);
	}
		
	public static Loader load(String code) {
		InputStream ss = new ByteArrayInputStream(code.getBytes());
		return load(ss);
	}
	public static Loader load(byte[] code) {
		InputStream ss = new ByteArrayInputStream(code);
		return load(ss);
	}
	
	public static void main(String[] args) {
		Loader loader = Loader.load(args);
		loader.printInsns();
	}
}
