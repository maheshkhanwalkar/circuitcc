# CircuitC Compiler

#### Author: Mahesh Khanwalkar (mk4548)

## Code Generation Implementation

The code generation phase is implemented in two steps. The input into the code generation phase is the abstract syntax
tree (AST) generated from the parsing phase. This AST is traversed to generate an intermediate representation (IR). Then,
the IR is traversed to generate the .sim language.

Prior to code generation, we run semantic analysis phase which performs semantic validation to ensure the program does
not have any semantic errors. If that analysis passes, then we proceed with code generation. If there are any failures,
then the compilation terminates.

### Symbol Table implementation

Both the semantic analysis, IR and SIM code generation phases rely on a symbol table implemented in `SymbolTable.kt`
which keeps track of symbols already seen. This allows the traversals to reference previously generated constructs by name.
The implementation also handles scoping and variable shadowing across scopes -- although for our purposes, this isn't
strictly necessary as we don't have multiple levels of scoping in the language.

### Semantic Analysis

The semantic analysis phase is implemented in `SemanticAnalysis.kt` using the AST visitor pattern. There are two
analyses performed: duplicate declaration and bit-width equality.

1. Duplicate declaration - check whether there are two or more variables with the same name (not allowed)
2. Bit-width equality - the bit widths of expressions are matching or can be promoted to match.

Duplicate declaration uses the symbol table to capture identifiers it has already seen. If we see a declaration
expression and the name already has an entry in the symbol table, then there's a duplicate declaration.

Bit-width equality predominantly ensures that expressions have the same bit-width. For example, the following code
snippet has multiple bit-width equality issues:

```
in bits<4> a;
in bits<8> b;
...
bits<2> out = a or b;
```

First, `a` and `b` don't have the same bit width so that's an issue. Any binary operand expression needs to have
bit-width matching for the left hand and right hand side of the operand, e.g. `a` and `b`. Second, in an assignment
expression the left-value (lval) and right-value (rval) should have the same bit-width as well, e.g. `out` and `a or b`.

For constant values (e.g. numerical values), the bit-width calculation is a bit interesting because we do not explicitly
specify the bit-width. For a constant value, there exists a minimum bit-width needed to represent that value but it is
not strict -- we can freely increase (**but not decrease**) the bit-width to match its usage in context with expressions
that have an explicit bit-width. For example `bits<4> a = 3;` while `3` can be represented using just 2 bits, since `a`
has a fixed width of 4 bits, we would then promote `3` to be 4 bits wide.

### Intermediate Representation (IR) Generation

The AST generated during parsing is still "high-level", so we take an intermediate step to lower the representation into
IR, which makes the final codegen step easier. For example, in the AST, there is a binary operand expression. This is
a high-level construct -- which in the IR will be directly mapped to the corresponding logical gate. So, if it is a
binary operand expression with an 'and', then it becomes an AndGateValue in the IR. When we get to the SIM code gen
step, then this AndGateValue directly becomes an AND gate.

The IR generation is implemented in `CodeGen.kt` in the `IRGen` class using the visitor pattern
over the AST. The IR constructs are defined within `IR.kt` and are much closer to the actual SIM language constructs.

### SIM Code Generation

Once we have the IR generated, we then perform a pass over the IR to generate the SIM code. This is also done using
a visitor pattern, albeit with the `IRVisitor` interface (rather than the `ASTVisitor`). The SIM language constructs
are defined within `Construct.kt` under the `edu.columbia.circuitc.sim` package. These can be directly serialized into
JSON output to create the final .sim file.

The interesting challenge here is the placement and wiring of components together. Since the .sim language is used in
a visual tool, the components have an (x, y) position and wiring connecting them. The code generation uses a simple
placement heuristic to space out the components and determines the wiring (multiple wires can be required) to connect
the components together. To space out the components, we start off in the top-left corner and move towards the
bottom-right corner, which prevents the situation of wires merging together (causing a short-circuit) when connecting
components together.

The (x, y) position of the component represents the top-left corner of the component itself but
not where the pins are on the component. For each component (input pin, output pin, register, etc.), we translate the
(x, y) position to the location of the input pin(s) and the output pin and store that in the generated `SimComponent`.
Wiring is handled by the `WireBuilder` class in `CodeGen.kt` file. Given a source point and a sink point, it computes
what wiring (horizontal and vertical) is needed to connect those two points. These source and sink points are exactly
what was computed as the location of input and output pins.

## Installation Steps and Running
To build and run the sample programs (described below), there is a provided Dockerfile which will set up a container with JDK 17 installed and run the execute.sh script which runs all the sample programs.

Assumption: docker is installed

```shell
docker build -t circuitcc .
docker run circuitcc
```

## Video Link
https://www.youtube.com/watch?v=8l0aJVOMzUk

## Sample Programs

The samples/ directory contains 5 sample programs. Here's a description for them and expected output

### invalidId.circuit

Circuit demonstrating the error handling/reporting due to a semantic error. In this example, we have two variables with
the same name (`dup`), which is not allowed.

```
error. redefinition of 'dup' found
```

As described above, the compiler is able to generate a descriptive error message with exact error location and
highlighting making it easy for the user to find and correct the error.

### nand.circuit

This is a circuit which implements a NAND logical gate. To add some complexity, we compute NAND of the two
inputs as (NOT A) OR (NOT B) which is derived from De Morgan's Law.

```
{
  "circuits" : [ {
    "name" : "nandGate",
    "components" : [ {
      "name" : "com.ra4king.circuitsim.gui.peers.wiring.PinPeer",
      "x" : 10,
      "y" : 10,
      "properties" : {
        "Label location" : "WEST",
        "Label" : "a",
        "Is input?" : "Yes",
        "Direction" : "EAST",
        "Bitsize" : "1"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.wiring.PinPeer",
      "x" : 18,
      "y" : 18,
      "properties" : {
        "Label location" : "WEST",
        "Label" : "b",
        "Is input?" : "Yes",
        "Direction" : "EAST",
        "Bitsize" : "1"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.gates.NotGatePeer",
      "x" : 26,
      "y" : 26,
      "properties" : {
        "Label location" : "NORTH",
        "Label" : "",
        "Direction" : "EAST",
        "Bitsize" : "1"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.wiring.Tunnel",
      "x" : 34,
      "y" : 34,
      "properties" : {
        "Label location" : "EAST",
        "Label" : "na",
        "Direction" : "WEST",
        "Bitsize" : "1"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.gates.NotGatePeer",
      "x" : 42,
      "y" : 42,
      "properties" : {
        "Label location" : "NORTH",
        "Label" : "",
        "Direction" : "EAST",
        "Bitsize" : "1"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.wiring.Tunnel",
      "x" : 50,
      "y" : 50,
      "properties" : {
        "Label location" : "EAST",
        "Label" : "nb",
        "Direction" : "WEST",
        "Bitsize" : "1"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.gates.OrGatePeer",
      "x" : 58,
      "y" : 58,
      "properties" : {
        "Negate 1" : "No",
        "Label location" : "NORTH",
        "Negate 0" : "No",
        "Number of Inputs" : "2",
        "Label" : "",
        "Direction" : "EAST",
        "Bitsize" : "1"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.wiring.PinPeer",
      "x" : 66,
      "y" : 66,
      "properties" : {
        "Label location" : "EAST",
        "Label" : "output",
        "Is input?" : "No",
        "Direction" : "WEST",
        "Bitsize" : "1"
      }
    } ],
    "wires" : [ {
      "x" : 12,
      "y" : 11,
      "length" : 16,
      "isHorizontal" : false
    }, {
      "x" : 12,
      "y" : 27,
      "length" : 14,
      "isHorizontal" : true
    }, {
      "x" : 29,
      "y" : 27,
      "length" : 8,
      "isHorizontal" : false
    }, {
      "x" : 29,
      "y" : 35,
      "length" : 5,
      "isHorizontal" : true
    }, {
      "x" : 20,
      "y" : 19,
      "length" : 24,
      "isHorizontal" : false
    }, {
      "x" : 20,
      "y" : 43,
      "length" : 22,
      "isHorizontal" : true
    }, {
      "x" : 45,
      "y" : 43,
      "length" : 8,
      "isHorizontal" : false
    }, {
      "x" : 45,
      "y" : 51,
      "length" : 5,
      "isHorizontal" : true
    }, {
      "x" : 34,
      "y" : 35,
      "length" : 24,
      "isHorizontal" : false
    }, {
      "x" : 34,
      "y" : 59,
      "length" : 24,
      "isHorizontal" : true
    }, {
      "x" : 50,
      "y" : 51,
      "length" : 10,
      "isHorizontal" : false
    }, {
      "x" : 50,
      "y" : 61,
      "length" : 8,
      "isHorizontal" : true
    }, {
      "x" : 62,
      "y" : 60,
      "length" : 7,
      "isHorizontal" : false
    }, {
      "x" : 62,
      "y" : 67,
      "length" : 4,
      "isHorizontal" : true
    } ]
  } ],
  "version" : "1.9.2b",
  "globalBitSize" : 1,
  "clockSpeed" : 1
}
```

![nand circuit](https://github.com/maheshkhanwalkar/circuitcc/blob/codegen-dev/images/nand.png?raw=true)

### orGate.circuit

A very basic circuit which uses the 'or' key word to create an or gate circuit

```
{
  "circuits" : [ {
    "name" : "basicOrGate",
    "components" : [ {
      "name" : "com.ra4king.circuitsim.gui.peers.wiring.PinPeer",
      "x" : 10,
      "y" : 10,
      "properties" : {
        "Label location" : "WEST",
        "Label" : "a",
        "Is input?" : "Yes",
        "Direction" : "EAST",
        "Bitsize" : "1"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.wiring.PinPeer",
      "x" : 18,
      "y" : 18,
      "properties" : {
        "Label location" : "WEST",
        "Label" : "b",
        "Is input?" : "Yes",
        "Direction" : "EAST",
        "Bitsize" : "1"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.gates.OrGatePeer",
      "x" : 26,
      "y" : 26,
      "properties" : {
        "Negate 1" : "No",
        "Label location" : "NORTH",
        "Negate 0" : "No",
        "Number of Inputs" : "2",
        "Label" : "",
        "Direction" : "EAST",
        "Bitsize" : "1"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.wiring.PinPeer",
      "x" : 34,
      "y" : 34,
      "properties" : {
        "Label location" : "EAST",
        "Label" : "output",
        "Is input?" : "No",
        "Direction" : "WEST",
        "Bitsize" : "1"
      }
    } ],
    "wires" : [ {
      "x" : 12,
      "y" : 11,
      "length" : 16,
      "isHorizontal" : false
    }, {
      "x" : 12,
      "y" : 27,
      "length" : 14,
      "isHorizontal" : true
    }, {
      "x" : 20,
      "y" : 19,
      "length" : 10,
      "isHorizontal" : false
    }, {
      "x" : 20,
      "y" : 29,
      "length" : 6,
      "isHorizontal" : true
    }, {
      "x" : 30,
      "y" : 28,
      "length" : 7,
      "isHorizontal" : false
    }, {
      "x" : 30,
      "y" : 35,
      "length" : 4,
      "isHorizontal" : true
    } ]
  } ],
  "version" : "1.9.2b",
  "globalBitSize" : 1,
  "clockSpeed" : 1
}
```

![orGate circuit](https://github.com/maheshkhanwalkar/circuitcc/blob/codegen-dev/images/orGate.png?raw=true)

### register.circuit

A circuit which takes in an input value and a 'set' flag. When the 'set' flag is 1 (true), then it updates the register
with the input value. The output of the circuit is the current register value.

```
{
  "circuits" : [ {
    "name" : "reg",
    "components" : [ {
      "name" : "com.ra4king.circuitsim.gui.peers.wiring.PinPeer",
      "x" : 10,
      "y" : 10,
      "properties" : {
        "Label location" : "WEST",
        "Label" : "input",
        "Is input?" : "Yes",
        "Direction" : "EAST",
        "Bitsize" : "8"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.wiring.PinPeer",
      "x" : 18,
      "y" : 18,
      "properties" : {
        "Label location" : "WEST",
        "Label" : "setBit",
        "Is input?" : "Yes",
        "Direction" : "EAST",
        "Bitsize" : "1"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.wiring.ClockPeer",
      "x" : 26,
      "y" : 26,
      "properties" : {
        "Label location" : "NORTH",
        "Label" : "clk",
        "Direction" : "EAST"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.wiring.ConstantPeer",
      "x" : 34,
      "y" : 34,
      "properties" : {
        "Label location" : "NORTH",
        "Label" : "",
        "Value" : "0",
        "Direction" : "EAST",
        "Bitsize" : "1",
        "Base" : "BINARY"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.memory.RegisterPeer",
      "x" : 42,
      "y" : 42,
      "properties" : {
        "Label location" : "NORTH",
        "Label" : "r",
        "Bitsize" : "8"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.wiring.PinPeer",
      "x" : 50,
      "y" : 50,
      "properties" : {
        "Label location" : "EAST",
        "Label" : "output",
        "Is input?" : "No",
        "Direction" : "WEST",
        "Bitsize" : "8"
      }
    } ],
    "wires" : [ {
      "x" : 18,
      "y" : 11,
      "length" : 33,
      "isHorizontal" : false
    }, {
      "x" : 18,
      "y" : 44,
      "length" : 24,
      "isHorizontal" : true
    }, {
      "x" : 20,
      "y" : 19,
      "length" : 26,
      "isHorizontal" : false
    }, {
      "x" : 20,
      "y" : 45,
      "length" : 22,
      "isHorizontal" : true
    }, {
      "x" : 28,
      "y" : 27,
      "length" : 21,
      "isHorizontal" : false
    }, {
      "x" : 28,
      "y" : 48,
      "length" : 15,
      "isHorizontal" : true
    }, {
      "x" : 43,
      "y" : 46,
      "length" : 2,
      "isHorizontal" : false
    }, {
      "x" : 36,
      "y" : 35,
      "length" : 12,
      "isHorizontal" : false
    }, {
      "x" : 36,
      "y" : 47,
      "length" : 9,
      "isHorizontal" : true
    }, {
      "x" : 45,
      "y" : 46,
      "length" : 1,
      "isHorizontal" : false
    }, {
      "x" : 46,
      "y" : 44,
      "length" : 7,
      "isHorizontal" : false
    }, {
      "x" : 46,
      "y" : 51,
      "length" : 4,
      "isHorizontal" : true
    } ]
  } ],
  "version" : "1.9.2b",
  "globalBitSize" : 1,
  "clockSpeed" : 1
}
```

![register circuit](https://github.com/maheshkhanwalkar/circuitcc/blob/codegen-dev/images/register.png?raw=true)

### selector.circuit

A circuit which implements a binary selector -- it takes in two inputs and a selector flag. The flag controls which
input to return as the output.

```
{
  "circuits" : [ {
    "name" : "binarySelect",
    "components" : [ {
      "name" : "com.ra4king.circuitsim.gui.peers.wiring.PinPeer",
      "x" : 10,
      "y" : 10,
      "properties" : {
        "Label location" : "WEST",
        "Label" : "a",
        "Is input?" : "Yes",
        "Direction" : "EAST",
        "Bitsize" : "4"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.wiring.PinPeer",
      "x" : 18,
      "y" : 18,
      "properties" : {
        "Label location" : "WEST",
        "Label" : "b",
        "Is input?" : "Yes",
        "Direction" : "EAST",
        "Bitsize" : "4"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.wiring.PinPeer",
      "x" : 26,
      "y" : 26,
      "properties" : {
        "Label location" : "WEST",
        "Label" : "sel",
        "Is input?" : "Yes",
        "Direction" : "EAST",
        "Bitsize" : "1"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.plexers.MultiplexerPeer",
      "x" : 34,
      "y" : 34,
      "properties" : {
        "Selector location" : "Right/Down",
        "Label location" : "NORTH",
        "Selector bits" : "1",
        "Label" : "",
        "Direction" : "EAST",
        "Bitsize" : "4"
      }
    }, {
      "name" : "com.ra4king.circuitsim.gui.peers.wiring.PinPeer",
      "x" : 42,
      "y" : 42,
      "properties" : {
        "Label location" : "EAST",
        "Label" : "output",
        "Is input?" : "No",
        "Direction" : "WEST",
        "Bitsize" : "4"
      }
    } ],
    "wires" : [ {
      "x" : 14,
      "y" : 11,
      "length" : 25,
      "isHorizontal" : false
    }, {
      "x" : 14,
      "y" : 36,
      "length" : 20,
      "isHorizontal" : true
    }, {
      "x" : 22,
      "y" : 19,
      "length" : 16,
      "isHorizontal" : false
    }, {
      "x" : 22,
      "y" : 35,
      "length" : 12,
      "isHorizontal" : true
    }, {
      "x" : 28,
      "y" : 27,
      "length" : 11,
      "isHorizontal" : false
    }, {
      "x" : 28,
      "y" : 38,
      "length" : 7,
      "isHorizontal" : true
    }, {
      "x" : 37,
      "y" : 36,
      "length" : 7,
      "isHorizontal" : false
    }, {
      "x" : 37,
      "y" : 43,
      "length" : 5,
      "isHorizontal" : true
    } ]
  } ],
  "version" : "1.9.2b",
  "globalBitSize" : 1,
  "clockSpeed" : 1
}
```

![selector circuit](https://github.com/maheshkhanwalkar/circuitcc/blob/codegen-dev/images/selector.png?raw=true)
