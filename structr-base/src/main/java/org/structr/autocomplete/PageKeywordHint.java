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
package org.structr.autocomplete;

import org.structr.docs.Documentation;
import org.structr.docs.ontology.ConceptType;

import java.util.List;

@Documentation(name="Page keywords", shortDescription="Built-in keywords that are available in page rendering contexts.")
public abstract class PageKeywordHint extends KeywordHint {

	@Override
	public List<String> getNotes() {
		return List.of(
			"This keywords is only available in Page elements (DOM nodes)."
		);
	}

	@Override
	public List<ConceptReference> getParentConcepts() {
		return List.of(ConceptReference.of(ConceptType.Topic, "Page keywords"));
	}
}
