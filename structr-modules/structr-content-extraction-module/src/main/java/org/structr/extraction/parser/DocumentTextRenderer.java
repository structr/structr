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
package org.structr.extraction.parser;

import org.structr.extraction.api.document.DocumentBlock;
import org.structr.extraction.api.document.DocumentBlockType;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders collected blocks as plain text and as Markdown, and assigns each block its offsets in the
 * plain text.
 *
 * Blocks are separated by a blank line, except consecutive list items, which are separated by a single
 * line break so a list stays one unit for the splitters.
 */
public class DocumentTextRenderer {

	private final List<DocumentBlock> blocks      = new ArrayList<>();
	private final StringBuilder text      = new StringBuilder();
	private final StringBuilder markdown  = new StringBuilder();

	public DocumentTextRenderer(final List<ParsedBlock> rawBlocks) {

		DocumentBlockType previous = null;

		for (final ParsedBlock raw : rawBlocks) {

			final String plain = plainText(raw);
			final String md    = markdown(raw);

			if (previous != null) {

				final String separator = (previous == DocumentBlockType.LIST_ITEM && raw.type() == DocumentBlockType.LIST_ITEM) ? "\n" : "\n\n";

				text.append(separator);
				markdown.append(separator);
			}

			final int start = text.length();

			text.append(plain);
			markdown.append(md);

			blocks.add(new DocumentBlock(raw.type(), raw.level(), plain, raw.page(), raw.headingPath(), raw.rows(), start, text.length()));

			previous = raw.type();
		}
	}

	public List<DocumentBlock> getBlocks() {

		return blocks;
	}

	public String getText() {

		return text.toString();
	}

	public String getMarkdown() {

		return markdown.toString();
	}

	// ----- private methods -----
	private static String plainText(final ParsedBlock block) {

		if (block.type() == DocumentBlockType.TABLE) {

			final List<String> lines = new ArrayList<>();

			for (final List<String> row : block.rows()) {

				lines.add(String.join(" | ", row));
			}

			return String.join("\n", lines);
		}

		return block.text();
	}

	private static String markdown(final ParsedBlock block) {

		return switch (block.type()) {

			case HEADING   -> "#".repeat(Math.min(6, Math.max(1, block.level()))) + " " + block.text();
			case LIST_ITEM -> "  ".repeat(Math.max(0, block.level() - 1)) + "- " + block.text();
			case QUOTE     -> "> " + block.text();
			case CODE      -> "```\n" + block.text() + "\n```";
			case TABLE     -> markdownTable(block.rows());
			default        -> block.text();
		};
	}

	/**
	 * A pipe table; the first row is used as the header, since documents rarely mark header rows reliably.
	 */
	private static String markdownTable(final List<List<String>> rows) {

		final int columns       = rows.stream().mapToInt(List::size).max().orElse(0);
		final StringBuilder out = new StringBuilder();
		boolean header          = true;

		for (final List<String> row : rows) {

			if (!out.isEmpty()) {

				out.append('\n');
			}

			out.append(markdownRow(row, columns));

			if (header) {

				out.append("\n|").append(" --- |".repeat(columns));
				header = false;
			}
		}

		return out.toString();
	}

	private static String markdownRow(final List<String> row, final int columns) {

		final StringBuilder out = new StringBuilder("|");

		for (int i = 0; i < columns; i++) {

			final String cell = i < row.size() ? row.get(i) : "";

			out.append(' ').append(cell.replace("|", "\\|")).append(" |");
		}

		return out.toString();
	}
}
