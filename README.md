# CircuitC Compiler

#### Author: Mahesh Khanwalkar (mk4548)

## Grammar

The following productions represent the grammar of the CircuitC language.

```
PROG -> CIRCUIT ID '(' ARG-LIST ')' '{' STMT-LIST '}'
ARG-LIST -> ARG COMMA-ARG-LIST | epsilon
ARG -> IN BITS '<' NUM '>' ID | OUT BITS '<' NUM '>' ID
COMMA-ARG-LIST -> ',' ARG COMMA-ARG-LIST | epsilon
STMT-LIST -> STMT SEMI-STMT-LIST | epsilon
STMT -> LVAL | LVAL '=' RVAL
LVAL -> BITS '<' NUM '>' ID | CLOCK ID | REGISTER '<' NUM '>' ID '(' OP-LIST ')' | ID
SEMI-STMT-LIST -> ';' | ';' STMT SEMI-STMT-LIST
OP-LIST -> OP COMMA-OP-LIST | epsilon
OP -> ID | NUM
COMMA-OP-LIST -> ',' OP COMMA-OP-LIST | epsilon
RVAL -> NOT OP | OP | OP BIN-OP OP | OP '?' OP ':' OP
BIN-OP -> AND | OR | XOR
```

The terminals in the grammar are:
1. Anything in single-quotes
2. CIRCUIT, ID, NUM, IN, OUT, BITS, CLOCK, REGISTER, NOT, AND, OR, XOR

Everything else is a non-terminal in the grammar.

## Parser Implementation

### Parsing Algorithm
The parser for this grammar is a recursive descent parser implemented in `Parser.kt`. Each non-terminal has its own
parse method which processes the tokens in the list, calls any other methods to process non-terminals in the current
production, and returns a AST node. The resulting AST nodes (leafs and intermediate nodes) are combined to form the
AST for the entire source file.

### AST Node Definitions
The "nodes" within the AST are defined within `Expression.kt` in the `parser` package. Each concrete class implements
the `Expression` interface, which acts as the generic AST node type.

### Error Handling
Error handling is a fail-fast approach where the first error encountered is reported. Since we keep track of token
positions, which was done during lexical analysis, the error messages directly describe and print out where the error
in the program is, which makes it easy for the user to locate and fix it.

For example, if the program uses a keyword in an assignment (not allowed),
then this would be how the error message would look like:

```
example.circuit:2:14 error: unexpected token 'circuit', expected: 'identifier', or 'number'
    output = circuit;
             ^~~~~~~
```

It prints out:
1. The file name and row:column position of the error
2. What the error is and the expected value
3. The entire line where the error occurred and highlighting of the unexpected token.

This type of verbose error messaging used by this compiler comes from inspiration from how the Clang C compiler
prints out error messages.

### Orchestration
The lexical analysis and parsing phases are joined together in `Main.kt` which first calls the lexical analysis method
which returns a list of tokens. That list is then passed as input to the parsing method, which returns the constructed
AST.

### AST Output Printing
To print the constructed AST to console output, `Main.kt` calls the `printAST` method which is defined within the
`ASTPrinter.kt` file in the `visitor` package.

This method will print out the AST with a tree-formatting to the console. It leverages the **visitor design pattern**
to visit all the nodes in the AST and print out the relevant information.

The visitor interface is defined within `Visitor.kt` which defines a `visit` method for each concrete type of node within
the AST. The `ASTPrinter` class then implements the `Visitor` interface with the functionality of printing the node's
information. The purpose of creating this visitor interface and pattern is to be forward-looking, since this can be
used during the type-checking (semantic analysis) and code generation phases.

## Installation Steps and Running
To build and run the sample programs (described below), there is a provided Dockerfile which will set up a container with JDK 17 installed and run the execute.sh script which runs all the sample programs.

Assumption: docker is installed

```shell
docker build -t circuitcc .
docker run circuitcc
```

## Video Link
https://www.youtube.com/watch?v=4ZarytGdUsM

## Sample Programs

The samples/ directory contains 5 sample programs. Here's a description for them and expected output

### invalidId.circuit

Circuit demonstrating the error handling/reporting capability of the compiler. In this example, we try to use a
keyword as an identifier, which is not allowed. The compiler reports the error and expectation.

```
samples/invalidId.circuit:3:13 error: unexpected token 'circuit', expected: 'identifier'
    bits<1> circuit = 1;
            ^~~~~~~
```

As described above, the compiler is able to generate a descriptive error message with exact error location and
highlighting making it easy for the user to find and correct the error.

### nand.circuit

This is a circuit which implements a NAND logical gate. To add some complexity, we compute NAND of the two
inputs as (NOT A) OR (NOT B) which is derived from De Morgan's Law.

```
CircuitExpression
|__ name: nandGate
|__ args: ArgumentListExpression
    |__ arg0: Argument
        |__ type: INPUT
        |__ name: a
        |__ 1 bits wide
    |__ arg1: Argument
        |__ type: INPUT
        |__ name: b
        |__ 1 bits wide
    |__ arg2: Argument
        |__ type: OUTPUT
        |__ name: output
        |__ 1 bits wide
|__ statements: StatementListExpression
    |__ statement0: AssignmentExpression
        |__ lhs: WireDeclExpression
            |__ name: na
            |__ 1 bits wide
        |__ rhs: UnaryOpExpression
            |__ op: NOT
            |__ rhs: IdentifierOperand
                |__ ID(a)
    |__ statement1: AssignmentExpression
        |__ lhs: WireDeclExpression
            |__ name: nb
            |__ 1 bits wide
        |__ rhs: UnaryOpExpression
            |__ op: NOT
            |__ rhs: IdentifierOperand
                |__ ID(b)
    |__ statement2: AssignmentExpression
        |__ lhs: IdentifierOperand
            |__ ID(output)
        |__ rhs: BinOpExpression
            |__ op: OR
            |__ lhs: IdentifierOperand
                |__ ID(na)
            |__ rhs: IdentifierOperand
                |__ ID(nb)
```

### orGate.circuit

A very basic circuit which uses the 'or' key word to create an or gate circuit

```
CircuitExpression
|__ name: basicOrGate
|__ args: ArgumentListExpression
    |__ arg0: Argument
        |__ type: INPUT
        |__ name: a
        |__ 1 bits wide
    |__ arg1: Argument
        |__ type: INPUT
        |__ name: b
        |__ 1 bits wide
    |__ arg2: Argument
        |__ type: OUTPUT
        |__ name: output
        |__ 1 bits wide
|__ statements: StatementListExpression
    |__ statement0: AssignmentExpression
        |__ lhs: IdentifierOperand
            |__ ID(output)
        |__ rhs: BinOpExpression
            |__ op: OR
            |__ lhs: IdentifierOperand
                |__ ID(a)
            |__ rhs: IdentifierOperand
                |__ ID(b)
```

### register.circuit

A circuit which takes in an input value and a 'set' flag. When the 'set' flag is 1 (true), then it updates the register
with the input value. The output of the circuit is the current register value.

```
CircuitExpression
|__ name: reg
|__ args: ArgumentListExpression
    |__ arg0: Argument
        |__ type: INPUT
        |__ name: input
        |__ 8 bits wide
    |__ arg1: Argument
        |__ type: INPUT
        |__ name: setBit
        |__ 1 bits wide
    |__ arg2: Argument
        |__ type: OUTPUT
        |__ name: output
        |__ 8 bits wide
|__ statements: StatementListExpression
    |__ statement0: ClockDeclExpression
        |__ name: clk
    |__ statement1: RegisterDeclExpression
        |__ name: r
        |__ 8 bits wide
        |__ params: OperandListExpression
            |__ arg0: IdentifierOperand
                |__ ID(clk)
            |__ arg1: IdentifierOperand
                |__ ID(input)
            |__ arg2: IdentifierOperand
                |__ ID(setBit)
            |__ arg3: NumericalOperand
                |__ NUM(0)
    |__ statement2: AssignmentExpression
        |__ lhs: IdentifierOperand
            |__ ID(output)
        |__ rhs: IdentifierOperand
            |__ ID(r)
```

### selector.circuit

A circuit which implements a binary selector -- it takes in two inputs and a selector flag. The flag controls which
input to return as the output.

```
CircuitExpression
|__ name: binarySelect
|__ args: ArgumentListExpression
    |__ arg0: Argument
        |__ type: INPUT
        |__ name: a
        |__ 4 bits wide
    |__ arg1: Argument
        |__ type: INPUT
        |__ name: b
        |__ 4 bits wide
    |__ arg2: Argument
        |__ type: INPUT
        |__ name: sel
        |__ 1 bits wide
    |__ arg3: Argument
        |__ type: OUTPUT
        |__ name: output
        |__ 4 bits wide
|__ statements: StatementListExpression
    |__ statement0: AssignmentExpression
        |__ lhs: IdentifierOperand
            |__ ID(output)
        |__ rhs: TernaryOpExpression
            |__ condition: IdentifierOperand
                |__ ID(sel)
            |__ trueOp: IdentifierOperand
                |__ ID(a)
            |__ falseOp: IdentifierOperand
                |__ ID(b)
```
