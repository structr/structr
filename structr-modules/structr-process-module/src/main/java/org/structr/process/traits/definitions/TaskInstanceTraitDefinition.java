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

import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.process.engine.ProcessEngine;
import org.structr.process.ProcessTraits;
import org.structr.schema.action.ActionContext;

import java.util.Date;
import java.util.Map;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.process.entity.TaskInstance;
import org.structr.process.traits.wrappers.TaskInstanceTraitWrapper;
import java.util.Set;

/**
 * A task instance is created when a token arrives at a userTask element.
 * It represents a work item that must be completed by a principal before the
 * process can advance.
 *
 * Status lifecycle (aligned with WS-HumanTask vocabulary):
 *   created   : just created, no assignee or candidate assignees
 *   available : has candidate assignees, no specific assignee yet (WS-HumanTask "Ready")
 *   reserved  : has a specific assignee (set by self-claim, admin assignment, delegation,
 *               or BPMN-declared humanPerformer at creation time). The {@code assigneeSetBy}
 *               audit field records how the assignment was established.
 *   completed : terminal
 *   cancelled : terminal
 *
 * Exposes two methods:
 *   POST /structr/rest/TaskInstance/{id}/claim    : claim this task (requires available status)
 *   POST /structr/rest/TaskInstance/{id}/complete : complete and advance the process
 */
public class TaskInstanceTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String STATUS_PROPERTY              = "status";
	public static final String ASSIGNEE_PROPERTY            = "assignee";
	public static final String ASSIGNEE_SET_BY_PROPERTY     = "assigneeSetBy";
	public static final String CANDIDATE_ASSIGNEES_PROPERTY = "candidateAssignees";
	public static final String DECLINED_BY_PROPERTY         = "declinedBy";
	public static final String CREATED_TIME_PROPERTY        = "createdTime";
	public static final String CLAIMED_TIME_PROPERTY        = "claimedTime";
	public static final String COMPLETED_TIME_PROPERTY      = "completedTime";
	public static final String CANCELLED_TIME_PROPERTY      = "cancelledTime";
	public static final String PROCESS_INSTANCE_PROPERTY    = "processInstance";
	public static final String DEFINED_BY_PROPERTY          = "definedBy";

	// Status constants
	public static final String STATUS_CREATED   = "created";
	public static final String STATUS_AVAILABLE = "available";
	public static final String STATUS_RESERVED  = "reserved";
	public static final String STATUS_COMPLETED = "completed";
	public static final String STATUS_CANCELLED = "cancelled";

	// assigneeSetBy audit values: how the current assignment was established.
	// null when there is no assignee (status=created or status=available).
	public static final String SET_BY_SELF       = "self";        // claim() by the assignee
	public static final String SET_BY_ADMIN      = "admin";       // assignTask() by an admin
	public static final String SET_BY_DELEGATION = "delegation";  // delegate() by a previous assignee or candidate
	public static final String SET_BY_BPMN       = "bpmn";        // BPMN-declared humanPerformer at task creation

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			TaskInstance.class, (traits, node) -> new TaskInstanceTraitWrapper(traits, node)
		);
	}

	public TaskInstanceTraitDefinition() {
		super(ProcessTraits.TASK_INSTANCE);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> status                                      = new StringProperty(STATUS_PROPERTY).indexed();
		final Property<NodeInterface> assignee                             = new EndNode(traitsInstance, ASSIGNEE_PROPERTY, ProcessTraits.TASK_INSTANCE_ASSIGNED_TO);
		final Property<String> assigneeSetBy                               = new StringProperty(ASSIGNEE_SET_BY_PROPERTY);
		final Property<Iterable<NodeInterface>> candidateAssignees         = new EndNodes(traitsInstance, CANDIDATE_ASSIGNEES_PROPERTY, ProcessTraits.TASK_INSTANCE_HAS_CANDIDATE_ASSIGNEE);
		final Property<Iterable<NodeInterface>> declinedBy                 = new EndNodes(traitsInstance, DECLINED_BY_PROPERTY, ProcessTraits.TASK_INSTANCE_DECLINED_BY);
		final Property<Date> createdTime                                   = new DateProperty(CREATED_TIME_PROPERTY);
		final Property<Date> claimedTime                                   = new DateProperty(CLAIMED_TIME_PROPERTY);
		final Property<Date> completedTime                                 = new DateProperty(COMPLETED_TIME_PROPERTY);
		final Property<Date> cancelledTime                                 = new DateProperty(CANCELLED_TIME_PROPERTY);
		final Property<NodeInterface> processInst                          = new EndNode(traitsInstance, PROCESS_INSTANCE_PROPERTY, ProcessTraits.TASK_INSTANCE_OF_PROCESS);
		final Property<NodeInterface> definedBy                            = new EndNode(traitsInstance, DEFINED_BY_PROPERTY, ProcessTraits.TASK_INSTANCE_DEFINED_BY);

		return newSet(status, assignee, assigneeSetBy, candidateAssignees, declinedBy, createdTime, claimedTime, completedTime, cancelledTime, processInst, definedBy);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(STATUS_PROPERTY, ASSIGNEE_PROPERTY, ASSIGNEE_SET_BY_PROPERTY, CANDIDATE_ASSIGNEES_PROPERTY, DECLINED_BY_PROPERTY, DEFINED_BY_PROPERTY, PROCESS_INSTANCE_PROPERTY),
			PropertyView.Ui, newSet(STATUS_PROPERTY, ASSIGNEE_PROPERTY, ASSIGNEE_SET_BY_PROPERTY, CANDIDATE_ASSIGNEES_PROPERTY, DECLINED_BY_PROPERTY, CREATED_TIME_PROPERTY, CLAIMED_TIME_PROPERTY, COMPLETED_TIME_PROPERTY, CANCELLED_TIME_PROPERTY, PROCESS_INSTANCE_PROPERTY, DEFINED_BY_PROPERTY)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return Set.of(

			new JavaMethod("claim", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {
					final SecurityContext securityContext = actionContext.getSecurityContext();
					final ProcessEngine engine = new ProcessEngine(securityContext);
					engine.claimTask((NodeInterface) entity);
					return entity;
				}

				@Override
				public String getDescription() {
					return "Claims this task for the calling user. Participant action: requires status=available and the caller to be a candidate assignee (directly or via group membership). For administrative reassignment, use 'assignTask' instead.";
				}
			},

			new JavaMethod("assignTask", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {
					final SecurityContext callerContext = actionContext.getSecurityContext();
					final NodeInterface task            = (NodeInterface) entity;

					// Authorization: caller must have accessControl on the task.
					// This is the admin-level distinction from 'claim': a participant
					// claims for themselves (potential-ownership check); an admin
					// assigns to anyone (accessControl check on the task).
					if (!task.isGranted(Permission.accessControl, callerContext, false)) {

						throw new FrameworkException(403,
							"Caller lacks accessControl on this task: cannot reassign. " +
							"Use 'claim' for participant self-assignment instead."
						);
					}

					final Object assigneeArg = arguments.toMap().get("assignee");
					if (assigneeArg == null) {

						throw new FrameworkException(422,
							"assignTask requires an 'assignee' parameter (User or Group node UUID)."
						);
					}

					final NodeInterface assignee = resolveAssignee(actionContext, assigneeArg);
					final ProcessEngine engine   = new ProcessEngine(callerContext);
					engine.assignTask(task, assignee);
					return entity;
				}

				@Override
				public String getDescription() {
					return "Administratively assigns this task to a User or Group. Caller must have accessControl on the task. The new assignee does not need to be among the BPMN-declared candidate assignees (admin override). Pass 'assignee' as a node UUID or {id: '<uuid>'} object.";
				}
			},

			new JavaMethod("complete", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {
					final SecurityContext securityContext = actionContext.getSecurityContext();
					final ProcessEngine engine = new ProcessEngine(securityContext);
					final java.util.Map<String, Object> params = arguments.toMap();
					engine.completeTask((NodeInterface) entity, params.isEmpty() ? null : params);
					return entity;
				}

				@Override
				public String getDescription() {
					return "Completes this user task and advances the process to the next step.";
				}
			},

			// completeWithSubject: declarative replacement for the legacy
			// `submitLeaveRequest`-style task listeners. In one round trip:
			//   1. Create a node of `subjectType` from the supplied form
			//      parameters (any args other than `subjectType`).
			//   2. Wire it as the parent ProcessInstance's subject.
			//   3. Call the existing complete() flow so token advancement,
			//      lifecycle events and parameter persistence run unchanged.
			//
			// Argument keys:
			//   subjectType : String        - SchemaNode type name (required).
			//                                 The EAM dispatch injects this
			//                                 from ActionMapping.dataType.
			//   <other>     : form fields   - become properties on the new
			//                                 subject node AND continue to
			//                                 the engine for parameter-value
			//                                 persistence.
			//
			// Returns the created subject node.
			new JavaMethod("completeWithSubject", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					final SecurityContext securityContext = actionContext.getSecurityContext();
					final java.util.Map<String, Object> args = new java.util.LinkedHashMap<>(arguments.toMap());

					final Object subjectTypeArg = args.remove("subjectType");
					if (!(subjectTypeArg instanceof String) || ((String) subjectTypeArg).isBlank()) {

						throw new FrameworkException(422, "completeWithSubject requires a 'subjectType' argument naming the SchemaNode type to instantiate as the process instance's subject.");
					}
					final String subjectType = ((String) subjectTypeArg).trim();
					if (!Traits.exists(subjectType)) {

						throw new FrameworkException(422, "completeWithSubject: unknown subjectType '" + subjectType + "' -- no SchemaNode of that name exists.");
					}

					// 1. Create the subject node UNDER SU.
					//
					// Why SU: `app.create(...)` under the caller's context
					// makes the caller the owner with full permissions on
					// the new node. For process-managed subjects that's
					// the wrong access model: the form submitter would end
					// up with read+write+accessControl on the LeaveRequest
					// they just filed, bypassing the engine-grant model
					// (&#167;11.17). Creating under SU means no caller-
					// scoped owner role is established; access flows
					// purely through engine grants on the parent instance
					// plus HAS_SUBJECT read propagation (see the rel
					// trait). Net result: the submitter has read-only on
					// their submitted LeaveRequest, reviewers gain read
					// when they claim downstream tasks, nobody gets write
					// unless the schema author grants it.
					final NodeInterface task = (NodeInterface) entity;
					final Traits taskTraits  = task.getTraits();
					final NodeInterface instance = task.getProperty(taskTraits.key(PROCESS_INSTANCE_PROPERTY));
					if (instance == null) {

						throw new FrameworkException(422, "completeWithSubject: task has no parent ProcessInstance (data integrity error).");
					}

					final SecurityContext suCtx = SecurityContext.getSuperUserInstance();
					final App suApp             = StructrApp.getInstance(suCtx);
					final org.structr.core.property.PropertyMap initialProps = args.isEmpty()
						? new org.structr.core.property.PropertyMap()
						: org.structr.core.property.PropertyMap.inputTypeToJavaType(suCtx, subjectType, args);
					final NodeInterface subject = suApp.create(subjectType, initialProps);

					// 2. Wire as the parent instance's subject -- also
					//    under SU, because the user holds read-only on the
					//    instance (engine grant from startProcess) and
					//    can't write the SUBJECT_PROPERTY themselves.
					final NodeInterface instanceSu = suApp.getNodeById(instance.getUuid());
					instanceSu.setProperty(instanceSu.getTraits().key(ProcessInstanceTraitDefinition.SUBJECT_PROPERTY), subject);

					// 3. Complete the task back under the caller's
					//    context. This stays user-scoped so engine grants
					//    on downstream tasks (R+W to the next assignee /
					//    candidates) are established as part of the
					//    caller's audited action -- the engine code path
					//    is unchanged from a plain `complete`.
					final ProcessEngine engine = new ProcessEngine(securityContext);
					engine.completeTask(task, args.isEmpty() ? null : args);
					return subject;
				}

				@Override
				public String getDescription() {
					return "Atomic 'create subject + complete task' for form-driven user tasks. Pass `subjectType` (the SchemaNode type name) and any field values; the method creates the node under SU (so the caller doesn't auto-become its owner), wires it as the parent ProcessInstance's subject, and completes the task. Replaces hand-written 'create then set subject' listeners. Subject access flows through engine grants on the instance plus HAS_SUBJECT read propagation -- the form submitter gets read-only on their submitted node, not full ownership.";
				}
			},

			new JavaMethod("cancelBoundaryTimer", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {
					final SecurityContext securityContext = actionContext.getSecurityContext();
					final NodeInterface task              = (NodeInterface) entity;

					final Object bpmnIdArg = arguments.toMap().get("bpmnId");
					if (bpmnIdArg == null) {

						throw new FrameworkException(422, "cancelBoundaryTimer requires a 'bpmnId' parameter (the boundary event's BPMN id, e.g. 'Boundary_Review_Escalate').");
					}

					final ProcessEngine engine = new ProcessEngine(securityContext);
					return engine.cancelBoundaryTimerByBpmnId(task, bpmnIdArg.toString());
				}

				@Override
				public String getDescription() {
					return "Cancel a single pending boundary timer attached to this task, identified by the boundary event's BPMN id. Use case: a 'claimed' task listener cancels the escalation timer once a reviewer picks up the task, so still-running work isn't preempted by an interrupting boundary timer. Returns the number of timers cancelled (0 if no match).";
				}
			},

			new JavaMethod("release", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {
					final SecurityContext securityContext = actionContext.getSecurityContext();
					final ProcessEngine engine = new ProcessEngine(securityContext);
					engine.releaseTask((NodeInterface) entity);
					return entity;
				}

				@Override
				public String getDescription() {
					return "Releases this task back to the available pool. Participant action: caller must be the current assignee. Status transitions to 'available'; the task can be re-claimed (potentially by the same caller). claimedTime is preserved as a 'previously-claimed' marker. Clears the assigneeSetBy audit field.";
				}
			},

			new JavaMethod("decline", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {
					final SecurityContext securityContext = actionContext.getSecurityContext();
					final ProcessEngine engine = new ProcessEngine(securityContext);
					engine.declineTask((NodeInterface) entity);
					return entity;
				}

				@Override
				public String getDescription() {
					return "Records that the calling user declines this task. Vote semantics: the caller's R+W stays intact (decline is reversible, they can claim later). The decline is captured in the task's declinedBy collection for audit and stalled-task detection. Caller must be among the (effective) candidate assignees.";
				}
			},

			new JavaMethod("delegate", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {
					final SecurityContext securityContext = actionContext.getSecurityContext();
					final NodeInterface task              = (NodeInterface) entity;

					final Object delegateArg = arguments.toMap().get("delegate");
					if (delegateArg == null) {

						throw new FrameworkException(422, "delegate requires a 'delegate' parameter (User or Group node UUID).");
					}
					final NodeInterface delegate = resolveAssignee(actionContext, delegateArg);

					final ProcessEngine engine = new ProcessEngine(securityContext);
					engine.delegateTask(task, delegate);
					return entity;
				}

				@Override
				public String getDescription() {
					return "Delegates this task to another User or Group. Participant action: caller must be the current assignee (when status=reserved) or a candidate assignee (when status=available). Sets the new assignee, status='reserved', assigneeSetBy='delegation', claimedTime=now, and grants R+W to the delegate. Pass 'delegate' as a node UUID or {id: '<uuid>'} object.";
				}
			},

			new JavaMethod("cancel", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {
					final SecurityContext callerContext = actionContext.getSecurityContext();
					final NodeInterface task            = (NodeInterface) entity;

					if (!task.isGranted(Permission.accessControl, callerContext, false)) {

						throw new FrameworkException(403,
							"Caller lacks accessControl on this task: cannot cancel."
						);
					}

					final ProcessEngine engine = new ProcessEngine(callerContext);
					engine.cancelTask(task);
					return entity;
				}

				@Override
				public String getDescription() {
					return "Administratively cancels this task. Caller must have accessControl on the task. Sets status='cancelled', cancelledTime=now, marks the waiting token as completed (no advance). The instance is left running for admin to terminate or otherwise handle; cancel is destructive at the task level only.";
				}
			},

			new JavaMethod("makeAvailable", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {
					final SecurityContext callerContext = actionContext.getSecurityContext();
					final NodeInterface task            = (NodeInterface) entity;

					if (!task.isGranted(Permission.accessControl, callerContext, false)) {

						throw new FrameworkException(403,
							"Caller lacks accessControl on this task: cannot return it to the pool."
						);
					}

					final ProcessEngine engine = new ProcessEngine(callerContext);
					engine.makeTaskAvailable(task);
					return entity;
				}

				@Override
				public String getDescription() {
					return "Administratively returns this task to the available pool: clears the assignee and assigneeSetBy, transitions status='available', revokes the previous assignee's grant if they are not a current candidate assignee, and re-grants R+W to all current candidate assignees. The 'available' lifecycle event fires so notification handlers can re-notify candidates. Caller must have accessControl on the task.";
				}
			}
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	/**
	 * Resolve the {@code assignee} argument of {@code assignTask} into a
	 * NodeInterface. Accepts a NodeInterface, a UUID string, or a Map with
	 * an {@code id} field. Lists are explicitly rejected: a task has at
	 * most one assignee.
	 */
	private static NodeInterface resolveAssignee(final ActionContext actionContext, final Object arg) throws FrameworkException {

		if (arg instanceof Iterable<?> || (arg != null && arg.getClass().isArray())) {

			throw new FrameworkException(422,
				"assignTask accepts a single 'assignee'. Pass exactly one User or Group, not a list."
			);
		}
		if (arg instanceof NodeInterface) {

			return (NodeInterface) arg;
		}

		String uuid = null;
		if (arg instanceof String) {

			uuid = (String) arg;

		} else if (arg instanceof Map) {

			final Object idObj = ((Map<?, ?>) arg).get("id");
			if (idObj instanceof String) {

				uuid = (String) idObj;
			}
		}
		if (uuid == null || uuid.isEmpty()) {

			throw new FrameworkException(422,
				"Cannot resolve assignee from value of type " +
				(arg != null ? arg.getClass().getName() : "null") +
				" (expected NodeInterface, UUID string, or {id: \"<uuid>\"})"
			);
		}
		final App app = StructrApp.getInstance(actionContext.getSecurityContext());
		final NodeInterface node = app.getNodeById(uuid);
		if (node == null) {

			throw new FrameworkException(422, "Assignee with id '" + uuid + "' not found");
		}
		return node;
	}
}
