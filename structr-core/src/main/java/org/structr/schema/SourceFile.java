/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import javax.tools.SimpleJavaFileObject;
import org.parboiled.common.StringUtils;

/**
 */
public class SourceFile extends SimpleJavaFileObject {

	private List<SourceLine> lines = new LinkedList<>();
	private String className       = null;
	private CharSequence content   = null;
	private int indentLevel        = 0;
		
	public SourceFile(final String className) {
		
		super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
	}

	public void append(final String data) {
		lines.add(new SourceLine(data));
	}

	public void importLine(final String className) {
		line("import ", className, ";");
	}

	public void open(final Object... data) {
		line(data);
		indent();
	}

	public void close() {
		outdent();
		line("}");
	}
	
	

	public SourceLine line(final Object... data) {

		final SourceLine line = new SourceLine(getIndentation());

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
		return StringUtils.join(lines, "");
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

	// ----- private methods -----
	private String getIndentation() {

		final StringBuilder buf = new StringBuilder();
		
		for (int i=0; i<indentLevel; i++) {

			buf.append("\t");
		}

		return buf.toString();
	}
	
}
