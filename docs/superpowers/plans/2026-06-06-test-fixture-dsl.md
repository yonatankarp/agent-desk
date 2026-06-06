# Test Fixture DSL Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a three-layer test fixture DSL (string-friendly builders, event-sequence builder, custom Kotest matchers) to `:test-fixtures` and migrate all module tests to it.

**Architecture:** Everything new lives in `:test-fixtures` commonMain (depends only on `:core` + kotest-assertions), so all five consumer modules can use it. App-specific projection helpers live in `app/src/commonTest` (a `:test-fixtures` → `:app` dependency would be circular). Plain named builder functions and matchers — no infix/fluent chains.

**Tech Stack:** Kotlin Multiplatform (jvm target), Kotest 6.1.11 (BehaviorSpec for behavior tests), Konsist rules already in `:test-fixtures`, Spotless, Kover.

**Conventions that bind this work:**
- Kotest-only; no `kotlin.test`/`org.junit` imports (Konsist-enforced).
- No experimental APIs / `@OptIn` (the existing `ExperimentalTestApi` in Compose smoke tests is tracked debt #279 — do not touch, do not extend).
- Fixtures must be deterministic and public-safe.
- Run `./gradlew spotlessApply` before every commit.
- Full verification command: `make test` (= `:core:allTests :app:allTests :cli:test :desktop:allTests :mobile:allTests`).

**Deliberately out of scope (YAGNI):**
- Temp-file helper wrappers (each call site is 1–3 stdlib lines; no real win).
- `sectionRows()` dedup across desktop/mobile (two unrelated snapshot types; unifying needs production-code changes).
- Mobile `MobileWorkItem` builders (single module, low frequency).
- Compose test harness helper (glued to experimental API; defer until #279).
- Unifying `RuntimeWorkEventImporterTest`'s specialized repository double (it has failure-mode knobs the plain copies don't need; consolidate only the plain copies).

---

## File Structure

```
test-fixtures/
  build.gradle.kts                                  (modify: test source sets, kotest dep in commonMain)
  src/commonMain/kotlin/com/yonatankarp/agentdesk/testfixtures/
    WorkEventFixtures.kt                            (existing — unchanged)
    TimestampFixtures.kt                            (new: deterministic timestamp factory)
    EvidenceFixtures.kt                             (new: evidence reference factories)
    WorkEventSequenceBuilder.kt                     (new: workEvents { } builder)
    matchers/PublicSafetyMatchers.kt                (new: shouldBePublicSafe)
    matchers/ProjectionMatchers.kt                  (new: shouldBeEmptyProjection)
  src/commonTest/kotlin/com/yonatankarp/agentdesk/testfixtures/
    TimestampFixturesTest.kt                        (new)
    EvidenceFixturesTest.kt                         (new)
    WorkEventSequenceBuilderTest.kt                 (new)
    matchers/PublicSafetyMatchersTest.kt            (new)
    matchers/ProjectionMatchersTest.kt              (new)
  src/jvmTest/kotlin/com/yonatankarp/agentdesk/testfixtures/
    ArchitectureKonsistTest.kt                      (new: dogfood assertKotestOnly)
app/src/commonTest/kotlin/com/yonatankarp/agentdesk/app/fixtures/
  OperatorStateFixtures.kt                          (new: operatorState { } / projectedWorkItem { })
  InMemoryWorkEventRepository.kt                    (new: consolidated plain test double)
docs/engineering-style.md                           (modify: point at the DSL)
```

All existing test files in core/app/cli/desktop/mobile are migration targets (Tasks 8–11).

---

### Task 0: Branch

- [ ] **Step 1: Create a work branch**

```bash
# from the repository root
git checkout -b test/fixture-dsl
```

---

### Task 1: Wire test sources and kotest into :test-fixtures

**Files:**
- Modify: `test-fixtures/build.gradle.kts`
- Create: `test-fixtures/src/jvmTest/kotlin/com/yonatankarp/agentdesk/testfixtures/ArchitectureKonsistTest.kt`

- [ ] **Step 1: Replace `test-fixtures/build.gradle.kts` with:**

```kotlin
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(25)

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(libs.kotest.assertions.core)
        }
        jvmMain.dependencies {
            implementation(libs.konsist)
        }
        commonTest.dependencies {
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.framework.engine)
        }
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    failOnNoDiscoveredTests = true
}
```

Note: `kotest-assertions-core` moves into commonMain because the matchers (Task 5–6) ship as main code. Konsist stays jvmMain-only.

- [ ] **Step 2: Create the architecture test (dogfoods the module's own rule):**

`test-fixtures/src/jvmTest/kotlin/com/yonatankarp/agentdesk/testfixtures/ArchitectureKonsistTest.kt`

```kotlin
package com.yonatankarp.agentdesk.testfixtures

import com.yonatankarp.agentdesk.testfixtures.architecture.ModuleArchitectureRules
import io.kotest.core.spec.style.FunSpec

class ArchitectureKonsistTest :
    FunSpec({
        test("test-fixtures tests use Kotest only") {
            ModuleArchitectureRules.assertKotestOnly(
                moduleName = "test-fixtures",
                testSourceSets = listOf("commonTest", "jvmTest"),
            )
        }
    })
```

Check the exact signature of `ModuleArchitectureRules.assertKotestOnly` in `test-fixtures/src/jvmMain/kotlin/com/yonatankarp/agentdesk/testfixtures/architecture/ModuleArchitectureRules.kt` before writing — mirror how `core/src/jvmTest/.../ArchitectureKonsistTest.kt` calls it and adjust parameters to match.

- [ ] **Step 3: Verify the build wires up and the test runs**

Run: `./gradlew :test-fixtures:allTests`
Expected: PASS (1 test discovered; `failOnNoDiscoveredTests` proves wiring works)

- [ ] **Step 4: Commit**

```bash
./gradlew spotlessApply
git add test-fixtures/
git commit -m "test: wire test sources and kotest assertions into :test-fixtures"
```

---

### Task 2: Timestamp fixture factory

**Files:**
- Create: `test-fixtures/src/commonMain/kotlin/com/yonatankarp/agentdesk/testfixtures/TimestampFixtures.kt`
- Test: `test-fixtures/src/commonTest/kotlin/com/yonatankarp/agentdesk/testfixtures/TimestampFixturesTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.yonatankarp.agentdesk.testfixtures

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class TimestampFixturesTest :
    BehaviorSpec({
        given("the canonical fixture day") {
            `when`("a timestamp is requested at a minute offset") {
                then("it renders the canonical RFC 3339 instant") {
                    eventTimestampAt(minute = 5).value shouldBe "2026-06-02T21:05:00Z"
                }
            }

            `when`("hour and second are overridden") {
                then("they render zero-padded") {
                    eventTimestampAt(hour = 22, minute = 0, second = 7).value shouldBe "2026-06-02T22:00:07Z"
                }
            }

            `when`("a component is out of range") {
                then("construction fails") {
                    shouldThrow<IllegalArgumentException> { eventTimestampAt(minute = 60) }
                    shouldThrow<IllegalArgumentException> { eventTimestampAt(hour = 24, minute = 0) }
                    shouldThrow<IllegalArgumentException> { eventTimestampAt(minute = 0, second = 60) }
                }
            }
        }
    })
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :test-fixtures:allTests --tests '*TimestampFixturesTest*' 2>&1 | tail -20`
Expected: FAIL — `eventTimestampAt` unresolved

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.yonatankarp.agentdesk.testfixtures

import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp

/**
 * Deterministic timestamp on the canonical fixture day (2026-06-02).
 * Defaults to the canonical fixture hour so `eventTimestampAt(minute = 5)`
 * lands shortly after [WorkEventFixtures.startedAt].
 */
fun eventTimestampAt(
    minute: Int,
    hour: Int = 21,
    second: Int = 0,
): EventTimestamp {
    require(hour in 0..23) { "hour must be 0-23" }
    require(minute in 0..59) { "minute must be 0-59" }
    require(second in 0..59) { "second must be 0-59" }
    val hh = hour.toString().padStart(2, '0')
    val mm = minute.toString().padStart(2, '0')
    val ss = second.toString().padStart(2, '0')
    return EventTimestamp.parse("2026-06-02T$hh:$mm:${ss}Z")
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :test-fixtures:allTests --tests '*TimestampFixturesTest*' 2>&1 | tail -5`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add test-fixtures/
git commit -m "test: add deterministic eventTimestampAt fixture factory"
```

---

### Task 3: Evidence reference factories

**Files:**
- Create: `test-fixtures/src/commonMain/kotlin/com/yonatankarp/agentdesk/testfixtures/EvidenceFixtures.kt`
- Test: `test-fixtures/src/commonTest/kotlin/com/yonatankarp/agentdesk/testfixtures/EvidenceFixturesTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.yonatankarp.agentdesk.testfixtures

import com.yonatankarp.agentdesk.core.domain.events.EvidenceReferenceKind
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class EvidenceFixturesTest :
    BehaviorSpec({
        given("evidence factories") {
            `when`("a check-run reference is built from raw strings") {
                then("kind, label, and target are parsed") {
                    val reference = checkRunEvidence(
                        label = "Gradle Build",
                        target = "https://github.com/yonatankarp/agent-desk/actions/runs/1",
                    )

                    reference.kind shouldBe EvidenceReferenceKind.CheckRun
                    reference.label.value shouldBe "Gradle Build"
                    reference.target.value shouldBe "https://github.com/yonatankarp/agent-desk/actions/runs/1"
                }
            }

            `when`("the other kinds are built") {
                then("each maps to its kind") {
                    commitEvidence("Implementation commit", "commit:80de329").kind shouldBe
                        EvidenceReferenceKind.Commit
                    sanitizedNoteEvidence("Decision context", "docs/decision-context.md").kind shouldBe
                        EvidenceReferenceKind.SanitizedNote
                    artifactEvidence("Verification output", "artifact:verification-output").kind shouldBe
                        EvidenceReferenceKind.Artifact
                    screenshotEvidence("Desktop smoke", "artifact:desktop-smoke.png").kind shouldBe
                        EvidenceReferenceKind.Screenshot
                }
            }
        }
    })
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :test-fixtures:allTests --tests '*EvidenceFixturesTest*' 2>&1 | tail -20`
Expected: FAIL — factories unresolved

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.yonatankarp.agentdesk.testfixtures

import com.yonatankarp.agentdesk.core.domain.events.EvidenceLabel
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReferenceKind
import com.yonatankarp.agentdesk.core.domain.events.EvidenceTarget

fun commitEvidence(label: String, target: String): EvidenceReference =
    evidence(EvidenceReferenceKind.Commit, label, target)

fun checkRunEvidence(label: String, target: String): EvidenceReference =
    evidence(EvidenceReferenceKind.CheckRun, label, target)

fun artifactEvidence(label: String, target: String): EvidenceReference =
    evidence(EvidenceReferenceKind.Artifact, label, target)

fun screenshotEvidence(label: String, target: String): EvidenceReference =
    evidence(EvidenceReferenceKind.Screenshot, label, target)

fun sanitizedNoteEvidence(label: String, target: String): EvidenceReference =
    evidence(EvidenceReferenceKind.SanitizedNote, label, target)

private fun evidence(
    kind: EvidenceReferenceKind,
    label: String,
    target: String,
): EvidenceReference = EvidenceReference(
    kind = kind,
    label = EvidenceLabel.parse(label),
    target = EvidenceTarget.parse(target),
)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :test-fixtures:allTests --tests '*EvidenceFixturesTest*' 2>&1 | tail -5`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add test-fixtures/
git commit -m "test: add evidence reference fixture factories"
```

---

### Task 4: Event-sequence builder

**Files:**
- Create: `test-fixtures/src/commonMain/kotlin/com/yonatankarp/agentdesk/testfixtures/WorkEventSequenceBuilder.kt`
- Test: `test-fixtures/src/commonTest/kotlin/com/yonatankarp/agentdesk/testfixtures/WorkEventSequenceBuilderTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.yonatankarp.agentdesk.testfixtures

import com.yonatankarp.agentdesk.core.domain.events.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkEventType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class WorkEventSequenceBuilderTest :
    BehaviorSpec({
        given("the workEvents builder") {
            `when`("a canonical started/blocked chain is built") {
                then("it matches the canonical fixtures") {
                    val events = workEvents {
                        started()
                        blocked()
                    }

                    events shouldBe listOf(
                        WorkEventFixtures.workStartedEvent(),
                        WorkEventFixtures.workBlockedEvent(),
                    )
                }
            }

            `when`("a second work item is described with raw strings") {
                then("ids and payload values are derived and parsed") {
                    val events = workEvents {
                        started(workItemId = "agent-task:43", at = eventTimestampAt(minute = 1))
                        succeeded(workItemId = "agent-task:43", at = eventTimestampAt(minute = 2))
                    }

                    events.map { it.id.value } shouldContainExactly listOf(
                        "event:agent-task:43:started",
                        "event:agent-task:43:succeeded",
                    )
                    events.map { it.workItemId.value }.toSet() shouldBe setOf("agent-task:43")
                }
            }

            `when`("a blocked reason and evidence are provided") {
                then("they land on the event") {
                    val events = workEvents {
                        blocked(
                            reason = "CI failed twice in a row.",
                            evidence = listOf(checkRunEvidence("CI run", "https://github.com/x/y/actions/runs/1")),
                        )
                    }

                    val payload = events.single().payload.shouldBeInstanceOf<WorkBlockedPayload>()
                    payload.reason.value shouldBe "CI failed twice in a row."
                    events.single().evidenceReferences.single().label.value shouldBe "CI run"
                }
            }

            `when`("a pre-built event is appended") {
                then("it is kept verbatim and in order") {
                    val custom = WorkEventFixtures.workNeedsDecisionEvent()

                    val events = workEvents {
                        started()
                        event(custom)
                    }

                    events.map { it.type } shouldContainExactly listOf(
                        WorkEventType.WorkStarted,
                        WorkEventType.WorkNeedsDecision,
                    )
                    events[1] shouldBe custom
                }
            }
        }
    })
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :test-fixtures:allTests --tests '*WorkEventSequenceBuilderTest*' 2>&1 | tail -20`
Expected: FAIL — `workEvents` unresolved

- [ ] **Step 3: Write the implementation**

```kotlin
package com.yonatankarp.agentdesk.testfixtures

import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference
import com.yonatankarp.agentdesk.core.domain.events.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkCanceledPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkFailedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkNeedsDecisionPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary

/** Builds an ordered list of public-safe work events with canonical defaults. */
fun workEvents(
    fixtures: CanonicalWorkEventFixtures = WorkEventFixtures,
    block: WorkEventSequenceBuilder.() -> Unit,
): List<WorkEvent> = WorkEventSequenceBuilder(fixtures).apply(block).build()

class WorkEventSequenceBuilder internal constructor(
    private val fixtures: CanonicalWorkEventFixtures,
) {
    private val events = mutableListOf<WorkEvent>()

    fun started(
        workItemId: String? = null,
        at: EventTimestamp = fixtures.startedAt,
        title: String? = null,
        summary: String? = null,
        evidence: List<EvidenceReference> = emptyList(),
    ) {
        val itemId = itemId(workItemId)
        events += fixtures.workStartedEvent(
            id = eventId(itemId, suffix = "started"),
            occurredAt = at,
            workItemId = itemId,
            payload = WorkStartedPayload(
                title = title?.let(WorkItemTitle::parse) ?: fixtures.workTitle,
                summary = summary?.let(WorkSummary::parse) ?: fixtures.startedSummary,
            ),
        ).withEvidence(evidence)
    }

    fun blocked(
        workItemId: String? = null,
        at: EventTimestamp = fixtures.blockedAt,
        reason: String? = null,
        evidence: List<EvidenceReference> = emptyList(),
    ) {
        val itemId = itemId(workItemId)
        events += fixtures.workBlockedEvent(
            id = eventId(itemId, suffix = "blocked"),
            occurredAt = at,
            workItemId = itemId,
            payload = WorkBlockedPayload(
                reason = reason?.let(WorkSummary::parse) ?: fixtures.blockedReason,
            ),
        ).withEvidence(evidence)
    }

    fun needsDecision(
        workItemId: String? = null,
        at: EventTimestamp = fixtures.needsDecisionAt,
        reason: String = "Operator decision needed.",
        evidence: List<EvidenceReference> = emptyList(),
    ) {
        val itemId = itemId(workItemId)
        events += fixtures.workNeedsDecisionEvent(
            id = eventId(itemId, suffix = "needs-decision"),
            occurredAt = at,
            workItemId = itemId,
            payload = WorkNeedsDecisionPayload(reason = WorkSummary.parse(reason)),
        ).withEvidence(evidence)
    }

    fun succeeded(
        workItemId: String? = null,
        at: EventTimestamp = fixtures.terminalAt,
        evidence: List<EvidenceReference> = emptyList(),
    ) {
        val itemId = itemId(workItemId)
        events += fixtures.workSucceededEvent(
            id = eventId(itemId, suffix = "succeeded"),
            occurredAt = at,
            workItemId = itemId,
        ).withEvidence(evidence)
    }

    fun failed(
        workItemId: String? = null,
        at: EventTimestamp = fixtures.terminalAt,
        reason: String = "Build failed.",
        evidence: List<EvidenceReference> = emptyList(),
    ) {
        val itemId = itemId(workItemId)
        events += fixtures.workFailedEvent(
            id = eventId(itemId, suffix = "failed"),
            occurredAt = at,
            workItemId = itemId,
            payload = WorkFailedPayload(reason = WorkSummary.parse(reason)),
        ).withEvidence(evidence)
    }

    fun canceled(
        workItemId: String? = null,
        at: EventTimestamp = fixtures.terminalAt,
        reason: String = "Operator canceled the task.",
        evidence: List<EvidenceReference> = emptyList(),
    ) {
        val itemId = itemId(workItemId)
        events += fixtures.workCanceledEvent(
            id = eventId(itemId, suffix = "canceled"),
            occurredAt = at,
            workItemId = itemId,
            payload = WorkCanceledPayload(reason = WorkSummary.parse(reason)),
        ).withEvidence(evidence)
    }

    /** Escape hatch for events the named builders cannot express. */
    fun event(event: WorkEvent) {
        events += event
    }

    internal fun build(): List<WorkEvent> = events.toList()

    private fun itemId(raw: String?): WorkItemId = raw?.let(WorkItemId::parse) ?: fixtures.workItemId

    private fun eventId(
        itemId: WorkItemId,
        suffix: String,
    ): WorkEventId = WorkEventId.parse("event:${itemId.value}:$suffix")

    private fun WorkEvent.withEvidence(evidence: List<EvidenceReference>): WorkEvent =
        if (evidence.isEmpty()) this else copy(evidenceReferences = evidence)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :test-fixtures:allTests --tests '*WorkEventSequenceBuilderTest*' 2>&1 | tail -5`
Expected: PASS. If the first scenario fails on event id mismatch: the canonical default ids in `CanonicalWorkEventFixtures` are `event:agent-task:42:<suffix>` and `itemId(null)` is `agent-task:42`, so derived ids must be identical — debug the derivation, do not change the existing fixtures.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add test-fixtures/
git commit -m "test: add workEvents sequence builder DSL"
```

---

### Task 5: Public-safety matcher

**Files:**
- Create: `test-fixtures/src/commonMain/kotlin/com/yonatankarp/agentdesk/testfixtures/matchers/PublicSafetyMatchers.kt`
- Test: `test-fixtures/src/commonTest/kotlin/com/yonatankarp/agentdesk/testfixtures/matchers/PublicSafetyMatchersTest.kt`

The denylist is the union of the three existing copies: `AgentDeskCliTest.assertPublicSafe` (16 checks), `PrivacyBoundaryRegressionTest.assertPublicSafeOutput` (11 checks), and the desktop/mobile inline checks. Union risk: the bare term `token` (from the app copy) is stricter than what CLI/desktop output was ever checked against. If migration (Tasks 9–11) surfaces a failure on a denylist term, treat it as a finding to inspect — either a real leak or a term to discuss with the owner — never silently weaken the list.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.yonatankarp.agentdesk.testfixtures.matchers

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain

class PublicSafetyMatchersTest :
    BehaviorSpec({
        given("the public-safety matcher") {
            `when`("text is public-safe") {
                then("it passes") {
                    "Run public hygiene check: work.started for agent-task:42".shouldBePublicSafe()
                }
            }

            `when`("text leaks a private path") {
                then("it fails and names the violation") {
                    val leak = "/home/" + "operator/notes.txt"

                    val failure = shouldThrow<AssertionError> { "stored at $leak".shouldBePublicSafe() }

                    failure.message.orEmpty() shouldContain "/home/"
                }
            }

            `when`("text leaks credential-shaped content") {
                then("it fails regardless of casing") {
                    shouldThrow<AssertionError> { "Bearer abc".shouldBePublicSafe() }
                    shouldThrow<AssertionError> { "GHP_abcdef".shouldBePublicSafe() }
                    shouldThrow<AssertionError> { ("op:" + "//vault/item").shouldBePublicSafe() }
                }
            }

            `when`("text contains a raw numeric identifier") {
                then("it fails") {
                    val rawIdentifier = "123456789" + "012345678"

                    shouldThrow<AssertionError> { "channel $rawIdentifier".shouldBePublicSafe() }
                }
            }
        }
    })
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :test-fixtures:allTests --tests '*PublicSafetyMatchersTest*' 2>&1 | tail -20`
Expected: FAIL — `shouldBePublicSafe` unresolved

- [ ] **Step 3: Write the implementation**

String concatenation in the literals below is deliberate — it keeps the denylist itself from tripping `scripts/validate-public-hygiene.sh` (same trick `AgentDeskCliTest` uses). Keep it.

```kotlin
package com.yonatankarp.agentdesk.testfixtures.matchers

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty

/**
 * Canonical public-safety denylist. Union of the per-module lists this
 * matcher replaced (CLI, app privacy regression, desktop/mobile inline).
 * Matched case-insensitively.
 */
private val denyTerms: List<String> = listOf(
    "/home/",
    "/users/",
    "\\users\\",
    "c:\\",
    "file:",
    "localhost",
    "private-token",
    "auth_token",
    "github_pat_",
    "ghp_",
    "xoxb-",
    "bearer",
    "password",
    "secret",
    "token",
    "op:" + "//",
    "discord",
    "channel:",
    "message:",
    "session:",
    "thread:",
    "raw transcript",
)

private val rawIdentifier: String = "123456789" + "012345678"

/** Asserts the text contains none of the canonical public-safety denylist terms. */
fun String.shouldBePublicSafe() {
    val lowered = lowercase()
    val violations = denyTerms.filter { it in lowered } +
        listOfNotNull(rawIdentifier.takeIf { it in this })

    withClue("Expected public-safe text but found denylisted content $violations in: $this") {
        violations.shouldBeEmpty()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :test-fixtures:allTests --tests '*PublicSafetyMatchersTest*' 2>&1 | tail -5`
Expected: PASS

- [ ] **Step 5: Run the hygiene scanner against the new files**

Run: `make hygiene`
Expected: PASS. If the denylist literals trip it, split the offending literal with `+` concatenation as above.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add test-fixtures/
git commit -m "test: add shouldBePublicSafe matcher with canonical denylist"
```

---

### Task 6: Projection matcher

**Files:**
- Create: `test-fixtures/src/commonMain/kotlin/com/yonatankarp/agentdesk/testfixtures/matchers/ProjectionMatchers.kt`
- Test: `test-fixtures/src/commonTest/kotlin/com/yonatankarp/agentdesk/testfixtures/matchers/ProjectionMatchersTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
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
```

Before running: check `WorkEventProjector`'s actual entry point in `core/src/commonMain/.../projections/WorkEventProjector.kt` — if `project` takes extra parameters (e.g. a stale threshold) or has a different name, adapt the second scenario to the real signature.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :test-fixtures:allTests --tests '*ProjectionMatchersTest*' 2>&1 | tail -20`
Expected: FAIL — `shouldBeEmptyProjection` unresolved

- [ ] **Step 3: Write the implementation**

```kotlin
package com.yonatankarp.agentdesk.testfixtures.matchers

import com.yonatankarp.agentdesk.core.domain.projections.OperatorStateProjection
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty

/** Asserts the projection accepted nothing: no items, no events, no ignored issues. */
fun OperatorStateProjection.shouldBeEmptyProjection() {
    assertSoftly(this) {
        workItems.shouldBeEmpty()
        recentEvents.shouldBeEmpty()
        ignoredEvents.shouldBeEmpty()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :test-fixtures:allTests --tests '*ProjectionMatchersTest*' 2>&1 | tail -5`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add test-fixtures/
git commit -m "test: add shouldBeEmptyProjection matcher"
```

---

### Task 7: App-layer projection helpers and consolidated repository double

**Files:**
- Create: `app/src/commonTest/kotlin/com/yonatankarp/agentdesk/app/fixtures/OperatorStateFixtures.kt`
- Create: `app/src/commonTest/kotlin/com/yonatankarp/agentdesk/app/fixtures/InMemoryWorkEventRepository.kt`

These are test helpers exercised by the migrations in Task 9; they get no standalone spec. They cannot live in `:test-fixtures` because they depend on `:app` types (`OperatorStateProjector`, `WorkEventRepository`) and `:app`'s commonTest already depends on `:test-fixtures` — the reverse dependency would be circular.

- [ ] **Step 1: Create `OperatorStateFixtures.kt`**

```kotlin
package com.yonatankarp.agentdesk.app.fixtures

import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjector
import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.testfixtures.WorkEventSequenceBuilder
import com.yonatankarp.agentdesk.testfixtures.workEvents

/** Projects an event chain described with the workEvents DSL into operator state. */
internal fun operatorState(block: WorkEventSequenceBuilder.() -> Unit): OperatorState =
    OperatorStateProjector.project(workEvents(block = block))

/** Projects an event chain and returns its single resulting work item. */
internal fun projectedWorkItem(block: WorkEventSequenceBuilder.() -> Unit): WorkItem =
    operatorState(block).workItems.single()
```

- [ ] **Step 2: Create `InMemoryWorkEventRepository.kt`**

```kotlin
package com.yonatankarp.agentdesk.app.fixtures

import com.yonatankarp.agentdesk.app.persistence.WorkEventReadResult
import com.yonatankarp.agentdesk.app.persistence.WorkEventRepository
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent

/** Plain in-memory repository test double for tests without failure-mode needs. */
internal class InMemoryWorkEventRepository : WorkEventRepository {
    private val events = mutableListOf<WorkEvent>()

    override fun append(event: WorkEvent) {
        events += event
    }

    override fun readAll(): WorkEventReadResult = WorkEventReadResult(events = events.toList())
}
```

Before writing: check `WorkEventReadResult`'s constructor in `app/src/commonMain/.../persistence/` — if it carries more than `events` (e.g. warnings), mirror the existing inline doubles' construction exactly.

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :app:compileTestKotlinJvm 2>&1 | tail -5` (use the actual compile task name `./gradlew :app:tasks | grep -i compile.*test` reports if this one is absent)
Expected: BUILD SUCCESSFUL. A "declaration is never used" warning is fine at this point — Task 9 adds the users. If `internal` visibility plus an unused-symbol check fails the build, proceed to Task 9 and verify then.

- [ ] **Step 4: Commit**

```bash
./gradlew spotlessApply
git add app/
git commit -m "test: add app-layer operatorState/projectedWorkItem helpers and shared repository double"
```

---

### Task 8: Migrate :core tests

**Files (modify):**
- `core/src/commonTest/kotlin/com/yonatankarp/agentdesk/core/domain/projections/WorkEventProjectorTest.kt`
- `core/src/commonTest/kotlin/com/yonatankarp/agentdesk/core/domain/events/EventTimestampMinuteTest.kt`
- `core/src/commonTest/kotlin/com/yonatankarp/agentdesk/core/domain/events/WorkEventTest.kt`
- Other core test files only where a pattern below appears.

Apply these mechanical rewrites; do not restructure scenarios or change assertions' meaning.

- [ ] **Step 1: Replace inline timestamp parsing with `eventTimestampAt`**

Before:
```kotlin
EventTimestamp.parse("2026-06-02T22:01:00Z")
```
After:
```kotlin
eventTimestampAt(hour = 22, minute = 1)
```
Keep `EventTimestamp.parse(...)` where the test's subject IS the parsing (e.g. invalid-format scenarios in `EventTimestampMinuteTest`) — only replace construction noise, not the behavior under test. Timestamps with fractional seconds (e.g. `21:05:00.123Z`) stay as `parse` calls; the factory deliberately does not model fractions.

- [ ] **Step 2: Replace hand-built event lists with `workEvents`**

Before (`WorkEventProjectorTest`):
```kotlin
val events = listOf(
    CoreFixtures.workStartedEvent(),
    CoreFixtures.workStartedEvent(
        id = WorkEventId.parse("event:agent-task:43:started"),
        occurredAt = EventTimestamp.parse("2026-06-02T22:01:00Z"),
        workItemId = WorkItemId.parse("agent-task:43"),
    ),
)
```
After:
```kotlin
val events = workEvents {
    started()
    started(workItemId = "agent-task:43", at = eventTimestampAt(hour = 22, minute = 1))
}
```

- [ ] **Step 3: Replace triple-empty projection assertions with the matcher**

Before:
```kotlin
projection.workItems shouldBe emptyList()
projection.recentEvents shouldBe emptyList()
projection.ignoredEvents shouldBe emptyList()
```
After:
```kotlin
projection.shouldBeEmptyProjection()
```

- [ ] **Step 4: Replace inline `EvidenceReference` construction with factories**

Before (`WorkEventTest`):
```kotlin
EvidenceReference(
    kind = EvidenceReferenceKind.CheckRun,
    label = EvidenceLabel.parse("Gradle Build"),
    target = EvidenceTarget.parse("https://github.com/yonatankarp/agent-desk/actions/runs/1"),
)
```
After:
```kotlin
checkRunEvidence("Gradle Build", "https://github.com/yonatankarp/agent-desk/actions/runs/1")
```
Keep inline construction where the test's subject IS label/target parsing.

- [ ] **Step 5: Run the core suite**

Run: `./gradlew :core:allTests 2>&1 | tail -10`
Expected: PASS, same test count as before migration (`git stash && ./gradlew :core:allTests` to compare if unsure, then `git stash pop`).

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add core/
git commit -m "test: migrate core tests to fixture DSL"
```

---

### Task 9: Migrate :app tests

**Files (modify, pattern-driven):** all under `app/src/commonTest` and `app/src/jvmTest` where the patterns below appear. Highest-traffic files: `ActionCapabilityPlannerTest`, `ActionPermissionGateTest`, `MockActionApprovalLoopTest`, `AuditTrailProjectorTest`, `DecisionQueueProjectorTest`, `MobileOperatorStateContractTest`, `ReadOnlyTimelineProjectorTest`, `OperatorStateProjectorTest`, `PrivacyBoundaryRegressionTest`, `OperatorStatePresenterTest`, `MockOperatorActionAdapterTest`, `VerificationEvidenceTest`, `RuntimeWorkEventImporterTest`, `OpenClawRuntimeObservationFixtureTest`.

- [ ] **Step 1: Replace project-then-single chains with `projectedWorkItem` / `operatorState`**

Before:
```kotlin
val state = OperatorStateProjector.project(
    listOf(
        AppFixtures.workStartedEvent(),
        AppFixtures.workBlockedEvent(),
    ),
)
val item = state.workItems.single()
```
After:
```kotlin
val item = projectedWorkItem {
    started()
    blocked()
}
```
Use `operatorState { ... }` where the test needs the whole state. Local helpers that now reduce to one DSL call (e.g. `ActionPermissionGateTest.blockedItem()`) get inlined and deleted.

- [ ] **Step 2: Replace evidence construction with factories**

Before (`DecisionQueueProjectorTest`):
```kotlin
val needsDecision = AppFixtures.workNeedsDecisionEvent().copy(
    evidenceReferences = listOf(
        EvidenceReference(
            kind = EvidenceReferenceKind.SanitizedNote,
            label = EvidenceLabel.parse("Decision context"),
            target = EvidenceTarget.parse("docs/decision-context.md"),
        ),
    ),
)
val state = OperatorStateProjector.project(listOf(AppFixtures.workStartedEvent(), needsDecision))
```
After:
```kotlin
val state = operatorState {
    started()
    needsDecision(evidence = listOf(sanitizedNoteEvidence("Decision context", "docs/decision-context.md")))
}
```

- [ ] **Step 3: Replace public-safety assertion copies with the matcher**

Delete `PrivacyBoundaryRegressionTest.assertPublicSafeOutput` and `MockOperatorActionAdapterTest.assertPublicSafe`; replace every call with:
```kotlin
text.shouldBePublicSafe()
```
Inline denylist blocks (e.g. `OperatorStatePresenterTest`) keep their positive `shouldContain` assertions and replace only the `shouldNotContain` block with the matcher. If any test now fails on a term its old local list didn't check (likely candidate: bare `token`): inspect the output — if it's a real leak, fix production; if the term is over-strict for that output, stop and surface it to the owner instead of weakening the denylist.

- [ ] **Step 4: Swap plain inline repository doubles for the shared one**

Delete the inline `InMemoryWorkEventRepository` in `PrivacyBoundaryRegressionTest` and `FixtureImportRepository` in `OpenClawRuntimeObservationFixtureTest`; import `com.yonatankarp.agentdesk.app.fixtures.InMemoryWorkEventRepository`. Leave `RuntimeWorkEventImporterTest`'s configurable double in place (failure-mode knobs are out of scope).

- [ ] **Step 5: Run the app suite**

Run: `./gradlew :app:allTests 2>&1 | tail -10`
Expected: PASS, same test count as before.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add app/
git commit -m "test: migrate app tests to fixture DSL and shared matchers"
```

---

### Task 10: Migrate :cli tests

**Files (modify):**
- `cli/src/test/kotlin/com/yonatankarp/agentdesk/cli/OperatorConsoleRendererTest.kt`
- `cli/src/test/kotlin/com/yonatankarp/agentdesk/cli/AgentDeskCliTest.kt`

- [ ] **Step 1: Replace hand-assembled entities in `OperatorConsoleRendererTest`**

Before:
```kotlin
WorkItem(
    id = workItemId,
    title = WorkItemTitle.parse("Run public hygiene check"),
    status = WorkStatus.NeedsDecision,
    summary = WorkSummary.parse("Operator decision needed before continuing."),
)
```
After:
```kotlin
WorkEventFixtures.workItem(
    status = WorkStatus.NeedsDecision,
    summary = WorkSummary.parse("Operator decision needed before continuing."),
)
```
And hand-assembled `WorkEvent` blocks become `workEvents { started(evidence = listOf(commitEvidence("Implementation commit", "commit:80de32988617392e1f42e6c4c48c66a56aaae4c4"))) }.single()` or stay as `WorkEventFixtures.workStartedEvent(...)` calls — pick whichever keeps the scenario's intent visible. The renderer's expected output strings must not change; if a canonical fixture value differs from the literal the test used, keep the explicit override rather than changing expectations.

- [ ] **Step 2: Replace `assertPublicSafe` in `AgentDeskCliTest`**

Delete the private `assertPublicSafe` companion helper; replace all calls with `result.output.shouldBePublicSafe()` / equivalent. Same escalation rule as Task 9 Step 3 if the stricter union list fails on CLI output. Keep `privateLinuxPath` (it constructs unsafe input, not an assertion).

- [ ] **Step 3: Run the cli suite**

Run: `./gradlew :cli:test 2>&1 | tail -10`
Expected: PASS, same test count.

- [ ] **Step 4: Commit**

```bash
./gradlew spotlessApply
git add cli/
git commit -m "test: migrate cli tests to fixture DSL and shouldBePublicSafe"
```

---

### Task 11: Migrate :desktop and :mobile tests

**Files (modify):**
- `desktop/src/jvmTest/kotlin/com/yonatankarp/agentdesk/desktop/DesktopSmokeSnapshotTest.kt`
- `desktop/src/jvmTest/kotlin/com/yonatankarp/agentdesk/desktop/DesktopRuntimeStateProviderTest.kt`
- `mobile/src/jvmTest/kotlin/com/yonatankarp/agentdesk/mobile/MobileSmokeSnapshotTest.kt`
- Do NOT touch `DesktopComposeSmokeTest.kt` / `MobileComposeSmokeTest.kt` (experimental-API debt #279 stays isolated).

- [ ] **Step 1: Replace inline public-safety checks with the matcher**

Before (`DesktopSmokeSnapshotTest`):
```kotlin
text shouldNotContain ("/" + "home/")
text shouldNotContainIgnoringCase "discord"
text shouldNotContainIgnoringCase "token"
text shouldNotContainIgnoringCase "op://"
text shouldNotContainIgnoringCase "raw transcript"
```
After:
```kotlin
text.shouldBePublicSafe()
```

- [ ] **Step 2: Replace hand-assembled core entities with fixtures**

Same rewrites as Task 10 Step 1 wherever `WorkItem(...)`/`WorkEvent(...)` are hand-built (e.g. `DesktopRuntimeStateProviderTest.startedEvent()` collapses to `WorkEventFixtures.workStartedEvent(...)` with only the locally-meaningful overrides kept). `sectionRows()` helpers and `MobileWorkItem` assembly stay as they are (out of scope).

- [ ] **Step 3: Run both suites**

Run: `./gradlew :desktop:allTests :mobile:allTests 2>&1 | tail -10`
Expected: PASS, same test counts.

- [ ] **Step 4: Commit**

```bash
./gradlew spotlessApply
git add desktop/ mobile/
git commit -m "test: migrate desktop and mobile tests to fixture DSL"
```

---

### Task 12: Full verification, coverage, and docs

**Files:**
- Modify: `docs/engineering-style.md`

- [ ] **Step 1: Run the full suite and coverage gates**

Run: `make test && make` (or the kover target the Makefile defines, e.g. the one wrapping `koverVerify` for all modules — check `Makefile` line 65+)
Expected: all module tests PASS; kover thresholds (core/app/desktop/mobile 90%, cli 88%) still met. The DSL lives in `:test-fixtures` which has no kover gate, so coverage must not regress — if a module's coverage dropped, a migration deleted an assertion; find and restore it.

- [ ] **Step 2: Run hygiene and format checks**

Run: `make hygiene && ./gradlew spotlessCheck`
Expected: PASS

- [ ] **Step 3: Document the DSL in `docs/engineering-style.md`**

Find the existing test-conventions section (the one containing "Avoid duplicated test data. Prefer fixtures and a small test DSL") and extend it with:

```markdown
- The shared test DSL lives in `:test-fixtures`: `workEvents { started(); blocked() }` for event
  chains, `eventTimestampAt(...)` for deterministic timestamps, `commitEvidence(...)`-style factories
  for evidence references, and `shouldBePublicSafe()` / `shouldBeEmptyProjection()` matchers.
  App-layer projection helpers (`operatorState { }`, `projectedWorkItem { }`) live in
  `app/src/commonTest/.../fixtures/`. New tests use these instead of hand-built domain objects;
  the public-safety denylist is maintained only in `PublicSafetyMatchers.kt`.
```

Match the file's existing prose style (no em dashes, concise).

- [ ] **Step 4: Final commit**

```bash
./gradlew spotlessApply
git add docs/
git commit -m "docs: document the shared test fixture DSL"
```

---

## Self-Review Notes

- Spec coverage: Layer 1 (Tasks 2–3 + entity reuse in Tasks 10–11), Layer 2 (Task 4 + app helpers Task 7), Layer 3 (Tasks 5–6); migration of all five modules (Tasks 8–11); verification + docs (Task 12). The `entries.map { it.eventId }.shouldContainExactly(...)` fusion matcher from the proposal was dropped: it operates on app-local timeline types, saves one `.map`, and isn't worth an app-test-only matcher (YAGNI).
- Type consistency: `eventTimestampAt(minute, hour, second)` (Task 2) is used with named args in Tasks 4, 8; `workEvents`/`WorkEventSequenceBuilder` names match between Tasks 4, 7, 8, 9; matcher names `shouldBePublicSafe`/`shouldBeEmptyProjection` consistent across Tasks 5, 6, 8–11.
- Known verify-before-writing points (signatures not confirmed from source): `ModuleArchitectureRules.assertKotestOnly` params (Task 1), `WorkEventProjector.project` signature (Task 6), `WorkEventReadResult` constructor (Task 7). Each task says to check the real source first.
