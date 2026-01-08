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

import org.structr.docs.ontology.AnnotatedConcept;
import org.structr.docs.ontology.Ontology;

import java.util.LinkedList;
import java.util.List;

public class DoBlacklistToken extends NamedConceptToken {

	public DoBlacklistToken(final ConceptToken conceptToken, final IdentifierToken identifierToken) {
		super(conceptToken, identifierToken);
	}

	@Override
	public List<AnnotatedConcept> resolve(final Ontology ontology) {

		final List<IdentifierToken> identifiers = identifierToken.resolve(ontology);
		final List<AnnotatedConcept> concepts            = new LinkedList<>();

		for (final IdentifierToken identifier : identifiers) {

			ontology.getBlacklist().add(identifier.getToken().getContent());
		}

		// empty list
		return concepts;
	}

	@Override
	public boolean isTerminal() {
		return true;
	}
}
