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
package org.structr.docs;

import org.apache.commons.lang3.StringUtils;
import org.structr.docs.ontology.Concept;

import java.util.*;

public abstract class Formatter {

	public abstract void format(final List<String> lines, final Concept concept, final OutputSettings settings, final String link, final int level);

	/**
	 * Walks through the whole ontology and calls formatters for all the elements.
	 * Output generation is controlled externally by the OutputSettings instance.
	 *
	 * @param lines
	 * @param concept
	 * @param outputSettings
	 * @param level
	 */
	public static void walkOntology(final List<String> lines, final Concept concept, final OutputSettings outputSettings, final String link, final int level, final Set<Concept> seenConcepts) {

		// max level reached, no output beyond this point
		if (level >= outputSettings.getMaxLevels()) {
			return;
		}

		// type filter
		if (concept == null) {

			System.out.println("Encountered null concept while processing ontology.");
			return;
		}

		if (!outputSettings.renderType(concept.getType())) {
			return;
		}

		// only output the same concept once
		if (!seenConcepts.add(concept)) {
			return;
		}

		if (level >= outputSettings.getStartLevel()) {

			// part1: fetch formatter for concept and call format()
			final Formatter formatter = outputSettings.getFormatterForConcept(concept, outputSettings.getOutputMode());
			if (formatter != null) {

				formatter.format(lines, concept, outputSettings, link, level);

			} else {

				System.out.println("No formatter registered for format " + outputSettings.getOutputFormat() + ", mode " + outputSettings.getOutputMode() + " and " + concept);
			}
		}

		// part2: recurse (children are handled externally, i.e. not inside the type-specific formatters)
		final Map<String, List<Concept>> children = concept.getChildren();
		for (final String key : children.keySet()) {

			if ("has".equals(key)) {

				final List<Concept> concepts = new LinkedList<>(children.get(key));

				// only sort second-level topics
				if (level > 0) {
					Collections.sort(concepts, Comparator.comparing(Concept::getName));
				}

				for (final Concept child : concepts) {

					walkOntology(lines, child, outputSettings, key, level + 1, seenConcepts);
				}
			}
		}

		/*
		final Map<String, List<Concept>> parents = concept.getParents();
		for (final String key : parents.keySet()) {

			for (final Concept parent : parents.get(key)) {

				walkOntology(lines, parent, outputSettings, key, level + 1, seenConcepts);
			}
		}
		*/
	}

	// ----- protected methods -----
	protected String formatMarkdownHeading(final String text, final int level) {
		return StringUtils.repeat("#", level) + " " + text;
	}

	protected String formatListHeading(final String text, final int level) {
		return StringUtils.repeat("  ", level) + "- " + text;
	}
}
