GRAMMAR=Asm

ANTLRPATH=../lib/antlr-4.5.3-complete.jar
ANTLR4=java -jar $(ANTLRPATH)
SRC=src
ANTLROUT=antlr-generated
CLASSES=antlr-generated/bin

all: $(CLASSES)/AsmParser.class

$(ANTLROUT)/$(SRC)/AsmParser.java: $(SRC)/Asm.g4
	$(ANTLR4) -no-listener -visitor -o $(ANTLROUT) $<

$(CLASSES)/AsmParser.class: $(ANTLROUT)/$(SRC)/AsmParser.java
	mkdir -p $(CLASSES)
	javac -cp $(ANTLRPATH) $(ANTLROUT)/$(SRC)/*.java -d $(CLASSES)

