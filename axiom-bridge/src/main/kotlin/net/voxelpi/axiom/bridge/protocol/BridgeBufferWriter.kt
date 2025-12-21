package net.voxelpi.axiom.bridge.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class BridgeBufferWriter(
    private val data: ByteArray,
) {
    constructor(capacity: Int) : this(ByteArray(capacity))

    private val buffer: ByteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

    fun writeUInt8(value: UByte) {
        buffer.put(value.toByte())
    }

    fun writeUInt16(value: UShort) {
        buffer.putShort(value.toShort())
    }

    fun writeUInt32(value: UInt) {
        buffer.putInt(value.toInt())
    }

    fun writeUInt64(value: ULong) {
        buffer.putLong(value.toLong())
    }

    fun writeUInt8Array(value: ByteArray) {
        buffer.put(value)
    }

    fun writeUInt8Array(value: UByteArray) {
        buffer.put(value.toByteArray())
    }

    fun writeString8(value: String) {
        require(value.length <= UByte.MAX_VALUE.toInt()) { "String too long: $value" }
        writeUInt8(value.length.toUByte())

        val bytes = value.toByteArray(Charsets.UTF_8)
        buffer.put(bytes)
    }

    fun writeString16(value: String) {
        require(value.length <= UShort.MAX_VALUE.toInt()) { "String too long: $value" }
        writeUInt16(value.length.toUShort())

        val bytes = value.toByteArray(Charsets.UTF_8)
        buffer.put(bytes)
    }

    fun writtenData(): ByteArray {
        val length = buffer.position()
        val writtenData = ByteArray(length)

        buffer.flip()
        buffer.get(writtenData)
        return writtenData
    }
}
