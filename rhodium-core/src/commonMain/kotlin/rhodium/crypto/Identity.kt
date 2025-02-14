/**
 * This file is copied(and modified) from https://github.com/Giszmo/NostrPostr.
 * Credits: Giszmo(on Github)
 */

package rhodium.crypto

class Identity(
    privKey: ByteArray? = null,
    pubKey: ByteArray? = null
) {
    val privKey: ByteArray?
    val pubKey: ByteArray

    init {
        if (privKey == null) {
            if (pubKey == null) {
                // create new, random keys
                this.privKey = CryptoUtils.generatePrivateKey()
                this.pubKey = CryptoUtils.getPublicKey(this.privKey)
            } else {
                // this is a read-only account
                check(pubKey.size == 32)
                this.privKey = null
                this.pubKey = pubKey
            }
        } else {
            // as private key is provided, ignore the public key and set keys according to private key
            this.privKey = privKey
            this.pubKey = CryptoUtils.getPublicKey(privKey)
        }
    }

    override fun toString(): String {
        return "Persona(privateKey=${privKey?.toHexString()}, publicKey=${pubKey.toHexString()})"
    }
}