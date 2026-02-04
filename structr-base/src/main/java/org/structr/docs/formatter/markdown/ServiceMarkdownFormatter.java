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

import org.structr.docs.Documentable;
import org.structr.docs.DocumentableType;
import org.structr.docs.Formatter;
import org.structr.docs.OutputSettings;
import org.structr.docs.ontology.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServiceMarkdownFormatter extends Formatter {

	private final Set<ConceptType> blacklistedTypes = Set.of(ConceptType.Text, ConceptType.Constant); //, ConceptType.Setting, ConceptType.Helper, ConceptType.Category, ConceptType.HttpVerb);

	@Override
	public boolean format(final List<String> lines, final Link link, final OutputSettings settings, final int level, final Set<Concept> seenConcepts) {

		final Concept concept = link.getTarget();

		// do not display blacklisted entries
		if (blacklistedTypes.contains(concept.getType())) {
			return false;
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

		/*
		if (concept.getDocumentable() != null) {

			final Documentable documentable = concept.getDocumentable();

			lines.addAll(documentable.createMarkdownDocumentation(settings.getDetails(), settings.getStartLevel()));
		}
		*/

		if (settings.hasDetail(Details.shortDescription)) {

			if (concept.getShortDescription() != null) {

				lines.addAll(split(concept.getShortDescription()));
				lines.add("");
			}

			for (final Link childLink : concept.getChildLinks(Verb.Has)) {

				// use markdown file as description for this type
				if (childLink.getFormatSpecification() != null && ConceptType.Description.equals(childLink.getFormatSpecification().getFormat())) {

					final Concept description = childLink.getTarget();
					if (description != null) {

						final OutputSettings topicSettings = OutputSettings.withDetails(concept.getOntology(), Details.all);

						topicSettings.setRenderComments(false);
						topicSettings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.MarkdownTopic, new MarkdownIncludeFormatter());

						// walk ontology
						Formatter.walkOntology(lines, new Link(null, null, concept), topicSettings, 0, new LinkedHashSet<>());
					}
				}
			}
		}

		return false;
	}
}






























