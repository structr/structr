# Structr Code Style

A short guide to how Structr code is written. Most of it is about consistency rather than taste: when
everything is laid out the same way, a diff shows what actually changed and nothing else.
`CONTRIBUTING.md` links here.

## Formatting

Indent with **tabs**. End every file with a single newline, skip trailing whitespace, and avoid two
blank lines in a row.

Each file starts with the license header of its own module. The wording differs between modules, so
the easiest way to get it right is to copy `<module>/header.txt`, or let `mvn license:format` insert
it for you.

The opening brace goes at the end of the line that starts the block, not on a line of its own, and
blocks are always braced, even when the body is a single statement:

```
if (child == null) {

    continue;
}
```

Method and constructor parameters are declared `final`.

Lines can run up to **200 columns**. That is deliberately generous, so that wrapping is something you
do when a line really is long rather than a habit. When you do wrap, break where it helps the reader:
one argument per line, or before each `&&`. Aligning assignments into columns is fine but never
expected.

## Blank lines

This is the part that surprises most people, so it is worth reading once. Blank lines are used to
group code, which means they carry information and no autoformatter can put them back if they are
lost.

The result is an airy layout, and that is the intention rather than an accident: about a quarter of a
Structr source file is blank. What it buys is that you can see the shape of a method before you read
it, which counts for more as the codebase grows. What it costs is lines on screen. So if the spacing
looks over-generous, it is meant to be, and closing it up file by file mostly makes the code less
predictable to read.

The pattern is easier to absorb from a file than from a list, and
`structr-modules/structr-process-module/src/main/java/org/structr/process/bpmn/interop/Xml.java` is a
good short example. In words:

1. **After an opening `{`.** Every method and type body starts with a blank line, and so does every
   `if`, `for`, `while`, `try`, `else`, `catch` and `finally`. Yes, this includes one-line getters.
2. **Assignments stay together.** A run of declarations or assignments has no blank lines inside it.
   The blank line goes after the run, where the logic begins.
3. **A check sticks to the line it checks.** When you assign something and immediately test it for
   null, empty or a sentinel value like `indexOf(...) < 0`, leave no blank line before the `if`,
   because the check belongs with the assignment above it. If two or more assignments come first,
   rule 2 wins and the run keeps its trailing blank line.
4. **Blocks stand alone.** A control block has a blank line before it and after its closing `}`, and
   `else`, `catch` and `finally` stay on the same line as the brace, as `} else {`.
5. **Before `return`.** Every `return` has a blank line in front of it, unless it is the first thing
   in a block and rule 1 already put one there. Nothing follows a `return` that ends a method.

## Naming, imports and layout

Names follow ordinary Java conventions. Most constants use upper snake case, though a good number of
long-lived objects, such as the entries in `Settings`, use camel case instead, so the practical advice
is to match the file you are in. A class logger is called `logger` and is the first member of the
class, with constants, fields, constructors and then methods after it.

Imports put `java.*` and `javax.*` last, after everything else, with static imports at the very
bottom. Wildcard imports are common in the codebase and there is no campaign to expand them.

## Comments

Comments are where the reasoning lives, so they are worth writing carefully. The useful ones explain
why the code is the way it is, particularly when that is not the obvious way: the constraint you ran
into, the bug that motivated it, the simpler approach that turned out not to work. Length is fine when
the reason is real, and a note like that does far more good next to the code than in a document,
because it is what keeps someone from quietly removing the odd-looking line next year. What the code
does is usually best left to the code.

## Tools

Formatters with opinions of their own (google-java-format, Palantir, strict Eclipse or IntelliJ
profiles) normalise blank lines away, which loses the grouping described above, so we do not use one.
These help instead:

- **`.editorconfig`** in the repo root sets tabs, the final newline and whitespace trimming. IntelliJ,
  Eclipse and VS Code all read it without a plugin.
- **`config/style/StyleLint.java`** takes care of the blank-line rules for you. It is a single Java
  file, so there is nothing to install: `java config/style/StyleLint.java --check <paths>` lists what
  it would change and `--fix <paths>` applies it. It also pulls a wrapped condition or argument list
  back onto one line when it fits, and it only ever moves whitespace around. It is not part of the
  build, so run it when you feel like it.
- **Checkstyle** (`config/checkstyle/checkstyle.xml`) covers the mechanical points: braces, final
  parameters, line length, trailing whitespace, one statement per line. It is opt-in and only ever
  warns: `mvn -Pcheckstyle checkstyle:check`.
- **License headers** are checked by `mvn license:check` and inserted by `mvn license:format`.
- **If you reformat in an IDE**, tell it to leave blank lines alone. In IntelliJ that is "Keep maximum
  blank lines in code" set to 1, with blank-line enforcement switched off.

Two more reports run on every build and look at the code and the test logs rather than at formatting.
They are described under **Build reports** in [CONTRIBUTING.md](CONTRIBUTING.md).
