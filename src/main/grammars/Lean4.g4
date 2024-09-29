grammar Lean4;

DEF: 'def';

// Fragment rules
fragment GREEK: [α-ωΑ-Ωἀ-῾] ~ [λΠΣ];
fragment COPTIC: [ϊ-ϻ];
fragment LETTER: [a-zA-Z];
fragment LETTERLIKE_SYMBOLS: [℀-⅏];
fragment ESCAPED_IDENT_PART: '«' ~[«»\r\n\t]* '»';
fragment SUBSCRIPT: [₀-₉ₐ-ₜᵢ-ᵪ];
fragment DIGIT_AND_PRIME_AND_POWER: [0-9'ⁿ];

// Lexer rules
COLON_EQ: ':=';
COLON: ':';
LBRACE: '{';
RBRACE: '}';
PIPE: '|';
LPAREN: '(';
RPAREN: ')';
COMMA: ',';


DOT : '.';
DASH : '_';
DOUBLE_LBRACKET: '«';
DOUBLE_RBRACKET: '»';
WS: [ \t\r\n]+ -> skip;

// Parser rules
file: (definition|example)+;
definition: DEF ident parameter* (type_annotation)? COLON_EQ expr;
example: 'example' parameter* (type_annotation)? COLON_EQ expr;
implicit_parameter: LBRACE (ident|DASH)+ ':' expr RBRACE;
explicit_parameter: LPAREN (ident|DASH)+ ':' expr RPAREN;
parameter: implicit_parameter | explicit_parameter;

// weird these seem have to be parser rules
fragment LETTERLIKE: LETTER | GREEK | COPTIC | LETTERLIKE_SYMBOLS;
fragment ATOMIC_IDENT_START: LETTERLIKE | DASH | ESCAPED_IDENT_PART;
fragment ATOMIC_IDENT_REST: ATOMIC_IDENT_START | DIGIT_AND_PRIME_AND_POWER | SUBSCRIPT;
ATOMIC_IDENT: ATOMIC_IDENT_START ATOMIC_IDENT_REST*;
atomic_ident : ATOMIC_IDENT;
ident: atomic_ident | ident DOT atomic_ident;

type_annotation: ':' expr;

// TODO define expr
// EXPR: 'Type*' | 'X → Y' | 'Filter X' | 'Filter Y' | 'Nat';
expr: EXPR|'sorry';

fragment EXPR_FRAG: [^}]+?;
EXPR: EXPR_FRAG+;