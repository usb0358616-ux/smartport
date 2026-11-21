package com.example.domain.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Generates and protects the SQLCipher database passphrase using Android Keystore AES-256-GCM.
 * Raw passphrase bytes are never stored in plaintext on disk.
 */
object EncryptionKeyManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "border_ports_db_master_key"
    private const val PREFS_NAME = "sovereign_keystore_prefs"
    private const val PREF_ENCRYPTED_PASS = "enc_db_pass_v1"
    private const val PREF_IV = "enc_db_iv_v1"

    fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingEnc = prefs.getString(PREF_ENCRYPTED_PASS, null)
        val existingIv = prefs.getString(PREF_IV, null)

        val secretKey = getOrCreateKeystoreSecretKey()
        if (existingEnc != null && existingIv != null) {
            return try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(128, Base64.decode(existingIv, Base64.NO_WRAP))
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                cipher.doFinal(Base64.decode(existingEnc, Base64.NO_WRAP))
            } catch (_: Exception) {
                createAndStoreNewPassphrase(prefs, secretKey)
            }
        }
        return createAndStoreNewPassphrase(prefs, secretKey)
    }

    private fun createAndStoreNewPassphrase(
        prefs: android.content.SharedPreferences,
        secretKey: SecretKey
    ): ByteArray {
        val rawPassphrase = ByteArray(32)
        SecureRandom().nextBytes(rawPassphrase)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(rawPassphrase)
        prefs.edit()
            .putString(PREF_ENCRYPTED_PASS, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(PREF_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
        return rawPassphrase
    }

    private fun getOrCreateKeystoreSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
