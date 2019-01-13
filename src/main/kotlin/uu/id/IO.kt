package uu.id

import java.nio.ByteBuffer
import java.util.*

internal fun bytes(uuid: UUID): ByteArray {
    val result = ByteArray(16)
    val msb = uuid.mostSignificantBits
    val lsb = uuid.leastSignificantBits
    for (i in 0..7) {
        result[i] = (msb shr (7 - i) * 8 and 0xff).toByte()
    }
    for (i in 8..15) {
        result[i] = (lsb shr (15 - i) * 8 and 0xff).toByte()
    }
    return result
}

internal fun bytes(data: ByteArray, intRange: IntRange) = ByteBuffer.wrap(data.sliceArray(intRange))

@ExperimentalUnsignedTypes internal fun ubytes(uuid: UUID): UByteArray = bytes(uuid).toUByteArray()