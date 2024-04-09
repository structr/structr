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

import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonReferenceType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;

import java.net.URI;

/**
 * Class as defined in ISO 25964 data model
 */

public interface ThesaurusConcept extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema            = SchemaService.getDynamicSchema();

		final JsonObjectType type                   = schema.addType("ThesaurusConcept");
		final JsonObjectType simpleTerm             = schema.addType("SimpleNonPreferredTerm");
		final JsonObjectType prefTerm               = schema.addType("PreferredTerm");
		final JsonObjectType thesArray              = schema.addType("ThesaurusArray");
		final JsonObjectType customConceptAttribute = schema.addType("CustomConceptAttribute");
		final JsonObjectType customNote             = schema.addType("CustomNote");
		final JsonObjectType scopeNote              = schema.addType("ScopeNote");
		final JsonObjectType historyNote            = schema.addType("HistoryNote");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/ThesaurusConcept"));

		type.addStringProperty(     "identifier", PropertyView.All, PropertyView.Ui).setIndexed(true).setRequired(true);
		type.addDateProperty(       "created",    PropertyView.All, PropertyView.Ui).setIndexed(true);
		type.addDateProperty(       "modified",   PropertyView.All, PropertyView.Ui).setIndexed(true);
		type.addStringProperty(     "status",     PropertyView.All, PropertyView.Ui).setIndexed(true);
		type.addStringArrayProperty("notation",   PropertyView.All, PropertyView.Ui).setIndexed(true);
		type.addBooleanProperty(    "topConcept", PropertyView.All, PropertyView.Ui).setIndexed(true);

		type.relate(type,                   "hasTopConcept",             Cardinality.ManyToMany, "childConcepts",        "topmostConcept");
		type.relate(type,                   "hasRelatedConcept",         Cardinality.ManyToMany, "relatedConcepts",      "relatedConcepts");
		type.relate(simpleTerm,             "hasNonPreferredLabel",      Cardinality.OneToMany,  "concepts",             "simpleNonPreferredTerms");
		type.relate(prefTerm,               "hasPreferredLabel",         Cardinality.OneToMany,  "concepts",             "preferredTerms");
		type.relate(thesArray,              "hasSubordinateArray",       Cardinality.OneToMany,  "superordinateConcept", "subordinateArrays");
		final JsonReferenceType hierarchichalRelationship = type.relate(type, "hasHierRelConcept", Cardinality.ManyToMany, "parentConcepts", "childConcepts");
		hierarchichalRelationship.addStringProperty("role", PropertyView.All, PropertyView.Ui).setIndexed(true).setRequired(true);
		type.relate(customConceptAttribute, "hasCustomConceptAttribute", Cardinality.OneToMany, "concept", "customConceptAttributes");
		type.relate(customNote,             "hasCustomNote",             Cardinality.OneToMany, "concept", "customNotes");
		type.relate(scopeNote,              "hasScopeNote",              Cardinality.OneToMany, "concept", "scopeNotes");
		type.relate(historyNote,            "hasHistoryNote",            Cardinality.OneToMany, "concept", "historyNotes");
	}}
}
