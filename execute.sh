printf "Lexing invalidId.circuit\n"
java -jar build/libs/circuitcc.jar samples/invalidId.circuit

printf "\nLexing nand.circuit\n"
java -jar build/libs/circuitcc.jar samples/nand.circuit

printf "\nLexing orGate.circuit\n"
java -jar build/libs/circuitcc.jar samples/orGate.circuit

printf "\nLexing register.circuit\n"
java -jar build/libs/circuitcc.jar samples/register.circuit

printf "\nLexing selector.circuit\n"
java -jar build/libs/circuitcc.jar samples/selector.circuit
