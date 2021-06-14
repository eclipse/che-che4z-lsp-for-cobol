/*
 * [The "BSD license"]
 *  Copyright (c) 2012-2015 Terence Parr
 *  Copyright (c) 2012-2015 Sam Harwell
 *  Copyright (c) 2015 Gerald Rosenberg
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/**
 *	A grammar for ANTLR v4 implemented using v4 syntax
 *
 *	Modified 2015.06.16 gbr
 *	-- update for compatibility with Antlr v4.5
 */

// ======================================================
// Lexer specification
// ======================================================

lexer grammar ANTLRv4Lexer;

options { superClass = org.eclipse.lsp.cobol.core.grammarcheck.LexerAdaptor; }

// Standard set of fragments
tokens { TOKEN_REF , RULE_REF , LEXER_CHAR_SET }
channels { OFF_CHANNEL , COMMENT }

// -------------------------
// Comments
DOC_COMMENT
   : DocComment -> channel (COMMENT)
   ;

BLOCK_COMMENT
   : BlockComment -> channel (COMMENT)
   ;

LINE_COMMENT
   : LineComment -> channel (COMMENT)
   ;

// -------------------------
// Integer

INT
   : DecimalNumeral
   ;

// -------------------------
// Literal string
//
// ANTLR makes no distinction between a single character literal and a
// multi-character string. All literals are single quote delimited and
// may contain unicode escape sequences of the form \uxxxx, where x
// is a valid hexadecimal number (per Unicode standard).
STRING_LITERAL
   : SQuoteLiteral
   ;

UNTERMINATED_STRING_LITERAL
   : USQuoteLiteral
   ;

// -------------------------
// Arguments
//
// Certain argument lists, such as those specifying call parameters
// to a rule invocation, or input parameters to a rule specification
// are contained within square brackets.
BEGIN_ARGUMENT
   : LBrack
   { this.handleBeginArgument(); }
   ;

// -------------------------
// Target Language Actions
BEGIN_ACTION
   : LBrace -> pushMode (TargetLanguageAction)
   ;

// -------------------------
// Keywords
//
// 'options', 'tokens', and 'channels' are considered keywords
// but only when followed by '{', and considered as a single token.
// Otherwise, the symbols are tokenized as RULE_REF and allowed as
// an identifier in a labeledElement.
OPTIONS      : 'options'  WSNLCHARS* '{'  ;
TOKENS       : 'tokens'   WSNLCHARS* '{'  ;
CHANNELS     : 'channels' WSNLCHARS* '{'  ;

fragment WSNLCHARS : ' ' | '\t' | '\f' | '\n' | '\r' ;

IMPORT
   : 'import'
   ;

FRAGMENT
   : 'fragment'
   ;

LEXER
   : 'lexer'
   ;

PARSER
   : 'parser'
   ;

GRAMMAR
   : 'grammar'
   ;

PROTECTED
   : 'protected'
   ;

PUBLIC
   : 'public'
   ;

PRIVATE
   : 'private'
   ;

RETURNS
   : 'returns'
   ;

LOCALS
   : 'locals'
   ;

THROWS
   : 'throws'
   ;

CATCH
   : 'catch'
   ;

FINALLY
   : 'finally'
   ;

MODE
   : 'mode'
   ;
   // -------------------------
   // Punctuation

COLON
   : Colon
   ;

COLONCOLON
   : DColon
   ;

COMMA
   : Comma
   ;

SEMI
   : Semi
   ;

LPAREN
   : LParen
   ;

RPAREN
   : RParen
   ;

LBRACE
   : LBrace
   ;

RBRACE
   : RBrace
   ;

RARROW
   : RArrow
   ;

LT
   : Lt
   ;

GT
   : Gt
   ;

ASSIGN
   : Equal
   ;

QUESTION
   : Question
   ;

STAR
   : Star
   ;

PLUS_ASSIGN
   : PlusAssign
   ;

PLUS
   : Plus
   ;

OR
   : Pipe
   ;

DOLLAR
   : Dollar
   ;

RANGE
   : Range
   ;

DOT
   : Dot
   ;

AT
   : At
   ;

POUND
   : Pound
   ;

NOT
   : Tilde
   ;
   // -------------------------
   // Identifiers - allows unicode rule/token names

ID
   : Id
   ;
   // -------------------------
   // Whitespace

WS
   : Ws+ -> channel (OFF_CHANNEL)
   ;

// -------------------------
// Illegal Characters
//
// This is an illegal character trap which is always the last rule in the
// lexer specification. It matches a single character of any value and being
// the last rule in the file will match when no other rule knows what to do
// about the character. It is reported as an error but is not passed on to the
// parser. This means that the parser to deal with the gramamr file anyway
// but we will not try to analyse or code generate from a file with lexical
// errors.

// Comment this rule out to allow the error to be propagated to the parser
ERRCHAR
   : . -> channel (HIDDEN)
   ;

// ======================================================
// Lexer modes
// -------------------------
// Arguments
mode Argument;
// E.g., [int x, List<String> a[]]
NESTED_ARGUMENT
   : LBrack -> type (ARGUMENT_CONTENT) , pushMode (Argument)
   ;

ARGUMENT_ESCAPE
   : EscAny -> type (ARGUMENT_CONTENT)
   ;

ARGUMENT_STRING_LITERAL
   : DQuoteLiteral -> type (ARGUMENT_CONTENT)
   ;

ARGUMENT_CHAR_LITERAL
   : SQuoteLiteral -> type (ARGUMENT_CONTENT)
   ;

END_ARGUMENT
   : RBrack
   { this.handleEndArgument(); }
   ;

// added this to return non-EOF token type here. EOF does something weird
UNTERMINATED_ARGUMENT
   : EOF -> popMode
   ;

ARGUMENT_CONTENT
   : .
   ;

// -------------------------
// Target Language Actions
//
// Many language targets use {} as block delimiters and so we
// must recursively match {} delimited blocks to balance the
// braces. Additionally, we must make some assumptions about
// literal string representation in the target language. We assume
// that they are delimited by ' or " and so consume these
// in their own alts so as not to inadvertantly match {}.
mode TargetLanguageAction;
NESTED_ACTION
   : LBrace -> type (ACTION_CONTENT) , pushMode (TargetLanguageAction)
   ;

ACTION_ESCAPE
   : EscAny -> type (ACTION_CONTENT)
   ;

ACTION_STRING_LITERAL
   : DQuoteLiteral -> type (ACTION_CONTENT)
   ;

ACTION_CHAR_LITERAL
   : SQuoteLiteral -> type (ACTION_CONTENT)
   ;

ACTION_DOC_COMMENT
   : DocComment -> type (ACTION_CONTENT)
   ;

ACTION_BLOCK_COMMENT
   : BlockComment -> type (ACTION_CONTENT)
   ;

ACTION_LINE_COMMENT
   : LineComment -> type (ACTION_CONTENT)
   ;

END_ACTION
   : RBrace
   { this.handleEndAction(); }
   ;

UNTERMINATED_ACTION
   : EOF -> popMode
   ;

ACTION_CONTENT
   : .
   ;

// -------------------------
mode LexerCharSet;
LEXER_CHAR_SET_BODY
   : (~ [\]\\] | EscAny)+ -> more
   ;

LEXER_CHAR_SET
   : RBrack -> popMode
   ;

UNTERMINATED_CHAR_SET
   : EOF -> popMode
   ;

// ------------------------------------------------------------------------------
// Grammar specific Keywords, Punctuation, etc.
fragment Id
   : NameStartChar NameChar*
   ;
   

fragment Ws
   : Hws
   | Vws
   ;

fragment Hws
   : [ \t]
   ;

fragment Vws
   : [\r\n\f]
   ;

fragment BlockComment
   : '/*' .*? ('*/' | EOF)
   ;

fragment DocComment
   : '/**' .*? ('*/' | EOF)
   ;

fragment LineComment
   : '//' ~ [\r\n]*
   ;
   // -----------------------------------
   // Escapes
   // Any kind of escaped character that we can embed within ANTLR literal strings.

fragment EscSeq
   : Esc ([btnfr"'\\] | UnicodeEsc | . | EOF)
   ;

fragment EscAny
   : Esc .
   ;

fragment UnicodeEsc
   : 'u' (HexDigit (HexDigit (HexDigit HexDigit?)?)?)?
   ;
   // -----------------------------------
   // Numerals

fragment DecimalNumeral
   : '0'
   | [1-9] DecDigit*
   ;
   // -----------------------------------
   // Digits

fragment HexDigit
   : [0-9a-fA-F]
   ;

fragment DecDigit
   : [0-9]
   ;
   // -----------------------------------
   // Literals

fragment BoolLiteral
   : 'true'
   | 'false'
   ;

fragment CharLiteral
   : SQuote (EscSeq | ~ ['\r\n\\]) SQuote
   ;

fragment SQuoteLiteral
   : SQuote (EscSeq | ~ ['\r\n\\])* SQuote
   ;

fragment DQuoteLiteral
   : DQuote (EscSeq | ~ ["\r\n\\])* DQuote
   ;

fragment USQuoteLiteral
   : SQuote (EscSeq | ~ ['\r\n\\])*
   ;
   // -----------------------------------
   // Character ranges

fragment NameChar
   : NameStartChar
   | '0' .. '9'
   | Underscore
   | '\u00B7'
   | '\u0300' .. '\u036F'
   | '\u203F' .. '\u2040'
   ;

fragment NameStartChar
   : 'A' .. 'Z'
   | 'a' .. 'z'
   | '\u00C0' .. '\u00D6'
   | '\u00D8' .. '\u00F6'
   | '\u00F8' .. '\u02FF'
   | '\u0370' .. '\u037D'
   | '\u037F' .. '\u1FFF'
   | '\u200C' .. '\u200D'
   | '\u2070' .. '\u218F'
   | '\u2C00' .. '\u2FEF'
   | '\u3001' .. '\uD7FF'
   | '\uF900' .. '\uFDCF'
   | '\uFDF0' .. '\uFFFD'
   ;
   // ignores | ['\u10000-'\uEFFFF] ;
   // -----------------------------------
   // Types

fragment Int
   : 'int'
   ;
   // -----------------------------------
   // Symbols

fragment Esc
   : '\\'
   ;

fragment Colon
   : ':'
   ;

fragment DColon
   : '::'
   ;

fragment SQuote
   : '\''
   ;

fragment DQuote
   : '"'
   ;

fragment LParen
   : '('
   ;

fragment RParen
   : ')'
   ;

fragment LBrace
   : '{'
   ;

fragment RBrace
   : '}'
   ;

fragment LBrack
   : '['
   ;

fragment RBrack
   : ']'
   ;

fragment RArrow
   : '->'
   ;

fragment Lt
   : '<'
   ;

fragment Gt
   : '>'
   ;

fragment Equal
   : '='
   ;

fragment Question
   : '?'
   ;

fragment Star
   : '*'
   ;

fragment Plus
   : '+'
   ;

fragment PlusAssign
   : '+='
   ;

fragment Underscore
   : '_'
   ;

fragment Pipe
   : '|'
   ;

fragment Dollar
   : '$'
   ;

fragment Comma
   : ','
   ;

fragment Semi
   : ';'
   ;

fragment Dot
   : '.'
   ;

fragment Range
   : '..'
   ;

fragment At
   : '@'
   ;

fragment Pound
   : '#'
   ;

fragment Tilde
   : '~'
   ;

