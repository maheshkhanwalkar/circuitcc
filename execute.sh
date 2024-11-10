printf "Lexing invalidId.circuit"
java -jar build/libs/circuitcc.jar samples/invalidId.circuit

printf "\nLexing nand.circuit"
java -jar build/libs/circuitcc.jar samples/nand.circuit

printf "\nLexing orGate.circuit"
java -jar build/libs/circuitcc.jar samples/orGate.circuit

printf "\nLexing register.circuit"
java -jar build/libs/circuitcc.jar samples/register.circuit

printf "\nLexing selector.circuit"
java -jar build/libs/circuitcc.jar samples/selector.circuit
