# Structr Code Style

The Java house style for Structr. New and changed code should match it, and it's what we look
for on review. `CONTRIBUTING.md` links here.

## Formatting conventions

Indentation is **tabs**; every source file carries the AGPL header and ends with a single
trailing newline. No trailing whitespace, and never more than one consecutive blank line.
Braces are K&R (opening brace at the end of the line) and **every** control statement is
braced, even a single-statement `if`/`for`. Method and constructor parameters are always
declared `final`. Lines stay under **200 columns** — a soft ceiling for side-by-side diffs and
review panes, not the old 80/100 rule; wrap only past it, and break at meaningful boundaries
(one argument per line, or before each `&&`), never arbitrarily at a column.

Blank lines are used *semantically* — the spacing is a readability signal, not uniform
padding — so they cannot be reproduced by an auto-reformatter (see **Tooling** below):

1. **Method / type bodies:** a blank line after the opening `{` of *every* body — including
   single-statement getters and delegators. No compact one-liners.
2. **Assignments group together:** a run of consecutive declarations/assignments has no
   blank lines between them; the blank goes *after* the group, before the logic begins.
   **This grouping takes precedence over guard binding (rule 3), always:** a guard that
   follows a *group* of two or more assignments is separated from it by the group's trailing
   blank line — the group is never broken open to weld a guard onto its last line. Only a
   *single* preceding assignment binds a guard with no blank between them.
3. **Guards bind to the assignment above.** A short check on the variable a line just
   assigned — a null/empty/blank check (`== null`, `.isEmpty()`, `StringUtils.isBlank(...)`)
   *or a numeric-sentinel check* (`indexOf(...) < 0`, `== -1`, `size() == 0`, …) — that
   immediately follows that assignment has **no** blank line between it and the `if`; the
   check belongs to the line above. (Per rule 2, a *group* of 2+ assignments still wins: its
   trailing blank separates the guard.) This is only about the blank *before* the `if`; the
   blank *after* the `{` is still required — see rule 4.
4. **Every control block gets a blank line after its opening `{`** — `if` (guards included),
   `for`, `while`, `try`, and the `else`/`catch`/`finally` continuations — and is separated
   from the surrounding code by a blank line: one before it, one after its closing `}`, and one
   before each `else`/`catch`/`finally` (which stay cuddled: `} else {`). A guard therefore reads:
   ```java
   final Node child = children.item(i);
   if (child == null) {

       continue;
   }
   ```
5. **Returns:** a blank line before *every* `return` — a guard's return already gets it from
   the blank after its `{` (rule 4). No blank line after a `return` that ends the method, and
   no compact one-line exits (`if (x == null) { return; }` gets the blank, per rule 4).

Column-aligning assignments is *not* required. `structr-modules/structr-process-module/src/main/java/org/structr/process/bpmn/interop/Xml.java` is a compact reference for all of the above.

## Tooling

No auto-formatter can *produce* the semantic blank-line rules above; opinionated
reformatters (google-java-format, Palantir, aggressive Eclipse/IntelliJ profiles) will
actively **destroy** them by normalizing blank lines, so we do not adopt one. Instead:

- **`.editorconfig`** (repo root) — tabs, final newline, trailing-whitespace trim; honored
  natively by IntelliJ, Eclipse and VS Code.
- **Checkstyle** (`config/checkstyle/checkstyle.xml`) — the mechanical/structural rules
  (always-brace, K&R, final parameters, 200-column line length, no trailing whitespace, no double
  blank lines, one-statement-per-line).
  Opt-in and non-failing: it does **not** run on a normal build; invoke `mvn -Pcheckstyle
  checkstyle:check` to get it as **warnings** (`failOnViolation=false`). It cannot express the
  semantic blank-line rules.
- **AGPL header** — already enforced by `com.mycila:license-maven-plugin` (`mvn license:check`;
  `mvn license:format` inserts it). Deliberately not re-checked by Checkstyle, to avoid duplication.
- **IDE reformat**, if used, must be set to *preserve* blank lines (IntelliJ: "Keep maximum
  blank lines in code" = 1; do not enable blank-line enforcement).
- **Semantic linter** (`config/style/StyleLint.java`) — a single-file Java program run via the
  JDK source launcher (no Python, no build): `java config/style/StyleLint.java --check <paths>`
  reports, `--fix <paths>` applies (and converges). It *inserts* a blank after every method/type
  open and every control-block open (`else`/`catch`/`finally` included), before every control block
  that follows a statement (except a guard bound to the assignment above), after every control
  block's closing `}` and before every `else`/`catch`/`finally`, after every run of two or more
  consecutive assignments, and before every `return`; and *removes* stray blanks (a blank between an
  assignment and its bound guard, blanks that split a run of consecutive assignments, runs of 2+
  blank lines). It also *joins* a wrapped `&&`/`||` chain or argument list back onto one line when
  the result fits in 200 columns (R9) — the one rule that edits code rather than blank lines, but a
  whitespace-only merge guarded against comments, annotations and text blocks, so it still preserves
  tokens and behaviour (a chain that would still exceed 200 is left wrapped). Not wired into the
  build; run on demand.
- **Review-priority scorer** (`config/style/ReviewPriority.java`) — a heuristic *triage* tool (not a gate)
  that ranks files by likely review value from ~20 signals (cyclomatic/cognitive complexity, long
  methods, C-style int loops, hand-rolled parsing, excessive switch, should-be-enum string clusters,
  broad/empty catches, reflection, concurrency, magic numbers, over-long lines, …). Run ad-hoc via the JDK source
  launcher (no Python, no build): `java config/style/ReviewPriority.java [--top N] [--by SIGNAL]
  [--main-only] <paths>`. On every build **run from the repo root**, a Maven **lifecycle extension**
  prints the `[review-priority]` top-list as the **last output — after the reactor summary, on success
  and failure alike** (it hooks `afterSessionEnd`, the only spot that always runs, and shells out to
  `ReviewPriority.java`). It always exits cleanly, so it **never fails the build**; `-DskipReviewPriority=true`
  turns it off, and running `mvn` from a submodule directory simply skips it (never fails). The
  extension ships **prebuilt and committed** at `.mvn/lib/structr-build-extension.jar`, loaded straight
  from that path via `.mvn/maven.config` (`-Dmaven.ext.class.path=…`) — so a fresh clone needs **no
  install, no Nexus, no bootstrap**: the report just appears. Its source is the `structr-build-extension`
  module; if you change it, refresh the committed jar with
  `mvn -o -pl structr-build-extension package && cp structr-build-extension/target/structr-build-extension.jar .mvn/lib/`
  and commit the jar. Reviewed-and-acceptable files are dismissed **in the
  source file**: put the marker `@review-priority:accept` in any comment with your reason next to it
  (the tool only checks that the marker string is present — it never reads the reason) and the
  file drops off the list, in the CLI and the build alike. Remove the marker to un-accept;
  `--show-accepted` lists what's hidden.

- **Test-log reviewer** (`config/testlog/TestLogReview.java`) — triage for a Maven/TestNG log,
  because a green build hides plenty: exceptions that were logged and swallowed, WARN storms,
  messages at the wrong level, tests that pass only because a negative case fired. Surefire reports
  contain none of that. Normalises away timestamps, UUIDs, numbers, tenant ids and paths so hundreds
  of occurrences of one message collapse to a single counted line, then ranks: logged/swallowed
  exceptions first (the valuable ones — no test reports them), then errors, warnings by logger,
  problem phrasings at INFO level, and the slowest classes. Run
  `mvn … 2>&1 | tee /tmp/run.log && java config/testlog/TestLogReview.java /tmp/run.log`;
  `--top N`, `--min-count N`, `--section NAME`, `--ignore REGEX` (repeatable, for known-expected
  noise such as deliberate negative tests). A count is a pointer, not a verdict.
