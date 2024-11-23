package edu.columbia.circuitc.codegen.sim

interface SimConstruct
data class SimComponent(val name: String, val x: Int, val y: Int, val properties: Map<String, String>): SimConstruct

data class SimCircuit(val name: String, val components: List<SimComponent>, val wires: List<SimWire>): SimConstruct {
    val version = "1.9.2b"
    val globalBitSize = 1
    val clockSpeed = 1
}

data class SimWire(val x: Int, val y: Int, val length: Int, val isHorizontal: Boolean)
