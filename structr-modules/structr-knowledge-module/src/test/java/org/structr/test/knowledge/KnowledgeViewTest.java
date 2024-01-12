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
package org.structr.test.knowledge;

import org.structr.test.web.StructrUiTest;
import org.structr.web.common.TestHelper;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 */
public class KnowledgeViewTest extends StructrUiTest {

	@Test
	public void testViews() {

		final Map<String, List<String>> additionalRequiredAttributes = new HashMap<>();

		additionalRequiredAttributes.put("ConceptGroup", Arrays.asList("identifier", "conceptGroupType"));
		additionalRequiredAttributes.put("ConceptGroupLabel", Arrays.asList("lexicalValue"));
		additionalRequiredAttributes.put("CustomConceptAttribute", Arrays.asList("lexicalValue", "customAttributeType"));
		additionalRequiredAttributes.put("CustomNote", Arrays.asList("lexicalValue"));
		additionalRequiredAttributes.put("CustomTermAttribute", Arrays.asList("lexicalValue", "customAttributeType"));
		additionalRequiredAttributes.put("Definition", Arrays.asList("lexicalValue"));
		additionalRequiredAttributes.put("EditorialNote", Arrays.asList("lexicalValue"));
		additionalRequiredAttributes.put("NodeLabel", Arrays.asList("lexicalValue"));
		additionalRequiredAttributes.put("Note", Arrays.asList("lexicalValue"));
		additionalRequiredAttributes.put("PreferredTerm", Arrays.asList("lexicalValue", "identifier"));
		additionalRequiredAttributes.put("ScopeNote", Arrays.asList("lexicalValue"));
		additionalRequiredAttributes.put("SimpleNonPreferredTerm", Arrays.asList("lexicalValue", "identifier"));
		additionalRequiredAttributes.put("SplitNonPreferredTerm", Arrays.asList("lexicalValue", "identifier"));
		additionalRequiredAttributes.put("Thesaurus", Arrays.asList("identifier", "lang:lang"));
		additionalRequiredAttributes.put("ThesaurusArray", Arrays.asList("identifier", "ordered"));
		additionalRequiredAttributes.put("ThesaurusConcept", Arrays.asList("identifier"));
		additionalRequiredAttributes.put("ThesaurusTerm", Arrays.asList("lexicalValue", "identifier"));
		additionalRequiredAttributes.put("VersionHistory", Arrays.asList("thisVersion"));


		TestHelper.testViews(app, KnowledgeViewTest.class.getResourceAsStream("/views.properties"), additionalRequiredAttributes);
	}
}
