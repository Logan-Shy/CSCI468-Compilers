/*Authors: Christopher Erickson, James Jacobs, Logan Shy */

grammar Little;

/* Parser Rules */////////////////////// 
//Program
program:    PROGRAM id BEGIN pgm_body END;
id:         IDENTIFIER;
pgm_body:   decl func_declarations;

decl
    :   string_decl decl
    |   var_decl decl 
    |   //empty
    ;

//Global String Declaration
string_decl: STRING id ASSIGN str SEMI;
str:         STRINGLITERAL;

//Variable Declarations
var_decl:   var_type id_list SEMI;
var_type:   FLOAT | INT;
any_type:   var_type | VOID;
id_list:    id id_tail;
id_tail
    :    COMMA id id_tail 
    | //empty
    ;

//Function Parameter List
param_decl_list
    :   param_decl param_decl_tail 
    | //empty
    ;

param_decl:     var_type id;
param_decl_tail
    :   COMMA param_decl param_decl_tail
    | //empty
    ;

//Function Declarations
func_declarations
    :   func_decl func_declarations
    | //empty
    ;

func_decl:   FUNCTION any_type id LPAREN param_decl_list RPAREN BEGIN func_body END;
func_body:  decl stmt_list;

//Statement List
stmt_list
    :   stmt stmt_list
    | //empty
    ;

stmt
    :   base_stmt
    |   if_stmt
    |   while_stmt
    ;

base_stmt
    :   assign_stmt
    |   read_stmt
    |   write_stmt
    |   return_stmt
    ;

//Basic Statements
assign_stmt:    assign_expr SEMI;
assign_expr:    id ASSIGN expr;
read_stmt:      READ LPAREN id_list RPAREN SEMI;
write_stmt:     WRITE LPAREN id_list RPAREN SEMI;
return_stmt:    RETURN expr SEMI;

//Expressions
expr:       expr_prefix factor;
expr_prefix
    :   expr_prefix factor addop
    |   //empty
    ;
factor:     factor_prefix postfix_expr;
factor_prefix
    :   factor_prefix postfix_expr mulop
    |   //empty
    ;
postfix_expr
    :   primary
    |   call_expr
    ;
call_expr:  id LPAREN expr_list RPAREN;
expr_list
    :   expr expr_list_tail
    |   //empty
    ;
expr_list_tail
    :   COMMA expr expr_list_tail 
    |   //empty
    ;
primary
    :   LPAREN expr RPAREN
    |   id
    |   INTLITERAL
    |   FLOATLITERAL
    ;
addop
    :   ADD
    |   SUB
    ;
mulop
    :   MUL
    |   DIV
    ;

//Complex Statements and Condition
if_stmt:    IF LPAREN cond RPAREN decl stmt_list else_part ENDIF;
else_part
    :   ELSE decl stmt_list
    |   //empty
    ;
cond:   expr compop expr;
compop
    :   LT
    |   GT
    |   EQUAL
    |   NOTEQUAL
    |   LE
    |   GE
    ;

//While Statements
while_stmt: WHILE LPAREN cond RPAREN decl stmt_list ENDWHILE;



/* Lexer Rules */////////////////////// 
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
