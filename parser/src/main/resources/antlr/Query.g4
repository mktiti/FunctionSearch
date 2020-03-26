grammar Query;

query   :   funSignature EOF;

fullName    :   FULL_PACKAGE? SIMPLE_NAME ;

templateArg :   TEMPLATE_PARAM | completeName ;
templateSignature   :   TEMPLATE_OPEN templateArg (ARG_SEP templateArg)* TEMPLATE_CLOSE ;
completeName    :   fullName templateSignature? ;

funArg  :   templateArg | PAREN_OPEN funSignature PAREN_CLOSE ;
wrappedFunArg   :   funArg | PAREN_OPEN wrappedFunArg PAREN_CLOSE ;
funSignature :   wrappedFunArg (ARG_SEP wrappedFunArg)* ARROW wrappedFunArg;

PAREN_OPEN  : '(' ;
PAREN_CLOSE  : ')' ;
ARROW   : ('->' | '=>') ;
ARG_SEP : ',' ;

fragment LOWERCASE  : [a-z] ;
fragment UPPERCASE  : [A-Z] ;
fragment NUMBER  : [0-9] ;

SIMPLE_NAME :   UPPERCASE (UPPERCASE | LOWERCASE | NUMBER)* ;

fragment PACKAGE_PART   :   LOWERCASE* ;
FULL_PACKAGE :   (PACKAGE_PART '.')+ ;

TEMPLATE_OPEN   :   '<' ;
TEMPLATE_CLOSE   :   '>' ;
TEMPLATE_PARAM  :   LOWERCASE+ ;

WHITESPACE : ' ' -> skip ;