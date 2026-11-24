package com.example.security

import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Real cryptographic engine for ChatX Secret Chats:
 * - ECDH P-256 (secp256r1) key exchange
 * - SHA-256 session key derivation + 60-digit Safety Number fingerprint
 * - AES-256-GCM authenticated encryption with per-message random 96-bit IV
 */
object SecretChatCrypto {

    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12
    private val secureRandom = SecureRandom()

    data class KeyPairEncoded(
        val publicKeyBase64: String,
        val privateKeyBase64: String
    )

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim()
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    fun generateEcKeyPair(): KeyPairEncoded {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        val pair = kpg.generateKeyPair()
        return KeyPairEncoded(
            publicKeyBase64 = bytesToHex(pair.public.encoded),
            privateKeyBase64 = bytesToHex(pair.private.encoded)
        )
    }

    fun deriveSharedKeyHex(myPrivateKeyBase64: String, peerPublicKeyBase64: String): String {
        val kf = java.security.KeyFactory.getInstance("EC")
        val priv: PrivateKey = kf.generatePrivate(
            PKCS8EncodedKeySpec(hexToBytes(myPrivateKeyBase64))
        )
        val pub: PublicKey = kf.generatePublic(
            X509EncodedKeySpec(hexToBytes(peerPublicKeyBase64))
        )
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(priv)
        agreement.doPhase(pub, true)
        val sharedSecret = agreement.generateSecret()
        val digest = MessageDigest.getInstance("SHA-256").digest(sharedSecret)
        return bytesToHex(digest)
    }

    fun generateFallbackSessionKeyHex(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return bytesToHex(bytes)
    }

    fun computeSafetyNumber(keyHex: String, peerId: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest("$keyHex:$peerId".toByteArray(Charsets.UTF_8))
        val digits = StringBuilder()
        for (i in 0 until 30) {
            val b = hash[i % hash.size].toInt() and 0xFF
            val twoDigits = (b % 100)
            digits.append("%02d".format(twoDigits))
        }
        return digits.toString().chunked(5).joinToString(" ")
    }

    private fun keyFromHex(hex: String): SecretKey {
        val clean = hex.trim()
        val bytes = if (clean.length == 64) {
            hexToBytes(clean)
        } else {
            MessageDigest.getInstance("SHA-256").digest(clean.toByteArray(Charsets.UTF_8))
        }
        return SecretKeySpec(bytes, "AES")
    }

    fun encrypt(plaintext: String, sessionKeyHex: String): String {
        val iv = ByteArray(IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyFromHex(sessionKeyHex), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return "E2EE:${bytesToHex(iv)}:${bytesToHex(cipherBytes)}"
    }

    fun decrypt(payload: String, sessionKeyHex: String): String {
        if (!payload.startsWith("E2EE:") || sessionKeyHex.isBlank()) return payload
        return try {
            val parts = payload.split(":")
            if (parts.size != 3) return payload
            val iv = hexToBytes(parts[1])
            val cipherBytes = hexToBytes(parts[2])
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keyFromHex(sessionKeyHex), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            "[Encrypted message — unable to decrypt with current session key]"
        }
    }

    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest("ChatX_PIN_$pin".toByteArray(Charsets.UTF_8))
        return bytesToHex(digest)
    }
}
