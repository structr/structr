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
package org.structr.process.traits.definitions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.process.ProcessTraits;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.dom.VisibilityMapping;
import org.structr.web.traits.wrappers.dom.VisibilityMappingTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 * Render-time predicate that decides whether a DOMNode should be emitted, based on
 * process / task state of a specific {@code ProcessInstance} derived from the page's
 * render context.
 *
 * <p><b>Architecture in a nutshell.</b> Two systems collaborate:</p>
 * <ol>
 *   <li><b>Engine grants</b> determine who can <i>read</i> what. When a Principal
 *       is added as initiator, candidate, or assignee, the engine grants read on
 *       the {@code ProcessInstance}; permission propagation on {@code TASK_OF} and
 *       {@code HAS_PARAMETER_VALUE} extends that grant to all the instance's tasks
 *       and parameter values. There's one rule: "participant of an instance can
 *       read its objects."</li>
 *   <li><b>This predicate</b> decides what to <i>render</i>. It's purely a state
 *       inspector against an explicit instance -- it doesn't recompute permissions
 *       and doesn't iterate "any matching instance ever." The instance comes from
 *       the page's render context (passed as {@code contextObject}).</li>
 * </ol>
 *
 * <p><b>Render-context resolution.</b> The {@code contextObject} is whatever the
 * DOMNode renderer's data object is at the partial's render point:
 * {@code ProcessInstance} -> used directly; {@code TaskInstance} -> walks to its
 * {@code processInstance}; anything else (or null) -> only {@code no-instance}
 * matches. So a process detail page (URL-routed to a ProcessInstance) and a
 * repeater over tasks both Just Work; a generic landing page sees null, and only
 * "starter" partials light up.</p>
 *
 * <p><b>State vocabulary.</b> Closed enum.</p>
 * <table>
 *   <tr><th>State</th><th>Means (relative to contextObject's instance)</th></tr>
 *   <tr><td>{@code task-available}</td><td>An active task at the bound step has status=available; current user is in candidates.</td></tr>
 *   <tr><td>{@code task-reserved-by-me}</td><td>Task at the bound step, status=reserved, assignee=$.me.</td></tr>
 *   <tr><td>{@code task-reserved-by-other}</td><td>Reserved by someone else, but I'm in candidates ("my pool, claimed by a peer").</td></tr>
 *   <tr><td>{@code task-completed}</td><td>A task at the bound step has status=completed.</td></tr>
 *   <tr><td>{@code task-cancelled}</td><td>A task at the bound step has status=cancelled.</td></tr>
 *   <tr><td>{@code process-completed}</td><td>This instance's status=completed.</td></tr>
 *   <tr><td>{@code process-terminated}</td><td>This instance's status=terminated.</td></tr>
 *   <tr><td>{@code process-failed}</td><td>This instance's status=error.</td></tr>
 *   <tr><td>{@code process-awaiting-action}</td><td>This instance is running, I initiated it, no active task assigned/candidate to me.</td></tr>
 *   <tr><td>{@code no-instance}</td><td>No instance in render context (i.e. {@code contextObject == null}). For starter / landing partials.</td></tr>
 * </table>
 *
 * <p><b>Step matching is by {@code bpmnId}, not node identity</b>, so re-imports
 * that create new BpmnElement nodes don't break the predicate -- the running
 * instance still references its own version's elements, and {@code bpmnId} is
 * stable across versions.</p>
 *
 * <p><b>OR-combining VMs on a single DOMNode</b> stays useful when multiple states
 * should produce the same display ("your request is being processed" for both
 * {@code running} and {@code awaiting-action}). It becomes a smell when the
 * partial hosts an action button: a button is step-specific, an OR-combined VM
 * isn't. Split such partials so each holds one VM + one action.</p>
 */
public class VisibilityMappingTraitDefinition extends AbstractNodeTraitDefinition {

	private static final Logger logger = LoggerFactory.getLogger(VisibilityMappingTraitDefinition.class);

	public static final String DOM_NODE_PROPERTY              = "domNode";
	public static final String BOUND_PROCESS_PROPERTY         = "boundProcess";
	public static final String BOUND_STEP_PROPERTY            = "boundStep";
	public static final String VISIBLE_WHEN_PROPERTY          = "visibleWhen";

	// Denormalized name backup -- the BPMN-side identifiers cached on the
	// mapping itself. Refreshed at save time and at re-import rewire. Used
	// (a) to drive the importer's rewire-by-bpmnId step, (b) for diagnostics,
	// and (c) optionally as a fallback if the rels go null. The relationships
	// remain authoritative; these strings are a denormalized cache.
	public static final String BOUND_PROCESS_ID_PROPERTY      = "boundProcessId";
	public static final String BOUND_STEP_BPMN_ID_PROPERTY    = "boundStepBpmnId";

	// State vocabulary. Closed enum.
	public static final String STATE_TASK_AVAILABLE          = "task-available";
	public static final String STATE_TASK_RESERVED_BY_ME     = "task-reserved-by-me";
	public static final String STATE_TASK_RESERVED_BY_OTHER  = "task-reserved-by-other";
	public static final String STATE_TASK_COMPLETED          = "task-completed";
	public static final String STATE_TASK_CANCELLED          = "task-cancelled";
	public static final String STATE_PROCESS_COMPLETED       = "process-completed";
	public static final String STATE_PROCESS_TERMINATED      = "process-terminated";
	public static final String STATE_PROCESS_FAILED          = "process-failed";
	public static final String STATE_PROCESS_AWAITING_ACTION = "process-awaiting-action";
	public static final String STATE_NO_INSTANCE             = "no-instance";
	public static final String STATE_HAS_ACTIVE_INSTANCE     = "has-active-instance";

	private static final Set<String> ALL_STATES = Set.of(
		STATE_TASK_AVAILABLE, STATE_TASK_RESERVED_BY_ME, STATE_TASK_RESERVED_BY_OTHER,
		STATE_TASK_COMPLETED, STATE_TASK_CANCELLED,
		STATE_PROCESS_COMPLETED, STATE_PROCESS_TERMINATED, STATE_PROCESS_FAILED,
		STATE_PROCESS_AWAITING_ACTION, STATE_NO_INSTANCE, STATE_HAS_ACTIVE_INSTANCE
	);

	public VisibilityMappingTraitDefinition() {
		super(ProcessTraits.VISIBILITY_MAPPING);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<NodeInterface> domNode       = new StartNode(traitsInstance, DOM_NODE_PROPERTY,      StructrTraits.DOM_NODE_HAS_VISIBILITY_MAPPING);
		final Property<NodeInterface> boundProcess  = new EndNode(traitsInstance,   BOUND_PROCESS_PROPERTY, ProcessTraits.VISIBILITY_MAPPING_FOR_BPMN_PROCESS);
		final Property<NodeInterface> boundStep     = new EndNode(traitsInstance,   BOUND_STEP_PROPERTY,    ProcessTraits.VISIBILITY_MAPPING_AT_BPMN_ELEMENT);
		final Property<String> visibleWhen          = new EnumProperty(VISIBLE_WHEN_PROPERTY, ALL_STATES).indexed();
		final Property<String> boundProcessId       = new StringProperty(BOUND_PROCESS_ID_PROPERTY).indexed();
		final Property<String> boundStepBpmnId      = new StringProperty(BOUND_STEP_BPMN_ID_PROPERTY).indexed();

		return newSet(domNode, boundProcess, boundStep, visibleWhen, boundProcessId, boundStepBpmnId);
	}

	@Override
	public java.util.Map<String, Set<String>> getViews() {

		return java.util.Map.of(
			PropertyView.Public, newSet(BOUND_PROCESS_PROPERTY, BOUND_STEP_PROPERTY, VISIBLE_WHEN_PROPERTY, BOUND_PROCESS_ID_PROPERTY, BOUND_STEP_BPMN_ID_PROPERTY),
			PropertyView.Ui,     newSet(DOM_NODE_PROPERTY, BOUND_PROCESS_PROPERTY, BOUND_STEP_PROPERTY, VISIBLE_WHEN_PROPERTY, BOUND_PROCESS_ID_PROPERTY, BOUND_STEP_BPMN_ID_PROPERTY)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return Set.of(

			// evaluate(contextObject): the predicate. Returns Boolean. Called from the
			// DOMNode render gate, scoped to a specific ProcessInstance derived from the
			// page's render context (passed via NamedArguments as "contextObject").
			//
			// All states are evaluated relative to that single instance: a partial bound
			// to "process-completed" lights up only when THIS instance is completed, not
			// when the user has any historical completed instance. Engine grants drive
			// participant access (initiator + candidates + assignees of the instance);
			// the predicate just consults state, doesn't recompute permissions.
			new JavaMethod("evaluate", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					final NodeInterface mapping  = (NodeInterface) entity;
					final Traits vmTraits        = mapping.getTraits();

					final String state           = mapping.getProperty(vmTraits.key(VISIBLE_WHEN_PROPERTY));
					final String boundProcessId  = mapping.getProperty(vmTraits.key(BOUND_PROCESS_ID_PROPERTY));
					final String boundStepBpmnId = mapping.getProperty(vmTraits.key(BOUND_STEP_BPMN_ID_PROPERTY));

					if (state == null || state.isEmpty()) {
						return Boolean.FALSE;
					}

					// Resolve the context object (passed by DOMNodeTraitWrapper from the
					// render context's data object) to a ProcessInstance:
					//   * ProcessInstance -> use directly
					//   * TaskInstance    -> walk to its parent processInstance
					//   * anything else   -> no instance scope (only no-instance can match)
					final NodeInterface contextObject = unwrapToProcessInstance((NodeInterface) arguments.get("contextObject"));
					final Principal currentUser       = actionContext.getSecurityContext().getUser(false);

					// Engine state queries run as superuser. Engine grants determine
					// whether the user can see the page at all; predicate just inspects
					// state.
					final App suApp = StructrApp.getInstance(SecurityContext.getSuperUserInstance());

					final boolean result = evaluatePredicate(suApp, state, contextObject, boundProcessId, boundStepBpmnId, currentUser);
					logger.debug("VM[{}] state={} boundProcess={} boundStep={} ctxInstance={} user={} -> {}",
						mapping.getUuid(), state, boundProcessId, boundStepBpmnId,
						contextObject == null ? "null" : contextObject.getUuid(),
						currentUser == null ? "anonymous" : currentUser.getName(),
						result);
					return Boolean.valueOf(result);
				}

				@Override
				public String getDescription() {
					return "Evaluate this VisibilityMapping for the current security context, scoped to a render-context ProcessInstance / TaskInstance. Returns true if the bound process / step is in the configured state.";
				}
			}
		);
	}

	/**
	 * Resolve a render-context object to the ProcessInstance to evaluate against.
	 * Accepts a ProcessInstance (used directly) or a TaskInstance (walked to its
	 * parent instance). Anything else, or null, returns null so process-scoped
	 * states won't match.
	 */
	private static NodeInterface unwrapToProcessInstance(final NodeInterface ctx) {
		if (ctx == null) return null;
		final Traits traits = ctx.getTraits();
		if (traits.contains(ProcessTraits.PROCESS_INSTANCE)) {
			return ctx;
		}
		if (traits.contains(ProcessTraits.TASK_INSTANCE)) {
			return ctx.getProperty(Traits.of(ProcessTraits.TASK_INSTANCE)
				.key(TaskInstanceTraitDefinition.PROCESS_INSTANCE_PROPERTY));
		}
		return null;
	}

	/**
	 * Evaluate a state against the given ProcessInstance. The instance defines
	 * the scope: tasks belong to it, status is read from it. Cross-version safety
	 * is automatic because tasks belong to one instance and one definition;
	 * step-matching is by bpmnId not node identity.
	 */
	private static boolean evaluatePredicate(final App app, final String state,
											 final NodeInterface instance,
											 final String boundProcessId,
											 final String boundStepBpmnId,
											 final Principal currentUser) throws FrameworkException {

		// "No process instance exists for me yet". Lights up "starter" partials
		// on a process page when the current user has no active instance for
		// the bound process. Semantics:
		//
		//   * No bound process configured: trust render context. True when the
		//     page passed no instance / task context; false otherwise.
		//   * Bound process configured: query for any active ProcessInstance
		//     (status = running OR suspended) with initiator = currentUser AND
		//     definition.processId = boundProcessId. True if none exist.
		//
		// The DB query is the authoritative check: we don't depend on the page
		// having a binding that filters by initiator. The initiator key is
		// indexed, so the query is cheap relative to the per-render evaluation.
		if (STATE_NO_INSTANCE.equals(state)) {
			if (boundProcessId == null || boundProcessId.isEmpty()) {
				return instance == null;
			}
			if (currentUser == null) {
				return false;
			}
			return !hasActiveInstanceForUser(app, currentUser, boundProcessId);
		}

		// Inverse of no-instance: the current user already has an active
		// (running / suspended) instance of the bound process. Context-free
		// like no-instance, so it works on a catalog / landing page that has
		// no ProcessInstance in render context.
		if (STATE_HAS_ACTIVE_INSTANCE.equals(state)) {
			if (boundProcessId == null || boundProcessId.isEmpty()) {
				return instance != null;
			}
			if (currentUser == null) {
				return false;
			}
			return hasActiveInstanceForUser(app, currentUser, boundProcessId);
		}

		if (instance == null) {
			return false;
		}

		final Traits instTraits = instance.getTraits();
		final Traits taskTraits = Traits.of(ProcessTraits.TASK_INSTANCE);

		// Verify the VM applies to this instance: the instance's process must
		// match the VM's bound process (compared by processId, version-immune).
		if (boundProcessId != null && !boundProcessId.isEmpty()) {
			final NodeInterface proc = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY));
			if (proc == null) return false;
			final String procId = proc.getProperty(proc.getTraits().key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY));
			if (!boundProcessId.equals(procId)) return false;
		}

		final boolean taskScoped = state.startsWith("task-");
		if (taskScoped && (boundStepBpmnId == null || boundStepBpmnId.isEmpty())) {
			return false; // task-level state needs a bound step bpmnId
		}

		final Set<String> meAndMyGroups = currentUser != null ? currentUser.getOwnAndRecursiveParentsUuids() : Set.of();

		final PropertyKey<NodeInterface>           taskDefinedByKey  = taskTraits.key(TaskInstanceTraitDefinition.DEFINED_BY_PROPERTY);
		final PropertyKey<String>                  taskStatusKey     = taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY);
		final PropertyKey<NodeInterface>           taskAssigneeKey   = taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_PROPERTY);
		final PropertyKey<Iterable<NodeInterface>> taskCandidatesKey = taskTraits.key(TaskInstanceTraitDefinition.CANDIDATE_ASSIGNEES_PROPERTY);

		final PropertyKey<String>        instStatusKey    = instTraits.key(ProcessInstanceTraitDefinition.STATUS_PROPERTY);
		final PropertyKey<NodeInterface> instInitiatorKey = instTraits.key(ProcessInstanceTraitDefinition.INITIATOR_PROPERTY);
		final PropertyKey<Iterable<NodeInterface>> instTasksKey = instTraits.key(ProcessInstanceTraitDefinition.TASKS_PROPERTY);

		switch (state) {

			case STATE_TASK_AVAILABLE: {
				if (currentUser == null) return false;
				for (final NodeInterface t : tasksAtStep(instance, instTasksKey, taskDefinedByKey, boundStepBpmnId)) {
					if (TaskInstanceTraitDefinition.STATUS_AVAILABLE.equals(t.getProperty(taskStatusKey))
							&& userIsCandidate(t, taskCandidatesKey, meAndMyGroups)) return true;
				}
				return false;
			}

			case STATE_TASK_RESERVED_BY_ME: {
				if (currentUser == null) return false;
				for (final NodeInterface t : tasksAtStep(instance, instTasksKey, taskDefinedByKey, boundStepBpmnId)) {
					if (!TaskInstanceTraitDefinition.STATUS_RESERVED.equals(t.getProperty(taskStatusKey))) continue;
					final NodeInterface assignee = t.getProperty(taskAssigneeKey);
					if (assignee != null && meAndMyGroups.contains(assignee.getUuid())) return true;
				}
				return false;
			}

			case STATE_TASK_RESERVED_BY_OTHER: {
				// Reserved by someone else, but I am still a candidate (this is "my pool,
				// already claimed by a peer"). Without the candidate filter, this would
				// light up for any reserved task at the step regardless of involvement.
				if (currentUser == null) return false;
				for (final NodeInterface t : tasksAtStep(instance, instTasksKey, taskDefinedByKey, boundStepBpmnId)) {
					if (!TaskInstanceTraitDefinition.STATUS_RESERVED.equals(t.getProperty(taskStatusKey))) continue;
					final NodeInterface assignee = t.getProperty(taskAssigneeKey);
					final boolean assigneeIsOther = assignee != null && !meAndMyGroups.contains(assignee.getUuid());
					if (assigneeIsOther && userIsCandidate(t, taskCandidatesKey, meAndMyGroups)) return true;
				}
				return false;
			}

			case STATE_TASK_COMPLETED:
				return tasksAtStepWithStatusExist(instance, instTasksKey, taskDefinedByKey, taskStatusKey,
					boundStepBpmnId, TaskInstanceTraitDefinition.STATUS_COMPLETED);

			case STATE_TASK_CANCELLED:
				return tasksAtStepWithStatusExist(instance, instTasksKey, taskDefinedByKey, taskStatusKey,
					boundStepBpmnId, TaskInstanceTraitDefinition.STATUS_CANCELLED);

			case STATE_PROCESS_COMPLETED:
				return ProcessInstanceTraitDefinition.STATUS_COMPLETED.equals(instance.getProperty(instStatusKey));

			case STATE_PROCESS_TERMINATED:
				return ProcessInstanceTraitDefinition.STATUS_TERMINATED.equals(instance.getProperty(instStatusKey));

			case STATE_PROCESS_FAILED:
				return ProcessInstanceTraitDefinition.STATUS_ERROR.equals(instance.getProperty(instStatusKey));

			case STATE_PROCESS_AWAITING_ACTION: {
				// I started this instance, it's still running, and there's no active
				// task waiting for me right now. "Thanks, we'll get back to you" pages.
				if (currentUser == null) return false;
				if (!ProcessInstanceTraitDefinition.STATUS_RUNNING.equals(instance.getProperty(instStatusKey))) return false;
				final NodeInterface initiator = instance.getProperty(instInitiatorKey);
				if (initiator == null || !meAndMyGroups.contains(initiator.getUuid())) return false;
				final Iterable<NodeInterface> tasks = instance.getProperty(instTasksKey);
				if (tasks != null) {
					for (final NodeInterface t : tasks) {
						final String tStatus = t.getProperty(taskStatusKey);
						if (TaskInstanceTraitDefinition.STATUS_COMPLETED.equals(tStatus)
								|| TaskInstanceTraitDefinition.STATUS_CANCELLED.equals(tStatus)) continue;
						final NodeInterface assignee = t.getProperty(taskAssigneeKey);
						final boolean isMine = (assignee != null && meAndMyGroups.contains(assignee.getUuid()))
								|| userIsCandidate(t, taskCandidatesKey, meAndMyGroups);
						if (isMine) return false;
					}
				}
				return true;
			}

			default:
				logger.warn("VisibilityMapping: unknown state '{}'", state);
				return false;
		}
	}

	/**
	 * True if {@code currentUser} initiated an active (running or suspended)
	 * ProcessInstance whose process matches {@code boundProcessId}. Context-free
	 * (DB query, runs as the engine's superuser app), shared by the
	 * {@code no-instance} (negated) and {@code has-active-instance} states.
	 */
	private static boolean hasActiveInstanceForUser(final App app, final Principal currentUser, final String boundProcessId) throws FrameworkException {

		final NodeInterface userNode = app.getNodeById(currentUser.getUuid());
		if (userNode == null) {
			return false;
		}
		final Traits piTraits = Traits.of(ProcessTraits.PROCESS_INSTANCE);
		final PropertyKey<NodeInterface> piInitKey   = piTraits.key(ProcessInstanceTraitDefinition.INITIATOR_PROPERTY);
		final PropertyKey<NodeInterface> piProcKey   = piTraits.key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY);
		final PropertyKey<String>        piStatusKey = piTraits.key(ProcessInstanceTraitDefinition.STATUS_PROPERTY);
		final PropertyKey<String>        procIdKey   = Traits.of(ProcessTraits.BPMN_PROCESS).key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY);

		for (final NodeInterface pi : app.nodeQuery(ProcessTraits.PROCESS_INSTANCE).key(piInitKey, userNode).getResultStream()) {
			final String status = pi.getProperty(piStatusKey);
			if (!ProcessInstanceTraitDefinition.STATUS_RUNNING.equals(status)
					&& !ProcessInstanceTraitDefinition.STATUS_SUSPENDED.equals(status)) {
				continue;
			}
			final NodeInterface proc = pi.getProperty(piProcKey);
			if (proc == null) continue;
			if (boundProcessId.equals(proc.getProperty(procIdKey))) {
				return true;
			}
		}
		return false;
	}

	private static boolean userIsCandidate(final NodeInterface task,
										   final PropertyKey<Iterable<NodeInterface>> candidatesKey,
										   final Set<String> meAndMyGroups) {

		final Iterable<NodeInterface> candidates = task.getProperty(candidatesKey);
		if (candidates == null) return false;
		for (final NodeInterface c : candidates) {
			if (c != null && meAndMyGroups.contains(c.getUuid())) return true;
		}
		return false;
	}

	/**
	 * Return tasks of the given instance whose definedBy element has the given
	 * bpmnId. Matching by bpmnId (not node identity) so a re-imported definition
	 * doesn't break the predicate -- the instance still references its own
	 * version's elements, and bpmnId is stable across versions.
	 */
	private static Iterable<NodeInterface> tasksAtStep(final NodeInterface instance,
													   final PropertyKey<Iterable<NodeInterface>> instTasksKey,
													   final PropertyKey<NodeInterface> taskDefinedByKey,
													   final String stepBpmnId) {

		final Iterable<NodeInterface> tasks = instance.getProperty(instTasksKey);
		if (tasks == null) return java.util.Collections.emptyList();
		final java.util.List<NodeInterface> matches = new java.util.ArrayList<>();
		final PropertyKey<String> elementBpmnIdKey = Traits.of(ProcessTraits.BPMN_ELEMENT)
			.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY);
		for (final NodeInterface t : tasks) {
			final NodeInterface definedBy = t.getProperty(taskDefinedByKey);
			if (definedBy == null) continue;
			if (stepBpmnId.equals(definedBy.getProperty(elementBpmnIdKey))) {
				matches.add(t);
			}
		}
		return matches;
	}

	private static boolean tasksAtStepWithStatusExist(final NodeInterface instance,
													  final PropertyKey<Iterable<NodeInterface>> instTasksKey,
													  final PropertyKey<NodeInterface> taskDefinedByKey,
													  final PropertyKey<String> taskStatusKey,
													  final String stepBpmnId,
													  final String status) {

		for (final NodeInterface t : tasksAtStep(instance, instTasksKey, taskDefinedByKey, stepBpmnId)) {
			if (status.equals(t.getProperty(taskStatusKey))) return true;
		}
		return false;
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			VisibilityMapping.class, (traits, node) -> new VisibilityMappingTraitWrapper(traits, node)
		);
	}
}
