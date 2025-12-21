package net.voxelpi.axiom.bridge

import net.voxelpi.axiom.arch.Architecture

public class AxiomBridgeLink internal constructor(
    private val architecture: Architecture,
    internal val connection: AxiomBridgeConnection,
) : AutoCloseable {

    override fun close() {
        connection.close()
    }

    public suspend fun fetchInfo(): Result<AxiomBridgeInfo> = runCatching {
        connection.sendPacketWithId(PACKET_ID_INFO) {}

        val response = connection.receiveSinglePacket().getOrThrow()

        val id = response.readUInt8()
        check(id == PACKET_ID_INFO_RESPONSE.toUByte()) { "Invalid response to info request" }

        val protocolVersion = response.readUInt32()
        val version = response.readString16()
        val gitVersion = response.readString16()

        return Result.success(AxiomBridgeInfo(protocolVersion, version, gitVersion))
    }

    public suspend fun uploadProgram(data: ByteArray): Result<Unit> {
        val architectureProgramByteCount = architecture.programSize * architecture.instructionWordType.bytes.toULong()

        // Check program length.
        if (data.size.toULong() != architectureProgramByteCount) {
            return Result.failure(IllegalArgumentException("Invalid program size. Must be ${architecture.programSize}"))
        }

        // Split data into chunks.
        val chunks = data.toList().chunked(1024).map { it.toByteArray() }
        check(chunks.size.toULong() == architectureProgramByteCount / 1024UL) { "Data too long" }

        val chunkUsed = chunks.map { chunk -> !chunk.all { it == 0.toByte() } }

        // Send program header.
        connection.sendPacketWithId(PACKET_ID_UPLOAD_PROGRAM_START) {
            chunkUsed
                .chunked(64)
                .map { chunkUsedChunk ->
                    var chunkPresentData: ULong = 0UL
                    for ((bit, used) in chunkUsedChunk.withIndex()) {
                        if (used) {
                            chunkPresentData = chunkPresentData or (1UL shl bit)
                        }
                    }
                    chunkPresentData
                }
                .forEach(this::writeUInt64)
        }

        // Send chunk.
        for ((chunkIndex, chunk) in chunks.withIndex()) {
            if (!chunkUsed[chunkIndex]) {
                continue
            }
            connection.sendPacketWithId(PACKET_ID_UPLOAD_PROGRAM_CHUNK) {
                writeUInt16(chunkIndex.toUShort())
                writeUInt8Array(chunk) // Always 1024 bytes.
            }
        }

        // Send program end.
        connection.sendPacketWithId(PACKET_ID_UPLOAD_PROGRAM_END) {}

        // Wait for the response.
        // delay(100)
        val response = connection.receiveSinglePacket().getOrElse {
            return Result.failure(it)
        }
        val id = response.readUInt8().toInt()
        check(id == PACKET_ID_UPLOAD_PROGRAM_RESPONSE) { "Received invalid response to program end packet." }

        val valid = response.readUInt8() != 0.toUByte()
        check(valid) { "Invalid chunk uploaded" }

        for (i in 0..<(chunks.size / 8)) {
            val data = response.readUInt8().toInt()
            check(data == 0) { "Failed to upload chunk" }
        }

        return Result.success(Unit)
    }

    public companion object {
        private const val PACKET_ID_INFO = 0x01
        private const val PACKET_ID_INFO_RESPONSE = 0x01
        private const val PACKET_ID_UPLOAD_PROGRAM_START = 0x10
        private const val PACKET_ID_UPLOAD_PROGRAM_CHUNK = 0x11
        private const val PACKET_ID_UPLOAD_PROGRAM_END = 0x12
        private const val PACKET_ID_UPLOAD_PROGRAM_RESPONSE = 0x13
    }
}
