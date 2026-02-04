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
package org.structr.docs.ontology;

import java.util.LinkedHashMap;
import java.util.Map;

public class AnnotatedConcept {

	private FormatSpecification formatSpecification = null;
	private final Concept concept;

	public AnnotatedConcept(final Concept concept) {
		this.concept = concept;
	}

	public AnnotatedConcept(final Concept concept, final FormatSpecification formatSpecification) {

		this.formatSpecification = formatSpecification;
		this.concept             = concept;
	}

	@Override
	public String toString() {
		return concept.toString();
	}

	public String getName() {
		return concept.getName();
	}

	public Concept getConcept() {
		return concept;
	}

	public FormatSpecification getFormatSpecification() {
		return formatSpecification;
	}

	public void setFormatSpecification(final FormatSpecification formatSpecification) {
		this.formatSpecification = formatSpecification;
	}
}
