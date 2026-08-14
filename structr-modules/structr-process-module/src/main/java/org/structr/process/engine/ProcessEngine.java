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
package org.structr.process.engine;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.Arguments;
import org.structr.core.api.NamedArguments;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.core.script.polyglot.config.ScriptConfig;
import org.structr.core.traits.Traits;
import org.structr.process.ProcessTraits;
import org.structr.process.bpmn.BpmnElementType;
import org.structr.process.entity.*;
import org.structr.process.traits.definitions.*;
import org.structr.schema.action.ActionContext;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Structr Process Engine. Executes BPMN process definitions by managing
 * tokens that traverse the process graph.
 *
 * The engine is stateless -- all process state is persisted in the graph as
 * ProcessInstance, ProcessToken, and TaskInstance nodes. Each token movement
 * is committed as a database transaction (per-step persistence).
 *
 * Entry points:
 *   startProcess(defNode)    -- create a new instance, place token on start event, advance
 *   completeTask(taskNode)   -- complete a user task and advance the waiting token
 */
public class ProcessEngine {

	private static final Logger logger = LoggerFactory.getLogger(ProcessEngine.class);

	private final Principal caller;

	/**
	 * Construct an engine for the given caller's security context. The engine
	 * internally runs as superuser (via {@link StructrApp#getInstance()} and an
	 * explicit {@link SecurityContext#getSuperUserInstance()} for scripts) so that
	 * newly created ProcessInstance / ProcessToken / TaskInstance /
	 * ProcessParameterValue nodes have no owner (engine-managed). The caller
	 * principal is remembered so the engine can grant access where appropriate
	 * (e.g. read on the ProcessInstance to the user who started it).
	 */
	public ProcessEngine(final SecurityContext callerContext) {

		final Principal callerPrincipal = (callerContext != null) ? callerContext.getUser(false) : null;

		// SuperUser is a singleton with no backing graph node and cannot be a grant endpoint.
		this.caller = (callerPrincipal instanceof SuperUser) ? null : callerPrincipal;
	}

	/**
	 * Re-fetch a node through the engine's superuser App ({@link StructrApp#getInstance()})
	 * so that all subsequent property reads on it use the elevated security context.
	 *
	 * Engine entry points are invoked with {@link NodeInterface} arguments that
	 * carry the caller's (user) security context. Property reads through such
	 * wrappers (especially collection rels like {@code TOKENS_PROPERTY}) filter
	 * by user permissions, which would hide engine-managed nodes (tokens,
	 * unowned tasks, parameter values). Re-fetching via the superuser App
	 * guarantees the engine sees the complete graph.
	 */
	private NodeInterface elevate(final NodeInterface node) throws FrameworkException {

		if (node == null) {

			return null;
		}

		return StructrApp.getInstance().getNodeById(node.getUuid());
	}

	private void grant(final NodeInterface node, final Principal principal, final Permission... permissions) throws FrameworkException {

		if (node == null || principal == null || permissions.length == 0) {

			return;
		}

		if (principal instanceof SuperUser) {

			return;
		}

		final AccessControllable ac = node.as(AccessControllable.class);

		for (final Permission p : permissions) {

			ac.grant(p, principal);
		}
	}

	/**
	 * Grant {@code read} on the participant-facing nodes attached to a process
	 * instance: the instance itself and, when attached, the subject. Used at
	 * every site where the engine establishes a new engagement between a
	 * principal and a task (creation, claim, assignTask, delegate,
	 * makeAvailable). Idempotent — re-granting an existing permission is a
	 * no-op in Structr's ACL.
	 *
	 * <p>Rationale: a candidate or assignee can't usefully render the process
	 * page without read on the instance (to bind {@code current}) and the
	 * subject (to render the domain data). Doing this in the engine is
	 * symmetric with the existing task R+W grant -- it's plumbing, not policy,
	 * and should not require an application-side listener to wire up.</p>
	 */
	private void grantParticipantReadAccess(final NodeInterface instance, final Principal principal) throws FrameworkException {

		if (instance == null || principal == null) {

			return;
		}

		grant(instance, principal, Permission.read);

		final NodeInterface subject = instance.getProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.SUBJECT_PROPERTY));
		if (subject != null) {

			grant(subject, principal, Permission.read);
		}
	}

	/**
	 * Start a new process instance from a BpmnDefinitions node.
	 * Creates the ProcessInstance, attaches the given subject (may be null
	 * for pure-workflow processes), places a token at the start event, and
	 * advances.
	 *
	 * Process semantics: each instance has at most one subject. For batch
	 * operations, callers loop and invoke startProcess once per subject -- the
	 * engine deliberately does not accept a collection here, because spawning
	 * N instances from one call would hide a major side effect.
	 *
	 * @param callerProcNode the BpmnProcess node to instantiate
	 * @param subject        the domain object this process instance operates on,
	 *                       or {@code null} for processes with no domain subject
	 * @return the created ProcessInstance node
	 */
	public NodeInterface startProcess(final NodeInterface callerProcNode, final NodeInterface subject) throws FrameworkException {

		return startProcess(callerProcNode, subject, null);
	}

	/**
	 * Start variant that also stores initial process parameters (e.g. the
	 * parameters collected by an Event-Action-Mapping 'start' action). Fields
	 * whose name matches the subject's schema populate the subject; the rest
	 * become {@code ProcessParameterValue} nodes on the instance. Stored before
	 * the {@code created}/{@code started} events so process listeners observe them.
	 */
	public NodeInterface startProcess(final NodeInterface callerProcNode, final NodeInterface subject, final Map<String, Object> parameters) throws FrameworkException {

		final App app = StructrApp.getInstance();
		// Re-fetch under the engine's superuser context so internal traversals
		// don't get filtered by the caller's permissions.

		final NodeInterface procNode = elevate(callerProcNode);
		final Traits procTraits      = procNode.getTraits();

		// Find the single top-level start event (BpmnProcess.getStartEvent rejects
		// an ambiguous definition with more than one).
		final NodeInterface startEvent = procNode.as(BpmnProcess.class).getStartEvent();
		if (startEvent == null) {

			throw new FrameworkException(422, "No startEvent found in process definition");
		}

		// Create ProcessInstance
		final NodeInterface instance = app.create(ProcessTraits.PROCESS_INSTANCE);
		final Traits instTraits      = instance.getTraits();

		instance.setProperty(instTraits.key(ProcessInstanceTraitDefinition.STATUS_PROPERTY), ProcessInstanceTraitDefinition.STATUS_RUNNING);
		instance.setProperty(instTraits.key(ProcessInstanceTraitDefinition.START_TIME_PROPERTY), new Date());
		instance.setProperty(instTraits.key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY), procNode);

		final String processName = procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.PROCESS_NAME_PROPERTY));
		instance.setProperty(instTraits.key("name"), processName != null ? processName : "Process Instance");

		// Record the initiator (may be null when invoked from a privileged context that doesn't
		// represent a real user, e.g. doPrivileged with no effective principal).
		if (caller != null) {

			instance.setProperty(instTraits.key(ProcessInstanceTraitDefinition.INITIATOR_PROPERTY), caller);
		}

		// Attach the domain subject this process operates on (e.g. LeaveRequest, Invoice).
		if (subject != null) {

			// Re-fetch under superuser too: the caller may pass us a wrapper bound
			// to their own context; the rel write would otherwise hit a permission
			// check on the subject from the user's perspective.
			instance.setProperty(instTraits.key(ProcessInstanceTraitDefinition.SUBJECT_PROPERTY), elevate(subject));
		}

		// Store initial parameters (e.g. an EAM 'start' action's params).
		// Subject-matching fields populate the subject; the rest become
		// ProcessParameterValues. Attributed to the start event, and done
		// before the lifecycle events so listeners observe them.
		if (parameters != null && !parameters.isEmpty()) {

			storeParameterValues(instance, startEvent, parameters);
		}

		// Grant the initiator read access on the ProcessInstance and its
		// subject so they can observe their own instance and the domain data
		// it operates on.
		grantParticipantReadAccess(instance, caller);

		// Fire 'created' before token placement: listeners observe an instance with a definition,
		// initiator, and possibly subject set, but no tokens yet.
		fireProcessEvent(BpmnProcessListenerTraitDefinition.EVENT_CREATED, instance);

		// Create token at start event
		final NodeInterface token = createToken(instance, startEvent);

		// Fire 'started' before advancing the token. A fully-automatic process runs
		// straight through to its end event during advanceToken() -- which sets the
		// instance to 'completed' and fires 'completed' -- so firing 'started' afterwards
		// would emit it after the process had already finished. Firing here guarantees
		// the lifecycle order created -> started -> ... -> completed for every process,
		// automatic or not; at this point the token sits on the (pass-through) start event.
		fireProcessEvent(BpmnProcessListenerTraitDefinition.EVENT_STARTED, instance);

		// Advance the token from the start event (start events are pass-through)
		advanceToken(instance, token);

		return instance;
	}

	/**
	 * Terminate a process instance. Sets status=terminated, marks all waiting tokens
	 * as completed (without advancing), and fires the {@code terminated} lifecycle
	 * event (via the OnModification hook on ProcessInstance).
	 *
	 * <p>Authorization: enforced by the JavaMethod entry point (caller must have
	 * accessControl on the instance, or be using a system context).</p>
	 */
	public void terminateProcess(final NodeInterface callerInstanceNode) throws FrameworkException {

		final ProcessInstance instance = elevate(callerInstanceNode).as(ProcessInstance.class);
		if (instance.isCompleted()) {

			throw new FrameworkException(422, "Cannot terminate a completed instance");
		}

		if (instance.isTerminated()) {

			throw new FrameworkException(422, "Instance is already terminated");
		}

		// Mark all waiting / active tokens as completed so the engine treats them as inert.
		// The instance is left in a terminal state; downstream handlers can clean up via
		// onProcessTerminated.
		for (final ProcessToken token : instance.getTokens()) {

			if (token.isActive() || token.isWaiting()) {

				token.markCompleted();
			}
		}

		instance.setStatus(ProcessInstanceTraitDefinition.STATUS_TERMINATED);
		instance.setEndTime(new Date());

		// 'terminated' event fires via OnModification on the status change.
		logger.info("Process instance {} terminated", instance.getUuid());
	}

	/**
	 * Suspend a process instance. Sets status=suspended; existing tokens are left in place,
	 * no new advancement until the instance is resumed. Fires the {@code suspended} lifecycle
	 * event (via the OnModification hook).
	 */
	public void suspendProcess(final NodeInterface callerInstanceNode) throws FrameworkException {

		final ProcessInstance instance = elevate(callerInstanceNode).as(ProcessInstance.class);
		if (!instance.isRunning()) {

			throw new FrameworkException(422, "Can only suspend a running instance (current status: " + instance.getStatus() + ")");
		}

		instance.setStatus(ProcessInstanceTraitDefinition.STATUS_SUSPENDED);

		// 'suspended' event fires via OnModification on the status change.
		logger.info("Process instance {} suspended", instance.getUuid());
	}

	/**
	 * Resume a suspended process instance. Sets status=running and explicitly fires the
	 * {@code resumed} lifecycle event. {@code resumed} is fired here rather than via
	 * OnModification because the OnModification hook cannot distinguish "running set
	 * during initial start" from "running set during resume" without inspecting prior
	 * state. The explicit fire keeps the two transitions distinct.
	 */
	public void resumeProcess(final NodeInterface callerInstanceNode) throws FrameworkException {

		final NodeInterface instanceNode = elevate(callerInstanceNode);
		final ProcessInstance instance = instanceNode.as(ProcessInstance.class);

		if (!instance.isSuspended()) {

			throw new FrameworkException(422, "Can only resume a suspended instance (current status: " + instance.getStatus() + ")");
		}

		instance.setStatus(ProcessInstanceTraitDefinition.STATUS_RUNNING);

		// Explicit fire: OnModification skips 'running' transitions (cannot disambiguate
		// initial start from resume), so 'resumed' is emitted here.
		fireProcessEvent(BpmnProcessListenerTraitDefinition.EVENT_RESUMED, instanceNode);

		logger.info("Process instance {} resumed", instance.getUuid());
	}

	/**
	 * Complete a user task and advance the process.
	 *
	 * @param parameters optional key-value map of process parameter values set by this task
	 */
	public void completeTask(final NodeInterface callerTaskNode, final Map<String, Object> parameters) throws FrameworkException {

		final App app = StructrApp.getInstance();
		// Re-fetch under superuser context: traversals to processInstance,
		// tokens, and parameter values must not be filtered by caller perms.
		final NodeInterface taskNode = elevate(callerTaskNode);
		final TaskInstance task = taskNode.as(TaskInstance.class);

		if (task.isCompleted()) {

			throw new FrameworkException(422, "Task is already completed");
		}

		if (task.isCancelled()) {

			throw new FrameworkException(422, "Task is cancelled");
		}

		// Enforce suspend: a task cannot be completed (and the token must not
		// advance) while the owning instance is not running.
		final ProcessInstance owningInstance = task.getProcessInstance();
		if (owningInstance != null && !owningInstance.isRunning()) {

			throw new FrameworkException(422, "Process instance is not running (status: " + owningInstance.getStatus() + "); cannot complete task");
		}

		// Mark task as completed
		task.setStatus(TaskInstanceTraitDefinition.STATUS_COMPLETED);
		task.setCompletedTime(new Date());

		// Get the process instance
		final ProcessInstance instance = task.getProcessInstance();
		if (instance == null) {

			throw new FrameworkException(422, "Task has no associated process instance");
		}

		// Find the waiting token at the user task element
		final NodeInterface userTaskElement = task.getDefinedBy();
		final NodeInterface waitingToken = findWaitingToken(instance, userTaskElement);

		if (waitingToken == null) {

			throw new FrameworkException(422, "No waiting token found at user task element");
		}

		// Lifecycle order: fire 'completed' listener BEFORE persisting form fields.
		// The listener receives the form fields via NamedArguments and may create
		// + attach the instance's subject (typical "submit"-style task pattern).
		//
		// Listeners that want to read just-completed values should use NamedArguments
		// (the parameters are passed in directly), not ${'$'}.process.<name> -- the
		// latter shows prior tasks' PVs only at this point in the lifecycle.
		final NodeInterface preListenerSubject = instance.getSubject();

		fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_COMPLETED, taskNode, parameters);

		final NodeInterface postListenerSubject     = instance.getSubject();
		final boolean listenerAttachedSubject       = (preListenerSubject == null && postListenerSubject != null);

		// Engine yields persistence when the listener attached the subject during
		// this completion: the listener already populated it from the form values
		// (the "submit"-style task pattern), so any engine-side write would be
		// redundant at best, and harmful when the listener applied transformations
		// or type conversions that the raw form strings would clobber.
		//
		// Otherwise: auto-route via storeParameterValues. With a pre-existing subject,
		// matching form fields update the subject; the rest become PVs. Without any
		// subject (pure control-flow process), everything becomes a PV.
		if (parameters != null && !parameters.isEmpty() && !listenerAttachedSubject) {

			storeParameterValues(instance, userTaskElement, parameters);

		} else if (listenerAttachedSubject) {

			logger.info("Task '{}' completion: subject '{}' was attached by the listener; engine yields persistence to it.", safeName(taskNode), postListenerSubject.getUuid());
		}

		// Reactivate the token and move it past the user task to the next element
		waitingToken.as(ProcessToken.class).markActive();

		// Cancel any pending boundary timers attached to this userTask --
		// they're moot now that the task is done.
		cancelTimersForActivity(userTaskElement, instance);

		completeTokenAndMoveToNext(instance, waitingToken, userTaskElement);
	}

	/**
	 * Signal an intermediate catch event, resuming the waiting token.
	 * The catch event is identified by its bpmnId within the process instance.
	 *
	 * @param eventBpmnId the bpmnId of the intermediateCatchEvent element
	 * @param parameters  optional key-value map of process parameter values
	 */
	public void signalEvent(final NodeInterface callerInstanceNode, final String eventBpmnId, final Map<String, Object> parameters) throws FrameworkException {

		final App app = StructrApp.getInstance();
		// Re-fetch under superuser context so token/element lookups are unfiltered.
		final NodeInterface instanceNode = elevate(callerInstanceNode);
		final Traits instTraits = instanceNode.getTraits();

		// Verify instance is running
		final String status = instanceNode.getProperty(instTraits.key(ProcessInstanceTraitDefinition.STATUS_PROPERTY));
		if (!ProcessInstanceTraitDefinition.STATUS_RUNNING.equals(status)) {

			throw new FrameworkException(422, "Process instance is not running (status: " + status + ")");
		}

		// Find the catch event element by bpmnId
		// Resolve the catch event WITHIN this instance's own definition version.
		// A global lookup would return the first bpmnId match in the index (often a
		// different, stale version) and the waiting token would not be found.
		final Traits elemTraits          = Traits.of(ProcessTraits.BPMN_ELEMENT);
		final NodeInterface catchElement = findElementByBpmnId(instanceNode, eventBpmnId);

		if (catchElement == null) {

			throw new FrameworkException(422, "No element found with bpmnId: " + eventBpmnId);
		}

		// Verify it is a catch event
		final String elementType = catchElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY));
		if (!BpmnElementType.INTERMEDIATE_CATCH_EVENT.matches(elementType)) {

			throw new FrameworkException(422, "Element " + eventBpmnId + " is not an intermediateCatchEvent (type: " + elementType + ")");
		}

		// Find the waiting token at this element
		final NodeInterface waitingToken = findWaitingToken(instanceNode, catchElement);
		if (waitingToken == null) {

			throw new FrameworkException(422, "No waiting token at catch event: " + eventBpmnId);
		}

		// Store parameter values if provided
		if (parameters != null && !parameters.isEmpty()) {

			storeParameterValues(instanceNode, catchElement, parameters);
		}

		// Reactivate and advance
		waitingToken.as(ProcessToken.class).markActive();
		completeTokenAndMoveToNext(instanceNode, waitingToken, catchElement);

		logger.info("Signalled catch event '{}' in process instance {}", eventBpmnId, instanceNode.getUuid());
	}

	// -----------------------------------------------------------------------
	// Core execution loop
	// -----------------------------------------------------------------------

	/**
	 * Advance a token from its current element to the next element(s).
	 * This is the heart of the engine -- it evaluates the current element type,
	 * determines the outgoing path(s), creates new tokens, and recurses for
	 * automatic elements (events, gateways, service/script tasks).
	 */
	// Token advancement runs iteratively via an explicit LIFO work stack (=
	// depth-first) so a long chain of automatic elements cannot overflow the call
	// stack. Public entry points call advanceToken(); nested advances (gateway
	// forks, pass-throughs, sub-processes) triggered while a drain is in progress
	// simply enqueue onto the same stack and are processed by the outermost call.
	// Because all sibling fork tokens are created (and enqueued) before any is
	// stepped, a branch that reaches an end event sees its siblings still active,
	// and an inclusive join is only reached once every sibling has arrived.
	private final java.util.Deque<NodeInterface> tokenWorkStack = new java.util.ArrayDeque<>();
	private boolean advancing = false;

	private void advanceToken(final NodeInterface instance, final NodeInterface token) throws FrameworkException {

		tokenWorkStack.addFirst(token);

		if (advancing) {

			return;
		}

		advancing = true;

		try {

			while (!tokenWorkStack.isEmpty()) {

				stepToken(instance, tokenWorkStack.pollFirst());
			}

		} finally {

			advancing = false;
			tokenWorkStack.clear();
		}
	}

	private void stepToken(final NodeInterface instance, final NodeInterface token) throws FrameworkException {

		final Traits tokenTraits           = token.getTraits();
		final NodeInterface currentElement = token.getProperty(tokenTraits.key(ProcessTokenTraitDefinition.AT_ELEMENT_PROPERTY));

		if (currentElement == null) {

			logger.warn("Token {} has no current element, cannot advance", token.getUuid());

			return;
		}

		final Traits elemTraits = currentElement.getTraits();
		final String elementType = currentElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY));

		logger.debug("Advancing token {} at element {} ({})", token.getUuid(), currentElement.getProperty(elemTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)), elementType);

		switch (BpmnElementType.fromBpmnName(elementType)) {

			case START_EVENT:
				// Pass-through: move to the single outgoing flow target
				completeTokenAndMoveToNext(instance, token, currentElement);
				break;

			case END_EVENT:
				// Token reaches the end: check if inside a sub-process
				completeToken(token);

				final NodeInterface parentElement = currentElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.PARENT_ELEMENT_PROPERTY));
				if (parentElement != null && isSubProcessLikeType(parentElement.getProperty(parentElement.getTraits().key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY)))) {

					// Sub-process end: resume the parent token
					resumeSubProcessParent(instance, parentElement);

				} else {

					checkProcessCompletion(instance);
				}

				break;

			case USER_TASK:
				// Create a TaskInstance, schedule any boundary timers, then put the token in waiting state.
				createTaskInstance(instance, currentElement);
				scheduleBoundaryTimers(instance, token, currentElement);
				token.as(ProcessToken.class).markWaiting();
				break;

			case SERVICE_TASK:
			case SCRIPT_TASK:
				// Run the body with Camunda inputOutput semantics (activity-local scope
				// when io mappings are present), then advance.
				runAutomaticTask(instance, currentElement, elemTraits, elementType);
				completeTokenAndMoveToNext(instance, token, currentElement);
				break;

			case MANUAL_TASK:
			case TASK:
				// Manual tasks and abstract tasks are pass-through for now
				// (manual tasks are performed outside the system)
				completeTokenAndMoveToNext(instance, token, currentElement);
				break;

			case EXCLUSIVE_GATEWAY:
				handleExclusiveGateway(instance, token, currentElement);
				break;

			case PARALLEL_GATEWAY:
				handleParallelGateway(instance, token, currentElement);
				break;

			case INCLUSIVE_GATEWAY:
				handleInclusiveGateway(instance, token, currentElement);
				break;

			case INTERMEDIATE_CATCH_EVENT:
				// Waiting for an external event -- token waits.
				token.as(ProcessToken.class).markWaiting();
				// If this catch event is a timer event, schedule the timer; the
				// service will fire it and advance the token when fireAt elapses.
				{
					final String evtDefType = currentElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.EVENT_DEF_TYPE_PROPERTY));
					if ("timerEventDefinition".equals(evtDefType)) {

						final String timerType  = currentElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.TIMER_TYPE_PROPERTY));
						final String timerValue = currentElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.TIMER_VALUE_PROPERTY));
						final Date fireAt = computeFireAt(timerType, timerValue);

						if (fireAt != null) {

							createTimer(fireAt, ProcessTimerTraitDefinition.TIMER_INTERMEDIATE, timerValue, instance, token, currentElement, true);
						}
					}
				}
				break;

			case INTERMEDIATE_THROW_EVENT:
				// Fire-and-forget event -- pass through
				completeTokenAndMoveToNext(instance, token, currentElement);
				break;

			case SUB_PROCESS:
			case TRANSACTION:
			case AD_HOC_SUB_PROCESS:
				// Enter the (sub-process-like) container: find its start event and create a token there
				handleSubProcess(instance, token, currentElement);
				break;

			default:
				logger.warn("Unknown element type '{}' at element {}, treating as pass-through", elementType, currentElement.getProperty(elemTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));
				completeTokenAndMoveToNext(instance, token, currentElement);
				break;
		}
	}

	// -----------------------------------------------------------------------
	// Gateway handlers
	// -----------------------------------------------------------------------

	/**
	 * Exclusive gateway (XOR): evaluate conditions on outgoing flows,
	 * take the first matching path (or the default flow).
	 */
	private void handleExclusiveGateway(final NodeInterface instance, final NodeInterface token, final NodeInterface element) throws FrameworkException {

		final BpmnElement gateway                  = element.as(BpmnElement.class);
		final List<BpmnSequenceFlow> outgoingFlows = Iterables.toList(gateway.getOutgoingFlows());

		if (outgoingFlows.isEmpty()) {

			throw new FrameworkException(422, "Exclusive gateway has no outgoing flows: " + gateway.getBpmnId());
		}

		// Check for default flow attribute on the gateway
		final String defaultFlowId = getAttributeValue(element, "default");
		BpmnSequenceFlow defaultFlow  = null;
		BpmnSequenceFlow selectedFlow = null;

		for (final BpmnSequenceFlow flow : outgoingFlows) {

			// Skip the default flow during condition evaluation
			final String flowId = flow.getBpmnId();
			if (flowId != null && flowId.equals(defaultFlowId)) {

				defaultFlow = flow;
				continue;
			}

			final String condition = flow.getConditionExpression();
			if (StringUtils.isNotBlank(condition)) {

				if (evaluateCondition(condition, element, instance)) {

					selectedFlow = flow;
					break;
				}

			} else if (selectedFlow == null) {

				// Unconditional, non-default flow -- use as fallback
				selectedFlow = flow;
			}
		}

		// Fall back to default flow
		if (selectedFlow == null) {

			selectedFlow = defaultFlow;
		}

		if (selectedFlow == null) {

			throw new FrameworkException(422, "No outgoing path matched at exclusive gateway: " + gateway.getBpmnId());
		}

		// Move token to the target of the selected flow
		moveTokenToElement(token, selectedFlow.getTargetElement());

		advanceToken(instance, token);
	}

	/**
	 * Parallel gateway: fork (one token becomes many) or join (many become one).
	 * Determined by counting incoming vs outgoing flows.
	 */
	private void handleParallelGateway(final NodeInterface instance, final NodeInterface token, final NodeInterface element) throws FrameworkException {

		final BpmnElement gateway                  = element.as(BpmnElement.class);
		final List<BpmnSequenceFlow> outgoingFlows = Iterables.toList(gateway.getOutgoingFlows());
		final List<BpmnSequenceFlow> incomingFlows = Iterables.toList(gateway.getIncomingFlows());

		if (outgoingFlows.size() > 1 && incomingFlows.size() <= 1) {

			// FORK: consume current token, create one new token per outgoing flow
			completeToken(token);

			for (final BpmnSequenceFlow flow : outgoingFlows) {

				final NodeInterface newToken = createToken(instance, flow.getTargetElement());
				advanceToken(instance, newToken);
			}

		} else if (incomingFlows.size() > 1) {

			// JOIN: wait until tokens have arrived from all incoming paths
			token.as(ProcessToken.class).markWaiting();

			// Fire only when a token has arrived on EVERY distinct incoming edge
			// (two tokens from the SAME edge must not satisfy the join).
			if (allIncomingEdgesDelivered(instance, element, incomingFlows)) {

				// All tokens arrived -- consume them all, then continue down EVERY outgoing flow (a gateway may both join and fork).
				consumeAllTokensAtElement(instance, element);

				for (final BpmnSequenceFlow out : outgoingFlows) {

						advanceToken(instance, createToken(instance, out.getTargetElement()));
					}
			}

		} else {

			// Single in, single out -- pass through
			completeTokenAndMoveToNext(instance, token, element);
		}
	}

	/**
	 * Inclusive gateway: fork to all outgoing paths whose conditions are true
	 * (at least one must match, or the default is taken). Join waits for all
	 * tokens that were actually created.
	 */
	private void handleInclusiveGateway(final NodeInterface instance, final NodeInterface token, final NodeInterface element) throws FrameworkException {

		final BpmnElement gateway                  = element.as(BpmnElement.class);
		final List<BpmnSequenceFlow> outgoingFlows = Iterables.toList(gateway.getOutgoingFlows());
		final List<BpmnSequenceFlow> incomingFlows = Iterables.toList(gateway.getIncomingFlows());

		if (outgoingFlows.size() > 1 && incomingFlows.size() <= 1) {

			// FORK: evaluate conditions on all outgoing flows
			final String defaultFlowId                = getAttributeValue(element, "default");
			final List<NodeInterface> selectedTargets = new LinkedList<>();
			BpmnSequenceFlow defaultFlow              = null;

			for (final BpmnSequenceFlow flow : outgoingFlows) {

				final String flowId = flow.getBpmnId();
				if (flowId != null && flowId.equals(defaultFlowId)) {

					defaultFlow = flow;
					continue;
				}

				final String condition = flow.getConditionExpression();
				if (StringUtils.isNotBlank(condition)) {

					if (evaluateCondition(condition, element, instance)) {

						selectedTargets.add(flow.getTargetElement());
					}

				} else {

					// No condition -- always taken
					selectedTargets.add(flow.getTargetElement());
				}
			}

			// If no paths matched, take the default
			if (selectedTargets.isEmpty() && defaultFlow != null) {

				selectedTargets.add(defaultFlow.getTargetElement());
			}

			if (selectedTargets.isEmpty()) {

				throw new FrameworkException(422, "No outgoing path matched at inclusive gateway: " + gateway.getBpmnId());
			}

			// Consume current token, create new tokens for selected paths
			completeToken(token);

			for (final NodeInterface target : selectedTargets) {

				final NodeInterface newToken = createToken(instance, target);

				advanceToken(instance, newToken);
			}

		} else if (incomingFlows.size() > 1) {

			// JOIN: an inclusive join synchronises only the branches that actually
			// carry tokens. It fires once no OTHER in-flight token can still reach
			// this gateway. Using reachability (rather than "no active token exists
			// anywhere") is what makes an unrelated parallel branch -- one that will
			// never flow into this join -- not block it; that global-count check
			// deadlocked whenever independent concurrent work was in progress.
			//
			// This is reliable because advanceToken drains iteratively: all sibling
			// fork tokens are created (with their atElement set) before any is
			// stepped, so a sibling still heading here is seen as reachable and the
			// join waits for it.
			//
			// Note: reachability follows outgoing sequence flows within the same
			// (sub-)process scope; a token inside a sibling sub-process is treated as
			// unable to reach a join outside it. That matches the intent here (fix the
			// unrelated-branch deadlock) and is no worse than the previous behaviour.
			token.as(ProcessToken.class).markWaiting();

			if (!anyActiveTokenCanReach(instance, element)) {

				// Every branch that could still deliver a token has arrived.
				consumeAllTokensAtElement(instance, element);

				for (final BpmnSequenceFlow out : outgoingFlows) {

						advanceToken(instance, createToken(instance, out.getTargetElement()));
					}
			}

		} else {

			completeTokenAndMoveToNext(instance, token, element);
		}
	}

	// -----------------------------------------------------------------------
	// Sub-process handling
	// -----------------------------------------------------------------------

	/** True for the container element types that host their own start event (subProcess / transaction / adHocSubProcess). */
	private static boolean isSubProcessLike(final BpmnElement e) {

		return e != null && (e.isType(BpmnElementType.SUB_PROCESS) || e.isType(BpmnElementType.TRANSACTION) || e.isType(BpmnElementType.AD_HOC_SUB_PROCESS));
	}

	private static boolean isSubProcessLikeType(final String typeName) {

		final BpmnElementType t = BpmnElementType.fromBpmnName(typeName);

		return t == BpmnElementType.SUB_PROCESS || t == BpmnElementType.TRANSACTION || t == BpmnElementType.AD_HOC_SUB_PROCESS;
	}

	private void handleSubProcess(final NodeInterface instance, final NodeInterface token, final NodeInterface subProcessElement) throws FrameworkException {

		// Find the start event inside the sub-process (child element)
		final Traits elemTraits                     = subProcessElement.getTraits();
		final Iterable<NodeInterface> childElements = subProcessElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.CHILD_ELEMENTS_PROPERTY));
		NodeInterface subStartEvent                 = null;

		if (childElements != null) {

			for (final NodeInterface child : childElements) {

				final String childType = child.getProperty(child.getTraits().key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY));
				if (BpmnElementType.START_EVENT.matches(childType)) {

					subStartEvent = child;
					break;
				}
			}
		}

		if (subStartEvent == null) {

			throw new FrameworkException(422, "Sub-process has no start event: " + getBpmnId(subProcessElement));
		}

		// Put the parent token in waiting state (it will be resumed when the sub-process completes)
		token.as(ProcessToken.class).markWaiting();

		// Create a new token at the sub-process start event
		final NodeInterface subToken = createToken(instance, subStartEvent);
		advanceToken(instance, subToken);
	}

	// -----------------------------------------------------------------------
	// Task execution
	// -----------------------------------------------------------------------

	private void executeAutomaticTask(final NodeInterface instance, final NodeInterface element, final Traits elemTraits, final String elementType, final Map<String, Object> localScope) throws FrameworkException {

		// Any automatic task carrying a script body runs it: a scriptTask's <script>,
		// or a serviceTask whose implementation was mapped to scriptContent on import
		// (a Camunda camunda:expression, or a Structr-native body). A serviceTask with
		// no runnable body stays a pass-through. The body may set variables (via
		// $.process.x = y or a transpiled execution.setVariable(...)): with a localScope
		// active (io mappings) those writes stay local; otherwise they persist through
		// the ProcessContext write sink.
		final String script = element.getProperty(elemTraits.key(BpmnElementTraitDefinition.SCRIPT_CONTENT_PROPERTY));
		if (StringUtils.isBlank(script)) {

			return;
		}

		final String scriptFormat = getAttributeValue(element, "scriptFormat");

		// Determine how to run the script from its declared format.
		final ScriptLanguage language = detectScriptLanguage(scriptFormat);

		// Foreign JavaScript (Camunda/Flowable) is transpiled to Structr JS;
		// everything else runs its source as-is.
		final String executableScript = (language == ScriptLanguage.FOREIGN_JAVASCRIPT)
			? transpileForeignScript(script)
			: script;

		try {

			final ActionContext ctx = new ActionContext(SecurityContext.getSuperUserInstance());
			installProcessContext(ctx, instance, element, localScope);
			final String expression = (language == ScriptLanguage.STRUCTR_SCRIPT)
				? "${" + executableScript + "}"
				: "${js{" + executableScript + "}}";
			Scripting.evaluate(ctx, element, expression, "automaticTask");

		} catch (Exception ex) {

			logger.warn("Automatic task {} failed (non-fatal): {}\n--- Original ---\n{}\n--- Transpiled ---\n{}", getBpmnId(element), ex.getMessage(), script, executableScript);
		}
	}

	/**
	 * Run an automatic task with Camunda {@code inputOutput} semantics.
	 *
	 * <p>When the element has io mappings, the activity gets its own local variable
	 * scope: input parameters are evaluated in the parent (process) scope and bound
	 * as locals; the body runs against those locals (reads local-over-process, writes
	 * stay local and do NOT persist); output parameters are then evaluated in the
	 * local scope and their results promoted (persisted) to process scope. The local
	 * scope is discarded afterwards, so inputs never leak, never clobber a same-named
	 * process variable or subject field, and parallel branches don't share them.</p>
	 *
	 * <p>Without io mappings the body runs normally and its writes persist to process
	 * scope (see {@link #executeAutomaticTask}).</p>
	 *
	 * <p>Scope is flat: outputs promote to the top-level process scope, not to an
	 * enclosing sub-process scope (a simplification vs. Camunda's nested scopes).</p>
	 */
	private void runAutomaticTask(final NodeInterface instance, final NodeInterface element, final Traits elemTraits, final String elementType) throws FrameworkException {

		final List<String[]> inputs  = parseIoParams(element, "inputs");
		final List<String[]> outputs = parseIoParams(element, "outputs");

		if (inputs.isEmpty() && outputs.isEmpty()) {

			// No io mappings: body writes persist to process scope.
			executeAutomaticTask(instance, element, elemTraits, elementType, null);

			return;
		}

		// Activity-local scope.
		final Map<String, Object> local = new LinkedHashMap<>();

		// Inputs: evaluated in the parent (process) scope, bound as locals.
		for (final String[] in : inputs) {

			local.put(in[0], evaluateSource(in[1], element, instance, null));
		}

		// Body: reads local-over-process, writes go local (not persisted).
		executeAutomaticTask(instance, element, elemTraits, elementType, local);

		// Outputs: evaluated in the local scope, promoted (persisted) to process scope.
		for (final String[] out : outputs) {

			final Object value            = evaluateSource(out[1], element, instance, local);
			final Map<String, Object> one = new HashMap<>();

			one.put(out[0], value);
			storeParameterValues(instance, element, one);
		}
	}

	/** Parse the stored ioMappings JSON into a list of {name, source} pairs for the given side ("inputs"/"outputs"). */
	private List<String[]> parseIoParams(final NodeInterface element, final String side) throws FrameworkException {

		final List<String[]> result = new LinkedList<>();
		final String json           = element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.IO_MAPPINGS_PROPERTY));

		if (StringUtils.isBlank(json)) {

			return result;
		}

		final com.google.gson.JsonElement root;

		try {

			root = com.google.gson.JsonParser.parseString(json);

		} catch (final com.google.gson.JsonSyntaxException ex) {

			logger.warn("Malformed ioMappings on {}: {}", getBpmnIdSafe(element), ex.getMessage());

			return result;
		}

		if (root == null || !root.isJsonObject()) {

			return result;
		}

		final com.google.gson.JsonElement listEl = root.getAsJsonObject().get(side);
		if (listEl == null || !listEl.isJsonArray()) {

			return result;
		}

		for (final com.google.gson.JsonElement itemEl : listEl.getAsJsonArray()) {

			if (!itemEl.isJsonObject()) {

				continue;
			}

			final com.google.gson.JsonObject item     = itemEl.getAsJsonObject();
			final com.google.gson.JsonElement nameEl   = item.get("name");

			if (nameEl == null || nameEl.isJsonNull()) {

				continue;
			}

			final com.google.gson.JsonElement srcEl = item.get("source");
			final String source                     = (srcEl != null && !srcEl.isJsonNull()) ? srcEl.getAsString() : null;

			result.add(new String[] { nameEl.getAsString(), source });
		}

		return result;
	}

	/**
	 * Evaluate an inputOutput parameter source against a given scope: a
	 * {@code ${...}} expression runs as JavaScript (bare names -- both process
	 * variables and, when present, local-scope names -- rewritten to
	 * {@code $.process.<name>}); anything else is a literal. {@code localScope} is
	 * null for inputs (evaluated in the parent scope) and the activity's local map
	 * for outputs (so they can read what the body produced).
	 */
	private Object evaluateSource(final String source, final NodeInterface element, final NodeInterface instance, final Map<String, Object> localScope) {

		if (source == null || source.isBlank()) {

			return null;
		}

		final String s = source.trim();
		if (!(s.startsWith("${") && s.endsWith("}"))) {

			return s; // literal
		}

		try {

			final ActionContext ctx = new ActionContext(SecurityContext.getSuperUserInstance());
			installProcessContext(ctx, instance, element, localScope);

			final String inner            = s.substring(2, s.length() - 1).trim();
			final java.util.Set<String> names = new java.util.HashSet<>(loadProcessVariables(instance).keySet());

			if (localScope != null) {

				names.addAll(localScope.keySet());
			}

			final String rewritten        = rewriteConditionExpression(inner, names);

			return Scripting.evaluate(ctx, element, "${js{" + rewritten + "}}", "ioMapping");

		} catch (final Exception ex) {

			logger.warn("ioMapping expression '{}' on {} failed (non-fatal): {}", source, getBpmnIdSafe(element), ex.getMessage());

			return null;
		}
	}

	/** getBpmnId without the checked exception, for logging. */
	private String getBpmnIdSafe(final NodeInterface element) {

		try {

			return getBpmnId(element);

		} catch (final Exception ex) {

			return "?";
		}
	}

	/**
	 * How a script task's {@code scriptFormat} is interpreted:
	 * <ul>
	 *   <li>{@code FOREIGN_JAVASCRIPT} -- Camunda/Flowable JavaScript, transpiled
	 *       to Structr JS before evaluation ({@code "javascript"} / {@code "js"}).</li>
	 *   <li>{@code STRUCTR_JAVASCRIPT} -- Structr-native JavaScript, run as-is
	 *       ({@code "structr-javascript"} / {@code "structr-js"}).</li>
	 *   <li>{@code STRUCTR_SCRIPT} -- anything else (including no format), run as
	 *       StructrScript.</li>
	 * </ul>
	 */
	enum ScriptLanguage { FOREIGN_JAVASCRIPT, STRUCTR_JAVASCRIPT, STRUCTR_SCRIPT }

	/**
	 * Classify a {@code scriptFormat} attribute value. Package-private and static
	 * so the (deliberately narrow) matching rules can be unit-tested directly.
	 */
	static ScriptLanguage detectScriptLanguage(final String scriptFormat) {

		if ("javascript".equalsIgnoreCase(scriptFormat) || "js".equalsIgnoreCase(scriptFormat)) {

			return ScriptLanguage.FOREIGN_JAVASCRIPT;
		}

		if ("structr-javascript".equalsIgnoreCase(scriptFormat) || "structr-js".equalsIgnoreCase(scriptFormat)) {

			return ScriptLanguage.STRUCTR_JAVASCRIPT;
		}

		return ScriptLanguage.STRUCTR_SCRIPT;
	}

	/**
	 * Transpile a foreign (Camunda/Flowable-style) JavaScript script into
	 * Structr-compatible JavaScript. The original source is preserved as
	 * a block comment, and equivalent Structr code is emitted below it.
	 *
	 * Supported transformations:
	 *   execution.getVariable("x")      -> $.process.x
	 *   execution.setVariable("x", val) -> $.process.x = val
	 *
	 * Lines that cannot be transpiled are kept as-is (best effort).
	 */
	public static String transpileForeignScript(final String script) {

		final StringBuilder out = new StringBuilder();

		// Preserve original as block comment
		out.append("/*\n");

		for (final String line : script.split("\n")) {

			out.append(line).append("\n");
		}

		out.append("*/\n");

		// Transpile line by line
		for (final String line : script.split("\n")) {

			final String trimmed = line.trim();

			// Skip empty lines
			if (trimmed.isEmpty()) {

				out.append("\n");
				continue;
			}

			// execution.setVariable("x", expr) -> $.process.x = expr;  (paren-depth aware,
			// so a value containing parentheses -- e.g. now() -- survives intact)
			// execution.getVariable("x")       -> $.process.x
			String transpiled = rewriteSetVariable(line);
			transpiled = GET_VARIABLE.matcher(transpiled).replaceAll("\\$.process.$1");

			// Bare function calls map onto Structr's function namespace by prefixing
			// "$." -- e.g. now() -> $.now(). Member calls, already-prefixed calls and
			// JS keywords / built-ins are left untouched.
			transpiled = prefixStructrFunctions(transpiled);

			// Camunda bean/service calls (receiver.method(...)) bind to a Structr service
			// class of the same (capitalized) name: notificationService.notify() ->
			// $.NotificationService.notify(). The importer scaffolds that service class and a
			// static stub method so the process runs; the user fills in the body.
			transpiled = rewriteServiceCalls(transpiled);

			out.append(transpiled).append("\n");
		}

		return out.toString();
	}

	/**
	 * Rewrite every {@code execution.setVariable("name", <expr>)} call on a line to
	 * {@code $.process.name = <expr>;}. The value expression is delimited by the
	 * setVariable call's OWN closing parenthesis, found with a paren-depth scan
	 * (quotes honored), so nested calls such as {@code now()} or {@code foo(bar())}
	 * survive intact -- unlike a non-greedy regex, which stops at the first {@code )}
	 * and corrupts the output (e.g. {@code now(;)}).
	 */
	static String rewriteSetVariable(final String line) {

		final String marker = "execution.setVariable";
		final StringBuilder out = new StringBuilder();
		int i = 0;

		while (true) {

			final int hit = line.indexOf(marker, i);
			if (hit < 0) {

				out.append(line, i, line.length());
				break;
			}

			// Must be a call: "execution.setVariable" then (optional ws) '('.
			int p = hit + marker.length();

			while (p < line.length() && Character.isWhitespace(line.charAt(p))) {

				p++;
			}

			if (p >= line.length() || line.charAt(p) != '(') {

				out.append(line, i, hit + marker.length());
				i = hit + marker.length();
				continue;
			}

			final int close = matchingParenIndex(line, p);
			if (close < 0) {

				// Unbalanced parentheses: keep the remainder verbatim (best effort).
				out.append(line.substring(i));
				break;
			}

			final String[] nv = splitFirstStringArg(line.substring(p + 1, close));
			out.append(line, i, hit);

			if (nv == null) {

				// Not a ("name", value) shape we understand -- keep the original call.
				out.append(line, hit, close + 1);
				i = close + 1;

			} else {

				out.append("$.process.").append(nv[0]).append(" = ").append(nv[1].trim()).append(";");
				i = close + 1;
				// Absorb an existing trailing ';' so we don't emit ';;'.
				if (i < line.length() && line.charAt(i) == ';') {

					i++;
				}
			}
		}

		return out.toString();
	}

	/** Index of the ')' closing the '(' at {@code openIdx}, honoring quoted strings; -1 if unbalanced. */
	static int matchingParenIndex(final String s, final int openIdx) {

		int depth = 0;
		char quote = 0;

		for (int i = openIdx; i < s.length(); i++) {

			final char c = s.charAt(i);

			if (quote != 0) {

				if (c == quote) {

					quote = 0;
				}

				continue;
			}

			if (c == '\'' || c == '"') {

				quote = c;

			} else if (c == '(') {

				depth++;

			} else if (c == ')') {

				depth--;

				if (depth == 0) {

					return i;
				}
			}
		}

		return -1;
	}

	/**
	 * Split an argument list of the form {@code "name", <expr>} into {@code [name, expr]}.
	 * The first argument must be a single- or double-quoted string literal (the variable
	 * name); everything after the first top-level comma is the value. Returns null if the
	 * shape doesn't match.
	 */
	static String[] splitFirstStringArg(final String args) {

		final String a = args.trim();
		if (a.isEmpty()) {

			return null;
		}

		final char q = a.charAt(0);
		if (q != '\'' && q != '"') {

			return null;
		}

		final int endQuote = a.indexOf(q, 1);
		if (endQuote < 0) {

			return null;
		}

		final String name = a.substring(1, endQuote);
		int c = endQuote + 1;

		while (c < a.length() && Character.isWhitespace(a.charAt(c))) {

			c++;
		}

		if (c >= a.length() || a.charAt(c) != ',') {

			return null;
		}

		final String value = a.substring(c + 1).trim();
		if (name.isEmpty() || value.isEmpty()) {

			return null;
		}

		return new String[] { name, value };
	}

	// JS keywords and common built-ins that look like calls but must NOT be
	// rewritten to Structr functions ($.<name>).
	private static final Set<String> NON_STRUCTR_CALLS = Set.of(
		"if", "else", "for", "while", "do", "switch", "case", "catch", "try", "finally",
		"return", "function", "var", "let", "const", "new", "typeof", "instanceof",
		"void", "delete", "in", "of", "throw", "this", "super", "yield", "await",
		"async", "break", "continue", "with", "class", "extends", "default",
		"true", "false", "null", "undefined",
		"Array", "Object", "String", "Number", "Boolean", "Date", "Math", "JSON",
		"RegExp", "Error", "Map", "Set", "Promise", "Symbol",
		"parseInt", "parseFloat", "isNaN", "isFinite", "eval",
		"encodeURIComponent", "decodeURIComponent", "execution"
	);

	// A bare function call: an identifier followed by '(' that is NOT a member
	// access (preceded by '.'), NOT already prefixed ('$.'), and NOT part of a
	// longer identifier.
	private static final Pattern BARE_FUNCTION_CALL = Pattern.compile("(?<![\\w.$])([A-Za-z_]\\w*)\\s*\\(");

	private static final Pattern GET_VARIABLE = Pattern.compile("execution\\.getVariable\\([\"']([^\"']+)[\"']\\)");

	/**
	 * Prefix bare function calls with Structr's {@code $.} namespace, e.g.
	 * {@code now()} -> {@code $.now()}. Member calls ({@code obj.foo()}),
	 * already-prefixed calls ({@code $.foo()}) and JS keywords / built-ins
	 * (see {@link #NON_STRUCTR_CALLS}) are left untouched. Best effort: a name that
	 * isn't a real Structr function becomes {@code $.<name>()} and fails at run time,
	 * but the original source is retained in the block comment above the transpiled body.
	 */
	static String prefixStructrFunctions(final String line) {

		final Matcher m = BARE_FUNCTION_CALL.matcher(line);
		final StringBuffer sb = new StringBuffer();

		while (m.find()) {

			final String name = m.group(1);
			final String replacement = NON_STRUCTR_CALLS.contains(name) ? m.group() : "$." + name + "(";

			m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}

		m.appendTail(sb);

		return sb.toString();
	}

	// Receivers that are NOT foreign services (script context / JS built-ins) and so
	// must never be rewritten to a $.<Type> service-class call.
	private static final Set<String> NON_SERVICE_RECEIVERS = Set.of(
		"$", "execution", "this", "process", "task", "Math", "JSON", "Object", "Array",
		"String", "Number", "Boolean", "Date", "RegExp", "console", "window", "document"
	);

	// Suffixes that mark a receiver as a service/bean by naming convention. We only
	// treat a receiver as a foreign service when it ends with one of these (on a
	// CamelCase boundary), so an ordinary variable like `task.complete()` or
	// `order.total()` is NOT minted into a service class. False negatives (an
	// unconventionally-named bean) just fall back to manual handling; false positives
	// (scaffolding a real variable) were the harmful case, so we err conservative.
	private static final String[] SERVICE_RECEIVER_SUFFIXES = {
		"Service", "Delegate", "Gateway", "Bean", "Client", "Manager", "Repository", "Facade", "Provider", "Dao", "Api", "Connector", "Adapter", "Endpoint"
	};

	// A bean/service call: receiver.method( ... ) where receiver is a bare identifier
	// (not itself a member access: negative lookbehind on '.', word char, '$').
	private static final Pattern SERVICE_CALL = Pattern.compile("(?<![\\w.$])([A-Za-z_]\\w*)\\.([A-Za-z_]\\w*)\\s*\\(");

	/** Upper-case the first character (service receiver -> service-class type name). */
	static String capitalize(final String s) {

		return (s == null || s.isEmpty()) ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	/**
	 * True if {@code receiver} should be treated as a foreign service/bean: not a known
	 * script-context / built-in object, and named by a service convention (ends with one
	 * of {@link #SERVICE_RECEIVER_SUFFIXES}). Shared by the rewrite and detection passes so
	 * they always agree on what is a service.
	 */
	static boolean isForeignServiceReceiver(final String receiver) {

		if (receiver == null || NON_SERVICE_RECEIVERS.contains(receiver)) {

			return false;
		}

		for (final String suffix : SERVICE_RECEIVER_SUFFIXES) {

			// Require a real prefix before the suffix so the receiver isn't just the bare word.
			if (receiver.length() > suffix.length() && receiver.endsWith(suffix)) {

				return true;
			}
		}

		return false;
	}

	/**
	 * Rewrite Camunda bean/service calls to Structr service-class calls:
	 * {@code notificationService.notify(x)} -> {@code $.NotificationService.notify(x)}.
	 * Receivers in {@link #NON_SERVICE_RECEIVERS} are left alone. Idempotent: an
	 * already-rewritten call ({@code $.Type.method(}) has '.' before the type name, so
	 * the negative lookbehind skips it on a second pass.
	 */
	static String rewriteServiceCalls(final String line) {

		final Matcher m = SERVICE_CALL.matcher(line);
		final StringBuffer sb = new StringBuffer();

		while (m.find()) {

			final String receiver = m.group(1);
			final String method   = m.group(2);
			final String replacement = isForeignServiceReceiver(receiver)
				? "$." + capitalize(receiver) + "." + method + "("
				: m.group();

			m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}

		m.appendTail(sb);

		return sb.toString();
	}

	/**
	 * Detect the foreign bean/service calls in a script. Returns a map of
	 * service-class name (capitalized receiver) -> method name -> argument count
	 * (the maximum seen across call sites). Powers the importer's service-class
	 * scaffolding. Receivers in {@link #NON_SERVICE_RECEIVERS} are ignored.
	 */
	public static Map<String, Map<String, Integer>> detectServiceCalls(final String script) {

		final Map<String, Map<String, Integer>> result = new LinkedHashMap<>();

		if (script == null) {

			return result;
		}

		final Matcher m = SERVICE_CALL.matcher(script);

		while (m.find()) {

			final String receiver = m.group(1);
			if (!isForeignServiceReceiver(receiver)) {

				continue;
			}

			final String method = m.group(2);
			final int open  = m.end() - 1;           // index of the '(' the match ended on
			final int close = matchingParenIndex(script, open);
			final int argc  = (close < 0) ? 0 : countTopLevelArgs(script.substring(open + 1, close));

			result.computeIfAbsent(capitalize(receiver), k -> new LinkedHashMap<>())
				.merge(method, argc, Math::max);
		}

		return result;
	}

	/** Count comma-separated top-level arguments (0 for an empty list), honoring nesting and quotes. */
	static int countTopLevelArgs(final String args) {

		final String a = args.trim();
		if (a.isEmpty()) {

			return 0;
		}

		int depth = 0;
		int count = 1;
		char quote = 0;

		for (int i = 0; i < a.length(); i++) {

			final char c = a.charAt(i);

			if (quote != 0) {

				if (c == quote) {

					quote = 0;
				}

				continue;
			}

			if (c == '\'' || c == '"') {

				quote = c;

			} else if (c == '(' || c == '[' || c == '{') {

				depth++;

			} else if (c == ')' || c == ']' || c == '}') {
				depth--;

			} else if (c == ',' && depth == 0) {

				count++;
			}
		}

		return count;
	}

	private void createTaskInstance(final NodeInterface instance, final NodeInterface userTaskElement) throws FrameworkException {

		final App app                   = StructrApp.getInstance();
		final NodeInterface task        = app.create(ProcessTraits.TASK_INSTANCE);
		final TaskInstance taskInstance = task.as(TaskInstance.class);

		taskInstance.setCreatedTime(new Date());
		taskInstance.setProcessInstance(instance);
		taskInstance.setDefinedBy(userTaskElement);

		// Set display name from the BPMN element name
		final String taskName = userTaskElement.as(BpmnElement.class).getBpmnName();

		task.setProperty(task.getTraits().key("name"), taskName != null ? taskName : "User Task");

		// Resolve performers declared on the BPMN element via standard
		// <bpmn:humanPerformer> / <bpmn:potentialOwner> sub-elements
		// (parsed by the importer into BpmnPerformer nodes linked via HAS_PERFORMER).
		final PrincipalExpressionResolver resolver = new PrincipalExpressionResolver(app, instance);
		final Iterable<BpmnPerformer> performers = userTaskElement.as(BpmnElement.class).getPerformers();
		NodeInterface assigneeNode = null;
		final List<NodeInterface> candidateAssignees = new LinkedList<>();

		if (performers != null) {

			for (final BpmnPerformer performer : performers) {

				final String kind         = performer.getKind();
				final String contextLabel = "task '" + taskName + "' (" + kind + ")";

				// Typed Principal binding takes priority over the expression
				// string. When the editor's Principal picker has populated the
				// HAS_PRINCIPAL relationship the linked nodes ARE the resolved
				// performers; expression evaluation is skipped entirely.
				final List<NodeInterface> linkedList = new LinkedList<>();

				for (final NodeInterface p : performer.getPrincipals()) {

					if (p != null) {

						linkedList.add(p);
					}
				}

				if (!linkedList.isEmpty()) {

					if (BpmnPerformerTraitDefinition.KIND_POTENTIAL_OWNER.equals(kind)) {

						candidateAssignees.addAll(linkedList);

					} else {

						// humanPerformer / generic performer: take the first
						// linked principal as the assignee. Multi-link on a
						// human performer is unusual but tolerated.
						if (assigneeNode == null) {

							assigneeNode = linkedList.get(0);
						}
					}

					continue;
				}

				// No typed binding: fall back to evaluating the expression.
				final String expression = performer.getExpression();
				if (StringUtils.isBlank(expression)) {

					continue;
				}

				if (BpmnPerformerTraitDefinition.KIND_POTENTIAL_OWNER.equals(kind)) {

					candidateAssignees.addAll(resolver.resolveAll(expression, contextLabel));

				} else {

					if (assigneeNode == null) {

						assigneeNode = resolver.resolveOne(expression.trim(), contextLabel);
					}
				}
			}
		}

		if (assigneeNode != null) {

			taskInstance.setAssignee(assigneeNode);
			taskInstance.setStatus(TaskInstanceTraitDefinition.STATUS_RESERVED);
			taskInstance.setAssigneeSetBy(TaskInstanceTraitDefinition.SET_BY_BPMN);

			// Direct assignment via <humanPerformer>: claimedTime equals createdTime,
			// i.e. the assignee was established at task-creation time, no separate claim event.
			taskInstance.setClaimedTime(new Date());

			grant(task, assigneeNode.as(Principal.class), Permission.read, Permission.write);

			// Generic participant grant: read on the parent ProcessInstance and
			// its subject (when attached). Read propagation on TASK_OF /
			// HAS_PARAMETER_VALUE then extends visibility to the instance's
			// other tasks and parameter values without per-object grants.
			// Single source of truth for participant access.
			grantParticipantReadAccess(instance, assigneeNode.as(Principal.class));

		} else if (!candidateAssignees.isEmpty()) {

			taskInstance.setCandidateAssignees(candidateAssignees);
			taskInstance.setStatus(TaskInstanceTraitDefinition.STATUS_AVAILABLE);

			// Grant read+write on the task to each candidate, plus read on the
			// parent ProcessInstance and its subject so propagation extends
			// visibility to the rest of the instance (sibling tasks, parameter
			// history) and the domain data the page renders.
			for (final NodeInterface owner : candidateAssignees) {

				final Principal ownerPrincipal = owner.as(Principal.class);

				grant(task, ownerPrincipal, Permission.read, Permission.write);
				grantParticipantReadAccess(instance, ownerPrincipal);
			}

		} else {

			// No performers declared. If the definition has the
			// defaultAssigneeFromInitiator flag set, fall back to assigning
			// the task to the process initiator. Convenience for editor-
			// authored processes where humanPerformer hasn't been wired up;
			// off by default so imported BPMN keeps spec semantics.
			NodeInterface fallbackAssignee = null;
			final ProcessInstance inst     = instance.as(ProcessInstance.class);
			final BpmnProcess def          = inst.getProcess();

			if (def != null && def.isDefaultAssigneeFromInitiator()) {

				fallbackAssignee = inst.getInitiator();
			}

			if (fallbackAssignee != null) {

				taskInstance.setAssignee(fallbackAssignee);
				taskInstance.setStatus(TaskInstanceTraitDefinition.STATUS_RESERVED);
				taskInstance.setAssigneeSetBy(TaskInstanceTraitDefinition.SET_BY_BPMN);
				taskInstance.setClaimedTime(new Date());
				grant(task, fallbackAssignee.as(Principal.class), Permission.read, Permission.write);
				grantParticipantReadAccess(instance, fallbackAssignee.as(Principal.class));

			} else {

				taskInstance.setStatus(TaskInstanceTraitDefinition.STATUS_CREATED);
			}
		}

		logger.info("Created task instance '{}' for process {} (status={})", taskName, instance.getUuid(), taskInstance.getStatus());

		// Lifecycle events: 'created' always; 'assigned' or 'available' depending on
		// the initial state we just established.
		fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_CREATED, task);

		final String createStatus = taskInstance.getStatus();
		if (createStatus != null) {

			switch (createStatus) {

				case TaskInstanceTraitDefinition.STATUS_RESERVED:
					fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_ASSIGNED, task);
					break;

				case TaskInstanceTraitDefinition.STATUS_AVAILABLE:
					fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_AVAILABLE, task);
					break;
			}
		}
	}

	/**
	 * Claim an available task for the calling user. Requires status = AVAILABLE and the caller
	 * to be among the candidate assignees (directly or via group membership).
	 */
	public void claimTask(final NodeInterface callerTaskNode) throws FrameworkException {

		if (caller == null) {

			throw new FrameworkException(401, "Cannot claim task without an authenticated caller");
		}

		// Re-fetch under superuser context so the candidateAssignees traversal
		// reflects the engine's view of the graph, not the caller's filter.
		final NodeInterface taskNode = elevate(callerTaskNode);
		final TaskInstance task      = taskNode.as(TaskInstance.class);

		if (!task.isAvailable()) {

			// Common authoring trap: action mapping resolves idExpression to a sibling
			// task in the same instance instead of the active one. Since participants
			// have read on every task of their instance (engine grants + propagation),
			// expressions like ${first(current.tasks).id} are unstable -- they may pick
			// a completed task. Configure the action's "step" so the engine resolves
			// the active task at that step, or use a step-scoped expression.
			throw new FrameworkException(422, "Task is not available for claim (status=" + task.getStatus() + "). "
				+ "Hint: check that the action mapping resolves to the active TaskInstance "
				+ "at the intended step -- prefer Control-process action with idExpression=${current.id} and step=<the user task>.");
		}

		// Collect the UUIDs of the caller's groups (privileged so we see groups regardless of ACLs).
		final Set<String> callerGroupIds = new HashSet<>();

		for (final NodeInterface g : caller.getParentsPrivileged()) {

			callerGroupIds.add(g.getUuid());
		}

		// The caller is eligible if they are a direct candidate assignee or if one of their groups is.
		boolean eligible = false;

		for (final NodeInterface owner : task.getCandidateAssignees()) {

			if (owner.getUuid().equals(caller.getUuid()) || callerGroupIds.contains(owner.getUuid())) {

				eligible = true;
				break;
			}
		}

		if (!eligible) {

			throw new FrameworkException(403, "Caller is not a candidate assignee of this task");
		}

		task.setAssignee(caller);
		task.setStatus(TaskInstanceTraitDefinition.STATUS_RESERVED);
		task.setAssigneeSetBy(TaskInstanceTraitDefinition.SET_BY_SELF);

		// Record the claim moment for audit: the gap between createdTime and
		// claimedTime tells operators how long the task waited for a claimer,
		// and a reserved task with no completedTime tells them the claimer is
		// still on it (or stalled).
		task.setClaimedTime(new Date());

		// Claim supersedes a prior decline: remove caller from declinedBy if present.
		final List<NodeInterface> currentDeclined = Iterables.toList(task.getDeclinedBy());
		boolean wasDeclined = currentDeclined.removeIf(d -> d.getUuid().equals(caller.getUuid()));

		if (wasDeclined) {

			task.setDeclinedBy(currentDeclined);
		}

		// Ensure the claimer has read+write on the task (may already be granted via candidate assignee).
		grant(taskNode, caller, Permission.read, Permission.write);

		// Generic participant grant: also ensure read on the parent
		// ProcessInstance and its subject. Idempotent if the candidate-
		// assignee path already granted them; defensive for claim flows where
		// the caller's eligibility came from group membership but no direct
		// instance grant exists.
		grantParticipantReadAccess(task.getProcessInstance(), caller);

		logger.info("Task '{}' claimed by '{}'", taskNode.getName(), caller.getName());

		// Lifecycle events: claimed (more specific) and assigned (general).
		fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_CLAIMED,  taskNode);
		fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_ASSIGNED, taskNode);
	}

	/**
	 * Administratively assign a task to a specific User or Group. Distinct from
	 * {@link #claimTask}:
	 *
	 * <ul>
	 *   <li>Claim is a participant action -- the caller takes the task for themselves
	 *       and must be a candidate assignee.</li>
	 *   <li>Assign is an admin action -- the caller assigns the task to someone else
	 *       and must have {@code accessControl} on the task. The new assignee does
	 *       not need to be in {@code candidateAssignees}; admin assignment can override
	 *       the BPMN-declared routing (e.g. a manager re-routing during illness).</li>
	 * </ul>
	 *
	 * Authorization for the admin operation is enforced at the JavaMethod entry
	 * point, not inside the engine -- this method assumes the caller has been
	 * validated to have accessControl on the task.
	 *
	 * @param callerTaskNode  the TaskInstance to assign
	 * @param callerAssignee  the User or Group to assign the task to (non-null)
	 */
	public void assignTask(final NodeInterface callerTaskNode, final NodeInterface callerAssignee) throws FrameworkException {

		if (callerAssignee == null) {

			throw new FrameworkException(422, "assignee must not be null");
		}

		final NodeInterface taskNode = elevate(callerTaskNode);
		final NodeInterface assignee = elevate(callerAssignee);
		final TaskInstance task      = taskNode.as(TaskInstance.class);

		if (task.isCompleted()) {

			throw new FrameworkException(422, "Cannot assign a completed task");
		}

		if (task.isCancelled()) {

			throw new FrameworkException(422, "Cannot assign a cancelled task");
		}

		// Capture the previous assignee BEFORE overwriting, so we can revoke
		// their grant if it came from an assignment (not from being a candidate).
		final NodeInterface previousAssignee = task.getAssignee();

		task.setAssignee(assignee);
		task.setStatus(TaskInstanceTraitDefinition.STATUS_RESERVED);
		task.setAssigneeSetBy(TaskInstanceTraitDefinition.SET_BY_ADMIN);
		task.setClaimedTime(new Date());

		// Revoke previous assignee's R+W if (a) it's a real reassignment (different
		// principal) AND (b) they're not a current candidate assignee. Candidate-assignee
		// membership keeps their grant; non-candidate admin-grants are removed cleanly.
		revokePreviousAssigneeGrantIfApplicable(taskNode, previousAssignee, assignee);

		grant(taskNode, assignee.as(Principal.class), Permission.read, Permission.write);

		// Generic participant grant on the parent ProcessInstance and its
		// subject for the new assignee.
		grantParticipantReadAccess(task.getProcessInstance(), assignee.as(Principal.class));

		logger.info("Task '{}' assigned to '{}' by admin caller '{}'", taskNode.getName(), assignee.getName(), (caller != null ? caller.getName() : "<system>"));

		fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_ASSIGNED, taskNode);
	}

	/**
	 * If {@code previousAssignee} is non-null, distinct from {@code newAssignee},
	 * and not a current candidate assignee of the task, revoke their R+W grant.
	 *
	 * Why the candidate check? When a Reviewer (in the Reviewers group) claims a task,
	 * they have R+W via the engine's candidate-assignee-time grant. If admin then
	 * reassigns the task to someone else, the original Reviewer should keep
	 * their candidate-derived access (they can still see the task in the group's queue).
	 * Only revoke for principals whose grant came purely from being assigned,
	 * i.e. not in candidateAssignees.
	 */
	private void revokePreviousAssigneeGrantIfApplicable(final NodeInterface taskNode, final NodeInterface previousAssignee, final NodeInterface newAssignee) throws FrameworkException {

		if (previousAssignee == null){

			return;
		}

		if (newAssignee != null && previousAssignee.getUuid().equals(newAssignee.getUuid())) {

			return;
		}

		// Membership check against direct candidateAssignees only. Group membership
		// is a separate inheritance path and we don't revoke at the user level
		// for group-derived grants: we don't have one to revoke.
		for (final NodeInterface candidate : taskNode.as(TaskInstance.class).getCandidateAssignees()) {

			if (candidate.getUuid().equals(previousAssignee.getUuid())) {

				return; // still a candidate, keep their grant
			}
		}

		try {

			final AccessControllable ac = taskNode.as(AccessControllable.class);

			ac.revoke(Permission.read,  previousAssignee.as(Principal.class));
			ac.revoke(Permission.write, previousAssignee.as(Principal.class));

			logger.info("Revoked R+W on task '{}' for previous assignee '{}'", taskNode.getName(), previousAssignee.getName());

		} catch (Exception ex) {

			logger.warn("Could not revoke previous assignee's grant on task '{}': {}", taskNode.getName(), ex.getMessage());
		}
	}

	/**
	 * Release a claimed task back to the available pool. Caller must be the
	 * current assignee. Status transitions to 'available'; the task can be
	 * re-claimed by any candidate assignee (including the same caller). The
	 * claimedTime is preserved as a 'previously-claimed' marker.
	 *
	 * The releaser's R+W grant is preserved -- if they were a potential
	 * owner before claiming, they still are. If they were assigned by an
	 * admin (not a candidate assignee), they keep the grant; admin can revoke
	 * separately if desired.
	 */
	public void releaseTask(final NodeInterface callerTaskNode) throws FrameworkException {

		if (caller == null) {

			throw new FrameworkException(401, "Cannot release task without an authenticated caller");
		}

		final NodeInterface taskNode = elevate(callerTaskNode);
		final TaskInstance task      = taskNode.as(TaskInstance.class);

		if (!task.isReserved()) {

			throw new FrameworkException(422, "Task is not assigned (status=" + task.getStatus() + "); cannot release");
		}

		final NodeInterface currentAssignee = task.getAssignee();
		if (currentAssignee == null || !caller.getUuid().equals(currentAssignee.getUuid())) {

			throw new FrameworkException(403, "Only the current assignee can release this task");
		}

		task.setAssignee(null);
		task.setStatus(TaskInstanceTraitDefinition.STATUS_AVAILABLE);
		task.setAssigneeSetBy(null);

		logger.info("Task '{}' released by '{}'", taskNode.getName(), caller.getName());

		fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_AVAILABLE, taskNode);
	}

	/**
	 * Record that the calling user declines this task -- a vote, not a permission
	 * change. The caller's R+W grant is preserved (they can change their mind);
	 * the decline is captured in the task's declinedBy collection for audit and
	 * stalled-task detection.
	 *
	 * If the caller previously declined (idempotent re-decline), this is a no-op.
	 * If the caller subsequently claims, claimTask removes them from declinedBy
	 * (claiming clearly supersedes a prior decline).
	 */
	public void declineTask(final NodeInterface callerTaskNode) throws FrameworkException {

		if (caller == null) {

			throw new FrameworkException(401, "Cannot decline task without an authenticated caller");
		}

		final App app                = StructrApp.getInstance();
		final NodeInterface taskNode = elevate(callerTaskNode);
		final TaskInstance task      = taskNode.as(TaskInstance.class);

		if (task.isTerminal()) {

			throw new FrameworkException(422, "Cannot decline a task in terminal status (" + task.getStatus() + ")");
		}

		// Authorization: caller must be among the effective candidate assignees
		// (directly or via group membership). Refusing decline from arbitrary
		// users keeps the audit signal meaningful -- only candidates can decline.
		if (!isCallerEffectivePotentialOwner(taskNode)) {

			throw new FrameworkException(403, "Caller is not a candidate assignee of this task; cannot decline");
		}

		// Add caller to declinedBy (idempotent).
		final List<NodeInterface> currentDeclined = Iterables.toList(task.getDeclinedBy());
		final NodeInterface callerNode            = app.getNodeById(caller.getUuid());
		boolean alreadyDeclined                   = false;

		for (final NodeInterface d : currentDeclined) {

			if (d.getUuid().equals(caller.getUuid())) {

				alreadyDeclined = true;
				break;
			}
		}

		if (!alreadyDeclined && callerNode != null) {

			currentDeclined.add(callerNode);
			task.setDeclinedBy(currentDeclined);
		}

		logger.info("Task '{}' declined by '{}'", taskNode.getName(), caller.getName());

		fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_DECLINED, taskNode);
	}

	/**
	 * Delegate this task to another User or Group. Participant action:
	 *
	 * <ul>
	 *   <li>From {@code reserved}: caller must be the current assignee.</li>
	 *   <li>From {@code available}: caller must be a candidate assignee.</li>
	 * </ul>
	 *
	 * The delegate becomes the new assignee with status=reserved, assigneeSetBy='delegation',
	 * and is granted R+W on the task. Distinct from administrative assign (which checks
	 * accessControl on the task instead of participant eligibility).
	 */
	public void delegateTask(final NodeInterface callerTaskNode, final NodeInterface callerDelegate) throws FrameworkException {

		if (caller == null) {

			throw new FrameworkException(401, "Cannot delegate task without an authenticated caller");
		}

		if (callerDelegate == null) {

			throw new FrameworkException(422, "delegate must not be null");
		}

		final NodeInterface taskNode = elevate(callerTaskNode);
		final NodeInterface delegate = elevate(callerDelegate);
		final TaskInstance task      = taskNode.as(TaskInstance.class);

		if (task.isTerminal()) {

			throw new FrameworkException(422, "Cannot delegate a task in terminal status (" + task.getStatus() + ")");
		}

		// Authorization: assigned -> caller must be the assignee. available ->
		// caller must be a candidate assignee.
		if (task.isReserved()) {

			final NodeInterface currentAssignee = task.getAssignee();
			if (currentAssignee == null || !caller.getUuid().equals(currentAssignee.getUuid())) {

				throw new FrameworkException(403, "Only the current assignee can delegate an assigned task");
			}

		} else if (task.isAvailable()) {

			if (!isCallerEffectivePotentialOwner(taskNode)) {

				throw new FrameworkException(403, "Caller is not a candidate assignee of this task; cannot delegate");
			}

		} else {

			throw new FrameworkException(422, "Task cannot be delegated in status=" + task.getStatus());
		}

		// Capture previous assignee (if any) for grant cleanup.
		final NodeInterface previousAssignee = task.getAssignee();

		task.setAssignee(delegate);
		task.setStatus(TaskInstanceTraitDefinition.STATUS_RESERVED);
		task.setAssigneeSetBy(TaskInstanceTraitDefinition.SET_BY_DELEGATION);
		task.setClaimedTime(new Date());

		// Same revocation rules as admin assignTask: revoke previous assignee's
		// grant if they're not also a candidate assignee.
		revokePreviousAssigneeGrantIfApplicable(taskNode, previousAssignee, delegate);

		grant(taskNode, delegate.as(Principal.class), Permission.read, Permission.write);

		// Generic participant grant on the parent ProcessInstance and its
		// subject for the delegate.
		grantParticipantReadAccess(task.getProcessInstance(), delegate.as(Principal.class));

		logger.info("Task '{}' delegated to '{}' by '{}'", taskNode.getName(), delegate.getName(), caller.getName());

		fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_ASSIGNED, taskNode);
	}

	/**
	 * Cancel a task. Sets status=cancelled, cancelledTime=now, and marks the
	 * waiting token as completed without advancing the process. The instance
	 * is left running -- admin can subsequently reassign + complete (to advance
	 * past this task) or terminate the instance entirely.
	 *
	 * Authorization (accessControl on the task) is enforced at the JavaMethod
	 * entry, not in the engine.
	 */
	public void cancelTask(final NodeInterface callerTaskNode) throws FrameworkException {

		final App app                = StructrApp.getInstance();
		final NodeInterface taskNode = elevate(callerTaskNode);
		final TaskInstance task      = taskNode.as(TaskInstance.class);

		if (task.isCompleted()) {

			throw new FrameworkException(422, "Task is already completed");
		}

		if (task.isCancelled()) {

			throw new FrameworkException(422, "Task is already cancelled");
		}

		task.setStatus(TaskInstanceTraitDefinition.STATUS_CANCELLED);
		task.setCancelledTime(new Date());

		// Find and complete the waiting token at the task's element so the
		// instance no longer carries an active token here. Process advancement
		// is intentionally not triggered -- admin must explicitly act on the
		// instance afterwards.
		final NodeInterface instance = task.getProcessInstance();
		final NodeInterface element  = task.getDefinedBy();

		if (instance != null && element != null) {

			final NodeInterface waitingToken = findWaitingToken(instance, element);
			if (waitingToken != null) {

				waitingToken.as(ProcessToken.class).markCompleted();
			}
		}

		logger.info("Task '{}' cancelled by admin caller '{}'", taskNode.getName(), (caller != null ? caller.getName() : "<system>"));

		fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_CANCELLED, taskNode);

		// Cancel pending boundary timers on this activity.
		cancelTimersForActivity(element, instance);
	}

	/**
	 * Administratively reset a task to {@code available}: clear the assignee,
	 * re-grant R+W to all current candidate assignees, and fire the
	 * {@code available} event so notifications go out again.
	 *
	 * Useful when admin wants to "give the task back to the pool" without
	 * picking a specific delegate -- e.g. after a reorganisation, when the
	 * original assignee left, or when the BPMN potentialOwner declaration was
	 * updated and the task should re-enter the candidate queue.
	 *
	 * Authorization (accessControl on the task) is enforced at the JavaMethod
	 * entry, not in the engine.
	 *
	 * Behavior:
	 * <ul>
	 *   <li>Refuses on terminal status (completed / cancelled).</li>
	 *   <li>Revokes the previous assignee's R+W if they're not also a candidate
	 *       assignee (same rule as assignTask reassignment).</li>
	 *   <li>Re-grants R+W to every current candidate assignee, idempotent for those
	 *       who already have it; restores access for those who lost it.</li>
	 *   <li>{@code declinedBy} is preserved -- decline votes are not auto-cleared
	 *       on make-available. Admin can clear them explicitly via property
	 *       editing if a fresh start is desired.</li>
	 * </ul>
	 */
	public void makeTaskAvailable(final NodeInterface callerTaskNode) throws FrameworkException {

		final NodeInterface taskNode = elevate(callerTaskNode);
		final TaskInstance task = taskNode.as(TaskInstance.class);

		if (task.isCompleted()) {

			throw new FrameworkException(422, "Cannot make a completed task available");
		}

		if (task.isCancelled()) {

			throw new FrameworkException(422, "Cannot make a cancelled task available");
		}

		if (task.isAvailable() && task.getAssignee() == null) {

			// Already available with no assignee: still useful to re-grant
			// candidate access in case a previous admin tweak removed grants.
			logger.info("Task '{}' already available; re-applying candidate-assignee grants", taskNode.getName());
		}

		final NodeInterface previousAssignee = task.getAssignee();

		task.setAssignee(null);
		task.setStatus(TaskInstanceTraitDefinition.STATUS_AVAILABLE);
		task.setAssigneeSetBy(null);

		// Revoke previous assignee's R+W if they're not a current candidate assignee.
		revokePreviousAssigneeGrantIfApplicable(taskNode, previousAssignee, null);

		// Re-grant R+W to every current candidate assignee, plus read on the
		// parent ProcessInstance and its subject. Idempotent for those who
		// already hold the grants; restores access where it was missing.
		final NodeInterface availInstance = task.getProcessInstance();

		for (final NodeInterface candidate : task.getCandidateAssignees()) {

			final Principal candidatePrincipal = candidate.as(Principal.class);

			grant(taskNode, candidatePrincipal, Permission.read, Permission.write);
			grantParticipantReadAccess(availInstance, candidatePrincipal);
		}

		logger.info("Task '{}' returned to available pool by admin caller '{}'", taskNode.getName(), (caller != null ? caller.getName() : "<system>"));

		fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_AVAILABLE, taskNode);
	}

	// -----------------------------------------------------------------------
	// Lifecycle event dispatch (task listeners)
	// -----------------------------------------------------------------------

	/**
	 * Fire a task lifecycle event. Dispatches to per-userTask BPMN listeners on
	 * the task's defining BpmnElement whose {@code event} matches. Each matched
	 * listener points directly at its SchemaMethod (no name resolution) and
	 * declares a phase:
	 * <ul>
	 *   <li>{@code on} -- runs inline (pre-commit). A thrown exception propagates
	 *       out and rolls back the surrounding transaction (validation / veto).</li>
	 *   <li>{@code after} (default) -- queued via the transaction post-process
	 *       queue and run after commit, so side-effects only fire if the
	 *       transition actually persisted.</li>
	 * </ul>
	 *
	 * @param eventName one of {@code created|assigned|claimed|available|declined|completed|cancelled}
	 * @param taskNode  the TaskInstance the event is about
	 */
	private void fireTaskEvent(final String eventName, final NodeInterface taskNode) throws FrameworkException {

		fireTaskEvent(eventName, taskNode, null);
	}

	/**
	 * Fire a task lifecycle event. The {@code submittedParams} variant is used
	 * for the {@code completed} event so the listener sees the just-submitted
	 * form fields via NamedArguments before they are persisted -- enabling the
	 * "submit"-style pattern where a listener creates and attaches the
	 * instance's subject from the form data.
	 */
	private void fireTaskEvent(final String eventName, final NodeInterface taskNode, final Map<String, Object> submittedParams) throws FrameworkException {

		// Collect the listeners matching this event up front. Enumeration
		// failures are non-fatal (the transition stands); per-listener dispatch
		// is handled below so an 'on'-phase exception can propagate.
		final List<NodeInterface> matching = new LinkedList<>();
		final Traits listenerTraits        = Traits.of(ProcessTraits.BPMN_TASK_LISTENER);

		try {

			final NodeInterface element = taskNode.getProperty(taskNode.getTraits().key(TaskInstanceTraitDefinition.DEFINED_BY_PROPERTY));
			if (element == null) {

				return;
			}

			final Iterable<NodeInterface> listeners = element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.TASK_LISTENERS_PROPERTY));
			if (listeners == null) {

				return;
			}

			final PropertyKey<String> eventKey = listenerTraits.key(BpmnTaskListenerTraitDefinition.EVENT_PROPERTY);

			for (final NodeInterface listener : listeners) {

				if (eventName.equals(listener.getProperty(eventKey))) {

					matching.add(listener);
				}
			}

		} catch (Exception ex) {

			logger.warn("Task listener enumeration for event '{}' on task '{}' failed: {}", eventName, safeName(taskNode), ex.getMessage());

			return;
		}

		final PropertyKey<String>        phaseKey  = listenerTraits.key(BpmnTaskListenerTraitDefinition.PHASE_PROPERTY);
		final PropertyKey<NodeInterface> methodKey = listenerTraits.key(BpmnTaskListenerTraitDefinition.METHOD_PROPERTY);

		for (final NodeInterface listener : matching) {

			final NodeInterface method = listener.getProperty(methodKey);
			if (method == null) {

				continue; // handler declared for the event but no method body yet
			}

			final String phase = listener.getProperty(phaseKey);
			if (BpmnTaskListenerTraitDefinition.PHASE_ON.equals(phase)) {

				// pre-commit: exceptions propagate and roll back the transition.
				runTaskListenerMethod(taskNode, method, eventName, submittedParams);

			} else {

				// after (default): run post-commit, side-effects only.
				queueAfterCommitTaskListener(taskNode, method, eventName, submittedParams);
			}
		}
	}

	/**
	 * Execute a task listener's method inline ({@code on} phase), with the task
	 * as {@code this} and the standard named arguments. Exceptions are NOT
	 * caught: they propagate so the surrounding engine transaction rolls back,
	 * giving the method true veto power over the transition.
	 */
	private void runTaskListenerMethod(final NodeInterface taskNode, final NodeInterface method, final String eventName, final Map<String, Object> submittedParams) throws FrameworkException {

		final Arguments args    = buildTaskListenerArgs(taskNode, eventName, submittedParams);
		final ActionContext ctx = new ActionContext(SecurityContext.getSuperUserInstance(), args.toMap());

		installTaskProcessContext(ctx, taskNode);
		runMethodWithContext(ctx, taskNode, method, "taskListener");
	}

	/** Install the $.process context for a task listener: scoped to the task's instance and defining element. */
	private void installTaskProcessContext(final ActionContext ctx, final NodeInterface taskNode) throws FrameworkException {

		final NodeInterface instance = taskNode.as(TaskInstance.class).getProcessInstance();
		if (instance == null) {

			return;
		}

		final NodeInterface element = taskNode.getProperty(taskNode.getTraits().key(TaskInstanceTraitDefinition.DEFINED_BY_PROPERTY));
		installProcessContext(ctx, instance, element);
	}

	/**
	 * Queue a task listener's method to run after the surrounding transaction
	 * commits ({@code after} phase). The runnable re-fetches the task and method
	 * by id and runs in its own transaction, so a post-commit side-effect never
	 * sees a half-built state and can't roll the transition back. Failures are
	 * logged, not propagated (the transition has already committed).
	 */
	private void queueAfterCommitTaskListener(final NodeInterface taskNode, final NodeInterface method, final String eventName, final Map<String, Object> submittedParams) {

		final String taskId   = taskNode.getUuid();
		final String methodId = method.getUuid();

		TransactionCommand.queuePostProcessProcedure(() -> {

			final App app = StructrApp.getInstance();

			try (final Tx tx = app.tx()) {

				final NodeInterface freshTask   = app.getNodeById(taskId);
				final NodeInterface freshMethod = app.getNodeById(methodId);

				if (freshTask != null && freshMethod != null) {

					final Arguments args    = buildTaskListenerArgs(freshTask, eventName, submittedParams);
					final ActionContext ctx = new ActionContext(SecurityContext.getSuperUserInstance(), args.toMap());

					installTaskProcessContext(ctx, freshTask);
					runMethodWithContext(ctx, freshTask, freshMethod, "taskListener");
				}

				tx.success();

			} catch (Throwable t) {

				logger.warn("After-commit task listener for event '{}' on task '{}' failed: {}", eventName, taskId, t.getMessage());
			}
		});
	}

	/**
	 * Build the named-argument set passed to every tier of task listener
	 * invocation. Same set in every tier so methods don't need to know
	 * which path resolved them.
	 *
	 * <p>Order: prior parameter values (loaded from the graph) -> just-submitted
	 * params (so they shadow prior PVs of the same name during the active task
	 * completion) -> engine-provided keys ({@code task}, {@code processInstance},
	 * {@code subject}, {@code eventName}, which win against any param-name
	 * collision).</p>
	 *
	 * <p>For the {@code completed} event in particular, {@code submittedParams}
	 * carries the form fields that have not yet been persisted -- the listener
	 * gets to see them before {@code storeParameterValues} routes them.</p>
	 */
	private Arguments buildTaskListenerArgs(final NodeInterface taskNode, final String eventName, final Map<String, Object> submittedParams) {

		final Map<String, Object> args = new LinkedHashMap<>();

		try {

			final NodeInterface instance = taskNode.getProperty(taskNode.getTraits().key(TaskInstanceTraitDefinition.PROCESS_INSTANCE_PROPERTY));
			if (instance != null) {

				args.putAll(loadParameterValues(instance));

				if (submittedParams != null) {

					args.putAll(submittedParams);
				}

				args.put("processInstance", instance);

				final NodeInterface subject = instance.getProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.SUBJECT_PROPERTY));
				if (subject != null) {

					args.put("subject", subject);
				}
			}

		} catch (Exception ignore) {

			// Best-effort enrichment. If processInstance / subject / params can't
			// be read for any reason, the listener still gets task + eventName.
		}

		args.put("task", taskNode);
		args.put("eventName", eventName);

		return NamedArguments.fromMap(args);
	}

	private String safeName(final NodeInterface node) {

		try {

			return node != null ? node.getName() : "<null>";

		} catch (Exception ex) {

			return "<unnamed>";
		}
	}

	// -----------------------------------------------------------------------
	// Process-level lifecycle event dispatch (process listeners)
	// -----------------------------------------------------------------------

	/**
	 * Fire a process-level lifecycle event for the given instance. Mirrors
	 * {@link #fireTaskEvent}, but operates on the process root: listeners are
	 * looked up on the BpmnProcess via {@code <structr:processListener>},
	 * filtered to those whose {@code event} matches. Each matched listener
	 * points directly at its SchemaMethod (no name resolution) and declares a
	 * phase:
	 * <ul>
	 *   <li>{@code on} -- runs inline (pre-commit); a thrown exception rolls
	 *       back the surrounding transition.</li>
	 *   <li>{@code after} (default) -- queued post-commit; side-effects only
	 *       fire if the transition actually persisted.</li>
	 * </ul>
	 *
	 * <p>The method runs with the {@code ProcessInstance} bound as {@code this}.
	 * This method is public so it can be called from the
	 * {@link org.structr.process.traits.definitions.ProcessInstanceTraitDefinition}
	 * OnModification hook (which forwards admin-UI / direct-setProperty status
	 * transitions into the engine's lifecycle dispatch path).</p>
	 *
	 * @param eventName one of {@code created|started|subjectAttached|completed|terminated|suspended|resumed}
	 * @param instance  the ProcessInstance the event is about
	 */
	public void fireProcessEvent(final String eventName, final NodeInterface instance) throws FrameworkException {

		final List<NodeInterface> matching = new LinkedList<>();
		final Traits listenerTraits        = Traits.of(ProcessTraits.BPMN_PROCESS_LISTENER);

		try {

			final NodeInterface defNode = instance.getProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY));
			if (defNode == null) {

				return;
			}

			final Iterable<NodeInterface> listeners = defNode.getProperty(defNode.getTraits().key(BpmnProcessTraitDefinition.PROCESS_LISTENERS_PROPERTY));
			if (listeners == null) {

				return;
			}

			final PropertyKey<String> eventKey = listenerTraits.key(BpmnProcessListenerTraitDefinition.EVENT_PROPERTY);

			for (final NodeInterface listener : listeners) {

				if (eventName.equals(listener.getProperty(eventKey))) {

					matching.add(listener);
				}
			}

		} catch (Exception ex) {

			logger.warn("Process listener enumeration for event '{}' on instance '{}' failed: {}", eventName, safeName(instance), ex.getMessage());

			return;
		}

		final PropertyKey<String>        phaseKey  = listenerTraits.key(BpmnProcessListenerTraitDefinition.PHASE_PROPERTY);
		final PropertyKey<NodeInterface> methodKey = listenerTraits.key(BpmnProcessListenerTraitDefinition.METHOD_PROPERTY);

		for (final NodeInterface listener : matching) {

			final NodeInterface method = listener.getProperty(methodKey);
			if (method == null) {

				continue; // handler declared for the event but no method body yet
			}

			final String phase = listener.getProperty(phaseKey);
			if (BpmnProcessListenerTraitDefinition.PHASE_ON.equals(phase)) {

				runProcessListenerMethod(instance, method, eventName);

			} else {

				queueAfterCommitProcessListener(instance, method, eventName);
			}
		}
	}

	/**
	 * Execute a process listener's method inline ({@code on} phase) with the
	 * instance as {@code this}. Exceptions propagate so the surrounding
	 * transition rolls back.
	 */
	private void runProcessListenerMethod(final NodeInterface instance, final NodeInterface method, final String eventName) throws FrameworkException {

		final Arguments args    = buildProcessListenerArgs(instance, eventName);
		final ActionContext ctx = new ActionContext(SecurityContext.getSuperUserInstance(), args.toMap());

		installProcessContext(ctx, instance, null);
		runMethodWithContext(ctx, instance, method, "processListener");
	}

	/**
	 * Queue a process listener's method to run after the surrounding
	 * transaction commits ({@code after} phase). Re-fetches instance and method
	 * by id and runs in its own transaction; failures are logged, not propagated.
	 */
	private void queueAfterCommitProcessListener(final NodeInterface instance, final NodeInterface method, final String eventName) {

		final String instanceId = instance.getUuid();
		final String methodId   = method.getUuid();

		TransactionCommand.queuePostProcessProcedure(() -> {

			final App app = StructrApp.getInstance();

			try (final Tx tx = app.tx()) {

				final NodeInterface freshInstance = app.getNodeById(instanceId);
				final NodeInterface freshMethod   = app.getNodeById(methodId);

				if (freshInstance != null && freshMethod != null) {

					final Arguments args    = buildProcessListenerArgs(freshInstance, eventName);
					final ActionContext ctx = new ActionContext(SecurityContext.getSuperUserInstance(), args.toMap());

					installProcessContext(ctx, freshInstance, null);
					runMethodWithContext(ctx, freshInstance, freshMethod, "processListener");
				}

				tx.success();

			} catch (Throwable t) {

				logger.warn("After-commit process listener for event '{}' on instance '{}' failed: {}", eventName, instanceId, t.getMessage());
			}
		});
	}

	/**
	 * Build the named-argument set passed to every tier of process listener
	 * invocation. Mirrors {@link #buildTaskListenerArgs} so methods can use the
	 * same {@code $.methodParameters} keys regardless of which event fired them.
	 * Keys: {@code processInstance}, {@code subject} (when set), {@code eventName}.
	 * The {@code task} key is intentionally omitted -- process events have no
	 * single task associated with them.
	 */
	private Arguments buildProcessListenerArgs(final NodeInterface instance, final String eventName) {

		// Order matters: see buildTaskListenerArgs for the rationale.
		final Map<String, Object> args = new LinkedHashMap<>();

		try {

			args.putAll(loadParameterValues(instance));

			final NodeInterface subject = instance.getProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.SUBJECT_PROPERTY));
			if (subject != null) {

				args.put("subject", subject);
			}

		} catch (Exception ignore) {

			// Best-effort enrichment.
		}

		args.put("processInstance", instance);
		args.put("eventName", eventName);

		return NamedArguments.fromMap(args);
	}

	/**
	 * Returns true if the caller is an effective candidate assignee of the task --
	 * i.e. directly listed in {@code candidateAssignees}, or a member of a Group
	 * that is listed.
	 */
	private boolean isCallerEffectivePotentialOwner(final NodeInterface taskNode) throws FrameworkException {

		if (caller == null) {

			return false;
		}

		final Set<String> callerGroupIds = new HashSet<>();

		for (final NodeInterface g : caller.getParentsPrivileged()) {

			callerGroupIds.add(g.getUuid());
		}

		for (final NodeInterface owner : taskNode.as(TaskInstance.class).getCandidateAssignees()) {

			if (owner.getUuid().equals(caller.getUuid()) || callerGroupIds.contains(owner.getUuid())) {

				return true;
			}
		}

		return false;
	}

	// -----------------------------------------------------------------------
	// Timer support
	// -----------------------------------------------------------------------

	/**
	 * Compute the absolute fire time for a BPMN timer expression.
	 *
	 * Supported formats:
	 * <ul>
	 *   <li>ISO 8601 duration ({@code PT30M}, {@code PT1H30M}, {@code P1D},
	 *       {@code P2DT3H}) -- relative to {@code now}</li>
	 *   <li>ISO 8601 instant ({@code 2026-04-25T14:30:00Z},
	 *       {@code 2026-04-25T14:30:00+02:00}) -- absolute</li>
	 * </ul>
	 *
	 * Cycle ({@code timeCycle}) format is not yet supported -- returns null
	 * with a logged warning so the engine doesn't crash on its presence.
	 *
	 * @param timerType  one of {@code timeDuration | timeDate | timeCycle}
	 * @param expression the BPMN timer expression body
	 * @return the absolute fire time, or null if unparseable / unsupported
	 */
	static Date computeFireAt(final String timerType, final String expression) {

		if (StringUtils.isBlank(expression)) {

			return null;
		}

		final String body = expression.trim();

		try {

			if ("timeDate".equals(timerType)) {

				// ISO 8601 instant; if no zone/offset is given, interpret in the system zone.
				try {

					return Date.from(Instant.parse(body));

				} catch (final DateTimeParseException ex) {

					return Date.from(java.time.LocalDateTime.parse(body).atZone(java.time.ZoneId.systemDefault()).toInstant());
				}
			}

			if ("timeDuration".equals(timerType)) {

				// ISO 8601 duration. Only the time-only PT... form is delegated to
				// Duration.parse; every other P-form (P1D, P1W, P1M, P1Y, ...) is routed
				// to the small fallback parser below, which also supports the week /
				// month / year components that Duration.parse rejects.
				if (body.startsWith("PT")) {

					return Date.from(Instant.now().plus(Duration.parse(body)));
				}

				// Handle "PnDTm..." forms -- split at "T" and combine day-component + time-component.
				final long millis = parseIso8601DurationMillis(body);
				if (millis >= 0) {

					return new Date(System.currentTimeMillis() + millis);
				}
			}

			if ("timeCycle".equals(timerType)) {

				logger.warn("timeCycle timer expressions are not yet supported (got '{}'); timer not scheduled", body);

				return null;
			}

		} catch (DateTimeParseException ex) {

			logger.warn("Invalid timer expression '{}' (type={}): {}", body, timerType, ex.getMessage());

			return null;
		}

		return null;
	}

	/**
	 * Minimal ISO 8601 duration parser for forms not handled by {@link Duration#parse}.
	 * Supports P[nY][nM][nW][nD][T[nH][nM][nS]] with year/month treated as approximations
	 * (year=365d, month=30d). Returns total milliseconds, or -1 if unparseable.
	 */
	static long parseIso8601DurationMillis(final String s) {

		if (s == null || !s.startsWith("P")) {

			return -1;
		}

		long millis           = 0;
		final int tIdx        = s.indexOf('T');
		final String datePart = (tIdx >= 0) ? s.substring(1, tIdx) : s.substring(1);
		final String timePart = (tIdx >= 0) ? s.substring(tIdx + 1) : "";

		try {

			millis += parseDurationComponent(datePart, 'Y', 365L * 24 * 60 * 60 * 1000);
			millis += parseDurationComponent(datePart, 'M',  30L * 24 * 60 * 60 * 1000);
			millis += parseDurationComponent(datePart, 'W',   7L * 24 * 60 * 60 * 1000);
			millis += parseDurationComponent(datePart, 'D',        24L * 60 * 60 * 1000);
			millis += parseDurationComponent(timePart, 'H',              60L * 60 * 1000);
			millis += parseDurationComponent(timePart, 'M',                   60L * 1000);
			millis += parseDurationComponent(timePart, 'S',                         1000L);

		} catch (NumberFormatException ex) {

			return -1;
		}

		return millis;
	}

	static long parseDurationComponent(final String s, final char unit, final long unitMillis) {

		final int idx = s.indexOf(unit);
		if (idx < 0) {

			return 0;
		}

		// scan backwards to find the start of the number
		int start = idx - 1;

		while (start >= 0 && (Character.isDigit(s.charAt(start)) || s.charAt(start) == '.')) {

			start--;
		}

		final String num = s.substring(start + 1, idx);
		if (num.isEmpty()) {

			return 0;
		}

		return (long) (Double.parseDouble(num) * unitMillis);
	}

	/**
	 * Create and persist a new ProcessTimer. The timer is fired by ProcessTimerService
	 * once {@code fireAt} elapses.
	 *
	 * @param fireAt          when to fire (UTC; computed via computeFireAt)
	 * @param timerType       intermediateTimer / boundaryTimer / timerStart
	 * @param expression      original BPMN expression (for audit / re-compute)
	 * @param instance        the ProcessInstance the timer belongs to (null for timerStart)
	 * @param token           the waiting/parent token (null for timerStart)
	 * @param element         the BPMN element (intermediateCatchEvent / boundaryEvent / startEvent)
	 * @param cancelActivity  for boundary events: interrupting (true) or non-interrupting (false)
	 * @return the created ProcessTimer node
	 */
	private NodeInterface createTimer(final Date fireAt, final String timerType, final String expression, final NodeInterface instance, final NodeInterface token, final NodeInterface element, final boolean cancelActivity) throws FrameworkException {

		final App app             = StructrApp.getInstance();
		final NodeInterface timer = app.create(ProcessTraits.PROCESS_TIMER);
		final Traits t            = timer.getTraits();

		timer.setProperty(t.key(ProcessTimerTraitDefinition.FIRE_AT_PROPERTY),           fireAt);
		timer.setProperty(t.key(ProcessTimerTraitDefinition.TIMER_TYPE_PROPERTY),        timerType);
		timer.setProperty(t.key(ProcessTimerTraitDefinition.TIMER_EXPRESSION_PROPERTY),  expression);
		timer.setProperty(t.key(ProcessTimerTraitDefinition.STATUS_PROPERTY),            ProcessTimerTraitDefinition.STATUS_PENDING);
		timer.setProperty(t.key(ProcessTimerTraitDefinition.CANCEL_ACTIVITY_PROPERTY),   cancelActivity);

		if (instance != null) {

			timer.setProperty(t.key(ProcessTimerTraitDefinition.INSTANCE_PROPERTY), instance);
		}

		if (token != null) {

			timer.setProperty(t.key(ProcessTimerTraitDefinition.TOKEN_PROPERTY), token);
		}

		if (element != null) {

			timer.setProperty(t.key(ProcessTimerTraitDefinition.ELEMENT_PROPERTY), element);
		}

		// Name for human readability in the Admin UI
		final String elemId = (element != null) ? (String) element.getProperty(element.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)) : "?";

		timer.setProperty(t.key("name"), timerType + "@" + elemId + " fires at " + fireAt);

		logger.info("Scheduled {} for element '{}' fireAt={} (expression='{}')", timerType, elemId, fireAt, expression);

		return timer;
	}

	/**
	 * Cancel a single pending boundary timer attached to the given task,
	 * identified by the boundary event's BPMN id.
	 *
	 * <p>Use case: a {@code claimed} task listener cancels the escalation timer
	 * (e.g. {@code Boundary_Review_Escalate}) once a reviewer picks up the task,
	 * so the still-running work isn't preempted. Reminders (non-interrupting
	 * timers) and other escalation paths stay armed.</p>
	 *
	 * <p>Returns the number of timers cancelled (0 if no match, 1 in the typical
	 * case, more only if duplicate bpmnIds exist on the same task).</p>
	 *
	 * <p>Engine-context (superuser) lookup so the cancellation succeeds
	 * regardless of caller permissions; the timer system is engine-managed.</p>
	 */
	public int cancelBoundaryTimerByBpmnId(final NodeInterface taskNode, final String boundaryBpmnId) throws FrameworkException {

		if (taskNode == null || StringUtils.isBlank(boundaryBpmnId)) {

			return 0;
		}

		final App app = StructrApp.getInstance();
		final NodeInterface instance = taskNode.as(TaskInstance.class).getProcessInstance();

		if (instance == null) {

			return 0;
		}

		final Traits timerTraits = Traits.of(ProcessTraits.PROCESS_TIMER);
		final Traits elemTraits  = Traits.of(ProcessTraits.BPMN_ELEMENT);
		int cancelled = 0;

		for (final NodeInterface timer : app.nodeQuery(ProcessTraits.PROCESS_TIMER)
				.key(timerTraits.key(ProcessTimerTraitDefinition.STATUS_PROPERTY), ProcessTimerTraitDefinition.STATUS_PENDING)
				.getResultStream()) {

			final NodeInterface tInstance = timer.getProperty(timerTraits.key(ProcessTimerTraitDefinition.INSTANCE_PROPERTY));
			if (tInstance == null || !tInstance.getUuid().equals(instance.getUuid())) {

				continue;
			}

			final NodeInterface elem = timer.getProperty(timerTraits.key(ProcessTimerTraitDefinition.ELEMENT_PROPERTY));
			if (elem == null) {

				continue;
			}

			final String elemBpmnId = elem.getProperty(elemTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
			if (!boundaryBpmnId.equals(elemBpmnId)) {

				continue;
			}

			timer.setProperty(timerTraits.key(ProcessTimerTraitDefinition.STATUS_PROPERTY), ProcessTimerTraitDefinition.STATUS_CANCELLED);
			cancelled++;

			logger.info("Cancelled boundary timer '{}' on task '{}' (explicit cancellation)", boundaryBpmnId, safeName(taskNode));
		}

		return cancelled;
	}

	void cancelTimersForActivity(final NodeInterface activityElement, final NodeInterface instance) throws FrameworkException {

		if (activityElement == null || instance == null) {

			return;
		}

		final App app            = StructrApp.getInstance();
		final Traits timerTraits = Traits.of(ProcessTraits.PROCESS_TIMER);
		final Iterable<NodeInterface> timers = app.nodeQuery(ProcessTraits.PROCESS_TIMER)
			.key(timerTraits.key(ProcessTimerTraitDefinition.STATUS_PROPERTY), ProcessTimerTraitDefinition.STATUS_PENDING)
			.getResultStream();

		for (final NodeInterface timer : timers) {

			final NodeInterface tInstance = timer.getProperty(timerTraits.key(ProcessTimerTraitDefinition.INSTANCE_PROPERTY));
			if (tInstance == null || !tInstance.getUuid().equals(instance.getUuid())) {

				continue;
			}

			final NodeInterface elem = timer.getProperty(timerTraits.key(ProcessTimerTraitDefinition.ELEMENT_PROPERTY));
			if (elem == null) {

				continue;
			}

			// Boundary timers: element is the boundaryEvent, attached to our activity.
			final String elemType = elem.getProperty(elem.getTraits().key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY));
			if (BpmnElementType.BOUNDARY_EVENT.matches(elemType)) {

				final String attachedTo = getAttachedToBpmnId(elem);
				if (attachedTo != null) {

					final String activityBpmnId = activityElement.getProperty(activityElement.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
					if (attachedTo.equals(activityBpmnId)) {

						timer.setProperty(timerTraits.key(ProcessTimerTraitDefinition.STATUS_PROPERTY), ProcessTimerTraitDefinition.STATUS_CANCELLED);
						logger.info("Cancelled boundary timer on element '{}' (parent activity completed/cancelled)", attachedTo);
					}
				}
			}
		}
	}

	/**
	 * Fire a timer that has elapsed. Called by ProcessTimerService.
	 *
	 * Dispatches by timerType:
	 * <ul>
	 *   <li>intermediateTimer: advance the waiting token past the catch event</li>
	 *   <li>boundaryTimer: spawn a parallel/replacement token via the boundary
	 *       event's outgoing flow; if cancelActivity, the parent activity's
	 *       token is consumed.</li>
	 *   <li>timerStart: deferred to a follow-up round</li>
	 * </ul>
	 */
	public void fireTimer(final NodeInterface callerTimer) throws FrameworkException {

		final App app            = StructrApp.getInstance();
		final ProcessTimer timer = elevate(callerTimer).as(ProcessTimer.class);

		if (!timer.isPending()) {

			return; // already fired/cancelled
		}

		final String timerType       = timer.getTimerType();
		final NodeInterface instance = timer.getInstance();
		final NodeInterface token    = timer.getToken();
		final NodeInterface element  = timer.getElement();

		// Enforce suspend: do not advance a non-running instance. For a suspended
		// instance the timer is left pending so it fires once the instance resumes.
		if (instance != null && !instance.as(ProcessInstance.class).isRunning()) {

			return;
		}

		try {

			// Claim the timer up-front: writing FIRED now acquires the database write
			// lock on the timer node at the START of this transaction. In a clustered
			// deployment two service instances can both read the timer as PENDING, but
			// only one can commit the claim -- the other's transaction conflicts on the
			// timer node and rolls back (RetryException), so the timer AND the token
			// advancement below (which shares this transaction) fire exactly once.
			// Single-node operation was already safe via the isPending guard; this
			// closes the multi-node window.
			timer.setStatus(ProcessTimerTraitDefinition.STATUS_FIRED);
			timer.setFiredAt(new Date());

			if (ProcessTimerTraitDefinition.TIMER_INTERMEDIATE.equals(timerType)) {

				fireIntermediateTimer(instance, token, element);

			} else if (ProcessTimerTraitDefinition.TIMER_BOUNDARY.equals(timerType)) {

				fireBoundaryTimer(instance, token, element, Boolean.TRUE.equals(timer.getCancelActivity()));

			} else {

				logger.warn("Timer type '{}' not yet supported -- timer {} marked as error", timerType, timer.getUuid());

				timer.setStatus(ProcessTimerTraitDefinition.STATUS_ERROR);
				timer.setErrorMessage("timerType not implemented: " + timerType);

				return;
			}

		} catch (Exception ex) {

			logger.error("Timer {} failed to fire: {}", timer.getUuid(), ex.getMessage(), ex);
			timer.setStatus(ProcessTimerTraitDefinition.STATUS_ERROR);
			timer.setErrorMessage(ex.getMessage());
		}
	}

	private void fireIntermediateTimer(final NodeInterface instance, final NodeInterface token, final NodeInterface element) throws FrameworkException {

		if (token == null || element == null) {

			logger.warn("Intermediate timer fire: missing token or element -- skipping");

			return;
		}

		// Reactivate the waiting token and advance past the catch event.
		token.as(ProcessToken.class).markActive();

		completeTokenAndMoveToNext(instance, token, element);
	}

	private void fireBoundaryTimer(final NodeInterface instance, final NodeInterface parentToken, final NodeInterface boundaryElement, final boolean cancelActivity) throws FrameworkException {

		if (boundaryElement == null) {

			logger.warn("Boundary timer fire: missing boundary element -- skipping");

			return;
		}

		// Find the boundary event's outgoing sequence flow.
		final Iterable<NodeInterface> outgoingFlows = boundaryElement.getProperty(boundaryElement.getTraits().key(BpmnElementTraitDefinition.OUTGOING_FLOWS_PROPERTY));
		NodeInterface targetElement = null;

		if (outgoingFlows != null) {

			for (final NodeInterface flow : outgoingFlows) {

				final String targetId = flow.getProperty(flow.getTraits().key(BpmnSequenceFlowTraitDefinition.TARGET_REF_ID_PROPERTY));
				if (targetId != null) {

					targetElement = findElementByBpmnId(instance, targetId);

					if (targetElement != null) {

						break;
					}
				}
			}
		}

		if (targetElement == null) {

			final String boundaryId = boundaryElement.getProperty(boundaryElement.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));

			logger.warn("Boundary timer on element '{}' has no resolvable outgoing flow -- timer fired but no token routed", boundaryId);

			return;
		}

		// For interrupting boundary events: consume the parent activity's token.
		if (cancelActivity && parentToken != null) {

			parentToken.as(ProcessToken.class).markCompleted();

			// Cancel any TaskInstance that was created for the parent userTask.
			cancelTaskInstanceFor(instance, parentToken);
		}

		// Spawn a fresh token at the boundary event's target element and advance.
		final NodeInterface newToken = createToken(instance, targetElement);

		advanceToken(instance, newToken);
	}

	/**
	 * If the parent token had an associated TaskInstance, mark it cancelled so
	 * the audit trail reflects the boundary-driven interruption.
	 */
	private void cancelTaskInstanceFor(final NodeInterface instance, final NodeInterface token) throws FrameworkException {

		if (instance == null || token == null) {

			return;
		}

		final NodeInterface element = token.as(ProcessToken.class).getAtElement();
		if (element == null) {

			return;
		}

		for (final TaskInstance task : instance.as(ProcessInstance.class).getTasks()) {

			if (task.isTerminal()) {

				continue;
			}

			final NodeInterface tElement = task.getDefinedBy();
			if (tElement != null && tElement.getUuid().equals(element.getUuid())) {

				task.setStatus(TaskInstanceTraitDefinition.STATUS_CANCELLED);
				task.setCancelledTime(new Date());

				fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_CANCELLED, task);
			}
		}
	}

	/**
	 * Look up a BpmnElement by its bpmnId within a process instance's definition.
	 * Must be scoped to the instance's definition: bpmnIds are NOT unique across
	 * BPMN versions, so a global lookup would return the first match in the index
	 * (typically v1) and route the engine into a stale, foreign-version element.
	 */
	private NodeInterface findElementByBpmnId(final NodeInterface instance, final String bpmnId) throws FrameworkException {

		if (instance == null || bpmnId == null) {

			return null;
		}

		final NodeInterface defNode = instance.getProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY));
		if (defNode == null) {

			return null;
		}

		return defNode.as(BpmnProcess.class).getElementByBpmnId(bpmnId);
	}

	/**
	 * Schedule any boundary timers attached to the given activity element.
	 * Called when a userTask is created -- iterates the definition's elements
	 * to find boundary events with timerEventDefinition that target this
	 * activity, and creates a ProcessTimer for each.
	 */
	void scheduleBoundaryTimers(final NodeInterface instance, final NodeInterface token, final NodeInterface activityElement) throws FrameworkException {

		if (instance == null || activityElement == null) {

			return;
		}

		final NodeInterface defNode = instance.getProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY));
		if (defNode == null) {

			return;
		}

		final String activityBpmnId = activityElement.getProperty(activityElement.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
		if (activityBpmnId == null) {

			return;
		}

		// Iterate the definition's elements, find boundaryEvents attached to this activity.
		final Iterable<NodeInterface> elements = defNode.getProperty(defNode.getTraits().key(BpmnProcessTraitDefinition.ELEMENTS_PROPERTY));
		if (elements == null) {

			return;
		}

		for (final NodeInterface el : elements) {

			final Traits elemTraits = el.getTraits();
			final String elType     = el.getProperty(elemTraits.key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY));

			if (!BpmnElementType.BOUNDARY_EVENT.matches(elType)) {

				continue;
			}

			final String attachedTo = getAttachedToBpmnId(el);
			if (attachedTo == null || !attachedTo.equals(activityBpmnId)) {

				continue;
			}

			final String timerType  = el.getProperty(elemTraits.key(BpmnElementTraitDefinition.TIMER_TYPE_PROPERTY));
			final String timerValue = el.getProperty(elemTraits.key(BpmnElementTraitDefinition.TIMER_VALUE_PROPERTY));

			if (timerType == null || timerValue == null) {

				continue; // non-timer boundary event
			}

			final Date fireAt = computeFireAt(timerType, timerValue);
			if (fireAt == null) {

				continue;
			}

			// Interrupting unless cancelActivity="false"
			final String cancelAttr = getAttributeValue(el, "cancelActivity");
			final boolean interrupting = !"false".equalsIgnoreCase(cancelAttr);

			createTimer(fireAt, ProcessTimerTraitDefinition.TIMER_BOUNDARY, timerValue, instance, token, el, interrupting);
		}
	}

	// -----------------------------------------------------------------------
	// Token management helpers
	// -----------------------------------------------------------------------

	private NodeInterface createToken(final NodeInterface instance, final NodeInterface element) throws FrameworkException {

		final App app                 = StructrApp.getInstance();
		final NodeInterface tokenNode = app.create(ProcessTraits.PROCESS_TOKEN);
		final ProcessToken token      = tokenNode.as(ProcessToken.class);

		token.markActive();
		token.setProcessInstance(instance);
		token.setAtElement(element);

		tokenNode.setProperty(token.getTraits().key("name"), tokenName(element));

		return tokenNode;
	}

	private void moveTokenToElement(final NodeInterface tokenNode, final NodeInterface element) throws FrameworkException {

		final ProcessToken token = tokenNode.as(ProcessToken.class);

		// Record the element (bpmnId) the token is leaving, so a parallel join can
		// tell which incoming edge each waiting token arrived on.
		final NodeInterface from = tokenNode.getProperty(token.getTraits().key(ProcessTokenTraitDefinition.AT_ELEMENT_PROPERTY));
		if (from != null) {

			tokenNode.setProperty(token.getTraits().key(ProcessTokenTraitDefinition.ARRIVED_FROM_BPMN_ID_PROPERTY),
				from.getProperty(from.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));
		}

		token.setAtElement(element);

		tokenNode.setProperty(token.getTraits().key("name"), tokenName(element));
	}

	private String tokenName(final NodeInterface element) throws FrameworkException {

		return "Token@" + element.as(BpmnElement.class).getBpmnId();
	}

	private void completeToken(final NodeInterface token) throws FrameworkException {

		token.as(ProcessToken.class).markCompleted();
	}

	/** True if the token currently sits on the given element. */
	private boolean isAtElement(final ProcessToken token, final NodeInterface element) {

		final NodeInterface at = token.getAtElement();

		return at != null && element != null && at.getUuid().equals(element.getUuid());
	}

	/**
	 * Complete the current token, find the single outgoing flow, move to its target, advance.
	 */
	private void completeTokenAndMoveToNext(final NodeInterface instance, final NodeInterface token, final NodeInterface element) throws FrameworkException {

		final BpmnElement current                  = element.as(BpmnElement.class);
		final List<BpmnSequenceFlow> outgoingFlows = Iterables.toList(current.getOutgoingFlows());

		if (outgoingFlows.isEmpty()) {

			// No outgoing flow -- check if this is a sub-process end event
			if (current.isType(BpmnElementType.END_EVENT)) {

				final BpmnElement parent = current.getParentElement();
				if (parent != null && isSubProcessLike(parent)) {

					// Sub-process end: complete this token, resume the parent
					completeToken(token);
					resumeSubProcessParent(instance, parent);

					return;
				}
			}

			// Truly terminal
			completeToken(token);
			checkProcessCompletion(instance);

			return;
		}

		if (outgoingFlows.size() == 1) {

			moveTokenToElement(token, outgoingFlows.get(0).getTargetElement());
			advanceToken(instance, token);

		} else {

			// Multiple outgoing flows on a non-gateway -- implicit exclusive gateway
			for (final BpmnSequenceFlow flow : outgoingFlows) {

				final String condition = flow.getConditionExpression();
				if (StringUtils.isBlank(condition) || evaluateCondition(condition, element, instance)) {

					moveTokenToElement(token, flow.getTargetElement());
					advanceToken(instance, token);

					return;
				}
			}

			throw new FrameworkException(422, "No outgoing path matched at element: " + current.getBpmnId());
		}
	}

	private void resumeSubProcessParent(final NodeInterface instance, final NodeInterface subProcessElement) throws FrameworkException {

		// A sub-process completes only when ALL of its internal tokens have been
		// consumed. If any branch is still live inside it -- e.g. a parallel fork
		// where one branch reached an internal end event first -- leave the parent
		// token waiting; resuming now would abandon the still-running branch and
		// advance the outer flow prematurely.
		if (hasActiveTokensInsideSubProcess(instance, subProcessElement)) {

			return;
		}

		final NodeInterface waitingToken = findWaitingToken(instance, subProcessElement);
		if (waitingToken != null) {

			waitingToken.as(ProcessToken.class).markActive();

			completeTokenAndMoveToNext(instance, waitingToken, subProcessElement);
		}
	}

	/**
	 * True if any non-completed token in the instance is currently sitting on an
	 * element nested (transitively) inside {@code subProcessElement}. The parent
	 * token itself waits <em>at</em> the sub-process element (not inside it), so it
	 * is correctly excluded; only tokens on the sub-process's own descendants count.
	 */
	private boolean hasActiveTokensInsideSubProcess(final NodeInterface instance, final NodeInterface subProcessElement) throws FrameworkException {

		for (final ProcessToken token : instance.as(ProcessInstance.class).getTokens()) {

			if (token.isCompleted()) {

				continue;
			}

			final NodeInterface at = token.getAtElement();
			if (at == null) {

				continue;
			}

			// Walk the parent-element chain upward; if we reach the sub-process,
			// this token is executing inside it.
			NodeInterface ancestor = at.getProperty(at.getTraits().key(BpmnElementTraitDefinition.PARENT_ELEMENT_PROPERTY));

			while (ancestor != null) {

				if (ancestor.getUuid().equals(subProcessElement.getUuid())) {

					return true;
				}

				ancestor = ancestor.getProperty(ancestor.getTraits().key(BpmnElementTraitDefinition.PARENT_ELEMENT_PROPERTY));
			}
		}

		return false;
	}

	// -----------------------------------------------------------------------
	// Process completion
	// -----------------------------------------------------------------------
	private void checkProcessCompletion(final NodeInterface instanceNode) throws FrameworkException {

		final ProcessInstance instance = instanceNode.as(ProcessInstance.class);

		for (final ProcessToken token : instance.getTokens()) {

			if (token.isActive() || token.isWaiting()) {

				return; // Still active tokens
			}
		}

		// All tokens completed
		instance.setStatus(ProcessInstanceTraitDefinition.STATUS_COMPLETED);
		instance.setEndTime(new Date());

		logger.info("Process instance {} completed", instanceNode.getUuid());
	}

	private NodeInterface findWaitingToken(final NodeInterface instance, final NodeInterface element) throws FrameworkException {

		for (final ProcessToken token : instance.as(ProcessInstance.class).getTokens()) {

			if (token.isWaiting() && isAtElement(token, element)) {

				return token;
			}
		}

		return null;
	}

	/**
	 * True when, for every incoming sequence flow of {@code element}, at least one
	 * non-completed token is waiting at {@code element} that arrived from that
	 * flow's source (tracked via {@code arrivedFromBpmnId}). This enforces the
	 * parallel-join semantics of one token per distinct incoming edge, rather than
	 * a plain total count that two tokens from the same edge could satisfy.
	 */
	private boolean allIncomingEdgesDelivered(final NodeInterface instance, final NodeInterface element, final List<BpmnSequenceFlow> incomingFlows) throws FrameworkException {

		final Set<String> arrivedFrom = new HashSet<>();
		int waiting                   = 0;
		boolean unknownArrival        = false;

		for (final ProcessToken t : instance.as(ProcessInstance.class).getTokens()) {

			if (!t.isCompleted() && isAtElement(t, element)) {

				waiting++;

				final String from = t.getProperty(t.getTraits().key(ProcessTokenTraitDefinition.ARRIVED_FROM_BPMN_ID_PROPERTY));
				if (from != null) {

					arrivedFrom.add(from);

				} else {

					unknownArrival = true;
				}
			}
		}

		// Fallback: a token created directly at the join (e.g. a fork whose flow
		// targets the join) has no recorded arrival edge -- use the plain count so
		// such topologies never deadlock.
		if (unknownArrival) {

			return waiting >= incomingFlows.size();
		}

		for (final BpmnSequenceFlow f : incomingFlows) {

			final String src = f.getSourceRefId();
			if (src == null || !arrivedFrom.contains(src)) {

				return false;
			}
		}

		return true;
	}

	/**
	 * True if any non-completed token (other than one already at {@code gateway})
	 * can still reach {@code gateway} by following outgoing sequence flows. Used by
	 * the inclusive-gateway join to decide whether more tokens may yet arrive: an
	 * unrelated parallel branch that cannot flow into the join does not block it.
	 */
	private boolean anyActiveTokenCanReach(final NodeInterface instance, final NodeInterface gateway) throws FrameworkException {

		for (final ProcessToken token : instance.as(ProcessInstance.class).getTokens()) {

			if (token.isCompleted() || isAtElement(token, gateway)) {

				continue;
			}

			final NodeInterface at = token.getAtElement();
			if (at != null && canReachElement(at, gateway, new HashSet<>())) {

				return true;
			}
		}

		return false;
	}

	/**
	 * Depth-first search along outgoing sequence flows: can {@code from} reach
	 * {@code target}? The {@code visited} set (by UUID) makes this safe on cyclic
	 * (looping) process graphs.
	 */
	private boolean canReachElement(final NodeInterface from, final NodeInterface target, final Set<String> visited) throws FrameworkException {

		if (from == null || !visited.add(from.getUuid())) {

			return false;
		}

		if (from.getUuid().equals(target.getUuid())) {

			return true;
		}

		for (final BpmnSequenceFlow out : from.as(BpmnElement.class).getOutgoingFlows()) {

			final NodeInterface next = out.getTargetElement();
			if (next != null && canReachElement(next, target, visited)) {

				return true;
			}
		}

		return false;
	}

	private void consumeAllTokensAtElement(final NodeInterface instance, final NodeInterface element) throws FrameworkException {

		for (final ProcessToken token : instance.as(ProcessInstance.class).getTokens()) {

			if (!token.isCompleted() && isAtElement(token, element)) {

				token.markCompleted();
			}
		}
	}

	// -----------------------------------------------------------------------
	// Process parameter management
	// -----------------------------------------------------------------------

	/**
	 * Store the values submitted with a task completion (or intermediate-event signal).
	 *
	 * <p><b>Auto-routing.</b> A submitted field whose name matches a property on
	 * {@code instance.subject}'s type is written to the subject (engine-elevated:
	 * task completion is the sanctioned write path while a task is active, and the
	 * participant may not have direct write on the field). Anything else is stored
	 * as a {@code ProcessParameterValue} -- preserving the audit trail and isolating
	 * process metadata from domain data.</p>
	 *
	 * <p>This is the "single subject" principle in action (see Process Engine docs):
	 * domain data lives on the subject, process metadata lives on PVs, the form is
	 * the contract that decides which is which. No separate parameter declarations
	 * are required.</p>
	 *
	 * <p>When no subject is attached to the instance, every submitted field becomes
	 * a PV (current behaviour for processes without a domain object).</p>
	 */
	private void storeParameterValues(final NodeInterface instance, final NodeInterface element, final Map<String, Object> parameters) throws FrameworkException {

		final App app               = StructrApp.getInstance();
		final Traits pvTraits       = Traits.of(ProcessTraits.PROCESS_PARAMETER_VALUE);
		final Traits instTraits     = instance.getTraits();
		final Date now              = new Date();
		final NodeInterface subject = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.SUBJECT_PROPERTY));
		final Traits subjectTraits  = subject != null ? subject.getTraits() : null;

		// Split submitted fields by name-match against the subject's schema.
		final Map<String, Object> subjectFields   = new LinkedHashMap<>();
		final Map<String, Object> parameterFields = new LinkedHashMap<>();

		for (final Map.Entry<String, Object> entry : parameters.entrySet()) {

			final String name = entry.getKey();

			// Reserved fields never go to the subject -- "id" and "type" are
			// framework-managed and would corrupt subject identity / typing.
			if ("id".equals(name) || "type".equals(name)) {

				parameterFields.put(name, entry.getValue());
				continue;
			}

			if (subjectTraits != null && subjectTraits.hasKey(name)) {

				subjectFields.put(name, entry.getValue());

			} else {

				parameterFields.put(name, entry.getValue());
			}
		}

		// Subject fields: convert form-string inputs to typed values via the
		// schema's property converters, then write under engine privileges.
		if (subject != null && !subjectFields.isEmpty()) {

			final NodeInterface elevatedSubject = elevate(subject);
			final PropertyMap typed             = PropertyMap.inputTypeToJavaType(SecurityContext.getSuperUserInstance(), subject.getType(), subjectFields);

			elevatedSubject.setProperties(SecurityContext.getSuperUserInstance(), typed);
		}

		// Parameter fields: store as ProcessParameterValue. Each PV is self-describing
		// (carries its own parameterName / parameterType / stringValue) so there's no
		// separate "parameter definition" node to look up.
		for (final Map.Entry<String, Object> entry : parameterFields.entrySet()) {

			final String paramName  = entry.getKey();
			final Object paramValue = entry.getValue();
			final NodeInterface pvNode = app.create(ProcessTraits.PROCESS_PARAMETER_VALUE);

			pvNode.setProperty(pvTraits.key(ProcessParameterValueTraitDefinition.PARAMETER_NAME_PROPERTY), paramName);
			pvNode.setProperty(pvTraits.key(ProcessParameterValueTraitDefinition.PARAMETER_TYPE_PROPERTY), inferParameterType(paramValue));
			pvNode.setProperty(pvTraits.key(ProcessParameterValueTraitDefinition.STRING_VALUE_PROPERTY), stringValueOf(paramValue));
			pvNode.setProperty(pvTraits.key(ProcessParameterValueTraitDefinition.SET_AT_PROPERTY), now);
			pvNode.setProperty(pvTraits.key(ProcessParameterValueTraitDefinition.PROCESS_INSTANCE_PROPERTY), instance);
			pvNode.setProperty(pvTraits.key(ProcessParameterValueTraitDefinition.SET_BY_ELEMENT_PROPERTY), element);
			pvNode.setProperty(pvTraits.key("name"), paramName);
		}
	}

	/**
	 * Infer a {@code parameterType} value from the Java type of the submitted
	 * value. For form posts the value is almost always a String, in which case
	 * {@code parameterType} stays unset (the default load path returns the
	 * string unchanged). The non-String branches are forward-compat for
	 * programmatic invocations that pass typed values.
	 */
	/** Largest magnitude at which a double still represents every integer exactly (2^53). */
	private static final double MAX_EXACT_INTEGER_DOUBLE = 9007199254740992d;

	/**
	 * The string form to persist for a parameter value. A whole-valued Double / Float is
	 * written without its fractional part: JavaScript has no integer type, so a service task
	 * doing {@code $.process.amount = 25000} hands the engine a Double, and a plain
	 * {@code toString()} persists "25000.0" -- which is then what REST clients, the UI and any
	 * string comparison see. Values with an actual fractional part, and magnitudes beyond the
	 * range where a double represents integers exactly, are left alone. Read-back is
	 * unaffected: the value is parsed by parameterType, and "25000" parses as a Double just as
	 * well.
	 */
	static String stringValueOf(final Object paramValue) {

		if (paramValue == null) {

			return null;
		}

		if (paramValue instanceof Double || paramValue instanceof Float) {

			final double value = ((Number) paramValue).doubleValue();
			if (Double.isFinite(value) && value == Math.rint(value) && Math.abs(value) <= MAX_EXACT_INTEGER_DOUBLE) {

				return Long.toString((long) value);
			}
		}

		return paramValue.toString();
	}

	static String inferParameterType(final Object paramValue) {

		if (paramValue instanceof Boolean) {

			return ProcessParameterValueTraitDefinition.TYPE_BOOLEAN;
		}

		if (paramValue instanceof Integer || paramValue instanceof Long) {

			return ProcessParameterValueTraitDefinition.TYPE_INTEGER;
		}

		if (paramValue instanceof Double  || paramValue instanceof Float) {

			return ProcessParameterValueTraitDefinition.TYPE_DOUBLE;
		}

		if (paramValue instanceof Date) {

			return ProcessParameterValueTraitDefinition.TYPE_DATE;
		}

		return null;
	}

	/**
	 * Load the current (most recent) parameter values for a process instance
	 * into a map suitable for condition evaluation and listener arg-enrichment.
	 * Each PV carries its own name; there is no separate definition node.
	 */
	private Map<String, Object> loadParameterValues(final NodeInterface instance) throws FrameworkException {

		// Collect all values, keeping only the most recent per parameter name
		// (by comparing setAt timestamps)
		final Map<String, Object> values = new LinkedHashMap<>();
		final Map<String, Date> timestamps = new LinkedHashMap<>();

		for (final ProcessParameterValue pv : instance.as(ProcessInstance.class).getParameterValues()) {

			final String paramName = pv.getParameterName();
			if (paramName == null) {

				continue;
			}

			final Date setAt    = pv.getSetAt();
			final Date existing = timestamps.get(paramName);

			if (existing == null || (setAt != null && setAt.after(existing))) {

				values.put(paramName, convertParameterValue(pv.getStringValue(), pv.getParameterType()));
				timestamps.put(paramName, setAt);
			}
		}

		return values;
	}

	/**
	 * Convert a stored string value to the appropriate Java type. Falls back to
	 * the raw String when no type was declared (the common case for form-driven
	 * processes) or when parsing fails.
	 */
	static Object convertParameterValue(final String stringValue, final String paramType) {

		if (stringValue == null) {

			return null;
		}

		if (paramType == null || ProcessParameterValueTraitDefinition.TYPE_STRING.equals(paramType)) {

			return stringValue;
		}

		try {

			return switch (paramType) {

				case ProcessParameterValueTraitDefinition.TYPE_BOOLEAN -> Boolean.parseBoolean(stringValue);
				case ProcessParameterValueTraitDefinition.TYPE_INTEGER -> Integer.parseInt(stringValue);
				case ProcessParameterValueTraitDefinition.TYPE_DOUBLE  -> Double.parseDouble(stringValue);
				default -> stringValue;
			};

		} catch (NumberFormatException e) {

			return stringValue;
		}
	}

	// -----------------------------------------------------------------------
	// Condition evaluation
	// -----------------------------------------------------------------------

	/**
	 * Evaluate a condition expression. BPMN condition expressions use JUEL/JavaScript
	 * syntax (e.g. ${approved == true}), so they are always evaluated as JavaScript.
	 * Process parameter values are loaded from the graph and injected as JavaScript
	 * variables before evaluation.
	 */
	private boolean evaluateCondition(final String condition, final NodeInterface contextElement, final NodeInterface instance) {

		try {

			final ActionContext ctx = new ActionContext(SecurityContext.getSuperUserInstance());
			final String expression;

			installProcessContext(ctx, instance, contextElement);

			if (condition.startsWith("${") && condition.endsWith("}") && !condition.startsWith("${{")) {

				// BPMN-style ${...} expression -- evaluate as JavaScript
				// because BPMN uses JUEL syntax (==, !=, &&, ||) which is
				// compatible with JS but not with StructrScript.
				// Process parameters are available via $.process.<n>.
				final String inner = condition.substring(2, condition.length() - 1).trim();

				// Rewrite bare variable references to $.process.<n>
				final String rewritten = rewriteConditionExpression(inner, instance);
				expression = "${js{" + rewritten + "}}";

			} else if (condition.startsWith("${")) {

				// Already a Structr expression (e.g. ${{...}} for JS)
				expression = condition;

			} else {

				// Bare expression -- wrap as JavaScript
				final String rewritten = rewriteConditionExpression(condition, instance);
				expression = "${js{" + rewritten + "}}";
			}

			final Object result = Scripting.evaluate(ctx, contextElement, expression, "conditionExpression");
			if (result instanceof Boolean) {

				return (Boolean) result;

			} else if (result instanceof String) {

				return "true".equalsIgnoreCase((String) result);

			} else {

				return result != null;
			}

		} catch (Exception ex) {

			logger.warn("Error evaluating condition '{}': {}", condition, ex.getMessage());

			return false;
		}
	}

	// -----------------------------------------------------------------------
	// Process context helpers
	// -----------------------------------------------------------------------

	/**
	 * Create and install a ProcessContext on the ActionContext so that scripts
	 * can access process data via $.process.<name>, $.process.instance, etc.
	 *
	 * <p>The variable map exposed at $.process.<name> is the merged view: subject
	 * properties (when a subject is attached to the instance) plus parameter
	 * values, with PVs overriding subject on name collision. This keeps Camunda-
	 * style gateway expressions like {@code ${days <= 2}} working regardless of
	 * which side of the storage split the data lives on -- the BPMN author
	 * doesn't need to know whether {@code days} is a domain field or a PV.</p>
	 */
	/**
	 * Run a SchemaMethod's body against the ActionContext prepared by the caller, so the
	 * constants installed on it -- {@code $.process} above all -- are visible to the script.
	 *
	 * <p>{@code ScriptMethod#execute} cannot be used for this: it forwards only
	 * {@code actionContext.getSecurityContext()} to {@code Actions#execute}, which builds a
	 * FRESH ActionContext. Everything installed on the context the engine just prepared is
	 * silently dropped, so a listener body doing {@code $.process.x = y} saw {@code $.process}
	 * as null and died with "Cannot set property 'x' of null". The automatic-task, io-mapping
	 * and condition paths never hit this because they already call
	 * {@code Scripting.evaluate(ctx, ...)} directly, which is what this does.</p>
	 *
	 * <p>{@code wrapJsInMain} is taken from the method itself, as ScriptMethod does, so
	 * user-authored handlers keep their configured wrapping.</p>
	 */
	private Object runMethodWithContext(final ActionContext ctx, final GraphObject entity, final NodeInterface method, final String label) throws FrameworkException {

		final SchemaMethod schemaMethod = method.as(SchemaMethod.class);
		final String source             = schemaMethod.getSource();

		if (StringUtils.isBlank(source)) {

			return null; // handler declared but no body yet
		}

		final ScriptConfig scriptConfig = ScriptConfig.builder()
			.wrapJsInMain(schemaMethod.wrapJsInMain())
			.build();

		return Scripting.evaluate(ctx, entity, "${" + source.trim() + "}", label, method.getUuid(), scriptConfig);
	}

	private void installProcessContext(final ActionContext ctx, final NodeInterface instance, final NodeInterface element) throws FrameworkException {

		installProcessContext(ctx, instance, element, null);
	}

	private void installProcessContext(final ActionContext ctx, final NodeInterface instance, final NodeInterface element, final Map<String, Object> localScope) throws FrameworkException {

		final Traits instTraits             = instance.getTraits();
		final NodeInterface definition      = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY));
		final Map<String, Object> variables = loadProcessVariables(instance);

		// Write sink: $.process.x = y (and transpiled execution.setVariable(...)) persist
		// through the same auto-routing storeParameterValues used by task/start params.
		// Ignored while a localScope is active -- there writes stay local.
		final ProcessContext.VariableSink sink = (name, value) -> {
			final Map<String, Object> one = new HashMap<>();
			one.put(name, value);
			storeParameterValues(instance, element, one);
		};

		ctx.setConstant("process", new ProcessContext(instance, element, definition, variables, sink, localScope));
	}

	/**
	 * Load the merged variable view for an instance: subject properties plus
	 * parameter values. PVs win on name collision (they're the explicit
	 * process-state observations; subject is the default fallback for domain
	 * data routed there by the auto-detect).
	 */
	private Map<String, Object> loadProcessVariables(final NodeInterface instance) throws FrameworkException {

		final Map<String, Object> merged = new LinkedHashMap<>();
		final NodeInterface subject      = instance.getProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.SUBJECT_PROPERTY));

		if (subject != null) {

			final Traits subjectTraits = subject.getTraits();

			for (final PropertyKey<?> key : subjectTraits.getAllPropertyKeys()) {

				final String name = key.jsonName();

				// Skip framework-managed identity / type / system fields. Domain
				// fields keep their natural names.
				if ("id".equals(name) || "type".equals(name) || "internalEntityContextPath".equals(name)) {

					continue;
				}

				try {

					final Object value = subject.getProperty(key);
					if (value != null) {

						merged.put(name, value);
					}

				} catch (Exception ignore) {

					// Best-effort: a property reader that throws shouldn't break gateway eval.
				}
			}
		}

		// PVs override subject on name collision.
		merged.putAll(loadParameterValues(instance));

		return merged;
	}

	/**
	 * Rewrite bare variable references in BPMN condition expressions to
	 * $.process.<name> references. For example:
	 *   approved == true       -> $.process.approved == true
	 *   claimAmount < 1000     -> $.process.claimAmount < 1000
	 *   delivery == 'express'  -> $.process.delivery == 'express'
	 *
	 * The set of rewritable names spans both subject properties and PVs (i.e.
	 * everything reachable via $.process at runtime). JS keywords, literals,
	 * and operators are left unchanged.
	 */
	private String rewriteConditionExpression(final String expression, final NodeInterface instance) throws FrameworkException {

		return rewriteConditionExpression(expression, loadProcessVariables(instance).keySet());
	}

	/**
	 * Pure rewrite step: given the set of known process-variable names, rewrite
	 * their bare occurrences in the expression to {@code $.process.<name>}.
	 * Package-private and static so the JUEL-to-JavaScript rewriting can be
	 * unit-tested without a process instance.
	 */
	static String rewriteConditionExpression(final String expression, final Set<String> variableNames) {

		if (expression == null || variableNames == null || variableNames.isEmpty()) {

			return expression;
		}

		String result = expression;

		for (final String name : variableNames) {

			// Replace word-boundary occurrences of the variable name with $.process.<name>
			// Negative lookbehind for '.' prevents double-rewriting.
			result = result.replaceAll("(?<!\\.)\\b" + name + "\\b", "\\$.process." + name);
		}

		return result;
	}

	// -----------------------------------------------------------------------
	// Utility helpers
	// -----------------------------------------------------------------------

	private String getBpmnId(final NodeInterface element) throws FrameworkException {

		return element.getProperty(element.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
	}

	/**
	 * The element(s) this instance is currently at: the {@code atElement} of each of
	 * its non-completed (waiting/active) tokens. Normally one, but a parallel split
	 * can put an instance at several steps at once. Empty once the instance has
	 * finished. De-duplicated by element, preserving first-seen order.
	 *
	 * <p>Returns the {@code BpmnElement} nodes themselves so callers stay flexible --
	 * read {@code bpmnName}, {@code bpmnElementType}, {@code bpmnId}, etc.</p>
	 */
	public static List<NodeInterface> currentStepElements(final NodeInterface instance) throws FrameworkException {

		final Traits tokTraits = Traits.of(ProcessTraits.PROCESS_TOKEN);
		final PropertyKey<Iterable<NodeInterface>> tokensKey = instance.getTraits().key(ProcessInstanceTraitDefinition.TOKENS_PROPERTY);
		final PropertyKey<String> statusKey                  = tokTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY);
		final PropertyKey<NodeInterface> atElementKey        = tokTraits.key(ProcessTokenTraitDefinition.AT_ELEMENT_PROPERTY);
		final List<NodeInterface> steps      = new LinkedList<>();
		final Set<String> seen               = new LinkedHashSet<>();
		final Iterable<NodeInterface> tokens = instance.getProperty(tokensKey);

		if (tokens != null) {

			for (final NodeInterface token : tokens) {

				if (ProcessTokenTraitDefinition.STATUS_COMPLETED.equals(token.getProperty(statusKey))) {

					continue;
				}

				final NodeInterface at = token.getProperty(atElementKey);
				if (at != null && seen.add(at.getUuid())) {

					steps.add(at);
				}
			}
		}

		return steps;
	}

	/**
	 * Count the non-completed (waiting or active) tokens currently sitting at each
	 * element, aggregated across every instance of {@code process}. Returns a map of
	 * element {@code bpmnId -> count}; elements with no live token are absent.
	 *
	 * <p>Powers the editor's live instance-count overlay (Camunda-Cockpit-style
	 * activity badges). Callers should pass a super-user {@code app} so the count
	 * reflects all instances, not just those the current user may read.</p>
	 */
	public static Map<String, Integer> computeLiveTokenCounts(final App app, final NodeInterface process) throws FrameworkException {

		return computeTokenCountsByElement(app, process, false);
	}

	/**
	 * Count the completed tokens that passed through each element, aggregated across
	 * every instance of {@code process}. Returns a map of element {@code bpmnId ->
	 * count}; elements no token has finished at are absent.
	 *
	 * <p>Companion to {@link #computeLiveTokenCounts}: powers the editor's "finished"
	 * badge (Camunda-Cockpit-style historic activity-instance count), shown alongside
	 * the live/active badge. Callers should pass a super-user {@code app} so the count
	 * reflects all instances, not just those the current user may read.</p>
	 */
	public static Map<String, Integer> computeCompletedTokenCounts(final App app, final NodeInterface process) throws FrameworkException {

		return computeTokenCountsByElement(app, process, true);
	}

	/**
	 * Shared implementation for the live / completed per-element token counts.
	 * When {@code completed} is false, counts tokens still sitting at an element
	 * (status != completed); when true, counts tokens that have completed there.
	 */
	private static Map<String, Integer> computeTokenCountsByElement(final App app, final NodeInterface process, final boolean completed) throws FrameworkException {

		final Traits piTraits  = Traits.of(ProcessTraits.PROCESS_INSTANCE);
		final Traits tokTraits = Traits.of(ProcessTraits.PROCESS_TOKEN);
		final Traits elTraits  = Traits.of(ProcessTraits.BPMN_ELEMENT);
		final PropertyKey<NodeInterface> processKey          = piTraits.key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY);
		final PropertyKey<Iterable<NodeInterface>> tokensKey = piTraits.key(ProcessInstanceTraitDefinition.TOKENS_PROPERTY);
		final PropertyKey<String> tokenStatusKey             = tokTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY);
		final PropertyKey<NodeInterface> atElementKey        = tokTraits.key(ProcessTokenTraitDefinition.AT_ELEMENT_PROPERTY);
		final PropertyKey<String> bpmnIdKey                  = elTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY);
		final Map<String, Integer> counts = new LinkedHashMap<>();

		for (final NodeInterface instance : app.nodeQuery(ProcessTraits.PROCESS_INSTANCE).key(processKey, process).getResultStream()) {

			final Iterable<NodeInterface> tokens = instance.getProperty(tokensKey);
			if (tokens == null) {

				continue;
			}

			for (final NodeInterface token : tokens) {

				final boolean isCompleted = ProcessTokenTraitDefinition.STATUS_COMPLETED.equals(token.getProperty(tokenStatusKey));
				// live overlay: only tokens still sitting somewhere; completed overlay: only finished tokens
				if (completed != isCompleted) {

					continue;
				}

				final NodeInterface at = token.getProperty(atElementKey);
				if (at == null) {

					continue;
				}

				final String bpmnId = at.getProperty(bpmnIdKey);
				if (bpmnId != null) {

					counts.merge(bpmnId, 1, Integer::sum);
				}
			}
		}

		return counts;
	}

	/**
	 * Get an attribute value from an element's bpmnAttributes JSON map.
	 * Delegates to {@link #getJsonAttributeValue(String, String)} for the actual
	 * parsing; this wrapper only reads the property off the node.
	 */
	private String getAttributeValue(final NodeInterface element, final String attrName) {

		try {

			final String json = element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.BPMN_ATTRIBUTES_PROPERTY));

			return getJsonAttributeValue(json, attrName);

		} catch (Exception ex) {

			logger.warn("Error reading attribute '{}': {}", attrName, ex.getMessage());

			return null;
		}
	}

	/**
	 * Extract a single named attribute from a {@code bpmnAttributes} JSON object
	 * string. The importer builds this JSON with Gson from a
	 * {@code Map<String,String>}, so every value is a string; this returns that
	 * string, or {@code null} when the JSON is {@code null} / blank / not a JSON
	 * object, the key is absent, or the value is JSON {@code null}.
	 *
	 * <p>Uses a real JSON parser rather than string scanning so that values
	 * containing quotes, colons or braces, keys that are substrings of other
	 * keys, and keys whose name also occurs inside a value, are all handled
	 * correctly (the previous {@code indexOf}-based extractor got these wrong).</p>
	 *
	 * <p>Package-private and static so it can be unit-tested in isolation.</p>
	 */
	static String getJsonAttributeValue(final String json, final String attrName) {

		if (json == null || attrName == null) {

			return null;
		}

		final String trimmed = json.trim();
		if (trimmed.isEmpty()) {

			return null;
		}

		final com.google.gson.JsonElement root;

		try {

			root = com.google.gson.JsonParser.parseString(trimmed);

		} catch (final com.google.gson.JsonSyntaxException ex) {

			return null;
		}

		if (root == null || !root.isJsonObject()) {

			return null;
		}

		final com.google.gson.JsonObject obj = root.getAsJsonObject();
		final com.google.gson.JsonElement value = obj.get(attrName);

		if (value == null || value.isJsonNull()) {

			return null;
		}

		// String values (the only kind the importer emits) and other primitives
		// return their string form; nested objects/arrays return compact JSON.

		return value.isJsonPrimitive() ? value.getAsString() : value.toString();
	}

	/**
	 * Resolve a boundary event's host activity bpmnId. Prefers the typed
	 * {@code attachedTo} relationship populated by the importer, falling
	 * back to the legacy {@code attachedToRef} attribute in bpmnAttributes
	 * for diagrams imported before the relationship existed. Returns
	 * {@code null} when neither is set.
	 */
	private String getAttachedToBpmnId(final NodeInterface element) {

		try {

			final NodeInterface host = element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.ATTACHED_TO_PROPERTY));
			if (host != null) {

				final String hostBpmnId = host.getProperty(host.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
				if (StringUtils.isNotBlank(hostBpmnId)) {

					return hostBpmnId;
				}
			}

		} catch (Exception ex) {

			logger.warn("Error reading attachedTo relationship: {}", ex.getMessage());
		}

		// Legacy fallback: attachedToRef in bpmnAttributes JSON.

		return getAttributeValue(element, "attachedToRef");
	}
}
