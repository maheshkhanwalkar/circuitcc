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

### Parser Implementation

#### Parsing Algorithm
The parser for this grammar is a recursive descent parser implemented in `Parser.kt`. Each non-terminal has its own
parse method which processes the tokens in the list, calls any other methods to process non-terminals in the current
production, and returns a AST node. The resulting AST nodes (leafs and intermediate nodes) are combined to form the
AST for the entire source file.

#### AST Node Definitions
The "nodes" within the AST are defined within `Expression.kt` in the `parser` package. Each concrete class implements
the `Expression` interface, which acts as the generic AST node type.

#### Error Handling
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

#### Orchestration
The lexical analysis and parsing phases are joined together in `Main.kt` which first calls the lexical analysis method
which returns a list of tokens. That list is then passed as input to the parsing method, which returns the constructed
AST.

#### AST Output Printing
To print the constructed AST to console output, `Main.kt` calls the `printAST` method which is defined within the
`ASTPrinter.kt` file in the `visitor` package.

This method will print out the AST with a tree-formatting to the console. It leverages the **visitor design pattern**
to visit all the nodes in the AST and print out the relevant information.

The visitor interface is defined within `Visitor.kt` which defines a `visit` method for each concrete type of node within
the AST. The `ASTPrinter` class then implements the `Visitor` interface with the functionality of printing the node's
information. The purpose of creating this visitor interface and pattern is to be forward-looking, since this can be
used during the type-checking (semantic analysis) and code generation phases.
