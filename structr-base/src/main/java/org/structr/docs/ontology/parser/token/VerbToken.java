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

import org.graalvm.collections.Pair;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.Ontology;

public class VerbToken extends Token<Pair<Concept, Concept>> {

	private final String inverse;

	public VerbToken(final String name, final String inverse) {

		super(name);

		this.inverse = inverse;
	}

	@Override
	public boolean isUnresolved() {
		return false;
	}

	public String getInverse() {
		return inverse;
	}

	public Pair<Concept, Concept> resolve(final Ontology ontology, final String sourceFile, final int line) {

		final Concept verb1 = ontology.getOrCreateConcept(sourceFile, line, "verb", name);
		final Concept verb2 = ontology.getOrCreateConcept(sourceFile, line, "verb", inverse);

		return Pair.create(verb1, verb2);
	}
}
