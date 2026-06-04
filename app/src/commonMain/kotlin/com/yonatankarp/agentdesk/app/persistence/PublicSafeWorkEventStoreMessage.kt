package com.yonatankarp.agentdesk.app.persistence

internal object PublicSafeWorkEventStoreMessage {
    private val duplicateWithLinePattern =
        Regex("""Duplicate work event id .+ at line (\d+) in configured event store""")

    fun from(
        error: WorkEventStoreException,
        unreadableMessage: String,
    ): String {
        val text = error.message.orEmpty()
        val duplicateLine = duplicateWithLinePattern
            .matchEntire(text)
            ?.groupValues
            ?.get(1)
        if (duplicateLine != null) {
            return "Configured event store contains a duplicate work event id at line $duplicateLine."
        }
        if (text.startsWith("Duplicate work event id ")) {
            return "Configured event store contains a duplicate work event id."
        }
        if (text.startsWith("Corrupt work event record at line ")) {
            return text
        }
        return unreadableMessage
    }
}
