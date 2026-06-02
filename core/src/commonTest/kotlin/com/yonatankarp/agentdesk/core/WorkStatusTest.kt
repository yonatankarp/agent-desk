package com.yonatankarp.agentdesk.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkStatusTest {
    @Test
    fun queuedWorkCanStartOrBeCanceled() {
        assertTrue(WorkStatus.Queued.canTransitionTo(WorkStatus.Running))
        assertTrue(WorkStatus.Queued.canTransitionTo(WorkStatus.Canceled))

        assertFalse(WorkStatus.Queued.canTransitionTo(WorkStatus.Succeeded))
        assertFalse(WorkStatus.Queued.canTransitionTo(WorkStatus.Blocked))
    }

    @Test
    fun runningWorkCanEnterAttentionStatesOrFinish() {
        assertTrue(WorkStatus.Running.canTransitionTo(WorkStatus.Waiting))
        assertTrue(WorkStatus.Running.canTransitionTo(WorkStatus.NeedsDecision))
        assertTrue(WorkStatus.Running.canTransitionTo(WorkStatus.Blocked))
        assertTrue(WorkStatus.Running.canTransitionTo(WorkStatus.Succeeded))
        assertTrue(WorkStatus.Running.canTransitionTo(WorkStatus.Failed))
        assertTrue(WorkStatus.Running.canTransitionTo(WorkStatus.Canceled))
    }

    @Test
    fun attentionStatesCanResumeOrMoveBetweenAttentionStates() {
        assertTrue(WorkStatus.Waiting.canTransitionTo(WorkStatus.Running))
        assertTrue(WorkStatus.NeedsDecision.canTransitionTo(WorkStatus.Blocked))
        assertTrue(WorkStatus.Blocked.canTransitionTo(WorkStatus.NeedsDecision))
    }

    @Test
    fun terminalStatusesCannotReopen() {
        assertFalse(WorkStatus.Succeeded.canTransitionTo(WorkStatus.Running))
        assertFalse(WorkStatus.Failed.canTransitionTo(WorkStatus.Running))
        assertFalse(WorkStatus.Canceled.canTransitionTo(WorkStatus.Running))
    }

    @Test
    fun humanAttentionIsExplicit() {
        assertTrue(WorkStatus.NeedsDecision.requiresHumanAttention)
        assertTrue(WorkStatus.Blocked.requiresHumanAttention)

        assertFalse(WorkStatus.Waiting.requiresHumanAttention)
        assertFalse(WorkStatus.Running.requiresHumanAttention)
    }
}
