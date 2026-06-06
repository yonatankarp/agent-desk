package com.yonatankarp.agentdesk.app.fixtures

import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjector
import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.testfixtures.WorkEventSequenceBuilder
import com.yonatankarp.agentdesk.testfixtures.workEvents

/** Projects an event chain described with the workEvents DSL into operator state. */
internal fun operatorState(block: WorkEventSequenceBuilder.() -> Unit): OperatorState = OperatorStateProjector.project(workEvents(block = block))

/** Projects an event chain and returns its single resulting work item. */
internal fun projectedWorkItem(block: WorkEventSequenceBuilder.() -> Unit): WorkItem = operatorState(block).workItems.single()
