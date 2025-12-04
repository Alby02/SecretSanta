/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal

class CryptoManager(private val context: Context) {

    private val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private val KEYSTORE_ALIAS = "SECRET_SANTA_USER_KEY"

    // Symmetric encryption (AES)
    private val SYMMETRIC_ALGORITHM = "AES/GCM/NoPadding"
    private val SYMMETRIC_KEY_SIZE_BITS = 256
    private val IV_SIZE_BYTES = 12
    private val TAG_SIZE_BITS = 128

    // Key derivation (password to key)
    private val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private val PBKDF2_ITERATIONS = 100000 // A good standard
    private val SALT_SIZE_BYTES = 16

    private val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    fun doesPrivateKeyExist(): Boolean {
        return keyStore.containsAlias(KEYSTORE_ALIAS)
    }

    fun generateAndStoreNewKeyPair(): String {
        if (doesPrivateKeyExist()) {
            keyStore.deleteEntry(KEYSTORE_ALIAS)
        }

        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER
        )

        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(2048)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .setCertificateSubject(X500Principal("CN=SecretSantaUser"))
            .build()

        keyPairGenerator.initialize(spec)
        val keyPair = keyPairGenerator.generateKeyPair()

        return Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
    }

    private fun getPrivateKeyFromKeystore(): PrivateKey? {
        val entry = keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.PrivateKeyEntry
        return entry?.privateKey
    }

    fun encryptPrivateKeyForBackup(password: String): Pair<String, String> {
        val privateKey = getPrivateKeyFromKeystore()
            ?: throw IllegalStateException("Private key not found in Keystore to encrypt.")
        val privateKeyBytes = privateKey.encoded

        val salt = ByteArray(SALT_SIZE_BYTES)
        SecureRandom().nextBytes(salt)

        val keySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, SYMMETRIC_KEY_SIZE_BITS)
        val secretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val secretKeyBytes = secretKeyFactory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(secretKeyBytes, "AES")

        val iv = ByteArray(IV_SIZE_BYTES)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE_BITS, iv))
        val encryptedBytes = cipher.doFinal(privateKeyBytes)

        val combined = iv + encryptedBytes

        return Pair(
            Base64.encodeToString(combined, Base64.NO_WRAP),
            Base64.encodeToString(salt, Base64.NO_WRAP)
        )
    }

    fun recoverAndStorePrivateKey(password: String, saltBase64: String, encryptedKeyBase64: String) {
        if (doesPrivateKeyExist()) return

        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val encryptedDataWithIv = Base64.decode(encryptedKeyBase64, Base64.NO_WRAP)

        val keySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, SYMMETRIC_KEY_SIZE_BITS)
        val secretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val secretKeyBytes = secretKeyFactory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(secretKeyBytes, "AES")

        val iv = encryptedDataWithIv.copyOfRange(0, IV_SIZE_BYTES)
        val encryptedBytes = encryptedDataWithIv.copyOfRange(IV_SIZE_BYTES, encryptedDataWithIv.size)

        val cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE_BITS, iv))
        val decryptedKeyBytes = cipher.doFinal(encryptedBytes)

        val rsaKeyFactory = java.security.KeyFactory.getInstance("RSA")
        val privateKeySpec = PKCS8EncodedKeySpec(decryptedKeyBytes)
        val privateKey = rsaKeyFactory.generatePrivate(privateKeySpec)

        // Store the recovered key. Note: An imported key may not be hardware-backed.
        // We pass a null certificate chain as we don't have the original.
        keyStore.setKeyEntry(KEYSTORE_ALIAS, privateKey, null, null)
    }

    // --- AES Helpers ---

    fun generateAesKey(): javax.crypto.SecretKey {
        val keyGenerator = javax.crypto.KeyGenerator.getInstance("AES")
        keyGenerator.init(SYMMETRIC_KEY_SIZE_BITS)
        return keyGenerator.generateKey()
    }

    fun encrypt(data: ByteArray, key: javax.crypto.SecretKey): ByteArray {
        val cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    fun decrypt(data: ByteArray, key: javax.crypto.SecretKey): ByteArray {
        val iv = data.copyOfRange(0, IV_SIZE_BYTES)
        val encrypted = data.copyOfRange(IV_SIZE_BYTES, data.size)
        val cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, iv))
        return cipher.doFinal(encrypted)
    }

    fun encrypt(data: ByteArray, publicKeyBase64: String): String {
         val publicBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
         val keySpec = java.security.spec.X509EncodedKeySpec(publicBytes)
         val keyFactory = java.security.KeyFactory.getInstance("RSA")
         val publicKey = keyFactory.generatePublic(keySpec)

         val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
         cipher.init(Cipher.ENCRYPT_MODE, publicKey)
         return Base64.encodeToString(cipher.doFinal(data), Base64.NO_WRAP)
    }
    
    fun decrypt(encryptedBase64: String): ByteArray {
        val privateKey = getPrivateKeyFromKeystore() ?: throw IllegalStateException("No private key found")
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(Base64.decode(encryptedBase64, Base64.NO_WRAP))
    }


    // --- Shamir's Secret Sharing (GF(2^8)) ---

    fun splitSecret(secret: ByteArray, n: Int, k: Int): List<ByteArray> {
        return ShamirSecretSharing.splitSecret(secret, n, k)
    }

    fun combineShares(shares: List<ByteArray>): ByteArray {
        return ShamirSecretSharing.combineShares(shares)
    }
}