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
package org.structr.knowledge.iso25964;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;
import org.structr.knowledge.iso25964.relationship.ConceptGrouphasAsMemberThesaurusConcept;
import org.structr.knowledge.iso25964.relationship.ConceptGrouphasConceptGroupLabelConceptGroupLabel;
import org.structr.knowledge.iso25964.relationship.ConceptGrouphasSubGroupConceptGroup;
import org.structr.knowledge.iso25964.relationship.ThesauruscontainsConceptGroup;

/**
 * Class as defined in ISO 25964 data model
 */
public class ConceptGroup extends AbstractNode {

	public static final Property<Iterable<Thesaurus>> thesaurusProperty = new StartNodes<>("thesaurus", ThesauruscontainsConceptGroup.class);

	public static final Property<Iterable<ConceptGroup>> subGroupsProperty   = new EndNodes<>("subGroups", ConceptGrouphasSubGroupConceptGroup.class);
	public static final Property<Iterable<ConceptGroup>> superGroupsProperty = new StartNodes<>("superGroups", ConceptGrouphasSubGroupConceptGroup.class);

	public static final Property<Iterable<ThesaurusConcept>> thesaurusConceptsProperty   = new EndNodes<>("thesaurusConcepts", ConceptGrouphasAsMemberThesaurusConcept.class);
	public static final Property<Iterable<ConceptGroupLabel>> conceptGroupLabelsProperty = new EndNodes<>("conceptGroupLabels", ConceptGrouphasConceptGroupLabelConceptGroupLabel.class);

	public static final Property<String[]> identifierProperty       = new ArrayProperty("identifier", String.class).indexed().notNull();
	public static final Property<String[]> conceptGroupTypeProperty = new ArrayProperty("conceptGroupType", String.class).indexed().notNull();
	public static final Property<String[]> notationProperty         = new ArrayProperty("notation", String.class);

	public static final View uiView = new View(ConceptGroup.class, PropertyView.Ui,
		identifierProperty, conceptGroupTypeProperty, notationProperty
	);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidPropertyNotNull(this, ConceptGroup.conceptGroupTypeProperty, errorBuffer);
		valid &= ValidationHelper.isValidPropertyNotNull(this, ConceptGroup.identifierProperty, errorBuffer);

		return valid;
	}
}
