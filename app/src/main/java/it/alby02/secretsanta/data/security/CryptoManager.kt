/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.data.security

import android.content.Context
import java.security.PrivateKey

// A stubbed-out CryptoManager. This is where your most complex
// client-side security logic will live.
class CryptoManager(private val context: Context) {

    private val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private val KEYSTORE_ALIAS = "SECRET_SANTA_USER_KEY"

    /**
     * Checks if the user's private key already exists in the Android Keystore.
     * This is used in LoginViewModel to decide if we need to run the recovery flow.
     */
    fun doesPrivateKeyExist(): Boolean {
        // TODO: Implement Android Keystore lookup
        // 1. val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        // 2. keyStore.load(null)
        // 3. return keyStore.containsAlias(KEYSTORE_ALIAS)
        println("CryptoManager: STUB - Checking if key exists... (returning false)")
        return false // Stub
    }

    /**
     * Generates a new RSA/ECC key pair and stores it in the Android Keystore.
     * This is used in SignUpViewModel (Flow 1).
     * @return The public key as a Base64-encoded string to be stored in Firestore.
     */
    fun generateAndStoreNewKeyPair(): String {
        // TODO: Implement KeyPairGenerator
        // 1. Use KeyPairGeneratorSpec for Android Keystore (API 23+)
        //    .setAlias(KEYSTORE_ALIAS)
        //    .setKeySize(2048)
        //    .setCertificateSubject(X500Principal("CN=SecretSantaUser"))
        //    .setKeyValidityEnd(Date(Long.MAX_VALUE))
        // 2. val generator = KeyPairGenerator.getInstance("RSA", KEYSTORE_PROVIDER)
        // 3. generator.initialize(spec)
        // 4. val keyPair = generator.generateKeyPair()
        // 5. Get public key: keyPair.public.encoded
        // 6. Base64-encode the public key and return it

        println("CryptoManager: STUB - Generating new key pair...")
        val publicKeyString = "---STUBBED-PUBLIC-KEY-BASE64---" // Stub
        return publicKeyString
    }

    /**
     * Retrieves the private key from the Android Keystore.
     * This is a helper function for encryptPrivateKeyForBackup.
     */
    private fun getPrivateKeyFromKeystore(): PrivateKey {
        // TODO: Implement Keystore private key retrieval
        // 1. val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        // 2. keyStore.load(null)
        // 3. val entry = keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.PrivateKeyEntry
        // 4. return entry.privateKey

        println("CryptoManager: STUB - Getting private key from Keystore...")
        // This is a STUB. In reality, you'd return a real PrivateKey object.
        // We throw an exception here to simulate it working.
        if (true) { // Simulate success
            // return mock(PrivateKey::class.java) // If you have mockito
        } else {
            throw Exception("Private key not found in Keystore.")
        }
        // This is not a real key, just for the stub logic
        val keyPair = java.security.KeyPairGenerator.getInstance("RSA").genKeyPair()
        return keyPair.private
    }

    /**
     * Derives a symmetric key from the password and uses it to encrypt the
     * newly generated private key (retrieved from Keystore).
     * This is also used in SignUpViewModel (Flow 1).
     * @return A Pair containing the (Base64) encrypted private key and the (Base64) salt used.
     */
    fun encryptPrivateKeyForBackup(password: String): Pair<String, String> {
        // TODO: Implement PBKDF2 and AES-GCM
        // 1. val privateKey = getPrivateKeyFromKeystore()
        // 2. Generate a new random salt (e.g., 16 bytes)
        // 3. Use PBKDF2WithHmacSHA256 (SecretKeyFactory) to derive an S-Key (AES 256) from password + salt
        // 4. Use AES-GCM (Cipher) to encrypt the privateKey.encoded
        // 5. Base64-encode the encrypted data (ciphertext + IV) and the salt

        println("CryptoManager: STUB - Encrypting private key for backup...")
        val salt = "STUBBED_SALT_BASE64" // Stub
        val encryptedKey = "STUBBED_ENCRYPTED_KEY_BASE64" // Stub
        return Pair(encryptedKey, salt)
    }

    /**
     * Decrypts the private key from Firestore using the password and
     * saves it to the Android Keystore.
     * This is used in LoginViewModel (Flow 2).
     */
    fun recoverAndStorePrivateKey(password: String, saltBase64: String, encryptedKeyBase64: String) {
        // TODO: Implement PBKDF2 and AES-GCM decryption
        // 1. Base64-decode the salt and encryptedKey
        // 2. Use PBKDF2WithHmacSHA256 to derive the S-Key from password + salt
        // 3. Use AES-GCM to decrypt the key data
        // 4. Reconstruct the PrivateKey object (e.g., KeyFactory)
        // 5. Get instance of AndroidKeyStore
        // 6. Save the reconstructed private key into the Keystore under KEYSTORE_ALIAS
        //    (This part is tricky, you might need to import it)

        println("CryptoManager: STUB - Recovering key with password...")
        // This function will throw an exception if the password is wrong
        // (e.g., AEADBadTagException), which the ViewModel will catch.
        if (password == "wrongpassword") { // Stub for testing
            throw Exception("Decryption failed (bad password)")
        }
    }
}