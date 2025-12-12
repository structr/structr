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
package org.structr.docs.ontology.parser.token;

import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.Ontology;

public abstract class Token<T> {

	protected String name;

	public Token(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + name + ")";
	}

	public abstract boolean isUnresolved();
	public abstract T resolve(final Ontology ontology, final String sourceFile, final int line);

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getType() {
		return getClass().getSimpleName();
	}

	public boolean isInQuotes() {

		if (name.startsWith("\"") && name.endsWith("\"")) {
			return true;
		}

		if (name.startsWith("\'") && name.endsWith("\'")) {
			return true;
		}

		return false;
	}
}
