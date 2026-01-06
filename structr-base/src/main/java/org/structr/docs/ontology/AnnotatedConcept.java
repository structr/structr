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
package org.structr.docs.ontology;

import java.util.LinkedHashMap;
import java.util.Map;

public class AnnotatedConcept {

	private final Map<String, Object> annotations = new LinkedHashMap<>();
	private final Concept concept;

	public AnnotatedConcept(final Concept concept) {
		this.concept = concept;
	}

	public AnnotatedConcept(final Concept concept, final Map<String, Object> annotations) {

		this.concept = concept;
		this.annotations.putAll(annotations);
	}

	public Map<String, Object> getAnnotations() {
		return annotations;
	}

	public Concept getConcept() {
		return concept;
	}

	public String getName() {

		if (concept != null) {

			return concept.getName();
		}

		return null;
	}

	public ConceptType getFormat() {
		return (ConceptType) annotations.get("format");
	}

	public ConceptType getType() {
		return concept.getType();
	}
}
