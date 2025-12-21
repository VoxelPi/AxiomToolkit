package net.voxelpi.axiom.bridge

@JvmRecord
public data class AxiomBridgeInfo(
    val protocolVersion: UInt,
    val version: String,
    val gitVersion: String,
)
