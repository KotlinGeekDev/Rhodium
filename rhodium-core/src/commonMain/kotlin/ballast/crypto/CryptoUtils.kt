//@file:JvmName("CryptoUtils")

package ballast.crypto

import dev.whyoleg.cryptography.algorithms.SHA256
import fr.acinq.secp256k1.Hex
import fr.acinq.secp256k1.Secp256k1
import fr.acinq.secp256k1.Secp256k1Exception

//Class containing all the cryptographic helpers for Nostr
object CryptoUtils {

    fun generatePrivateKey(): ByteArray {
        val secretKey = ByteArray(32)
        val pseudoRandomBytes = SecureRandom()
        pseudoRandomBytes.nextBytes(secretKey)
        return secretKey
    }

    /**
     * Generates(creates) a 32-byte public key from the provided private key.
     * @param privateKey the 32-byte private key, provided as a byte
     * array.
     *
     * @return the public key, as a byte array.
     */
    fun getPublicKey(privateKey: ByteArray): ByteArray {
        if (!Secp256k1.secKeyVerify(privateKey)) throw Exception("Invalid private key!")
        val pubKey = Secp256k1.pubkeyCreate(privateKey).drop(1).take(32).toByteArray()
        //context.cleanup()
        return pubKey
    }

    /**
     * Function that returns the hash of content
     * @param content the content to be hashed
     * @return the content hash, as a byte array.
     */
    //TODO: Should the function return a string or a byte array?
    fun contentHash(content: String): ByteArray {
        return getCryptoProvider().get(SHA256)
            .hasher()
            .hashBlocking(content.encodeToByteArray())

    }

    /**
     * The function signs the content provided to it, and
     * returns the 64-byte schnorr signature of the content.
     * @param privateKey the private key used for signing, provided
     * as a byte array.
     * @param content the content to be signed, provided as a
     * byte array.
     *
     * @return the 64-byte signature, as a byte array.
     */
    @Throws(Error::class)
    fun signContent(privateKey: ByteArray, content: ByteArray): ByteArray {
        val freshRandomBytes = ByteArray(32)
        SecureRandom().nextBytes(freshRandomBytes)
        val contentSignature = Secp256k1.signSchnorr(content, privateKey, freshRandomBytes)
        return contentSignature
    }

    /**
     * The function verifies the provided 64-byte signature.
     * @param signature the signature to provide, as a byte array.
     * @param publicKey the 32-byte public key to provide, as a byte array.
     * @param content the signed content to provide, as a byte array.
     *
     * @return the validity of the signature, as a boolean.
     */
    @Throws(Secp256k1Exception::class)
    fun verifyContentSignature(
        signature: ByteArray,
        publicKey: ByteArray,
        content: ByteArray
    ): Boolean {
        val verificationStatus = Secp256k1.verifySchnorr(signature, content, publicKey)

        return verificationStatus
    }

}

fun ByteArray.toHexString() = Hex.encode(this)
fun String.toBytes() = Hex.decode(this)