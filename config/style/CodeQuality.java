/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Review-priority triage scorer for Java files. A heuristic prioritiser (NOT a quality verdict):
 * it ranks files by how much they are likely to reward a human review, from signals drawn from
 * established metrics (cyclomatic/cognitive complexity, Maintainability Index, PMD/SonarQube
 * smells) plus Structr-specific smells. Regex + brace-depth heuristics, no parser — expect some
 * false positives; treat scores as a "look here first" ordering. Test classes (anything under a
 * src/test directory) are skipped entirely, so test code never shows up in the ranking.
 *
 * Single-file program; runs with the JDK source launcher (no build, no dependencies, no Python):
 *     java config/style/CodeQuality.java [--top N] [--min SCORE] [--by SIGNAL] [--main-only] [--summary] [--detail N] [paths...]
 *
 * Files scoring below --min (default 50) are omitted from both the ranking and the counts; raise it (e.g. --min 100)
 * to see only the noisier files.
 *
 * The ranked table has a leading # position column. --detail N switches to manual-analysis mode
 * for the file at position N: it lists every locatable finding grouped by signal as
 * "Lnnn  <source line>", so a reviewer can walk through and amend them (run it with the same paths
 * and flags used for the table, so N lines up). Its per-line match counts are independent of the
 * ranking's normalisation, so they may differ from the score's flags.
 *
 * With no path it scans the current directory. The Maven build invokes it with --summary
 * --main-only over the repo (non-failing; see the root pom).
 *
 * Accepting a flagged file ("I reviewed it, it's fine"): put the marker  @code-quality:accept
 * anywhere in the file, in any comment style, with your reason written next to it. The tool only
 * checks that the marker string is present -- it never reads the reason -- and drops the file from
 * the ranking. Remove the marker to un-accept. List the accepted files with --show-accepted.
 */
public class CodeQuality {

	// @code-quality:accept -- this analyzer is regex-heavy, prints its results to stdout, and wraps
	// everything in a broad catch BY DESIGN (it is a regex scanner + a CLI that must never fail the
	// build). Those signals are its nature, not defects to refactor; see the class documentation.

	// Built by concatenation so the tool does not match its own detection code -- only an intentional
	// marker (like the one above) accepts a file.
	static final String ACCEPT_MARKER = "@code-quality" + ":accept";

	// Long lines limit (NOT counting tabs!)
	static final int LONG_LINES_LIMIT = 200;

	// Default threshold for findings we only report the worst files
	static final int DEFAULT_THRESHOLD = 50;

	/**
	 * Label for the build-summary lines. Deliberately NOT on Maven's severity axis
	 * (INFO/WARNING/ERROR): this report prioritises, it does not judge, so it is printed in
	 * magenta rather than the yellow or red that would imply something is wrong.
	 */
	static final String LABEL = "code-quality";

	static final String MAGENTA = "\u001B[95m";
	static final String BOLD    = "\u001B[1m";
	static final String DIM     = "\u001B[2m";
	static final String RESET   = "\u001B[0m";

	/** auto: colour only when stderr/stdout is a terminal and the environment does not forbid it. */
	static boolean color = detectColor();

	/** The --by signal, if any. It is always reported, even when its weight is 0 - it is what the table is sorted on. */
	static String rankedBy = null;

	/** Whether a signal is reported at all: switched off (weight 0) means invisible, unless it is the one being ranked by. */
	static boolean reported(final String signal) {

		return W.get(signal) > 0 || signal.equals(rankedBy);
	}

	static boolean detectColor() {

		if (System.getenv("NO_COLOR") != null) {

			return false;
		}

		final String term = System.getenv("TERM");
		if ("dumb".equals(term)) {

			return false;
		}

		return System.console() != null;
	}

	/** The "[code-quality]" prefix every summary line carries, coloured when enabled. */
	static String prefix() {

		return color ? MAGENTA + "[" + LABEL + "]" + RESET : "[" + LABEL + "]";
	}

	static String bold(final String text) {

		return color ? BOLD + text + RESET : text;
	}

	static String dim(final String text) {

		return color ? DIM + text + RESET : text;
	}

	static final int LONG_METHOD = 60, CX = 12, DEEP = 4;

	static final Map<String, Integer> W = new LinkedHashMap<>();
	static {

		// FORBIDDEN: make sure fixmes and bugs are always at the top of the list
		W.put("fixmes",           200);

		// UNWANTED: make sure each of these flags raises the score over the threshold
		W.put("static_block",     50);
		W.put("loop_invariant",   50);
		W.put("multi_statement",  50);

		// NICE TO HAVE: only multiple of these will cause the score to reach the threshold
		W.put("log_args",         10);
		W.put("sysout",           10);
		W.put("cstyle_for",       5);
		W.put("str_const_enum",   4);
		W.put("parser_smell",     4);
		W.put("regex_heavy",      4);
		W.put("equals_literal",   3);
		W.put("switch_heavy",     2);
		W.put("long_params",      1);
		W.put("long_methods",     1);
		W.put("boolean_params",   1);
		W.put("instanceof_heavy", 1);
		W.put("magic_numbers",    1);
		W.put("broad_catch",      1);

		// IGNORED: disabled because Structr is inherently complex and needs complex logic
		W.put("oversized",        0);
		W.put("concurrency",      0);
		W.put("reflection",       0);
		W.put("long_lines",       0);
		W.put("deep_nesting",     0);
		W.put("complexity",       0);
	}

	// short human-readable descriptions, printed as a legend under the ranked list
	static final Map<String, String> DESC = new LinkedHashMap<>();
	static {
		DESC.put("long_methods", "method body longer than " + LONG_METHOD + " lines");
		DESC.put("complexity", "high cyclomatic complexity — many branches, loops and conditions");
		DESC.put("deep_nesting", "blocks nested deeper than " + DEEP + " levels");
		DESC.put("cstyle_for", "C-style indexed for-loop nested inside another loop (a single flat one is fine)");
		DESC.put("loop_invariant", "loop condition re-evaluates size()/length() on every iteration instead of hoisting it");
		DESC.put("static_block", "static block in a class nothing references — it may never run (module/ServiceLoader case)");
		DESC.put("multi_statement", "more than one statement on the same line");
		DESC.put("switch_heavy", "many switch statements (possible missing polymorphism)");
		DESC.put("str_const_enum", "cluster of 3+ sibling String constants (probably wants to be an enum)");
		DESC.put("parser_smell", "hand-rolled string parsing (charAt/substring/indexOf/StringBuilder)");
		DESC.put("oversized", "God class — too many methods or too much code in one file");
		DESC.put("fixmes", "FIXME / HACK / 'known bug' marker — unresolved work or a known defect");
		DESC.put("magic_numbers", "unexplained numeric literals (other than 0, 1, 2)");
		DESC.put("long_params", "method with more than 5 parameters");
		DESC.put("boolean_params", "method with 2 or more boolean parameters (flag arguments)");
		DESC.put("equals_literal", "x.equals(\"literal\") — NPE-prone; prefer \"literal\".equals(x)");
		DESC.put("instanceof_heavy", "many instanceof checks (missing polymorphism)");
		DESC.put("broad_catch", "catches Exception/Throwable/RuntimeException, or an empty catch block");
		DESC.put("reflection", "reflection use (getClass/forName/getDeclared*/setAccessible)");
		DESC.put("concurrency", "concurrency primitives (synchronized/volatile/Thread/Atomic/locks)");
		DESC.put("sysout", "System.out/err or printStackTrace — should use logging");
		DESC.put("log_args", "log/exception message whose {} placeholders don't match its arguments — the detail is dropped");
		DESC.put("regex_heavy", "regex compiled per call (Pattern.compile / matches / replaceAll outside a static final field)");
		DESC.put("long_lines", "physical line longer than " + LONG_LINES_LIMIT + " columns");
	}

	static final Pattern DECISION      = Pattern.compile("\\b(if|for|while|case|catch)\\b|&&|\\|\\||\\?");
	static final Pattern CSTYLE_FOR    = Pattern.compile("\\bfor\\s*\\(\\s*(final\\s+)?(int|long|short|byte|Integer|Long)\\s+\\w+\\s*=");
	static final Pattern STR_CONST     = Pattern.compile("\\b(private|public|protected)?\\s*static\\s+final\\s+String\\s+\\w+\\s*=\\s*\"");
	static final Pattern PARSER        = Pattern.compile("\\.charAt\\(|\\.substring\\(|\\.indexOf\\(|\\.lastIndexOf\\(|\\.split\\(|\\.toCharArray\\(|StringBuilder|StringTokenizer|Character\\.is");
	static final Pattern METHOD_SIG    = Pattern.compile("^\\s*(?!(if|for|while|switch|catch|synchronized|return|new|else)\\b)[\\w@].*\\)\\s*(throws [\\w., ]+)?\\{$");
	static final Pattern TYPE_DECL     = Pattern.compile("\\b(class|interface|enum|record)\\b");
	static final Pattern TODO          = Pattern.compile("\\b(FIXME|HACK)\\b|(?i:\\bknown\\s+bugs?\\b)");
	static final Pattern SWITCH        = Pattern.compile("\\bswitch\\s*\\(");
	static final Pattern STRING_LIT    = Pattern.compile("\"(\\\\.|[^\"\\\\])*\"");
	static final Pattern CHAR_LIT      = Pattern.compile("'(\\\\.|[^'\\\\])*'");
	static final Pattern MAGIC         = Pattern.compile("(?<![\\w.])\\d+(\\.\\d+)?");
	static final Pattern EQ_LIT        = Pattern.compile("\\.equals(IgnoreCase)?\\(\"");
	static final Pattern INSTANCEOF    = Pattern.compile("\\binstanceof\\b");
	static final Pattern BROAD_CATCH   = Pattern.compile("catch\\s*\\(\\s*(final\\s+)?(Exception|Throwable|RuntimeException)\\b");
	static final Pattern EMPTY_CATCH   = Pattern.compile("catch\\s*\\([^)]*\\)\\s*\\{\\s*\\}");
	static final Pattern REFLECT       = Pattern.compile("\\.getClass\\(\\)|Class\\.forName|\\.getDeclared(Method|Field|Constructor)s?\\(|\\.getMethod\\(|\\.newInstance\\(|\\.setAccessible\\(");
	static final Pattern CONCURRENCY   = Pattern.compile("\\bsynchronized\\b|\\bvolatile\\b|\\bThread\\b|\\.wait\\(|\\.notify(All)?\\(|Atomic(Integer|Long|Boolean|Reference)|ConcurrentHashMap|CountDownLatch|ExecutorService|ReentrantLock");
	static final Pattern SYSOUT        = Pattern.compile("System\\.(out|err)\\.|\\.printStackTrace\\(");
	/**
	 * Regex work that happens per call: compiling a pattern, or the String methods that compile one
	 * internally. Calls on an existing Matcher are excluded - "m.matches()" and "X.matcher(s).replaceAll(r)"
	 * use a pattern that was compiled elsewhere, which is the fixed form, not the smell.
	 */
	static final Pattern MATCHER_DECL  = Pattern.compile("\\bMatcher\\s+(\\w+)");
	static final Pattern REGEX_HEAVY   = Pattern.compile("Pattern\\.compile|(?<![Mm]atcher)(?<!\\.matcher\\([^)]{0,60}\\))\\.(?:matches|replaceAll)\\(");
	static final Pattern BOOLEAN_PARAM = Pattern.compile("\\bboolean\\b");
	static final Pattern LOOP_OPEN     = Pattern.compile("\\b(?:for|while)\\s*\\(");
	static final Pattern FOR_OPEN      = Pattern.compile("\\bfor\\s*\\(");
	// a counter compared against a size that is recomputed on every iteration ("i < list.size()").
	// The call has to be the bound, on the right of the comparison: "while (buffer.length() > 0)" reads a
	// length that the body changes, so there is nothing to hoist there. Array .length is a field, not a call.
	static final Pattern INVARIANT_CALL = Pattern.compile("\\b\\w+\\s*(?:<|<=)\\s*[^;&|]*\\.\\s*(?:size|length|getLength|getSize|count|getCount)\\s*\\(\\s*\\)");
	static final Pattern STATIC_BLOCK  = Pattern.compile("(?<![\\w.])static\\s*\\{");
	static final Pattern TYPE_NAME     = Pattern.compile("\\b[A-Z]\\w*\\b");
	static final Pattern LOG_CALL      = Pattern.compile("\\b(?:logger|log|LOG|LOGGER)\\s*\\.\\s*(?:trace|debug|info|warn|error)\\s*\\(");
	static final Pattern EXCEPTION_CTOR = Pattern.compile("\\bnew\\s+\\w*(?:Exception|Error)\\s*\\(");
	// an unescaped {} -- SLF4J reads \{} as a literal brace pair, not as a placeholder
	static final Pattern PLACEHOLDER   = Pattern.compile("(?<!\\\\)\\{\\}");
	// a freshly constructed throwable passed as the last argument; named throwables are recognised by
	// their declaration instead (see throwableNames), because variable names like "uoe" or "nfe" are
	// not guessable and excusing a missing placeholder wrongly is exactly what would hide the defect
	static final Pattern THROWABLE_ARG  = Pattern.compile("^new\\s+\\w*(?:Exception|Error)\\b");
	static final Pattern CATCH_PARAM    = Pattern.compile("catch\\s*\\(\\s*(?:final\\s+)?[\\w.]+(?:\\s*\\|\\s*[\\w.]+)*\\s+(\\w+)\\s*\\)");
	static final Pattern THROWABLE_DECL = Pattern.compile("\\b\\w*(?:Exception|Error|Throwable)\\s+(\\w+)\\s*[=;,)]");
	static final Pattern COMMA         = Pattern.compile(",");
	static final Pattern OPEN_BRACE    = Pattern.compile("\\{");
	static final Pattern CLOSE_BRACE   = Pattern.compile("\\}");
	static final Pattern OPEN_PAREN    = Pattern.compile("\\(");
	static final Pattern CLOSE_PAREN   = Pattern.compile("\\)");

	// signals locatable by a single per-line pattern (used by --detail); the rest need special logic
	static final Map<String, Pattern> LINE_SIG = new LinkedHashMap<>();
	static {
		LINE_SIG.put("switch_heavy", SWITCH);
		LINE_SIG.put("str_const_enum", STR_CONST);
		LINE_SIG.put("parser_smell", PARSER);
		LINE_SIG.put("equals_literal", EQ_LIT);
		LINE_SIG.put("instanceof_heavy", INSTANCEOF);
		LINE_SIG.put("reflection", REFLECT);
		LINE_SIG.put("concurrency", CONCURRENCY);
		LINE_SIG.put("sysout", SYSOUT);
		LINE_SIG.put("regex_heavy", REGEX_HEAVY);
	}

	record Result(int loc, int score, Map<String, Integer> sub, boolean accepted) {}
	record Row(int score, int loc, Path path, Map<String, Integer> sub, boolean accepted) {}

	/**
	 * Marks the lines that belong to a {@code static final} field, including the continuation lines of a
	 * multi-line initializer. A Pattern compiled there is compiled once when the class is loaded, i.e. it
	 * is the fix for regex_heavy rather than an instance of it, so those lines must not be counted.
	 */
	/** The names of the Matcher variables declared in a file ("final Matcher m = ..." yields "m"). */
	static Set<String> matcherNames(final String code) {

		final Matcher declarations = MATCHER_DECL.matcher(code);
		final Set<String> names    = new LinkedHashSet<>();

		while (declarations.find()) {

			names.add(declarations.group(1));
		}

		return names;
	}

	/**
	 * Blanks out the regex calls that run on an existing Matcher, so they are not counted as per-call
	 * regex work: the pattern behind them was compiled somewhere else. The names come from the whole
	 * file, because the declaration is rarely on the same line as the call.
	 */
	static String maskMatcherCalls(final String text, final Set<String> names) {

		String result = text;

		for (final String name : names) {

			result = result.replace(name + ".matches(",    name + ".onMatcher(");
			result = result.replace(name + ".replaceAll(", name + ".onMatcher(");
		}

		return result;
	}

	static boolean[] staticFinalLines(final List<String> code) {

		final boolean[] result = new boolean[code.size()];
		boolean inField        = false;
		int depth              = 0;

		for (int i = 0; i < code.size(); i++) {

			final String line = code.get(i);

			if (!inField && line.contains("static final")) {

				inField = true;
				depth   = 0;
			}

			if (inField) {

				result[i] = true;
				depth    += count(OPEN_PAREN, line) - count(CLOSE_PAREN, line);

				// the declaration ends with the semicolon of its outermost statement
				if (depth <= 0 && line.contains(";")) {

					inField = false;
					depth   = 0;
				}
			}
		}

		return result;
	}

	/**
	 * The content between the parenthesis at the given index and its match, or null if unbalanced.
	 * String and char literals are skipped, so a parenthesis inside one does not end the header.
	 */
	static String parenContent(final String text, final int open) {

		final StringBuilder cur = new StringBuilder();
		boolean inString = false, inChar = false, escaped = false;
		int depth = 0;

		for (int i = open; i < text.length(); i++) {

			final char c = text.charAt(i);

			if (inString || inChar) {

				cur.append(c);

				if (escaped) {

					escaped = false;

				} else if (c == '\\') {

					escaped = true;

				} else if (inString && c == '"') {

					inString = false;

				} else if (inChar && c == '\'') {

					inChar = false;
				}

				continue;
			}

			if (c == '"') {

				inString = true;

			} else if (c == '\'') {

				inChar = true;

			} else if (c == '(') {

				depth++;

				if (depth == 1) {

					continue;
				}

			} else if (c == ')') {

				depth--;

				if (depth == 0) {

					return cur.toString();
				}
			}

			cur.append(c);
		}

		return null;
	}

	/**
	 * Loop headers that recompute their bound on every iteration, i.e. "i < list.size()" rather than
	 * a size read once before the loop. Only the condition is examined - the same call in the body or
	 * in the initialiser is not the pattern being described.
	 *
	 * Iterator loops ("it.hasNext()") are not this smell, and array ".length" is a field read, so only
	 * the calls that return a count are counted: size, length, getLength, getSize, count, getCount.
	 *
	 * @return the 0-based line indices of the offending loop headers
	 */
	static List<Integer> loopInvariantLines(final List<String> code) {

		final List<Integer> result = new ArrayList<>();

		for (int li = 0; li < code.size(); li++) {

			final String line = code.get(li);
			final Matcher m   = LOOP_OPEN.matcher(line);

			while (m.find()) {

				final String header = parenContent(line, m.end() - 1);
				if (header == null) {

					continue;
				}

				// a for header is "init; condition; update"; a while header is all condition
				final boolean isFor      = line.charAt(m.start()) == 'f';
				final String[] sections  = header.split(";", -1);
				final String condition   = isFor ? (sections.length > 1 ? sections[1] : "") : header;

				if (find(INVARIANT_CALL, condition)) {

					result.add(li);
					break;
				}
			}
		}

		return result;
	}

	/**
	 * C-style indexed for-loops that sit inside another loop. A single flat one is a perfectly good
	 * way to walk an index; it is the nested ones that multiply work and are worth a second look, so
	 * only a loop with an enclosing for or while is counted.
	 *
	 * @return the 0-based line indices of the nested loops
	 */
	static List<Integer> nestedCStyleForLines(final List<String> code) {

		final List<Integer> result   = new ArrayList<>();
		final Deque<Integer> enclosing = new ArrayDeque<>();
		int depth = 0;

		for (int li = 0; li < code.size(); li++) {

			final String line = code.get(li);

			// a loop whose body has closed is no longer enclosing anything
			while (!enclosing.isEmpty() && depth <= enclosing.peek()) {

				enclosing.pop();
			}

			if (find(CSTYLE_FOR, line) && !enclosing.isEmpty()) {

				result.add(li);
			}

			if (find(LOOP_OPEN, line)) {

				enclosing.push(depth);
			}

			depth += count(OPEN_BRACE, line) - count(CLOSE_BRACE, line);
		}

		return result;
	}

	/**
	 * Static initializer blocks in a class that no other class references in its code.
	 *
	 * A static block runs when the class is initialised, and the class is initialised when something
	 * first touches it. If nothing does - the class is reached only through a module-info provides
	 * clause, a ServiceLoader, a configuration entry or a name passed to reflection - then the block
	 * runs late, or not at all. Whatever it installs (a URL handler, a registration, a global default)
	 * is then simply missing, with no error anywhere: the code is there, it just never executed.
	 *
	 * References are counted from real code only. An import does not initialise a class, a mention in
	 * a comment is not a reference, and module-info.java is excluded on purpose - being named there
	 * as a service provider is exactly the dynamic-loading case this looks for.
	 *
	 * Needs the whole tree to judge, so it stays silent when only a handful of files were scanned.
	 *
	 * @return the 0-based line indices of the static blocks in such a class
	 */
	static List<Integer> staticBlockLines(final List<String> code, final Path path) {

		final List<Integer> result = new ArrayList<>();

		if (referencedElsewhere(simpleName(path))) {

			return result;
		}

		final String text    = String.join("\n", code);
		final Matcher blocks = STATIC_BLOCK.matcher(text);

		while (blocks.find()) {

			result.add(lineOf(text, blocks.start()));
		}

		return result;
	}

	/** The type a file declares, i.e. its name without the extension. */
	static String simpleName(final Path path) {

		return path.getFileName().toString().replace(".java", "");
	}

	/** Type names referenced from the code of some other class; built once per run by indexReferences(). */
	static final Map<String, Set<String>> references = new HashMap<>();
	static int indexedFiles = 0;

	/** Below this many files the index cannot tell "unreferenced" from "not scanned", so nothing is claimed. */
	static final int MIN_INDEX_SIZE = 100;

	static boolean referencedElsewhere(final String type) {

		if (indexedFiles < MIN_INDEX_SIZE) {

			return true;
		}

		final Set<String> from = references.get(type);

		return from != null && from.stream().anyMatch(f -> !f.equals(type));
	}

	/**
	 * Records, for every type name, which classes mention it in their code. Import and package lines
	 * are skipped (an import initialises nothing) and so is module-info.java, along with comments and
	 * string literals.
	 */
	static void indexReferences(final List<Path> files) {

		for (final Path path : files) {

			final String own = simpleName(path);

			if ("module-info".equals(own)) {

				continue;
			}

			try {

				final List<String> raw = Arrays.asList(new String(Files.readAllBytes(path), StandardCharsets.UTF_8).split("\n", -1));

				for (final String line : toCode(raw)) {

					final String trimmed = line.trim();

					if (trimmed.startsWith("import ") || trimmed.startsWith("package ")) {

						continue;
					}

					final Matcher names = TYPE_NAME.matcher(line);

					while (names.find()) {

						references.computeIfAbsent(names.group(), k -> new LinkedHashSet<>()).add(own);
					}
				}

			} catch (Exception ignore) {

				/* skip unreadable file */
			}
		}

		indexedFiles = files.size();
	}

	/**
	 * Lines carrying more than one statement. A semicolon at paren depth 0 followed by more code on
	 * the same line is a second statement; a trailing comment, and the closing braces of a block that
	 * ends on the same line, are not.
	 *
	 * @return the 0-based line indices of the offending lines
	 */
	static List<Integer> multiStatementLines(final List<String> code) {

		final List<Integer> result = new ArrayList<>();

		for (int li = 0; li < code.size(); li++) {

			final String line = code.get(li);
			int depth = 0;

			for (int i = 0; i < line.length(); i++) {

				final char c = line.charAt(i);

				if (c == '(') {

					depth++;

				} else if (c == ')') {

					depth--;

				} else if (c == ';' && depth == 0) {

					final String rest = line.substring(i + 1).trim();

					// only closing braces (and the ; of a do-while or a lambda assignment) may follow
					if (!rest.isEmpty() && !rest.startsWith("}") && !rest.startsWith("//")) {

						result.add(li);
						break;
					}
				}
			}
		}

		return result;
	}

	/**
	 * Splits the argument list that starts at the given '(' into its top-level arguments, ignoring
	 * commas inside nested calls, arrays, lambdas and string literals. Returns null if the list is
	 * not closed (a truncated file, or a construct this scanner does not understand), in which case
	 * the caller must not draw a conclusion.
	 */
	static List<String> topLevelArgs(final String text, final int open) {

		final List<String> args = new ArrayList<>();
		final StringBuilder cur = new StringBuilder();
		boolean inString = false, inChar = false, escaped = false;
		int depth = 0;

		for (int i = open; i < text.length(); i++) {

			final char c = text.charAt(i);

			if (inString || inChar) {

				cur.append(c);

				if (escaped) {

					escaped = false;

				} else if (c == '\\') {

					escaped = true;

				} else if (inString && c == '"') {

					inString = false;

				} else if (inChar && c == '\'') {

					inChar = false;
				}

				continue;
			}

			switch (c) {

				case '"'            -> { inString = true; cur.append(c); }
				case '\''           -> { inChar   = true; cur.append(c); }
				case '(', '[', '{'  -> { depth++; if (depth > 1) { cur.append(c); } }
				case ')', ']', '}'  -> {

					depth--;

					if (depth == 0) {

						if (!cur.toString().isBlank() || !args.isEmpty()) {

							args.add(cur.toString().trim());
						}

						return args;
					}

					cur.append(c);
				}
				case ','            -> {

					if (depth == 1) {

						args.add(cur.toString().trim());
						cur.setLength(0);

					} else {

						cur.append(c);
					}
				}
				default             -> { if (depth >= 1) { cur.append(c); } }
			}
		}

		return null;
	}

	/**
	 * The names of the throwables in a file, from their declarations: the parameter of every catch
	 * clause plus every variable whose type ends in Exception, Error or Throwable. A throwable passed
	 * as the last argument of a log call needs no placeholder - SLF4J prints its stack trace - so
	 * telling those apart from an ordinary value is what keeps this signal free of false alarms.
	 */
	static Set<String> throwableNames(final String code) {

		final Set<String> names = new LinkedHashSet<>();

		for (final Pattern p : List.of(CATCH_PARAM, THROWABLE_DECL)) {

			final Matcher m = p.matcher(code);

			while (m.find()) {

				names.add(m.group(1));
			}
		}

		return names;
	}

	/** Whether an argument expression is nothing but string literals (possibly concatenated). */
	static boolean isLiteralOnly(final String expr) {

		if (!expr.contains("\"")) {

			return false;
		}

		return STRING_LIT.matcher(expr).replaceAll("").replace("+", "").isBlank();
	}

	/** The number of unescaped {} placeholders in the string literals of an expression. */
	static int placeholders(final String expr) {

		final Matcher literals = STRING_LIT.matcher(expr);
		int found = 0;

		while (literals.find()) {

			found += count(PLACEHOLDER, literals.group());
		}

		return found;
	}

	/**
	 * The line index (0-based) an offset in the joined text falls on.
	 */
	static int lineOf(final String text, final int offset) {

		int line = 0;

		for (int i = 0; i < offset && i < text.length(); i++) {

			if (text.charAt(i) == '\n') {

				line++;
			}
		}

		return line;
	}

	/**
	 * Log calls and exception messages whose {@code {}} placeholders do not line up with the values
	 * meant to fill them - the message then silently drops the one detail it was written to carry
	 * ({@code logger.warn("node {} not found")} prints a literal "{}", {@code logger.warn("node not
	 * found", uuid)} prints no uuid at all). Both forms compile, and neither can fail a test, which
	 * is why they survive: the message is only ever read after something has already gone wrong.
	 *
	 * Counted:
	 *   - an SLF4J call whose placeholder count differs from its argument count. A trailing throwable
	 *     is allowed to have no placeholder, since SLF4J prints its stack trace instead.
	 *   - a {} inside an exception message, where nothing ever substitutes it: exception constructors
	 *     take a plain String, not a format.
	 *
	 * Only calls whose message is a literal (or a concatenation of literals) are judged; when the
	 * message comes from a variable or a method call, the placeholders cannot be counted and the
	 * call is skipped rather than guessed at.
	 *
	 * @return the 0-based line indices of the offending calls
	 */
	static List<Integer> logArgLines(final List<String> stripped) {

		final String text            = String.join("\n", stripped);
		final List<Integer> result   = new ArrayList<>();
		final Set<String> throwables = throwableNames(text);
		final Matcher calls          = LOG_CALL.matcher(text);

		while (calls.find()) {

			final List<String> args = topLevelArgs(text, calls.end() - 1);

			if (args == null || args.isEmpty() || !isLiteralOnly(args.get(0))) {

				continue;
			}

			final int expected = placeholders(args.get(0));
			final int supplied = args.size() - 1;
			final String last               = supplied > 0 ? args.get(supplied).replaceAll("\\s+", " ") : "";
			final boolean trailingThrowable = supplied > 0 && (throwables.contains(last) || find(THROWABLE_ARG, last));

			// a trailing throwable may, but need not, have a placeholder of its own
			if (expected == supplied || (trailingThrowable && expected == supplied - 1)) {

				continue;
			}

			result.add(lineOf(text, calls.start()));
		}

		final Matcher constructors = EXCEPTION_CTOR.matcher(text);

		while (constructors.find()) {

			final List<String> args = topLevelArgs(text, constructors.end() - 1);

			if (args == null) {

				continue;
			}

			for (final String arg : args) {

				// "{}" on its own is an empty JSON object, not a placeholder that lost its argument
				if (isLiteralOnly(arg) && !arg.trim().equals("\"{}\"") && placeholders(arg) > 0) {

					result.add(lineOf(text, constructors.start()));
					break;
				}
			}
		}

		Collections.sort(result);

		return result;
	}

	static int count(final Pattern p, final String s) {

		final Matcher m = p.matcher(s);
		int n = 0;

		while (m.find()) {

			n++;
		}

		return n;
	}

	static boolean find(final Pattern p, final String s) {

		return p.matcher(s).find();
	}

	/**
	 * Strips comments but keeps the string literals, unlike {@link #toCode}, which blanks them: the
	 * placeholder detector has to read the message text. Walks the source character by character
	 * (tracking literal, char-literal and comment state) instead of using line patterns, so a "/*"
	 * or a "//" inside a string literal does not start a comment. Comments become spaces, so line
	 * numbering and column offsets stay aligned with the raw source.
	 */
	static List<String> stripComments(final List<String> raw) {

		final String text     = String.join("\n", raw);
		final StringBuilder b = new StringBuilder(text.length());
		boolean inString = false, inChar = false, inLine = false, inBlock = false, escaped = false;

		for (int i = 0; i < text.length(); i++) {

			final char c    = text.charAt(i);
			final char next = i + 1 < text.length() ? text.charAt(i + 1) : '\0';

			if (c == '\n') {

				// a line comment ends here; a block comment and a string do not
				inLine  = false;
				escaped = false;

				b.append(c);
				continue;
			}

			if (inLine || inBlock) {

				if (inBlock && c == '*' && next == '/') {

					inBlock = false;
					b.append("  ");
					i++;

				} else {

					b.append(' ');
				}

				continue;
			}

			if (inString || inChar) {

				b.append(c);

				if (escaped) {

					escaped = false;

				} else if (c == '\\') {

					escaped = true;

				} else if (inString && c == '"') {

					inString = false;

				} else if (inChar && c == '\'') {

					inChar = false;
				}

				continue;
			}

			if (c == '/' && next == '/') {

				inLine = true;
				b.append("  ");
				i++;

			} else if (c == '/' && next == '*') {

				inBlock = true;
				b.append("  ");
				i++;

			} else {

				if (c == '"') {

					inString = true;

				} else if (c == '\'') {

					inChar = true;
				}

				b.append(c);
			}
		}

		return Arrays.asList(b.toString().split("\n", -1));
	}

	/**
	 * Blanks the content of text blocks, which the per-line literal patterns cannot see: a table or a
	 * snippet inside a """ ... """ is prose, and counting its semicolons, numbers or braces as code
	 * produces findings that point at documentation. Java requires a line terminator right after the
	 * opening delimiter, so a line either opens a block or closes one, never both.
	 */
	static List<String> blankTextBlocks(final List<String> lines) {

		final List<String> out = new ArrayList<>();
		boolean inBlock        = false;

		for (String line : lines) {

			final int marker = line.indexOf("\"\"\"");

			if (inBlock) {

				if (marker < 0) {

					// a content line of the block
					out.add("");
					continue;
				}

				// the closing delimiter may be followed by code, e.g. """.formatted(x)
				out.add("\"\"" + line.substring(marker + 3));
				inBlock = false;
				continue;
			}

			if (marker >= 0) {

				out.add(line.substring(0, marker) + "\"\"");
				inBlock = true;
				continue;
			}

			out.add(line);
		}

		return out;
	}

	/**
	 * Strip // and block comments and blank string/char literals so tokens aren't miscounted.
	 *
	 * Comment removal is delegated to {@link #stripComments}, which knows what is inside a string
	 * literal. Doing it with line patterns instead used to lose whole files: one "/*" inside a
	 * literal with no "*&#47;" after it - a description of a URL pattern, say - put the scanner in
	 * block-comment mode for the rest of the file, and everything below that line became invisible
	 * to every signal.
	 */
	static List<String> toCode(final List<String> raw) {

		final List<String> out = new ArrayList<>();

		for (String line : blankTextBlocks(stripComments(raw))) {

			line = STRING_LIT.matcher(line).replaceAll("\"\"");
			line = CHAR_LIT.matcher(line).replaceAll("''");

			out.add(line);
		}

		return out;
	}

	/**
	 * Whether a file is a test source, i.e. lives under a {@code src/test} directory. Test classes are
	 * skipped entirely - not just their {@code @Test} methods - because the scaffolding around those
	 * methods (setup, fixtures, helpers) is written to different standards than production code and
	 * would otherwise dominate the ranking.
	 */
	static boolean isTestSource(final Path path) {

		return path.toString().replace('\\', '/').contains("/src/test/");
	}

	static Result analyze(final Path path) throws Exception {

		final String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
		final boolean accepted = content.contains(ACCEPT_MARKER);
		final List<String> raw = Arrays.asList(content.split("\n", -1));
		final List<String> code = toCode(raw);
		final String joined = String.join("\n", code);
		int loc = 0;

		for (String c : code) {

			if (!c.trim().isEmpty()) {

				loc++;
			}
		}

		int longM = 0, cx = 0, deep = 0, longParams = 0, boolParams = 0, numMethods = 0;
		int i = 0;

		while (i < code.size()) {

			final String line = code.get(i);
			final boolean isMethod = METHOD_SIG.matcher(line).find() && !TYPE_DECL.matcher(line).find() && !line.contains("->") && !line.contains(" new ") && !line.contains("=");

			if (!isMethod) {

				i++; continue;
			}

			numMethods++;
			final int lp = line.indexOf('('), rp = line.lastIndexOf(')');
			if (lp >= 0 && rp > lp) {

				final String params = line.substring(lp + 1, rp).trim();
				if (!params.isEmpty()) {

					if (count(COMMA, params) + 1 > 5) {

						longParams++;
					}

					if (count(BOOLEAN_PARAM, params) >= 2) {

						boolParams++;
					}
				}
			}

			int depth = count(OPEN_BRACE, line) - count(CLOSE_BRACE, line);
			int j = i + 1, body = 0, cyc = 1, maxRel = 0;

			while (j < code.size() && depth > 0) {

				final String cj = code.get(j);
				if (!cj.trim().isEmpty()) {

					body++;
				}

				cyc += count(DECISION, cj);
				maxRel = Math.max(maxRel, depth);
				depth += count(OPEN_BRACE, cj) - count(CLOSE_BRACE, cj);
				j++;
			}

			if (body >= LONG_METHOD) {

				longM++;
			}

			if (cyc > CX) {

				cx++;
			}

			if (maxRel >= DEEP + 1) {

				deep++;
			}

			i = j;
		}

		// clusters of >= 3 sibling String constants (blanks don't break the run)
		int clusters = 0, run = 0;

		for (String c : code) {

			if (STR_CONST.matcher(c).find()) {

				run++;

			} else if (c.trim().isEmpty()) {

				/* keep the run going */

			} else {

				if (run >= 3) {

					clusters++;
				}

				run = 0;
			}
		}

		if (run >= 3) {

			clusters++;
		}

		// magic numbers: literals other than 0/1/2, excluding constant declarations
		int magic = 0;

		for (String c : code) {

			if (c.contains("static final") || TYPE_DECL.matcher(c).find()) {

				continue;
			}

			final Matcher m = MAGIC.matcher(c);

			while (m.find()) {

				final String v = m.group();
				if (!v.equals("0") && !v.equals("1") && !v.equals("2")) {

					magic++;
				}
			}
		}

		final int parser = count(PARSER, joined);
		final int size   = raw.size();
		int longLines = 0;

		for (int li = 0; li < size; li++) {

			// code.get(li) is blank for comment and blank lines (aligned 1:1 with raw), so long lines
			// inside comments do not count — same view as every other signal.
			if (code.get(li).isBlank()) {

				continue;
			}

			final String c = raw.get(li);
			final String t = c.strip();

			if (t.startsWith("import ") || t.startsWith("package ")) {

				continue;
			}

			final int w = width(c);

			if (w > LONG_LINES_LIMIT) {

				longLines++;
			}
		}

		final Map<String, Integer> sub = new LinkedHashMap<>();

		sub.put("long_methods", longM);
		sub.put("complexity", cx);
		sub.put("deep_nesting", deep);
		sub.put("cstyle_for", nestedCStyleForLines(code).size());
		sub.put("loop_invariant", loopInvariantLines(code).size());
		sub.put("static_block", staticBlockLines(code, path).size());
		sub.put("multi_statement", multiStatementLines(code).size());
		sub.put("switch_heavy", count(SWITCH, joined));
		sub.put("str_const_enum", clusters);
		sub.put("parser_smell", Math.round(parser / Math.max(1f, loc / 100f)));
		sub.put("oversized", Math.min(20, Math.max(0, numMethods - 30) / 4 + Math.max(0, (loc - 800) / 300)));
		sub.put("fixmes", count(TODO, content));
		sub.put("magic_numbers", magic / 5);
		sub.put("long_params", longParams);
		sub.put("boolean_params", boolParams);
		sub.put("equals_literal", count(EQ_LIT, joined) / 2);
		sub.put("instanceof_heavy", Math.max(0, count(INSTANCEOF, joined) - 3));
		sub.put("broad_catch", count(BROAD_CATCH, joined) + count(EMPTY_CATCH, joined));
		sub.put("reflection", count(REFLECT, joined) / 2);
		sub.put("concurrency", count(CONCURRENCY, joined));
		sub.put("sysout", count(SYSOUT, joined));
		// needs the message text, so it runs on the source with its string literals still in place
		sub.put("log_args", logArgLines(stripComments(raw)).size());
		// regex compiled in a static final field is compiled once, so only the rest counts here
		final boolean[] staticFinal   = staticFinalLines(code);
		final StringBuilder perCall   = new StringBuilder();

		for (int li = 0; li < code.size(); li++) {

			if (!staticFinal[li]) {

				perCall.append(code.get(li)).append("\n");
			}
		}

		sub.put("regex_heavy", count(REGEX_HEAVY, maskMatcherCalls(perCall.toString(), matcherNames(joined))) / 2);
		sub.put("long_lines", longLines);

		int score = 0;

		for (Map.Entry<String, Integer> e : sub.entrySet()) {

			score += e.getValue() * W.get(e.getKey());
		}

		return new Result(loc, score, sub, accepted);
	}

	/**
	 * All scoring flags, most-contributing first, as {@code name=count} (the score is their weighted
	 * sum). A signal whose weight is 0 is switched off deliberately, so it is left out: it moves no
	 * file in the ranking, and listing it among "most-contributing first" would only lengthen every
	 * row with counts nobody acts on. Give it a weight again and it reappears.
	 */
	static String signals(final Map<String, Integer> sub) {

		final List<Map.Entry<String, Integer>> contrib = new ArrayList<>(sub.entrySet());

		contrib.removeIf(e -> e.getValue() == 0 || !reported(e.getKey()));
		contrib.sort((p, q) -> {

			// the signal the table is ranked by leads, whatever it contributes to the score
			if (p.getKey().equals(rankedBy)) {

				return -1;
			}

			if (q.getKey().equals(rankedBy)) {

				return 1;
			}

			return q.getValue() * W.get(q.getKey()) - p.getValue() * W.get(p.getKey());
		});

		final StringBuilder sig = new StringBuilder();

		for (final Map.Entry<String, Integer> e : contrib) {

			sig.append(sig.length() > 0 ? ", " : "").append(e.getKey()).append("=").append(e.getValue());
		}

		return sig.length() == 0 ? "-" : sig.toString();
	}

	/** "token - description" lines for the signals that actually appear in the given rows (W order). */
	static List<String> legend(final List<Row> rows) {

		final Set<String> seen = new LinkedHashSet<>();

		for (final Row r : rows) {

			for (final Map.Entry<String, Integer> e : r.sub().entrySet()) {

				// only explain what the rows actually show
				if (e.getValue() > 0 && reported(e.getKey())) {

					seen.add(e.getKey());
				}
			}
		}

		final List<String> keys = new ArrayList<>();
		int w = 0;

		for (final String k : W.keySet()) {

			if (seen.contains(k)) {

				keys.add(k);
				w = Math.max(w, k.length());
			}
		}

		Collections.sort(keys);

		final List<String> out = new ArrayList<>();

		for (final String k : keys) {

			out.add(String.format("%-" + w + "s - %s", k, DESC.get(k)));
		}

		return out;
	}

	static String rel(final Path base, final Path p) {

		try {

			return base.relativize(p.toAbsolutePath()).toString();

		} catch (Exception e) {

			return p.toString();
		}
	}

	/** A "Lnnn  <trimmed source>" location for line index i. */
	static String loc(final int i, final List<String> raw) {

		return String.format("L%-5d %s", i + 1, raw.get(i).strip());
	}

	static boolean isMethodSig(final String c) {

		return METHOD_SIG.matcher(c).find() && !TYPE_DECL.matcher(c).find() && !c.contains("->") && !c.contains(" new ") && !c.contains("=");
	}

	/** True if the (comment/string-stripped) line carries a magic number literal (not 0/1/2). */
	static boolean magic(final String c) {

		final Matcher m = MAGIC.matcher(c);

		while (m.find()) {

			final String v = m.group();
			if (!v.equals("0") && !v.equals("1") && !v.equals("2")) {

				return true;
			}
		}

		return false;
	}

	static int width(final String s) {
		return s.length();
	}

	/**
	 * Manual-analysis mode: print every locatable finding in one file, grouped by signal, each as
	 * "Lnnn  <source line>", so a reviewer can walk through and amend them. Counts here are per-line
	 * matches, so they may differ from the score's normalised flags.
	 */
	static void detail(final Path path) throws Exception {

		final String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
		final List<String> raw = Arrays.asList(content.split("\n", -1));
		final List<String> code = toCode(raw);
		final Result res = analyze(path);
		final Map<String, List<String>> hits = new LinkedHashMap<>();

		final boolean[] staticFinal = staticFinalLines(code);
		final Set<String> matchers  = matcherNames(String.join("\n", code));

		for (final String k : W.keySet()) {

			hits.put(k, new ArrayList<>());
		}

		for (final int li : logArgLines(stripComments(raw))) {

			hits.get("log_args").add(loc(li, raw));
		}

		// the signals that need the surrounding code, not just the line
		for (final int li : nestedCStyleForLines(code)) {

			hits.get("cstyle_for").add(loc(li, raw));
		}

		for (final int li : loopInvariantLines(code)) {

			hits.get("loop_invariant").add(loc(li, raw));
		}

		for (final int li : multiStatementLines(code)) {

			hits.get("multi_statement").add(loc(li, raw));
		}

		for (final int li : staticBlockLines(code, path)) {

			hits.get("static_block").add(loc(li, raw));
		}

		for (int li = 0; li < code.size(); li++) {

			final String c = code.get(li);

			if (find(TODO, raw.get(li))) {

				hits.get("fixmes").add(loc(li, raw));
			}

			if (c.isBlank()) {

				continue;
			}

			for (final Map.Entry<String, Pattern> e : LINE_SIG.entrySet()) {

				// same rule as the score: a regex in a static final field is compiled once, not per call
				if ("regex_heavy".equals(e.getKey()) && staticFinal[li]) {

					continue;
				}

				final String line = "regex_heavy".equals(e.getKey()) ? maskMatcherCalls(c, matchers) : c;

				if (find(e.getValue(), line)) {

					hits.get(e.getKey()).add(loc(li, raw));
				}
			}

			if (find(BROAD_CATCH, c) || find(EMPTY_CATCH, c)) {

				hits.get("broad_catch").add(loc(li, raw));
			}

			if (!c.contains("static final") && !find(TYPE_DECL, c) && magic(c)) {

				hits.get("magic_numbers").add(loc(li, raw));
			}

			if (isMethodSig(c)) {

				final int lp = c.indexOf('('), rp = c.lastIndexOf(')');
				if (lp >= 0 && rp > lp) {

					final String params = c.substring(lp + 1, rp).trim();
					if (!params.isEmpty() && count(COMMA, params) + 1 > 5) {

						hits.get("long_params").add(loc(li, raw));
					}

					if (count(BOOLEAN_PARAM, params) >= 2) {

						hits.get("boolean_params").add(loc(li, raw));
					}
				}
			}

			if (width(raw.get(li)) > LONG_LINES_LIMIT) {

				final String t = raw.get(li).strip();
				if (!t.startsWith("import ") && !t.startsWith("package ")) {

					hits.get("long_lines").add(loc(li, raw));
				}
			}
		}

		int mi = 0;

		while (mi < code.size()) {

			if (!isMethodSig(code.get(mi))) {

				mi++;
				continue;
			}

			int depth = count(OPEN_BRACE, code.get(mi)) - count(CLOSE_BRACE, code.get(mi));
			int j = mi + 1, body = 0, cyc = 1, maxRel = 0;

			while (j < code.size() && depth > 0) {

				final String cj = code.get(j);
				if (!cj.trim().isEmpty()) {

					body++;
				}

				cyc += count(DECISION, cj);
				maxRel = Math.max(maxRel, depth);
				depth += count(OPEN_BRACE, cj) - count(CLOSE_BRACE, cj);
				j++;
			}

			if (body >= LONG_METHOD) {

				hits.get("long_methods").add(loc(mi, raw) + "  (" + body + " lines)");
			}

			if (cyc > CX) {

				hits.get("complexity").add(loc(mi, raw) + "  (cyclomatic " + cyc + ")");
			}

			if (maxRel >= DEEP + 1) {

				hits.get("deep_nesting").add(loc(mi, raw) + "  (depth " + maxRel + ")");
			}

			mi = j;
		}

		if (res.sub().get("oversized") > 0) {

			hits.get("oversized").add("whole file — " + res.loc() + " code lines");
		}

		System.out.println(path + "  —  score " + res.score() + (res.accepted() ? "  [accepted]" : ""));

		for (final String k : W.keySet()) {

			final List<String> ls = hits.get(k);
			if (ls.isEmpty()) {

				continue;
			}

			System.out.println();
			System.out.println("  " + k + " (" + ls.size() + ") — " + DESC.get(k));

			for (final String s : ls) {

				System.out.println("    " + s);
			}
		}

		System.out.println();
	}

	static void usage(final String err) {

		if (err != null) {

			System.err.println("error: " + err);
		}

		System.out.println("usage: java config/style/CodeQuality.java [options] [paths...]");
		System.out.println("  --top N          keep the top N rows in the CLI table (default 25)");
		System.out.println("  --min SCORE      omit files scoring below SCORE (default " + DEFAULT_THRESHOLD + "; a file with only switched-off signals scores 0)");
		System.out.println("  --by SIGNAL      rank by one signal's count instead of the composite score");
		System.out.println("  --detail N       list every finding in the file at ranking position N");
		System.out.println("  --main-only      scan only src/main (test sources are always skipped)");
		System.out.println("  --summary        one line per file, [" + LABEL + "]-prefixed (used by the build)");
		System.out.println("  --show-accepted  include @code-quality:accept files in the listing");
		System.out.println("  --color MODE     always | never | auto (default auto: only on a terminal)");
		System.out.println("  --help, -h       show this help");
		System.out.println("  paths...         files or directories to scan (default: current directory)");
		System.out.println("  signals for --by: " + W.keySet());
		System.exit(err == null ? 0 : 2);
	}

	/** The value following the flag at index i, or a usage error if it is missing. */
	static String argValue(final String[] a, final int i, final String flag) {

		if (i + 1 >= a.length) {

			usage(flag + " requires a value");
		}

		return a[i + 1];
	}

	/** The integer following the flag at index i, or a usage error if it is missing or not a number. */
	static int argInt(final String[] a, final int i, final String flag) {

		final String v = argValue(a, i, flag);

		try {

			return Integer.parseInt(v);

		} catch (NumberFormatException e) {

			usage(flag + " expects an integer, got '" + v + "'");

			return 0;
		}
	}

	public static void main(final String[] a) {

		int top = 25, min = DEFAULT_THRESHOLD, detailPos = 0;
		String by = null;
		boolean summary = false, mainOnly = false, showAccepted = false;
		final List<String> paths = new ArrayList<>();

		for (int i = 0; i < a.length; i++) {

			final String arg = a[i];

			switch (arg) {
				case "--top"           -> { top = argInt(a, i, arg); i++; }
				case "--min"           -> { min = argInt(a, i, arg); i++; }
				case "--by"            -> { by = argValue(a, i, arg); rankedBy = by; i++; }
				case "--summary"       -> summary = true;
				case "--main-only"     -> mainOnly = true;
				case "--show-accepted" -> showAccepted = true;
				case "--color"         -> {

					final String mode = argValue(a, i, arg);
					i++;

					switch (mode) {

						case "always" -> color = true;
						case "never"  -> color = false;
						case "auto"   -> color = detectColor();
						default       -> usage("--color expects always, never or auto, got: " + mode);
					}
				}
				case "--help", "-h"    -> usage(null);
				case "--detail"        -> {

					detailPos = argInt(a, i, arg);
					i++;

					if (detailPos < 1) {

						usage("--detail needs a position >= 1 (the # from the ranked table)");
					}
				}
				default                -> {

					if (arg.startsWith("-")) {

						usage("unknown option: " + arg);
					}

					paths.add(arg);
				}
			}
		}

		final boolean mo = mainOnly;

		if (paths.isEmpty()) {

			paths.add(".");
		}

		if (by != null && !W.containsKey(by)) {

			usage("--by must be one of " + W.keySet());
		}

		if (top < 1) {

			usage("--top must be >= 1");
		}

		if (min < 0) {

			usage("--min must be >= 0");
		}

		// Never fail the caller (the build must not break on findings, or on a hostile file): any
		// runtime problem is swallowed with a one-line note and a zero exit.
		try {

			final List<Path> fs = new ArrayList<>();

			for (String p : paths) {

				final Path pp = Paths.get(p);
				if (!Files.exists(pp)) {

					continue;
				}

				try (Stream<Path> s = Files.walk(pp)) {

					s.filter(x -> x.toString().endsWith(".java")
						&& !x.toString().replace('\\', '/').contains("/target/")
						&& !isTestSource(x)
						&& (!mo || x.toString().replace('\\', '/').contains("/src/main/"))).forEach(fs::add);
				}
			}

			// static_block needs to know which classes nothing references, which only the whole tree shows
			indexReferences(fs);

			final List<Row> all = new ArrayList<>();

			for (Path f : fs) {

				try {

					final Result res = analyze(f);
					all.add(new Row(res.score(), res.loc(), f, res.sub(), res.accepted()));

				} catch (Exception ignore) {

					/* skip unreadable file */
				}
			}

			final String fby = by;
			all.sort((x, y) -> fby != null ? y.sub().get(fby) - x.sub().get(fby) : y.score() - x.score());
			final Path base = Paths.get(paths.get(0)).toAbsolutePath();
			int acceptedCount = 0, noFindings = 0, belowThreshold = 0;
			final List<Row> visible = new ArrayList<>();

			for (Row r : all) {

				final int metric = by != null ? r.sub().get(by) : r.score();

				// "no findings" means nothing was found, not "nothing that scores": with a signal
				// switched off (weight 0), a file can be full of findings and still score 0, and
				// counting those as clean would overstate how much of the codebase has been cleared
				final boolean nothingFound = by != null
					? metric <= 0
					: r.sub().values().stream().noneMatch(v -> v > 0);

				if (nothingFound) {

					noFindings++;
					continue;
				}

				if (metric <= 0) {

					belowThreshold++;
					continue;
				}

				if (metric < min) {

					belowThreshold++;
					continue;
				}

				if (r.accepted()) {

					acceptedCount++;

					if (showAccepted) {

						visible.add(r);
					}

				} else {

					visible.add(r);
				}
			}

			if (detailPos > 0) {

				if (detailPos > visible.size()) {

					System.out.println("only " + visible.size() + " file(s) to review — no #" + detailPos + ".");

					return;
				}

				detail(visible.get(detailPos - 1).path());

				return;
			}

			final String counts = noFindings + " with no findings"
				+ (belowThreshold > 0 ? ", " + belowThreshold + " below threshold" : "")
				+ (acceptedCount > 0 ? ", " + acceptedCount + " accepted (hidden)" : "");

			if (summary) {

				if (visible.isEmpty()) {

					System.out.println(prefix() + " " + all.size() + " files scored, 0 to review (" + counts + ").");

					return;
				}

				final int n = Math.min(10, visible.size());
				System.out.println(prefix() + " " + bold(visible.size() + " of " + all.size() + " files need review") + " (score >= " + min + "; " + counts + "). Top " + n + ":");

				for (int i = 0; i < n; i++) {

					final Row r = visible.get(i);
					final String tag = r.accepted() ? "  [accepted]" : "";

					System.out.printf("%s  #%-2d %s  (score %d)%s  %s%n", prefix(), i + 1, rel(base, r.path()), r.score(), tag, signals(r.sub()));
				}

				System.out.println(prefix() + "  " + dim("legend:"));

				for (final String line : legend(visible.subList(0, n))) {

					System.out.println(prefix() + "    " + dim(line));
				}

				return;
			}

			if (visible.isEmpty()) {

				System.out.println("Nothing to review (" + all.size() + " scored; " + counts + ").");

				return;
			}

			System.out.printf("%4s %6s %6s  %s%n", "#", "score", "loc", by != null ? "path, flags   (ranked by " + by + ")" : "path, flags");
			System.out.println("-".repeat(80));
			final List<Row> shown = new ArrayList<>();

			for (int i = 0; i < Math.min(top, visible.size()); i++) {

				final Row row = visible.get(i);
				if (by != null && row.sub().get(by) == 0) {

					continue;
				}

				final String tag = row.accepted() ? " [accepted]" : "";
				System.out.printf("%4d %6d %6d  %s%s  %s%n", i + 1, row.score(), row.loc(), rel(base, row.path()), tag, signals(row.sub()));
				shown.add(row);
			}

			System.out.println("\n" + visible.size() + " file(s) to review (score >= " + min + "); " + counts + " (" + all.size() + " scored). Heuristic.");
			System.out.println("\nlegend:");

			for (final String line : legend(shown)) {

				System.out.println("  " + line);
			}

		} catch (Throwable t) {

			System.out.println(prefix() + " skipped (" + t.getClass().getSimpleName() + ": " + t.getMessage() + ")");
		}
	}
}
