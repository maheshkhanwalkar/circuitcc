# CircuitC Compiler

#### Author: Mahesh Khanwalkar (mk4548)

## Lexical Grammar

There are four main components of the lexical grammar: keywords, symbols, identifiers, and numbers. The priorities
in case of ambiguity is exactly in the order presented, with keywords having the highest and numbers the lowest. There
are also whitespace and comments, which the lexer understands, but ignores (does not generate tokens for them)

### Keywords

#### Logical Gates

These keywords represent logical gates

1. and
2. or
3. not
4. xor

#### Pin Type

The two keywords `in` and `out` represent whether a pin is an input pin or an output pin of the circuit.

1. in
2. out

#### Pin/Wire

The keyword `bits` represents a pin or wire. In the complete grammar (not in lexical grammar), you can specify the
bit-width of the pin like this: `bits<8>` (for a 8-bit wire)

#### Clock

The keyword `clock` represents a clock-source, for use with memory sources.

#### Register

The keyword `register` represents a register.

#### Circuit

The keyword `circuit` defines a circuit. The different components represented by the keywords above would be within a
"circuit" (logically and syntactically - although not relevant at the lexical grammar stage)

### Symbols

These are the list of single character symbols that are accepted:
1. \<
2. \>
3. =
4. {
5. }
6. (
7. )
8. ;
9. ,
10. ?
11. :

### Identifiers

An identifier is a string made up of one or more lowercase alphabetic characters. The regex that would describe the set
of acceptable identifiers would be `[a-z]+`

### Numbers

A number is a string made up of one or more numeric characters, with the restriction that there cannot be any leading
zeros, e.g. 0 is fine, but 08 would be split into two tokens. The regex that would describe the set of acceptable
identifiers would be: `0|([1-9][0-9]*)`

### Whitespace

Whitespace (space, tab, newline, carriage return) is ignored.

### Comment

The "//" denotes the start of a comment, which causes the lexer to ignore all characters that follow it until the next
line is reached. Only single line comments are supported. (e.g. no /* */ multi-line comments)

## Scanning Algorithm

The scanning algorithm is split up between DFA and Lexer classes in `edu.columbia.circuitc.lexer` package. The DFA class
implements the actual DFA logic for consuming a character and performing state transitions. The Lexer class contains
all the DFAs for the language and exposes the `tokenize` method which takes the source program and returns a list of
tokens (represented by the class Token).

The DFALoader class loads a graph representation from disk and builds a DFA. The graph representations are stored within
the `resources/lexer/tokens.json` file which contains all the DFA graphs in adjacency list format. This file was
constructed by hand.

The Token class represents a token and contains four fields:

1. type - the type of token
2. text - the text payload of the token
3. start - the start position (row, column) of the token in the original source text
4. end - the end position (row, column) of the token in the original source text

The reason for keeping `start` and `end` fields in the token is to allow for better error messaging (especially down
the line with syntactic parsing) which can pinpoint where in the original source the error arises.

## Error Handling

Upon encountering an invalid token, the lexer prints out the error with the invalid token string and position. However,
the lexer does not just exit after encountering the first invalid token. Instead, it prints the error and keeps on
trying to generate more tokens and printing out more errors if it encounters more invalid tokens.

## Installation Steps and Running

To build and run the sample programs (described below), there is a provided Dockerfile which will set up a container
with JDK 17 installed and run the `execute.sh` script which runs all the sample programs.

**Assumption:** docker is installed

```shell
docker build -t circuitcc .
docker run circuitcc
```

## Sample Programs

The samples/ directory contains 5 sample programs. Here's a description for them and expected output. The actual output
from the lexer contains additional information (token positions), which is omitted here for brevity.

### one.circuit

Very basic circuit representing an 1-bit input pin which we're setting to 1.

```
<IN, int>
<BITS, bits>
<LEFT_ANGLE, < >
<NUM, 1>
<RIGHT_ANGLE, > >
<IDENTIFIER, i>
<EQUALS, = >
<NUM, 1>
<SEMICOLON, ; >
```

### two.circuit

An example where the lexer can tokenize without whitespace.

```
<IDENTIFIER, abc>
<EQUALS, = >
<IDENTIFIER, def>
<SEMICOLON, ; >
```
### three.circuit

An example to demonstrate that comments are ignored by the lexer

```
<CIRCUIT, circuit>
<IDENTIFIER, c>
<LEFT_PAREN, ( >
<RIGHT_PAREN, ) >
```

### four.circuit

An example to demonstrate error handling. The errors are reported, but the lexer still tries to tokenize everything
else that is still valid.

```
invalid token: #
invalid token: !

<IDENTIFIER, a>
<EQUALS, = >
<IDENTIFIER, abc>
<OR, or>
<IDENTIFIER, def>
<SEMICOLON, ; >
<IDENTIFIER, c>
<EQUALS, = >
<IDENTIFIER, a>
<QUESTION, ? >
<NUM, 1>
<COLON, : >
<NUM, 0>
<SEMICOLON, ; >
```

### five.circuit

A more complicated circuit which uses more language constructs.

```
<CIRCUIT, circuit>
<IDENTIFIER, example>
<LEFT_PAREN, ( >
<OUT, out>
<BITS, bits>
<LEFT_ANGLE, < >
<NUM, 1>
<RIGHT_ANGLE, > >
<IDENTIFIER, output>
<RIGHT_PAREN, ) >
<LEFT_BRACE, { >
<CLOCK, clock>
<IDENTIFIER, c>
<SEMICOLON, ; >
<REGISTER, register>
<LEFT_ANGLE, < >
<NUM, 1>
<RIGHT_ANGLE, > >
<IDENTIFIER, reg>
<LEFT_PAREN, ( >
<IDENTIFIER, c>
<COMMA, , >
<NUM, 0>
<OR, or>
<NUM, 1>
<COMMA, , >
<NUM, 1>
<COMMA, , >
<NUM, 0>
<RIGHT_PAREN, )>
<SEMICOLON, ; >
<RIGHT_BRACE, } >
```
