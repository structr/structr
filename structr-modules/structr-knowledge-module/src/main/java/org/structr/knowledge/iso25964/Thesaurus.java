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
import org.structr.core.property.*;
import org.structr.knowledge.iso25964.relationship.ThesauruscontainsConceptGroup;
import org.structr.knowledge.iso25964.relationship.ThesauruscontainsThesaurusArray;
import org.structr.knowledge.iso25964.relationship.ThesauruscontainsThesaurusConcept;
import org.structr.knowledge.iso25964.relationship.ThesaurushasVersionVersionHistory;

import java.util.Date;

/**
 * Class as defined in ISO 25964 data model
 */

public class Thesaurus extends AbstractNode {

	public static final Property<Iterable<ConceptGroup>> conceptGroupsProperty         = new EndNodes<>("conceptGroups", ThesauruscontainsConceptGroup.class);
	public static final Property<Iterable<VersionHistory>> versionsProperty            = new EndNodes<>("versions", ThesaurushasVersionVersionHistory.class);
	public static final Property<Iterable<ThesaurusArray>> thesaurusArraysProperty     = new EndNodes<>("thesaurusArrays", ThesauruscontainsThesaurusArray.class);
	public static final Property<Iterable<ThesaurusConcept>> thesaurusConceptsProperty = new EndNodes<>("concepts", ThesauruscontainsThesaurusConcept.class);

	public static final Property<String[]> identifierProperty    = new ArrayProperty<>("identifier", String.class).indexed().notNull();
	public static final Property<String[]> contributorProperty   = new ArrayProperty<>("contributor", String.class);
	public static final Property<String[]> coverageProperty      = new ArrayProperty<>("coverage", String.class);
	public static final Property<String[]> creatorProperty       = new ArrayProperty<>("creator", String.class);
	public static final Property<Date[]> dateProperty            = new DateArrayProperty("date");
	public static final Property<Date> createdProperty           = new DateProperty("created");
	public static final Property<String[]> descriptionProperty   = new ArrayProperty<>("description", String.class);
	public static final Property<String[]> formatProperty        = new ArrayProperty<>("format", String.class);
	public static final Property<String> langProperty            = new EnumProperty("lang", ThesaurusTerm.Lang.class).notNull();
	public static final Property<String[]> publisherProperty     = new ArrayProperty<>("publisher", String.class);

	public static final Property<String[]> relationProperty      = new ArrayProperty<>("relation", String.class);
	public static final Property<String[]> rightsProperty        = new ArrayProperty<>("rights", String.class);
	public static final Property<String[]> sourceProperty        = new ArrayProperty<>("source", String.class);
	public static final Property<String[]> subjectProperty       = new ArrayProperty<>("subject", String.class);
	public static final Property<String[]> titleProperty         = new ArrayProperty<>("title", String.class);
	public static final Property<String[]> thesaurusTypeProperty = new ArrayProperty<>("thesaurusType", String.class);

	public static final View uiView      = new View(Thesaurus.class, PropertyView.Ui,
		identifierProperty, contributorProperty, coverageProperty, createdProperty, dateProperty, createdProperty, descriptionProperty, formatProperty,
		langProperty, publisherProperty, relationProperty, rightsProperty, sourceProperty, subjectProperty, titleProperty, thesaurusTypeProperty
	);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidPropertyNotNull(this, Thesaurus.identifierProperty, errorBuffer);
		valid &= ValidationHelper.isValidPropertyNotNull(this, Thesaurus.langProperty, errorBuffer);

		return valid;
	}
}
