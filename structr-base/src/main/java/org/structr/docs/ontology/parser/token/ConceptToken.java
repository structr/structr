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

import org.structr.docs.ontology.ConceptType;
import org.structr.docs.ontology.Ontology;

/**
 * A token that resolves to a class in the ontology.
 */
public class ConceptToken extends Token<ConceptType> {

	private final String originalToken;
	private final ConceptType type;

	public ConceptToken(final ConceptType type, final String originalToken) {

		super(type.getIdentifier());

		this.type          = type;
		this.originalToken = originalToken;
	}

	@Override
	public boolean isUnresolved() {
		return false;
	}

	@Override
	public ConceptType resolve(final Ontology ontology, final String sourceFile, final int line) {
		return type;
	}

	public String getOriginalToken() {
		return originalToken;
	}

	public ConceptType getType() {
		return type;
	}
}
