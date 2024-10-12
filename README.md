# CircuitC Compiler

#### Author: Mahesh Khanwalkar (mk4548)

## Lexical Grammar

There are four main components of the lexical grammar: keywords, symbols, identifiers, and numbers. The priorities
in case of ambiguity is exactly in the order presented, with keywords having the highest and numbers the lowest.

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