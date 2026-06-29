package com.snifinder.app.util

import java.util.Random

/**
 * Complete Cloudflare IP ranges (official from cloudflare.com/ips-v4).
 * Generates random IPs from ALL Cloudflare CIDR ranges.
 */
object CloudflareIpProvider {

    /**
     * All official Cloudflare IPv4 CIDR ranges
     * Source: https://www.cloudflare.com/ips-v4/
     */
    data class CidrRange(
        val base1: Int, val base2: Int, val base3: Int, val base4: Int,
        val mask: Int
    )

    val allCidrRanges = listOf(
        // 173.245.48.0/20
        CidrRange(173, 245, 48, 0, 20),
        // 103.21.244.0/22
        CidrRange(103, 21, 244, 0, 22),
        // 103.22.200.0/22
        CidrRange(103, 22, 200, 0, 22),
        // 103.31.4.0/22
        CidrRange(103, 31, 4, 0, 22),
        // 141.101.64.0/18
        CidrRange(141, 101, 64, 0, 18),
        // 108.162.192.0/18
        CidrRange(108, 162, 192, 0, 18),
        // 190.93.240.0/20
        CidrRange(190, 93, 240, 0, 20),
        // 188.114.96.0/20
        CidrRange(188, 114, 96, 0, 20),
        // 197.234.240.0/22
        CidrRange(197, 234, 240, 0, 22),
        // 198.41.128.0/17
        CidrRange(198, 41, 128, 0, 17),
        // 162.158.0.0/15
        CidrRange(162, 158, 0, 0, 15),
        // 104.16.0.0/13
        CidrRange(104, 16, 0, 0, 13),
        // 104.24.0.0/14
        CidrRange(104, 24, 0, 0, 14),
        // 172.64.0.0/13
        CidrRange(172, 64, 0, 0, 13),
        // 131.0.72.0/22
        CidrRange(131, 0, 72, 0, 22)
    )

    /**
     * Generate a random IP from a CIDR range
     */
    private fun randomIpFromCidr(cidr: CidrRange, random: Random): String {
        val baseIp = (cidr.base1 shl 24) or (cidr.base2 shl 16) or (cidr.base3 shl 8) or cidr.base4
        val hostBits = 32 - cidr.mask
        val hostCount = (1 shl hostBits) - 2 // exclude network and broadcast
        if (hostCount <= 0) return "${cidr.base1}.${cidr.base2}.${cidr.base3}.${cidr.base4}"

        val hostPart = random.nextInt(hostCount) + 1 // 1 to hostCount
        val ip = baseIp or hostPart

        val a = (ip shr 24) and 0xFF
        val b = (ip shr 16) and 0xFF
        val c = (ip shr 8) and 0xFF
        val d = ip and 0xFF

        return "$a.$b.$c.$d"
    }

    /**
     * Generate random IPs distributed across ALL Cloudflare ranges
     */
    fun generateRandomIps(count: Int = 100): List<String> {
        val random = Random()
        val result = mutableSetOf<String>()

        // Distribute evenly across ranges, plus some extra random picks
        val perRange = count / allCidrRanges.size
        val remainder = count % allCidrRanges.size

        for (cidr in allCidrRanges) {
            repeat(perRange) {
                result.add(randomIpFromCidr(cidr, random))
            }
        }

        // Fill remainder with random picks from random ranges
        repeat(remainder + (count - result.size).coerceAtLeast(0)) {
            val cidr = allCidrRanges[random.nextInt(allCidrRanges.size)]
            result.add(randomIpFromCidr(cidr, random))
        }

        // Ensure we hit target count
        while (result.size < count) {
            val cidr = allCidrRanges[random.nextInt(allCidrRanges.size)]
            result.add(randomIpFromCidr(cidr, random))
        }

        return result.toList().shuffled()
    }

    /**
     * Some well-known clean IPs (community tested, good starting points)
     */
    val knownCleanIps = listOf(
        "1.1.1.1", "1.0.0.1", "1.1.1.2", "1.0.0.2",
        "104.16.0.1", "104.17.0.1", "104.18.0.1", "104.19.0.1",
        "104.20.0.1", "104.21.0.1", "104.22.0.1", "104.23.0.1",
        "104.24.0.1", "104.25.0.1", "104.26.0.1", "104.27.0.1",
        "172.64.0.1", "172.65.0.1", "172.66.0.1", "172.67.0.1",
        "162.158.0.1", "162.159.0.1",
        "198.41.128.1", "198.41.192.1", "198.41.200.1",
        "188.114.96.1", "188.114.97.1", "188.114.98.1", "188.114.99.1",
        "141.101.64.1", "141.101.96.1", "141.101.112.1",
        "108.162.192.1", "108.162.224.1", "108.162.240.1",
        "190.93.240.1", "190.93.244.1", "190.93.248.1",
        "173.245.48.1", "173.245.56.1", "173.245.60.1",
        "103.21.244.1", "103.22.200.1", "103.31.4.1",
        "197.234.240.1", "197.234.242.1",
        "131.0.72.1", "131.0.74.1"
    )

    /**
     * Get IPs: known clean + random from all ranges
     */
    fun getAllIps(randomCount: Int = 100): List<String> {
        return (knownCleanIps + generateRandomIps(randomCount)).distinct()
    }

    /**
     * Generate random IPs from CIDR strings (e.g. "104.16.0.0/13")
     * Used when ranges are fetched from remote
     */
    fun generateFromCidrStrings(cidrs: List<String>, perRange: Int = 10): List<Pair<String, List<String>>> {
        val random = Random()
        return cidrs.mapNotNull { cidr ->
            parseCidrString(cidr)?.let { parsed ->
                val ips = (1..perRange).map { randomIpFromCidr(parsed, random) }.distinct()
                cidr to ips
            }
        }
    }

    private fun parseCidrString(cidr: String): CidrRange? {
        return try {
            val parts = cidr.split("/")
            val mask = parts[1].trim().toInt()
            val ipParts = parts[0].trim().split(".")
            CidrRange(
                ipParts[0].toInt(),
                ipParts[1].toInt(),
                ipParts[2].toInt(),
                ipParts[3].toInt(),
                mask
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generate IPs grouped by range. Each group has IPs from one CIDR range.
     * This allows smart scanning: skip a range if too many failures.
     */
    fun getIpsGroupedByRange(perRange: Int = 10): List<Pair<String, List<String>>> {
        val random = Random()
        return allCidrRanges.map { cidr ->
            val rangeName = "${cidr.base1}.${cidr.base2}.${cidr.base3}.0/${cidr.mask}"
            val ips = (1..perRange).map { randomIpFromCidr(cidr, random) }.distinct()
            rangeName to ips
        }
    }

    /**
     * Get range description for an IP (for display)
     */
    fun getRangeInfo(ip: String): String {
        val parts = ip.split(".")
        if (parts.size != 4) return ""
        val a = parts[0].toIntOrNull() ?: return ""
        val b = parts[1].toIntOrNull() ?: return ""
        return when {
            a == 104 && b in 16..23 -> "104.16.0.0/13"
            a == 104 && b in 24..27 -> "104.24.0.0/14"
            a == 172 && b in 64..71 -> "172.64.0.0/13"
            a == 162 && b in 158..159 -> "162.158.0.0/15"
            a == 198 && b == 41 -> "198.41.128.0/17"
            a == 188 && b == 114 -> "188.114.96.0/20"
            a == 141 && b == 101 -> "141.101.64.0/18"
            a == 108 && b == 162 -> "108.162.192.0/18"
            a == 190 && b == 93 -> "190.93.240.0/20"
            a == 173 && b == 245 -> "173.245.48.0/20"
            a == 103 && b == 21 -> "103.21.244.0/22"
            a == 103 && b == 22 -> "103.22.200.0/22"
            a == 103 && b == 31 -> "103.31.4.0/22"
            a == 197 && b == 234 -> "197.234.240.0/22"
            a == 131 && b == 0 -> "131.0.72.0/22"
            else -> ""
        }
    }
}
