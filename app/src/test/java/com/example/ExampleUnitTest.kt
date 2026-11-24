package com.example

import com.example.security.SecretChatCrypto
import com.example.util.QrCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun secretChatCrypto_roundTripEncryption() {
        val key = SecretChatCrypto.generateFallbackSessionKeyHex()
        val plaintext = "Hello ChatX Secret World!"
        val ciphertext = SecretChatCrypto.encrypt(plaintext, key)
        assertNotEquals(plaintext, ciphertext)
        assertTrue(ciphertext.startsWith("E2EE:"))
        assertEquals(plaintext, SecretChatCrypto.decrypt(ciphertext, key))
    }

    @Test
    fun qrCodeGenerator_producesSquareMatrix() {
        val matrix = QrCodeGenerator.encode("chatx://user/test?phone=+919876543210")
        assertTrue(matrix.isNotEmpty())
        assertEquals(matrix.size, matrix[0].size)
    }
}
