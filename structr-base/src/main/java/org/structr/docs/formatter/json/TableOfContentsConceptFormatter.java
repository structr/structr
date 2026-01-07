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
package org.structr.docs.formatter.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.structr.docs.Formatter;
import org.structr.docs.OutputSettings;
import org.structr.docs.ontology.AnnotatedConcept;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.ConceptType;

import java.util.*;

public class TableOfContentsConceptFormatter extends Formatter {

	@Override
	public boolean format(final List<String> lines, final AnnotatedConcept annotatedConcept, final OutputSettings settings, final String link, final int level, final Set<AnnotatedConcept> visited) {

		final Concept concept                 = annotatedConcept.getConcept();
		final Gson gson                       = new GsonBuilder().create();
		final List<Map<String, Object>> links = new LinkedList<>();
		final Map<String, Object> data        = new LinkedHashMap<>();

		data.put("name",  concept.getName());
		data.put("type",  concept.getType());
		data.put("links", links);

		for (final Map.Entry<String, List<AnnotatedConcept>> child : concept.getChildren().entrySet()) {

			final List<Map<String, Object>> childList = new LinkedList<>();
			final String key                          = child.getKey();

			if ("has".equals(key)) {

				for (final AnnotatedConcept annotatedChildConcept : child.getValue()) {

					final Concept childConcept = annotatedChildConcept.getConcept();
					final Map<String, Object> childMap = new LinkedHashMap<>();

					childMap.put("name", childConcept.getName());
					childMap.put("type", childConcept.getType());

					childList.add(childMap);
				}

				links.add(Map.of(
					"name", child.getKey(),
					"targets", childList
				));
			}
		}

		lines.add(gson.toJson(data));

		return false;
	}
}
