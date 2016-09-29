grammar Asm;

program: line*
	;

line: label? instruction? EOL
	;

label: LABEL_DEF
	;

instruction: rator rands
	;

rator: SYMBOL
	;

rands: (rand (',' rand)*)?
	;

rand: r=INT
	| r=STRING
	| r=SYMBOL
	| r=REGISTER
	;

REGISTER: 'r' DIGIT+;
LABEL_DEF: ALPHA ALPHADIGIT* ':';
INT: ('+'|'-')? DIGIT+;
STRING: '"' CHARINSTR* '"';
SYMBOL: ALPHA ALPHADIGIT*;
EOL: (';'~[\n]*)? '\n';
WS: [ \t\r] -> skip;

fragment ALPHA: [a-zA-Z];
fragment DIGIT: [0-9];
fragment ALPHADIGIT: ALPHA | DIGIT;
fragment NOESCAPECHAR: ~[\\"];
fragment CHARINSTR: NOESCAPECHAR | '\\\\' | '\\"';