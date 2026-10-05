package com.example.crypto

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.Arrays

data class VaultHeader(
    val version: Short,
    val kdfAlgo: Byte,
    val iterations: Int,
    val salt: ByteArray,
    val baseNonce: ByteArray,
    val chunkSize: Int,
    val encryptedMetadata: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VaultHeader
        if (version != other.version) return false
        if (kdfAlgo != other.kdfAlgo) return false
        if (iterations != other.iterations) return false
        if (!salt.contentEquals(other.salt)) return false
        if (!baseNonce.contentEquals(other.baseNonce)) return false
        if (chunkSize != other.chunkSize) return false
        if (!encryptedMetadata.contentEquals(other.encryptedMetadata)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = version.toInt()
        result = 31 * result + kdfAlgo.toInt()
        result = 31 * result + iterations
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + baseNonce.contentHashCode()
        result = 31 * result + chunkSize
        result = 31 * result + encryptedMetadata.contentHashCode()
        return result
    }
}

data class VaultMetadata(
    val originalFileName: String,
    val originalFileSize: Long,
    val originalMimeType: String,
    val createdAtTimestamp: Long
) {
    fun serialize(): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)
        val nameBytes = originalFileName.toByteArray(StandardCharsets.UTF_8)
        dos.writeShort(nameBytes.size)
        dos.write(nameBytes)
        dos.writeLong(originalFileSize)
        val mimeBytes = originalMimeType.toByteArray(StandardCharsets.UTF_8)
        dos.writeShort(mimeBytes.size)
        dos.write(mimeBytes)
        dos.writeLong(createdAtTimestamp)
        dos.flush()
        return bos.toByteArray()
    }

    companion object {
        fun deserialize(bytes: ByteArray): VaultMetadata {
            val dis = DataInputStream(ByteArrayInputStream(bytes))
            val nameLen = dis.readUnsignedShort()
            val nameBytes = ByteArray(nameLen)
            dis.readFully(nameBytes)
            val name = String(nameBytes, StandardCharsets.UTF_8)
            val size = dis.readLong()
            val mimeLen = dis.readUnsignedShort()
            val mimeBytes = ByteArray(mimeLen)
            dis.readFully(mimeBytes)
            val mime = String(mimeBytes, StandardCharsets.UTF_8)
            val timestamp = dis.readLong()
            return VaultMetadata(name, size, mime, timestamp)
        }
    }
}

object VaultContainerFormat {
    fun writeHeader(
        outputStream: OutputStream,
        header: VaultHeader
    ) {
        val dos = DataOutputStream(outputStream)
        // 1. Magic
        dos.write(CryptoConstants.MAGIC)
        // 2. Version
        dos.writeShort(header.version.toInt())
        // 3. KDF Algo
        dos.writeByte(header.kdfAlgo.toInt())
        // 4. Iterations
        dos.writeInt(header.iterations)
        // 5. Salt
        dos.writeShort(header.salt.size)
        dos.write(header.salt)
        // 6. Base Nonce
        dos.writeShort(header.baseNonce.size)
        dos.write(header.baseNonce)
        // 7. Chunk size
        dos.writeInt(header.chunkSize)
        // 8. Encrypted Metadata
        dos.writeInt(header.encryptedMetadata.size)
        dos.write(header.encryptedMetadata)
        dos.flush()
    }

    fun readHeader(inputStream: InputStream): VaultHeader {
        val dis = DataInputStream(inputStream)
        val magic = ByteArray(CryptoConstants.MAGIC.size)
        dis.readFully(magic)
        if (!Arrays.equals(magic, CryptoConstants.MAGIC)) {
            throw IllegalArgumentException(CryptoConstants.ERROR_INVALID_HEADER)
        }

        val version = dis.readShort()
        if (version != CryptoConstants.CURRENT_VERSION) {
            throw IllegalArgumentException(CryptoConstants.ERROR_UNSUPPORTED_VERSION)
        }

        val kdfAlgo = dis.readByte()
        val iterations = dis.readInt()

        val saltLen = dis.readShort().toInt()
        if (saltLen <= 0 || saltLen > 1024) throw IllegalArgumentException("Corrupted salt length")
        val salt = ByteArray(saltLen)
        dis.readFully(salt)

        val nonceLen = dis.readShort().toInt()
        if (nonceLen != CryptoConstants.BASE_NONCE_SIZE_BYTES) throw IllegalArgumentException("Corrupted nonce length")
        val baseNonce = ByteArray(nonceLen)
        dis.readFully(baseNonce)

        val chunkSize = dis.readInt()
        if (chunkSize <= 0 || chunkSize > 10 * 1024 * 1024) throw IllegalArgumentException("Invalid chunk size")

        val metaLen = dis.readInt()
        if (metaLen <= 0 || metaLen > 65536) throw IllegalArgumentException("Corrupted metadata block")
        val encMetadata = ByteArray(metaLen)
        dis.readFully(encMetadata)

        return VaultHeader(
            version = version,
            kdfAlgo = kdfAlgo,
            iterations = iterations,
            salt = salt,
            baseNonce = baseNonce,
            chunkSize = chunkSize,
            encryptedMetadata = encMetadata
        )
    }
}
