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
import java.time.LocalDate;
import java.io.Reader;
import java.util.stream.Stream;
import java.util.*;
import java.util.regex.*;

/**
 * Triage for a Structr test log: turn a log nobody can read into a ranked, deduplicated summary of
 * what happened, and -- against a baseline -- of what is NEW.
 *
 * <p>A green build hides plenty: exceptions logged and swallowed, WARN storms nobody reads,
 * messages at the wrong level, tests that pass only because a negative case fired. Surefire reports
 * contain none of it; only the full log does, and it is far too large to read.</p>
 *
 * <h3>What it recognises -- and what it refuses to</h3>
 *
 * <p>Two kinds of knowledge, and the difference decides whether this tool ages well:</p>
 * <ul>
 * <li><b>Formats</b> are fair game: an ISO timestamp, a {@code [thread]} field, a log level,
 *     Maven's {@code [LEVEL]} prefix, a stack frame, a long hex id, an absolute path. They are
 *     syntax, they change only when a log framework or a serialiser changes, and recognising them is
 *     what lets a line be attributed to a level and a logger at all.</li>
 * <li><b>Message wording is not.</b> An earlier version learned the random tenant identifiers by
 *     looking for the harness's sentence "##### Starting X with tenant identifier Y". That works
 *     right until somebody rewords or deletes that line -- after which every tenant id becomes part
 *     of a signature, grouping quietly falls apart, and the tool reports one message as hundreds.
 *     A tool whose failure mode is silence must not rest on someone else's phrasing.</li>
 * </ul>
 *
 * <h3>The variables are measured, not assumed</h3>
 *
 * <p>Pass one measures the log, pass two uses the measurements. Lines are bucketed by SHAPE (kind,
 * token count, first token) -- deliberately not by logger, so moving or renaming a logger cannot
 * change how lines group -- and for every token position the pass records which values occur. Two
 * independent signals then mark a position as a variable:</p>
 * <ol>
 * <li>it takes many different values across occurrences of that shape; or</li>
 * <li>every value it takes is globally rare while the shape itself repeats -- which is exactly how
 *     an identifier looks in a message that only occurs a few times.</li>
 * </ol>
 *
 * <p>Whatever fills those positions gets replaced by {@code *}: tenant identifiers, ids, counts,
 * durations, thread names, host names, and formats this tool has never heard of, including ones
 * added after it was written. Nothing in the mechanism knows what a tenant is.</p>
 *
 * <p>{@link #maskFormats} still runs first, on shapes only. It earns its place for the lines that
 * occur once, where there is nothing to learn from and a raw id would otherwise land in a
 * signature. Small numbers are deliberately NOT masked -- an HTTP 403 and an HTTP 422 are different
 * problems -- and where a small number really is incidental, the measurement masks it anyway.</p>
 *
 * <h3>Baseline diff -- what makes it a daily tool</h3>
 *
 * <p>Even perfectly grouped, a full run yields a few hundred signatures, which nobody reads twice.
 * {@code --write-baseline} records a log you consider clean; later runs then report only what is new
 * or has grown ({@code --baseline}, and {@code --fail-on-new} for CI).</p>
 *
 * <h3>Every line names its test -- which is what makes the output actionable</h3>
 *
 * <p>Counting and grouping can only ever say what was logged, never who did it, and "what was logged"
 * is mostly noise: on a fully green run, some 60 of 63 distinct ERROR signatures were deliberate --
 * negative tests provoking errors to assert on them. Level is the wrong axis. What separates a finding
 * from noise is whether anybody <b>asserted</b> on it, and that question needs the test behind the
 * line.</p>
 *
 * <p>The merged log cannot answer it: with {@code forkCount=4} up to four JVMs write into one stream,
 * and 73% of WARN/ERROR lines were measured to be emitted while more than one test was open. So the
 * information is preserved at the source instead -- {@code TestContextListener} puts the running test
 * into logback's MDC, the test pattern prints it as {@code {Class#method}}, and the same listener logs
 * one machine-readable verdict per test. This tool then reads ownership rather than guessing it, and
 * the fragile alternative it replaces (learning tests from the harness's {@code ##### Starting}
 * sentence) is gone.</p>
 *
 * <p>Three sections follow from that, and none of them can be had from a surefire report:
 * exceptions logged by a test that then <b>passed</b>; ERROR-level messages seen <b>only</b> in
 * passing tests, i.e. a work list of wrong levels whose completion shrinks every future log; and
 * output belonging to <b>no test at all</b>, where teardown bugs and leaks live -- a per-test capture
 * hides those by charging them to whichever test happened to be open.</p>
 *
 * <p>Attribution is additive: the field is optional in {@link #APP_LINE}, no signature contains it, and
 * a log recorded without it produces exactly the report it always did, minus those three sections.</p>
 *
 * <p>Single file, JDK source launcher, no dependencies -- same as {@code config/style/StyleLint.java}
 * and {@code config/style/CodeQuality.java}.</p>
 */
public class TestLogReview {

	// ----- formats: syntax, never wording -----

	/**
	 * logback: "2026-08-07 12:00:32.867 [main] {BasicTest#test01} WARN  logger - message".
	 *
	 * <p>The {@code {test}} field is written by {@code TestContextListener} through the MDC and names
	 * the test that produced the line. It is optional in this pattern on purpose: a log recorded before
	 * that listener existed, or one from a running server, still parses -- and so does the committed
	 * baseline, because the field is not part of any signature.</p>
	 */
	static final Pattern APP_LINE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}[.,]\\d{3} \\[([^\\]]*)\\](?: \\{([^}]*)\\})? (TRACE|DEBUG|INFO|WARN|ERROR)\\s+(\\S+) - (.*)$");

	/**
	 * The verdict line written by {@code TestContextListener} after every test method. Read for the
	 * status alone, and machine-generated key=value rather than prose -- the same standing as
	 * {@link #ERROR_TOKENS}. Nothing groups on it; if it disappears, the sections that need a verdict
	 * are simply absent instead of wrong.
	 */
	static final Pattern TEST_RESULT = Pattern.compile("^test=(\\S+) status=(\\S+) durationMs=(\\d+)$");

	static final Pattern BUILD_LINE = Pattern.compile("^\\[(INFO|WARNING|ERROR)\\] (.*)$");
	static final Pattern JVM_LINE   = Pattern.compile("^(WARNING|SEVERE): (.*)$");

	static final Pattern EXCEPTION = Pattern.compile("^((?:[a-z][a-zA-Z0-9_]*\\.)+[A-Z][A-Za-z0-9_$]*(?:Exception|Error|Throwable))(?::\\s*(.*))?$");
	static final Pattern FRAME     = Pattern.compile("^\\s+(at\\s+|\\.\\.\\.\\s+\\d+\\s+more)");
	static final Pattern CAUSED_BY = Pattern.compile("^(Caused by|Suppressed):\\s*(.*)$");

	/** Surefire's output formats. */
	static final Pattern FAILURE_MARK = Pattern.compile("<<< (FAILURE|ERROR)!");
	static final Pattern TEST_METHOD  = Pattern.compile("^(?:\\[ERROR\\]\\s+)?(\\S+\\.[A-Za-z0-9_]+)(?:\\s+--\\s+Time elapsed).*<<< (FAILURE|ERROR)!");
	static final Pattern CLASS_RESULT = Pattern.compile("Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+), Time elapsed: ([\\d.,]+) s.*?-- in (\\S+)$");
	static final Pattern TOTALS       = Pattern.compile("Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)\\s*$");

	/** Maven names the goal and project that broke the build; without it "FAILURE" sends you hunting. */
	static final Pattern FAILING_GOAL = Pattern.compile("Failed to execute goal (\\S+) .*? on project (\\S+):");

	/**
	 * Structr's ErrorBuffer serialisation. This is the one place message text is read, and it is
	 * generated by code rather than typed by a person. Nothing groups on it: if the format ever
	 * changes the section is simply empty instead of wrong.
	 */
	static final Pattern ERROR_TOKENS = Pattern.compile("ErrorTokens:\\s*(\\S+\\.\\S+)\\s+(?:[A-Z][A-Z0-9_]*\\s+)?([a-z][a-z0-9_]*)");

	/** Used only to SELECT lines worth showing, never to group them. */
	static final Pattern PHRASES = Pattern.compile(
		"(?i)\\b(unable to|could not|cannot|failed to|not found|no such|invalid|rejected|refus|deprecated|timed out|retrying|skipped because|ignoring)\\b");

	// ----- format masks: universal shapes for the lines that occur only once -----

	static final Pattern ISO_TIME  = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d+)?");

	/**
	 * A bare date, with no time after it. It earns its own mask because generated file names carry one:
	 * "renaming to test_2026-08-10-073132742.txt" changed every midnight, so five signatures reported
	 * themselves as NEW once a day, for ever. BIG_NUM cannot help -- in "test_2026" there is no word
	 * boundary before the year.
	 */
	static final Pattern ISO_DATE  = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

	/** Java's identity hash suffix, "FileTraitDefinition$6@1486ca8e" -- a new value on every run. */
	static final Pattern IDENTITY  = Pattern.compile("@[0-9a-f]{6,}\\b");

	/** A long lower-case token containing at least one digit: session ids and the like. */
	static final Pattern LONG_ID   = Pattern.compile("\\b(?=[a-z0-9]*\\d)[a-z0-9]{20,}\\b");
	static final Pattern UUID_DASH = Pattern.compile("\\b[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}\\b");
	static final Pattern HEX_ID    = Pattern.compile("\\b[0-9a-f]{16,}\\b");
	static final Pattern ABS_PATH  = Pattern.compile("(?:/[\\w.@+-]+){2,}/?|[A-Za-z]:\\\\[^\\s,;)\\]]*");
	static final Pattern BIG_NUM   = Pattern.compile("\\b\\d{4,}\\b");

	static String maskFormats(final String message) {

		String result = message;

		result = ISO_TIME.matcher(result).replaceAll("<time>");
		result = ISO_DATE.matcher(result).replaceAll("<date>");
		result = UUID_DASH.matcher(result).replaceAll("<id>");
		result = HEX_ID.matcher(result).replaceAll("<id>");
		result = IDENTITY.matcher(result).replaceAll("@<id>");
		result = LONG_ID.matcher(result).replaceAll("<id>");
		result = ABS_PATH.matcher(result).replaceAll("<path>");
		result = BIG_NUM.matcher(result).replaceAll("<n>");

		return result.trim();
	}

	// ----- what pass one measures -----

	/** shape -> per token position, the values seen there (capped). */
	static final Map<String, List<Set<String>>> shapeValues = new HashMap<>();
	static final Map<String, Integer> shapeCounts           = new HashMap<>();
	static final Map<String, Set<Integer>> shapeVariables   = new HashMap<>();

	/** token value -> number of lines it appeared in, to recognise globally rare values. */
	static final Map<String, Integer> tokenLines = new HashMap<>();

	static final int VALUE_CAP     = 128;
	static final int RARE_LINES    = 3;
	static final int MIN_SHAPE_HITS = 4;

	// ----- collected results -----

	static final Map<String, Stat> errors       = new LinkedHashMap<>();
	static final Map<String, Stat> warnings     = new LinkedHashMap<>();
	static final Map<String, Stat> phrases      = new LinkedHashMap<>();
	static final Map<String, Stat> loggedTraces = new LinkedHashMap<>();
	static final Map<String, Stat> testTraces   = new LinkedHashMap<>();
	static final Map<String, Stat> errorTokens  = new LinkedHashMap<>();
	static final List<String[]> failingTests    = new ArrayList<>();
	static final List<String> retryExhausted    = new ArrayList<>();
	static final List<Object[]> classTimes      = new ArrayList<>();
	static final List<Pattern> ignores          = new ArrayList<>();

	/**
	 * Verdicts, keyed by the SIMPLE test name that log lines carry; the qualified name is kept for
	 * display. A retried test overwrites its earlier verdict, so what remains is the final outcome.
	 */
	static final Map<String, String> testStatus     = new LinkedHashMap<>();
	static final Map<String, String> qualifiedNames  = new LinkedHashMap<>();

	/**
	 * Owner of the most recent application line. Stack traces inherit it: a bare {@code at ...} frame
	 * carries no field of its own, and logback renders a throwable immediately after the line it
	 * belongs to. Maven's INFO chatter does not clear it, so a trace stays attributed across it.
	 */
	static String currentTest   = null;
	static String currentThread = null;
	static int attributedLines  = 0;
	static int appLines        = 0;
	static String sourceName   = null;
	static int reportedClasses = 0;

	static int totalTests = 0, totalFailures = 0, totalErrors = 0, totalSkipped = 0;
	static int moduleSummaries = 0;
	static String failingGoal  = null;
	static String buildResult = "unknown";
	static long lineCount = 0;
	static int learnedShapes = 0;

	static int top        = 8;
	static int minCount   = 1;
	static String section = "all";
	static boolean learn  = true;
	static boolean failOnNew = false;
	static Path baseline      = null;
	static Path reports       = null;
	static boolean brief      = false;
	static boolean color      = false;
	static Path writeBaseline = null;

	/**
	 * The state of a log we currently consider normal, committed alongside this tool. Used unless
	 * {@code --baseline} says otherwise, so the everyday invocation needs no arguments and shows only
	 * what deviates. Because it is a file in the repository, accepting new noise becomes an explicit,
	 * reviewable commit rather than a habit of ignoring lines -- and the diff of that commit says
	 * exactly which messages the team decided to live with.
	 */
	static final Path DEFAULT_BASELINE = Paths.get("config/testlog/normal.baseline");
	static boolean usingDefaultBaseline = false;

	public static void main(final String[] args) throws IOException {

		Path file = null;

		for (int i = 0; i < args.length; i++) {

			final String a = args[i];

			switch (a) {

				case "--top"            -> top      = argInt(args, ++i, "--top");
				case "--min-count"      -> minCount = argInt(args, ++i, "--min-count");
				case "--section"        -> section  = argValue(args, ++i, "--section");
				case "--ignore"         -> ignores.add(Pattern.compile(argValue(args, ++i, "--ignore")));
				case "--baseline"       -> baseline = Paths.get(argValue(args, ++i, "--baseline"));
				case "--write-baseline" -> writeBaseline = Paths.get(argValue(args, ++i, "--write-baseline"));
				case "--reports"        -> reports = Paths.get(argValue(args, ++i, "--reports"));
				case "--brief"          -> brief = true;
				case "--color"          -> color = "always".equals(argValue(args, ++i, "--color"));
				case "--no-learn"       -> learn = false;
				case "--fail-on-new"    -> failOnNew = true;
				case "--help", "-h"     -> usage(null);

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

		if (baseline != null && !Files.isReadable(baseline)) {

			usage("cannot read baseline: " + baseline);
		}

		if (baseline == null && Files.isReadable(DEFAULT_BASELINE)) {

			baseline            = DEFAULT_BASELINE;
			usingDefaultBaseline = true;
		}

		if (!Set.of("all", "summary", "failures", "errors", "warnings", "traces", "phrases", "tokens", "slow", "diff", "passed", "calibration", "unowned").contains(section)) {

			usage("unknown section: " + section);
		}

		final List<Path> reportFiles = (reports != null) ? findReports(reports) : List.of();
		if (reports != null && reportFiles.isEmpty()) {

			// A build with no tests, or -DskipTests. Say nothing rather than report an empty log as clean.

			return;
		}

		if (file == null && reports == null) {

			// stdin cannot be measured and then re-read, so grouping falls back to formats only
			learn = false;
		}

		if (learn) {

			try (final BufferedReader reader = openInput(file, reportFiles)) {

				measure(reader);
			}

			resolveVariables();
		}

		if (reports != null) {

			countSuites(reportFiles);
		}

		try (final BufferedReader reader = openInput(file, reportFiles)) {

			collect(reader);
		}

		if (file != null) {

			sourceName = file.getFileName().toString();
		}

		final Diff diff = (baseline != null) ? compareToBaseline(baseline) : null;

		if (brief) {

			reportBriefly(diff);

		} else {

			report(file, diff);
		}

		if (writeBaseline != null) {

			writeBaseline(writeBaseline);
		}

		if (failOnNew && diff != null && !diff.added.isEmpty()) {

			System.exit(1);
		}
	}

	/**
	 * The captured stdout of every test class, straight out of surefire's and failsafe's report XMLs.
	 *
	 * <p>This is what lets the review run on EVERY build rather than only when somebody remembers to
	 * pipe the console through {@code tee}: the forked JVM's output is already written to
	 * {@code target/*-reports/TEST-*.xml}, in the same textual format, {@code {test}} field included.
	 * Maven's own {@code [INFO]}/{@code [WARNING]} lines are not in there, so a piped full log remains
	 * the better input when build-level noise matters.</p>
	 */
	static final Pattern SYSTEM_OUT = Pattern.compile("<system-out>(.*?)</system-out>", Pattern.DOTALL);
	static final Pattern SUITE      = Pattern.compile("<testsuite[^>]*?tests=\"(\\d+)\"[^>]*?>");
	static final Pattern SUITE_ATTR = Pattern.compile("(failures|errors|skipped)=\"(\\d+)\"");
	static final Pattern NUMERIC    = Pattern.compile("&#(\\d+);");

	static List<Path> findReports(final Path root) throws IOException {

		try (final Stream<Path> stream = Files.walk(root, 6)) {

			return stream
				.filter(path -> path.getFileName().toString().startsWith("TEST-") && path.toString().endsWith(".xml"))
				.filter(path -> path.getParent() != null && path.getParent().getFileName().toString().endsWith("-reports"))
				.sorted()
				.toList();
		}
	}

	/** Test counts come from the XML in this mode; Maven's summary lines are not part of the reports. */
	static void countSuites(final List<Path> files) {

		for (final Path file : files) {

			final String xml     = readQuietly(file);
			final Matcher suite  = SUITE.matcher(xml);

			if (suite.find()) {

				totalTests += Integer.parseInt(suite.group(1));
				reportedClasses++;

				final Matcher attr = SUITE_ATTR.matcher(suite.group());

				while (attr.find()) {

					final int value = Integer.parseInt(attr.group(2));

					switch (attr.group(1)) {

						case "failures" -> totalFailures += value;
						case "errors"   -> totalErrors   += value;
						default         -> totalSkipped  += value;
					}
				}
			}
		}
	}

	static String readQuietly(final Path file) {

		try {

			// bytes, not readString(): a malformed byte in captured output must not abort the review

			return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);

		} catch (final IOException ioex) {

			return "";
		}
	}

	static String systemOut(final Path file) {

		final Matcher matcher   = SYSTEM_OUT.matcher(readQuietly(file));
		final StringBuilder out = new StringBuilder();

		while (matcher.find()) {

			out.append(unescape(matcher.group(1))).append('\n');
		}

		return out.toString();
	}

	static String unescape(final String text) {

		String result = text.replace("<![CDATA[", "").replace("]]>", "");

		result = NUMERIC.matcher(result).replaceAll(match -> String.valueOf((char) Integer.parseInt(match.group(1))));

		return result
			.replace("&lt;", "<")
			.replace("&gt;", ">")
			.replace("&quot;", "\"")
			.replace("&apos;", "'")
			.replace("&amp;", "&");
	}

	/** Streams the reports so the two passes never hold every captured log in memory at once. */
	static final class ReportReader extends Reader {

		private final Iterator<Path> files;
		private String buffer = "";
		private int position  = 0;

		ReportReader(final List<Path> files) {

			this.files = files.iterator();
		}

		@Override
		public int read(final char[] target, final int offset, final int length) {

			while (position >= buffer.length()) {

				if (!files.hasNext()) {

					return -1;
				}

				buffer   = systemOut(files.next());
				position = 0;
			}

			final int count = Math.min(length, buffer.length() - position);

			buffer.getChars(position, position + count, target, offset);

			position += count;

			return count;
		}

		@Override
		public void close() {
		}
	}

	static BufferedReader openInput(final Path file, final List<Path> reportFiles) throws IOException {

		if (!reportFiles.isEmpty()) {

			return new BufferedReader(new ReportReader(reportFiles));
		}

		if (file != null) {

			return Files.newBufferedReader(file, StandardCharsets.UTF_8);
		}

		return new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
	}

	// ----- pass one: measure -----

	static void measure(final BufferedReader reader) throws IOException {

		String line;

		while ((line = reader.readLine()) != null) {

			final Parsed parsed = parse(strip(line));
			if (parsed == null) {

				continue;
			}

			final String[] tokens = tokenize(parsed.message());
			final String shape    = shapeOf(parsed, tokens);
			final List<Set<String>> perPosition = shapeValues.computeIfAbsent(shape, k -> new ArrayList<>());

			shapeCounts.merge(shape, 1, Integer::sum);

			while (perPosition.size() < tokens.length) {

				perPosition.add(new HashSet<>());
			}

			for (int i = 0; i < tokens.length; i++) {

				final Set<String> seen = perPosition.get(i);

				// past the cap the position is plainly a variable; stop growing the set
				if (seen.size() <= VALUE_CAP) {

					seen.add(tokens[i]);
				}
			}

			for (final String token : new HashSet<>(Arrays.asList(tokens))) {

				tokenLines.merge(token, 1, Integer::sum);
			}
		}
	}

	/**
	 * Mark the token positions that hold values rather than words, from the measurements alone.
	 * Shapes with too few occurrences are left untouched: there is nothing to learn from them, and
	 * guessing would throw away the detail that makes a rare line worth reading.
	 */
	static void resolveVariables() {

		for (final Map.Entry<String, List<Set<String>>> shape : shapeValues.entrySet()) {

			final int occurrences = shapeCounts.getOrDefault(shape.getKey(), 0);
			if (occurrences < MIN_SHAPE_HITS) {

				continue;
			}

			final List<Set<String>> perPosition = shape.getValue();
			final Set<Integer> variables        = new HashSet<>();

			for (int i = 0; i < perPosition.size(); i++) {

				final Set<String> values = perPosition.get(i);
				final int distinct       = values.size();

				if (distinct == 1) {

					continue;
				}

				// (1) the position varies across occurrences of this shape
				if (distinct > VALUE_CAP || distinct >= occurrences * 0.3) {

					variables.add(i);
					continue;
				}

				// (2) or everything it holds is rare in the log as a whole -- an identifier in a
				// message that repeats only a handful of times
				if (allGloballyRare(values)) {

					variables.add(i);
				}
			}

			// If most of the line would be masked, the bucket is holding messages that merely look
			// alike; grouping them destroys more than it saves, so leave them alone and let them
			// appear as separate signatures.
			if (!variables.isEmpty() && variables.size() <= Math.max(1, perPosition.size() * 0.5)) {

				shapeVariables.put(shape.getKey(), variables);
			}
		}

		learnedShapes = shapeVariables.size();
	}

	static boolean allGloballyRare(final Set<String> values) {

		for (final String value : values) {

			if (tokenLines.getOrDefault(value, 0) > RARE_LINES) {

				return false;
			}
		}

		return true;
	}

	// ----- pass two: collect -----

	static void collect(final BufferedReader reader) throws IOException {

		final ArrayDeque<String> lookback = new ArrayDeque<>();
		String line;

		lineCount = 0;

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

				// Both taken BEFORE the trace is read: collectTrace consumes every frame, so afterwards
				// lineCount points at the end of the stack and currentTest may already have moved on to
				// the next test's output. Reporting either of those sends the reader to the wrong place.
				final long start   = lineCount;
				final String owner = currentTest;
				final String signature = collectTrace(reader, exception.group(1), exception.group(2));

				attribute(count(fromFailure ? testTraces : loggedTraces, signature, start, clean), owner);
				remember(lookback, clean);

				continue;
			}

			collectSurefire(clean);
			collectErrorTokens(clean);
			collectMessage(clean);
			remember(lookback, clean);
		}
	}

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

			collectSurefire(clean);
			collectErrorTokens(clean);
			collectMessage(clean);

			break;
		}

		return type + (message != null ? ": " + maskFormats(message) : "")
			+ (origin != null ? "\n      first structr frame: " + origin : "");
	}

	static void collectMessage(final String clean) {

		final Parsed parsed = parse(clean);
		if (parsed == null) {

			return;
		}

		if ("app".equals(parsed.kind())) {

			appLines++;
			currentTest   = parsed.test();
			currentThread = threadClass(parsed.thread());

			if (currentTest != null) {

				attributedLines++;
			}
		}

		if (collectVerdict(parsed)) {

			return;
		}

		final String key = pad(parsed.logger(), 40) + template(parsed);

		switch (parsed.level()) {

			case "ERROR" -> attribute(count(errors, key, lineCount, clean), parsed);
			case "WARN", "WARNING", "SEVERE" -> attribute(count(warnings, key, lineCount, clean), parsed);

			default -> {

				if (PHRASES.matcher(parsed.message()).find()) {

					attribute(count(phrases, pad(parsed.level() + " " + parsed.logger(), 40) + template(parsed), lineCount, clean), parsed);
				}
			}
		}
	}

	/** Record a test's verdict; returns true when the line was one and needs no further handling. */
	static boolean collectVerdict(final Parsed parsed) {

		final Matcher verdict = TEST_RESULT.matcher(parsed.message());
		if (!verdict.matches()) {

			return false;
		}

		final String qualified = verdict.group(1);
		final String simple    = simpleName(qualified);

		testStatus.put(simple, verdict.group(2));
		qualifiedNames.put(simple, qualified);

		return true;
	}

	/** "org.structr.test.common.BasicTest#test01" -> "BasicTest#test01", the form log lines carry. */
	static String simpleName(final String qualified) {

		final int hash    = qualified.indexOf('#');
		final String type = (hash < 0) ? qualified : qualified.substring(0, hash);
		final int dot     = type.lastIndexOf('.');

		return ((dot < 0) ? type : type.substring(dot + 1)) + ((hash < 0) ? "" : qualified.substring(hash));
	}

	/** Format-masked, then with every measured variable position replaced by {@code *}. */
	static String template(final Parsed parsed) {

		final String[] tokens = tokenize(parsed.message());

		if (!learn) {

			return String.join(" ", tokens);
		}

		final Set<Integer> variables = shapeVariables.get(shapeOf(parsed, tokens));
		if (variables == null) {

			return String.join(" ", tokens);
		}

		final StringBuilder buf = new StringBuilder();

		for (int i = 0; i < tokens.length; i++) {

			if (i > 0) {

				buf.append(' ');
			}

			buf.append(variables.contains(i) ? "*" : tokens[i]);
		}

		return buf.toString();
	}

	static void collectErrorTokens(final String clean) {

		final Matcher tokens = ERROR_TOKENS.matcher(clean);
		if (tokens.find()) {

			// the token is the actionable part, so it leads; an optional code between the two
			// (ERROR3, NOT_AN_INTEGER) is skipped
			count(errorTokens, pad(tokens.group(2), 30) + tokens.group(1), lineCount, clean);
		}
	}

	static void collectSurefire(final String clean) {

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

			// Every module prints its own summary, so these are ADDED UP. Overwriting them reported
			// whichever module happened to run last -- "3 tests" for a reactor that ran fifteen
			// hundred, which is exactly the kind of falsely reassuring number this tool exists to
			// stop people from believing.
			totalTests    += Integer.parseInt(totals.group(1));
			totalFailures += Integer.parseInt(totals.group(2));
			totalErrors   += Integer.parseInt(totals.group(3));
			totalSkipped  += Integer.parseInt(totals.group(4));

			moduleSummaries++;
		}

		final Matcher goal = FAILING_GOAL.matcher(clean);
		if (goal.find() && failingGoal == null) {

			failingGoal = goal.group(1) + " on " + goal.group(2);
		}

		// RetryAnalyzer's wording. Nothing groups on it -- a consistently failing test is worth
		// calling out, and if the wording changes the list is empty rather than wrong.
		if (clean.contains("attempts (retries exhausted)")) {

			final int at = clean.indexOf("Test ");
			if (at >= 0) {

				retryExhausted.add(clean.substring(at + 5).split("\\s+")[0]);
			}
		}

		if (clean.contains("BUILD SUCCESS")) {

			buildResult = "SUCCESS";
		}

		if (clean.contains("BUILD FAILURE")) {

			buildResult = "FAILURE";
		}
	}

	// ----- parsing and shaping -----

	static Parsed parse(final String clean) {

		final Matcher app = APP_LINE.matcher(clean);
		if (app.matches()) {

			return new Parsed("app", app.group(3), loggerName(app.group(4)), app.group(5), app.group(2), app.group(1));
		}

		final Matcher build = BUILD_LINE.matcher(clean);
		if (build.matches()) {

			// maven's own chatter is only interesting when it reports a problem
			if ("INFO".equals(build.group(1)) || build.group(2).isBlank() || build.group(2).startsWith("***")) {

				return null;
			}

			return new Parsed("maven", build.group(1), "[maven]", build.group(2), null, null);
		}

		final Matcher jvm = JVM_LINE.matcher(clean);
		if (jvm.matches()) {

			return new Parsed("jvm", jvm.group(1), "[jvm]", jvm.group(2), null, null);
		}

		return null;
	}

	static String[] tokenize(final String message) {

		return maskFormats(message).split("\\s+");
	}

	/**
	 * A line's shape: kind, token count, first token. Deliberately NOT the logger -- moving or
	 * renaming a logger must not change grouping, and identical shapes from different loggers still
	 * teach us where the variables sit.
	 */
	static String shapeOf(final Parsed parsed, final String[] tokens) {

		final StringBuilder key = new StringBuilder(parsed.kind()).append('|').append(tokens.length);

		// The first few tokens, not just one: bucketing on "Unable" alone collided every message
		// starting with that word, after which nearly every position looked variable and the
		// signature came out as "Unable to * * * * * * * * *" -- worse than no grouping at all.
		for (int i = 0; i < Math.min(3, tokens.length); i++) {

			key.append('|').append(tokens[i]);
		}

		return key.toString();
	}

	// ----- baseline -----

	static void writeBaseline(final Path target) throws IOException {

		final List<String> lines = new ArrayList<>();

		// Provenance, so that a stale or foreign baseline is visible at a glance and shows up in the diff
		// whenever somebody regenerates it. Regenerating is how noise becomes permanent, so it should be
		// an obvious, reviewable act rather than a quiet one. Deliberately no host name and no path: only
		// the file's name, because this file is committed and a full path names somebody's home directory.
		lines.add("# structr test-log baseline; written from a run considered clean");
		lines.add("# written    " + LocalDate.now());
		lines.add("# from       " + ((sourceName != null) ? sourceName : "<stdin>") + ", " + totalTests + " test(s) in " + moduleSummaries + " module(s), build " + buildResult);
		lines.add("# runtime    " + System.getProperty("java.vm.vendor", "?") + " " + System.getProperty("java.version", "?"));
		lines.add("# origin     " + (isContinuousIntegration() ? "CI" : "a developer machine (regenerate on CI: a laptop bakes in its JDK version and locale)"));
		lines.add("# kind<TAB>count<TAB>signature");

		appendBaseline(lines, "error", errors);
		appendBaseline(lines, "warning", warnings);
		appendBaseline(lines, "trace", loggedTraces);
		appendBaseline(lines, "phrase", phrases);

		Files.write(target, lines, StandardCharsets.UTF_8);

		System.out.println("Baseline written: " + target + " (" + (lines.size() - 2) + " signatures)");
	}

	static boolean isContinuousIntegration() {

		for (final String name : List.of("CI", "GITLAB_CI", "GITHUB_ACTIONS", "JENKINS_URL", "BUILD_NUMBER")) {

			if (System.getenv(name) != null) {

				return true;
			}
		}

		return false;
	}

	static void appendBaseline(final List<String> lines, final String kind, final Map<String, Stat> collected) {

		for (final Map.Entry<String, Stat> entry : collected.entrySet()) {

			lines.add(kind + "\t" + entry.getValue().count + "\t" + flatten(entry.getKey()));
		}
	}

	static Diff compareToBaseline(final Path source) throws IOException {

		final Map<String, Integer> known = new LinkedHashMap<>();

		for (final String line : Files.readAllLines(source, StandardCharsets.UTF_8)) {

			if (line.startsWith("#") || line.isBlank()) {

				continue;
			}

			final String[] parts = line.split("\t", 3);
			if (parts.length == 3) {

				known.put(parts[0] + "\t" + parts[2], Integer.parseInt(parts[1].trim()));
			}
		}

		final Diff diff                 = new Diff();
		final Map<String, Stat> current = new LinkedHashMap<>();

		collectForDiff(current, "error", errors);
		collectForDiff(current, "warning", warnings);
		collectForDiff(current, "trace", loggedTraces);
		collectForDiff(current, "phrase", phrases);

		for (final Map.Entry<String, Stat> entry : current.entrySet()) {

			final Integer before = known.get(entry.getKey());
			if (before == null) {

				diff.added.put(entry.getKey(), entry.getValue());

			} else if (entry.getValue().count > before * 2 && entry.getValue().count - before > 5) {

				diff.grew.put(entry.getKey(), before + " -> " + entry.getValue().count);
			}
		}

		for (final String key : known.keySet()) {

			if (!current.containsKey(key)) {

				diff.gone.add(key);
			}
		}

		return diff;
	}

	static void collectForDiff(final Map<String, Stat> target, final String kind, final Map<String, Stat> collected) {

		for (final Map.Entry<String, Stat> entry : collected.entrySet()) {

			target.put(kind + "\t" + flatten(entry.getKey()), entry.getValue());
		}
	}

	// ----- reporting -----

	static final String ORANGE = "\u001B[38;5;173m";
	static final String DIM    = "\u001B[2m";
	static final String RESET  = "\u001B[0m";

	static String label() {

		return color ? ORANGE + "[test-log]" + RESET : "[test-log]";
	}

	/**
	 * The few lines every build prints. Deliberately a handful: a report nobody can skim is a report
	 * nobody reads, and the counts are pointers into the full output rather than a verdict.
	 */
	static void reportBriefly(final Diff diff) {

		final int passedWhileLogging = passedWhileLogging().size();
		final int calibration        = levelCalibration().size();
		final int unowned            = unownedOutput().size();

		System.err.println();
		System.err.printf(Locale.ROOT, "%s %d test(s) in %d class(es), %d failure(s), %d error(s)%n", label(), totalTests, reportedClasses, totalFailures, totalErrors);
		System.err.printf(Locale.ROOT, "%s %d error, %d warning, %d swallowed-exception signature(s)%s%n",
			label(), errors.size(), warnings.size(), loggedTraces.size(),
			(diff != null) ? "; " + diff.added.size() + " new, " + diff.grew.size() + " grown vs baseline" : "");

		if (passedWhileLogging > 0 || calibration > 0 || unowned > 0) {

			System.err.printf(Locale.ROOT, "%s %d exception(s) logged by a PASSING test, %d ERROR signature(s) only in passing tests, %d unowned%n",
				label(), passedWhileLogging, calibration, unowned);
		}

		System.err.printf(Locale.ROOT, "%s %sdetails: java config/testlog/TestLogReview.java --reports .%s%n", label(), color ? DIM : "", color ? RESET : "");
		System.err.println();
	}

	static void report(final Path file, final Diff diff) {

		System.out.println();
		System.out.println("Structr test-log review: " + (file != null ? file : "<stdin>") + "  (" + lineCount + " lines)");

		if (show("summary")) {

			System.out.println();
			System.out.printf(Locale.ROOT, "  build            %s%n", buildResult);

			if (moduleSummaries > 0) {

				System.out.printf(Locale.ROOT, "  tests            %d run, %d failure(s), %d error(s), %d skipped (summed over %d module(s))%n",
					totalTests, totalFailures, totalErrors, totalSkipped, moduleSummaries);
			}

			if (failingGoal != null) {

				System.out.printf(Locale.ROOT, "  failing goal     %s%n", failingGoal);
			}

			System.out.printf(Locale.ROOT, "  measured         %s%n", learn
				? learnedShapes + " of " + shapeCounts.size() + " line shape(s) have variable positions"
				: "off (stdin or --no-learn): grouping by format only");
			System.out.printf(Locale.ROOT, "  distinct         %d error(s), %d warning(s), %d logged exception(s)%n", errors.size(), warnings.size(), loggedTraces.size());

			// A log with no application lines at all is almost never an empty log. Writing this tool
			// produced the proof: one invalid character in the test logback.xml turned off ALL test
			// logging, the suite still passed, and the report looked immaculate -- nothing logged, nothing
			// wrong. Silence must not read as a clean run.
			if (appLines == 0 && lineCount > 0) {

				System.out.printf(Locale.ROOT, "  WARNING          no application log line recognised in %d line(s): logging is off, misconfigured, or the pattern changed%n", lineCount);
			}

			// Say plainly whether the log can name the test behind a line. If it cannot, three sections
			// are missing from this report, and it should be obvious why rather than look like a clean run.
			if (attributedLines > 0) {

				System.out.printf(Locale.ROOT, "  attribution      %d line(s) name their test, %d verdict(s) recorded%n", attributedLines, testStatus.size());

			} else {

				System.out.printf(Locale.ROOT, "  attribution      none: no {test} field in this log, so the per-test sections are omitted%n");
			}
		}

		if (diff != null && show("diff")) {

			System.out.println();
			System.out.println("AGAINST BASELINE " + baseline + (usingDefaultBaseline ? " (committed default)" : "") + ": "
				+ diff.added.size() + " new, " + diff.grew.size() + " grown, "
				+ diff.gone.size() + " gone (\"gone\" means little across runs of different scope)");

			for (final Map.Entry<String, Stat> entry : limit(diff.added.entrySet())) {

				System.out.printf(Locale.ROOT, "  NEW    %6dx  L%-7d %s%n", entry.getValue().count, entry.getValue().firstLine, entry.getKey().replace("\t", "  "));
			}

			for (final Map.Entry<String, String> entry : limitStrings(diff.grew.entrySet())) {

				System.out.printf(Locale.ROOT, "  GREW   %13s  %s%n", entry.getValue(), entry.getKey().replace("\t", "  "));
			}
		}

		if (show("failures") && !failingTests.isEmpty()) {

			System.out.println();
			System.out.println("REPORTED TEST FAILURES (" + failingTests.size() + ")");

			for (final String[] failure : failingTests) {

				System.out.printf(Locale.ROOT, "  %-8s %s%n", failure[1], failure[0]);
			}
		}

		if (show("failures") && !retryExhausted.isEmpty()) {

			System.out.println();
			System.out.println("CONSISTENTLY FAILING (retries exhausted -- not flaky)");

			for (final String test : new LinkedHashSet<>(retryExhausted)) {

				System.out.println("  " + test);
			}
		}

		// The sections that need ownership come first: they are verdicts rather than counts. Without a
		// {test} field in the log they would all be empty, so they are skipped entirely and the report
		// looks exactly as it did before attribution existed.
		if (attributedLines > 0) {

			section("EXCEPTIONS LOGGED BY A TEST THAT THEN PASSED (nobody asserted on these)", passedWhileLogging(), "passed");
			section("ERROR LEVEL SEEN ONLY IN PASSING TESTS (wrong level, or a missing assertion)", levelCalibration(), "calibration");
			section("OUTPUT NO TEST OWNS (background threads, teardown, shutdown hooks)", unownedOutput(), "unowned");
		}

		section("LOGGED / SWALLOWED EXCEPTIONS (no test reported them)", loggedTraces, "traces");
		section("VALIDATION ERROR TOKENS (token, then type.property)", errorTokens, "tokens");
		section("ERRORS", errors, "errors");
		section("WARNINGS", warnings, "warnings");
		section("OTHER PROBLEM PHRASINGS (INFO/DEBUG level)", phrases, "phrases");

		if (show("traces") && !testTraces.isEmpty()) {

			section("EXCEPTIONS FROM REPORTED FAILURES (expected if tests are red)", testTraces, "traces");
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
		System.out.println("Rarest first, then the loudest: a single occurrence is often the real defect, while the");
		System.out.println("high counts are usually noise somebody already knows about. * marks a position the log");
		System.out.println("itself showed to be variable. A count is not a verdict: high");
		System.out.println("counts are often deliberate negative tests (--ignore REGEX), and a single occurrence can be");
		System.out.println("the real defect. Line numbers refer to the log.");

		if (baseline == null) {

			System.out.println("Tip: --write-baseline " + DEFAULT_BASELINE + " once a log is clean; it is then used automatically.");
		}

		System.out.println();
	}

	/**
	 * Exceptions logged by a test that then passed. The strongest signal in the report, and the reason
	 * ownership is worth carrying on every line: the test says the code works, the log says something
	 * went wrong on the way, and nobody looked. A count cannot express that -- it needs the verdict.
	 */
	static Map<String, Stat> passedWhileLogging() {

		final Map<String, Stat> result = new LinkedHashMap<>();

		for (final Map.Entry<String, Stat> entry : loggedTraces.entrySet()) {

			for (final String owner : entry.getValue().owners) {

				if ("SUCCESS".equals(testStatus.get(owner))) {

					// the qualified name, because this is the section where somebody goes and opens the class
					result.put(qualifiedNames.getOrDefault(owner, owner) + "\n      logged: " + entry.getKey(), entry.getValue());
				}
			}
		}

		return result;
	}

	/**
	 * ERROR-level messages whose every occurrence came from a test that passed. By construction nobody
	 * acts on them, so each is a candidate for a lower level or for a negative test that asserts on it
	 * instead. Acting on this list shrinks every future log -- the one section working towards its own
	 * obsolescence. Anything that also occurred outside a test is left out; it has not been shown to be
	 * harmless.
	 */
	static Map<String, Stat> levelCalibration() {

		final Map<String, Stat> result = new LinkedHashMap<>();

		for (final Map.Entry<String, Stat> entry : errors.entrySet()) {

			final Stat stat = entry.getValue();
			if (stat.owners.isEmpty() || stat.unowned > 0) {

				continue;
			}

			boolean allPassed = true;

			for (final String owner : stat.owners) {

				allPassed &= "SUCCESS".equals(testStatus.get(owner));
			}

			if (allPassed) {

				result.put(entry.getKey() + "   [only in " + stat.owners.size() + " passing test(s)]", stat);
			}
		}

		return result;
	}

	/**
	 * Output from threads no test owns: pools, agents, shutdown hooks, anything still running after the
	 * test that started it finished. Legitimate background activity lands here too, so this is a list to
	 * read rather than a list of defects -- but teardown bugs and leaks surface nowhere else, and a
	 * per-test capture would have charged them to whichever test happened to be open at the time.
	 */
	static Map<String, Stat> unownedOutput() {

		final Map<String, Stat> result = new LinkedHashMap<>();

		for (final Map<String, Stat> source : List.of(loggedTraces, errors, warnings)) {

			for (final Map.Entry<String, Stat> entry : source.entrySet()) {

				if (entry.getValue().owners.isEmpty() && entry.getValue().unowned > 0) {

					// The thread is the first thing to look at here: it separates a Jetty worker handling a
					// test's own HTTP request (unowned only because the MDC does not cross that boundary)
					// from a background job or a shutdown hook, which is what this section is really for.
					result.put(pad(threadsOf(entry.getValue()), 20) + entry.getKey(), entry.getValue());
				}
			}
		}

		return result;
	}

	/** At most two thread kinds, so one row stays one row. */
	static String threadsOf(final Stat stat) {

		final List<String> threads = new ArrayList<>(stat.threads);
		if (threads.isEmpty()) {

			return "[?]";
		}

		if (threads.size() <= 2) {

			return "[" + String.join(",", threads) + "]";
		}

		return "[" + threads.get(0) + " +" + (threads.size() - 1) + "]";
	}

	static void section(final String title, final Map<String, Stat> collected, final String name) {

		if (!show(name) || collected.isEmpty()) {

			return;
		}

		final List<Map.Entry<String, Stat>> rows = new ArrayList<>();

		for (final Map.Entry<String, Stat> row : collected.entrySet()) {

			if (row.getValue().count >= minCount) {

				rows.add(row);
			}
		}

		if (rows.isEmpty()) {

			return;
		}

		// Rarest first. Sorting by count descending and truncating hides precisely the messages that
		// this tool's own advice says to look at -- the one-offs. The loudest lines, by contrast, are
		// usually long-known noise. Both ends are worth seeing, so both ends are printed.
		rows.sort(Comparator.comparingInt(row -> row.getValue().count));

		System.out.println();
		System.out.println(title + " (" + collected.size() + " distinct)");

		if (rows.size() <= top * 2) {

			print(rows);

			return;
		}

		print(rows.subList(0, top));

		System.out.printf(Locale.ROOT, "  %s%n", "... " + (rows.size() - top * 2) + " more between these two ends ...");

		print(rows.subList(rows.size() - top, rows.size()));
	}

	static void print(final List<Map.Entry<String, Stat>> rows) {

		for (final Map.Entry<String, Stat> row : rows) {

			System.out.printf(Locale.ROOT, "  %6dx  L%-7d %s%n", row.getValue().count, row.getValue().firstLine, row.getKey());
		}
	}

	// ----- helpers -----

	/** New signatures, rarest first and capped at both ends, for the same reason as {@link #section}. */
	static List<Map.Entry<String, Stat>> limit(final Set<Map.Entry<String, Stat>> entries) {

		final List<Map.Entry<String, Stat>> list = new ArrayList<>(entries);

		list.sort(Comparator.comparingInt(row -> row.getValue().count));

		if (list.size() <= top * 2) {

			return list;
		}

		final List<Map.Entry<String, Stat>> ends = new ArrayList<>(list.subList(0, top));

		ends.addAll(list.subList(list.size() - top, list.size()));

		return ends;
	}

	static List<Map.Entry<String, String>> limitStrings(final Set<Map.Entry<String, String>> entries) {

		final List<Map.Entry<String, String>> list = new ArrayList<>(entries);

		return list.subList(0, Math.min(top, list.size()));
	}

	static String flatten(final String key) {

		return key.replace('\t', ' ').replace('\n', ' ');
	}

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

	static Stat count(final Map<String, Stat> collected, final String key, final long line, final String example) {

		final Stat stat = collected.computeIfAbsent(key, k -> new Stat(line, example));

		stat.count++;

		return stat;
	}

	/**
	 * Only application lines and the traces that follow them are attributed: Maven's and the JVM's own
	 * output belongs to no test by nature, and counting it as unowned would bury the cases where being
	 * unowned actually means something.
	 */
	static void attribute(final Stat stat, final Parsed parsed) {

		if ("app".equals(parsed.kind())) {

			attribute(stat, parsed.test());
		}
	}

	/** Record who produced this occurrence, or that nobody did. */
	static void attribute(final Stat stat, final String owner) {

		if (owner != null) {

			stat.owners.add(owner);

		} else {

			stat.unowned++;

			if (currentThread != null) {

				stat.threads.add(currentThread);
			}
		}
	}

	/**
	 * A thread's KIND, with its numbers masked: "qtp1863361500-25" becomes "qtp*-*", "pool-2-thread-1"
	 * becomes "pool-*-thread-*". Without that, one Jetty worker per request would be one signature per
	 * request, and the section that exists to explain unowned output would be the noisiest of all.
	 */
	static String threadClass(final String thread) {

		if (thread == null) {

			return null;
		}

		// ids before digits: masking digits first shreds a hex id into "*f*f*eb*fca*ab*d*e*"
		String result = UUID_DASH.matcher(thread).replaceAll("<id>");

		result = HEX_ID.matcher(result).replaceAll("<id>");

		return result.replaceAll("\\d+", "*");
	}

	/**
	 * Remove ANSI colour codes; Maven colourises its output.
	 *
	 * <p>{@code \\u001B} is written as an escape and never as a literal ESC byte: a rewrite of this
	 * file once dropped the invisible control character, leaving "any [ followed by a letter", which
	 * ate the {@code [main]} thread field of every line. No application line matched, and the tool
	 * cheerfully reported a log containing 112 errors as having none.</p>
	 */
	static String strip(final String line) {

		return line.replaceAll("\\u001B\\[[0-9;]*[A-Za-z]", "").stripTrailing();
	}

	/**
	 * Loggers are reported in full. An earlier version kept only the part after the last dot, which
	 * turned a testcontainers logger into the signature "INFO 0" -- the discarded part was the whole
	 * point. Nothing here shortens a line: a log line is evidence, and the tool has no business
	 * deciding which half of it matters.
	 */
	static String loggerName(final String logger) {

		return logger;
	}

	/** Pad for alignment, but never truncate -- an over-long logger name costs alignment, not content. */
	static String pad(final String value, final int width) {

		return (value.length() >= width) ? value + " " : value + " ".repeat(width - value.length());
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
		System.out.println("  --baseline FILE        report only what is NEW or has grown since that baseline");
		System.out.println("                         (default: " + DEFAULT_BASELINE + " when present)");
		System.out.println("  --write-baseline FILE  record this log's signatures as the accepted state");
		System.out.println("  --fail-on-new          exit 1 when new signatures appeared (for CI)");
		System.out.println("  --top N                rows per END of a section: N rarest and N loudest (default 8)");
		System.out.println("  --min-count N          hide signatures seen fewer than N times (default 1)");
		System.out.println("  --section NAME         summary|failures|errors|warnings|traces|phrases|tokens|slow|diff|all");
		System.out.println("                         passed|calibration|unowned  (need a log with the {test} field)");
		System.out.println("  --ignore REGEX         drop matching lines; repeatable, for known-expected noise");
		System.out.println("  --reports DIR          read the captured output of target/*-reports/TEST-*.xml below DIR");
		System.out.println("                         instead of a piped log; needs no tee, so it can run on every build");
		System.out.println("  --brief                a few lines only, for build output");
		System.out.println("  --color always|never   colourise the [test-log] label");
		System.out.println("  --no-learn             group by format only, do not measure variable positions");
		System.out.println("  --help, -h             show this help");
		System.out.println("  logfile                the Maven/TestNG log; reads stdin when omitted (no measuring)");
		System.out.println();
		System.out.println("produce a log with:  mvn ... 2>&1 | tee /tmp/full-run.log");
		System.exit(err == null ? 0 : 2);
	}

	/** One log line, split into the parts the formats give us. {@code test} is null when unattributed. */
	record Parsed(String kind, String level, String logger, String message, String test, String thread) {
	}

	static final class Stat {

		final long firstLine;
		final String example;
		int count = 0;

		/** The tests that produced this signature, and how often nobody did. */
		final Set<String> owners = new LinkedHashSet<>();
		int unowned = 0;

		/** For the unowned occurrences: which kind of thread wrote them. */
		final Set<String> threads = new LinkedHashSet<>();

		Stat(final long firstLine, final String example) {

			this.firstLine = firstLine;
			this.example   = example;
		}
	}

	static final class Diff {

		final Map<String, Stat> added  = new LinkedHashMap<>();
		final Map<String, String> grew = new LinkedHashMap<>();
		final List<String> gone        = new ArrayList<>();
	}
}
