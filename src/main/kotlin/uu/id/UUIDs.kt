/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uu.id

import java.lang.Long.toBinaryString
import java.time.Instant
import java.util.*
import java.util.UUID

@Suppress("unused") object UUIDs {

    private val nilUUID = UUID(0, 0)

    enum class NS(val namespace: UUID) {
        DNS(UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")),
        URL(UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8")),
        OID(UUID.fromString("6ba7b812-9dad-11d1-80b4-00c04fd430c8")),
        X500(UUID.fromString("6ba7b814-9dad-11d1-80b4-00c04fd430c8"))
    }

    // Utilities for Version 1 UUID timestamps

    @JvmStatic fun realTimestamp(uuid: UUID): Instant = when {
        uuid.version() == 1 -> realTimestamp(uuid.timestamp())
        else -> throw UnsupportedOperationException("Not a time-based UUID")
    }

    @JvmStatic fun realTimestamp(timestamp: Long): Instant =
        Instant.ofEpochMilli(timestamp / 10000).plusMillis(uuidUtcBaseTime.toEpochMilli())

    // Utilities for printing UUIDs

    @JvmStatic fun toBinaryString(uuid: UUID): String = formatBinaryString(
        toBinaryString(uuid.mostSignificantBits) +
                toBinaryString(uuid.leastSignificantBits)
    )

    private fun formatBinaryString(binaryString: String): String {
        return binaryString.substring(0..31) + '-' +
                binaryString.substring(32..47) + '-' +
                binaryString.substring(48..63) + '-' +
                binaryString.substring(64..79) + '-' +
                binaryString.substring(80..127)
    }

    // Comparison of UUIDs as per RFC 4122 implementation notes

    @ExperimentalUnsignedTypes @JvmStatic fun comparator(): Comparator<UUID> = Comparator { c1, c2 ->
        val bsx = ubytes(c1!!)
        val bsy = ubytes(c2!!)
        assert(bsx.indices == bsy.indices)
        for (i in bsx.indices) {
            if (bsx[i] == bsy[i]) continue
            else return@Comparator bsx[i].compareTo(bsy[i])
        }
        0
    }

    // Constructors and Factories

    // RFC 4122 Constructors

    // Version 1

    @JvmStatic fun timeBasedUUID(): UUID = v1UUID()

    @JvmStatic fun v1UUID(): UUID = rfc4122Version1()

    // Version 3

    @JvmStatic fun nameBasedUUID(namespace: NS, name: String): UUID = v3UUID(namespace, name)

    @JvmStatic fun md5UUID(namespace: NS, name: String): UUID = v3UUID(namespace, name)

    @JvmStatic fun v3UUID(namespace: NS, name: String): UUID = rfc4122Version3(namespace, name)

    // Version 4

    @JvmStatic fun randomUUID(): UUID = v4UUID()

    @JvmStatic fun v4UUID(): UUID = rfc4122Version4()

    // Version 5

    @JvmStatic fun sha1UUID(namespace: NS, name: String): UUID = rfc4122Version5(namespace, name)

    @JvmStatic fun v5UUID(namespace: NS, name: String): UUID = rfc4122Version5(namespace, name)

    // Version 0

    /**
     * Nil UUID
     *
     * The nil UUID is special form of UUID that is specified to have all
     * 128 bits set to zero.
     */
    @JvmStatic fun nilUUID(): UUID = nilUUID  // RFC 4122 4.1.7.  Nil UUID

    // Convenience Constructors

    @JvmStatic fun uuid(data: ByteArray): UUID = UUID(bytes(data, 0..7).long, bytes(data, 8..15).long)

    @JvmStatic fun uuid(name: String): UUID = UUID.fromString(name)
}
