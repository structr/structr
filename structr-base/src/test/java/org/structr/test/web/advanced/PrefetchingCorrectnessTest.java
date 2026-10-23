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
package org.structr.test.web.advanced;

import org.structr.api.DatabaseService;
import org.structr.api.config.Settings;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.AssertJUnit.*;

/**
 * Tests that prefetching changes how many queries a request runs, never what it reads. Every test reads the same
 * data once with a learned prefetch and once with prefetching turned off (database.prefetching.enabled), and the
 * results must be identical, including their order.
 *
 * <p>The schema reuses the relationship type name HAS for three relationships, Project-[:HAS]->Task,
 * Milestone-[:HAS]->Task and Project-[:HAS]->Note, like applications do. A learned pattern loads the relationships
 * of one node type, (n:Project)-[r:HAS]->(m), so the nodes at the other end only see part of their HAS
 * relationships in it.
 *
 * <p>The tests also check the self-regulation (which prefetches are deactivated) and the prefetch statistics, which
 * is where the effect of prefetching shows in a running application.
 */
public class PrefetchingCorrectnessTest extends StructrUiTest {

	private static final int PROJECTS   = 30;
	private static final int TASKS      = 10;
	private static final int NOTES      = 2;
	private static final int MILESTONES = 10;
	private static final int PERSONS    = 5;
	private static final int THRESHOLD  = 20;

	@BeforeMethod
	public void disableDurationComparison() {

		// the test database is local, so whether a prefetch takes longer than the lookups it saves depends on the
		// machine; the tests of the evaluation by duration turn it on
		Settings.PrefetchingCompareDuration.setValue(false);
	}

	@AfterMethod
	public void restorePrefetchSettings() {

		Settings.PrefetchingEnabled.setValue(Settings.PrefetchingEnabled.getDefaultValue());
		Settings.PrefetchingThreshold.setValue(Settings.PrefetchingThreshold.getDefaultValue());
		Settings.PrefetchingMaxCount.setValue(Settings.PrefetchingMaxCount.getDefaultValue());
		Settings.PrefetchingMaxDuration.setValue(Settings.PrefetchingMaxDuration.getDefaultValue());
		Settings.PrefetchingCostRatio.setValue(Settings.PrefetchingCostRatio.getDefaultValue());
		Settings.PrefetchingMaxFailures.setValue(Settings.PrefetchingMaxFailures.getDefaultValue());
		Settings.PrefetchingCompareDuration.setValue(Settings.PrefetchingCompareDuration.getDefaultValue());
	}

	/**
	 * The pattern (n:Project)-[r:HAS]->(m) loads the HAS relationships of all projects, which for a task is the one
	 * from its project but not the one from its milestone. The task's incoming HAS relationships must therefore not
	 * be answered from the prefetch.
	 */
	@Test
	public void testPrefetchDoesNotHideRelationshipsOfOtherSourceTypes() {

		final String hint = "shared relationship type, prefetch of the source side";

		setUp();

		request(hint, this::readProjectTasks);
		assertPatternIsActive(hint, "(n:Project", "-[r:HAS]->");

		final Map<String, List<String>> prefetched = request(hint, this::readEverything);

		assertPatternWasExecuted(hint, "(n:Project", "-[r:HAS]->");
		assertEquals("A prefetch changed what a request reads", reference(this::readEverything), prefetched);
	}

	/**
	 * The mirror image: (n:Task)<-[r:HAS]-(m) loads the incoming HAS relationships of all tasks, which for a project
	 * are its tasks but not its notes. The project's outgoing HAS relationships must not be answered from it.
	 *
	 * <p>The pattern runs twice: the first execution groups its rows by the tasks, the second one by the projects
	 * and milestones at the other end, because there are fewer of them. Both must load the same.
	 */
	@Test
	public void testPrefetchDoesNotHideRelationshipsOfOtherTargetTypes() {

		final String hint = "shared relationship type, prefetch of the target side";

		setUp();

		request(hint, this::readTaskProjects);
		assertPatternIsActive(hint, "(n:Task", "<-[r:HAS]-");

		final Map<String, List<String>> groupedByTask    = request(hint, this::readEverything);
		final Map<String, List<String>> groupedByProject = request(hint, this::readEverything);
		final Map<String, List<String>> expected         = reference(this::readEverything);

		assertEquals("The pattern did not run in both requests", 2L, executions(pattern(hint, "(n:Task", "<-[r:HAS]-")));
		assertEquals("A prefetch grouped by the tasks changed what a request reads", expected, groupedByTask);
		assertEquals("A prefetch grouped by the other end changed what a request reads", expected, groupedByProject);
	}

	/**
	 * A lookup of all outgoing relationships of a node must not be answered with the relationships of one type that
	 * were looked up or prefetched before. The relationship cache of a node is a tree, so it has an entry for
	 * all/OUTGOING as soon as it has one for all/OUTGOING/HAS.
	 */
	@Test
	public void testLookupOfAllRelationshipsIsNotAnsweredWithThoseOfOneType() {

		final String hint = "untyped lookup";

		setUp();

		// once without any prefetch, where the typed lookup alone fills the cache
		final Map<String, List<String>> plain = request("untyped lookup without prefetch", this::readTasksThenAllOutgoing);

		// and once with a learned prefetch of the HAS relationships of all projects
		request(hint, this::readProjectTasks);

		final Map<String, List<String>> prefetched = request(hint, this::readTasksThenAllOutgoing);

		assertPatternWasExecuted(hint, "(n:Project", "-[r:HAS]->");

		for (final Map<String, List<String>> result : List.of(plain, prefetched)) {

			for (final Map.Entry<String, List<String>> entry : result.entrySet()) {

				// HAS to each task and note, MANAGED_BY to the manager
				assertEquals("Wrong number of outgoing relationships of " + entry.getKey(), List.of(String.valueOf(TASKS + NOTES + 1)), entry.getValue());
			}
		}
	}

	/**
	 * Writes after the prefetch, in the same transaction, must be visible to the reads that follow them.
	 */
	@Test
	public void testWritesAfterThePrefetchAreVisibleInTheSameTransaction() {

		final String hint = "writes after prefetch";

		setUp();

		request(hint, this::readProjectTasks);

		final Map<String, List<String>> prefetched = request(hint, out -> {
			final PropertyKey project = Traits.of("Task").key("project");

			// move a task to another project, delete one and create one
			task("p00-t0").setProperty(project, project("p01"));
			app.delete(task("p02-t0"));
			app.create("Task", "p03-new").setProperty(project, project("p03"));

			readEverything(out);
		});

		assertPatternWasExecuted(hint, "(n:Project", "-[r:HAS]->");
		assertEquals("A write after the prefetch is missing from the reads that follow it", reference(this::readEverything), prefetched);
	}

	/**
	 * Two learned patterns with the same type and direction, (n:Project)-[r:HAS]->(m) and
	 * (n:Project)-[r:MANAGED_BY]->(m), are combined into one, which must give the same results.
	 */
	@Test
	public void testCombinedPatternGivesTheSameResults() {

		final String hint = "combined pattern";

		setUp();

		request(hint, this::readProjectTasksAndManagers);

		final Map<String, List<String>> prefetched = request(hint, this::readEverything);

		assertPatternWasExecuted(hint, "(n:Project", "|", "]->");
		assertEquals("A combined prefetch changed what a request reads", reference(this::readEverything), prefetched);
	}

	/**
	 * A pattern that is too large to be learned must not come back as part of a combined pattern.
	 */
	@Test
	public void testPatternThatIsTooLargeIsNotCombined() {

		final String hint = "too large to combine";

		setUp();

		// the HAS pattern loads PROJECTS * (TASKS + NOTES) paths, the MANAGED_BY pattern PROJECTS
		Settings.PrefetchingMaxCount.setValue(PROJECTS * 2);

		request(hint, this::readProjectTasksAndManagers);
		assertPatternIsDeactivated(hint, "(n:Project", "-[r:HAS]->");

		final Map<String, List<String>> prefetched = request(hint, this::readEverything);

		for (final Map<String, Object> pattern : patterns(hint)) {

			if (executions(pattern) > 0) {

				assertFalse("A pattern that is too large was executed: " + pattern.get("pattern"), ((String) pattern.get("pattern")).contains("-[r:HAS]->"));
			}
		}

		assertEquals("A prefetch changed what a request reads", reference(this::readEverything), prefetched);
	}

	/**
	 * A nested transaction can give the hint of the request again. The prefetch it ran first must still be
	 * evaluated, here it loads all projects for the tasks of one and must be deactivated.
	 */
	@Test
	public void testPrefetchIsEvaluatedWhenANestedTransactionGivesTheHintAgain() {

		final String hint = "nested hint";

		setUp();

		Settings.PrefetchingCostRatio.setValue(10);
		Settings.PrefetchingMaxFailures.setValue(1);

		request(hint, this::readProjectTasks);

		request(hint, out -> {

			try (final Tx nested = app.tx()) {

				nested.prefetchHint(hint);
				nested.success();
			}

			out.put("p00.tasks", names(project("p00").getProperty(Traits.of("Project").key("tasks"))));
		});

		assertPatternWasExecuted(hint, "(n:Project", "-[r:HAS]->");
		assertPatternIsDeactivated(hint, "(n:Project", "-[r:HAS]->");
	}

	/**
	 * Looking up the same few nodes again and again saves one query per node, not one per lookup, because the
	 * relationship cache of the node answers the repeated lookups without the prefetch as well.
	 */
	@Test
	public void testRepeatedLookupsOfTheSameNodesCountOnce() {

		final String hint = "repeated lookups";

		setUp();

		Settings.PrefetchingCostRatio.setValue(10);
		Settings.PrefetchingMaxFailures.setValue(1);

		request(hint, this::readProjectTasks);

		request(hint, out -> {

			final PropertyKey tasks = Traits.of("Project").key("tasks");

			for (int i = 0; i < 100; i++) {

				for (int p = 0; p < 5; p++) {

					names(project(String.format("p%02d", p)).getProperty(tasks));
				}
			}
		});

		final Map<String, Object> pattern = pattern(hint, "(n:Project", "-[r:HAS]->");

		assertEquals("Repeated lookups of the same nodes were counted more than once", 5.0, pattern.get("averageSavedLookups"));
		assertPatternIsDeactivated(hint, "(n:Project", "-[r:HAS]->");
	}

	/**
	 * A hint names a kind of request, and a single request of that kind that uses little of the prefetched data
	 * must not deactivate a pattern that pays off for the others. Only database.prefetching.maxfailures failed
	 * evaluations in a row do.
	 */
	@Test
	public void testPatternIsDeactivatedOnlyAfterRepeatedFailures() {

		final String hint = "repeated failures";

		setUp();

		// learns the pattern, which then loads all projects with their tasks and notes
		request(hint, this::readProjectTasks);

		// uses one project of all, far beyond the cost ratio
		request(hint, this::readFirstProjectTasks);
		assertPatternIsActive(hint, "(n:Project", "-[r:HAS]->");

		// uses all projects, which starts the count again
		request(hint, this::readProjectTasks);

		request(hint, this::readFirstProjectTasks);
		request(hint, this::readFirstProjectTasks);
		assertPatternIsActive(hint, "(n:Project", "-[r:HAS]->");

		request(hint, this::readFirstProjectTasks);
		assertPatternIsDeactivated(hint, "(n:Project", "-[r:HAS]->");

		assertEquals("Wrong number of failed evaluations", 4L, pattern(hint, "(n:Project", "-[r:HAS]->").get("failedEvaluations"));
	}

	/**
	 * A prefetch that takes longer than the lookups it answers would have taken fails its evaluation, also when it
	 * stays within the cost ratio. Here it loads all projects with their tasks and notes for the tasks of one project,
	 * which a single lookup would have read.
	 */
	@Test
	public void testPrefetchThatTakesLongerThanTheLookupsItSavesIsDeactivated() {

		final String hint = "compare duration";

		setUp();

		// only the comparison of durations may deactivate the pattern
		Settings.PrefetchingCostRatio.setValue(Integer.MAX_VALUE);
		Settings.PrefetchingMaxFailures.setValue(1);

		// the average lookup time starts with the first lookup of the test run, which is slow in a fresh JVM
		Settings.PrefetchingEnabled.setValue(false);

		for (int i = 0; i < 10; i++) {

			request("warm-up", this::readProjectTasks);
		}

		Settings.PrefetchingEnabled.setValue(true);

		// without the comparison, the pattern stays active
		request(hint, this::readProjectTasks);
		request(hint, this::readFirstProjectTasks);
		assertPatternIsActive(hint, "(n:Project", "-[r:HAS]->");

		Settings.PrefetchingCompareDuration.setValue(true);

		request(hint, this::readFirstProjectTasks);
		assertPatternIsDeactivated(hint, "(n:Project", "-[r:HAS]->");

		final Object status = pattern(hint, "(n:Project", "-[r:HAS]->").get("status");

		assertTrue("The pattern was deactivated for another reason: " + status, status.toString().contains("would have taken"));
	}

	@Test
	public void testDisabledPrefetchingNeitherLearnsNorPrefetches() {

		final String hint = "disabled";

		setUp();

		Settings.PrefetchingEnabled.setValue(false);

		request(hint, this::readProjectTasks);
		request(hint, this::readProjectTasks);

		assertTrue("Prefetching learned a pattern while it was turned off", patterns(hint).isEmpty());

		final Map<String, Object> without = transactions(hint, false);
		final Map<String, Object> with    = transactions(hint, true);

		assertEquals(2L, without.get("transactions"));
		assertEquals(0L, with.get("transactions"));
		assertEquals("Every lookup must query the database without prefetching", (double) PROJECTS, without.get("averageLookupQueries"));
	}

	/**
	 * The statistics count the lookups a prefetch answered: in the learning request every project's tasks are a
	 * query, in the next one the prefetch answers all of them.
	 */
	@Test
	public void testStatisticsCountTheSavedLookups() {

		final String hint = "statistics";

		setUp();

		request(hint, this::readProjectTasks);
		request(hint, this::readProjectTasks);

		final Map<String, Object> pattern = pattern(hint, "(n:Project", "-[r:HAS]->");
		final Map<String, Object> with    = transactions(hint, true);

		assertEquals("active", pattern.get("status"));
		assertEquals(1L, pattern.get("executions"));
		assertEquals((double) PROJECTS, pattern.get("averageSavedLookups"));

		// PROJECTS lookup queries in the first request, none in the second
		assertEquals(2L, with.get("transactions"));
		assertEquals(PROJECTS / 2.0, with.get("averageLookupQueries"));
	}

	/**
	 * Requests with the same hint learn, combine, run and evaluate patterns at the same time, and the learned
	 * patterns are flushed in between. Every request must still read the same data.
	 */
	@Test
	public void testConcurrentRequestsWithTheSameHint() {

		final String hint = "concurrent";

		setUp();

		final Map<String, List<String>> expected = reference(this::readEverything);
		final Queue<String> errors               = new ConcurrentLinkedQueue<>();
		final AtomicBoolean running              = new AtomicBoolean(true);
		final List<Thread> threads               = new ArrayList<>();
		final DatabaseService db                 = Services.getInstance().getDatabaseService();

		for (int i = 0; i < 6; i++) {

			final boolean readsEverything = i % 2 == 0;

			threads.add(new Thread(() -> {

				for (int j = 0; j < 15; j++) {

					try {

						final Map<String, List<String>> result = request(hint, readsEverything ? this::readEverything : this::readProjectTasksAndManagers);
						if (readsEverything && !expected.equals(result)) {

							errors.add("Different result in request " + j);
						}

					} catch (Throwable t) {

						errors.add(t.toString());
					}
				}
			}));
		}

		final Thread flusher = new Thread(() -> {

			while (running.get()) {

				db.flushCaches();

				try { Thread.sleep(20); } catch (InterruptedException ignore) { return; }
			}
		});

		threads.forEach(Thread::start);
		flusher.start();

		try {

			for (final Thread thread : threads) {

				thread.join();
			}

			running.set(false);
			flusher.join();

		} catch (InterruptedException iex) {

			fail("Interrupted");
		}

		assertTrue("Concurrent requests failed: " + errors, errors.isEmpty());
	}

	// ----- private methods -----
	private interface Reads {

		void read(final Map<String, List<String>> out) throws FrameworkException;
	}

	/**
	 * Creates the schema and the data, and sets a threshold that the reads exceed.
	 */
	private void setUp() {

		if (!Settings.DatabaseDriver.getValue().contains("bolt")) {

			throw new SkipException("Prefetching is a feature of the bolt driver, skipping test with " + Settings.DatabaseDriver.getValue());
		}

		try (final Tx tx = app.tx()) {

			final JsonSchema schema         = StructrSchema.createFromDatabase(app);
			final JsonObjectType project    = schema.addType("Project");
			final JsonObjectType task       = schema.addType("Task");
			final JsonObjectType milestone  = schema.addType("Milestone");
			final JsonObjectType note       = schema.addType("Note");
			final JsonObjectType person     = schema.addType("Person");

			project.relate(task, "HAS", Cardinality.OneToMany, "project", "tasks");
			project.relate(note, "HAS", Cardinality.OneToMany, "project", "notes");
			milestone.relate(task, "HAS", Cardinality.OneToMany, "milestone", "milestoneTasks");
			project.relate(person, "MANAGED_BY", Cardinality.ManyToOne, "managedProjects", "manager");

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> milestones = new ArrayList<>();
			final List<NodeInterface> persons    = new ArrayList<>();

			for (int i = 0; i < MILESTONES; i++) {

				milestones.add(app.create("Milestone", "m" + i));
			}

			for (int i = 0; i < PERSONS; i++) {

				persons.add(app.create("Person", "u" + i));
			}

			final PropertyKey taskProject   = Traits.of("Task").key("project");
			final PropertyKey taskMilestone = Traits.of("Task").key("milestone");
			final PropertyKey noteProject   = Traits.of("Note").key("project");
			final PropertyKey manager       = Traits.of("Project").key("manager");

			for (int p = 0; p < PROJECTS; p++) {

				final String name           = String.format("p%02d", p);
				final NodeInterface project = app.create("Project", name);

				project.setProperty(manager, persons.get(p % PERSONS));

				for (int t = 0; t < TASKS; t++) {

					final NodeInterface task = app.create("Task", name + "-t" + t);

					task.setProperty(taskProject, project);
					task.setProperty(taskMilestone, milestones.get((p * TASKS + t) % MILESTONES));
				}

				for (int n = 0; n < NOTES; n++) {

					app.create("Note", name + "-n" + n).setProperty(noteProject, project);
				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		Settings.PrefetchingThreshold.setValue(THRESHOLD);
		Services.getInstance().getDatabaseService().resetPrefetchStatistics();
	}

	/**
	 * Simulates a request: a transaction with a prefetch hint that runs the given reads.
	 */
	private Map<String, List<String>> request(final String hint, final Reads reads) {

		final Map<String, List<String>> out = new TreeMap<>();

		try (final Tx tx = app.tx()) {

			tx.prefetchHint(hint);

			reads.read(out);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		return out;
	}

	/**
	 * Runs the reads with prefetching turned off, in a transaction of its own.
	 */
	private Map<String, List<String>> reference(final Reads reads) {

		Settings.PrefetchingEnabled.setValue(false);

		try {

			return request("reference without prefetching", reads);

		} finally {

			Settings.PrefetchingEnabled.setValue(true);
		}
	}

	private void readProjectTasks(final Map<String, List<String>> out) throws FrameworkException {

		final PropertyKey tasks = Traits.of("Project").key("tasks");

		for (final NodeInterface project : all("Project")) {

			out.put(project.getName() + ".tasks", names(project.getProperty(tasks)));
		}
	}

	private void readFirstProjectTasks(final Map<String, List<String>> out) throws FrameworkException {

		out.put("p00.tasks", names(project("p00").getProperty(Traits.of("Project").key("tasks"))));
	}

	private void readProjectTasksAndManagers(final Map<String, List<String>> out) throws FrameworkException {

		final PropertyKey manager = Traits.of("Project").key("manager");

		readProjectTasks(out);

		for (final NodeInterface project : all("Project")) {

			out.put(project.getName() + ".manager", names(project.getProperty(manager)));
		}
	}

	private void readTaskProjects(final Map<String, List<String>> out) throws FrameworkException {

		final PropertyKey project = Traits.of("Task").key("project");

		for (final NodeInterface task : all("Task")) {

			out.put(task.getName() + ".project", names(task.getProperty(project)));
		}
	}

	private void readTasksThenAllOutgoing(final Map<String, List<String>> out) throws FrameworkException {

		final PropertyKey tasks = Traits.of("Project").key("tasks");

		for (final NodeInterface project : all("Project")) {

			names(project.getProperty(tasks));

			out.put(project.getName() + ".outgoing", List.of(String.valueOf(Iterables.count(project.getOutgoingRelationships()))));
		}
	}

	/**
	 * Reads every relationship of the schema, from both sides.
	 */
	private void readEverything(final Map<String, List<String>> out) throws FrameworkException {

		final Map<String, List<String>> properties = Map.of(
			"Project",   List.of("tasks", "notes", "manager"),
			"Task",      List.of("project", "milestone"),
			"Note",      List.of("project"),
			"Milestone", List.of("milestoneTasks"),
			"Person",    List.of("managedProjects")
		);

		for (final Map.Entry<String, List<String>> entry : properties.entrySet()) {

			final Traits traits = Traits.of(entry.getKey());

			for (final NodeInterface node : all(entry.getKey())) {

				for (final String property : entry.getValue()) {

					out.put(entry.getKey() + " " + node.getName() + "." + property, names(node.getProperty(traits.key(property))));
				}
			}
		}
	}

	private List<NodeInterface> all(final String type) throws FrameworkException {

		final PropertyKey name = Traits.of(type).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

		return app.nodeQuery(type).sort(name).getAsList();
	}

	private NodeInterface project(final String name) throws FrameworkException {

		return app.nodeQuery("Project").name(name).getFirst();
	}

	private NodeInterface task(final String name) throws FrameworkException {

		return app.nodeQuery("Task").name(name).getFirst();
	}

	private List<String> names(final Object value) {

		final List<String> names = new ArrayList<>();

		if (value instanceof NodeInterface node) {

			names.add(node.getName());

		} else if (value instanceof Iterable<?> iterable) {

			for (final Object o : iterable) {

				names.add(((NodeInterface) o).getName());
			}
		}

		return names;
	}

	// ----- statistics -----
	private Map<String, Object> hintStatistics(final String hint) {

		final Map<String, Object> statistics = Services.getInstance().getDatabaseService().getPrefetchStatistics();

		for (final Map<String, Object> entry : (List<Map<String, Object>>) statistics.get("hints")) {

			if (hint.equals(entry.get("hint"))) {

				return entry;
			}
		}

		return Map.of();
	}

	private List<Map<String, Object>> patterns(final String hint) {

		return (List<Map<String, Object>>) hintStatistics(hint).getOrDefault("patterns", List.of());
	}

	private Map<String, Object> transactions(final String hint, final boolean withPrefetching) {

		return (Map<String, Object>) hintStatistics(hint).get(withPrefetching ? "withPrefetching" : "withoutPrefetching");
	}

	private Map<String, Object> pattern(final String hint, final String... parts) {

		for (final Map<String, Object> pattern : patterns(hint)) {

			if (matches((String) pattern.get("pattern"), parts)) {

				return pattern;
			}
		}

		fail("No pattern " + Arrays.toString(parts) + " for " + hint + ", the statistics have " + patterns(hint));

		return null;
	}

	private long executions(final Map<String, Object> pattern) {

		return (Long) pattern.get("executions");
	}

	private void assertPatternIsActive(final String hint, final String... parts) {

		assertEquals("Pattern " + Arrays.toString(parts) + " was not learned", "active", pattern(hint, parts).get("status"));
	}

	private void assertPatternIsDeactivated(final String hint, final String... parts) {

		final Object status = pattern(hint, parts).get("status");

		assertTrue("Pattern " + Arrays.toString(parts) + " was not deactivated, its status is " + status, status != null && status.toString().startsWith("deactivated"));
	}

	private void assertPatternWasExecuted(final String hint, final String... parts) {

		assertTrue("Pattern " + Arrays.toString(parts) + " was not executed, the statistics have " + patterns(hint), executions(pattern(hint, parts)) > 0);
	}

	private boolean matches(final String pattern, final String... parts) {

		for (final String part : parts) {

			if (!pattern.contains(part)) {

				return false;
			}
		}

		return true;
	}
}
