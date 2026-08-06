# Structr Code Style

The Java house style for Structr. New and changed code should match it, and it's what we look
for on review. `CONTRIBUTING.md` links here.

## Formatting conventions

Indentation is **tabs**; every source file carries the AGPL header and ends with a single
trailing newline. No trailing whitespace, and never more than one consecutive blank line.
Braces are K&R (opening brace at the end of the line) and **every** control statement is
braced, even a single-statement `if`/`for`.

Blank lines are used *semantically* — the spacing is a readability signal, not uniform
padding — so they cannot be reproduced by an auto-reformatter (see **Tooling** below):

1. **Method / type bodies:** a blank line after the opening `{` of a *multi-statement*
   body. One-line bodies (getters, delegators) stay tight — no blank line.
2. **Assignments group together:** a run of consecutive declarations/assignments has no
   blank lines between them; the blank goes *after* the group, before the logic begins.
   **This grouping takes precedence over guard binding (rule 3), always:** a guard that
   follows a *group* of two or more assignments is separated from it by the group's trailing
   blank line — the group is never broken open to weld a guard onto its last line. Only a
   *single* preceding assignment binds a guard with no blank between them.
3. **Guards bind to the assignment above.** A null/empty/blank check (`== null`, `!= null`,
   `.isEmpty()`, `StringUtils.isBlank(...)`, …) that immediately follows the assignment of
   the variable it guards has **no** blank line between that assignment and the `if` — the
   check belongs to the line above. (Per rule 2, a *group* of 2+ assignments still wins: its
   trailing blank separates the guard.) This is only about the blank *before* the `if`; the
   blank *after* the `{` is still required — see rule 4.
4. **Every control block gets a blank line after its opening `{`** — `if` (guards included),
   `for`, `while`, `try` — and is separated from surrounding statements by a blank line. A
   guard therefore reads:
   ```java
   final Node child = children.item(i);
   if (child == null) {

       continue;
   }
   ```
5. **Returns:** a blank line before a `return` (except a guard's compact exit, per 3); no
   blank line after a `return` that ends the method.

Column-aligning assignments is *not* required. `structr-modules/structr-process-module/src/main/java/org/structr/process/bpmn/interop/Xml.java` is a compact reference for all of the above.

## Tooling

No auto-formatter can *produce* the semantic blank-line rules above; opinionated
reformatters (google-java-format, Palantir, aggressive Eclipse/IntelliJ profiles) will
actively **destroy** them by normalizing blank lines, so we do not adopt one. Instead:

- **`.editorconfig`** (repo root) — tabs, final newline, trailing-whitespace trim; honored
  natively by IntelliJ, Eclipse and VS Code.
- **Checkstyle** (`config/checkstyle/checkstyle.xml`) — the mechanical/structural rules
  (always-brace, K&R, no trailing whitespace, no double blank lines, one-statement-per-line).
  Opt-in and non-failing: it does **not** run on a normal build; invoke `mvn -Pcheckstyle
  checkstyle:check` to get it as **warnings** (`failOnViolation=false`). It cannot express the
  semantic blank-line rules.
- **AGPL header** — already enforced by `com.mycila:license-maven-plugin` (`mvn license:check`;
  `mvn license:format` inserts it). Deliberately not re-checked by Checkstyle, to avoid duplication.
- **IDE reformat**, if used, must be set to *preserve* blank lines (IntelliJ: "Keep maximum
  blank lines in code" = 1; do not enable blank-line enforcement).
- **Semantic linter** (`config/style/style_lint.py`) — `--check <paths>` reports, and
  `--fix <paths>` applies, the blank-line rules stock tools can't express: it *inserts*
  the blank after every multi-statement method open and every control-block open, and
  *removes* stray blanks (one-line method bodies, a blank between an assignment and its bound
  guard, blanks that split a run of consecutive assignments, runs of 2+ blank lines). It only
  ever inserts or deletes blank lines, so it cannot change behaviour. The fuzzy
  blank-before-return rule stays report-only. Not wired into the build; run on demand.
- **Review-need scorer** (`config/style/ReviewNeed.java`) — a heuristic *triage* tool (not a gate)
  that ranks files by likely review value from ~19 signals (cyclomatic/cognitive complexity, long
  methods, C-style int loops, hand-rolled parsing, excessive switch, should-be-enum string clusters,
  broad/empty catches, reflection, concurrency, magic numbers, …). Run ad-hoc via the JDK source
  launcher (no Python, no build): `java config/style/ReviewNeed.java [--top N] [--by SIGNAL]
  [--main-only] <paths>`. On every build **run from the repo root**, a Maven **lifecycle extension**
  prints the `[review-need]` top-list as the **last output — after the reactor summary, on success
  and failure alike** (it hooks `afterSessionEnd`, the only spot that always runs, and shells out to
  `ReviewNeed.java`). It always exits cleanly, so it **never fails the build**; `-DskipReviewNeed=true`
  turns it off, and running `mvn` from a submodule directory simply skips it (never fails). The
  extension ships **prebuilt and committed** at `.mvn/lib/structr-build-extension.jar`, loaded straight
  from that path via `.mvn/maven.config` (`-Dmaven.ext.class.path=…`) — so a fresh clone needs **no
  install, no Nexus, no bootstrap**: the report just appears. Its source is the `structr-build-extension`
  module; if you change it, refresh the committed jar with
  `mvn -o -pl structr-build-extension package && cp structr-build-extension/target/structr-build-extension.jar .mvn/lib/`
  and commit the jar. Reviewed-and-acceptable files are dismissed **in the
  source file**: put the marker `@review-need:accept` in any comment with your reason next to it
  (the tool only checks that the marker string is present — it never reads the reason) and the
  file drops off the list, in the CLI and the build alike. Remove the marker to un-accept;
  `--show-accepted` lists what's hidden.
