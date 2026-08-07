# Contributing to Structr

Thank you for your interest in contributing to Structr!

Structr is developed openly and contributions from the community are welcome.
This document outlines how to ask questions, report issues, and contribute
code or documentation.

---

## First: Where to Ask Questions

Before submitting issues or pull requests, please use the **Structr Forum** for
questions, ideas, and general discussions:

👉 https://structr.org/forum

The forum is the preferred place for:
- Usage questions
- Architecture and design discussions
- Feature ideas
- Conceptual or exploratory topics
- Clarifying whether something is a bug or expected behavior

This helps keep the GitHub issue tracker focused and actionable.

---

## Reporting Issues (GitHub)

Please use the GitHub issue tracker **only for confirmed bug reports**.

Before opening an issue:
- Check the forum to see whether the topic has already been discussed
- Ensure the issue describes a reproducible bug

When reporting a bug, include:
- Structr version
- Deployment setup
- Steps to reproduce
- Expected and actual behavior
- Relevant logs or error messages (if available)

---

## Code Contributions

Once a topic or solution has been discussed (e.g. in the forum), code
contributions are welcome.

To contribute code:

1. Fork the repository
2. Create a feature branch from `main`
3. Make your changes
4. Ensure code builds and tests pass
5. Submit a pull request

Pull requests should:
- Focus on a single change
- Include a clear description
- Reference related forum discussions or GitHub issues if applicable

---

## Coding Style & Quality

- Follow existing coding conventions
- Prefer clarity and readability
- Add documentation where appropriate
- Avoid breaking backward compatibility unless discussed beforehand

Formatting conventions (indentation, the semantic blank-line rules) and the style tooling
(`.editorconfig`, Checkstyle, the semantic linter, the review-priority scorer) live in
**[CODE_STYLE.md](CODE_STYLE.md)**.

---

## Running Tests

Structr targets Java 25 (GraalVM). Tests run against one of two database backends,
selected by Maven profile:

- **In-memory (fast, no Docker):**
  ```
  mvn -o test -Ddatabase=in-memory -Dsurefire.failIfNoSpecifiedTests=false
  ```
  Add `-o` only after the first build has cached dependencies. In-memory forks are
  memory-heavy (`-Xms8g -Xmx8g` each), so cap `-Dtest.forkCount` to fit your RAM.

- **Neo4j (default profile):** `mvn verify` runs the integration tests against a
  Dockerized Neo4j started automatically (requires Docker). Pass
  `-DskipDockerTestDB=true` and `-Denv.testDatabaseConnection=bolt://<host>:7687`
  to point at your own instance instead.

Run a single class (fast path):
```
mvn -o -pl structr-base test -Ddatabase=in-memory -Dtest='StorageSyncServiceTest' -Dsurefire.failIfNoSpecifiedTests=false
```

> **Run tests through Maven, not the IDE's own runner.** Tests execute on the
> **class path** (`useModulePath=false`); launching a test class directly from an
> IDE runs it on the **module path** and fails with
> `InaccessibleObjectException: ... does not "exports org.structr.test.web" ...`,
> because test packages are intentionally not exported from `structr.base`. In
> IntelliJ, enable *"Delegate IDE build/run actions to Maven"* (Build Tools →
> Maven → Runner) so IDE test runs use the profile configuration above. See the
> **Toolchain** section of `DEPENDENCY_MANAGEMENT.md` for the rationale.

---

## Licensing of Contributions

By submitting a contribution, you agree that your contribution may be licensed
under the same terms as the project:

- AGPL-3.0-or-later
- GPL-3.0-or-later
- and/or a commercial license offered by Structr GmbH

---

## Community Conduct

Please be respectful and constructive in all interactions.
See `CODE_OF_CONDUCT.md` for details.

Thank you for contributing to Structr!

