grammar Grammar;

selectCommand :
    'SELECT' fields
    'FROM' tableName
    ('WHERE' conditions)?
    limitations?
    EOF
    ;

fields
    : '*'
    | fieldsList+=field (',' fieldsList+=field)*
    ;
field : STRING ;
tableName : STRING ;
limitations
    : offset limit?
    | limit offset?
    ;
offset : 'OFFSET' offsetNumber ;
offsetNumber : NUMBER ;
limit : 'LIMIT' limitNumber ;
limitNumber : NUMBER ;
conditions : conditionsList+=condition ('AND' conditionsList+=condition)* ;
condition : field OP operand | operand OP field ;
operand : quotes | NUMBER ;
quotes : '\'' STRING '\'' ;


STRING : [a-zA-Z_]+ ;
NUMBER  : '0' | '-'?[0-9]+ ;
OP : GE | GT | LE | LT | EQ ;
GE : '>=' ;
GT : '>' ;
LE : '<=' ;
LT : '<' ;
EQ : '=' ;

SPACE
    : [ \n]+ -> skip
    ;

