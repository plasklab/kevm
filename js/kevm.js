
function InsnDef(op, at1, at2, at3) {
    this.op = op;
    this.at = [at1, at2, at3];
    return this;
}
InsnDef.REG = 1;
InsnDef.NUM = 2;
InsnDef.STR = 3;
InsnDef.DISP = 4;
InsnDef.UINT = 5;
InsnDef.SYM = 6;
var insnDefs = {
    number : new InsnDef("number", InsnDef.REG, InsnDef.NUM),
    string : new InsnDef("string", InsnDef.REG, InsnDef.STR),
    move   : new InsnDef("move", InsnDef.REG, InsnDef.REG),
    load   : new InsnDef("load", InsnDef.REG, InsnDef.REG),
    store  : new InsnDef("store", InsnDef.REG, InsnDef.REG),
    add    : new InsnDef("add", InsnDef.REG, InsnDef.REG, InsnDef.REG),
    sub    : new InsnDef("sub", InsnDef.REG, InsnDef.REG, InsnDef.REG),
    mul    : new InsnDef("mul", InsnDef.REG, InsnDef.REG, InsnDef.REG),
    div    : new InsnDef("div", InsnDef.REG, InsnDef.REG, InsnDef.REG),
    and    : new InsnDef("and", InsnDef.REG, InsnDef.REG, InsnDef.REG),
    or     : new InsnDef("or", InsnDef.REG, InsnDef.REG, InsnDef.REG),
    xor    : new InsnDef("xor", InsnDef.REG, InsnDef.REG, InsnDef.REG),
    eq     : new InsnDef("eq", InsnDef.REG, InsnDef.REG, InsnDef.REG),
    ne     : new InsnDef("ne", InsnDef.REG, InsnDef.REG, InsnDef.REG),
    lt     : new InsnDef("lt", InsnDef.REG, InsnDef.REG, InsnDef.REG),
    gt     : new InsnDef("gt", InsnDef.REG, InsnDef.REG, InsnDef.REG),
    jmp    : new InsnDef("jmp", InsnDef.DISP),
    jmpt   : new InsnDef("jmpt", InsnDef.REG, InsnDef.DISP),
    jmpf   : new InsnDef("jmpf", InsnDef.REG, InsnDef.DISP),
    call   : new InsnDef("call", InsnDef.REG, InsnDef.DISP),
    ret    : new InsnDef("ret", InsnDef.REG),
    exit   : new InsnDef("exit")
};

function Insn(tokens, lineNo) {
    var regnum, match;
    this.op = tokens[0];
    this.def = insnDefs[this.op];
    this.lineNo = lineNo;
    for (var i = 0; i < this.def.at.length; i++) {
        var operandType = this.def.at[i];
        var token = tokens[i + 1];
        switch(operandType) {
        case InsnDef.REG:
            if ((match = token.match(/r(\d+)/)))
                regnum = parseInt(match[1]);
            else {
                this.error = true;
                return this;
            }
            switch(i) {
            case 0: this.r1 = regnum; break;
            case 1: this.r2 = regnum; break;
            case 2: this.r3 = regnum; break;
            }
            break;
        case InsnDef.NUM:
            this.intOperand = parseInt(token);
            break;
        case InsnDef.STR:
            this.strOperand = token;
            break;
        case InsnDef.DISP:
            if (token.match(/^[0-9]/)) {
                this.intOperand = parseInt(token);
            } else {
                this.unresolvedLabel = token;
            }
            break;
        case InsnDef.UINT:
            this.intOperand = parseInt(token);
            break;
        case InsnDef.SYM:
            this.strOperand = token;
            break;
        }
    }
    return this;
}
Insn.prototype.getR1 = function() { return this.r1; };
Insn.prototype.getR2 = function() { return this.r2; };
Insn.prototype.getR3 = function() { return this.r3; };
Insn.prototype.getInt = function() { return this.intOperand; };
Insn.prototype.getStr = function() { return this.strOperand; };
Insn.prototype.toString = function() {
    var str = this.op;
    var first = true;
    var c = function() {if(first){first=false;return"";}else{return ",";}};
    if (this.r1 != undefined) str += c()+" r"+this.r1;
    if (this.r2 != undefined) str += c()+" r"+this.r2;
    if (this.r3 != undefined) str += c()+" r"+this.r3;
    if (this.strOperand != undefined) str += c()+" "+this.strOperand;
    if (this.intOperand != undefined) str += c()+" "+this.intOperand;
    return str;
};


function VM(ui) {
    this.pc = 0;
    this.program = null;
    this.registers = [];
    this.memory = [];
    this.currentCont = undefined;
    this.executedInstructions = 0;
    this.ui = ui;
    this.natives = {
	"put" : function(vm){
            vm.getUI().output(vm.rd(1));
            return false;
	},
	"get" : function(vm) {
            vm.getUI().activateInput();
            var cont = function() {
		var str = vm.getUI().input();
		if (str.length == 0)
                    return cont;
		var value = parseInt(str);
		vm.wt(1, value);
		vm.getUI().deactivateInput();
		return false;
            };
            return cont;
	}
    };
    return this;
}
VM.prototype.setup = function(code) {
    this.pc = 0;
    this.program = code;
    this.currentCont = undefined;
    this.executedInstructions = 0;
    this.ui.initialise();
};
VM.prototype.rd = function(reg) {
    return this.registers[reg];
};
VM.prototype.wt = function(reg, val) {
    this.registers[reg] = val;
};
VM.prototype.ld = function(addr) {
    return this.memory[addr];
};
VM.prototype.st = function(addr, val) {
    this.memory[addr] = val;
};
VM.prototype.getUI = function() {
    return this.ui;
};
VM.prototype.parse = function (code, error) {
    var lines = code.split("\n"),
    line, lineNo, i, pc, label, match, tokens, insn,
    labels = {},
    insns = [];

    var ptnLabel = "\\w+(?=:)",
        reLabel = new RegExp(ptnLabel, "g"),
        ptnReg = "r\\d+",
        ptnNum = "[+-]?\\d+",
        ptnSym = "[a-zA-Z_]\\w*",
        ptnSChar = "(?:[^\\\"\\\\]|\\\"|\\\\)",
        ptnStr = "\""+ptnSChar+"*\"",
        ptnA = "("+ptnReg+"|"+ptnNum+"|"+ptnSym+"|"+ptnStr+")",
        ptnSep = "\\s*,\\s*",
        ptnAs = ptnA+"(?:"+ptnSep+ptnA+"(?:"+ptnSep+ptnA+")?)?",
        ptnInsn = "("+ptnSym+")(?:\\s+"+ptnAs+")?\\s*(?:;.*)?$",
        reInsn = new RegExp(ptnInsn),
        ptnValidLine = "^\\s*("+ptnLabel+":\\s*)*("+ptnInsn+")?\\s*(:?;.*)?$",
        reValidLine = new RegExp(ptnValidLine);
    
    for (lineNo = 0; lineNo < lines.length; lineNo++) {
        line = lines[lineNo];
        if (!line.match(reValidLine)) {
            error(lineNo);
            return false;
        }
        match = line.match(reLabel);
        if (match) {
            for (i = 0; i < match.length; i++) {
                label = match[i],
                labels[label] = insns.length;
            }
        }
        match = line.match(reInsn);
        if (match) {
            tokens = [];
            for (i = 1; i < match.length; i++)
                if (match[i])
                    tokens.push(match[i]);
            insn = new Insn(tokens, lineNo);
            if (insn.error) {
                error(lineNo);
                return false;
            }
            insns.push(insn);
        }
    }
    
    for (pc = 0; pc < insns.length; pc++) {
        insn = insns[pc];
        label = insn.unresolvedLabel;
        if (label) {
            target = labels[label];
            if (target == undefined) {
		if (this.natives[label] == undefined) {
                    error(insn.lineNo);
                    return false;
		} else {
		    insn.strOperand = label
		}
	    } else {
		insn.intOperand = target - pc - 1;
	    }
        }
    }
    return insns;
};
VM.prototype.load = function (str, error) {
    var code = this.parse(str, error);
    if (code) {
        this.setup(code);
        this.dump();
    }
};
VM.prototype.callNative = function(fun) {
    var i;
    var vm  = this;
    var cont = function(nativeCont) {
	if (nativeCont == false) {
	    // No continuation; the execution of the native
	    // function has been completed.
	    //    => restore frame
	    vm.pc = vm.rd(7);
	    return true;
	} else {
	    return function(arg) {
		return cont(nativeCont(arg));
	    }
	}
    };
    return cont(fun(this));
};
VM.prototype.run = function() {
    var vm = this;
    if (vm.currentCont == false)
        return;
    if (vm.currentCont != undefined) {
        vm.currentCont();
    } else {
        var cont = function(vmCont) {
            switch (vmCont) {
            case true:  // execute next instruction
                vm.currentCont = function() {
                    cont(vm.interpStep());
                };
                vm.ui.completeInstruction();
                break;
            case false: // execution complete
                vm.currentCont = false;
                break;
            default: // some specific continuation
                vm.currentCont = function() {
                    cont(vmCont(true));
                };
            }
            vm.dump();
        };
        cont(this.interpStep());
    }
};
/**
 * One step execution
 * @return false if the execution complete
 *         true  if the instruction complete
 *         function (continuation) if there is a continuation of a native function to be called later
 */
VM.prototype.interpStep = function() {
    var insn = this.program[this.pc++];
    var vm = this;
    var rd1 = function() { return vm.rd(insn.getR1()); };
    var rd2 = function() { return vm.rd(insn.getR2()); };
    var rd3 = function() { return vm.rd(insn.getR3()); };
    var wt1 = function(v) { return vm.wt(insn.getR1(), v); };
    var wt2 = function(v) { return vm.wt(insn.getR2(), v); };
    this.executedInstructions++;
    switch(insn.op) {
    case "number":
        wt1(insn.getInt());
        break;
    case "string":
        wt1(insn.getStr());
        break;

    case "move": wt1(rd2()); break;

    case "load":
	wt2(vm.ld(rd1()));
	break;
    case "store":
	vm.st(rd1(), rd2());
	break;
	
    case "add":  wt1(rd2() + rd3()); break;
    case "sub":  wt1(rd2() - rd3()); break;
    case "mul":  wt1(rd2() * rd3()); break;
    case "div":  wt1(Math.floor(rd2() / rd3())); break;
    case "and":  wt1(rd2() & rd3()); break;
    case "or":   wt1(rd2() | rd3()); break;
    case "xor":  wt1(rd2() ^ rd3()); break;
    case "eq":   wt1(rd2() == rd3() ? 1 : 0); break;
    case "ne":   wt1(rd2() != rd3() ? 1 : 0); break;
    case "lt":   wt1(rd2() < rd3() ? 1 : 0); break;
    case "gt":   wt1(rd2() > rd3() ? 1 : 0); break;
	
    case "jmp":
        this.pc += insn.getInt();
        break;
    case "jmpt":
        if (rd1() != 0) this.pc += insn.getInt();
        break;
    case "jmpf":
        if (rd1() == 0) this.pc += insn.getInt();
        break;
    case "call":
	wt1(this.pc);
	if (insn.getInt())
	    this.pc += insn.getInt();
	else {
	    this.pc--;
	    return this.callNative(this.natives[insn.getStr()]);
	}
	break;
    case "ret":
	this.pc = rd1();
        break;
    case "exit":
	this.pc--;
	return false;
    }
    return true;
};
VM.prototype.dump = function () {
    this.ui.showVmState(this);
};
function UI(control) {
    this.cInputText    = window.document.getElementById(control.inputText);
    this.cInputButton  = window.document.getElementById(control.inputButton);
    this.cOutputText   = window.document.getElementById(control.outputText);
    this.cStepCheckbox = window.document.getElementById(control.stepCheckbox);
    this.cStepCount    = window.document.getElementById(control.stepCount);
    this.cProgram      = window.document.getElementById(control.program);
    this.cRegister     = window.document.getElementById(control.register);
    this.cMemory       = window.document.getElementById(control.memory);
    this.currentPC    = -1;
};
UI.prototype.clearChildren = function(elm) {
    for (var i = elm.childNodes.length - 1; i >= 0; i--) {
        if (elm.childNodes[i].getAttribute("class") == "dynamic")
            elm.removeChild(elm.childNodes[i]);
    }
};
UI.prototype.appendTR2 = function(tbl, td1, td2) {
    var tr = document.createElement("tr");
    tr.setAttribute("class", "dynamic");
    tr.appendChild(td1);
    tr.appendChild(td2);
    tbl.appendChild(tr);
};
UI.prototype.createTD = function(id, text) {
    var td = document.createElement("td");
    td.setAttribute("id", id);
    td.innerHTML = text;
    return td;
};
UI.prototype.showProgram = function (vm) {
    var i, tda, tdi, tr, td;
    var program = vm.program;
    var pc = vm.pc;
    if (!this.cProgram)
	return;
    this.clearChildren(this.cProgram);
    if (Array.isArray(program)) {
	for (i = 0; i < program.length; i++) {
            tda = this.createTD("codeAddr"+i, i);
            tda.setAttribute("class", "codeAddr");
            tdi = this.createTD("codeInsn"+i, program[i]);
            if (i == pc)
                tdi.setAttribute("class", "codeExec");
            else
                tdi.setAttribute("class", "codeNormal");
            this.appendTR2(this.cProgram, tda, tdi);
        }
    } else {
        document.createElement("tr");
        tr.setAttribute("class", "dynamic");
        td = this.createTD("codeInsn", insns);
        tr.appendChild(td);
        this.cProgram.appendChild(tr);
    }
};

UI.prototype.showRegisters = function(vm) {
    var i, tdn, tdv;
    var registers = vm.registers;
    if (!this.cRegister)
	return;
    this.clearChildren(this.cRegister);
    for (i = 1; i < registers.length; i++) {
        tdn = this.createTD("regName"+i, "r"+i);
        tdv = this.createTD("regValue"+i, registers[i]);
        this.appendTR2(this.cRegister, tdn, tdv);
    }
    while (i < 20) {
        tdn = this.createTD("regName"+i, "r"+i);
        tdv = this.createTD("regValue"+i, registers[i]);
        this.appendTR2(this.cRegister, tdn, tdv);
        i++;
    }
};
UI.prototype.showMemory = function(vm) {
    var addr, tdi, tdv;
    var memory = vm.memory;
    if (!this.cMemory)
	return;
    this.clearChildren(this.cMemory);
    for (addr in memory) {
        tdi = this.createTD("opIndex"+addr, addr);
        tdv = this.createTD("opValue"+addr, memory[addr]);
        this.appendTR2(this.cMemory, tdi, tdv);
    }
};

UI.prototype.showVmState = function(vm) {
    this.showProgram(vm);
    this.showRegisters(vm);
    this.showMemory(vm);
    if (this.cStepCount) {
	this.cStepCount.innerHTML = vm.executedInstructions;
    }
};

UI.prototype.activateInput = function() {
    this.cInputButton.disabled = false;
};
UI.prototype.deactivateInput = function() {
    this.cInputButton.disabled = true;
};
UI.prototype.inputChar = function() {
    var str = this.cInputText.value;
    if (str.length == 0)
        return "";
    var char = str[0];
    this.cInputText.value = str.substring(1, str.length);
    return char;
};
UI.prototype.input = function() {	
    var str = this.cInputText.value;
    this.cInputText.value = "";
    return str;
};
UI.prototype.output = function(str) {
    this.cOutputText.value += str;
};
UI.prototype.initialise = function() {
    this.deactivateInput();
    this.cOutputText.value = "";
};
UI.prototype.completeInstruction = function (isProgramEnd) {
    if (!isProgramEnd && !this.cStepCheckbox.checked)
        setTimeout(run, 0);
};
UI.prototype.native = {
    get: function (vm) {
        vm.getUI().activateInput();
        var cont = function() {
            var str = vm.getUI().inputChar();
            if (str.length == 0)
                return cont;
            var char = str.charCodeAt(0);
            vm.wtA(char);
            vm.getUI().deactivateInput();
            return false;
        };
        return cont;
    },
    getd: function (vm) {
    },
    put: function (vm) {
        var str = "";
        for (var i = 0; i < vm.getNumArgs(); i++) {
            var arg = vm.getArg(i);
            str += arg;
        }
        vm.getUI().output(str);
        return false;
    },
    putd: function (vm) {
        vm.getUI().output(vm.getArg(0));
        return false;
    }
};

