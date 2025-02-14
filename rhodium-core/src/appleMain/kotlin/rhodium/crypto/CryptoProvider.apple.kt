package rhodium.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.apple.Apple
import dev.whyoleg.cryptography.random.CryptographyRandom

actual fun SecureRandom(): CryptographyRandom {
    return CryptographyRandom
}

actual fun getCryptoProvider(): CryptographyProvider {
    return CryptographyProvider.Apple
}