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
import org.structr.docs.ontology.HasDisplayName;
import org.structr.docs.ontology.Ontology;

import java.util.LinkedList;
import java.util.List;

public class EnumSourceToken extends NamedConceptToken {

	public EnumSourceToken(final ConceptToken conceptToken, final IdentifierToken identifierToken) {
		super(conceptToken, identifierToken);
	}

	@Override
	public List<Concept> resolve(final Ontology ontology, final String sourceFile, final int lineNumber) {

		final List<String> identifiers = identifierToken.resolve(ontology, sourceFile, lineNumber);
		final List<Concept> concepts   = new LinkedList<>();

		for (final String identifier : identifiers) {

			try {

				final Class enumType = Class.forName(identifier);
				if (enumType != null && enumType.isEnum()) {

					for (final Object constant : enumType.getEnumConstants()) {

						if (constant instanceof HasDisplayName displayName) {

							final String name = displayName.getDisplayName();

							final Concept concept = ontology.getOrCreateConcept(sourceFile, lineNumber, "topic", name);
							if (concept != null) {

								concepts.add(concept);
							}
						}
					}
				}

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		return concepts;
	}
}
