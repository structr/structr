/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.process.bpmn;

import org.structr.process.traits.definitions.VisibilityMappingTraitDefinition;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.structr.process.bpmn.BpmnPageSkeletonGenerator.firstFree;
import static org.structr.process.bpmn.BpmnPageSkeletonGenerator.idPrefixFor;
import static org.structr.process.bpmn.BpmnPageSkeletonGenerator.slug;
import static org.structr.process.bpmn.BpmnPageSkeletonGenerator.visibleWhenFor;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Naming and step-selection rules of the page-skeleton generator. Pure logic, no graph /
 * database needed.
 */
public class BpmnPageSkeletonLogicTest {

	/** HTML4-safe id: starts with a letter, then letters / digits / dashes. */
	private static final Pattern SAFE_ID = Pattern.compile("[a-z][a-z0-9-]*");

	/**
	 * Slugs come from the platform's {@code Functions.cleanString} (the engine behind
	 * {@code $.clean}), which is also what builds the start-process URL's page-name
	 * fallback -- so a generated page's name matches the URL that falls back to it. These
	 * cases pin the properties the generator depends on, including the German umlaut
	 * expansion the process models in question rely on.
	 */
	@Test
	public void testSlugFoldsAccentsAndPunctuation() {

		assertEquals("lieferant-bewerten",         slug("Lieferant bewerten", "x"));
		assertEquals("antragsformular-ausfuellen", slug("Antragsformular ausfüllen", "x"));
		assertEquals("massnahme",                  slug("Maßnahme", "x"));
		assertEquals("budget-ausreichend",         slug("Budget ausreichend?", "x"));
		assertEquals("betrag-eur",                 slug("Betrag (EUR)", "x"));
		assertEquals("a-b",                        slug("a  ---  b", "x"));
		assertEquals("evaluation-terminee",        slug("Évaluation terminée", "x"));

		// every result is usable as an HTML id once prefixed by step kind
		for (final String name : new String[] { "Lieferant bewerten", "Antragsformular ausfüllen", "Maßnahme", "Betrag (EUR)", "1. Schritt" }) {

			final String id = "task-" + slug(name, "fallback");
			assertTrue("'" + id + "' is not a safe html id", SAFE_ID.matcher(id).matches());
		}
	}

	/** A name with nothing mappable still has to produce an id -- the bpmnId, then a constant. */
	@Test
	public void testSlugFallsBackWhenNothingRemains() {

		assertEquals("usertask-1", slug("!!!", "UserTask_1"));
		assertEquals("usertask-1", slug(null, "UserTask_1"));
		assertEquals("usertask-1", slug("", "UserTask_1"));
		assertEquals("step",       slug("???", "***"));
		assertEquals("step",       slug(null, null));
	}

	/**
	 * Two steps can carry the same name ("Freigabe" twice is normal), but their divs cannot
	 * carry the same id -- and two generated pages cannot carry the same name, since page
	 * names route requests.
	 */
	@Test
	public void testFirstFreeDisambiguatesRepeatedNames() {

		final Set<String> used = new HashSet<>();

		for (final String expected : new String[] { "task-freigabe", "task-freigabe-2", "task-freigabe-3" }) {

			final String id = firstFree("task-freigabe", used::contains);
			assertEquals(expected, id);
			used.add(id);
		}

		assertEquals("task-andere", firstFree("task-andere", used::contains));
	}

	/**
	 * The step-selection contract: exactly these four types are human-facing, each with
	 * states that can actually match at render time (a task-* state on a type the engine
	 * never creates a TaskInstance for would render never), and each with an id prefix that
	 * says what it is. One table drives all three, so this pins the table.
	 */
	@Test
	public void testHumanFacingTypesAndTheirStates() {

		final Map<BpmnElementType, Set<String>> expected = Map.of(

			BpmnElementType.USER_TASK, Set.of(
				VisibilityMappingTraitDefinition.STATE_TASK_AVAILABLE,
				VisibilityMappingTraitDefinition.STATE_TASK_RESERVED_BY_ME),

			BpmnElementType.START_EVENT, Set.of(VisibilityMappingTraitDefinition.STATE_NO_INSTANCE),
			BpmnElementType.MANUAL_TASK, Set.of(VisibilityMappingTraitDefinition.STATE_PROCESS_AWAITING_ACTION),
			BpmnElementType.INTERMEDIATE_CATCH_EVENT, Set.of(VisibilityMappingTraitDefinition.STATE_PROCESS_AWAITING_ACTION)
		);

		final Map<BpmnElementType, String> prefixes = Map.of(
			BpmnElementType.USER_TASK,                "task-",
			BpmnElementType.START_EVENT,              "start-",
			BpmnElementType.MANUAL_TASK,              "manual-",
			BpmnElementType.INTERMEDIATE_CATCH_EVENT, "event-"
		);

		for (final BpmnElementType type : BpmnElementType.values()) {

			assertEquals("states for " + type, expected.getOrDefault(type, Set.of()), visibleWhenFor(type));

			if (prefixes.containsKey(type)) {

				assertEquals("id prefix for " + type, prefixes.get(type), idPrefixFor(type));
			}
		}
	}

	/**
	 * Containment is asked of the enum rather than re-listed per call site, because a
	 * traversal that only follows sequence flows has to know to descend into these.
	 */
	@Test
	public void testSubProcessLikeTypes() {

		final Set<BpmnElementType> containers = Set.of(
			BpmnElementType.SUB_PROCESS, BpmnElementType.TRANSACTION, BpmnElementType.AD_HOC_SUB_PROCESS);

		for (final BpmnElementType type : BpmnElementType.values()) {

			assertEquals(type + ".isSubProcessLike()", containers.contains(type), type.isSubProcessLike());
		}
	}
}
