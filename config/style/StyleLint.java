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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Structr semantic blank-line linter. Enforces the house-style blank-line rules that no
 * off-the-shelf formatter can express (see CODE_STYLE.md). Single-file program; runs with the
 * JDK source launcher (no build, no dependencies, no Python — works on Windows too):
 *
 *     java config/style/StyleLint.java --check <paths...>    report violations, exit 1 if any
 *     java config/style/StyleLint.java --fix   <paths...>    apply the fixes in place (converges)
 *
 * Rules — R1–R8 only insert/delete BLANK lines (behaviour-safe); R9 also joins a wrapped
 * continuation back onto one line, a whitespace-only merge guarded against comments, annotations
 * and text blocks, so it too preserves tokens and behaviour:
 *   R1  method/type body -> blank line after '{' (every body, incl. one-liners)   (insert)
 *   R4  every control block (if/for/while/try/…) -> blank after '{'               (insert)
 *   R5  a return preceded by a statement -> blank line before it                  (insert)
 *   R6  a control block preceded by a statement -> blank line before it           (insert;
 *       skipped for a bound guard and when the previous line is a comment or brace)
 *   R7  control block -> blank line after its close, and before else/catch/finally  (insert)
 *   R8  a run of 2+ consecutive assignments -> blank line after the group          (insert)
 *   R9  a wrapped &&/||/arg-list continuation -> joined if the result fits in 200   (join)
 *   R3  guard testing the variable assigned directly above -> no blank before it  (remove;
 *       "guard" = an if whose condition names that variable — null/empty OR numeric)
 *   R2  consecutive single-line assignments grouped, no blank between             (remove)
 *   RB  never more than one consecutive blank line                                (collapse)
 *
 * @code-quality:accept
 */
public class StyleLint {

	static final Pattern CTRL_OPEN  = Pattern.compile("^\\s*(\\}\\s*)?(else if|else|if|for|while|catch|synchronized|try|do|finally)\\b");
	static final Pattern TYPE_OPEN  = Pattern.compile("\\b(class|interface|enum|record)\\b");
	static final Pattern ASSIGN     = Pattern.compile("^\\s*(final\\s+)?[\\w.$]+(\\s*<[^;=]*>)?(\\[\\])?\\s+\\w+\\s*=[^=]");
	static final Pattern EXIT       = Pattern.compile("^\\s*(return|continue|break|throw)\\b");
	static final Pattern SWITCH     = Pattern.compile("\\bswitch\\s*\\(");
	static final Pattern CTRL_LEAD  = Pattern.compile("^\\s*(if|for|while|try|do|synchronized)\\b");
	static final Pattern IF_LEAD    = Pattern.compile("^\\s*if\\b");
	static final Pattern CONT       = Pattern.compile("^\\s*\\}\\s*(else|catch|finally)\\b");
	static final Pattern COMMENT    = Pattern.compile("^\\s*(//|/?\\*)");
	static final int     MAX_WIDTH  = 200;

	record Rep(int idx, String msg) {}

	static boolean find(final Pattern p, final String s) {

		return p.matcher(s).find();
	}

	static String rstrip(final String s) {

		int e = s.length();

		while (e > 0 && Character.isWhitespace(s.charAt(e - 1))) {

			e--;
		}

		return s.substring(0, e);
	}

	static int countChar(final String s, final char c) {

		int n = 0;

		for (int i = 0; i < s.length(); i++) {

			if (s.charAt(i) == c) {

				n++;
			}
		}

		return n;
	}

	static boolean blank(final List<String> L, final int i) {

		return i >= 0 && i < L.size() && L.get(i).strip().isEmpty();
	}

	/** First line after the assignment statement starting at line g (skips over line-broken tails). */
	static int assignEnd(final List<String> L, final int g) {

		int e = g;

		while (e < L.size() && !rstrip(L.get(e)).endsWith(";")) {

			e++;
		}

		return e + 1;
	}

	/** The variable a single-line declaration/assignment defines (the identifier left of '='), or null. */
	static String assignedVar(final String line) {

		if (!find(ASSIGN, line)) {

			return null;
		}

		final String lhs = line.substring(0, line.indexOf('=')).strip();
		final int sp = Math.max(lhs.lastIndexOf(' '), lhs.lastIndexOf('\t'));

		return sp < 0 ? lhs : lhs.substring(sp + 1);
	}

	/**
	 * True if line i is a guard bound to the assignment directly above it: an {@code if} whose
	 * condition names the variable assigned on the previous line (ignoring one optional blank),
	 * where that assignment is not part of a group of two or more (rule 2 wins for groups). This
	 * covers null/empty AND numeric-sentinel checks alike ({@code indexOf(...) < 0}, {@code == -1},
	 * …) — a bound guard takes no blank line before it, and none is inserted.
	 */
	static boolean boundGuard(final List<String> L, final int i) {

		if (!find(IF_LEAD, L.get(i))) {

			return false;
		}

		int a = i - 1;
		if (a >= 0 && L.get(a).strip().isEmpty()) {

			a--;
		}

		final String var = a >= 0 ? assignedVar(rstrip(L.get(a))) : null;
		if (var == null || (a >= 1 && find(ASSIGN, L.get(a - 1)))) {

			return false;
		}

		return find(Pattern.compile("\\b" + Pattern.quote(var) + "\\b"), L.get(i));
	}

	/** Non-blank body lines inside the block opened on line i (brace-matched on the raw text). */
	static int matchBody(final List<String> L, final int i) {

		final String s = rstrip(L.get(i));
		final String head = s.stripLeading().startsWith("}") ? s.stripLeading().substring(1) : s;
		int depth = countChar(head, '{') - countChar(head, '}');
		int j = i + 1, body = 0;

		while (j < L.size() && depth > 0) {

			depth += countChar(L.get(j), '{') - countChar(L.get(j), '}');

			if (depth > 0 && !L.get(j).strip().isEmpty()) {

				body++;
			}

			j++;
		}

		return body;
	}

	/** Index of the line whose '{' the standalone '}' on line i closes (brace-matched), or -1. */
	static int openerOf(final List<String> L, final int i) {

		int balance = 0;

		for (int j = i; j >= 0; j--) {

			final String s = rstrip(L.get(j));
			balance += countChar(s, '}') - countChar(s, '{');

			if (balance == 0) {

				return j;
			}
		}

		return -1;
	}

	/** Visual width of a line, counting each tab as 8 columns (matches the 200-column ceiling). */
	static int lineWidth(final String s) {

		int w = 0;

		for (int k = 0; k < s.length(); k++) {

			w += s.charAt(k) == '\t' ? 8 : 1;
		}

		return w;
	}

	/** A copy of the line with char and string literals blanked, so a // or /* inside a literal is
	 *  not mistaken for a real comment. */
	static String noLiterals(final String s) {

		final StringBuilder b = new StringBuilder(s.length());
		int k = 0;

		while (k < s.length()) {

			final char c = s.charAt(k++);
			b.append(c);

			if (c == '"' || c == '\'') {

				while (k < s.length() && s.charAt(k) != c) {

					k += s.charAt(k) == '\\' ? 2 : 1;
				}

				if (k < s.length()) {

					b.append(c);
					k++;
				}
			}
		}

		return b.toString();
	}

	/**
	 * Marks lines lying entirely inside a block comment (and single-line and asterisk comment lines),
	 * so no rule ever reaches into a comment — license headers and javadoc code examples stay immune.
	 * Literals are blanked first, so a comment-opener inside a string does not start a phantom comment.
	 */
	static boolean[] commentMask(final List<String> L) {

		final boolean[] mask = new boolean[L.size()];
		boolean inBlock = false, inText = false;

		for (int i = 0; i < L.size(); i++) {

			final boolean startMasked = inBlock || inText;
			final String line = L.get(i);
			boolean sawText = false;
			int k = 0;

			while (k < line.length()) {

				if (inText) {

					if (line.startsWith("\"\"\"", k)) {

						inText = false;
						sawText = true;
						k += 3;
						continue;
					}

					k++;
					continue;
				}

				if (inBlock) {

					if (line.startsWith("*/", k)) {

						inBlock = false;
						k += 2;
						continue;
					}

					k++;
					continue;
				}

				if (line.startsWith("\"\"\"", k)) {

					inText = true;
					sawText = true;
					k += 3;
					continue;
				}

				if (line.startsWith("//", k)) {

					break;
				}

				if (line.startsWith("/*", k)) {

					inBlock = true;
					k += 2;
					continue;
				}

				if (line.charAt(k) == '"' || line.charAt(k) == '\'') {

					final char q = line.charAt(k++);

					while (k < line.length() && line.charAt(k) != q) {

						k += line.charAt(k) == '\\' ? 2 : 1;
					}

					k++;
					continue;
				}

				k++;
			}

			final String t = line.stripLeading();
			mask[i] = startMasked || sawText || t.startsWith("*") || t.startsWith("/*") || t.startsWith("//");
		}

		return mask;
	}

	/**
	 * True if continuation line {@code next} may be joined onto {@code prev} — a whitespace-only
	 * merge of a wrapped boolean chain or argument list. Guarded hard: never across a statement or
	 * block boundary, a comment, an annotation, or a text block, so it preserves tokens (and thus
	 * behaviour). The length ceiling is enforced by the caller.
	 */
	static boolean canJoin(final String prev, final String next) {

		final String p = rstrip(prev);
		final String n = next.strip();

		if (p.isEmpty() || n.isEmpty() || p.endsWith(";") || p.endsWith("{") || p.endsWith("}")) {

			return false;
		}

		// text-block hazard: never join a line carrying a triple-quote delimiter
		if (p.contains("\"\"\"") || n.contains("\"\"\"")) {

			return false;
		}

		// comment hazard: test with string/char literals blanked, so a // or /* INSIDE a literal
		// (like the "//" this very method looks for) is not mistaken for a real comment
		final String pc = noLiterals(p);
		final String nc = noLiterals(n);

		if (pc.contains("//") || pc.contains("/*") || nc.contains("//") || nc.contains("/*")) {

			return false;
		}

		if (n.startsWith("*") || n.startsWith("@")) {

			return false;
		}

		return p.endsWith("&&") || p.endsWith("||") || p.endsWith(",") || p.endsWith("(") || n.startsWith("&&") || n.startsWith("||") || n.startsWith(")");
	}

	/** Merge next onto prev with a single space, or none where a space would be wrong (after '(' etc). */
	static String join(final String prev, final String next) {

		final String p = rstrip(prev);
		final String n = next.strip();
		final boolean tight = p.endsWith("(") || n.startsWith(")") || n.startsWith(",");

		return p + (tight ? "" : " ") + n;
	}

	/**
	 * R9: join wrapped continuation lines back onto one line where the result fits the 200-column
	 * ceiling. A run that would still overflow is left exactly as written (no rebalancing). Returns a
	 * new list; when {@code reports} is non-null, records each join at its first original line.
	 */
	static List<String> joinRuns(final List<String> L, final List<Rep> reports) {

		final List<String> out = new ArrayList<>();
		int i = 0;
		final boolean[] mask = commentMask(L);

		while (i < L.size()) {

			String candidate = L.get(i);
			int j = i;

			while (j + 1 < L.size() && !mask[i] && !mask[j + 1] && canJoin(candidate, L.get(j + 1))) {

				candidate = join(candidate, L.get(j + 1));
				j++;
			}

			if (j > i && lineWidth(candidate) <= MAX_WIDTH) {

				out.add(candidate);

				if (reports != null) {

					reports.add(new Rep(i, "R9 wrapped line-break can be joined (fits within 200)"));
				}

			} else {

				for (int k = i; k <= j; k++) {

					out.add(L.get(k));
				}
			}

			i = j + 1;
		}

		return out;
	}

	static void scan(final List<String> L, final Set<Integer> insertAfter, final Set<Integer> remove, final List<Rep> reports) {

		final boolean[] commented = commentMask(L);

		for (int i = 0; i < L.size(); i++) {

			if (commented[i]) {

				continue;
			}

			final String raw = L.get(i);
			final String s = rstrip(raw);

			// R5: a return preceded by a statement (not an opening brace) wants a blank before it.
			if (find(EXIT, raw) && raw.strip().startsWith("return") && i > 0) {

				final String prev = rstrip(L.get(i - 1));
				if (!prev.strip().isEmpty() && !prev.endsWith("{")) {

					insertAfter.add(i - 1);
					reports.add(new Rep(i, "R5 blank line missing before return"));
				}
			}

			// R7: a control block is followed by a blank line — inserted after its standalone closing
			// brace, before the next statement. (The mirror case, a blank before an else/catch/finally,
			// is handled in the isCtrl branch where the continuation opens its block.)
			if (s.strip().equals("}") && i + 1 < L.size()) {

				final String nt = L.get(i + 1).strip();
				if (!nt.isEmpty() && !nt.startsWith("}")) {

					final int op = openerOf(L, i);
					if (op >= 0 && find(CTRL_OPEN, L.get(op)) && !find(SWITCH, rstrip(L.get(op)))) {

						insertAfter.add(i);
						reports.add(new Rep(i, "R7 blank line missing after control block"));
					}
				}
			}

			if (!s.endsWith("{")) {

				continue;
			}

			final boolean isCtrl = find(CTRL_OPEN, raw) && !find(SWITCH, s);
			final boolean isType = find(TYPE_OPEN, s) && !s.split("\\{", 2)[0].contains("(");
			final boolean isMethod = s.contains("(") && s.contains(")") && !s.contains("->") && !isCtrl && !s.contains("new ") && !raw.stripLeading().startsWith("@") && !find(SWITCH, s);
			final int body = matchBody(L, i);
			final boolean nxtBlank = blank(L, i + 1);

			if (isMethod || isType) {

				if (body >= 1 && !nxtBlank) {

					insertAfter.add(i);
					reports.add(new Rep(i, "R1 blank line missing after method/type \"{\""));
				}

			} else if (isCtrl) {

				if (body >= 1 && !nxtBlank) {

					insertAfter.add(i);
					reports.add(new Rep(i, "R4 blank line missing after control-block \"{\""));
				}

				// R7: a continuation (else/catch/finally) is preceded by a blank line, separating it from
				// the branch that just closed.
				if (find(CONT, raw) && i > 0 && !blank(L, i - 1) && !rstrip(L.get(i - 1)).endsWith("{")) {

					insertAfter.add(i - 1);
					reports.add(new Rep(i, "R7 blank line missing before else/catch/finally"));
				}

				// R3: a guard bound to the assignment above must not be separated from it by a blank line.
				if (blank(L, i - 1) && boundGuard(L, i)) {

					remove.add(i - 1);
					reports.add(new Rep(i, "R3 stray blank between assignment and bound guard"));
				}

				// R6: separate a control block from the preceding statement with a blank line, unless that
				// statement is a comment or an opening brace (already attached), or the block is a guard bound
				// to the assignment directly above it (boundGuard — covers null/empty and numeric checks).
				if (find(CTRL_LEAD, raw) && i > 0) {

					final String prev = rstrip(L.get(i - 1));
					final String pt = prev.strip();
					final boolean attached = pt.isEmpty() || prev.endsWith("{") || prev.endsWith("*/") || find(COMMENT, prev);

					if (!attached && !boundGuard(L, i)) {

						insertAfter.add(i - 1);
						reports.add(new Rep(i, "R6 blank line missing before control block"));
					}
				}
			}
		}

		// RB: collapse interior runs of 2+ blank lines (keep the first); leave any trailing/EOF run alone.
		Integer runStart = null;

		for (int i = 0; i < L.size(); i++) {

			if (L.get(i).strip().isEmpty() && !commented[i]) {

				if (runStart == null) {

					runStart = i;
				}

			} else {

				if (runStart != null && i - runStart > 1) {

					for (int k = runStart + 1; k < i; k++) {

						remove.add(k);
						reports.add(new Rep(k, "RB more than one consecutive blank line"));
					}
				}

				runStart = null;
			}
		}

		// R2: consecutive declarations/assignments must not be separated by a blank (a line-broken
		// assignment still counts: its first line starts with a type + name + '=').
		for (int i = 0; i < L.size() - 2; i++) {

			if (!commented[i] && !commented[i + 2] && find(ASSIGN, L.get(i)) && L.get(i + 1).strip().isEmpty() && find(ASSIGN, L.get(i + 2))) {

				remove.add(i + 1);
				reports.add(new Rep(i + 1, "R2 stray blank between grouped assignments"));
			}
		}

		// R8: a run of two or more consecutive assignment statements (each possibly line-broken) is
		// always followed by a blank line before the logic begins.
		for (int g = 0; g < L.size(); ) {

			if (commented[g] || !find(ASSIGN, L.get(g))) {

				g++;
				continue;
			}

			int count = 0, k = g;

			while (k < L.size() && find(ASSIGN, L.get(k))) {

				count++;
				k = assignEnd(L, k);
			}

			if (count >= 2 && k < L.size()) {

				final String nt = L.get(k).strip();
				if (!nt.isEmpty() && !nt.startsWith("}")) {

					insertAfter.add(k - 1);
					reports.add(new Rep(k - 1, "R8 blank line missing after assignment group"));
				}
			}

			g = k;
		}
	}

	static List<String> applyOnce(final List<String> L, final Set<Integer> insertAfter, final Set<Integer> remove) {

		final List<String> out = new ArrayList<>();

		for (int i = 0; i < L.size(); i++) {

			if (remove.contains(i)) {

				continue;
			}

			out.add(L.get(i));

			if (insertAfter.contains(i)) {

				out.add("");
			}
		}

		return out;
	}

	static List<Rep> checkFile(final Path f) throws IOException {

		final String content = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
		final List<String> L = Arrays.asList(content.split("\n", -1));
		final Set<Integer> insertAfter = new TreeSet<>(), remove = new TreeSet<>();
		final List<Rep> reports = new ArrayList<>();

		scan(L, insertAfter, remove, reports);
		joinRuns(L, reports);
		reports.sort((x, y) -> x.idx() != y.idx() ? Integer.compare(x.idx(), y.idx()) : x.msg().compareTo(y.msg()));

		return reports;
	}

	static int[] fixFile(final Path f) throws IOException {

		final String content = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
		List<String> L = new ArrayList<>(Arrays.asList(content.split("\n", -1)));
		int inserted = 0, removed = 0, joined = 0;

		for (int pass = 0; pass < 20; pass++) {

			final int sizeBefore = L.size();
			L = joinRuns(L, null);
			final boolean joinedThisPass = L.size() != sizeBefore;
			joined += sizeBefore - L.size();

			final Set<Integer> insertAfter = new TreeSet<>(), remove = new TreeSet<>();
			scan(L, insertAfter, remove, new ArrayList<>());

			if (insertAfter.isEmpty() && remove.isEmpty() && !joinedThisPass) {

				break;
			}

			inserted += insertAfter.size();
			removed += remove.size();
			L = applyOnce(L, insertAfter, remove);
		}

		if (inserted > 0 || removed > 0 || joined > 0) {

			Files.write(f, String.join("\n", L).getBytes(StandardCharsets.UTF_8));
		}

		return new int[]{ inserted, removed, joined };
	}

	static List<Path> javaFiles(final String[] a, final int from) throws IOException {

		final List<Path> files = new ArrayList<>();

		for (int i = from; i < a.length; i++) {

			final Path p = Paths.get(a[i]);
			if (Files.isRegularFile(p) && p.toString().endsWith(".java")) {

				files.add(p);

			} else if (Files.isDirectory(p)) {

				try (Stream<Path> s = Files.walk(p)) {

					s.filter(x -> x.toString().endsWith(".java")).forEach(files::add);
				}
			}
		}

		Collections.sort(files);

		return files;
	}

	public static void main(final String[] a) throws IOException {

		if (a.length < 2 || (!a[0].equals("--check") && !a[0].equals("--fix"))) {

			System.err.println("usage: java StyleLint.java (--check|--fix) <paths...>");
			System.exit(2);
		}

		final List<Path> files = javaFiles(a, 1);

		if (a[0].equals("--check")) {

			int total = 0;

			for (final Path f : files) {

				for (final Rep r : checkFile(f)) {

					System.out.println(f + ":" + (r.idx() + 1) + ": " + r.msg());
					total++;
				}
			}

			System.out.println("\n" + total + " violation(s).");
			System.exit(total > 0 ? 1 : 0);
		}

		int ins = 0, rem = 0, joi = 0, changed = 0;

		for (final Path f : files) {

			final int[] d = fixFile(f);
			if (d[0] > 0 || d[1] > 0 || d[2] > 0) {

				changed++;
			}

			ins += d[0]; rem += d[1]; joi += d[2];
		}

		System.out.println("total: +" + ins + " inserted, -" + rem + " removed, " + joi + " joined across " + changed + " file(s).");
	}
}
