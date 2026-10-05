package com.example

import com.example.crypto.CryptoEngine
import com.example.crypto.PasswordStrengthEvaluator
import com.example.crypto.PasswordStrengthLevel
import com.example.crypto.VaultCryptoException
import com.example.crypto.VaultMetadata
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class VaultCryptoUnitTest {

    @Test
    fun testPasswordEvaluator() {
        val empty = PasswordStrengthEvaluator.evaluate("")
        assertEquals(PasswordStrengthLevel.EMPTY, empty.level)

        val weak = PasswordStrengthEvaluator.evaluate("12345")
        assertEquals(PasswordStrengthLevel.WEAK, weak.level)

        val strong = PasswordStrengthEvaluator.evaluate("MyV@ultX2026SecureP@ss!")
        assertTrue(strong.level == PasswordStrengthLevel.STRONG || strong.level == PasswordStrengthLevel.EXCELLENT)
        assertTrue(strong.hasUpper)
        assertTrue(strong.hasLower)
        assertTrue(strong.hasDigit)
        assertTrue(strong.hasSymbol)
    }

    @Test
    fun testMetadataSerialization() {
        val original = VaultMetadata(
            originalFileName = "confidential_document.pdf",
            originalFileSize = 1048576L,
            originalMimeType = "application/pdf",
            createdAtTimestamp = 1700000000000L
        )

        val bytes = original.serialize()
        val deserialized = VaultMetadata.deserialize(bytes)

        assertEquals(original.originalFileName, deserialized.originalFileName)
        assertEquals(original.originalFileSize, deserialized.originalFileSize)
        assertEquals(original.originalMimeType, deserialized.originalMimeType)
        assertEquals(original.createdAtTimestamp, deserialized.createdAtTimestamp)
    }

    @Test
    fun testEncryptAndDecryptSuccess() = runBlocking {
        val plainText = "VaultX: Local Zero-Knowledge Authenticated Encryption for Android Devices.".repeat(100)
        val plainBytes = plainText.toByteArray(StandardCharsets.UTF_8)
        val password = "SuperSecretMasterKey123!".toCharArray()

        val metadata = VaultMetadata(
            originalFileName = "test_note.txt",
            originalFileSize = plainBytes.size.toLong(),
            originalMimeType = "text/plain",
            createdAtTimestamp = System.currentTimeMillis()
        )

        val encOut = ByteArrayOutputStream()
        CryptoEngine.encryptStream(
            inputStream = ByteArrayInputStream(plainBytes),
            outputStream = encOut,
            password = password,
            metadata = metadata,
            onProgress = { _, _ -> }
        )

        val cipherBytes = encOut.toByteArray()
        assertTrue(cipherBytes.size > plainBytes.size)

        val decOut = ByteArrayOutputStream()
        val restoredMeta = CryptoEngine.decryptStream(
            inputStream = ByteArrayInputStream(cipherBytes),
            outputStream = decOut,
            password = password,
            onProgress = { _, _ -> }
        )

        assertEquals("test_note.txt", restoredMeta.originalFileName)
        assertEquals(plainBytes.size.toLong(), restoredMeta.originalFileSize)
        assertArrayEquals(plainBytes, decOut.toByteArray())
    }

    @Test
    fun testWrongPasswordThrowsTamperError() = runBlocking {
        val plainBytes = "Secret bank records".toByteArray(StandardCharsets.UTF_8)
        val correctPassword = "CorrectPassword123!".toCharArray()
        val wrongPassword = "WrongPassword999!".toCharArray()

        val metadata = VaultMetadata(
            originalFileName = "bank.txt",
            originalFileSize = plainBytes.size.toLong(),
            originalMimeType = "text/plain",
            createdAtTimestamp = System.currentTimeMillis()
        )

        val encOut = ByteArrayOutputStream()
        CryptoEngine.encryptStream(
            inputStream = ByteArrayInputStream(plainBytes),
            outputStream = encOut,
            password = correctPassword,
            metadata = metadata,
            onProgress = { _, _ -> }
        )

        val decOut = ByteArrayOutputStream()
        try {
            CryptoEngine.decryptStream(
                inputStream = ByteArrayInputStream(encOut.toByteArray()),
                outputStream = decOut,
                password = wrongPassword,
                onProgress = { _, _ -> }
            )
            fail("Expected VaultCryptoException on wrong password")
        } catch (e: VaultCryptoException) {
            assertTrue(e.message?.contains("Unable to decrypt") == true)
        }
    }

    @Test
    fun testCorruptedCiphertextThrowsTamperError() = runBlocking {
        val plainBytes = "Sensitive legal agreement".toByteArray(StandardCharsets.UTF_8)
        val password = "StrongLegalPassword!42".toCharArray()

        val metadata = VaultMetadata(
            originalFileName = "agreement.docx",
            originalFileSize = plainBytes.size.toLong(),
            originalMimeType = "application/msword",
            createdAtTimestamp = System.currentTimeMillis()
        )

        val encOut = ByteArrayOutputStream()
        CryptoEngine.encryptStream(
            inputStream = ByteArrayInputStream(plainBytes),
            outputStream = encOut,
            password = password,
            metadata = metadata,
            onProgress = { _, _ -> }
        )

        val corruptedBytes = encOut.toByteArray()
        // Corrupt a byte in the encrypted chunk
        corruptedBytes[corruptedBytes.size - 5] = (corruptedBytes[corruptedBytes.size - 5] + 1).toByte()

        val decOut = ByteArrayOutputStream()
        try {
            CryptoEngine.decryptStream(
                inputStream = ByteArrayInputStream(corruptedBytes),
                outputStream = decOut,
                password = password,
                onProgress = { _, _ -> }
            )
            fail("Expected VaultCryptoException on corrupted file")
        } catch (e: VaultCryptoException) {
            assertTrue(e.message?.contains("Unable to decrypt") == true)
        }
    }
}
