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
package org.structr.docs.formatter.markdown;

import org.apache.commons.lang3.StringUtils;
import org.structr.docs.Formatter;
import org.structr.docs.OutputSettings;
import org.structr.docs.ontology.*;

import java.util.*;

public class MarkdownGlossaryFormatter extends Formatter {

	@Override
	public boolean format(final List<String> lines, final Link link, final OutputSettings settings, final int level, final Set<Concept> seenConcepts) {

		final Concept concept = link.getTarget();

		if (settings.hasDetail(Details.name) || settings.hasDetail(Details.all)) {

			// add parent topic here, but only at level 0
			if (level == 0) {

				final String parentConceptName = concept.getParentConceptName();
				if (parentConceptName != null) {

					lines.add(formatMarkdownHeading(parentConceptName, level));
					lines.add("");
				}
			}

			lines.add(formatMarkdownHeading(concept.getName(), level + 1));
			lines.add("");
		}

		final Ontology ontology      = settings.getOntology();
		final List<Concept> concepts = new LinkedList<>(ontology.getAllConcepts());
		final Set<String> visited    = new LinkedHashSet<>();

		Collections.sort(concepts, Comparator.comparing(c -> c.getName().toLowerCase()));

		String firstCharacter = null;

		for (final Concept c : concepts) {

			final String name = clean(c.getName());

			if (StringUtils.isNotBlank(name) && wordCount(name) < 4) {

				final String first = name.substring(0, 1).toUpperCase();

				// show duplicates only once
				if (!visited.add(name.toLowerCase())) {
					continue;
				}

				if (firstCharacter == null || !firstCharacter.equals(first)) {
					firstCharacter = first;

					lines.add(formatMarkdownHeading(firstCharacter, level + 2));
					lines.add("");

					lines.add("| Name | Parent |");
					lines.add("| --- | --- |");
				}

				lines.add("| " + c.getName() + " | [" + collectParents(c) + "](" + createLinkForParentsString(collectParents(c)) + ") |");
			}
		}

		return true;
	}

	// ----- private methods -----
	private String collectParents(final Concept concept) {

		final Set<String> strings = new LinkedHashSet<>();
		final List<String> parents = new LinkedList<>();

		collectParents(concept, strings, 0);

		parents.addAll(strings);

		return StringUtils.join(parents.reversed(), " / ");
	}

	private void collectParents(final Concept concept, final Set<String> data, final int level) {

		if (level > 3) {
			return;
		}

		for (final Map.Entry<Verb, List<Link>> parentList : concept.getParentLinks().entrySet()) {

			for (final Link link : parentList.getValue()) {

				final Concept parent = link.getSource();

				if (!"Structr".equals(parent.getName())) {

					data.add(parent.getName());

					collectParents(parent, data, level + 1);
				}
			}
		}
	}

	private static int wordCount(final String name) {
		return name.split(" ").length;
	}

	private static String clean(final String name) {

		if (name.startsWith("\"") && name.endsWith("\"")) {
			return name.substring(1, name.length() - 1);
		}

		return name;
	}

	private static String cleanStringForLink(String str) {
		return str.replace("?", "")
				.replaceAll("[\\W]+", "-")
				.toLowerCase();
	}

	private static String createLinkForParentsString(final String input) {
		long count = input.chars().filter(c -> c == '/').count();
		if (count < 2) {
			return "/structr/docs/ontology/"
					+ input.replaceAll(" / ", "/")
					.replaceAll(" ", "%20");
		}
		return "/structr/docs/ontology/"
				+ (input.substring(0, input.lastIndexOf("/") + 2) + "#" + cleanStringForLink(input.substring(input.lastIndexOf("/") + 2)))
				.replaceAll(" / ", "/")
				.replaceAll(" ", "%20");
	}
}
