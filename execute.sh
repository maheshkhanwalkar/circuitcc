printf "Compiling invalidId.circuit\n"
java -jar build/libs/circuitcc.jar samples/invalidId.circuit

printf "\nCompiling nand.circuit\n"
java -jar build/libs/circuitcc.jar samples/nand.circuit
cat samples/nand.sim

printf "\nCompiling orGate.circuit\n"
java -jar build/libs/circuitcc.jar samples/orGate.circuit
cat samples/orGate.sim

printf "\nCompiling register.circuit\n"
java -jar build/libs/circuitcc.jar samples/register.circuit
cat samples/register.sim

printf "\nCompiling selector.circuit\n"
java -jar build/libs/circuitcc.jar samples/selector.circuit
cat samples/selector.sim
