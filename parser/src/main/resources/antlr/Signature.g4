//
// Copyright 2019 Viridian Software Ltd.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
grammar Signature;


//------------ Class Signature ------------
classSignature
    : typeParameters? superclassSignature superinterfaceSignature*
    ;

typeParameters
    : LT typeParameter+ GT
    ;

typeParameter
    : identifier classBound interfaceBounds?
    ;

classBound
    : COLON referenceTypeSignature?
    ;

interfaceBounds
    : interfaceBound+
    ;

interfaceBound
    : COLON classTypeSignature
    ;

superclassSignature
    : classTypeSignature;

superinterfaceSignature
    : classTypeSignature;

//------------ Method Signature ------------
methodSignature
    : typeParameters? LEFTROUNDBRACKET javaTypeSignature* RIGHTROUNDBRACKET returnType throwsSignature*
    ;

returnType
    : javaTypeSignature
    | VoidDescriptor
    ;

throwsSignature
    : CARET classTypeSignature
    | CARET typeVariableSignature
    ;

//------------ Field Signature ------------
fieldSignature
    : referenceTypeSignature
    ;

//------------ Type Signatures ------------
javaTypeSignature
    : referenceTypeSignature
    | PrimitiveType
    ;

referenceTypeSignature
    : classTypeSignature
    | typeVariableSignature
    | arrayTypeSignature
    ;

classTypeSignature
    : ObjectType packageSpecifier? simpleClassTypeSignature classTypeSignatureSuffix* SEMICOLON
    ;

packageSpecifier
    : identifier FORWARDSLASH packageSpecifier?
    ;

simpleClassTypeSignature
    : identifier typeArguments?
    ;

typeArguments
    : LT typeArgument+ GT
    ;

typeArgument
    : WILDCARD
    | WildcardIndicator? referenceTypeSignature
    ;

WildcardIndicator
    : EXTENDSWILDCARD
    | SUPERWILDCARD
    ;

classTypeSignatureSuffix
    : FULLSTOP simpleClassTypeSignature
    ;

typeVariableSignature
    : TypeIndicator identifier SEMICOLON
    ;

arrayTypeSignature
    : LEFTBOXBRACKET javaTypeSignature
    ;

LT : '<';
GT : '>';
WILDCARD : '*';
EXTENDSWILDCARD : '+';
SUPERWILDCARD : '-';
LEFTBOXBRACKET : '[';
LEFTROUNDBRACKET : '(';
RIGHTROUNDBRACKET : ')';
COLON : ':';
SEMICOLON : ';';
FULLSTOP : '.';
FORWARDSLASH : '/';
CARET : '^';

identifier
	:	(JavaLetterOrDigit | TypeIndicator | ObjectType | PrimitiveType | VoidDescriptor)+
	;

TypeIndicator
    : 'T'
    ;

ObjectType
    : 'L'
    ;

PrimitiveType
    : [BCDFIJSZ]
    ;

VoidDescriptor
    : 'V'
    ;

JavaLetterOrDigit
	:	[a-zA-Z0-9$_] // these are the "java letters or digits" below 0x7F
	|	// covers all characters above 0x7F which are not a surrogate
		~[\u0000-\u007F\uD800-\uDBFF]
		{Character.isJavaIdentifierPart(_input.LA(-1))}?
	|	// covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
		[\uD800-\uDBFF] [\uDC00-\uDFFF]
		{Character.isJavaIdentifierPart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
	;