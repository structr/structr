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

import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * A concept in the Structr Documentation Ontology. This class
 * will store everything you add into the ontology.
 */
public abstract class Concept {

	protected final Root root;
	protected final String name;

	public abstract List<String> getFilteredDocumentationLines(final Set<Details> details, final int level);

	protected Concept(final Root root, final String name) {

		this.root = root;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	// ----- protected methods -----
	protected String formatMarkdownHeading(final String text, final int level) {
		return StringUtils.repeat("#", level) + " " + text;
	}
}
