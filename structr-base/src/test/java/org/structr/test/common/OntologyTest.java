/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.structr.docs.ontology.*;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.testng.AssertJUnit.assertNotNull;

public class OntologyTest extends StructrUiTest {

	@Test
	public void testOntology() {

		final String facts = """
			"Core System" has service "Cron Service"
			topic "Core System" has capability "Scheduled execution of business logic"
			Area Pages has buttons Test1, Test2, "Test Button", Test3, Test-Button-4
			It has areas "Page Tree" and "Localizations"
			it has areas Widgets, Preview"
		""";

		final Ontology ontology = new Ontology(new TestFacts("testOntology", facts));
		final Concept concept   = ontology.getConcept(ConceptType.Unknown, "Core System").get(0);

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
		assertEquals("\"It\" keyword does not reference last subject", 9, ontology.getConcept(ConceptType.Area, "Pages").get(0).getChildLinks().get(Verb.Has).size());
	}

	@Test
	public void testUnsupportedPhrases() {

		try {
			new Ontology(new TestFacts("testUnsupportedPhrases1", "Auth0 button uses Auth0 provider and has hint \"Only visible if Auth0 provider is configured in structr.conf\""));
			fail("Using compound phrases should fail.");
		} catch (RuntimeException e) { }

		try {
			new Ontology(new TestFacts("testUnsupportedPhrases2", "Pages area has buttons one and two and has inputs three and four"));
			fail("Using compound phrases should fail.");
		} catch (RuntimeException e) { }

	}

	@Test
	public void testAmbiguousIdentifiers() {

		final String facts = """
			# References
			Topic References has topics Keywords, Functions, "Lifecycle Methods", "System Types", Services, "Maintenance Commands", Settings

			"Keywords" has code-source "keywords"
			"Functions" has code-source "functions"
			"Lifecycle Methods" has code-source "lifecycle-methods"
			"System Types" has code-source "system-types"
			"Services" has code-source "services"
			"Maintenance Commands" has code-source "maintenance-commands"
			"Settings" has code-source "settings"
		""";

		final Ontology ontology = new Ontology(new TestFacts("testAmbiguousIdentifiers", facts));

		assertNotNull(ontology.getConcept(ConceptType.Topic, "Services"));
	}

	@Test
	public void testInverseVerbs() {

		final String facts = "Type Test isCreatedBy button Test";

		final Ontology ontology = new Ontology(new TestFacts("testInverseVerbs", facts));

		assertNotNull(ontology.getConcept(ConceptType.Type, "Test"));
		assertNotNull(ontology.getConcept(ConceptType.Button, "Test"));
		assertEquals("Link(Button(Test) Creates Type(Test))", ontology.getConcept(ConceptType.Button, "Test").get(0).getChildLinks().get(Verb.Creates).get(0).toString());
		assertEquals("Link(Button(Test) Creates Type(Test))", ontology.getConcept(ConceptType.Type, "Test").get(0).getParentLinks().get(Verb.Creates).get(0).toString());
	}

	@Test
	public void testMarkdownFolders() {

		new Ontology(new TestFacts("testMarkdownFolders", "topic Structr has markdown-folder \"1-Introduction\""));
	}

	@Test
	public void testJavascriptFile() {

		new Ontology(new TestFacts("testJavascriptFile", "topic Structr has javascript-file \"structr/js/dashboard.js\""));
	}

	@Test
	public void testWithKeyword() {

		final Ontology ontology  = new Ontology(new TestFacts("testWithKeyword", "topic Structr has topic \"Test\" with topic \"Added\""));

		for (final Concept concept : ontology.getAllConcepts()) {

			System.out.println(concept.getNameTypeAndLinks());
		}

		final Concept test = ontology.getConcept(ConceptType.Topic, "Test").get(0);

		assertNotNull(test);

		// information from the prepositional clause "with <type> <name>" is stored in the concept's metadata
		assertEquals("Added", test.getMetadata().get("topic"));
	}

	@Test
	public void testAsKeyword() {

		final Ontology ontology  = new Ontology(new TestFacts("testAsKeyword", "topic Structr has topic \"File types\" as table"));

		for (final Concept concept : ontology.getAllConcepts()) {

			System.out.println(concept.getNameTypeAndLinks());
		}

		final Concept test = ontology.getConcept(ConceptType.Topic, "Structr").get(0);

		assertNotNull(test);

		assertEquals(ConceptType.Table, test.getChildLinks(Verb.Has).get(0).getFormatSpecification().getFormat());
	}

	@Test
	public void testBlacklist() {

		final Ontology ontology  = new Ontology(new TestFacts("testBlacklist", """
			blacklist "10pt", "1rem", "80px", "8212", "300px", "500px", Action, Advanced, Boolean, Byte, Copy, Create, Date, Delete, Description, Double, Filter, Filters, Float, Integer, List, Load, Long
			blacklist Name, None, Options, Parameters, Refresh, Save, Select, String, Style, Type, absolute, auto, black, checkbox, class, controls, crud, custom, desc, error
			blacklist favorites, hint, href, info, input, inter, invalid, json, label, loose, monaco, name, nodeHover, number, numeric, plain, pointer, quot, right, root
		"""));

		for (final Concept concept : ontology.getAllConcepts()) {

			System.out.println(concept.getNameTypeAndLinks());
		}

		assertEquals(70, ontology.getBlacklist().size());
		assertEquals( 0, ontology.getAllConcepts().size());
	}

	@Test
	public void testCodeSources() {

		final Ontology ontology  = new Ontology(new TestFacts("testCodeSources", "topic Structr has code-source \"rest-endpoints\""));

		for (final Concept concept : ontology.getAllConcepts()) {

			System.out.println(concept.getNameTypeAndLinks());
		}
	}

	@Test
	public void testPrepositionBinding1() {

		final Ontology ontology  = new Ontology(new TestFacts("testPrepositionBinding1", """
			Topic "Business Logic" has topics "Dynamic types", "Relationships", "Built-in types" as table, "Built-in properties", "Scripting contexts", "Advanced find"
			"""));

		for (final Concept concept : ontology.getAllConcepts()) {

			System.out.println(concept.getNameTypeAndLinks());
		}
	}

	@Test
	public void testPrepositionBinding2() {

		final Ontology ontology  = new Ontology(new TestFacts("testPrepositionBinding2", """
			Topic "Business Logic" has markdown-file "snippets/Business Logic.md" with heading "Overview"
			"""));

		for (final Concept concept : ontology.getAllConcepts()) {

			System.out.println(concept.getNameTypeAndLinks());
		}
	}

	@Test
	public void testGetGroupedChildren() {

		final Ontology ontology  = new Ontology(new TestFacts("testGetGroupedChildren", "Structr has code-source \"system-type\""));
		ontology.initializeFromDocumentationAnnotations();

		final Concept root = ontology.getConcept(ConceptType.Unknown, "File").get(0);

		System.out.println(root.getGroupedChildren());
	}

	@Test
	public void testGlossary() {

		final Ontology ontology  = new Ontology(new TestFacts("testGlossary", """
			Structr has topics "Glossary"
			"Glossary" has glossary "Glossary"
		"""));

		ontology.initializeFromDocumentationAnnotations();

		System.out.println(ontology.getConceptsByName("Glossary"));
	}

	@Test
	public void testExistingKeyword() {

		final Ontology ontology  = new Ontology(new TestFacts("testExistingKeyword", """
			"Structr" has topics "One", "Two", "Three"
			One has topic "Authentication"
			Two has topic "Authentication"
			Three has new topic "Authentication"
		"""));

		System.out.println(ontology.getAllConcepts());
	}

	@Test
	public void testMultilineFacts() {

		final String facts = """
			#
			# testMultilineFacts.txt
			#
			
			"Structr" has topics "One", "Two", "Three"
			One has topic "Authentication"
			Two has topic "Authentication"
			Three has new topic "Authentication"
			""";

		final Ontology ontology  = new Ontology(new TestFacts("testMultilineFacts", facts));

		System.out.println(ontology.getAllConcepts());
	}

	@Test
	public void testFactsFile() {

		final String src = """
		Structr has topics "One", "Two", "Three"
			Topic "One" has topic "One.1"
			Topic "One" has topic "One.2" as table
			Topic "Two" has topic "Two.2"
		""";

		final TestFacts facts   = new TestFacts("test", src);
		final Ontology ontology = new Ontology(facts);

		for (final Concept concept : ontology.getConceptsByName("One")) {

			concept.renameTo("TEST");
			concept.getChildLinks(Verb.Has).get(0).setFormat(ConceptType.List);
		}

		for (final Concept concept : ontology.getConceptsByName("Structr")) {

			final List<Link> children = concept.getChildLinks(Verb.Has);

			/*
			children.addChild("Juhu!");
			children.removeChild(2);
			children.moveChild(0, 2);
			*/
		}



		System.out.println(facts.toString());
	}
}