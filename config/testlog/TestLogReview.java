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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Triage for a Maven/TestNG log. A green build hides a great deal: exceptions that were logged
 * and swallowed, WARN storms nobody reads, messages whose level is miscalibrated, tests that
 * pass only because a negative case fired. Surefire reports contain none of that -- they only
 * know about failures -- so the full log is the only place it exists, and it is far too large to
 * read.
 *
 * <p>This groups a log into a ranked, deduplicated summary. The important step is
 * NORMALISATION: timestamps, thread names, UUIDs, numbers, tenant identifiers, paths and
 * {@code {...}} payloads are replaced with placeholders, so five hundred occurrences of one
 * message collapse into a single line with a count. What is left is usually a few dozen distinct
 * things, which is a readable amount.</p>
 *
 * <p>Heuristics, not a parser -- it recognises Structr's logback pattern, Maven's
 * {@code [LEVEL]} prefixes and Surefire's failure blocks. Nothing here is a verdict: a high
 * count may be a deliberate negative test (use {@code --ignore} for those), and a single
 * occurrence may be the real problem.</p>
 *
 * <p>Run: {@code java config/testlog/TestLogReview.java /tmp/full-run.log}, or pipe a log in on
 * stdin. Single file, JDK source launcher, no build step and no Python -- same as
 * {@code config/style/StyleLint.java} and {@code config/style/ReviewNeed.java}.</p>
 */
public class TestLogReview {

	// ----- log shapes we recognise -----

	/** Structr logback: "2026-08-07 12:00:32.867 [main] WARN  o.s.p.Logger - message". */
	static final Pattern APP_LINE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}[.,]\\d{3} \\[([^\\]]*)\\] (TRACE|DEBUG|INFO|WARN|ERROR)\\s+(\\S+) - (.*)$");

	/** Maven / JVM: "[WARNING] ...", "[ERROR] ...", "WARNING: ...". */
	static final Pattern BUILD_LINE = Pattern.compile("^\\[(INFO|WARNING|ERROR)\\] (.*)$");
	static final Pattern JVM_LINE   = Pattern.compile("^(WARNING|SEVERE): (.*)$");

	/** An exception header line, with or without a message. */
	static final Pattern EXCEPTION  = Pattern.compile("^((?:[a-z][a-zA-Z0-9_]*\\.)+[A-Z][A-Za-z0-9_$]*(?:Exception|Error|Throwable))(?::\\s*(.*))?$");
	static final Pattern FRAME      = Pattern.compile("^\\s+(at\\s+|\\.\\.\\.\\s+\\d+\\s+more)");
	static final Pattern CAUSED_BY  = Pattern.compile("^(Caused by|Suppressed):\\s*(.*)$");

	/** Surefire: failure markers, per-class results, and the final totals. */
	static final Pattern FAILURE_MARK = Pattern.compile("<<< (FAILURE|ERROR)!");
	static final Pattern TEST_METHOD  = Pattern.compile("^(?:\\[ERROR\\]\\s+)?(\\S+\\.[A-Za-z0-9_]+)(?:\\s+--\\s+Time elapsed).*<<< (FAILURE|ERROR)!");
	static final Pattern CLASS_RESULT = Pattern.compile("Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+), Time elapsed: ([\\d.,]+) s.*?-- in (\\S+)$");
	static final Pattern TOTALS       = Pattern.compile("Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)\\s*$");

	/** Phrasings worth surfacing even at INFO level: something did not work as intended. */
	static final Pattern PHRASES = Pattern.compile(
		"(?i)\\b(unable to|could not|cannot|failed to|not found|no such|invalid|rejected|refus|deprecated|timed out|retrying|skipped because|ignoring)\\b");

	// ----- normalisation: collapse variants of one message into a single signature -----

	static final Pattern[] NOISE = {
		Pattern.compile("\\b[0-9a-f]{32}\\b"),                       // uuids
		Pattern.compile("\\b[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}\\b"),
		Pattern.compile("\\{[^}]*\\}"),                              // map payloads
		Pattern.compile("'[^']{0,120}'"),                            // quoted values
		Pattern.compile("\"[^\"]{0,120}\""),
		Pattern.compile("(/tmp|/home|/var|[A-Z]:\\\\)[^\\s,;)\\]]*"),// paths
		Pattern.compile("\\b[A-Z]{8,12}\\b"),                        // tenant identifiers
		Pattern.compile("\\b\\d+(?:[.,]\\d+)?\\b")                   // numbers
	};

	static final String[] NOISE_REPLACEMENT = { "<id>", "<id>", "{..}", "'..'", "\"..\"", "<path>", "<tenant>", "#" };

	// ----- collected state -----

	static final Map<String, Stat> errors       = new LinkedHashMap<>();
	static final Map<String, Stat> warnings     = new LinkedHashMap<>();
	static final Map<String, Stat> phrases      = new LinkedHashMap<>();
	static final Map<String, Stat> loggedTraces = new LinkedHashMap<>();
	static final Map<String, Stat> testTraces   = new LinkedHashMap<>();
	static final List<String[]> failingTests    = new ArrayList<>();
	static final List<Object[]> classTimes      = new ArrayList<>();
	static final List<Pattern> ignores          = new ArrayList<>();

	static int totalTests = -1, totalFailures = -1, totalErrors = -1, totalSkipped = -1;
	static String buildResult = "unknown";
	static long lineCount = 0;

	static int top      = 15;
	static int minCount = 1;
	static String section = "all";

	public static void main(final String[] args) throws IOException {

		Path file = null;

		for (int i = 0; i < args.length; i++) {

			final String a = args[i];

			switch (a) {

				case "--top"       -> top      = argInt(args, ++i, "--top");
				case "--min-count" -> minCount = argInt(args, ++i, "--min-count");
				case "--section"   -> section  = argValue(args, ++i, "--section");
				case "--ignore"    -> ignores.add(Pattern.compile(argValue(args, ++i, "--ignore")));
				case "--help", "-h" -> usage(null);

				default -> {

					if (a.startsWith("-")) {

						usage("unknown option: " + a);
					}

					if (file != null) {

						usage("only one log file can be analysed at a time");
					}

					file = Paths.get(a);
				}
			}
		}

		if (file != null && !Files.isReadable(file)) {

			usage("cannot read log file: " + file);
		}

		if (!Set.of("all", "summary", "failures", "errors", "warnings", "traces", "phrases", "slow").contains(section)) {

			usage("unknown section: " + section);
		}

		try (final BufferedReader reader = (file != null)
			? Files.newBufferedReader(file, StandardCharsets.UTF_8)
			: new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

			analyze(reader);
		}

		report(file);
	}

	// ----- single pass over the log -----

	static void analyze(final BufferedReader reader) throws IOException {

		// a short lookback is enough to tell "this stack trace belongs to a reported test
		// failure" from "somebody logged an exception and carried on"
		final ArrayDeque<String> lookback = new ArrayDeque<>();
		String line;

		while ((line = reader.readLine()) != null) {

			lineCount++;

			final String clean = strip(line);
			if (ignored(clean)) {

				remember(lookback, clean);
				continue;
			}

			final Matcher exception = EXCEPTION.matcher(clean);
			if (exception.matches()) {

				final boolean fromFailure = lookback.stream().anyMatch(l -> FAILURE_MARK.matcher(l).find());
				final String signature    = collectTrace(reader, exception.group(1), exception.group(2));

				count(fromFailure ? testTraces : loggedTraces, signature, lineCount, clean);
				remember(lookback, clean);

				continue;
			}

			collectResults(clean);
			collectMessages(clean);
			remember(lookback, clean);
		}
	}

	/**
	 * Consume the frames belonging to an exception header and build its signature: the exception
	 * type, its normalised message, and the first Structr frame -- which is the line a developer
	 * actually needs.
	 */
	static String collectTrace(final BufferedReader reader, final String type, final String message) throws IOException {

		String origin = null;
		String line;

		while ((line = reader.readLine()) != null) {

			lineCount++;

			final String clean = strip(line);
			if (FRAME.matcher(clean).find()) {

				if (origin == null && clean.contains("at org.structr.")) {

					origin = clean.trim();
				}

				continue;
			}

			if (CAUSED_BY.matcher(clean).find()) {

				continue;
			}

			// not part of the trace any more: put the line back through the normal path
			collectResults(clean);
			collectMessages(clean);

			break;
		}

		return type + (message != null ? ": " + normalize(message) : "") + (origin != null ? "\n      first structr frame: " + origin : "");
	}

	static void collectResults(final String clean) {

		final Matcher failingTest = TEST_METHOD.matcher(clean);
		if (failingTest.find()) {

			failingTests.add(new String[] { failingTest.group(1), failingTest.group(2) });
		}

		final Matcher classResult = CLASS_RESULT.matcher(clean);
		if (classResult.find()) {

			classTimes.add(new Object[] { classResult.group(6), Double.parseDouble(classResult.group(5).replace(',', '.')) });
		}

		final Matcher totals = TOTALS.matcher(clean);
		if (totals.find() && !clean.contains("-- in ")) {

			totalTests    = Integer.parseInt(totals.group(1));
			totalFailures = Integer.parseInt(totals.group(2));
			totalErrors   = Integer.parseInt(totals.group(3));
			totalSkipped  = Integer.parseInt(totals.group(4));
		}

		if (clean.contains("BUILD SUCCESS")) {

			buildResult = "SUCCESS";
		}

		if (clean.contains("BUILD FAILURE")) {

			buildResult = "FAILURE";
		}
	}

	static void collectMessages(final String clean) {

		final Matcher app = APP_LINE.matcher(clean);
		if (app.matches()) {

			final String level   = app.group(2);
			final String logger  = shorten(app.group(3));
			final String message = app.group(4);
			final String key     = pad(logger, 44) + normalize(message);

			if ("ERROR".equals(level)) {

				count(errors, key, lineCount, clean);

			} else if ("WARN".equals(level)) {

				count(warnings, key, lineCount, clean);

			} else if (PHRASES.matcher(message).find()) {

				count(phrases, pad(level + " " + logger, 44) + normalize(message), lineCount, clean);
			}

			return;
		}

		final Matcher build = BUILD_LINE.matcher(clean);
		final Matcher jvm   = JVM_LINE.matcher(clean);

		final String level;
		final String message;

		if (build.matches()) {

			level   = build.group(1);
			message = build.group(2);

		} else if (jvm.matches()) {

			level   = jvm.group(1);
			message = jvm.group(2);

		} else {

			return;
		}

		// maven's own chatter is not interesting unless it reports a problem
		if ("ERROR".equals(level)) {

			count(errors, pad("[maven]", 44) + normalize(message), lineCount, clean);

		} else if ("WARNING".equals(level) && !message.isBlank() && !message.startsWith("***")) {

			count(warnings, pad("[maven]", 44) + normalize(message), lineCount, clean);
		}
	}

	// ----- reporting -----

	static void report(final Path file) {

		System.out.println();
		System.out.println("Test log review: " + (file != null ? file : "<stdin>") + "  (" + lineCount + " lines)");

		if (show("summary")) {

			System.out.println();
			System.out.printf(Locale.ROOT, "  build           %s%n", buildResult);

			if (totalTests >= 0) {

				System.out.printf(Locale.ROOT, "  tests           %d run, %d failure(s), %d error(s), %d skipped%n", totalTests, totalFailures, totalErrors, totalSkipped);
			}

			System.out.printf(Locale.ROOT, "  distinct        %d error signature(s), %d warning signature(s), %d logged exception(s)%n",
				errors.size(), warnings.size(), loggedTraces.size());
		}

		if (show("failures") && !failingTests.isEmpty()) {

			System.out.println();
			System.out.println("REPORTED TEST FAILURES (" + failingTests.size() + ")");

			for (final String[] failure : failingTests) {

				System.out.printf(Locale.ROOT, "  %-8s %s%n", failure[1], failure[0]);
			}
		}

		// The most valuable section: an exception that was logged and execution continued. No
		// test failed, so nothing else in the build mentions it.
		section("LOGGED / SWALLOWED EXCEPTIONS (no test reported them)", loggedTraces, "traces");
		section("ERRORS", errors, "errors");
		section("WARNINGS", warnings, "warnings");
		section("OTHER PROBLEM PHRASINGS (INFO/DEBUG level)", phrases, "phrases");

		if (show("traces") && !testTraces.isEmpty()) {

			section("EXCEPTIONS FROM REPORTED FAILURES (expected if the tests are red)", testTraces, "traces");
		}

		if (show("slow") && !classTimes.isEmpty()) {

			classTimes.sort((a, b) -> Double.compare((Double) b[1], (Double) a[1]));

			System.out.println();
			System.out.println("SLOWEST TEST CLASSES");

			for (int i = 0; i < Math.min(top, classTimes.size()); i++) {

				System.out.printf(Locale.ROOT, "  %8.1fs  %s%n", (Double) classTimes.get(i)[1], classTimes.get(i)[0]);
			}
		}

		System.out.println();
		System.out.println("A count is not a verdict: high counts are often deliberate negative tests (filter them with");
		System.out.println("--ignore REGEX), and a single occurrence can be the real defect. Line numbers refer to the log.");
		System.out.println();
	}

	static void section(final String title, final Map<String, Stat> collected, final String name) {

		if (!show(name) || collected.isEmpty()) {

			return;
		}

		final List<Map.Entry<String, Stat>> rows = new ArrayList<>(collected.entrySet());

		rows.sort((a, b) -> Integer.compare(b.getValue().count, a.getValue().count));

		int shown   = 0;
		int hidden  = 0;
		final StringBuilder buf = new StringBuilder();

		for (final Map.Entry<String, Stat> row : rows) {

			if (row.getValue().count < minCount) {

				hidden++;
				continue;
			}

			if (shown >= top) {

				hidden++;
				continue;
			}

			buf.append(String.format(Locale.ROOT, "  %6dx  L%-7d %s%n", row.getValue().count, row.getValue().firstLine, row.getKey()));
			shown++;
		}

		if (shown == 0) {

			return;
		}

		System.out.println();
		System.out.println(title + " (" + collected.size() + " distinct" + (hidden > 0 ? ", " + hidden + " not shown" : "") + ")");
		System.out.print(buf);
	}

	// ----- helpers -----

	static boolean show(final String name) {

		return "all".equals(section) || section.equals(name);
	}

	static boolean ignored(final String clean) {

		for (final Pattern pattern : ignores) {

			if (pattern.matcher(clean).find()) {

				return true;
			}
		}

		return false;
	}

	static void remember(final ArrayDeque<String> lookback, final String clean) {

		lookback.addLast(clean);

		while (lookback.size() > 20) {

			lookback.removeFirst();
		}
	}

	static void count(final Map<String, Stat> collected, final String key, final long line, final String example) {

		collected.computeIfAbsent(key, k -> new Stat(line, example)).count++;
	}

	/** Remove ANSI colour codes and trailing whitespace; Maven colourises its output. */
	static String strip(final String line) {

		return line.replaceAll("\\[[0-9;]*[A-Za-z]", "").stripTrailing();
	}

	static String normalize(final String message) {

		String result = message;

		for (int i = 0; i < NOISE.length; i++) {

			result = NOISE[i].matcher(result).replaceAll(NOISE_REPLACEMENT[i]);
		}

		return result.trim();
	}

	/** "org.structr.core.property.PropertyMap" and "o.s.c.p.PropertyMap" both become "PropertyMap". */
	static String shorten(final String logger) {

		final int dot = logger.lastIndexOf('.');

		return (dot < 0) ? logger : logger.substring(dot + 1);
	}

	static String pad(final String value, final int width) {

		return (value.length() >= width) ? value.substring(0, width - 1) + " " : value + " ".repeat(width - value.length());
	}

	static String argValue(final String[] args, final int i, final String flag) {

		if (i >= args.length) {

			usage("missing value for " + flag);
		}

		return args[i];
	}

	static int argInt(final String[] args, final int i, final String flag) {

		final String value = argValue(args, i, flag);

		try {

			return Integer.parseInt(value);

		} catch (final NumberFormatException nfe) {

			usage(flag + " expects a number, got: " + value);

			return 0;
		}
	}

	static void usage(final String err) {

		if (err != null) {

			System.err.println("error: " + err);
		}

		System.out.println("usage: java config/testlog/TestLogReview.java [options] [logfile]");
		System.out.println("  --top N          max rows per section (default 15)");
		System.out.println("  --min-count N    hide signatures seen fewer than N times (default 1)");
		System.out.println("  --section NAME    show one section only: summary|failures|errors|warnings|traces|phrases|slow|all");
		System.out.println("  --ignore REGEX   drop matching lines; repeatable, for known-expected noise");
		System.out.println("  --help, -h       show this help");
		System.out.println("  logfile          the Maven/TestNG log; reads stdin when omitted");
		System.out.println();
		System.out.println("produce a log with:  mvn ... 2>&1 | tee /tmp/full-run.log");
		System.exit(err == null ? 0 : 2);
	}

	static final class Stat {

		final long firstLine;
		final String example;
		int count = 0;

		Stat(final long firstLine, final String example) {

			this.firstLine = firstLine;
			this.example   = example;
		}
	}
}
