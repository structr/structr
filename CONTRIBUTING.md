# Contributing to Structr

Structr is developed in the open, and we are glad you are here. This page covers where to ask
questions, how to report a bug, and what happens to a pull request.

## Where to ask questions

The **[Structr Forum](https://structr.org/forum)** is the best place for usage questions,
architecture and design discussions, feature ideas, and anything still taking shape, including "is
this a bug or am I holding it wrong?". Asking there first also keeps the issue tracker readable.

## Reporting a bug

The GitHub issue tracker works best when it holds bugs that someone can reproduce, so it helps if you
have seen the problem twice and can describe the path to it. Useful things to include: the Structr
version, how it is deployed, the steps you took, what you expected, what happened instead, and any
log output you have.

If you are not sure yet whether it is a bug, the forum is a friendlier place to find out.

## Contributing code

The usual path: talk about it first, fork the repository, branch off `main`, make the change, check
that it builds and the tests pass, and open a pull request. Smaller pull requests get read sooner, so
one change at a time is ideal. A short description and a link to the forum thread or issue give a
reviewer everything they need.

If a change would break backward compatibility, please raise it before writing the code, since that
question is usually about product decisions rather than about the patch.

Formatting conventions and the tools that help with them are in **[CODE_STYLE.md](CODE_STYLE.md)**.

## Running tests

Structr targets Java 25 (GraalVM). Tests run against one of two database backends, chosen by profile.

**In-memory**, which is fast and needs no Docker:

```
mvn -o test -Ddatabase=in-memory -Dsurefire.failIfNoSpecifiedTests=false
```

Add `-o` once a first build has cached its dependencies. In-memory forks ask for a lot of memory
(`-Xms8g -Xmx8g` each), so lower `-Dtest.forkCount` if your machine has less to spare.

**Neo4j**, the default profile: `mvn verify` starts a Dockerised Neo4j for you and runs the
integration tests against it. To use an instance of your own, pass `-DskipDockerTestDB=true` and
`-Denv.testDatabaseConnection=bolt://<host>:7687`.

One class at a time:

```
mvn -o -pl structr-base test -Ddatabase=in-memory -Dtest='StorageSyncServiceTest' -Dsurefire.failIfNoSpecifiedTests=false
```

One thing that catches people out: run tests through Maven rather than the IDE's own test runner.
Tests execute on the class path (`useModulePath=false`), and starting a test class directly from an
IDE puts it on the module path, where it fails with
`InaccessibleObjectException: ... does not "exports org.structr.test.web" ...`, because test packages
are intentionally not exported from `structr.base`. In IntelliJ, switching on *"Delegate IDE
build/run actions to Maven"* under Build Tools, Maven, Runner is enough. The **Toolchain** section of
`DEPENDENCY_MANAGEMENT.md` explains why it is set up that way.

Test classes share one lifecycle whichever base class they use: `setup()` runs before the class and
finishes by calling `createSchema()`, which is a plain method you can override, and `teardown()` runs
after the class.

## Build reports

Two small reports appear after the reactor summary on every build, whether it succeeded or not.
Both are single Java files run straight from source, so there is nothing to install, and neither can
fail your build. They are suggestions, not gates.

**Code quality** (`config/style/CodeQuality.java`) ranks files by how much they would probably
benefit from a careful read, using around twenty maintainability signals such as complexity, long
methods, hand-written parsing, broad catch blocks, reflection and unexplained numbers. You can run it
yourself with `java config/style/CodeQuality.java [--top N] [--by SIGNAL] [--main-only] <paths>`. If
you have read a file and are happy with it, add `@code-quality:accept` in a comment together with your
reason and it drops off the list. `-DskipCodeQuality=true` hides the report.

**Test-log review** (`config/testlog/TestLogReview.java`) looks for the things a green build hides:
exceptions that were logged and then swallowed, messages at the wrong level, tests that pass while
their own log output says otherwise. None of that shows up in a surefire report. The build prints a
four-line `[test-log]` summary, which it can do without any piping because surefire and failsafe
already capture test output into `target/*-reports/TEST-*.xml`. `-DskipTestLogReview=true` hides it.

For the whole report, hand it a saved log, which has Maven's own output in it as well:

```
mvn verify 2>&1 | tee /tmp/run.log
java config/testlog/TestLogReview.java /tmp/run.log
```

Handy flags: `--section passed|calibration|unowned` for the per-test views, `--top N` for more rows,
`--ignore REGEX` for noise you know about, and `--fail-on-new` if you want CI to be strict.

`config/testlog/normal.baseline` is a record of the log output we currently live with, so a run can
point out only what is new or has grown. It hides nothing: every section is still computed and printed
in full, and the baseline simply adds the "what is new" view. `--write-baseline` refreshes it, and CI
is the better place to do that, since a baseline written on a laptop carries that machine's JDK
version and language settings with it. A refresh is also how noise quietly becomes normal, so it is
worth keeping in its own commit where someone can see it; the file's header says which run it came
from.

Both reports come from `.mvn/lib/structr-build-extension.jar`, which is committed and loaded through
`.mvn/maven.config`, so a fresh clone shows them straight away. If you change that extension, rebuild
the jar and commit it:

```
mvn -o -pl structr-build-extension package && cp structr-build-extension/target/structr-build-extension.jar .mvn/lib/
```

Both tools carry their reasoning in their own javadoc, next to the code it describes.

## Licensing of contributions

By contributing you agree that your work may be licensed on the same terms as the project:
AGPL-3.0-or-later, GPL-3.0-or-later, and/or a commercial license offered by Structr GmbH.

## Community conduct

Please be kind and constructive, in the forum, in issues and in review.
