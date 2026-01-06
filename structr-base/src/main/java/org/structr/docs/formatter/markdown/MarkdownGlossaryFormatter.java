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
package org.structr.docs.formatter.markdown;

import org.apache.commons.lang3.StringUtils;
import org.structr.docs.Formatter;
import org.structr.docs.OutputSettings;
import org.structr.docs.ontology.AnnotatedConcept;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.Ontology;

import java.util.*;

public class MarkdownGlossaryFormatter extends Formatter {

	@Override
	public void format(final List<String> lines, final AnnotatedConcept concept, final OutputSettings settings, String link, final int level) {

		final Ontology ontology      = settings.getOntology();
		final List<Concept> concepts = new LinkedList<>(ontology.getAllConcepts());
		final Set<String> visited    = new LinkedHashSet<>();

		Collections.sort(concepts, Comparator.comparing(c -> c.getName().toLowerCase()));

		String firstCharacter = null;

		for (final Concept c : concepts) {

			final String name  = c.getName();
			final String first = name.substring(0, 1).toUpperCase();

			// show duplicates only once
			if (!visited.add(name.toLowerCase())) {
				continue;
			}

			if (firstCharacter == null || !firstCharacter.equals(first)) {
				firstCharacter = first;

				lines.add(formatMarkdownHeading(firstCharacter, level + 1));
				lines.add("");

				lines.add("| Name | Parent |");
				lines.add("| --- | --- |");
			}

			lines.add("| " + c.getName() + " | " + c.getParentConceptName() + " |");
		}
	}

	// ----- private methods -----
	private String coalesce(final String... strings) {

		for (final String string : strings) {

			if (StringUtils.isNotBlank(string)) {
				return string;
			}
		}

		return null;
	}
}
