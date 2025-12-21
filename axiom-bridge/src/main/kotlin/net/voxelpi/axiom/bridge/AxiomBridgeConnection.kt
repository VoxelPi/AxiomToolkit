package net.voxelpi.axiom.bridge

import com.fazecast.jSerialComm.SerialPort
import net.voxelpi.axiom.bridge.protocol.BridgeBufferReader
import net.voxelpi.axiom.bridge.protocol.BridgeBufferWriter
import net.voxelpi.axiom.bridge.util.inputChannel
import net.voxelpi.axiom.bridge.util.receiveArray
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

internal class AxiomBridgeConnection(
    private val port: SerialPort,
) : AutoCloseable {

    private val inputChannel = port.inputChannel()
    private val outputStream = port.outputStream

    override fun close() {
        outputStream.close()
        port.closePort()
    }

    fun sendPacket(payload: BridgeBufferWriter) {
        val payloadBytes = payload.writtenData()

        // Calculate the size.
        val size = payloadBytes.size
        val sizeBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(size).array()

        // Compute the packet hash.
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(sizeBytes)
        digest.update(payloadBytes)
        val hashBytes = digest.digest()

        // Send the packet.
        outputStream.write(hashBytes)
        outputStream.write(sizeBytes)
        outputStream.write(payloadBytes)
        outputStream.flush()
    }

    fun sendPacket(builder: BridgeBufferWriter.() -> Unit) {
        val payload = BridgeBufferWriter(MAX_PACKET_PAYLOAD_SIZE).apply(builder)
        sendPacket(payload)
    }

    fun sendPacketWithId(packetId: Int, builder: BridgeBufferWriter.() -> Unit) {
        val payload = BridgeBufferWriter(MAX_PACKET_PAYLOAD_SIZE)
            .apply { writeUInt8(packetId.toUByte()) }
            .apply(builder)
        sendPacket(payload)
    }

    suspend fun receiveSinglePacket(): Result<BridgeBufferReader> = runCatching {
        // Read packet hash.
        val hashBytes = inputChannel.receiveArray(32)

        // Read packet size.
        val sizeBytes = inputChannel.receiveArray(4)
        val size = ByteBuffer.wrap(sizeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt()
        check(size >= 0) { "Invalid payload size $size" }

        // Read packet payload.
        val payload = inputChannel.receiveArray(size)

        // Compute the packet hash.
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(sizeBytes)
        digest.update(payload)
        val computedHashBytes = digest.digest()

        // Verify the packet hash.
        check(hashBytes.contentEquals(computedHashBytes)) { "Hash mismatch" }

        // Return the payload
        return@runCatching BridgeBufferReader(payload)
    }

    companion object {
        const val MAX_PACKET_PAYLOAD_SIZE = 2048
    }
}
