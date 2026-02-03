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

import org.structr.api.util.Category;
import org.structr.docs.DocumentableType;
import org.structr.docs.ontology.ConceptType;

import java.util.List;

public abstract class BuiltinFunctionHint extends AbstractHint {

	@Override
	public String getDisplayName(boolean includeParameters) {

		if (includeParameters) {

			// show method with signature right away
			return getName() + "(" + getFirstSignature() + ")";
		}

		return getName() + "()";
	}

	@Override
	public DocumentableType getDocumentableType() {
		return DocumentableType.BuiltInFunction;
	}

	@Override
	public List<ConceptReference> getParentConcepts() {

		final List<ConceptReference> concepts = super.getParentConcepts();

		final Category category = getCategory();
		if (category != null) {

			final String displayName = category.getDisplayName();
			if (displayName != null) {

				concepts.add(ConceptReference.of(ConceptType.Topic, displayName + " functions"));
			}
		}

		return concepts;
	}
}
