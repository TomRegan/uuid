package uu.id

import java.util.*

internal fun rfc4122Version3(namespace: UUIDs.NS, name: String) = rfc4122Version3(namespace, name.toByteArray())

internal fun rfc4122Version3(namespace: UUIDs.NS, name: ByteArray) =
    UUID.nameUUIDFromBytes(bytes(namespace.namespace) + name)!!

