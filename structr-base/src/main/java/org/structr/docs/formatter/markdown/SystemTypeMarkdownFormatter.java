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
import org.structr.docs.ontology.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SystemTypeMarkdownFormatter extends Formatter {

	private final Set<ConceptType> blacklistedTypes = Set.of(ConceptType.Text, ConceptType.Constant); //, ConceptType.Setting, ConceptType.Helper, ConceptType.Category, ConceptType.HttpVerb);

	@Override
	public void format(final List<String> lines, final AnnotatedConcept annotatedConcept, final OutputSettings settings, final String link, final int level) {

		final Concept concept = annotatedConcept.getConcept();

		// do not display blacklisted entries
		if (blacklistedTypes.contains(concept.getType())) {
			return;
		}

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

		if (settings.hasDetail(Details.source) || settings.hasDetail(Details.all)) {

			final List<String> buf = new LinkedList<>();

			for (final Occurrence occurrence : concept.getOccurrences()) {

				buf.add(occurrence.getSourceFile() + ":" + occurrence.getLineNumber());
			}

			lines.add("<small>Sources: " + StringUtils.join(buf, ", ") + "</small>");
			lines.add("");
		}

		if (settings.hasDetail(Details.all)) {

			if (concept.getShortDescription() != null) {

				lines.add(concept.getShortDescription());
			}
		}
	}
}
