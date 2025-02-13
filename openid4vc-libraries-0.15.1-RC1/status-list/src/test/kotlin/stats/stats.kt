/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.ssi.statuslist

import com.nimbusds.jose.jwk.Curve.P_256
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import de.bdr.openid4vc.common.signing.JwkSigner
import de.bdr.openid4vc.statuslist.StatusList
import de.bdr.openid4vc.statuslist.StatusListRegistry.StatusTypes.INVALID
import de.bdr.openid4vc.statuslist.StatusListToken
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.time.Instant
import java.util.zip.DeflaterOutputStream

val signer = JwkSigner(ECKeyGenerator(P_256).generate())

fun main() {
    val stats = Path.of("stats.csv")
    val capacities = listOf(10000, 20000, 100000, 1000000)
    val revokedRates = listOf(0.01, 0.015, 0.02, 0.05)
    val runs = 30
    var index = 0
    val amount = capacities.size * revokedRates.size * runs
    Files.newBufferedWriter(stats).use { out ->
        out.write("index,capacity,revoked,size,compressedSize")
        capacities.forEach { capacity ->
            revokedRates.forEach { revoked ->
                for (i in 1..runs) {
                    out.write(result(index++, capacity, revoked).toString())
                    out.write("\n")
                    println("%d%%".format((index + 1) * 100 / amount))
                }
            }
        }
    }
}

fun result(index: Int, capacity: Int, revoked: Double): Result {
    val serialized = makeStatusList(capacity, revoked)
    val compressed = compress(serialized)
    return Result(index, capacity, revoked, serialized.size, compressed.size)
}

private val random = SecureRandom()

fun makeStatusList(capacity: Int, revoked: Double): ByteArray {
    val statusList = StatusList(capacity, 2)
    for (i in 0 until capacity) {
        if (random.nextDouble() <= revoked) {
            statusList.set(i, INVALID.v)
        }
    }
    return StatusListToken(
            issuerUri = "https://statuslist.bundesdruckerei.de/cf72a49",
            statusListUri = "https://statuslist.bundesdruckerei.de/cf72a49/83",
            issuedAt = Instant.now(),
            statusList = statusList,
        )
        .asJwt(signer)
        .serialize()
        .toByteArray()
}

fun compress(serialized: ByteArray): ByteArray {
    val bytes = ByteArrayOutputStream()
    DeflaterOutputStream(bytes).use { it.write(serialized) }
    return bytes.toByteArray()
}

class Result(
    val index: Int,
    val capacity: Int,
    val revoked: Double,
    val size: Int,
    val compressedSize: Int
) {
    override fun toString() = "$index,$capacity,$revoked,$size,$compressedSize"
}
