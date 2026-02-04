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
import org.structr.docs.Formatter;
import org.structr.docs.OutputSettings;
import org.structr.docs.ontology.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SystemTypeMarkdownFormatter extends Formatter {

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

		if (settings.hasDetail(Details.all)) {

			// iterate over all links
			for (final Map.Entry<ConceptType, Set<Concept>> entry : concept.getGroupedChildren().entrySet()) {

				final ConceptType conceptType = entry.getKey();
				final Set<Concept> concepts   = entry.getValue();

				// do not output markdown files as children here
				if (!concepts.isEmpty() && !ConceptType.MarkdownTopic.equals(conceptType)) {

					if (ConceptType.Property.equals(conceptType)) { lines.add(formatMarkdownHeading("Properties", level + 2)); }
					if (ConceptType.Method.equals(conceptType)) { lines.add(formatMarkdownHeading("Methods", level + 2)); }

					//lines.add(formatMarkdownHeading(conceptType.name(), level + 2));

					lines.add("");
					lines.add("| Name | Type | Description |");
					lines.add("| --- | --- | --- |");

					for (final Concept child : entry.getValue()) {

						final Documentable documentable = child.getDocumentable();
						if (documentable != null) {

							lines.add("| " + documentable.getName() + " | " + documentable.getShortDescription() + " |");
							throw new RuntimeException("Code path still in use but should be removed.");

						} else {

							final String name = child.getName();
							final String type = (String) child.getMetadata().get("propertyType");
							final String desc = child.getShortDescription();

							lines.add("| `" + name + "` | `" + type + "` | " + desc + " |");
						}

					}
				}
			}
		}

		return true;
	}
}