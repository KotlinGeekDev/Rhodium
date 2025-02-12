package ballast.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.openssl3.Openssl3
import dev.whyoleg.cryptography.random.CryptographyRandom

actual fun SecureRandom() : CryptographyRandom {
    return CryptographyRandom
}

actual fun getCryptoProvider(): CryptographyProvider {
    return CryptographyProvider.Openssl3
}