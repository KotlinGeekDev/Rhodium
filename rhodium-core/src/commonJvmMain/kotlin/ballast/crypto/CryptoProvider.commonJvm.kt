package ballast.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.jdk.JDK
import dev.whyoleg.cryptography.random.CryptographyRandom
import dev.whyoleg.cryptography.random.asCryptographyRandom
import java.security.SecureRandom

actual fun SecureRandom() : CryptographyRandom = SecureRandom().asCryptographyRandom()

actual fun getCryptoProvider(): CryptographyProvider {
    return CryptographyProvider.JDK
}