/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.schema.SchemaService;

/**
 * Base class of a thesaurus term as defined in ISO 25964
 */

public class ThesaurusTerm extends AbstractNode {
	
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
}
