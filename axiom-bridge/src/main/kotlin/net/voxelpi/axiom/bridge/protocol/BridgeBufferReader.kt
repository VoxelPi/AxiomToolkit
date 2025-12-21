package net.voxelpi.axiom.bridge.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class BridgeBufferReader(
    val data: ByteArray,
) {
    private val buffer: ByteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

    fun readUInt8(): UByte {
        return buffer.get().toUByte()
    }

    fun readUInt16(): UShort {
        return buffer.getShort().toUShort()
    }

    fun readUInt32(): UInt {
        return buffer.getInt().toUInt()
    }

    fun readUInt64(): ULong {
        return buffer.getLong().toULong()
    }

    fun readString8(): String {
        val length = readUInt8().toInt()
        val bytes = ByteArray(length)
        buffer.get(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    fun readString16(): String {
        val length = readUInt16().toInt()
        val bytes = ByteArray(length)
        buffer.get(bytes)
        return String(bytes, Charsets.UTF_8)
    }
}
