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

import java.util.LinkedList;
import java.util.List;

public class DocumentationEntry {

	protected final List<String> lines = new LinkedList<>();
	protected String fileName;
	protected String header;

	public DocumentationEntry(final String fileName, final String header) {

		this.fileName = fileName;
		this.header   = header;

		lines.add("# " + header);
		lines.add("");
	}

	public void addLines(final List<String> lines, final String... additionalInfo) {
		this.lines.addAll(lines);
	}

	public List<String> getLines() {
		return lines;
	}

	public String getFileName() {
		return fileName;
	}

	public String getHeader() {
		return header;
	}
}
