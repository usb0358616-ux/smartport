package com.example.domain.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    fun generateSaltBase64(): String {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return salt.toHexString()
    }

    fun hashPassword(password: String, saltHex: String): String {
        val salt = hexToBytes(saltHex)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val hash = factory.generateSecret(spec).encoded
            hash.toHexString()
        } finally {
            spec.clearPassword()
        }
    }

    fun verifyPassword(password: String, saltHex: String, expectedHashHex: String): Boolean {
        val computed = hashPassword(password, saltHex)
        val a = computed.toByteArray(Charsets.UTF_8)
        val b = expectedHashHex.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(a, b)
    }

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.toHexString()
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim()
        require(clean.length % 2 == 0) { "Invalid hex length" }
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
