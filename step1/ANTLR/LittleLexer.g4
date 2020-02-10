/*Authors: Christopher Erickson, James Jacobs, Logan Shy */

lexer grammar LittleLexer;



// Keywords

BEGIN:      'BEGIN';
BREAK:      'BREAK';
CONTINUE:   'CONTINUE';
ELSE:       'ELSE';
END:        'END';
ENDIF:      'ENDIF';  
ENDWHILE:   'ENDWHILE';
FLOAT:      'FLOAT';
FUNCTION:   'FUNCTION';
IF:         'IF';
INT:        'INT';
PROGRAM:    'PROGRAM';
READ:       'READ';
RETURN:     'RETURN';
STRING:     'STRING';
VOID:       'VOID';
WHILE:      'WHILE';
WRITE:      'WRITE';


// Separators

LPAREN:     '(';
RPAREN:     ')';
SEMI:       ';';
COMMA:      ',';

// Operators
ASSIGN:     ':=';
GT:         '>';
LT:         '<';
GE:         '>=';
LE:         '<=';
EQUAL:      '=';
NOTEQUAL:   '!=';
ADD:        '+';
SUB:        '-';
MUL:        '*';
DIV:        '/';

// Identifiers

IDENTIFIER: ([a-zA-Z]) ([a-zA-Z] | [0-9])*;

// Literals

INTLITERAL
    :   '0'
    |   ([1-9] [0-9]*)
    ;

FLOATLITERAL:   [0-9]* '.' [0-9]+;

STRINGLITERAL:   '"' (~["])* '"';

// WhiteSpace and Comments
 
WS: ('\t'|'\r'|'\n'|' ')+ -> skip;
COMMENT: '--' ~[\r\n]* -> skip;

// Fragments
