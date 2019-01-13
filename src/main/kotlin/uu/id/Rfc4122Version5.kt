package uu.id

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or

internal fun rfc4122Version5(namespace: UUIDs.NS, name: String) = rfc4122Version5(namespace, name.toByteArray())

internal fun rfc4122Version5(namespace: UUIDs.NS, name: ByteArray): UUID {
    val digest = try {
        MessageDigest.getInstance("SHA1")
    } catch (e: NoSuchAlgorithmException) {
        throw InternalError("SHA1 not supported", e)
    }
    digest.update(bytes(namespace.namespace) + name)
    val sha1Bytes = digest.digest()
    sha1Bytes[6] = sha1Bytes[6] and 0x0f       // clear version
    sha1Bytes[6] = sha1Bytes[6] or 0x50        // set to version 5
    sha1Bytes[8] = sha1Bytes[8] and 0x3f       // clear variant
    sha1Bytes[8] = sha1Bytes[8] or 0x7f.inv()  // set to variant 2
    return UUIDs.uuid(sha1Bytes)
}
