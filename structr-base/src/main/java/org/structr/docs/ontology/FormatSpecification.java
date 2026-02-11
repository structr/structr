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

import org.structr.docs.ontology.parser.token.ConceptToken;

public class FormatSpecification {

	private ConceptType format;
	private ConceptToken token;

	public FormatSpecification(final ConceptType format, final ConceptToken token) {
		this.format = format;
		this.token = token;
	}

	@Override
	public String toString() {
		return format.toString();
	}

	public ConceptType getFormat() {
		return format;
	}

	public ConceptToken getToken() {
		return token;
	}

	public void setFormat(final ConceptType format) {

		token.renameTo(format.name());
	}

	public void setToken(final ConceptToken token) {
		this.token = token;
	}
}
