/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.ManyToMany;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.knowledge.iso25964.ThesaurusConcept;

public class ThesaurusConcepthasHierRelConceptThesaurusConcept extends ManyToMany<ThesaurusConcept, ThesaurusConcept> {

	public static final Property<String> roleProperty = new StringProperty("role").indexed().notNull();

	public static final View defaultView = new View(ThesaurusConcepthasHierRelConceptThesaurusConcept.class, PropertyView.Public,
		roleProperty
	);

	public static final View uiView      = new View(ThesaurusConcepthasHierRelConceptThesaurusConcept.class, PropertyView.Ui,
		roleProperty
	);

	@Override
	public Class<ThesaurusConcept> getSourceType() {
		return ThesaurusConcept.class;
	}

	@Override
	public Class<ThesaurusConcept> getTargetType() {
		return ThesaurusConcept.class;
	}

	@Override
	public String name() {
		return "hasHierRelConcept";
	}
}
