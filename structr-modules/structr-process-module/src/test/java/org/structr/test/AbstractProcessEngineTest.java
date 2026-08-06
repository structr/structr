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
package org.structr.test;

import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.process.ProcessTraits;
import org.structr.process.bpmn.BpmnImporter;
import org.structr.process.engine.ProcessEngine;
import org.structr.process.traits.definitions.*;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.BeforeMethod;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * Shared fixture-loading and graph-inspection helpers for the process-engine
 * test classes. Every helper is designed to be called inside a transaction the
 * test opens itself, mirroring the style of {@link BpmnRoundTripTest}.
 *
 * <p>{@link StructrUiTest} wipes the database before each test method, so no
 * per-test cleanup is required.</p>
 */
public abstract class AbstractProcessEngineTest extends StructrUiTest {

	// ------------------------------------------------------------------
	// Fixture import
	// ------------------------------------------------------------------

	/**
	 * Import a BPMN fixture and return the UUID of its (first) BpmnProcess node.
	 * The import is committed so the returned id can be re-fetched from a later
	 * transaction.
	 */
	protected String importProcess(final String resource) throws FrameworkException {

		final String xml = loadResource(resource);
		String processUuid;

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode = new BpmnImporter(securityContext).importBpmn(xml);
			assertNotNull("Import returned null for " + resource, defNode);

			final NodeInterface procNode = firstProcess(defNode);
			assertNotNull("No BpmnProcess in " + resource, procNode);
			processUuid = procNode.getUuid();

			tx.success();
		}

		return processUuid;
	}

	protected String loadResource(final String path) {

		try (final InputStream is = getClass().getResourceAsStream(path)) {

			assertNotNull("Resource not found: " + path, is);
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);

		} catch (final Exception ex) {

			throw new RuntimeException("Could not load resource " + path, ex);
		}
	}

	// ------------------------------------------------------------------
	// Engine factories
	//
	// Engines are cached per caller for the duration of a single test method
	// (cleared before each method) so a test that drives the engine across
	// several transactions reuses one instance per acting principal instead of
	// constructing a new ProcessEngine on every call.
	// ------------------------------------------------------------------

	private static final String SUPERUSER_KEY = "__superuser__";

	private final Map<String, ProcessEngine> engineCache = new HashMap<>();

	@BeforeMethod
	public void clearEngineCache() {
		engineCache.clear();
	}

	/** Engine acting as the super user (no recorded initiator / grants). */
	protected ProcessEngine engine() {
		return engineCache.computeIfAbsent(SUPERUSER_KEY, k -> new ProcessEngine(securityContext));
	}

	/** Engine acting as the given principal (records initiator, applies grants). */
	protected ProcessEngine engineAs(final NodeInterface user) {
		return engineCache.computeIfAbsent(user.getUuid(), k -> new ProcessEngine(userContext(user)));
	}

	protected SecurityContext userContext(final NodeInterface user) {
		return SecurityContext.getInstance(user.as(Principal.class), AccessMode.Backend);
	}

	// ------------------------------------------------------------------
	// Node navigation
	// ------------------------------------------------------------------

	protected NodeInterface firstProcess(final NodeInterface defNode) throws FrameworkException {

		final Iterable<NodeInterface> processes = defNode.getProperty(
			defNode.getTraits().key(BpmnDefinitionsTraitDefinition.PROCESSES_PROPERTY));
		if (processes != null) {

			for (final NodeInterface p : processes) {

				return p;
			}
		}
		return null;
	}

	protected NodeInterface elementByBpmnId(final NodeInterface procNode, final String bpmnId) throws FrameworkException {

		final Traits procTraits = Traits.of(ProcessTraits.BPMN_PROCESS);
		final Iterable<NodeInterface> elements = procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.ELEMENTS_PROPERTY));
		if (elements != null) {

			for (final NodeInterface e : elements) {

				if (bpmnId.equals(e.getProperty(e.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)))) {

					return e;
				}
			}
		}
		return null;
	}

	// ------------------------------------------------------------------
	// Instance / token / task inspection
	// ------------------------------------------------------------------

	protected String instanceStatus(final NodeInterface instance) throws FrameworkException {
		return instance.getProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.STATUS_PROPERTY));
	}

	protected NodeInterface subjectOf(final NodeInterface instance) throws FrameworkException {
		return instance.getProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.SUBJECT_PROPERTY));
	}

	protected List<NodeInterface> tokens(final NodeInterface instance) throws FrameworkException {
		return collect(instance.getProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.TOKENS_PROPERTY)));
	}

	/** Count tokens of the instance in the given status. */
	protected int tokenCount(final NodeInterface instance, final String status) throws FrameworkException {

		int count = 0;
		final Traits t = Traits.of(ProcessTraits.PROCESS_TOKEN);
		for (final NodeInterface token : tokens(instance)) {

			if (status.equals(token.getProperty(t.key(ProcessTokenTraitDefinition.STATUS_PROPERTY)))) {

				count++;
			}
		}
		return count;
	}

	/** The bpmnId of the element a waiting token currently sits on (first match), or null. */
	protected List<String> waitingTokenElementIds(final NodeInterface instance) throws FrameworkException {

		final List<String> ids = new LinkedList<>();
		final Traits t = Traits.of(ProcessTraits.PROCESS_TOKEN);
		for (final NodeInterface token : tokens(instance)) {

			final String status = token.getProperty(t.key(ProcessTokenTraitDefinition.STATUS_PROPERTY));
			if (ProcessTokenTraitDefinition.STATUS_WAITING.equals(status)) {

				final NodeInterface el = token.getProperty(t.key(ProcessTokenTraitDefinition.AT_ELEMENT_PROPERTY));
				if (el != null) {

					ids.add(el.getProperty(el.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));
				}
			}
		}
		return ids;
	}

	protected List<NodeInterface> tasks(final NodeInterface instance) throws FrameworkException {
		return collect(instance.getProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.TASKS_PROPERTY)));
	}

	protected String taskStatus(final NodeInterface task) throws FrameworkException {
		return task.getProperty(task.getTraits().key(TaskInstanceTraitDefinition.STATUS_PROPERTY));
	}

	protected NodeInterface assigneeOf(final NodeInterface task) throws FrameworkException {
		return task.getProperty(task.getTraits().key(TaskInstanceTraitDefinition.ASSIGNEE_PROPERTY));
	}

	/**
	 * Find the (single) non-terminal TaskInstance defined by the element with the
	 * given bpmnId. Returns null if none is open.
	 */
	protected NodeInterface openTaskAt(final NodeInterface instance, final String elementBpmnId) throws FrameworkException {

		for (final NodeInterface task : tasks(instance)) {

			final String status = taskStatus(task);
			if (TaskInstanceTraitDefinition.STATUS_COMPLETED.equals(status)
				|| TaskInstanceTraitDefinition.STATUS_CANCELLED.equals(status)) {
				continue;
			}
			final NodeInterface el = task.getProperty(task.getTraits().key(TaskInstanceTraitDefinition.DEFINED_BY_PROPERTY));
			if (el != null && elementBpmnId.equals(el.getProperty(el.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)))) {

				return task;
			}
		}
		return null;
	}

	/** The first TaskInstance (any status) defined by the given element bpmnId. */
	protected NodeInterface anyTaskAt(final NodeInterface instance, final String elementBpmnId) throws FrameworkException {

		for (final NodeInterface task : tasks(instance)) {

			final NodeInterface el = task.getProperty(task.getTraits().key(TaskInstanceTraitDefinition.DEFINED_BY_PROPERTY));
			if (el != null && elementBpmnId.equals(el.getProperty(el.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)))) {

				return task;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------
	// Parameter values / timers
	// ------------------------------------------------------------------

	/** paramName -> stringValue for all ProcessParameterValues on the instance. */
	protected Map<String, String> parameterValues(final NodeInterface instance) throws FrameworkException {

		final Map<String, String> out = new LinkedHashMap<>();
		final Traits pv = Traits.of(ProcessTraits.PROCESS_PARAMETER_VALUE);
		final Iterable<NodeInterface> pvs = instance.getProperty(
			instance.getTraits().key(ProcessInstanceTraitDefinition.PARAMETER_VALUES_PROPERTY));
		if (pvs != null) {

			for (final NodeInterface p : pvs) {

				out.put(p.getProperty(pv.key(ProcessParameterValueTraitDefinition.PARAMETER_NAME_PROPERTY)),
						p.getProperty(pv.key(ProcessParameterValueTraitDefinition.STRING_VALUE_PROPERTY)));
			}
		}
		return out;
	}

	protected List<NodeInterface> pendingTimers() throws FrameworkException {

		final Traits t = Traits.of(ProcessTraits.PROCESS_TIMER);
		return app.nodeQuery(ProcessTraits.PROCESS_TIMER)
			.key(t.key(ProcessTimerTraitDefinition.STATUS_PROPERTY), ProcessTimerTraitDefinition.STATUS_PENDING)
			.getAsList();
	}

	protected List<NodeInterface> allTimers() throws FrameworkException {
		return app.nodeQuery(ProcessTraits.PROCESS_TIMER).getAsList();
	}

	protected String timerStatus(final NodeInterface timer) throws FrameworkException {
		return timer.getProperty(timer.getTraits().key(ProcessTimerTraitDefinition.STATUS_PROPERTY));
	}

	// ------------------------------------------------------------------
	// Users / groups
	// ------------------------------------------------------------------

	protected NodeInterface createUser(final String name) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.create(StructrTraits.USER, name);
			tx.success();
			return user;
		}
	}

	protected NodeInterface createGroup(final String name) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final NodeInterface group = app.create(StructrTraits.GROUP, name);
			tx.success();
			return group;
		}
	}

	protected void addToGroup(final NodeInterface group, final NodeInterface user) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			group.as(Group.class).addMember(securityContext, user.as(Principal.class));
			tx.success();
		}
	}

	// ------------------------------------------------------------------
	// misc
	// ------------------------------------------------------------------

	protected NodeInterface createTestSubject(final String name) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final NodeInterface subject = app.create("TestOne", name);
			tx.success();
			return subject;
		}
	}

	protected List<NodeInterface> collect(final Iterable<NodeInterface> it) {

		final List<NodeInterface> out = new LinkedList<>();
		if (it != null) {

			for (final NodeInterface n : it) {

				out.add(n);
			}
		}
		return out;
	}
}
