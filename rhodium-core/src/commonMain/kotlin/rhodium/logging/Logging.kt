package rhodium.logging

import co.touchlab.kermit.DefaultFormatter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter

val serviceLogger = Logger(
    config = loggerConfigInit(platformLogWriter(DefaultFormatter)),
    tag = "RhodiumLogger"
)