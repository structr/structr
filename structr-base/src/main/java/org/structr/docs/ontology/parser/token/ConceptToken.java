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
import org.structr.docs.ontology.ConceptType;
import org.structr.docs.ontology.Ontology;

/**
 * A token that resolves to a class in the ontology.
 */
public class ConceptToken extends StringToken<ConceptType> {

	private boolean allowReuse = true;
	private final Token originalToken;
	private final ConceptType type;

	public ConceptToken(final ConceptType type, final Token originalToken) {

		//super(type.getIdentifier());
		super(originalToken);

		this.type          = type;
		this.originalToken = originalToken;
	}

	@Override
	public ConceptType resolve(final Ontology ontology) {
		return type;
	}

	public Token getOriginalToken() {
		return originalToken;
	}

	public ConceptType getType() {
		return type;
	}

	public IdentifierToken asIdentifierToken() {
		return new IdentifierToken(originalToken);
	}

	public void setAllowReuse(final boolean allowReuse) {
		this.allowReuse = allowReuse;
	}

	public boolean allowReuse() {
		return allowReuse;
	}

	@Override
	public boolean isTerminal() {
		return false;
	}

	@Override
	public void renameTo(final String newName) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}
}
