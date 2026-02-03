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
import org.graalvm.nativeimage.AnnotationAccess;
import org.structr.core.function.tokenizer.Token;
import org.structr.docs.Documentable;
import org.structr.docs.Formatter;
import org.structr.docs.OutputSettings;
import org.structr.docs.ontology.*;
import org.structr.docs.ontology.parser.token.AbstractToken;

import java.util.LinkedList;
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

		if (settings.hasDetail(Details.source) || settings.hasDetail(Details.all)) {

			final List<String> buf = new LinkedList<>();

			for (final AbstractToken token : concept.getTokens()) {

				buf.add(token.toString());
				//buf.add(token.getSourceFile() + ":" + token.getRow());
			}

			lines.add("<small>Sources: " + StringUtils.join(buf, ", ") + "</small>");
			lines.add("");
		}

		if (settings.hasDetail(Details.all)) {

			// iterate over all links
			for (final Map.Entry<ConceptType, Set<Concept>> entry : concept.getGroupedChildren().entrySet()) {

				final ConceptType conceptType = entry.getKey();
				final Set<Concept> concepts   = entry.getValue();

				if (!concepts.isEmpty()) {

					lines.add(formatMarkdownHeading(conceptType.name(), level + 2));

					lines.add("");
					lines.add("| Name | Description |");
					lines.add("| --- | --- |");

					for (final Concept child : entry.getValue()) {

						final Documentable documentable = child.getDocumentable();
						if (documentable != null) {

							lines.add("| " + documentable.getName() + " | " + documentable.getShortDescription() + " |");

						} else {

							lines.add("| " + child.getName() + " | " + child.getShortDescription() + " |");
						}

					}
				}
			}
		}

		return false;
	}
}






























