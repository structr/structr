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
package org.structr.process.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.process.ProcessTraits;
import org.structr.process.engine.ProcessEngine;
import org.structr.schema.action.ActionContext;

import java.util.Date;
import java.util.Map;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.process.entity.ProcessInstance;
import org.structr.process.traits.wrappers.ProcessInstanceTraitWrapper;
import java.util.Set;

/**
 * A running instance of a BPMN process definition. Tracks the current execution
 * state through tokens, task instances, and process data.
 *
 * Status lifecycle: running -> completed | suspended | terminated | error
 *
 * <p>Process-level lifecycle events are dispatched through two paths:
 * <ul>
 *   <li><b>Engine-explicit fire</b> for {@code created}, {@code started}, and
 *       {@code resumed}. {@code created} and {@code started} are emitted by
 *       {@link ProcessEngine#startProcess} after the initial token is placed.
 *       {@code resumed} is emitted by {@link ProcessEngine#resumeProcess} so it
 *       can be distinguished from the initial transition into running.</li>
 *   <li><b>OnModification-driven fire</b> for {@code completed}, {@code terminated},
 *       {@code suspended}, and {@code subjectAttached}. The hook detects status
 *       transitions and forwards them to {@link ProcessEngine#fireProcessEvent},
 *       so the events fire whether the transition was triggered by an engine
 *       method or by direct {@code setProperty('status', ...)} from the admin UI
 *       or scripts. {@code subjectAttached} fires whenever the subject relationship
 *       is set to a non-null target.</li>
 * </ul>
 */
public class ProcessInstanceTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String STATUS_PROPERTY       = "status";
	public static final String START_TIME_PROPERTY   = "startTime";
	public static final String END_TIME_PROPERTY     = "endTime";
	public static final String PROCESS_PROPERTY      = "process";
	public static final String INITIATOR_PROPERTY    = "initiator";
	public static final String SUBJECT_PROPERTY      = "subject";
	public static final String TOKENS_PROPERTY            = "tokens";
	public static final String TASKS_PROPERTY             = "tasks";
	public static final String PARAMETER_VALUES_PROPERTY  = "parameterValues";

	// Status constants
	public static final String STATUS_RUNNING     = "running";
	public static final String STATUS_COMPLETED   = "completed";
	public static final String STATUS_SUSPENDED   = "suspended";
	public static final String STATUS_TERMINATED  = "terminated";
	public static final String STATUS_ERROR       = "error";

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			ProcessInstance.class, (traits, node) -> new ProcessInstanceTraitWrapper(traits, node)
		);
	}

	public ProcessInstanceTraitDefinition() {
		super(ProcessTraits.PROCESS_INSTANCE);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

			// Detect terminal status transitions and subject attachment, forward to the
			// engine's fire helper. The 'running' transition is intentionally NOT handled
			// here: the initial running-from-creation must not be confused with resumed-
			// from-suspended. The engine fires 'started' and 'resumed' explicitly so the
			// two cases stay distinct.
			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final Traits traits = graphObject.getTraits();

					// Status transitions: completed / terminated / suspended.
					// Direct setProperty('status', ...) from the admin UI / scripts flows through here.
					if (modificationQueue.isPropertyModified(graphObject, traits.key(STATUS_PROPERTY))) {

						final String newStatus = graphObject.getProperty(traits.key(STATUS_PROPERTY));
						final ProcessEngine engine = new ProcessEngine(securityContext);

						if (STATUS_COMPLETED.equals(newStatus)) {

							if (graphObject.getProperty(traits.key(END_TIME_PROPERTY)) == null) {
								graphObject.setProperty(traits.key(END_TIME_PROPERTY), new Date());
							}
							engine.fireProcessEvent(BpmnProcessListenerTraitDefinition.EVENT_COMPLETED, (NodeInterface) graphObject);

						} else if (STATUS_TERMINATED.equals(newStatus)) {

							if (graphObject.getProperty(traits.key(END_TIME_PROPERTY)) == null) {
								graphObject.setProperty(traits.key(END_TIME_PROPERTY), new Date());
							}
							engine.fireProcessEvent(BpmnProcessListenerTraitDefinition.EVENT_TERMINATED, (NodeInterface) graphObject);

						} else if (STATUS_SUSPENDED.equals(newStatus)) {

							engine.fireProcessEvent(BpmnProcessListenerTraitDefinition.EVENT_SUSPENDED, (NodeInterface) graphObject);
						}
						// 'running' transitions are handled by the engine: 'started' from startProcess,
						// 'resumed' from resumeProcess.
					}

					// Subject attached: any change to the subject relationship that produces a non-null
					// target fires the subjectAttached event. Common pattern: the LeaveRequest case where
					// the subject is created from form data after startProcess and attached post-hoc.
					if (modificationQueue.isPropertyModified(graphObject, traits.key(SUBJECT_PROPERTY))) {

						final NodeInterface newSubject = graphObject.getProperty(traits.key(SUBJECT_PROPERTY));
						if (newSubject != null) {

							final ProcessEngine engine = new ProcessEngine(securityContext);
							engine.fireProcessEvent(BpmnProcessListenerTraitDefinition.EVENT_SUBJECT_ATTACHED, (NodeInterface) graphObject);
						}
					}
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> status          = new StringProperty(STATUS_PROPERTY).indexed();
		final Property<Date> startTime         = new DateProperty(START_TIME_PROPERTY);
		final Property<Date> endTime           = new DateProperty(END_TIME_PROPERTY);
		final Property<NodeInterface> process  = new EndNode(traitsInstance, PROCESS_PROPERTY, ProcessTraits.PROCESS_INSTANCE_OF_PROCESS);
		final Property<NodeInterface> initiator = new EndNode(traitsInstance, INITIATOR_PROPERTY, ProcessTraits.PROCESS_INSTANCE_INITIATED_BY);
		final Property<NodeInterface> subject                   = new EndNode(traitsInstance, SUBJECT_PROPERTY, ProcessTraits.PROCESS_INSTANCE_HAS_SUBJECT);
		final Property<Iterable<NodeInterface>> tokens          = new EndNodes(traitsInstance, TOKENS_PROPERTY, ProcessTraits.PROCESS_INSTANCE_HAS_TOKEN);
		final Property<Iterable<NodeInterface>> tasks            = new StartNodes(traitsInstance, TASKS_PROPERTY, ProcessTraits.TASK_INSTANCE_OF_PROCESS);
		final Property<Iterable<NodeInterface>> parameterValues  = new EndNodes(traitsInstance, PARAMETER_VALUES_PROPERTY, ProcessTraits.PROCESS_INSTANCE_HAS_PARAMETER_VALUE);

		return newSet(status, startTime, endTime, process, initiator, subject, tokens, tasks, parameterValues);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(STATUS_PROPERTY, START_TIME_PROPERTY, END_TIME_PROPERTY, PROCESS_PROPERTY, INITIATOR_PROPERTY, SUBJECT_PROPERTY),
			PropertyView.Ui, newSet(STATUS_PROPERTY, START_TIME_PROPERTY, END_TIME_PROPERTY, PROCESS_PROPERTY, INITIATOR_PROPERTY, SUBJECT_PROPERTY, TOKENS_PROPERTY, TASKS_PROPERTY, PARAMETER_VALUES_PROPERTY)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return Set.of(

			new JavaMethod("signalEvent", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {
					final SecurityContext securityContext = actionContext.getSecurityContext();
					final ProcessEngine engine = new ProcessEngine(securityContext);
					final java.util.Map<String, Object> params = arguments.toMap();
					final String eventBpmnId = (String) params.remove("eventBpmnId");
					if (eventBpmnId == null || eventBpmnId.isEmpty()) {

						throw new FrameworkException(422, "Missing required parameter: eventBpmnId");
					}
					engine.signalEvent((NodeInterface) entity, eventBpmnId, params.isEmpty() ? null : params);
					return entity;
				}

				@Override
				public String getDescription() {
					return "Signals an intermediate catch event by bpmnId, resuming the waiting token. Pass eventBpmnId as a required parameter.";
				}
			},

			new JavaMethod("getCurrentSteps", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {
					// The BpmnElement node(s) this instance is currently at (elements of its
					// non-completed tokens). Returns the nodes so callers can read bpmnName /
					// bpmnElementType / bpmnId / etc. as needed.
					return ProcessEngine.currentStepElements((NodeInterface) entity);
				}

				@Override
				public String getDescription() {
					return "Returns the BpmnElement node(s) this instance is currently at (the elements of its non-completed tokens). Usually one; a parallel split yields several. Empty once the instance has finished.";
				}
			},

			new JavaMethod("terminate", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {
					final SecurityContext securityContext = actionContext.getSecurityContext();
					final ProcessEngine engine = new ProcessEngine(securityContext);
					engine.terminateProcess((NodeInterface) entity);
					return entity;
				}

				@Override
				public String getDescription() {
					return "Terminates this process instance: status='terminated', endTime=now, all waiting tokens are marked completed without advancing. Fires the 'terminated' lifecycle event.";
				}
			},

			new JavaMethod("suspend", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {
					final SecurityContext securityContext = actionContext.getSecurityContext();
					final ProcessEngine engine = new ProcessEngine(securityContext);
					engine.suspendProcess((NodeInterface) entity);
					return entity;
				}

				@Override
				public String getDescription() {
					return "Suspends this process instance: status='suspended'. Existing tokens stay in place; no new advancement occurs until the instance is resumed. Fires the 'suspended' lifecycle event.";
				}
			},

			new JavaMethod("resume", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {
					final SecurityContext securityContext = actionContext.getSecurityContext();
					final ProcessEngine engine = new ProcessEngine(securityContext);
					engine.resumeProcess((NodeInterface) entity);
					return entity;
				}

				@Override
				public String getDescription() {
					return "Resumes a suspended process instance: status='running'. Fires the 'resumed' lifecycle event (distinct from the initial 'started' event fired at startProcess time).";
				}
			}
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
