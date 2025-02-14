package rhodium.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.random.CryptographyRandom

expect fun SecureRandom(): CryptographyRandom
expect fun getCryptoProvider(): CryptographyProvider