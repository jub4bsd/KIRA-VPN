package com.v2ray.ang.util

import javax.crypto.AEADBadTagException
import javax.crypto.Mac
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.spec.SecretKeySpec

class KiraConfigCipherTest {
    private val passphrase = "a-long-private-passphrase"
    private val plaintext = "vless://example-profile".toByteArray()

    @Test
    fun encryptedFileRoundTripsAndDoesNotContainPlaintext() {
        val file = KiraConfigCipher.encrypt(plaintext, passphrase)

        assertArrayEquals(plaintext, KiraConfigCipher.decrypt(file, passphrase))
        assertFalse(file.toString(Charsets.ISO_8859_1).contains("vless://"))
        assertTrue(file.size <= KiraConfigCipher.MAX_FILE_BYTES)
    }

    @Test
    fun pbkdf2MatchesKnownHmacSha256Vector() {
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec("password".toByteArray(), "HmacSHA256"))
        }

        assertArrayEquals(
            "120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b".hexToBytes(),
            KiraConfigCipher.pbkdf2HmacSha256(mac, "salt".toByteArray(), 1, 32),
        )
    }

    @Test(expected = AEADBadTagException::class)
    fun wrongPassphraseIsRejected() {
        KiraConfigCipher.decrypt(KiraConfigCipher.encrypt(plaintext, passphrase), "another-private-passphrase")
    }

    @Test(expected = AEADBadTagException::class)
    fun modifiedCiphertextIsRejected() {
        val file = KiraConfigCipher.encrypt(plaintext, passphrase)
        file[file.lastIndex] = (file.last().toInt() xor 1).toByte()

        KiraConfigCipher.decrypt(file, passphrase)
    }

    @Test(expected = IllegalArgumentException::class)
    fun shortPassphraseCannotCreateAFile() {
        KiraConfigCipher.encrypt(plaintext, "too-short")
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidHeaderIsRejected() {
        KiraConfigCipher.decrypt(ByteArray(64), passphrase)
    }

    @Test
    fun exportFileNameUsesTheChosenNameAndAddsOnlyOneExtension() {
        assertEquals("My profile.kira", KiraConfigCipher.exportFileName("My profile.kira"))
        assertEquals("My profile.kira", KiraConfigCipher.exportFileName("My profile"))
    }

    @Test
    fun exportFileNameRemovesPathCharactersAndUsesFallbackWhenEmpty() {
        assertEquals("profile.kira", KiraConfigCipher.exportFileName("../profile"))
        assertEquals("KIRA VPN.kira", KiraConfigCipher.exportFileName("../"))
    }

    @Test
    fun profileNameIsTrimmedAndRejectsBlankOrTooLongValues() {
        assertEquals("New node", KiraConfigCipher.normalizeProfileName("  New node  "))
        assertEquals(null, KiraConfigCipher.normalizeProfileName("   "))
        assertEquals(
            null,
            KiraConfigCipher.normalizeProfileName(
                "x".repeat(KiraConfigCipher.MAX_PROFILE_NAME_CHARS + 1)
            ),
        )
    }

    @Test
    fun profileNameFromFileNameUsesTheFileNameWithoutExtension() {
        assertEquals("My profile", KiraConfigCipher.profileNameFromFileName("My profile.kira"))
        assertEquals("My profile", KiraConfigCipher.profileNameFromFileName("My profile.KIRA"))
    }

    @Test
    fun profileNameFromFileNameFallsBackAndLimitsTheName() {
        assertEquals("KIRA VPN", KiraConfigCipher.profileNameFromFileName(".kira"))
        assertEquals("KIRA VPN", KiraConfigCipher.profileNameFromFileName(null))
        assertEquals("x".repeat(KiraConfigCipher.MAX_PROFILE_NAME_CHARS),
            KiraConfigCipher.profileNameFromFileName("x".repeat(80) + ".kira"))
    }

    @Test
    fun encryptedExportRetainsTheChosenUnicodeName() {
        listOf("YOUTUBE 🇺🇲", "YOUTUBE 🇧🇪").forEach { fileName ->
            val profileName = KiraConfigCipher.exportProfileName(fileName)
            val payload = KiraConfigCipher.encodeExportPayload(profileName, "vless://example-profile")
            val encrypted = KiraConfigCipher.encrypt(payload, passphrase)
            val imported = KiraConfigCipher.decodeExportPayload(
                KiraConfigCipher.decrypt(encrypted, passphrase)
            )
            assertEquals(fileName, imported.profileName)
            assertEquals("vless://example-profile", imported.configText)
        }
    }

    @Test
    fun decodeExportPayloadKeepsLegacyPlainConfigurationsImportable() {
        val legacyConfig = "vless://legacy-profile"
        assertEquals(
            KiraConfigCipher.ExportPayload(null, legacyConfig),
            KiraConfigCipher.decodeExportPayload(legacyConfig.toByteArray()),
        )
    }

    private fun String.hexToBytes(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
