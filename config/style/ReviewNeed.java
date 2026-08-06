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
 * Review-need triage scorer for Java files. A heuristic prioritiser (NOT a quality verdict):
 * it ranks files by how much they are likely to reward a human review, from signals drawn from
 * established metrics (cyclomatic/cognitive complexity, Maintainability Index, PMD/SonarQube
 * smells) plus Structr-specific smells. Regex + brace-depth heuristics, no parser — expect some
 * false positives; treat scores as a "look here first" ordering.
 *
 * Single-file program; runs with the JDK source launcher (no build, no dependencies, no Python):
 *     java config/style/ReviewNeed.java [--top N] [--by SIGNAL] [--main-only] [--summary] [paths...]
 *
 * With no path it scans the current directory. The Maven build invokes it with --summary
 * --main-only over the repo (non-failing; see the root pom).
 *
 * Accepting a flagged file ("I reviewed it, it's fine"): put the marker  @review-need:accept
 * anywhere in the file, in any comment style, with your reason written next to it. The tool only
 * checks that the marker string is present -- it never reads the reason -- and drops the file from
 * the ranking. Remove the marker to un-accept. List the accepted files with --show-accepted.
 */
public class ReviewNeed {

	// @review-need:accept -- this analyzer is regex-heavy, prints its results to stdout, and wraps
	// everything in a broad catch BY DESIGN (it is a regex scanner + a CLI that must never fail the
	// build). Those signals are its nature, not defects to refactor; see the class documentation.

	// Built by concatenation so the tool does not match its own detection code -- only an intentional
	// marker (like the one above) accepts a file.
	static final String ACCEPT_MARKER = "@review-need" + ":accept";

	static final int LONG_METHOD = 60, CX = 12, DEEP = 4;

	static final Map<String, Integer> W = new LinkedHashMap<>();
	static {
		// the smells the team called out first, weighted highest
		W.put("long_methods", 3); W.put("complexity", 3); W.put("deep_nesting", 2);
		W.put("cstyle_for", 2); W.put("switch_heavy", 2); W.put("str_const_enum", 4);
		W.put("parser_smell", 3); W.put("oversized", 1); W.put("todos", 1);
		// the more exotic signals
		W.put("magic_numbers", 1); W.put("long_params", 2); W.put("boolean_params", 2);
		W.put("equals_literal", 2); W.put("instanceof_heavy", 2); W.put("broad_catch", 2);
		W.put("reflection", 2); W.put("concurrency", 3); W.put("sysout", 2); W.put("regex_heavy", 2);
	}

	static final Pattern DECISION      = Pattern.compile("\\b(if|for|while|case|catch)\\b|&&|\\|\\||\\?");
	static final Pattern CSTYLE_FOR    = Pattern.compile("\\bfor\\s*\\(\\s*(final\\s+)?(int|long|short|byte|Integer|Long)\\s+\\w+\\s*=");
	static final Pattern STR_CONST     = Pattern.compile("\\b(private|public|protected)?\\s*static\\s+final\\s+String\\s+\\w+\\s*=\\s*\"");
	static final Pattern PARSER        = Pattern.compile("\\.charAt\\(|\\.substring\\(|\\.indexOf\\(|\\.lastIndexOf\\(|\\.split\\(|\\.toCharArray\\(|StringBuilder|StringTokenizer|Character\\.is");
	static final Pattern METHOD_SIG    = Pattern.compile("^\\s*(?!(if|for|while|switch|catch|synchronized|return|new|else)\\b)[\\w@].*\\)\\s*(throws [\\w., ]+)?\\{$");
	static final Pattern TYPE_DECL     = Pattern.compile("\\b(class|interface|enum|record)\\b");
	static final Pattern TODO          = Pattern.compile("\\b(TODO|FIXME|XXX|HACK)\\b");
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
			final boolean isMethod = METHOD_SIG.matcher(line).find() && !TYPE_DECL.matcher(line).find()
				&& !line.contains("->") && !line.contains(" new ") && !line.contains("=");
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
		final Map<String, Integer> sub = new LinkedHashMap<>();
		sub.put("long_methods", longM);
		sub.put("complexity", cx);
		sub.put("deep_nesting", deep);
		sub.put("cstyle_for", count(CSTYLE_FOR, joined));
		sub.put("switch_heavy", count(SWITCH, joined));
		sub.put("str_const_enum", clusters);
		sub.put("parser_smell", Math.round(parser / Math.max(1f, loc / 100f)));
		sub.put("oversized", Math.min(20, Math.max(0, numMethods - 30) / 4 + Math.max(0, (loc - 800) / 300)));
		sub.put("todos", count(TODO, content));
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

	static String rel(final Path base, final Path p) {

		try {

			return base.relativize(p.toAbsolutePath()).toString();
		} catch (Exception e) {
			return p.toString();
		}
	}

	public static void main(final String[] a) {

		int top = 25;
		String by = null;
		boolean summary = false, mainOnly = false, showAccepted = false;
		final List<String> paths = new ArrayList<>();
		for (int i = 0; i < a.length; i++) {

			switch (a[i]) {
				case "--top"           -> top = Integer.parseInt(a[++i]);
				case "--by"            -> by = a[++i];
				case "--summary"       -> summary = true;
				case "--main-only"     -> mainOnly = true;
				case "--show-accepted" -> showAccepted = true;
				default                -> paths.add(a[i]);
			}
		}
		final boolean mo = mainOnly;
		if (paths.isEmpty()) {

			paths.add(".");
		}
		if (by != null && !W.containsKey(by)) {

			System.err.println("--by must be one of: " + W.keySet());
			System.exit(2);
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
			int acceptedCount = 0;
			final List<Row> visible = new ArrayList<>();
			for (Row r : all) {

				if (r.accepted()) {

					acceptedCount++;
					if (showAccepted) {

						visible.add(r);
					}
				} else {
					visible.add(r);
				}
			}
			final String acc = acceptedCount > 0 ? acceptedCount + " accepted, " : "";

			if (summary) {

				if (visible.isEmpty()) {

					System.out.println("[review-need] " + all.size() + " files scored (" + acc + "0 to review).");
					return;
				}
				final int n = Math.min(10, visible.size());
				System.out.println("[review-need] " + all.size() + " files scored (" + acc + "heuristic). Top " + n + " needing review:");
				for (int i = 0; i < n; i++) {

					final Row r = visible.get(i);
					final String tag = r.accepted() ? "  [accepted]" : "";
					System.out.printf("[review-need]  %s  (score %d)%s  %s%n", rel(base, r.path()), r.score(), tag, signals(r.sub()));
				}
				return;
			}

			if (visible.isEmpty()) {

				System.out.println("Nothing to review (" + acc + all.size() + " scored).");
				return;
			}
			System.out.printf("%6s %6s  %s%n", "score", "loc", by != null ? "path, flags   (ranked by " + by + ")" : "path, flags");
			System.out.println("-".repeat(80));
			for (int i = 0; i < Math.min(top, visible.size()); i++) {

				final Row row = visible.get(i);
				if (by != null && row.sub().get(by) == 0) {

					continue;
				}
				final String tag = row.accepted() ? " [accepted]" : "";
				System.out.printf("%6d %6d  %s%s  %s%n", row.score(), row.loc(), rel(base, row.path()), tag, signals(row.sub()));
			}
			System.out.println("\n" + visible.size() + " file(s) to review" + (acceptedCount > 0 ? " (" + acceptedCount + " accepted, hidden)" : "") + ". Heuristic.");

		} catch (Throwable t) {
			System.out.println("[review-need] skipped (" + t.getClass().getSimpleName() + ": " + t.getMessage() + ")");
		}
	}
}
