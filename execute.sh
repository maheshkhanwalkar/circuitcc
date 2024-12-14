printf "Compiling deadCode.circuit\n"
java -jar build/libs/circuitcc.jar samples/deadCode.circuit
cat samples/deadCode.sim

printf "\nCompiling constantFolding.circuit\n"
java -jar build/libs/circuitcc.jar samples/constantFolding.circuit
cat samples/constantFolding.sim

printf "\nCompiling constantProp.circuit\n"
java -jar build/libs/circuitcc.jar samples/constantProp.circuit
cat samples/constantProp.sim

printf "\nCompiling copyProp.circuit\n"
java -jar build/libs/circuitcc.jar samples/copyProp.circuit
cat samples/copyProp.sim

printf "\nCompiling gateElim.circuit\n"
java -jar build/libs/circuitcc.jar samples/gateElim.circuit
cat samples/gateElim.sim
