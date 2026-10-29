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
package org.structr.extraction.parser.figures;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The lines of text of a PDF, with their boxes on the page, in the order of the content stream. That
 * order keeps the columns of a two-column layout apart, where sorting by position would join a line of
 * the left column with the line next to it in the right column.
 */
final class PdfTextLines extends PDFTextStripper {

	/** A line of text: its page (1-based), its text and its box. */
	record Line(int page, String text, Box box) {

		double height() {

			return box.height();
		}
	}

	private final Map<Integer, List<Line>> lines = new HashMap<>();
	private final StringBuilder text             = new StringBuilder();
	private double x0;
	private double y0;
	private double x1;
	private double y1;

	private PdfTextLines() {

		super();

		setSortByPosition(false);
		resetLine();
	}

	/**
	 * The lines of the first {@code maxPages} pages, by page.
	 */
	static Map<Integer, List<Line>> read(final PDDocument document, final int maxPages) throws IOException {

		final PdfTextLines stripper = new PdfTextLines();

		stripper.setStartPage(1);
		stripper.setEndPage(maxPages);
		stripper.writeText(document, new StringWriter());

		return stripper.lines;
	}

	@Override
	protected void writeString(final String string, final List<TextPosition> positions) throws IOException {

		if (!text.isEmpty()) {

			text.append(' ');
		}

		text.append(string);

		for (final TextPosition position : positions) {

			final double left   = position.getXDirAdj();
			final double bottom = position.getYDirAdj();
			final double top    = bottom - position.getHeightDir();

			x0 = Math.min(x0, left);
			y0 = Math.min(y0, top);
			x1 = Math.max(x1, left + position.getWidthDirAdj());
			y1 = Math.max(y1, bottom);
		}
	}

	@Override
	protected void writeLineSeparator() throws IOException {

		flush();
	}

	@Override
	protected void writeParagraphEnd() throws IOException {

		flush();
	}

	@Override
	protected void endPage(final PDPage page) throws IOException {

		flush();
	}

	// ----- private methods -----
	private void flush() {

		final String line = text.toString().strip();
		if (!line.isEmpty() && x0 <= x1) {

			lines.computeIfAbsent(getCurrentPageNo(), k -> new ArrayList<>()).add(new Line(getCurrentPageNo(), line, new Box(x0, y0, x1, y1)));
		}

		resetLine();
	}

	private void resetLine() {

		text.setLength(0);

		x0 = Double.MAX_VALUE;
		y0 = Double.MAX_VALUE;
		x1 = -Double.MAX_VALUE;
		y1 = -Double.MAX_VALUE;
	}
}
