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
package org.structr.docs.ontology;

public class Occurrence {

	private final String sourceFile;
	private final int lineNumber;

	public Occurrence(final String sourceFile, final int lineNumber) {
		this.sourceFile = sourceFile;
		this.lineNumber = lineNumber;
	}

	@Override
	public int hashCode() {
		return (sourceFile + ":" + lineNumber).hashCode();
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof Occurrence other) {
			return sourceFile.equals(other.sourceFile) && lineNumber == other.lineNumber;
		}

		return false;
	}

	public String getSourceFile() {
		return sourceFile;
	}
	public int getLineNumber() {
		return lineNumber;
	}
}
