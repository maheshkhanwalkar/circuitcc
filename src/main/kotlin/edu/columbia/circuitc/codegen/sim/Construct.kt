package edu.columbia.circuitc.codegen.sim

interface SimConstruct
interface SimComponent: SimConstruct

data class SimCircuit(val name: String, val components: List<SimComponent>, val wires: List<SimWire>): SimConstruct
data class SimWire(val x: Int, val y: Int, val length: Int, val isHorizontal: Boolean)
