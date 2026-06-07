package com.yonatankarp.agentdesk.app.runtime

import java.nio.file.Path

object RuntimeWorkEventSources {
    fun openClawObservationFile(exportPath: Path): RuntimeWorkEventSource = OpenClawRuntimeObservationFileSource(exportPath)
}
