/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.knowledge.iso25964.relationship;

import org.structr.core.entity.OneToMany;
import org.structr.knowledge.iso25964.PreferredTerm;
import org.structr.knowledge.iso25964.ThesaurusConcept;

public class ThesaurusConcepthasPreferredLabelPreferredTerm extends OneToMany<ThesaurusConcept, PreferredTerm> {

	@Override
	public Class<ThesaurusConcept> getSourceType() {
		return ThesaurusConcept.class;
	}

	@Override
	public Class<PreferredTerm> getTargetType() {
		return PreferredTerm.class;
	}

	@Override
	public String name() {
		return "hasPreferredLabel";
	}
}