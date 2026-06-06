package com.yonatankarp.agentdesk.testfixtures.matchers

import com.yonatankarp.agentdesk.core.domain.projections.OperatorStateProjection
import com.yonatankarp.agentdesk.core.domain.projections.WorkEventProjector
import com.yonatankarp.agentdesk.testfixtures.WorkEventFixtures
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec

class ProjectionMatchersTest :
    BehaviorSpec({
        given("the empty-projection matcher") {
            `when`("the projection has no items, events, or issues") {
                then("it passes") {
                    OperatorStateProjection(
                        workItems = emptyList(),
                        recentEvents = emptyList(),
                        ignoredEvents = emptyList(),
                    ).shouldBeEmptyProjection()
                }
            }

            `when`("the projection contains work") {
                then("it fails") {
                    val projection = WorkEventProjector.project(listOf(WorkEventFixtures.workStartedEvent()))

                    shouldThrow<AssertionError> { projection.shouldBeEmptyProjection() }
                }
            }
        }
    })
