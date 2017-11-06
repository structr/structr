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
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StartNodes;
import org.structr.schema.SchemaService;

/**
 * Base class of a thesaurus concept as defined in ISO 25964
 */

public class ThesaurusConcept extends AbstractNode {
	
	private static final Logger logger = LoggerFactory.getLogger(ThesaurusConcept.class.getName());
	
	public static final Property<Thesaurus> thesaurus                = new StartNode<>("thesaurus", ThesaurusContainsConcepts.class);
	public static final Property<List<PreferredTerm>> preferredTerms = new EndNodes<>("preferredTerms", ConceptPreferredTerm.class);
	public static final Property<List<ThesaurusTerm>> terms          = new EndNodes<>("terms", ConceptTerm.class);
	
	public static final Property<List<ThesaurusConcept>> broaderTerms = new EndNodes<>("broaderTerms", ConceptBTConcept.class);
	public static final Property<List<ThesaurusConcept>> narrowerTerms = new StartNodes<>("narrowerTerms", ConceptBTConcept.class);
	
	public static final Property<List<ThesaurusConcept>> relatedTerms   = new EndNodes<>("relatedTerms", ConceptRTConcept.class);
	public static final Property<List<ThesaurusConcept>> relatedTermsOf = new StartNodes<>("relatedTermsOf", ConceptRTConcept.class);
	
	

	public static final org.structr.common.View uiView = new org.structr.common.View(ThesaurusConcept.class, PropertyView.Ui,
		type, name, thesaurus, preferredTerms, terms, broaderTerms, narrowerTerms, relatedTerms, relatedTermsOf
	);

	public static final org.structr.common.View publicView = new org.structr.common.View(ThesaurusConcept.class, PropertyView.Public,
		type, name, thesaurus, preferredTerms, terms, broaderTerms, narrowerTerms, relatedTerms, relatedTermsOf
	);

	static {

		SchemaService.registerBuiltinTypeOverride("ThesaurusConcept", ThesaurusConcept.class.getName());
	}	
}
