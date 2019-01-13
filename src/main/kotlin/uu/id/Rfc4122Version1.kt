package uu.id

import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.atomic.AtomicLong

private val leastSignificantBits = clockSequenceAndNode()
private val lastTimestamp = AtomicLong(0L)

internal fun rfc4122Version1() = UUID(mostSignificantBits(), leastSignificantBits)

private fun mostSignificantBits(): Long {
    val timestamp = timestamp()
    var bs: Long = 0
    bs = bs or (0x0000_0000_ffff_ffff and timestamp shl 32)
    bs = bs or (0x0000_ffff_0000_0000 and timestamp).ushr(16)
    bs = bs or (0x0fff_0000_0000_0000 and timestamp).ushr(48)
    bs = bs or (0x0000_0000_0000_1000) // sets the version to 1.
    return bs
}

/**
 * 4.1.4.  Timestamp
 *
 *
 * The timestamp is a 60-bit value.  For UUID version 1, this is
 * represented by Coordinated Universal Time (UTC) as a count of 100-
 * nanosecond intervals since 00:00:00.00, 15 October 1582 (the date of
 * Gregorian reform to the Christian calendar).
 */
private fun timestamp(): Long {
    // we are limited to generating 10k UUIDs/ms (ten-million/s), so we may have to block
    while (true) {
        val now = now()
        val last = lastTimestamp.get()
        if (now > last) {
            if (lastTimestamp.compareAndSet(last, now)) {
                return now
            }
        } else {
            val lastMillis = milliseconds(last)
            if (milliseconds(now) < lastMillis) {
                return lastTimestamp.incrementAndGet()
            }
            val candidate = last + 1
            // If we've generated more than 10k uuid in that millisecond,
            // we restart the whole process until we get to the next millis.
            // Otherwise, we try use our candidate ... unless we've been
            // beaten by another thread in which case we try again.
            if (milliseconds(candidate) == lastMillis && lastTimestamp.compareAndSet(last, candidate)) {
                return candidate
            }
        }
    }
}

private fun milliseconds(timestamp: Long) = timestamp / 10000

private fun now() = (System.currentTimeMillis() - uuidUtcBaseTime.toEpochMilli()) * 10000

private fun clockSequenceAndNode(): Long {
    val clock = clockSequence()
    val node = node()
    var bs: Long = 0
    bs = bs or (clock and 0x0000_0000_0000_3FFF) shl 48
    // kotlin makes it difficult to express hexadecimal bitmaps, so here I would like 0x8000_0000_0000_0000
    // to represent a 64b binary value with high-order bit set to 1 followed by 63 0s. This would be an
    // unsigned Long, and Kotlin disallows assigning an unsigned Long literal to a reference of Long type.
    // This smart-arsed workaround evaluates the bitwise negation of the inverse bitmap: other similarly
    // bonkers implementations are possible, but I personally find this easiest to understand.
    bs = bs or 0x7fff_ffff_ffff_ffff.inv()
    bs = bs or node
    return bs
}

/**
 * 4.1.5.  Clock Sequence
 *
 *
 * For UUID version 1, the clock sequence is used to help avoid
 * duplicates that could arise when the clock is set backwards in time
 * or if the node ID changes.
 *
 *
 * If the clock is set backwards, or might have been set backwards
 * (e.g., while the system was powered off), and the UUID generator can
 * not be sure that no UUIDs were generated with timestamps larger than
 * the value to which the clock was set, then the clock sequence has to
 * be changed.  If the previous value of the clock sequence is known, it
 * can just be incremented; otherwise it should be set to a random or
 * high-quality pseudo-random value.
 */
private fun clockSequence() = random(System.currentTimeMillis())

private fun random(seed: Long) = Random(seed).nextLong()

/**
 * 4.1.6.  Node
 *
 *
 * For UUID version 1, the node field consists of an IEEE 802 MAC
 * address, usually the host address.  For systems with multiple IEEE
 * 802 addresses, any available one can be used.  The lowest addressed
 * octet (octet number 10) contains the global/local bit and the
 * unicast/multicast bit, and is the first octet of the address
 * transmitted on an 802.3 LAN.
 */
private fun node() = try {
    macAddress()
} catch (e: UnknownHostException) {
    // For systems with no IEEE address, a randomly or pseudo-randomly
    // generated value may be used; see Section 4.5.
    randomAddress()
} catch (e: SocketException) {
    randomAddress()
}

private fun macAddress(): Long {
    val ip = InetAddress.getLocalHost()
    val network = NetworkInterface.getByInetAddress(ip)
        ?: throw SocketException("No interface found")
    val mac = network.hardwareAddress ?: throw SocketException("No hardware address found")
    return bytesToLong(mac, 6, reverse = true)
}

private fun randomAddress(): Long {
    val digest = try {
        MessageDigest.getInstance("MD5")
    } catch (e: NoSuchAlgorithmException) {
        throw InternalError("MD5 not supported", e)
    }
    allLocalAddresses().forEach { update(digest, it) }
    val properties = System.getProperties()
    update(digest, properties.getProperty("java.vendor"))
    update(digest, properties.getProperty("java.vendor.url"))
    update(digest, properties.getProperty("java.version"))
    update(digest, properties.getProperty("os.arch"))
    update(digest, properties.getProperty("os.name"))
    update(digest, properties.getProperty("os.version"))
    val hash = digest.digest()
    val node = bytesToLong(hash, 6)
    // The multicast bit must be set (1) in pseudo-random addresses, in order that they will never
    // conflict with addresses obtained from network cards.
    //                                 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    //                multicast bit -> |M        node (0-1)            |
    // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    // |                         node (2-5)                            |
    // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    return node or 0x0000_0100_0000_0000
}

private fun allLocalAddresses(): Set<String> {
    val results: MutableSet<String> = mutableSetOf()
    val localhost = localhost()
    results.add(localhost.toString())
    // Also return the hostname if available, it won't hurt (this does a dns lookup, it's only done once at startup)
    results.add(localhost.canonicalHostName)
    val localIps = localAliases(localhost)
    for (ip in localIps) {
        results.add(ip.toString())
    }
    val ips = ipAddresses().map { it.toString() }
    results.addAll(ips)
    return results
}

private fun localhost() = try {
    InetAddress.getLocalHost()!!
} catch (e: UnknownHostException) {
    // todo --- what does the RFC say should be done in this case?
    InetAddress.getLoopbackAddress()!!
}

private fun localAliases(localhost: InetAddress): Array<InetAddress> = try {
    InetAddress.getAllByName(localhost.canonicalHostName) ?: arrayOf()
} catch (e: UnknownHostException) {
    // Ignore, we'll try the network interfaces anyway
    arrayOf()
}

private fun ipAddresses(): Set<InetAddress> {
    return networkInterfaces()
        .map { it.inetAddresses }
        .flatMap { it.toList() }
        .toSet()
}

private fun networkInterfaces() = try {
    NetworkInterface.getNetworkInterfaces().toList()
} catch (e: SocketException) {
    // If we've really got nothing so far, we'll end up throwing an exception
    listOf<NetworkInterface>()
}

private fun update(digest: MessageDigest, value: String) = digest.update(value.toByteArray(StandardCharsets.UTF_8))

private fun bytesToLong(bs: ByteArray, length: Int, reverse: Boolean = false): Long {
    var result: Long = 0
    val leftShift = length - 1
    val bitmask: Long = if (reverse) 0xff else 0x0000_0000_0000_00ff
    for (i in 0 until length) {
        result = result or (bitmask and bs[i].toLong() shl (if (reverse) leftShift - i else i) * 8)
    }
    return result
}
