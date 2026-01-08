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

import org.structr.api.util.Category;
import org.structr.docs.Documentable;
import org.structr.docs.DocumentableType;
import org.structr.docs.ontology.*;

public class EnumSourceToken extends NamedConceptToken {

	public EnumSourceToken(final ConceptToken conceptToken, final IdentifierToken identifierToken) {
		super(conceptToken, identifierToken);
	}

	@Override
	public AnnotatedConcept resolve(final Ontology ontology) {

		final String identifier = identifierToken.resolve(ontology);

		try {


			final Class enumType = Class.forName(identifier);
			if (enumType != null && enumType.isEnum()) {

				final Concept enumConcept = ontology.getOrCreateConcept(this, ConceptType.EnumSource, identifier, false);

				for (final Object constant : enumType.getEnumConstants()) {

					// Documentable?
					if (constant instanceof Documentable documentable) {

						final String name = documentable.getDisplayName();
						if (name != null) {

							final DocumentableType documentableType = documentable.getDocumentableType();
							final Concept concept = ontology.getOrCreateConcept(this, documentableType.getConcept(), name, false);
							if (concept != null) {

								if (documentable.getShortDescription() != null) {
									concept.setShortDescription(documentable.getShortDescription());
								}

								enumConcept.createSymmetricLink(Verb.Has, new AnnotatedConcept(concept));
							}
						}
					}

					// or just Category?
					if (constant instanceof Category category) {

						final String name = category.getDisplayName();
						if (name != null) {

							final Concept concept = ontology.getOrCreateConcept(this, ConceptType.Topic, name, true);
							if (concept != null) {

								if (category.getShortDescription() != null) {
									concept.setShortDescription(category.getShortDescription());
								}

								enumConcept.createSymmetricLink(Verb.Has, new AnnotatedConcept(concept));
							}
						}
					}
				}

				return new AnnotatedConcept(enumConcept);
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return null;
	}

	@Override
	public boolean isTerminal() {
		return false;
	}
}
