package com.example.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.coroutineContext

class VaultCryptoException(message: String, cause: Throwable? = null) : Exception(message, cause)

object CryptoEngine {
    private val secureRandom = SecureRandom()

    fun generateSalt(): ByteArray {
        val salt = ByteArray(CryptoConstants.SALT_SIZE_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun generateBaseNonce(): ByteArray {
        val nonce = ByteArray(CryptoConstants.BASE_NONCE_SIZE_BYTES)
        secureRandom.nextBytes(nonce)
        return nonce
    }

    fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int = CryptoConstants.PBKDF2_ITERATIONS
    ): SecretKey {
        return try {
            val keySpec = PBEKeySpec(password, salt, iterations, CryptoConstants.KEY_SIZE_BITS)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = factory.generateSecret(keySpec).encoded
            keySpec.clearPassword()
            val secretKey = SecretKeySpec(keyBytes, "AES")
            Arrays.fill(keyBytes, 0.toByte())
            secretKey
        } catch (e: Exception) {
            throw VaultCryptoException("Failed to derive encryption key: ${e.message}", e)
        }
    }

    private fun deriveChunkIv(baseNonce: ByteArray, chunkIndex: Int): ByteArray {
        val chunkIv = ByteArray(12)
        System.arraycopy(baseNonce, 0, chunkIv, 0, 8)
        chunkIv[8] = ((chunkIndex ushr 24) and 0xFF).toByte()
        chunkIv[9] = ((chunkIndex ushr 16) and 0xFF).toByte()
        chunkIv[10] = ((chunkIndex ushr 8) and 0xFF).toByte()
        chunkIv[11] = (chunkIndex and 0xFF).toByte()
        return chunkIv
    }

    private fun buildAad(chunkIndex: Int, isLastChunk: Boolean): ByteArray {
        val aad = ByteArray(11)
        val magic = "VAULTX".toByteArray(StandardCharsets.US_ASCII)
        System.arraycopy(magic, 0, aad, 0, 6)
        aad[6] = ((chunkIndex ushr 24) and 0xFF).toByte()
        aad[7] = ((chunkIndex ushr 16) and 0xFF).toByte()
        aad[8] = ((chunkIndex ushr 8) and 0xFF).toByte()
        aad[9] = (chunkIndex and 0xFF).toByte()
        aad[10] = if (isLastChunk) 1.toByte() else 0.toByte()
        return aad
    }

    suspend fun encryptStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        password: CharArray,
        metadata: VaultMetadata,
        onProgress: (progress: Float, bytesProcessed: Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val bufferedIn = BufferedInputStream(inputStream, CryptoConstants.DEFAULT_CHUNK_SIZE_BYTES)
        val bufferedOut = BufferedOutputStream(outputStream, CryptoConstants.DEFAULT_CHUNK_SIZE_BYTES)

        val salt = generateSalt()
        val baseNonce = generateBaseNonce()
        val secretKey = deriveKey(password, salt)

        // 1. Encrypt Metadata
        val metaIv = deriveChunkIv(baseNonce, -1)
        val metaAad = buildAad(-1, false)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(CryptoConstants.GCM_TAG_LENGTH_BITS, metaIv))
        cipher.updateAAD(metaAad)
        val encMetadata = cipher.doFinal(metadata.serialize())

        // 2. Write Container Header
        val header = VaultHeader(
            version = CryptoConstants.CURRENT_VERSION,
            kdfAlgo = CryptoConstants.KDF_ALGO_PBKDF2_SHA256,
            iterations = CryptoConstants.PBKDF2_ITERATIONS,
            salt = salt,
            baseNonce = baseNonce,
            chunkSize = CryptoConstants.DEFAULT_CHUNK_SIZE_BYTES,
            encryptedMetadata = encMetadata
        )
        VaultContainerFormat.writeHeader(bufferedOut, header)

        // 3. Stream File In Chunks with 1-chunk lookahead and throttled progress to avoid UI lag
        val dos = DataOutputStream(bufferedOut)
        var totalBytesRead: Long = 0
        var chunkIndex = 0
        val totalSize = if (metadata.originalFileSize > 0) metadata.originalFileSize else 1L

        var lastProgressTime = 0L
        var lastReportedPercent = -1

        fun reportProgress(isFinal: Boolean = false) {
            val now = System.currentTimeMillis()
            val progress = (totalBytesRead.toFloat() / totalSize).coerceIn(0f, 1f)
            val percent = (progress * 100).toInt()
            if (isFinal || percent != lastReportedPercent && now - lastProgressTime > 120L) {
                lastProgressTime = now
                lastReportedPercent = percent
                onProgress(progress, totalBytesRead)
            }
        }

        var currentChunk: ByteArray? = readChunkBlock(bufferedIn, CryptoConstants.DEFAULT_CHUNK_SIZE_BYTES)

        if (currentChunk == null) {
            // Handle 0-byte file securely
            val emptyIv = deriveChunkIv(baseNonce, 0)
            val emptyAad = buildAad(0, true)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(CryptoConstants.GCM_TAG_LENGTH_BITS, emptyIv))
            cipher.updateAAD(emptyAad)
            val enc = cipher.doFinal(ByteArray(0))
            dos.writeInt(enc.size)
            dos.writeBoolean(true)
            dos.write(enc)
            dos.flush()
            onProgress(1f, 0L)
        } else {
            while (currentChunk != null) {
                coroutineContext.ensureActive()
                val nextChunk = readChunkBlock(bufferedIn, CryptoConstants.DEFAULT_CHUNK_SIZE_BYTES)
                val isLastChunk = (nextChunk == null)

                val chunkIv = deriveChunkIv(baseNonce, chunkIndex)
                val chunkAad = buildAad(chunkIndex, isLastChunk)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(CryptoConstants.GCM_TAG_LENGTH_BITS, chunkIv))
                cipher.updateAAD(chunkAad)
                val encryptedChunk = cipher.doFinal(currentChunk)

                dos.writeInt(encryptedChunk.size)
                dos.writeBoolean(isLastChunk)
                dos.write(encryptedChunk)

                totalBytesRead += currentChunk.size
                reportProgress(isLastChunk)

                chunkIndex++
                currentChunk = nextChunk
            }
        }
        dos.flush()
        bufferedOut.flush()
    }

    private fun readChunkBlock(inputStream: InputStream, chunkSize: Int): ByteArray? {
        val buffer = ByteArray(chunkSize)
        var bytesRead = 0
        while (bytesRead < chunkSize) {
            val r = inputStream.read(buffer, bytesRead, chunkSize - bytesRead)
            if (r == -1) break
            bytesRead += r
        }
        if (bytesRead == 0) return null
        return if (bytesRead == chunkSize) buffer else buffer.copyOf(bytesRead)
    }

    suspend fun inspectVaultHeader(
        inputStream: InputStream,
        password: CharArray
    ): VaultMetadata = withContext(Dispatchers.IO) {
        val header = try {
            VaultContainerFormat.readHeader(inputStream)
        } catch (e: Exception) {
            throw VaultCryptoException(CryptoConstants.ERROR_INVALID_HEADER, e)
        }

        val secretKey = deriveKey(password, header.salt, header.iterations)
        val metaIv = deriveChunkIv(header.baseNonce, -1)
        val metaAad = buildAad(-1, false)

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(CryptoConstants.GCM_TAG_LENGTH_BITS, metaIv))
            cipher.updateAAD(metaAad)
            val decryptedBytes = cipher.doFinal(header.encryptedMetadata)
            VaultMetadata.deserialize(decryptedBytes)
        } catch (e: GeneralSecurityException) {
            throw VaultCryptoException(CryptoConstants.ERROR_DECRYPTION_FAILED, e)
        }
    }

    suspend fun decryptStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        password: CharArray,
        onProgress: (progress: Float, bytesProcessed: Long) -> Unit
    ): VaultMetadata = withContext(Dispatchers.IO) {
        val bufferedIn = BufferedInputStream(inputStream, CryptoConstants.DEFAULT_CHUNK_SIZE_BYTES)
        val bufferedOut = BufferedOutputStream(outputStream, CryptoConstants.DEFAULT_CHUNK_SIZE_BYTES)

        val header = try {
            VaultContainerFormat.readHeader(bufferedIn)
        } catch (e: Exception) {
            throw VaultCryptoException(CryptoConstants.ERROR_INVALID_HEADER, e)
        }

        val secretKey = deriveKey(password, header.salt, header.iterations)

        // Decrypt & verify metadata first (authenticates password and file structure)
        val metadata = try {
            val metaIv = deriveChunkIv(header.baseNonce, -1)
            val metaAad = buildAad(-1, false)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(CryptoConstants.GCM_TAG_LENGTH_BITS, metaIv))
            cipher.updateAAD(metaAad)
            val decryptedBytes = cipher.doFinal(header.encryptedMetadata)
            VaultMetadata.deserialize(decryptedBytes)
        } catch (e: GeneralSecurityException) {
            throw VaultCryptoException(CryptoConstants.ERROR_DECRYPTION_FAILED, e)
        }

        // Stream and decrypt chunks
        val dis = DataInputStream(bufferedIn)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        var chunkIndex = 0
        var totalBytesWritten: Long = 0
        val targetSize = if (metadata.originalFileSize > 0) metadata.originalFileSize else 1L

        var lastProgressTime = 0L
        var lastReportedPercent = -1

        fun reportProgress(isFinal: Boolean = false) {
            val now = System.currentTimeMillis()
            val progress = (totalBytesWritten.toFloat() / targetSize).coerceIn(0f, 1f)
            val percent = (progress * 100).toInt()
            if (isFinal || percent != lastReportedPercent && now - lastProgressTime > 120L) {
                lastProgressTime = now
                lastReportedPercent = percent
                onProgress(progress, totalBytesWritten)
            }
        }

        while (true) {
            coroutineContext.ensureActive()

            val chunkSize = try {
                dis.readInt()
            } catch (e: java.io.EOFException) {
                break
            }

            if (chunkSize <= 0 || chunkSize > 10 * 1024 * 1024) {
                throw VaultCryptoException(CryptoConstants.ERROR_DECRYPTION_FAILED)
            }

            val isLastChunk = dis.readBoolean()
            val chunkCiphertext = ByteArray(chunkSize)
            dis.readFully(chunkCiphertext)

            val chunkIv = deriveChunkIv(header.baseNonce, chunkIndex)
            val chunkAad = buildAad(chunkIndex, isLastChunk)

            val decryptedChunk = try {
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(CryptoConstants.GCM_TAG_LENGTH_BITS, chunkIv))
                cipher.updateAAD(chunkAad)
                cipher.doFinal(chunkCiphertext)
            } catch (e: GeneralSecurityException) {
                throw VaultCryptoException(CryptoConstants.ERROR_DECRYPTION_FAILED, e)
            }

            bufferedOut.write(decryptedChunk)
            totalBytesWritten += decryptedChunk.size

            reportProgress(isLastChunk)
            chunkIndex++

            if (isLastChunk) {
                break
            }
        }
        bufferedOut.flush()
        metadata
    }
}
