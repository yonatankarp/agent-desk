package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityState

data class OperatorHealthSummary(
    val status: OperatorHealthStatus,
    val ingestion: String,
    val source: String,
    val backend: String,
    val lastEvent: String,
    val lastReplay: String,
    val nextSafeAction: String,
    val diagnostics: List<String> = emptyList(),
)

enum class OperatorHealthStatus(val label: String) {
    Healthy("Healthy"),
    Empty("Empty"),
    Delayed("Delayed"),
    PartialImport("Partial import"),
    FailedImport("Failed import"),
    SourceDisconnected("Source disconnected"),
    SourcePermissionMissing("Source permission missing"),
}

object OperatorHealthProjector {
    fun project(
        state: OperatorState,
        sourceLabel: String = "current replay input",
        replayedAt: String? = null,
    ): OperatorHealthSummary {
        val latestEvent = state.events.maxByOrNull { it.occurredAt }?.occurredAt?.toString()
        val eventCount = state.events.size
        val warning = state.storeReadWarning
        val staleCount = state.staleAttention.size
        val hostConnectivity = state.hostConnectivity

        val status = when {
            warning != null -> OperatorHealthStatus.PartialImport

            hostConnectivity != null && hostConnectivity.state != RuntimeHostReachabilityState.Reachable ->
                OperatorHealthStatus.SourceDisconnected

            state.workItems.isEmpty() && state.events.isEmpty() -> OperatorHealthStatus.Empty

            staleCount > 0 -> OperatorHealthStatus.Delayed

            else -> OperatorHealthStatus.Healthy
        }

        return OperatorHealthSummary(
            status = status,
            ingestion = when (status) {
                OperatorHealthStatus.PartialImport -> "Recovered $eventCount committed event(s) from a partial import."
                OperatorHealthStatus.Empty -> "No replay events are available."
                OperatorHealthStatus.Delayed -> "Replayed $eventCount event(s); $staleCount delayed attention item(s) need review."
                OperatorHealthStatus.Healthy -> "Replayed $eventCount event(s) into operator state."
                OperatorHealthStatus.FailedImport -> "Runtime import failed."
                OperatorHealthStatus.SourceDisconnected -> "Runtime source is disconnected."
                OperatorHealthStatus.SourcePermissionMissing -> "Runtime source permission is missing."
            },
            source = "Source: $sourceLabel.",
            backend = warning?.let { "Local store warning: $it" } ?: "Backend: local replay state readable.",
            lastEvent = latestEvent?.let { "Last event: $it." } ?: "Last event: none.",
            lastReplay = replayedAt?.let { "Last replay: $it." } ?: "Last replay: not recorded.",
            nextSafeAction = when (status) {
                OperatorHealthStatus.PartialImport -> "Next safe action: repair the event store before importing more observations."
                OperatorHealthStatus.Empty -> "Next safe action: import sanitized observations or verify the sample surface."
                OperatorHealthStatus.Delayed -> "Next safe action: review delayed attention before treating the queue as healthy."
                OperatorHealthStatus.Healthy -> "Next safe action: continue monitoring the replay timeline."
                OperatorHealthStatus.FailedImport -> "Next safe action: inspect the public-safe error and fix configuration or source access."
                OperatorHealthStatus.SourceDisconnected -> "Next safe action: reconnect the runtime source before importing observations."
                OperatorHealthStatus.SourcePermissionMissing -> "Next safe action: restore runtime source read permission before importing observations."
            },
            diagnostics = buildList {
                warning?.let(::add)
                hostConnectivity?.publicMessage()?.let(::add)
            },
        )
    }

    fun failedImport(publicSafeMessage: String): OperatorHealthSummary = failure(
        status = OperatorHealthStatus.FailedImport,
        ingestion = "Runtime import failed.",
        source = "Source: unavailable.",
        backend = "Backend: unavailable.",
        lastEvent = "Last event: unavailable.",
        lastReplay = "Last replay: unavailable.",
        nextSafeAction = "Next safe action: inspect the public-safe error and fix configuration or source access.",
        publicSafeMessage = publicSafeMessage,
    )

    fun sourceDisconnected(publicSafeMessage: String): OperatorHealthSummary = failure(
        status = OperatorHealthStatus.SourceDisconnected,
        ingestion = "Runtime source is disconnected.",
        source = "Source: disconnected.",
        backend = "Backend: unavailable.",
        lastEvent = "Last event: unavailable.",
        lastReplay = "Last replay: unavailable.",
        nextSafeAction = "Next safe action: reconnect the runtime source before importing observations.",
        publicSafeMessage = publicSafeMessage,
    )

    fun sourcePermissionMissing(publicSafeMessage: String): OperatorHealthSummary = failure(
        status = OperatorHealthStatus.SourcePermissionMissing,
        ingestion = "Runtime source permission is missing.",
        source = "Source: permission missing.",
        backend = "Backend: unavailable.",
        lastEvent = "Last event: unavailable.",
        lastReplay = "Last replay: unavailable.",
        nextSafeAction = "Next safe action: restore runtime source read permission before importing observations.",
        publicSafeMessage = publicSafeMessage,
    )

    fun failedImportSurface(publicSafeMessage: String): OperatorHealthSummary = when {
        publicSafeMessage.contains("permission", ignoreCase = true) -> sourcePermissionMissing(publicSafeMessage)

        publicSafeMessage.contains("could not be read", ignoreCase = true) ||
            publicSafeMessage.contains("could not be reached", ignoreCase = true) -> sourceDisconnected(publicSafeMessage)

        else -> failedImport(publicSafeMessage)
    }

    fun failed(publicSafeMessage: String): OperatorHealthSummary = failedImport(publicSafeMessage)

    private fun failure(
        status: OperatorHealthStatus,
        ingestion: String,
        source: String,
        backend: String,
        lastEvent: String,
        lastReplay: String,
        nextSafeAction: String,
        publicSafeMessage: String,
    ): OperatorHealthSummary = OperatorHealthSummary(
        status = status,
        ingestion = ingestion,
        source = source,
        backend = backend,
        lastEvent = lastEvent,
        lastReplay = lastReplay,
        nextSafeAction = nextSafeAction,
        diagnostics = listOf(publicSafeMessage),
    )
}
