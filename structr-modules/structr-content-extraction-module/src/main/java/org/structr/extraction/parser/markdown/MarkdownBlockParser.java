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
package org.structr.extraction.parser.markdown;

import org.structr.extraction.api.document.DocumentBlockType;
import org.structr.extraction.parser.ParsedBlock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the block structure of a Markdown document.
 *
 * Tika parses Markdown as plain text, which keeps the "#" characters but loses what they mean. This
 * covers the block elements that matter for chunking (ATX headings, lists, quotes, fenced code and pipe
 * tables); inline markup stays in the text as written.
 */
public class MarkdownBlockParser {

	private static final Pattern HEADING      = Pattern.compile("^(#{1,6})\\s+(.*?)\\s*#*\\s*$");
	private static final Pattern LIST_ITEM    = Pattern.compile("^(\\s*)(?:[-*+]|\\d+[.)])\\s+(.*)$");
	private static final Pattern QUOTE        = Pattern.compile("^\\s*>\\s?(.*)$");
	private static final Pattern FENCE        = Pattern.compile("^\\s*(```|~~~).*$");
	private static final Pattern TABLE_ROW    = Pattern.compile("^\\s*\\|.*\\|\\s*$");
	private static final Pattern TABLE_RULE   = Pattern.compile("^\\s*\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)*\\|?\\s*$");
	private static final Pattern WHITESPACE   = Pattern.compile("\\s+");

	private final List<ParsedBlock> blocks        = new ArrayList<>();
	private final LinkedList<Heading> headings  = new LinkedList<>();
	private final int maxCharacters;

	private boolean truncated = false;

	public MarkdownBlockParser(final int maxCharacters) {

		this.maxCharacters = maxCharacters;
	}

	public List<ParsedBlock> parse(final String source) {

		final String input        = source.length() > maxCharacters ? source.substring(0, maxCharacters) : source;
		final String[] lines      = input.split("\\R", -1);
		final StringBuilder para  = new StringBuilder();

		truncated = source.length() > maxCharacters;

		int i = 0;

		while (i < lines.length) {

			final String line = lines[i];
			if (FENCE.matcher(line).matches()) {

				flushParagraph(para);

				final String fence      = line.trim().substring(0, 3);
				final StringBuilder code = new StringBuilder();

				i++;

				while (i < lines.length && !lines[i].trim().startsWith(fence)) {

					code.append(lines[i]).append('\n');
					i++;
				}

				addBlock(DocumentBlockType.CODE, 0, code.toString().stripTrailing(), List.of());
				i++;
				continue;
			}

			final Matcher heading = HEADING.matcher(line);
			if (heading.matches()) {

				flushParagraph(para);

				final int level    = heading.group(1).length();
				final String title = normalize(heading.group(2));

				while (!headings.isEmpty() && headings.getLast().level() >= level) {

					headings.removeLast();
				}

				headings.addLast(new Heading(level, title));

				addBlock(DocumentBlockType.HEADING, level, title, List.of());
				i++;
				continue;
			}

			if (TABLE_ROW.matcher(line).matches() && i + 1 < lines.length && TABLE_RULE.matcher(lines[i + 1]).matches()) {

				flushParagraph(para);

				final List<List<String>> rows = new ArrayList<>();

				rows.add(cells(line));
				i += 2;

				while (i < lines.length && TABLE_ROW.matcher(lines[i]).matches()) {

					rows.add(cells(lines[i]));
					i++;
				}

				addBlock(DocumentBlockType.TABLE, 0, null, rows);
				continue;
			}

			final Matcher item = LIST_ITEM.matcher(line);
			if (item.matches()) {

				flushParagraph(para);

				final int level = item.group(1).replace("\t", "  ").length() / 2 + 1;

				addBlock(DocumentBlockType.LIST_ITEM, level, normalize(item.group(2)), List.of());
				i++;
				continue;
			}

			final Matcher quote = QUOTE.matcher(line);
			if (quote.matches()) {

				flushParagraph(para);

				final StringBuilder text = new StringBuilder(quote.group(1));

				i++;

				while (i < lines.length && QUOTE.matcher(lines[i]).matches()) {

					final Matcher next = QUOTE.matcher(lines[i]);

					next.matches();
					text.append(' ').append(next.group(1));
					i++;
				}

				addBlock(DocumentBlockType.QUOTE, 0, normalize(text.toString()), List.of());
				continue;
			}

			if (line.isBlank()) {

				flushParagraph(para);

			} else {

				para.append(line).append('\n');
			}

			i++;
		}

		flushParagraph(para);

		return blocks;
	}

	public boolean isTruncated() {

		return truncated;
	}

	// ----- private methods -----
	private void flushParagraph(final StringBuilder para) {

		final String text = normalize(para.toString());

		para.setLength(0);

		if (!text.isEmpty()) {

			addBlock(DocumentBlockType.PARAGRAPH, 0, text, List.of());
		}
	}

	private void addBlock(final DocumentBlockType type, final int level, final String text, final List<List<String>> rows) {

		final List<String> path = headings.stream().map(Heading::title).toList();

		blocks.add(new ParsedBlock(type, level, text, null, path, rows, null));
	}

	private static List<String> cells(final String line) {

		String trimmed = line.trim();
		if (trimmed.startsWith("|")) {

			trimmed = trimmed.substring(1);
		}

		if (trimmed.endsWith("|")) {

			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}

		return Arrays.stream(trimmed.split("(?<!\\\\)\\|", -1)).map(cell -> normalize(cell.replace("\\|", "|"))).toList();
	}

	private static String normalize(final String text) {

		return WHITESPACE.matcher(text).replaceAll(" ").strip();
	}

	private record Heading(int level, String title) {
	}
}
