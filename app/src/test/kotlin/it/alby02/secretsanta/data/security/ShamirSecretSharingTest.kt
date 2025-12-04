package it.alby02.secretsanta.data.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.security.SecureRandom
import java.util.Arrays

class ShamirSecretSharingTest {

    @Test
    fun testSplitAndCombine() {
        val secret = "This is a secret message!".toByteArray()
        val n = 5
        val k = 3

        val shares = ShamirSecretSharing.splitSecret(secret, n, k)

        // Verify we have n shares
        assert(shares.size == n)

        // 1. Combine k shares (should succeed)
        val subsetK = shares.subList(0, k)
        val recoveredSecretK = ShamirSecretSharing.combineShares(subsetK)
        assertArrayEquals(secret, recoveredSecretK)

        // 2. Combine n shares (should succeed)
        val recoveredSecretN = ShamirSecretSharing.combineShares(shares)
        assertArrayEquals(secret, recoveredSecretN)

        // 3. Combine k-1 shares (should fail to recover original secret)
        val subsetKMinus1 = shares.subList(0, k - 1)
        val recoveredSecretKMinus1 = ShamirSecretSharing.combineShares(subsetKMinus1)
        assertFalse(Arrays.equals(secret, recoveredSecretKMinus1))
        
        // 4. Combine random k shares (should succeed)
        val shuffledShares = shares.shuffled()
        val randomSubsetK = shuffledShares.subList(0, k)
        val recoveredRandomK = ShamirSecretSharing.combineShares(randomSubsetK)
        assertArrayEquals(secret, recoveredRandomK)
    }
    
    @Test
    fun testSplitAndCombineBinarySecret() {
        val secret = ByteArray(32)
        SecureRandom().nextBytes(secret)
        val n = 10
        val k = 6

        val shares = ShamirSecretSharing.splitSecret(secret, n, k)

        val subsetK = shares.shuffled().subList(0, k)
        val recoveredSecret = ShamirSecretSharing.combineShares(subsetK)
        
        assertArrayEquals(secret, recoveredSecret)
    }
}
