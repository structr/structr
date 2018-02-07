/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.knowledge;

import java.net.URI;
import org.structr.common.PropertyView;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;

/**
 * Base class of a thesaurus term as defined in ISO 25964
 */

public interface ThesaurusTerm extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("ThesaurusTerm");
		final JsonObjectType attr = schema.addType("CustomTermAttribute");
		final JsonObjectType lang = schema.addType("Language");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/ThesaurusTerm"));

		type.addStringArrayProperty("normalizedWords", PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("lang",                 PropertyView.Public, PropertyView.Ui);

		type.relate(attr, "HAS_CUSTOM_ATTRIBUTE", Cardinality.OneToMany, "term", "customAttributes");
		type.relate(lang, "HAS_LABEL",            Cardinality.OneToMany, "term", "preferredLabels");

		type.addViewProperty(PropertyView.Public, "name");
		type.addViewProperty(PropertyView.Public, "concept");
		type.addViewProperty(PropertyView.Public, "customAttributes");
		type.addViewProperty(PropertyView.Public, "normalizedWords");

		type.addViewProperty(PropertyView.Ui, "name");
		type.addViewProperty(PropertyView.Ui, "concept");
		type.addViewProperty(PropertyView.Ui, "customAttributes");
	}}

	/*

	private static final Logger logger = LoggerFactory.getLogger(ThesaurusTerm.class.getName());

	public static final Property<ThesaurusConcept> concept = new StartNode<>("concept", ConceptTerm.class);
	public static final Property<String>   name            = new StringProperty("name").indexedWhenEmpty().cmis().notNull();
	public static final Property<String[]> normalizedWords = new ArrayProperty("normalizedWords", String.class).indexedWhenEmpty();
	public static final Property<String>   lang            = new StringProperty("lang");

	public static final Property<List<CustomTermAttribute>> customAttributes = new EndNodes<>("customAttributes", TermHasCustomAttributes.class);

	public static final org.structr.common.View uiView = new org.structr.common.View(ThesaurusTerm.class, PropertyView.Ui,
		type, name, normalizedWords, lang, concept, customAttributes
	);

	public static final org.structr.common.View publicView = new org.structr.common.View(ThesaurusTerm.class, PropertyView.Public,
		type, name, normalizedWords, lang, concept, customAttributes
	);

	static {

		SchemaService.registerBuiltinTypeOverride("ThesaurusTerm", ThesaurusTerm.class.getName());
	}
	*/
}
