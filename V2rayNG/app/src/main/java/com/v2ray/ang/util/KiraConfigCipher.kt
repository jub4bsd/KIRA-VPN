package com.v2ray.ang.util

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Portable, password-encrypted KIRA profile files.
 *
 * The passphrase is deliberately not embedded in the app or the file. Share it with recipients
 * through a separate private channel.
 */
object KiraConfigCipher {
    const val MIME_TYPE = "application/vnd.kira.vpn-config"
    const val FILE_EXTENSION = ".kira"
    const val MIN_PASSPHRASE_LENGTH = 12
    const val MAX_PASSPHRASE_CHARS = 1024
    const val MAX_EXPORT_FILE_NAME_CHARS = 100
    const val MAX_PROFILE_NAME_CHARS = 64
    const val MAX_PLAINTEXT_BYTES = 256 * 1024
    const val MAX_FILE_BYTES = MAX_PLAINTEXT_BYTES + 64

    fun exportFileName(name: String): String {
        val trimmedName = name.trim()
        val withoutExtension = if (
            trimmedName.endsWith(FILE_EXTENSION, ignoreCase = true)
        ) {
            trimmedName.dropLast(FILE_EXTENSION.length)
        } else {
            trimmedName
        }
        val safeBaseName = withoutExtension
            .filter { !it.isISOControl() && it !in "/\\:*?\"<>|" }
            .trim()
            .trimStart('.')
            .take(MAX_EXPORT_FILE_NAME_CHARS)
            .trimEnd('.', ' ')
            .ifBlank { "KIRA VPN" }
        return safeBaseName + FILE_EXTENSION
    }

    fun normalizeProfileName(name: String): String? {
        val normalizedName = name.trim()
        return normalizedName.takeIf {
            it.isNotEmpty() &&
                it.length <= MAX_PROFILE_NAME_CHARS &&
                it.none { character -> character.isISOControl() }
        }
    }

    fun profileNameFromFileName(fileName: String?): String {
        val name = fileName.orEmpty()
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .trim()
        val baseName = if (name.endsWith(FILE_EXTENSION, ignoreCase = true)) {
            name.dropLast(FILE_EXTENSION.length)
        } else {
            name
        }
        val safeName = baseName
            .filter { !it.isISOControl() && it !in "/\\:*?\"<>|" }
            .trim()
            .take(MAX_PROFILE_NAME_CHARS)
            .trim()
        return normalizeProfileName(safeName) ?: "KIRA VPN"
    }

    private val magic = "KIRACFG\u0001".toByteArray(StandardCharsets.US_ASCII)
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 600_000
    private const val KEY_BITS = 256

    fun encrypt(plaintext: ByteArray, passphrase: String): ByteArray {
        requireValidPassphrase(passphrase)
        require(plaintext.isNotEmpty() && plaintext.size <= MAX_PLAINTEXT_BYTES) {
            "Invalid KIRA configuration size"
        }

        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val iv = ByteArray(IV_BYTES).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(magic)
        return magic + salt + iv + cipher.doFinal(plaintext)
    }

    fun decrypt(fileBytes: ByteArray, passphrase: String): ByteArray {
        requireValidPassphrase(passphrase)
        val headerSize = magic.size + SALT_BYTES + IV_BYTES
        require(
            fileBytes.size in (headerSize + TAG_BITS / 8)..MAX_FILE_BYTES &&
                fileBytes.copyOfRange(0, magic.size).contentEquals(magic)
        ) {
            "Invalid KIRA configuration file"
        }

        val saltStart = magic.size
        val ivStart = saltStart + SALT_BYTES
        val cipherStart = ivStart + IV_BYTES
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            deriveKey(passphrase, fileBytes.copyOfRange(saltStart, ivStart)),
            GCMParameterSpec(TAG_BITS, fileBytes.copyOfRange(ivStart, cipherStart)),
        )
        cipher.updateAAD(magic)
        val plaintext = cipher.doFinal(fileBytes.copyOfRange(cipherStart, fileBytes.size))
        require(plaintext.isNotEmpty() && plaintext.size <= MAX_PLAINTEXT_BYTES) {
            "Invalid KIRA configuration size"
        }
        return plaintext
    }

    private fun requireValidPassphrase(passphrase: String) {
        require(passphrase.length in MIN_PASSPHRASE_LENGTH..MAX_PASSPHRASE_CHARS) {
            "Invalid passphrase length"
        }
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val passwordBytes = passphrase.toByteArray(StandardCharsets.UTF_8)
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(passwordBytes, "HmacSHA256"))
            val derivedKey = pbkdf2HmacSha256(mac, salt, PBKDF2_ITERATIONS, KEY_BITS / 8)
            try {
                SecretKeySpec(derivedKey, "AES")
            } finally {
                derivedKey.fill(0)
            }
        } finally {
            passwordBytes.fill(0)
        }
    }

    internal fun pbkdf2HmacSha256(
        mac: Mac,
        salt: ByteArray,
        iterations: Int,
        keyLength: Int,
    ): ByteArray {
        require(iterations > 0 && keyLength > 0)
        val hmacLength = mac.macLength
        val blockCount = (keyLength + hmacLength - 1) / hmacLength
        val derivedKey = ByteArray(keyLength)
        var outputOffset = 0

        for (blockIndex in 1..blockCount) {
            val blockInput = salt + byteArrayOf(
                (blockIndex ushr 24).toByte(),
                (blockIndex ushr 16).toByte(),
                (blockIndex ushr 8).toByte(),
                blockIndex.toByte(),
            )
            var current = mac.doFinal(blockInput)
            val block = current.copyOf()
            repeat(iterations - 1) {
                val next = mac.doFinal(current)
                current.fill(0)
                current = next
                for (index in block.indices) {
                    block[index] = (block[index].toInt() xor current[index].toInt()).toByte()
                }
            }
            val copyLength = minOf(block.size, keyLength - outputOffset)
            block.copyInto(derivedKey, outputOffset, 0, copyLength)
            outputOffset += copyLength
            blockInput.fill(0)
            block.fill(0)
            current.fill(0)
        }
        return derivedKey
    }
}
