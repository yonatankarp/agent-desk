package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.runtime.RuntimeHostProfile
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityDiagnostic
import com.yonatankarp.agentdesk.app.runtime.RuntimeHostReachabilityDiagnostics
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PrintStream

/** Shared CLI test harness: runs the real dispatcher with captured streams and a fixed clock. */
internal data class CliRunResult(
    val exitCode: Int,
    val output: String,
    val error: String,
)

internal data class UsageErrorCase(
    val args: List<String>,
    val expectedErrors: List<String>,
)

internal fun runCli(
    vararg args: String,
    input: String = "",
    hostReachabilityCheck: (RuntimeHostProfile) -> RuntimeHostReachabilityDiagnostic = {
        RuntimeHostReachabilityDiagnostics.unreachable(it.alias)
    },
): CliRunResult = runCli(
    args = args,
    input = ByteArrayInputStream(input.encodeToByteArray()),
    hostReachabilityCheck = hostReachabilityCheck,
)

internal fun runCli(
    vararg args: String,
    input: InputStream,
    hostReachabilityCheck: (RuntimeHostProfile) -> RuntimeHostReachabilityDiagnostic = {
        RuntimeHostReachabilityDiagnostics.unreachable(it.alias)
    },
): CliRunResult {
    val output = ByteArrayOutputStream()
    val error = ByteArrayOutputStream()
    val exitCode =
        AgentDeskCli.run(
            args = args.toList().toTypedArray(),
            input = input,
            output = PrintStream(output),
            error = PrintStream(error),
            now = { EventTimestamp.parse("2026-06-06T09:30:00Z") },
            hostReachabilityCheck = hostReachabilityCheck,
        )

    return CliRunResult(
        exitCode = exitCode,
        output = output.toString().trimEnd(),
        error = error.toString().trimEnd(),
    )
}

internal fun privateLinuxPath(fileName: String): String = "/home/" + "operator/$fileName"

internal fun usageErrorCases(): List<UsageErrorCase> = listOf(
    UsageErrorCase(
        args = listOf("--events"),
        expectedErrors = listOf("Missing value for --events.", "Run with --help for usage."),
    ),
    UsageErrorCase(
        args = listOf("--config"),
        expectedErrors = listOf("Missing value for --config.", "Run with --help for usage."),
    ),
    UsageErrorCase(
        args = listOf("--private-token-file=${privateLinuxPath("private-token.txt")}"),
        expectedErrors = listOf("Unknown option."),
    ),
    UsageErrorCase(
        args = listOf("inspect", "agent-task:42", "import-mock-runtime"),
        expectedErrors = listOf("Choose only one command.", "Run with --help for usage."),
    ),
    UsageErrorCase(
        args = listOf("import-mock-runtime", "import-openclaw-observations"),
        expectedErrors = listOf("Choose only one command.", "Run with --help for usage."),
    ),
    UsageErrorCase(
        args = listOf("import-openclaw-observations", "act", "resume", "agent-task:42"),
        expectedErrors = listOf("Choose only one command.", "Run with --help for usage."),
    ),
    UsageErrorCase(
        args = listOf("act", "resume", "agent-task:42", "inspect", "agent-task:43"),
        expectedErrors = listOf("Choose only one command.", "Run with --help for usage."),
    ),
    UsageErrorCase(
        args = listOf("import-mock-runtime", "--event-store", "store-one.ndjson", "--event-store", "store-two.ndjson"),
        expectedErrors = listOf("Choose only one event store."),
    ),
    UsageErrorCase(
        args = listOf("act", "resume", "agent-task:42", "--event-store", "store-one.ndjson", "--event-store", "store-two.ndjson"),
        expectedErrors = listOf("Choose only one event store."),
    ),
    UsageErrorCase(
        args = listOf("import-openclaw-observations", "--event-store", "store-one.ndjson", "--event-store", "store-two.ndjson"),
        expectedErrors = listOf("Choose only one event store."),
    ),
    UsageErrorCase(
        args = listOf(
            "import-openclaw-observations",
            "--observations",
            "observations-one.json",
            "--observations",
            "observations-two.json",
        ),
        expectedErrors = listOf("Choose only one observations export."),
    ),
    UsageErrorCase(
        args = listOf("--event-store", "store.ndjson"),
        expectedErrors = listOf("--event-store is only valid with import commands or act."),
    ),
    UsageErrorCase(
        args = listOf("inspect", "agent-task:42", "--event-store", "store.ndjson"),
        expectedErrors = listOf("--event-store is only valid with import commands or act."),
    ),
    UsageErrorCase(
        args = listOf("--observations", "observations.json"),
        expectedErrors = listOf("--observations is only valid with import-openclaw-observations."),
    ),
    UsageErrorCase(
        args = listOf("import-mock-runtime", "--observations", "observations.json"),
        expectedErrors = listOf("--observations is only valid with import-openclaw-observations."),
    ),
    UsageErrorCase(
        args = listOf("act", "resume", "agent-task:42", "--observations", "observations.json"),
        expectedErrors = listOf("--observations is only valid with import-openclaw-observations."),
    ),
    UsageErrorCase(
        args = listOf("act"),
        expectedErrors = listOf("Missing action intent for act."),
    ),
    UsageErrorCase(
        args = listOf("act", "--event-store", "store.ndjson"),
        expectedErrors = listOf("Missing action intent for act."),
    ),
    UsageErrorCase(
        args = listOf("act", "resume"),
        expectedErrors = listOf("Missing work item id for act."),
    ),
    UsageErrorCase(
        args = listOf("act", "resume", "--event-store"),
        expectedErrors = listOf("Missing work item id for act."),
    ),
    UsageErrorCase(
        args = listOf("act", "resume", "agent-task:42", "--event-store", "store.ndjson"),
        expectedErrors = listOf("Missing value for --audit-store."),
    ),
    UsageErrorCase(
        args = listOf(
            "act", "resume", "agent-task:42",
            "--event-store", "store.ndjson",
            "--audit-store", "audit-one.ndjson",
            "--audit-store", "audit-two.ndjson",
        ),
        expectedErrors = listOf("Choose only one audit store."),
    ),
    UsageErrorCase(
        args = listOf("--audit-store", "audit.ndjson"),
        expectedErrors = listOf("--audit-store is only valid with act or report."),
    ),
    UsageErrorCase(
        args = listOf("import-mock-runtime", "--audit-store", "audit.ndjson"),
        expectedErrors = listOf("--audit-store is only valid with act or report."),
    ),
    UsageErrorCase(
        args = listOf("report"),
        expectedErrors = listOf("Missing work item id for report."),
    ),
    UsageErrorCase(
        args = listOf("report", "--events"),
        expectedErrors = listOf("Missing work item id for report."),
    ),
    UsageErrorCase(
        args = listOf("report", "agent-task:42", "inspect", "agent-task:43"),
        expectedErrors = listOf("Choose only one command.", "Run with --help for usage."),
    ),
    UsageErrorCase(
        args = listOf(
            "report",
            "agent-task:42",
            "--audit-store",
            "audit-one.ndjson",
            "--audit-store",
            "audit-two.ndjson",
        ),
        expectedErrors = listOf("Choose only one audit store."),
    ),
    UsageErrorCase(
        args = listOf("report", "agent-task:42", "--event-store", "store.ndjson"),
        expectedErrors = listOf("--event-store is only valid with import commands or act."),
    ),
    UsageErrorCase(
        args = listOf("report", "agent-task:42", "--approve"),
        expectedErrors = listOf("--approve is only valid with act."),
    ),
    UsageErrorCase(
        args = listOf("--approve"),
        expectedErrors = listOf("--approve is only valid with act."),
    ),
    UsageErrorCase(
        args = listOf("inspect", "agent-task:42", "--approve"),
        expectedErrors = listOf("--approve is only valid with act."),
    ),
    UsageErrorCase(
        args = listOf("inspect"),
        expectedErrors = listOf("Missing work item id for inspect."),
    ),
    UsageErrorCase(
        args = listOf("inspect", "--sample"),
        expectedErrors = listOf("Missing work item id for inspect."),
    ),
    UsageErrorCase(
        args = listOf("import-mock-runtime"),
        expectedErrors = listOf("Missing value for --event-store."),
    ),
    UsageErrorCase(
        args = listOf("host-smoke"),
        expectedErrors = listOf("Missing value for --host-config."),
    ),
    UsageErrorCase(
        args = listOf("host-smoke", "--host-config", "one.properties", "--host-config", "two.properties"),
        expectedErrors = listOf("Choose only one host config."),
    ),
    UsageErrorCase(
        args = listOf("--host-config", "agent-desk.host.properties"),
        expectedErrors = listOf("--host-config is only valid with host-smoke."),
    ),
)

internal const val STARTED_EVENT =
    """{"id":"event:agent-task:42:started","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.started","payload":{"title":"Run public hygiene check","summary":"Agent accepted the task and started local checks."}}"""

internal const val BLOCKED_EVENT =
    """{"id":"event:agent-task:42:blocked","occurredAt":"2026-06-02T21:05:00.123Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.blocked","payload":{"reason":"CI failed on the core test task."}}"""

internal const val NEEDS_DECISION_EVENT =
    """{"id":"event:agent-task:42:needs-decision","occurredAt":"2026-06-02T21:03:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.needs-decision","payload":{"reason":"Operator decision needed."}}"""

internal const val SUCCEEDED_EVENT =
    """{"id":"event:agent-task:42:succeeded","occurredAt":"2026-06-02T21:10:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.succeeded","payload":{}}"""

internal const val VERIFICATION_RECORDED_EVENT =
    """{"id":"event:agent-task:42:verification-recorded","occurredAt":"2026-06-02T21:04:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.verification-recorded","payload":{"outcome":"ready","verificationAttempted":true,"touchedArtifacts":["cli/src/main/kotlin/com/yonatankarp/agentdesk/cli/ReportCommand.kt"],"verificationResults":[{"name":"Gradle check","kind":"local-test","result":"passed","durationMillis":1200,"outputReference":"checks/gradle-check","evidenceReference":{"kind":"check-run","label":"Gradle check","target":"https://github.com/yonatankarp/agent-desk/actions/runs/27793545211"},"inputBinding":{"digest":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","algorithm":"sha-256","capturedAt":"2026-06-02T21:04:00Z"}}]}}"""

internal const val OTHER_STARTED_EVENT =
    """{"id":"event:agent-task:43:started","occurredAt":"2026-06-02T21:01:00Z","source":"mock-adapter","workItemId":"agent-task:43","type":"work.started","payload":{"title":"Prepare release checklist","summary":"Agent started release preparation."}}"""

internal const val UNSUPPORTED_EVENT =
    """{"id":"event:agent-task:42:paused","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.paused","payload":{}}"""

internal const val SANITIZED_OBSERVATION_EXPORT =
    """
    {
      "schemaVersion": 1,
      "observations": [
        {
          "eventId": "event:agent-task:212:started",
          "occurredAt": "2026-06-05T18:40:00Z",
          "source": "openclaw-local",
          "workItemId": "agent-task:212",
          "kind": "started",
          "title": "Run sanitized import smoke",
          "summary": "Agent started a public-safe smoke command.",
          "evidenceReferences": [
            {
              "kind": "sanitized-note",
              "label": "Runtime adapter decision",
              "target": "docs/runtime-adapter-scope-decision.md"
            }
          ]
        },
        {
          "eventId": "event:agent-task:212:blocked",
          "occurredAt": "2026-06-05T18:41:00Z",
          "source": "openclaw-local",
          "workItemId": "agent-task:212",
          "kind": "blocked",
          "reason": "Waiting for smoke command evidence."
        }
      ]
    }
    """
