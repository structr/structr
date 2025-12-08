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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class StructuralConcept extends Concept {

	public StructuralConcept(final String name) {
		super(name);
	}

	@Override
	public List<String> getFilteredDocumentationLines(final Set<Details> details, final int level) {

		final List<String> lines = new LinkedList<>();

		final List<String> childrenLines = new LinkedList<>();

		for (final Concept child : children) {
			childrenLines.addAll(child.getFilteredDocumentationLines(details, level + 1));
		}

		// only output parent info if children produce output
		if (!childrenLines.isEmpty()) {

			// level 0 is invisible
			if (level > 0 && details.contains(Details.Name)) {

				lines.add(formatMarkdownHeading(getName(), level));
				lines.add(""); // empty line
			}

			lines.addAll(childrenLines);
		}

		return lines;
	}
}
