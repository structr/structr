/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.docs;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Badges on passages of the markdown documentation, e.g. to mark a feature of Structr Enterprise. A
 * passage is marked in one of two ways:
 *
 * <ul>
 *   <li>a section: {@code {enterprise}} at the end of a heading marks everything up to the next heading
 *       of the same or a higher level,</li>
 *   <li>a block: {@code {enterprise}} and {@code {/enterprise}} on lines of their own enclose any
 *       paragraphs, lists, tables or code blocks.</li>
 * </ul>
 *
 * In HTML, a marked passage is wrapped in an element that the stylesheet shows with a line along it and
 * the badge in the margin. In plain text, the passage starts with "(Enterprise)". In names (headings in
 * the navigation, search results), the marker is left out.
 */
public final class Badges {

	private static final Pattern HEADING     = Pattern.compile("^(#{1,6})\\s+(.*?)\\s*$");
	private static final Pattern SECTION     = Pattern.compile("^(#{1,6}\\s+.*?)\\s*\\{enterprise\\}\\s*$");
	private static final Pattern BLOCK_START = Pattern.compile("^\\s*\\{enterprise\\}\\s*$");
	private static final Pattern BLOCK_END   = Pattern.compile("^\\s*\\{/enterprise\\}\\s*$");
	private static final Pattern FENCE       = Pattern.compile("^\\s*(```|~~~)");
	private static final Pattern MARKER      = Pattern.compile("\\s*\\{/?enterprise\\}");

	private static final String HTML_START  = "<div class=\"badge-passage badge-enterprise\" title=\"Available in Structr Enterprise\">";
	private static final String HTML_INLINE = "<span class=\"badge-inline badge-enterprise\" title=\"Available in Structr Enterprise\"></span>";
	private static final String HTML_END    = "</div>";
	private static final String TEXT_LABEL  = "(Enterprise)";

	private Badges() {
	}

	public static List<String> toHtml(final List<String> lines) {

		return convert(lines, true);
	}

	public static List<String> toText(final List<String> lines) {

		return convert(lines, false);
	}

	public static String strip(final String name) {

		return MARKER.matcher(name).replaceAll("").strip();
	}

	/**
	 * Marks the section that starts with the first heading of the given lines, e.g. the documentation of
	 * a function of Structr Enterprise, so that it gets the badge.
	 */
	public static List<String> markFirstSection(final List<String> lines) {

		final List<String> result = new ArrayList<>(lines);

		for (int i = 0; i < result.size(); i++) {

			if (HEADING.matcher(result.get(i)).matches()) {

				result.set(i, result.get(i).stripTrailing() + " {enterprise}");
				break;
			}
		}

		return result;
	}

	/** The badge as an inline HTML element, e.g. next to a name in a table. */
	public static String inlineHtml() {

		return HTML_INLINE;
	}

	// ----- private methods -----
	private static List<String> convert(final List<String> lines, final boolean html) {

		final List<String> result          = new ArrayList<>();
		final LinkedList<Integer> sections = new LinkedList<>();
		boolean code                       = false;

		for (final String line : lines) {

			if (FENCE.matcher(line).find()) {

				code = !code;
			}

			if (code) {

				result.add(line);
				continue;
			}

			final Matcher heading = HEADING.matcher(line);
			if (heading.matches()) {

				final int level = heading.group(1).length();

				// a heading of the same or a higher level ends the marked sections it is not part of
				while (!sections.isEmpty() && sections.getLast() >= level) {

					sections.removeLast();
					end(result, html);
				}

				final Matcher section = SECTION.matcher(line);
				if (section.matches()) {

					sections.addLast(level);
					start(result, html);
					result.add(html ? section.group(1) : section.group(1) + " " + TEXT_LABEL);

					continue;
				}
			}

			if (BLOCK_START.matcher(line).matches()) {

				start(result, html);

				if (!html) {

					result.add(TEXT_LABEL);
				}

				continue;
			}

			if (BLOCK_END.matcher(line).matches()) {

				end(result, html);
				continue;
			}

			result.add(line);
		}

		while (!sections.isEmpty()) {

			sections.removeLast();
			end(result, html);
		}

		return result;
	}

	private static void start(final List<String> result, final boolean html) {

		if (html) {

			// blank lines around the element, so the markdown inside is parsed as markdown
			result.add("");
			result.add(HTML_START);
			result.add("");
		}
	}

	private static void end(final List<String> result, final boolean html) {

		if (html) {

			result.add("");
			result.add(HTML_END);
			result.add("");
		}
	}
}
