package edu.columbia.circuitc.codegen.sim

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

enum class PointOrientation {
    NORTH, SOUTH, EAST, WEST
}

data class WirePoint(val x: Int, val y: Int, val orientation: PointOrientation)

interface SimConstruct

data class SimContainer(val circuits: List<SimCircuit>): SimConstruct {
    val version = "1.9.2b"
    val globalBitSize = 1
    val clockSpeed = 1
}

data class SimComponent(val name: String, val x: Int, val y: Int, val properties: Map<String, String>,
                        @JsonIgnore val inPositions: List<WirePoint>, @JsonIgnore val outPosition: List<WirePoint>): SimConstruct

data class SimCircuit(val name: String, val components: List<SimConstruct>, val wires: List<SimWire>): SimConstruct
data class SimWire(val x: Int, val y: Int, val length: Int, @get:JsonProperty("isHorizontal") val isHorizontal: Boolean)
