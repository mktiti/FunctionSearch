grammar Query;

query   :   funSignature EOF;

fullName    :   FULL_PACKAGE? SIMPLE_NAME ;

templateSignature   :   TEMPLATE_OPEN completeName (ARG_SEP completeName)* TEMPLATE_CLOSE ;
completeName    :   fullName templateSignature? ;

funArg  :   completeName | PAREN_OPEN funSignature PAREN_CLOSE ;
wrappedFunArg   :   funArg | PAREN_OPEN wrappedFunArg PAREN_CLOSE ;
inArgs  :   wrappedFunArg (ARG_SEP wrappedFunArg)* | EMPTY_ARG  ;
outArg  :   wrappedFunArg | EMPTY_ARG  ;
funSignature :  inArgs ARROW outArg;

EMPTY_ARG  : PAREN_OPEN PAREN_CLOSE ;
PAREN_OPEN  : '(' ;
PAREN_CLOSE  : ')' ;
ARROW   : ('->' | '=>') ;
ARG_SEP : ',' ;

SIMPLE_NAME :   JavaLetterOrDigit+ ;

fragment PACKAGE_PART   :   JavaLetterOrDigit* ;
FULL_PACKAGE :   (PACKAGE_PART '.')+ ;

TEMPLATE_OPEN   :   '<' ;
TEMPLATE_CLOSE   :   '>' ;

JavaLetterOrDigit
	:	[a-zA-Z0-9$_] // these are the "java letters or digits" below 0x7F
	|	// covers all characters above 0x7F which are not a surrogate
		~[\u0000-\u007F\uD800-\uDBFF]
		{Character.isJavaIdentifierPart(_input.LA(-1))}?
	|	// covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
		[\uD800-\uDBFF] [\uDC00-\uDFFF]
		{Character.isJavaIdentifierPart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
	;

WHITESPACE : ' ' -> skip ;