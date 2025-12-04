package it.alby02.secretsanta.data.security

import java.security.SecureRandom

object ShamirSecretSharing {

    // --- Shamir's Secret Sharing (GF(2^8)) ---

    fun splitSecret(secret: ByteArray, n: Int, k: Int): List<ByteArray> {
        require(k <= n) { "Threshold k must be less than or equal to total shares n" }
        require(n < 256) { "Total shares n must be less than 256 for GF(2^8)" }

        val shares = MutableList(n) { ByteArray(secret.size + 1) }
        val random = SecureRandom()

        for (i in secret.indices) {
            // Polynomial coefficients: a_0 = secret[i], a_1...a_{k-1} are random
            val coeffs = ByteArray(k)
            coeffs[0] = secret[i]
            for (j in 1 until k) {
                coeffs[j] = random.nextInt(256).toByte()
            }

            // Evaluate polynomial at x = 1, 2, ..., n
            for (x in 1..n) {
                var y = coeffs[0]
                for (j in 1 until k) {
                    y = gfAdd(y, gfMul(coeffs[j], gfPow(x.toByte(), j.toByte())))
                }
                shares[x - 1][0] = x.toByte() // Store x at the beginning of the share
                shares[x - 1][i + 1] = y
            }
        }
        return shares
    }

    fun combineShares(shares: List<ByteArray>): ByteArray {
        if (shares.isEmpty()) return ByteArray(0)
        val secretLength = shares[0].size - 1
        val secret = ByteArray(secretLength)

        // We need at least k shares. We assume the caller provides enough valid shares.
        // Using Lagrange interpolation at x=0 to find the secret (a_0).

        val xValues = shares.map { it[0] }
        
        for (i in 0 until secretLength) {
            val yValues = shares.map { it[i + 1] }
            var result: Byte = 0
            
            for (j in shares.indices) {
                val xj = xValues[j]
                val yj = yValues[j]
                
                var numerator: Byte = 1
                var denominator: Byte = 1
                
                for (m in shares.indices) {
                    if (j == m) continue
                    val xm = xValues[m]
                    
                    // L_j(0) = product( (0 - xm) / (xj - xm) )
                    numerator = gfMul(numerator, xm) // 0 - xm in GF(2^8) is just xm because addition is XOR
                    denominator = gfMul(denominator, gfAdd(xj, xm)) // xj - xm is xj XOR xm
                }
                
                val lagrangeCoeff = gfDiv(numerator, denominator)
                result = gfAdd(result, gfMul(yj, lagrangeCoeff))
            }
            secret[i] = result
        }
        
        return secret
    }

    // --- GF(2^8) Arithmetic Helpers (Rijndael's finite field) ---
    
    private fun gfAdd(a: Byte, b: Byte): Byte {
        return (a.toInt() xor b.toInt()).toByte()
    }

    private fun gfMul(a: Byte, b: Byte): Byte {
        var aa = a.toInt() and 0xFF
        var bb = b.toInt() and 0xFF
        var p = 0
        for (i in 0 until 8) {
            if ((bb and 1) != 0) {
                p = p xor aa
            }
            val highBitSet = (aa and 0x80) != 0
            aa = (aa shl 1) and 0xFF
            if (highBitSet) {
                aa = aa xor 0x1B // Rijndael's irreducible polynomial
            }
            bb = bb shr 1
        }
        return p.toByte()
    }
    
    private fun gfPow(a: Byte, b: Byte): Byte {
        var res: Byte = 1
        var base = a
        var exp = b.toInt() and 0xFF
        while (exp > 0) {
            if ((exp and 1) == 1) res = gfMul(res, base)
            base = gfMul(base, base)
            exp = exp shr 1
        }
        return res
    }

    private fun gfDiv(a: Byte, b: Byte): Byte {
        if (b.toInt() == 0) throw ArithmeticException("Division by zero in GF(2^8)")
        return gfMul(a, gfInverse(b))
    }

    private fun gfInverse(a: Byte): Byte {
        // In GF(2^8), a^254 is the multiplicative inverse of a
        return gfPow(a, 254.toByte())
    }
}
