/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.schema;

import org.apache.commons.lang3.StringUtils;

import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

/**
 */
public class SourceFile extends SimpleJavaFileObject {

	private final List<SourceLine> lines = new LinkedList<>();
	private String className             = null;
	private int indentLevel              = 0;

	public SourceFile(final String className) {

		super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);

		this.className = className;
	}

	public void importLine(final String className) {
		line(null, "import ", className, ";");
	}

	public SourceLine begin(final Object source, final Object... data) {

		final SourceLine line = line(source, data);

		indent();

		return line;
	}

	public void end() {

		outdent();
		line(null, "}");
	}

	public SourceLine end(final Object... data) {

		outdent();
		return line(null, data);
	}

	public SourceLine endBegin(final Object source, final Object... data) {

		outdent();
		return begin(source, data);
	}

	public SourceLine line(final Object source, final Object... data) {

		final SourceLine line = new SourceLine(source, getIndentation());

		for (final Object d : data) {
			line.append(d);
		}

		lines.add(line);

		return line;
	}

	public List<SourceLine> getLines() {
		return lines;
	}

	public String getContent() {
		return StringUtils.join(lines, "\n");
	}

	public void indent() {
		indentLevel++;
	}

	public void outdent() {
		indentLevel--;
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
		return getContent();
	}

	public String getClassName() {

		return className;
	}

	// ----- private methods -----
	private String getIndentation() {

		final StringBuilder buf = new StringBuilder();

		for (int i=0; i<indentLevel; i++) {

			buf.append("\t");
		}

		return buf.toString();
	}

	private void storePosition(final String uuid) {




	}
}
