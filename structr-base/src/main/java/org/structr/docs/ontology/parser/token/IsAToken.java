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
import org.structr.docs.ontology.*;
import org.structr.docs.ontology.Concept;

import java.util.LinkedList;
import java.util.List;

public class IsAToken extends AbstractToken<AnnotatedConcept> {

	private final NamedConceptToken namedConceptToken;
	private final ConceptToken conceptToken;

	public IsAToken(final NamedConceptToken namedConceptToken, final ConceptToken conceptToken) {

		if (namedConceptToken != null) {
			namedConceptToken.setParent(this);
		}

		if (conceptToken != null) {
			conceptToken.setParent(this);
		}

		this.namedConceptToken = namedConceptToken;
		this.conceptToken      = conceptToken;
	}

	@Override
	public AnnotatedConcept resolve(final Ontology ontology) {

		final ConceptType type                  = conceptToken.resolve(ontology);
		final AnnotatedConcept annotatedConcept = namedConceptToken.resolve(ontology);
		final Concept concept                   = annotatedConcept.getConcept();
		final Concept toRefine                  = ontology.getOrCreateConcept(this, type, concept.getName(), true);

		if (toRefine != null) {

			toRefine.setType(type);

			return new AnnotatedConcept(toRefine);
		}

		return annotatedConcept;
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
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void updateContent(final String key, final String value) {
		throw new UnsupportedOperationException("Not supported.");
	}
}
