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
import org.structr.docs.*;
import org.structr.docs.Formatter;
import org.structr.docs.ontology.AnnotatedConcept;
import org.structr.docs.ontology.Concept;

import java.util.*;

public class MarkdownTableFormatter extends Formatter {

	@Override
	public boolean format(final List<String> lines, final AnnotatedConcept annotatedConcept, final OutputSettings settings, final String link, final int level, final Set<AnnotatedConcept> visited) {

		final Concept concept = annotatedConcept.getConcept();

		lines.add(formatMarkdownHeading(concept.getName(), level + 1));

		if (concept.getShortDescription() != null) {
			lines.add(concept.getShortDescription());
		}

		lines.add("");
		lines.add("| Name | Description |");
		lines.add("| --- | --- |");

		// format children
		final List<AnnotatedConcept> children = concept.getChildren("has");
		if (children != null) {

			final List<Documentable> documentables = new LinkedList<>();

			for (final AnnotatedConcept annotatedChild : children) {

				// mark concept as visited so it is not rendered again
				visited.add(annotatedChild);

				final Concept child             = annotatedChild.getConcept();
				final Documentable documentable = child.getDocumentable();

				if (documentable != null) {

					documentables.add(documentable);

				} else {

					documentables.add(new Documentable() {
						@Override
						public DocumentableType getDocumentableType() {
							return DocumentableType.Constant;
						}

						@Override
						public String getName() {
							return child.getName();
						}

						@Override
						public String getShortDescription() {
							return coalesce(child.getShortDescription(), (String) child.getMetadata().get("description"));
						}
					});
				}
			}

			Collections.sort(documentables, Comparator.comparing(Documentable::getDisplayName));

			for (final Documentable documentable : documentables) {
				lines.add("| `" + documentable.getDisplayName() + "` | " + documentable.getShortDescription() + " |");
			}
		}

		return false;
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
