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
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;

import java.net.URI;
import java.util.Locale;

/**
 * Class as defined in ISO 25964 data model
 */

public interface Thesaurus extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema      = SchemaService.getDynamicSchema();
		final JsonObjectType type    = schema.addType("Thesaurus");
		final JsonObjectType concept = schema.addType("ThesaurusConcept");
		final JsonObjectType array   = schema.addType("ThesaurusArray");
		final JsonObjectType group   = schema.addType("ConceptGroup");
		final JsonObjectType version = schema.addType("VersionHistory");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Thesaurus"));

		type.addStringArrayProperty("identifier", PropertyView.All, PropertyView.Ui).setIndexed(true).setRequired(true);
		type.addStringArrayProperty("contributor", PropertyView.All, PropertyView.Ui).setIndexed(true);
		type.addStringArrayProperty("coverage", PropertyView.All, PropertyView.Ui).setIndexed(true);
		type.addStringArrayProperty("creator", PropertyView.All, PropertyView.Ui).setIndexed(true);
		type.addDateArrayProperty("date", PropertyView.All, PropertyView.Ui).setIndexed(true);
		type.addDateProperty("created", PropertyView.All, PropertyView.Ui).setIndexed(true);
		type.addStringArrayProperty("description", PropertyView.All, PropertyView.Ui).setIndexed(true);
		type.addStringArrayProperty("format", PropertyView.All, PropertyView.Ui).setIndexed(true);
		type.addEnumProperty("lang", PropertyView.All, PropertyView.Ui).setEnums(Locale.getISOLanguages()).setRequired(true);
		type.addStringArrayProperty("publisher", PropertyView.All, PropertyView.Ui).setIndexed(true);

		type.addStringArrayProperty("relation", PropertyView.All, PropertyView.Ui).setIndexed(true);
		type.addStringArrayProperty("rights", PropertyView.All, PropertyView.Ui).setIndexed(true);
		type.addStringArrayProperty("source", PropertyView.All, PropertyView.Ui).setIndexed(true);
		type.addStringArrayProperty("subject", PropertyView.All, PropertyView.Ui).setIndexed(true);
		type.addStringArrayProperty("title", PropertyView.All, PropertyView.Ui).setIndexed(true);
		type.addStringArrayProperty("thesaurusType", PropertyView.All, PropertyView.Ui).setIndexed(true);

		type.relate(group,   "contains",   Cardinality.ManyToMany, "thesaurus", "conceptGroups");
		type.relate(version, "hasVersion", Cardinality.OneToMany,  "thesaurus", "versions");
		type.relate(array,   "contains",   Cardinality.OneToMany,  "thesaurus", "thesaurusArrays");
		type.relate(concept, "contains",   Cardinality.OneToMany,  "thesaurus", "concepts");
	}}
}
