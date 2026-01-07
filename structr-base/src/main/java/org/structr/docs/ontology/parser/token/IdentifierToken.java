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

import org.structr.core.function.tokenizer.Token;
import org.structr.docs.ontology.Ontology;

import java.util.LinkedList;
import java.util.List;

public class IdentifierToken extends AbstractToken<List<IdentifierToken>> {

	private final List<IdentifierToken> identifiers = new LinkedList<>();

	protected ConceptToken formatSpecification = null;

	public IdentifierToken(final Token token) {

		super(token);

		identifiers.add(this);
	}

	@Override
	public boolean isUnresolved() {
		return false;
	}

	public void addIdentifier(final IdentifierToken identifier) {
		identifiers.add(identifier);
	}

	public List<IdentifierToken> getIdentifiers() {
		return identifiers;
	}

	public List<IdentifierToken> resolve(final Ontology ontology, final String sourceFile, final int line) {
		return identifiers;
	}

	public void setFormat(final ConceptToken format) {
		this.formatSpecification = format;
	}

	public ConceptToken getFormat() {
		return formatSpecification;
	}
}
