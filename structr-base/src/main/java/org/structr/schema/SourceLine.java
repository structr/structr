/*
 * Copyright (C) 2010-2024 Structr GmbH
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

/**
 */
public class SourceLine {

	private final StringBuilder buf = new StringBuilder();
	private CodeSource codeSource   = null;

	public SourceLine(final CodeSource codeSource) {
		this.codeSource = codeSource;
	}

	public SourceLine(final CodeSource codeSource, final Object initial) {
		
		buf.append(initial); 
		
		this.codeSource = codeSource;
	}

	@Override
	public String toString() {
		return buf.toString();
	}

	public SourceLine append(final Object data) {

		buf.append(data);

		return this;
	}

	public SourceLine quoted(final Object data) {

		buf.append("\"");
		buf.append(data);
		buf.append("\"");

		return this;
	}

	public CodeSource getCodeSource() {
		return codeSource;
	}

	public int getNumberOfLines() {

		return toString().split("\r\n|\r|\n").length;
	}
}
