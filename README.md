# Unified UI + API Automation Framework

Cucumber + TestNG framework for UI and API test automation.

## Current Status: Phase 0 (Project Skeleton)

The project is in scaffolding state. All four Maven modules exist with proper
parent-child wiring and dependency management, but contain no logic yet.
This is intentional — Phase 0 verifies the build infrastructure works
before any code is written.

## Module Structure

```
unified-framework/                 (parent POM, packaging=pom)
├── framework-common/              shared infra (config, context, reporting)
├── framework-api/                 depends on common — API testing
├── framework-ui/                  depends on common — UI testing
└── framework-tests/               depends on api+ui — runners, features, hooks
```

## Verifying Phase 0

```bash
mvn clean install
```

Expected: BUILD SUCCESS for all 5 modules in this order:
1. unified-framework (parent)
2. framework-common
3. framework-api
4. framework-ui
5. framework-tests

If the build fails, the reactor order or parent inheritance is broken.

## Useful diagnostics

```bash
mvn dependency:tree -pl framework-tests       # see all deps for the test module
mvn help:effective-pom -pl framework-api      # see fully resolved POM
mvn validate                                  # POM structure check (no compile)
```

## What's coming next

- Phase 1: framework-common — OWNER config + Log4j2 wiring
- Phase 2: TestContext, ExtentReports, AssertionService
- Phase 3: First end-to-end Spotify API test
- ...through Phase 11

See the project plan for the full sequence.

## Prerequisites

- Java 17 (Java 21 also works)
- Maven 3.8+
