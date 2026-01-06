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

import org.structr.docs.Documentable;
import org.structr.docs.DocumentableType;
import org.structr.docs.Formatter;
import org.structr.docs.OutputSettings;
import org.structr.docs.ontology.AnnotatedConcept;
import org.structr.docs.ontology.Concept;

import java.util.*;

public class MarkdownCodeSourceFormatter extends Formatter {

	@Override
	public void format(final List<String> lines, final AnnotatedConcept annotatedConcept, final OutputSettings settings, String link, final int level) {

		final List<Documentable> documentables  = new LinkedList<>();
		final Concept concept                   = annotatedConcept.getConcept();
		final DocumentableType documentableType = DocumentableType.forOntologyType(concept.getType());

		if (documentableType != null) {

			documentables.addAll(documentableType.getDocumentables());
		}

		// sort
		Collections.sort(documentables, Comparator.comparing(Documentable::getDisplayName));

		// render
		for (final Documentable documentable : documentables) {
			lines.addAll(documentable.createMarkdownDocumentation(settings.getDetails(), level));
		}
	}
}
