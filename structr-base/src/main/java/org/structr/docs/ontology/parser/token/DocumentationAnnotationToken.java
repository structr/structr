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
package org.structr.docs.ontology.parser.token;

import org.structr.core.function.tokenizer.Token;
import org.structr.docs.ontology.Ontology;

public class DocumentationAnnotationToken extends AbstractToken<Void> {

	private final String location;

	public DocumentationAnnotationToken(final String location) {
		this.location = location;
	}

	@Override
	public Void resolve(final Ontology ontology) {
		return null;
	}

	@Override
	public String toString() {
		return "@Documentation on " + location;
	}

	@Override
	public boolean isTerminal() {
		return false;
	}

	@Override
	public Token getToken() {
		return null;
	}

	@Override
	public void renameTo(final String newName) {
		throw new UnsupportedOperationException("Cannot rename annotated Java elements.");
	}

	@Override
	public void updateContent(final String key, final String value) {
		throw new UnsupportedOperationException("Cannot update annotated Java elements.");
	}
}
