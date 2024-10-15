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
import org.structr.common.error.ErrorBuffer;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.*;
import org.structr.knowledge.iso25964.relationship.*;

import java.util.Date;

/**
 * Class as defined in ISO 25964 data model
 */

public class ThesaurusConcept extends AbstractNode {

	public static final Property<Thesaurus> thesaurusProperty                                      = new StartNode<>("thesaurus", ThesauruscontainsThesaurusConcept.class);
	public static final Property<Iterable<Note>> notesAttributesProperty                           = new StartNodes<>("customConceptAttributesIn", NoterefersToThesaurusConcept.class);
	public static final Property<Iterable<ConceptGroup>> thesaurusConceptsProperty                 = new StartNodes<>("conceptGroups", ConceptGrouphasAsMemberThesaurusConcept.class);
	public static final Property<Iterable<ThesaurusConcept>> childConceptsProperty                 = new EndNodes<>("childConcepts", ThesaurusConcepthasTopConceptThesaurusConcept.class);
	public static final Property<Iterable<ThesaurusConcept>> topmostConceptsProperty               = new StartNodes<>("topmostConcepts", ThesaurusConcepthasTopConceptThesaurusConcept.class);
	public static final Property<Iterable<ThesaurusConcept>> relatedConceptsOutProperty            = new EndNodes<>("relatedConceptsIn", ThesaurusConcepthasRelatedConceptThesaurusConcept.class);
	public static final Property<Iterable<ThesaurusConcept>> relatedConceptsInProperty             = new StartNodes<>("relatedConcepts", ThesaurusConcepthasRelatedConceptThesaurusConcept.class);
	public static final Property<Iterable<SimpleNonPreferredTerm>> simpleNonPreferredTermsProperty = new EndNodes<>("simpleNonPreferredTerms", ThesaurusConcepthasNonPreferredLabelSimpleNonPreferredTerm.class);
	public static final Property<Iterable<PreferredTerm>> preferredTermsProperty                   = new EndNodes<>("preferredTerms", ThesaurusConcepthasPreferredLabelPreferredTerm.class);
	public static final Property<Iterable<ThesaurusArray>> subordinateArraysProperty               = new EndNodes<>("subordinateArrays", ThesaurusConcepthasSubordinateArrayThesaurusArray.class);
	public static final Property<Iterable<ThesaurusConcept>> hierarchicalChildConceptsProperty     = new EndNodes<>("childConceptsIn", ThesaurusConcepthasHierRelConceptThesaurusConcept.class);
	public static final Property<Iterable<ThesaurusConcept>> hierarchicalParentConceptProperty     = new StartNodes<>("parentConcepts", ThesaurusConcepthasHierRelConceptThesaurusConcept.class);
	public static final Property<Iterable<CustomConceptAttribute>> customConceptAttributesProperty = new EndNodes<>("customConceptAttributes", ThesaurusConcepthasCustomConceptAttributeCustomConceptAttribute.class);
	public static final Property<Iterable<CustomNote>> customNotesProperty                         = new EndNodes<>("customNotes", ThesaurusConcepthasCustomNoteCustomNote.class);
	public static final Property<Iterable<ScopeNote>> scopeNotesProperty                           = new EndNodes<>("scopeNotes", ThesaurusConcepthasScopeNoteScopeNote.class);
	public static final Property<Iterable<HistoryNote>> historyNotesProperty                       = new EndNodes<>("historyNotes", ThesaurusConcepthasHistoryNoteHistoryNote.class);
	public static final Property<Iterable<ThesaurusArray>> thesaurusArraysProperty                 = new StartNodes<>("thesaurusArrays", ThesaurusArrayhasMemberConceptThesaurusConcept.class);

	public static final Property<String> identifierProperty  = new StringProperty("identifier").indexed().notNull();
	public static final Property<Date> createdProperty       = new DateProperty("created");
	public static final Property<Date> modifiedProperty      = new DateProperty("modified");
	public static final Property<String> statusProperty      = new StringProperty("status");
	public static final Property<String[]> notationProperty  = new ArrayProperty("notation", String.class);
	public static final Property<Boolean> topConceptProperty = new BooleanProperty(    "topConcept");

	public static final View uiView      = new View(ThesaurusConcept.class, PropertyView.Ui,
		identifierProperty, createdProperty, modifiedProperty, statusProperty, notationProperty, topConceptProperty
	);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidPropertyNotNull(this, ThesaurusConcept.identifierProperty, errorBuffer);

		return valid;
	}
}
