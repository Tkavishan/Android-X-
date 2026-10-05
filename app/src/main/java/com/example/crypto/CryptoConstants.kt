package com.example.crypto

object CryptoConstants {
    val MAGIC = byteArrayOf('V'.code.toByte(), 'A'.code.toByte(), 'U'.code.toByte(), 'L'.code.toByte(), 'X'.code.toByte())
    const val CURRENT_VERSION: Short = 1

    const val KDF_ALGO_PBKDF2_SHA256: Byte = 1
    const val PBKDF2_ITERATIONS: Int = 15_000
    const val KEY_SIZE_BITS: Int = 256
    const val SALT_SIZE_BYTES: Int = 32
    const val BASE_NONCE_SIZE_BYTES: Int = 12
    const val GCM_TAG_LENGTH_BITS: Int = 128
    const val GCM_TAG_LENGTH_BYTES: Int = 16

    const val DEFAULT_CHUNK_SIZE_BYTES: Int = 256 * 1024 // 256 KB high-throughput streaming chunk
    const val FILE_EXTENSION: String = ".vaultx"
    const val MIME_TYPE: String = "application/octet-stream"

    const val ERROR_DECRYPTION_FAILED = "Unable to decrypt. Incorrect password or file is corrupted."
    const val ERROR_INVALID_HEADER = "Invalid file format. Not a recognized VaultX container."
    const val ERROR_UNSUPPORTED_VERSION = "Unsupported container version."
}
