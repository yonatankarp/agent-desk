SHELL := /bin/bash
.SHELLFLAGS := -eu -o pipefail -c

GRADLE ?= ./gradlew

.DEFAULT_GOAL := help

.PHONY: help
help: ## Show available project entrypoints.
	@awk 'BEGIN {FS = ":.*##"; printf "Agent Desk make targets:\n\n"} /^[a-zA-Z0-9_.-]+:.*##/ {printf "  %-26s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

.PHONY: hygiene
hygiene: ## Run the public-safe tracked-file hygiene check.
	bash scripts/validate-public-hygiene.sh

.PHONY: hygiene-self-test
hygiene-self-test: ## Run the public hygiene scanner self-test.
	bash scripts/validate-public-hygiene.sh --self-test

.PHONY: format-check
format-check: ## Check deterministic formatting with Spotless.
	$(GRADLE) spotlessCheck

.PHONY: format
format: ## Apply deterministic formatting with Spotless.
	$(GRADLE) spotlessApply

.PHONY: build
build: ## Build all current modules.
	$(GRADLE) :core:build :test-fixtures:build :app:build :cli:build :desktop:build :mobile:build

.PHONY: test
test: ## Run all module test tasks covered by the standard build.
	$(GRADLE) :core:allTests :test-fixtures:allTests :app:allTests :cli:test :desktop:allTests :mobile:allTests

.PHONY: check
check: hygiene format-check build ## Run the default local pre-PR check set.

.PHONY: smoke
smoke: smoke-mock smoke-mobile smoke-compose smoke-sanitized-runtime smoke-host-lab ## Run all public-safe smoke workflows.

.PHONY: smoke-local-first
smoke-local-first: hygiene replay-sanitized-runtime smoke-mock ## Run the core local-first operator loop smoke without external services.

.PHONY: smoke-mock
smoke-mock: ## Run the mock runtime/operator smoke.
	bash scripts/mock-runtime-smoke.sh

.PHONY: smoke-mobile
smoke-mobile: ## Run the mobile read-only smoke.
	bash scripts/mobile-read-only-smoke.sh

.PHONY: smoke-compose
smoke-compose: ## Run the non-interactive desktop/mobile Compose run smoke.
	bash scripts/compose-run-smoke.sh

.PHONY: smoke-sanitized-runtime
smoke-sanitized-runtime: replay-sanitized-runtime ## Run the sanitized runtime observation import smoke.

.PHONY: smoke-host-lab
smoke-host-lab: ## Run the public-safe simulated host connectivity lab.
	bash scripts/host-connectivity-lab-smoke.sh

.PHONY: replay-sanitized-runtime
replay-sanitized-runtime: ## Run the canonical sanitized replay scenario.
	bash scripts/canonical-sanitized-replay-smoke.sh

.PHONY: coverage
coverage: ## Verify Kover thresholds and generate XML/HTML reports for testable modules.
	$(GRADLE) \
		:core:koverVerify :core:koverXmlReport :core:koverHtmlReport \
		:app:koverVerify :app:koverXmlReport :app:koverHtmlReport \
		:cli:koverVerify :cli:koverXmlReport :cli:koverHtmlReport \
		:desktop:koverVerify :desktop:koverXmlReport :desktop:koverHtmlReport \
		:mobile:koverVerify :mobile:koverXmlReport :mobile:koverHtmlReport

.PHONY: ci-local
ci-local: hygiene hygiene-self-test format-check build smoke ## Run the local equivalent of CI-adjacent checks.

.PHONY: cli-jar
cli-jar: ## Build the standalone executable CLI jar.
	$(GRADLE) :cli:executableJar

.PHONY: cli-smoke
cli-smoke: cli-jar ## Smoke-run the standalone executable CLI jar.
	java -jar cli/build/libs/agent-desk-cli-all.jar

.PHONY: release-gate
release-gate: hygiene format-check ## Run local release gates that do not tag, publish, or require secrets.
	$(GRADLE) :cli:test
	bash scripts/mock-runtime-smoke.sh
	$(GRADLE) :cli:executableJar
	java -jar cli/build/libs/agent-desk-cli-all.jar

.PHONY: desktop-run
desktop-run: ## Run the sample Compose desktop shell.
	$(GRADLE) :desktop:run

.PHONY: mobile-run
mobile-run: ## Run the sample-only Compose mobile shell.
	$(GRADLE) :mobile:run
