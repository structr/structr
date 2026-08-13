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

		W.put("fixmes",           200);
		W.put("sysout",           10);
		W.put("cstyle_for",       5);
		W.put("str_const_enum",   4);
		W.put("parser_smell",     4);
		W.put("regex_heavy",      4);
		W.put("equals_literal",   3);
		W.put("switch_heavy",     2);
		W.put("oversized",        1);
		W.put("long_params",      1);
		W.put("long_methods",     1);
		W.put("concurrency",      1);
		W.put("boolean_params",   1);
		W.put("instanceof_heavy", 1);
		W.put("broad_catch",      1);
		W.put("reflection",       1);
		W.put("long_lines",       1);
		W.put("magic_numbers",    1);
		W.put("deep_nesting",     1);
		W.put("complexity",       1);
	}

	// short human-readable descriptions, printed as a legend under the ranked list
	static final Map<String, String> DESC = new LinkedHashMap<>();
	static {
		DESC.put("long_methods", "method body longer than " + LONG_METHOD + " lines");
		DESC.put("complexity", "high cyclomatic complexity — many branches, loops and conditions");
		DESC.put("deep_nesting", "blocks nested deeper than " + DEEP + " levels");
		DESC.put("cstyle_for", "C-style indexed for-loop (often replaceable by for-each or a stream)");
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
		DESC.put("regex_heavy", "heavy regex use (Pattern.compile / matches / replaceAll)");
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
	static final Pattern REGEX_HEAVY   = Pattern.compile("Pattern\\.compile|\\.matches\\(|\\.replaceAll\\(");
	static final Pattern BOOLEAN_PARAM = Pattern.compile("\\bboolean\\b");
	static final Pattern COMMA         = Pattern.compile(",");
	static final Pattern OPEN_BRACE    = Pattern.compile("\\{");
	static final Pattern CLOSE_BRACE   = Pattern.compile("\\}");

	// signals locatable by a single per-line pattern (used by --detail); the rest need special logic
	static final Map<String, Pattern> LINE_SIG = new LinkedHashMap<>();
	static {
		LINE_SIG.put("cstyle_for", CSTYLE_FOR);
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

	/** Strip // and block comments and blank string/char literals so tokens aren't miscounted. */
	static List<String> toCode(final List<String> raw) {

		final List<String> out = new ArrayList<>();
		boolean inBlock = false;

		for (String line : raw) {

			if (inBlock) {

				final int end = line.indexOf("*/");
				if (end < 0) {

					out.add(""); continue;
				}

				line = line.substring(end + 2); inBlock = false;
			}

			while (true) {

				final int start = line.indexOf("/*");
				if (start < 0) {

					break;
				}

				final int end = line.indexOf("*/", start + 2);
				if (end < 0) {

					line = line.substring(0, start); inBlock = true; break;
				}

				line = line.substring(0, start) + " " + line.substring(end + 2);
			}

			line = STRING_LIT.matcher(line).replaceAll("\"\"");
			line = CHAR_LIT.matcher(line).replaceAll("''");
			final int c = line.indexOf("//");
			if (c >= 0) {

				line = line.substring(0, c);
			}

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
		sub.put("cstyle_for", count(CSTYLE_FOR, joined));
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
		sub.put("regex_heavy", count(REGEX_HEAVY, joined) / 2);
		sub.put("long_lines", longLines);

		int score = 0;

		for (Map.Entry<String, Integer> e : sub.entrySet()) {

			score += e.getValue() * W.get(e.getKey());
		}

		return new Result(loc, score, sub, accepted);
	}

	/** All non-zero flags, most-contributing first, as {@code name=count} (the score is their weighted sum). */
	static String signals(final Map<String, Integer> sub) {

		final List<Map.Entry<String, Integer>> contrib = new ArrayList<>(sub.entrySet());

		contrib.removeIf(e -> e.getValue() == 0);
		contrib.sort((p, q) -> q.getValue() * W.get(q.getKey()) - p.getValue() * W.get(p.getKey()));

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

				if (e.getValue() > 0) {

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

		for (final String k : W.keySet()) {

			hits.put(k, new ArrayList<>());
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

				if (find(e.getValue(), c)) {

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
		System.out.println("  --min SCORE      omit files scoring below SCORE (default 1; score 0 = no findings)");
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
				case "--by"            -> { by = argValue(a, i, arg); i++; }
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
				if (metric <= 0) {

					noFindings++;
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
