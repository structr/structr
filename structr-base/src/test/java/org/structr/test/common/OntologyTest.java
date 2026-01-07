/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.common;

import org.structr.docs.ontology.AnnotatedConcept;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.ConceptType;
import org.structr.docs.ontology.Ontology;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.testng.AssertJUnit.assertNotNull;

public class OntologyTest extends StructrUiTest {

	@Test
	public void testOntology() {

		final List<String> facts = List.of(

			"\"Core System\" has service \"Cron Service\"",
			"topic \"Core System\" has capability \"Scheduled execution of business logic\"",
			"Area Pages has buttons Test1, Test2, \"Test Button\", Test3, Test-Button-4",
			"It has areas \"Page Tree\" and \"Localizations\"",
			"it has areas Widgets, Preview"
		);

		final Ontology ontology = new Ontology("testOntology", facts);
		final Concept concept   = ontology.getConcept(ConceptType.Unknown, "Core System");

		assertNotNull("concept should not be null", concept);

		final List<Concept> concepts = ontology.getAllConcepts();
		int i=0;

		assertEquals("Topic was not parsed correctly", "Unknown(Core System)",  concepts.get(i++).toString());
		assertEquals("Service was not parsed correctly", "Service(Cron Service)",  concepts.get(i++).toString());
		assertEquals("Topic was not parsed correctly", "Topic(Core System)",  concepts.get(i++).toString());
		assertEquals("Capability was not parsed correctly", "Capability(Scheduled execution of business logic)",  concepts.get(i++).toString());
		assertEquals("Area was not parsed correctly", "Area(Pages)",  concepts.get(i++).toString());
		assertEquals("Button list was not parsed correctly", "Button(Test1)",  concepts.get(i++).toString());
		assertEquals("Button list was not parsed correctly", "Button(Test2)",  concepts.get(i++).toString());
		assertEquals("Button list was not parsed correctly", "Button(Test Button)",  concepts.get(i++).toString());
		assertEquals("Button list was not parsed correctly", "Button(Test3)",  concepts.get(i++).toString());
		assertEquals("Button list was not parsed correctly", "Button(Test-Button-4)",  concepts.get(i++).toString());

		// "It" should reference the area "Pages" in the above list of facts, hence that area should have 9 "has" links
		assertEquals("\"It\" keyword does not reference last subject", 9, ontology.getConcept(ConceptType.Area, "Pages").getChildren().get("has").size());
	}

	@Test
	public void testUnsupportedPhrases() {

		try {
			new Ontology("testUnsupportedPhrases1", "Auth0 button uses Auth0 provider and has hint \"Only visible if Auth0 provider is configured in structr.conf\"");
			fail("Using compound phrases should fail.");
		} catch (RuntimeException e) { }

		try {
			new Ontology("testUnsupportedPhrases2", "Pages area has buttons one and two and has inputs three and four");
			fail("Using compound phrases should fail.");
		} catch (RuntimeException e) { }

	}

	@Test
	public void testAmbiguousIdentifiers() {

		final List<String> facts = List.of(
			"# References",
			"Topic References has topics Keywords, Functions, \"Lifecycle Methods\", \"System Types\", Services, \"Maintenance Commands\", Settings",

			"\"Keywords\" has code-source \"keywords\"",
			"\"Functions\" has code-source \"functions\"",
			"\"Lifecycle Methods\" has code-source \"lifecycle-methods\"",
			"\"System Types\" has code-source \"system-types\"",
			"\"Services\" has code-source \"services\"",
			"\"Maintenance Commands\" has code-source \"maintenance-commands\"",
			"\"Settings\" has code-source \"settings\""
		);

		final Ontology ontology = new Ontology("testAmbiguousIdentifiers", facts);

		assertNotNull(ontology.getConcept(ConceptType.Topic, "Services"));
	}

	@Test
	public void testInverseVerbs() {

		final List<String> facts = List.of(
			"Type Test isCreatedBy button Test"
		);

		final Ontology ontology = new Ontology("testInverseVerbs", facts);

		assertNotNull(ontology.getConcept(ConceptType.Type, "Test"));
		assertNotNull(ontology.getConcept(ConceptType.Button, "Test"));
		assertEquals("Type(Test)", ontology.getConcept(ConceptType.Button, "Test").getChildren().get("creates").get(0).toString());
		assertEquals("Button(Test)", ontology.getConcept(ConceptType.Type, "Test").getParents().get("iscreatedby").get(0).toString());
	}

	@Test
	public void testMarkdownFolders() {

		new Ontology("testMarkdownFolders", "topic Structr has markdown-folder \"1-Introduction\"");
	}

	@Test
	public void testJavascriptFile() {

		new Ontology("testJavascriptFile", "topic Structr has javascript-file \"structr/js/dashboard.js\"");
	}

	@Test
	public void testPrepositions() {

		final Ontology ontology  = new Ontology("testPrepositions", "topic Structr has topic \"Test\" with topic \"Added\"");

		for (final Concept concept : ontology.getAllConcepts()) {

			System.out.println(concept.getNameTypeAndLinks());
		}

		final Concept test = ontology.getConcept(ConceptType.Topic, "Test");

		assertNotNull(test);

		// information from the prepositional clause "with <type> <name>" is stored in the concept's metadata
		assertEquals("Added", test.getMetadata().get("topic"));
	}

	@Test
	public void testBlacklist() {

		final Ontology ontology  = new Ontology("testBlacklist", "blacklist One, Two, Three");

		for (final Concept concept : ontology.getAllConcepts()) {

			System.out.println(concept.getNameTypeAndLinks());
		}

		assertEquals(10, ontology.getBlacklist().size());
		assertEquals( 0, ontology.getAllConcepts().size());
	}

	@Test
	public void testCodeSources() {

		final Ontology ontology  = new Ontology("testCodeSources", "topic Structr has code-source \"rest-endpoints\"");

		for (final Concept concept : ontology.getAllConcepts()) {

			System.out.println(concept.getNameTypeAndLinks());
		}
	}

	@Test
	public void testPrepositionBinding1() {

		final Ontology ontology  = new Ontology("testPrepositionBinding1", """
			Topic "Business Logic" has topics "Dynamic types", "Relationships", "Built-in types" as table, "Built-in properties", "Scripting contexts", "Advanced find"
			""");

		for (final Concept concept : ontology.getAllConcepts()) {

			System.out.println(concept.getNameTypeAndLinks());
		}
	}

	@Test
	public void testPrepositionBinding2() {

		final Ontology ontology  = new Ontology("testPrepositionBinding2", """
			Topic "Business Logic" has markdown-file "snippets/Business Logic.md" with heading "Overview"
			""");

		for (final Concept concept : ontology.getAllConcepts()) {

			System.out.println(concept.getNameTypeAndLinks());
		}
	}

	@Test
	public void testGetGroupedChildren() {

		final Ontology ontology  = new Ontology("testGetGroupedChildren", "Structr has code-source \"system-type\"");
		ontology.initializeFromDocumentationAnnotations();

		final Concept root = ontology.getConcept(ConceptType.Unknown, "File");

		System.out.println(root.getGroupedChildren());

	}

	@Test
	public void testGlossary() {

		final Ontology ontology  = new Ontology("testGlossary", List.of(
			"Structr has topics \"Glossary\"",
			"\"Glossary\" has glossary \"Glossary\""
		));

		ontology.initializeFromDocumentationAnnotations();

		System.out.println(ontology.getConceptsByName("Glossary"));

	}


	// ----- private methods -----
	private void printConcepts(final List<Concept> concepts) {

		for (final Concept c : concepts) {

			System.out.println(c);

			final Map<String, List<AnnotatedConcept>> links = c.getChildren();

			for (final String key : links.keySet()) {

				System.out.println("        " + key + ": " + links.get(key));
			}
		}
	}
}