package edu.columbia.circuitc.codegen

import edu.columbia.circuitc.codegen.sim.SimConstruct
import edu.columbia.circuitc.visitor.Visitor

// TODO: implement code generation and remove abstract qualifier
abstract class CodeGen: Visitor<SimConstruct> {
}
