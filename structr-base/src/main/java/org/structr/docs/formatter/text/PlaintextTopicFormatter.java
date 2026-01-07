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
package org.structr.docs.formatter.text;

import org.apache.commons.lang3.StringUtils;
import org.structr.docs.Formatter;
import org.structr.docs.OutputSettings;
import org.structr.docs.ontology.AnnotatedConcept;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.ConceptType;
import org.structr.docs.ontology.Details;

import java.util.List;
import java.util.Set;

public class PlaintextTopicFormatter extends Formatter {

	@Override
	public boolean format(final List<String> lines, final AnnotatedConcept annotatedConcept, final OutputSettings settings, String link, final int level, final Set<AnnotatedConcept> seenConcepts) {

		final Concept concept = annotatedConcept.getConcept();

		if (settings.hasDetail(Details.name) && !ConceptType.Text.equals(concept.getType())) {

			//final String text = (link != null ? link + " " : "") + concept.getType() + " \"" + concept.getName() + "\" from " + concept.getSourceFile() + ":" + concept.getLineNumber();
			//lines.add(formatListHeading(text, level));

			final String text = concept.getName();
			lines.add(StringUtils.repeat(" ", level * 2) + text);
		}

		return true;
	}
}
