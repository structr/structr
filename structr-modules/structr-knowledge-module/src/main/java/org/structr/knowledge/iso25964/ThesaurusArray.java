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
package org.structr.knowledge.iso25964;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.*;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.helper.ValidationHelper;
import org.structr.knowledge.iso25964.relationship.*;

/**
 * Class as defined in ISO 25964 data model
 */
public class ThesaurusArray extends AbstractNode {

	public static final Property<Thesaurus> thesaurusProperty                       = new StartNode<>("thesaurus", ThesauruscontainsThesaurusArray.class);
	public static final Property<ThesaurusConcept> superordinateConceptProperty     = new StartNode<>("superordinateConcept", ThesaurusConcepthasSubordinateArrayThesaurusArray.class);
	public static final Property<Iterable<NodeLabel>> nodeLabelsProperty            = new EndNodes<>("nodeLabels", ThesaurusArrayhasNodeLabelNodeLabel.class);
	public static final Property<ThesaurusArray> superordinateArrayProperty         = new StartNode<>("superOrdinateArray", ThesaurusArrayhasMemberArrayThesaurusArray.class);
	public static final Property<Iterable<ThesaurusArray>> memberArraysProperty     = new EndNodes<>("memberArrays", ThesaurusArrayhasMemberArrayThesaurusArray.class);
	public static final Property<Iterable<ThesaurusConcept>> memberConceptsProperty = new EndNodes<>("memberConcepts", ThesaurusArrayhasMemberConceptThesaurusConcept.class);

	public static final Property<String> identifierProperty = new StringProperty("identifier").indexed().notNull();
	public static final Property<Boolean> orderedProperty   = new BooleanProperty("ordered").defaultValue(false).notNull();
	public static final Property<String[]> notationProperty = new ArrayProperty("notation", String.class);

	public static final View uiView      = new View(ThesaurusArray.class, PropertyView.Ui,
		identifierProperty, orderedProperty, notationProperty
	);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidPropertyNotNull(this, ThesaurusArray.orderedProperty, errorBuffer);
		valid &= ValidationHelper.isValidPropertyNotNull(this, ThesaurusArray.identifierProperty, errorBuffer);

		return valid;
	}
}
