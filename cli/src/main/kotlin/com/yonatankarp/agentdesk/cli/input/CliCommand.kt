package com.yonatankarp.agentdesk.cli.input

import com.yonatankarp.agentdesk.app.operator.OperatorActionIntent

internal sealed interface CliCommand {
    data object Dashboard : CliCommand

    data class Inspect(val rawWorkItemId: String) : CliCommand

    data class ImportMockRuntime(val eventStorePath: String? = null) : CliCommand

    data class ImportOpenClawObservations(
        val observationsPath: String? = null,
        val eventStorePath: String? = null,
    ) : CliCommand

    data class HostSmoke(val hostConfigPath: String? = null) : CliCommand

    data object HostSmokeLab : CliCommand

    data class Act(
        val intent: OperatorActionIntent,
        val rawWorkItemId: String,
        val eventStorePath: String? = null,
        val auditStorePath: String? = null,
        val approve: Boolean = false,
    ) : CliCommand

    data class Report(
        val rawWorkItemId: String,
        val auditStorePath: String? = null,
    ) : CliCommand
}
