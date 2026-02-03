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
import org.structr.docs.ontology.AnnotatedConcept;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.Ontology;

/**
 * A token that references the last subject or entity mentioned
 * earlier in the text.
 */
public class AnaphoricPronounToken extends NamedConceptToken {

	private final Token token;

	public AnaphoricPronounToken(final Token token) {

		super(null, null);

		this.token = token;
	}

	@Override
	public AnnotatedConcept resolve(final Ontology ontology) {

		final Concept currentSubject = ontology.getCurrentSubject();
		if (currentSubject != null) {

			return new AnnotatedConcept(ontology.getCurrentSubject());
		}

		throw new RuntimeException("Syntax error in " + token + ": pronoun \"it\" has no subject to refer to.");
	}
}
