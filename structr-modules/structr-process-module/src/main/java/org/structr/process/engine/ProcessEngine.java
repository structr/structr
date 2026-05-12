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
package org.structr.process.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.core.api.Arguments;
import org.structr.core.api.NamedArguments;
import org.structr.core.api.ScriptMethod;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.NodeInterface;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.process.traits.definitions.*;
import org.structr.schema.action.ActionContext;
import org.structr.process.ProcessTraits;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;

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

	private final SecurityContext securityContext;
	private final Principal caller;

	/**
	 * Construct an engine for the given caller's security context. The engine
	 * internally runs as superuser so that newly created ProcessInstance /
	 * ProcessToken / TaskInstance / ProcessParameterValue nodes have no owner
	 * (engine-managed). The caller principal is remembered so the engine can
	 * grant access where appropriate (e.g. read on the ProcessInstance to the
	 * user who started it).
	 */
	public ProcessEngine(final SecurityContext callerContext) {
		final Principal callerPrincipal = (callerContext != null) ? callerContext.getUser(false) : null;
		// SuperUser is a singleton with no backing graph node and cannot be a grant endpoint.
		this.caller = (callerPrincipal instanceof SuperUser) ? null : callerPrincipal;
		this.securityContext = SecurityContext.getSuperUserInstance();
	}

	/**
	 * Re-fetch a node through the engine's superuser-bound App so that all
	 * subsequent property reads on it use the elevated security context.
	 *
	 * Engine entry points are invoked with {@link NodeInterface} arguments that
	 * carry the caller's (user) security context. Property reads through such
	 * wrappers (especially collection rels like {@code TOKENS_PROPERTY}) filter
	 * by user permissions, which would hide engine-managed nodes (tokens,
	 * unowned tasks, parameter values). Re-fetching via the superuser App
	 * guarantees the engine sees the complete graph.
	 */
	private NodeInterface elevate(final App app, final NodeInterface node) throws FrameworkException {
		if (node == null) {
			return null;
		}
		return app.getNodeById(node.getUuid());
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

		final App app = StructrApp.getInstance(securityContext);
		// Re-fetch under the engine's superuser context so internal traversals
		// don't get filtered by the caller's permissions.
		final NodeInterface procNode = elevate(app, callerProcNode);
		final Traits procTraits = procNode.getTraits();

		// Find the start event (top-level element with bpmnElementType == "startEvent")
		final NodeInterface startEvent = findStartEvent(procNode, procTraits);
		if (startEvent == null) {
			throw new FrameworkException(422, "No startEvent found in process definition");
		}

		// Create ProcessInstance
		final NodeInterface instance = app.create(ProcessTraits.PROCESS_INSTANCE, (String) null);
		final Traits instTraits = instance.getTraits();

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
			instance.setProperty(instTraits.key(ProcessInstanceTraitDefinition.SUBJECT_PROPERTY), elevate(app, subject));
		}

		// Grant the initiator read access on the ProcessInstance so they can observe their own instance.
		grant(instance, caller, Permission.read);

		// Fire 'created' before token placement: listeners observe an instance with a definition,
		// initiator, and possibly subject set, but no tokens yet.
		fireProcessEvent(BpmnProcessListenerTraitDefinition.EVENT_CREATED, instance);

		// Create token at start event
		final NodeInterface token = createToken(app, instance, startEvent);

		// Advance the token from the start event (start events are pass-through)
		advanceToken(app, instance, token);

		// Fire 'started' after the initial token has been placed and advanced. By this point
		// the engine has reached the first wait state (a userTask, an intermediate catch event,
		// or the end of the process if it advanced through to completion synchronously).
		fireProcessEvent(BpmnProcessListenerTraitDefinition.EVENT_STARTED, instance);

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

		final App app = StructrApp.getInstance(securityContext);
		final NodeInterface instance = elevate(app, callerInstanceNode);
		final Traits instTraits = instance.getTraits();

		final String currentStatus = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.STATUS_PROPERTY));
		if (ProcessInstanceTraitDefinition.STATUS_COMPLETED.equals(currentStatus)) {
			throw new FrameworkException(422, "Cannot terminate a completed instance");
		}
		if (ProcessInstanceTraitDefinition.STATUS_TERMINATED.equals(currentStatus)) {
			throw new FrameworkException(422, "Instance is already terminated");
		}

		// Mark all waiting / active tokens as completed so the engine treats them as inert.
		// The instance is left in a terminal state; downstream handlers can clean up via
		// onProcessTerminated.
		final Iterable<NodeInterface> tokens = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.TOKENS_PROPERTY));
		if (tokens != null) {
			for (final NodeInterface t : tokens) {
				final Traits tokenTraits = t.getTraits();
				final String tStatus = t.getProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY));
				if (ProcessTokenTraitDefinition.STATUS_ACTIVE.equals(tStatus) || ProcessTokenTraitDefinition.STATUS_WAITING.equals(tStatus)) {
					t.setProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY), ProcessTokenTraitDefinition.STATUS_COMPLETED);
				}
			}
		}

		instance.setProperty(instTraits.key(ProcessInstanceTraitDefinition.STATUS_PROPERTY), ProcessInstanceTraitDefinition.STATUS_TERMINATED);
		instance.setProperty(instTraits.key(ProcessInstanceTraitDefinition.END_TIME_PROPERTY), new Date());

		// 'terminated' event fires via OnModification on the status change.
		logger.info("Process instance {} terminated", instance.getUuid());
	}

	/**
	 * Suspend a process instance. Sets status=suspended; existing tokens are left in place,
	 * no new advancement until the instance is resumed. Fires the {@code suspended} lifecycle
	 * event (via the OnModification hook).
	 */
	public void suspendProcess(final NodeInterface callerInstanceNode) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
		final NodeInterface instance = elevate(app, callerInstanceNode);
		final Traits instTraits = instance.getTraits();

		final String currentStatus = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.STATUS_PROPERTY));
		if (!ProcessInstanceTraitDefinition.STATUS_RUNNING.equals(currentStatus)) {
			throw new FrameworkException(422, "Can only suspend a running instance (current status: " + currentStatus + ")");
		}

		instance.setProperty(instTraits.key(ProcessInstanceTraitDefinition.STATUS_PROPERTY), ProcessInstanceTraitDefinition.STATUS_SUSPENDED);

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

		final App app = StructrApp.getInstance(securityContext);
		final NodeInterface instance = elevate(app, callerInstanceNode);
		final Traits instTraits = instance.getTraits();

		final String currentStatus = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.STATUS_PROPERTY));
		if (!ProcessInstanceTraitDefinition.STATUS_SUSPENDED.equals(currentStatus)) {
			throw new FrameworkException(422, "Can only resume a suspended instance (current status: " + currentStatus + ")");
		}

		instance.setProperty(instTraits.key(ProcessInstanceTraitDefinition.STATUS_PROPERTY), ProcessInstanceTraitDefinition.STATUS_RUNNING);

		// Explicit fire: OnModification skips 'running' transitions (cannot disambiguate
		// initial start from resume), so 'resumed' is emitted here.
		fireProcessEvent(BpmnProcessListenerTraitDefinition.EVENT_RESUMED, instance);

		logger.info("Process instance {} resumed", instance.getUuid());
	}

	/**
	 * Complete a user task and advance the process.
	 *
	 * @param parameters optional key-value map of process parameter values set by this task
	 */
	public void completeTask(final NodeInterface callerTaskNode, final Map<String, Object> parameters) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
		// Re-fetch under superuser context: traversals to processInstance,
		// tokens, and parameter values must not be filtered by caller perms.
		final NodeInterface taskNode = elevate(app, callerTaskNode);
		final Traits taskTraits = taskNode.getTraits();

		final String status = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY));
		if (TaskInstanceTraitDefinition.STATUS_COMPLETED.equals(status)) {
			throw new FrameworkException(422, "Task is already completed");
		}
		if (TaskInstanceTraitDefinition.STATUS_CANCELLED.equals(status)) {
			throw new FrameworkException(422, "Task is cancelled");
		}

		// Mark task as completed
		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY), TaskInstanceTraitDefinition.STATUS_COMPLETED);
		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.COMPLETED_TIME_PROPERTY), new Date());

		// Get the process instance
		final NodeInterface instance = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.PROCESS_INSTANCE_PROPERTY));
		if (instance == null) {
			throw new FrameworkException(422, "Task has no associated process instance");
		}

		// Find the waiting token at the user task element
		final NodeInterface userTaskElement = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.DEFINED_BY_PROPERTY));
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
		final PropertyKey<NodeInterface> subjectKey = instance.getTraits().key(ProcessInstanceTraitDefinition.SUBJECT_PROPERTY);
		final NodeInterface preListenerSubject      = instance.getProperty(subjectKey);

		fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_COMPLETED, taskNode, parameters);

		final NodeInterface postListenerSubject     = instance.getProperty(subjectKey);
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
			storeParameterValues(app, instance, userTaskElement, parameters);
		} else if (listenerAttachedSubject) {
			logger.info("Task '{}' completion: subject '{}' was attached by the listener; engine yields persistence to it.",
				safeName(taskNode), postListenerSubject.getUuid());
		}

		// Reactivate the token and move it past the user task to the next element
		final Traits tokenTraits = waitingToken.getTraits();
		waitingToken.setProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY), ProcessTokenTraitDefinition.STATUS_ACTIVE);

		// Cancel any pending boundary timers attached to this userTask --
		// they're moot now that the task is done.
		cancelTimersForActivity(app, userTaskElement, instance);

		completeTokenAndMoveToNext(app, instance, waitingToken, userTaskElement);
	}

	/**
	 * Signal an intermediate catch event, resuming the waiting token.
	 * The catch event is identified by its bpmnId within the process instance.
	 *
	 * @param eventBpmnId the bpmnId of the intermediateCatchEvent element
	 * @param parameters  optional key-value map of process parameter values
	 */
	public void signalEvent(final NodeInterface callerInstanceNode, final String eventBpmnId,
						   final Map<String, Object> parameters) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
		// Re-fetch under superuser context so token/element lookups are unfiltered.
		final NodeInterface instanceNode = elevate(app, callerInstanceNode);
		final Traits instTraits = instanceNode.getTraits();

		// Verify instance is running
		final String status = instanceNode.getProperty(instTraits.key(ProcessInstanceTraitDefinition.STATUS_PROPERTY));
		if (!ProcessInstanceTraitDefinition.STATUS_RUNNING.equals(status)) {
			throw new FrameworkException(422, "Process instance is not running (status: " + status + ")");
		}

		// Find the catch event element by bpmnId
		final Traits elemTraits = Traits.of(ProcessTraits.BPMN_ELEMENT);
		final NodeInterface catchElement = app.nodeQuery(ProcessTraits.BPMN_ELEMENT)
			.and().key(elemTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY), eventBpmnId)
			.getFirst();

		if (catchElement == null) {
			throw new FrameworkException(422, "No element found with bpmnId: " + eventBpmnId);
		}

		// Verify it is a catch event
		final String elementType = catchElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY));
		if (!"intermediateCatchEvent".equals(elementType)) {
			throw new FrameworkException(422, "Element " + eventBpmnId + " is not an intermediateCatchEvent (type: " + elementType + ")");
		}

		// Find the waiting token at this element
		final NodeInterface waitingToken = findWaitingToken(instanceNode, catchElement);
		if (waitingToken == null) {
			throw new FrameworkException(422, "No waiting token at catch event: " + eventBpmnId);
		}

		// Store parameter values if provided
		if (parameters != null && !parameters.isEmpty()) {
			storeParameterValues(app, instanceNode, catchElement, parameters);
		}

		// Reactivate and advance
		final Traits tokenTraits = waitingToken.getTraits();
		waitingToken.setProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY), ProcessTokenTraitDefinition.STATUS_ACTIVE);
		completeTokenAndMoveToNext(app, instanceNode, waitingToken, catchElement);

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
	private void advanceToken(final App app, final NodeInterface instance, final NodeInterface token) throws FrameworkException {

		final Traits tokenTraits = token.getTraits();
		final NodeInterface currentElement = token.getProperty(tokenTraits.key(ProcessTokenTraitDefinition.AT_ELEMENT_PROPERTY));
		if (currentElement == null) {
			logger.warn("Token {} has no current element, cannot advance", token.getUuid());
			return;
		}

		final Traits elemTraits = currentElement.getTraits();
		final String elementType = currentElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY));

		logger.debug("Advancing token {} at element {} ({})",
			token.getUuid(),
			currentElement.getProperty(elemTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)),
			elementType);

		switch (elementType) {

			case "startEvent":
				// Pass-through: move to the single outgoing flow target
				completeTokenAndMoveToNext(app, instance, token, currentElement);
				break;

			case "endEvent":
				// Token reaches the end: check if inside a sub-process
				completeToken(token, tokenTraits);
				final NodeInterface parentElement = currentElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.PARENT_ELEMENT_PROPERTY));
				if (parentElement != null && "subProcess".equals(parentElement.getProperty(parentElement.getTraits().key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY)))) {
					// Sub-process end: resume the parent token
					resumeSubProcessParent(app, instance, parentElement);
				} else {
					checkProcessCompletion(app, instance);
				}
				break;

			case "userTask":
				// Create a TaskInstance, schedule any boundary timers, then put the token in waiting state.
				createTaskInstance(app, instance, currentElement);
				scheduleBoundaryTimers(app, instance, token, currentElement);
				token.setProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY), ProcessTokenTraitDefinition.STATUS_WAITING);
				break;

			case "serviceTask":
			case "scriptTask":
				// Execute the task logic, then advance
				executeAutomaticTask(app, instance, currentElement, elemTraits, elementType);
				completeTokenAndMoveToNext(app, instance, token, currentElement);
				break;

			case "manualTask":
			case "task":
				// Manual tasks and abstract tasks are pass-through for now
				// (manual tasks are performed outside the system)
				completeTokenAndMoveToNext(app, instance, token, currentElement);
				break;

			case "exclusiveGateway":
				handleExclusiveGateway(app, instance, token, currentElement);
				break;

			case "parallelGateway":
				handleParallelGateway(app, instance, token, currentElement);
				break;

			case "inclusiveGateway":
				handleInclusiveGateway(app, instance, token, currentElement);
				break;

			case "intermediateCatchEvent":
				// Waiting for an external event -- token waits.
				token.setProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY), ProcessTokenTraitDefinition.STATUS_WAITING);
				// If this catch event is a timer event, schedule the timer; the
				// service will fire it and advance the token when fireAt elapses.
				{
					final String evtDefType = currentElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.EVENT_DEF_TYPE_PROPERTY));
					if ("timerEventDefinition".equals(evtDefType)) {
						final String timerType  = currentElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.TIMER_TYPE_PROPERTY));
						final String timerValue = currentElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.TIMER_VALUE_PROPERTY));
						final Date fireAt = computeFireAt(timerType, timerValue);
						if (fireAt != null) {
							createTimer(app, fireAt, ProcessTimerTraitDefinition.TIMER_INTERMEDIATE, timerValue,
								instance, token, currentElement, true);
						}
					}
				}
				break;

			case "intermediateThrowEvent":
				// Fire-and-forget event -- pass through
				completeTokenAndMoveToNext(app, instance, token, currentElement);
				break;

			case "subProcess":
				// Enter the sub-process: find its start event and create a token there
				handleSubProcess(app, instance, token, currentElement);
				break;

			default:
				logger.warn("Unknown element type '{}' at element {}, treating as pass-through",
					elementType,
					currentElement.getProperty(elemTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));
				completeTokenAndMoveToNext(app, instance, token, currentElement);
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
	private void handleExclusiveGateway(final App app, final NodeInterface instance,
										final NodeInterface token, final NodeInterface element) throws FrameworkException {

		final List<NodeInterface> outgoingFlows = getOutgoingFlows(element);
		if (outgoingFlows.isEmpty()) {
			throw new FrameworkException(422, "Exclusive gateway has no outgoing flows: " + getBpmnId(element));
		}

		final Traits flowTraits = Traits.of(ProcessTraits.BPMN_SEQUENCE_FLOW);
		NodeInterface defaultFlow = null;
		NodeInterface selectedFlow = null;

		// Check for default flow attribute on the gateway
		final String defaultFlowId = getAttributeValue(element, "default");

		for (final NodeInterface flow : outgoingFlows) {

			final String flowId = flow.getProperty(flowTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));

			// Skip the default flow during condition evaluation
			if (flowId != null && flowId.equals(defaultFlowId)) {
				defaultFlow = flow;
				continue;
			}

			final String condition = flow.getProperty(flowTraits.key(BpmnSequenceFlowTraitDefinition.CONDITION_EXPRESSION_PROPERTY));
			if (condition != null && !condition.isEmpty()) {
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
			throw new FrameworkException(422, "No outgoing path matched at exclusive gateway: " + getBpmnId(element));
		}

		// Move token to the target of the selected flow
		final NodeInterface target = selectedFlow.getProperty(flowTraits.key(BpmnSequenceFlowTraitDefinition.TARGET_ELEMENT_PROPERTY));
		moveTokenToElement(token, target);
		advanceToken(app, instance, token);
	}

	/**
	 * Parallel gateway: fork (one token becomes many) or join (many become one).
	 * Determined by counting incoming vs outgoing flows.
	 */
	private void handleParallelGateway(final App app, final NodeInterface instance,
									   final NodeInterface token, final NodeInterface element) throws FrameworkException {

		final List<NodeInterface> outgoingFlows = getOutgoingFlows(element);
		final List<NodeInterface> incomingFlows = getIncomingFlows(element);
		final Traits flowTraits = Traits.of(ProcessTraits.BPMN_SEQUENCE_FLOW);

		if (outgoingFlows.size() > 1 && incomingFlows.size() <= 1) {

			// FORK: consume current token, create one new token per outgoing flow
			completeToken(token, token.getTraits());

			for (final NodeInterface flow : outgoingFlows) {
				final NodeInterface target = flow.getProperty(flowTraits.key(BpmnSequenceFlowTraitDefinition.TARGET_ELEMENT_PROPERTY));
				final NodeInterface newToken = createToken(app, instance, target);
				advanceToken(app, instance, newToken);
			}

		} else if (incomingFlows.size() > 1) {

			// JOIN: wait until tokens have arrived from all incoming paths
			final Traits tokenTraits = token.getTraits();
			token.setProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY), ProcessTokenTraitDefinition.STATUS_WAITING);

			// Count how many tokens are waiting at this element
			int waitingCount = countTokensAtElement(instance, element);

			if (waitingCount >= incomingFlows.size()) {

				// All tokens arrived -- consume them all, create one continuing token
				consumeAllTokensAtElement(app, instance, element);

				if (!outgoingFlows.isEmpty()) {
					final NodeInterface outFlow = outgoingFlows.get(0);
					final NodeInterface target = outFlow.getProperty(flowTraits.key(BpmnSequenceFlowTraitDefinition.TARGET_ELEMENT_PROPERTY));
					final NodeInterface newToken = createToken(app, instance, target);
					advanceToken(app, instance, newToken);
				}
			}

		} else {

			// Single in, single out -- pass through
			completeTokenAndMoveToNext(app, instance, token, element);
		}
	}

	/**
	 * Inclusive gateway: fork to all outgoing paths whose conditions are true
	 * (at least one must match, or the default is taken). Join waits for all
	 * tokens that were actually created.
	 */
	private void handleInclusiveGateway(final App app, final NodeInterface instance,
										final NodeInterface token, final NodeInterface element) throws FrameworkException {

		final List<NodeInterface> outgoingFlows = getOutgoingFlows(element);
		final List<NodeInterface> incomingFlows = getIncomingFlows(element);
		final Traits flowTraits = Traits.of(ProcessTraits.BPMN_SEQUENCE_FLOW);

		if (outgoingFlows.size() > 1 && incomingFlows.size() <= 1) {

			// FORK: evaluate conditions on all outgoing flows
			final String defaultFlowId = getAttributeValue(element, "default");
			NodeInterface defaultFlow = null;
			final List<NodeInterface> selectedTargets = new ArrayList<>();

			for (final NodeInterface flow : outgoingFlows) {
				final String flowId = flow.getProperty(flowTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));

				if (flowId != null && flowId.equals(defaultFlowId)) {
					defaultFlow = flow;
					continue;
				}

				final String condition = flow.getProperty(flowTraits.key(BpmnSequenceFlowTraitDefinition.CONDITION_EXPRESSION_PROPERTY));
				if (condition != null && !condition.isEmpty()) {
					if (evaluateCondition(condition, element, instance)) {
						final NodeInterface target = flow.getProperty(flowTraits.key(BpmnSequenceFlowTraitDefinition.TARGET_ELEMENT_PROPERTY));
						selectedTargets.add(target);
					}
				} else {
					// No condition -- always taken
					final NodeInterface target = flow.getProperty(flowTraits.key(BpmnSequenceFlowTraitDefinition.TARGET_ELEMENT_PROPERTY));
					selectedTargets.add(target);
				}
			}

			// If no paths matched, take the default
			if (selectedTargets.isEmpty() && defaultFlow != null) {
				final NodeInterface target = defaultFlow.getProperty(flowTraits.key(BpmnSequenceFlowTraitDefinition.TARGET_ELEMENT_PROPERTY));
				selectedTargets.add(target);
			}

			if (selectedTargets.isEmpty()) {
				throw new FrameworkException(422, "No outgoing path matched at inclusive gateway: " + getBpmnId(element));
			}

			// Consume current token, create new tokens for selected paths
			completeToken(token, token.getTraits());

			for (final NodeInterface target : selectedTargets) {
				final NodeInterface newToken = createToken(app, instance, target);
				advanceToken(app, instance, newToken);
			}

		} else if (incomingFlows.size() > 1) {

			// JOIN: wait for all tokens that are actually in flight.
			// Unlike a parallel join (which always waits for all incoming paths),
			// an inclusive join only waits for paths that actually have tokens.
			// We detect this by checking if any active/waiting tokens exist
			// elsewhere in the process (not at this gateway). If not, all
			// expected tokens have arrived.
			final Traits tokenTraitsInc = token.getTraits();
			token.setProperty(tokenTraitsInc.key(ProcessTokenTraitDefinition.STATUS_PROPERTY), ProcessTokenTraitDefinition.STATUS_WAITING);

			int tokensAtGateway = countTokensAtElement(instance, element);
			int tokensElsewhere = countActiveTokensNotAtElement(instance, element);

			if (tokensElsewhere == 0) {
				// All in-flight tokens have arrived at this gateway
				consumeAllTokensAtElement(app, instance, element);

				if (!outgoingFlows.isEmpty()) {
					final NodeInterface outFlow = outgoingFlows.get(0);
					final NodeInterface target = outFlow.getProperty(flowTraits.key(BpmnSequenceFlowTraitDefinition.TARGET_ELEMENT_PROPERTY));
					final NodeInterface newToken = createToken(app, instance, target);
					advanceToken(app, instance, newToken);
				}
			}

		} else {

			completeTokenAndMoveToNext(app, instance, token, element);
		}
	}

	// -----------------------------------------------------------------------
	// Sub-process handling
	// -----------------------------------------------------------------------

	private void handleSubProcess(final App app, final NodeInterface instance,
								  final NodeInterface token, final NodeInterface subProcessElement) throws FrameworkException {

		// Find the start event inside the sub-process (child element)
		final Traits elemTraits = subProcessElement.getTraits();
		final Iterable<NodeInterface> childElements = subProcessElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.CHILD_ELEMENTS_PROPERTY));

		NodeInterface subStartEvent = null;
		if (childElements != null) {
			for (final NodeInterface child : childElements) {
				final String childType = child.getProperty(child.getTraits().key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY));
				if ("startEvent".equals(childType)) {
					subStartEvent = child;
					break;
				}
			}
		}

		if (subStartEvent == null) {
			throw new FrameworkException(422, "Sub-process has no start event: " + getBpmnId(subProcessElement));
		}

		// Put the parent token in waiting state (it will be resumed when the sub-process completes)
		final Traits tokenTraits = token.getTraits();
		token.setProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY), ProcessTokenTraitDefinition.STATUS_WAITING);

		// Create a new token at the sub-process start event
		final NodeInterface subToken = createToken(app, instance, subStartEvent);
		advanceToken(app, instance, subToken);
	}

	// -----------------------------------------------------------------------
	// Task execution
	// -----------------------------------------------------------------------

	private void executeAutomaticTask(final App app, final NodeInterface instance,
									  final NodeInterface element, final Traits elemTraits,
									  final String elementType) throws FrameworkException {

		if ("scriptTask".equals(elementType)) {

			final String script = element.getProperty(elemTraits.key(BpmnElementTraitDefinition.SCRIPT_CONTENT_PROPERTY));
			if (script != null && !script.isEmpty()) {
				final String scriptFormat = getAttributeValue(element, "scriptFormat");

				// Determine executable script: if the script uses a foreign format
				// (javascript/js), transpile it to Structr-compatible code.
				// Native Structr scripts (no format or "structrscript") run as-is.
				final String executableScript;
				final boolean isJavaScript;

				if ("javascript".equalsIgnoreCase(scriptFormat) || "js".equalsIgnoreCase(scriptFormat)) {
					// Foreign JavaScript (Camunda/Flowable): transpile to Structr JS
					executableScript = transpileForeignScript(script);
					isJavaScript = true;
				} else if ("structr-javascript".equalsIgnoreCase(scriptFormat) || "structr-js".equalsIgnoreCase(scriptFormat)) {
					// Structr-native JavaScript: execute as-is
					executableScript = script;
					isJavaScript = true;
				} else {
					// No format or "structrscript": execute as StructrScript
					executableScript = script;
					isJavaScript = false;
				}

				try {
					final ActionContext ctx = new ActionContext(securityContext);
					installProcessContext(ctx, instance, element);
					final String expression = isJavaScript
						? "${js{" + executableScript + "}}"
						: "${" + executableScript + "}";
					Scripting.evaluate(ctx, element, expression, "scriptTask");
				} catch (Exception ex) {
					logger.warn("Script task {} failed (non-fatal): {}\n--- Original ---\n{}\n--- Transpiled ---\n{}",
						getBpmnId(element), ex.getMessage(), script, executableScript);
				}
			}
		}

		// serviceTask: could invoke a Structr method, flow, or REST call.
		// For now, service tasks are pass-through. The service task implementation
		// will be connected via schema methods on the BpmnElement or via
		// extension attributes referencing Structr methods.
	}

	/**
	 * Transpile a foreign (Camunda/Flowable-style) JavaScript script into
	 * Structr-compatible JavaScript. The original source is preserved as
	 * a block comment, and equivalent Structr code is emitted below it.
	 *
	 * Supported transformations:
	 *   execution.getVariable("x")      -> $.process.x
	 *   execution.setVariable("x", val) -> (removed, assignment sufficient)
	 *
	 * Lines that cannot be transpiled are kept as-is (best effort).
	 */
	private String transpileForeignScript(final String script) {

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

			// execution.setVariable("x", expr); -> drop entirely
			// (the variable was already assigned via getVariable, and
			// mutations on the object are in-place in JS)
			if (trimmed.matches("execution\\.setVariable\\(.*\\);")) {
				continue;
			}

			// execution.getVariable("x") -> $.process.x
			String transpiled = line;
			transpiled = transpiled.replaceAll(
				"execution\\.getVariable\\([\"']([^\"']+)[\"']\\)",
				"\\$.process.$1"
			);

			out.append(transpiled).append("\n");
		}

		return out.toString();
	}

	private void createTaskInstance(final App app, final NodeInterface instance,
									final NodeInterface userTaskElement) throws FrameworkException {

		final NodeInterface task = app.create(ProcessTraits.TASK_INSTANCE, (String) null);
		final Traits taskTraits = task.getTraits();

		task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.CREATED_TIME_PROPERTY), new Date());
		task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.PROCESS_INSTANCE_PROPERTY), instance);
		task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.DEFINED_BY_PROPERTY), userTaskElement);

		// Set display name from the BPMN element name
		final Traits elemTraits = userTaskElement.getTraits();
		final String taskName = userTaskElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.BPMN_NAME_PROPERTY));
		task.setProperty(taskTraits.key("name"), taskName != null ? taskName : "User Task");

		// Resolve performers declared on the BPMN element via standard
		// <bpmn:humanPerformer> / <bpmn:potentialOwner> sub-elements
		// (parsed by the importer into BpmnPerformer nodes linked via HAS_PERFORMER).
		final PrincipalExpressionResolver resolver = new PrincipalExpressionResolver(app, instance);
		final Iterable<NodeInterface> performers = userTaskElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.PERFORMERS_PROPERTY));

		NodeInterface assigneeNode = null;
		final List<NodeInterface> candidateAssignees = new ArrayList<>();

		if (performers != null) {
			final Traits perfTraits = Traits.of(ProcessTraits.BPMN_PERFORMER);
			for (final NodeInterface performer : performers) {

				final String kind       = performer.getProperty(perfTraits.key(BpmnPerformerTraitDefinition.KIND_PROPERTY));
				final String contextLabel = "task '" + taskName + "' (" + kind + ")";

				// Typed Principal binding takes priority over the expression
				// string. When the editor's Principal picker has populated the
				// HAS_PRINCIPAL relationship the linked nodes ARE the resolved
				// performers; expression evaluation is skipped entirely.
				final Iterable<NodeInterface> linkedPrincipals = performer.getProperty(perfTraits.key(BpmnPerformerTraitDefinition.PRINCIPALS_PROPERTY));
				final List<NodeInterface> linkedList = new ArrayList<>();
				if (linkedPrincipals != null) {
					for (final NodeInterface p : linkedPrincipals) if (p != null) linkedList.add(p);
				}

				if (!linkedList.isEmpty()) {

					if (BpmnPerformerTraitDefinition.KIND_POTENTIAL_OWNER.equals(kind)) {
						candidateAssignees.addAll(linkedList);
					} else {
						// humanPerformer / generic performer: take the first
						// linked principal as the assignee. Multi-link on a
						// human performer is unusual but tolerated.
						if (assigneeNode == null) assigneeNode = linkedList.get(0);
					}
					continue;
				}

				// No typed binding: fall back to evaluating the expression.
				final String expression = performer.getProperty(perfTraits.key(BpmnPerformerTraitDefinition.EXPRESSION_PROPERTY));
				if (expression == null || expression.isEmpty()) {
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

			task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_PROPERTY),        assigneeNode);
			task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY),          TaskInstanceTraitDefinition.STATUS_RESERVED);
			task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_SET_BY_PROPERTY), TaskInstanceTraitDefinition.SET_BY_BPMN);
			// Direct assignment via <humanPerformer>: claimedTime equals createdTime,
			// i.e. the assignee was established at task-creation time, no separate claim event.
			task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.CLAIMED_TIME_PROPERTY),    new Date());
			grant(task, assigneeNode.as(Principal.class), Permission.read, Permission.write);
			// Generic participant grant: read on the parent ProcessInstance. Read
			// propagation on TASK_OF / HAS_PARAMETER_VALUE then extends visibility
			// to the instance's other tasks and parameter values without per-object
			// grants. Single source of truth for participant access.
			grant(instance, assigneeNode.as(Principal.class), Permission.read);

		} else if (!candidateAssignees.isEmpty()) {

			task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.CANDIDATE_ASSIGNEES_PROPERTY), candidateAssignees);
			task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY),           TaskInstanceTraitDefinition.STATUS_AVAILABLE);

			// Grant read+write on the task to each candidate, plus read on the parent
			// ProcessInstance so propagation extends visibility to the rest of the
			// instance (sibling tasks, parameter history).
			for (final NodeInterface owner : candidateAssignees) {
				final Principal ownerPrincipal = owner.as(Principal.class);
				grant(task,     ownerPrincipal, Permission.read, Permission.write);
				grant(instance, ownerPrincipal, Permission.read);
			}

		} else {

			// No performers declared. If the definition has the
			// defaultAssigneeFromInitiator flag set, fall back to assigning
			// the task to the process initiator. Convenience for editor-
			// authored processes where humanPerformer hasn't been wired up;
			// off by default so imported BPMN keeps spec semantics.
			NodeInterface fallbackAssignee = null;
			final NodeInterface defForFlag = instance.getProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY));
			if (defForFlag != null) {
				final Traits defTraitsForFlag = defForFlag.getTraits();
				final Boolean flag = defForFlag.getProperty(defTraitsForFlag.key(BpmnProcessTraitDefinition.DEFAULT_ASSIGNEE_FROM_INITIATOR_PROPERTY));
				if (Boolean.TRUE.equals(flag)) {
					fallbackAssignee = instance.getProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.INITIATOR_PROPERTY));
				}
			}

			if (fallbackAssignee != null) {
				task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_PROPERTY),        fallbackAssignee);
				task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY),          TaskInstanceTraitDefinition.STATUS_RESERVED);
				task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_SET_BY_PROPERTY), TaskInstanceTraitDefinition.SET_BY_BPMN);
				task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.CLAIMED_TIME_PROPERTY),    new Date());
				grant(task,     fallbackAssignee.as(Principal.class), Permission.read, Permission.write);
				grant(instance, fallbackAssignee.as(Principal.class), Permission.read);
			} else {
				task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY), TaskInstanceTraitDefinition.STATUS_CREATED);
			}
		}

		logger.info("Created task instance '{}' for process {} (status={})", taskName, instance.getUuid(),
			task.getProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY)));

		// Lifecycle events: 'created' always; 'assigned' or 'available' depending on
		// the initial state we just established.
		fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_CREATED, task);
		final String createStatus = task.getProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY));
		if (TaskInstanceTraitDefinition.STATUS_RESERVED.equals(createStatus)) {
			fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_ASSIGNED, task);
		} else if (TaskInstanceTraitDefinition.STATUS_AVAILABLE.equals(createStatus)) {
			fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_AVAILABLE, task);
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

		final App app = StructrApp.getInstance(securityContext);
		// Re-fetch under superuser context so the candidateAssignees traversal
		// reflects the engine's view of the graph, not the caller's filter.
		final NodeInterface taskNode = elevate(app, callerTaskNode);
		final Traits taskTraits = taskNode.getTraits();
		final String status = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY));

		if (!TaskInstanceTraitDefinition.STATUS_AVAILABLE.equals(status)) {
			// Common authoring trap: action mapping resolves idExpression to a sibling
			// task in the same instance instead of the active one. Since participants
			// have read on every task of their instance (engine grants + propagation),
			// expressions like ${first(current.tasks).id} are unstable -- they may pick
			// a completed task. Configure the action's "step" so the engine resolves
			// the active task at that step, or use a step-scoped expression.
			throw new FrameworkException(422, "Task is not available for claim (status=" + status + "). "
				+ "Hint: check that the action mapping resolves to the active TaskInstance "
				+ "at the intended step -- prefer Control-process action with idExpression=${current.id} and step=<the user task>.");
		}

		// Collect the UUIDs of the caller's groups (privileged so we see groups regardless of ACLs).
		final Set<String> callerGroupIds = new HashSet<>();
		for (final NodeInterface g : caller.getParentsPrivileged()) {
			callerGroupIds.add(g.getUuid());
		}

		// The caller is eligible if they are a direct candidate assignee or if one of their groups is.
		final Iterable<NodeInterface> candidateAssignees = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.CANDIDATE_ASSIGNEES_PROPERTY));
		boolean eligible = false;
		if (candidateAssignees != null) {
			for (final NodeInterface owner : candidateAssignees) {
				if (owner.getUuid().equals(caller.getUuid()) || callerGroupIds.contains(owner.getUuid())) {
					eligible = true;
					break;
				}
			}
		}

		if (!eligible) {
			throw new FrameworkException(403, "Caller is not a candidate assignee of this task");
		}

		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_PROPERTY),        caller);
		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY),          TaskInstanceTraitDefinition.STATUS_RESERVED);
		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_SET_BY_PROPERTY), TaskInstanceTraitDefinition.SET_BY_SELF);
		// Record the claim moment for audit: the gap between createdTime and
		// claimedTime tells operators how long the task waited for a claimer,
		// and a reserved task with no completedTime tells them the claimer is
		// still on it (or stalled).
		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.CLAIMED_TIME_PROPERTY),    new Date());

		// Claim supersedes a prior decline: remove caller from declinedBy if present.
		final List<NodeInterface> currentDeclined = collect(taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.DECLINED_BY_PROPERTY)));
		boolean wasDeclined = currentDeclined.removeIf(d -> d.getUuid().equals(caller.getUuid()));
		if (wasDeclined) {
			taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.DECLINED_BY_PROPERTY), currentDeclined);
		}

		// Ensure the claimer has read+write on the task (may already be granted via candidate assignee).
		grant(taskNode, caller, Permission.read, Permission.write);
		// Generic participant grant: also ensure read on the parent ProcessInstance.
		// Idempotent if the candidate-assignee path already granted it; defensive for
		// claim flows where the caller's eligibility came from group membership but
		// no direct instance grant exists.
		final NodeInterface claimedInstance = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.PROCESS_INSTANCE_PROPERTY));
		if (claimedInstance != null) {
			grant(claimedInstance, caller, Permission.read);
		}

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

		final App app = StructrApp.getInstance(securityContext);
		final NodeInterface taskNode = elevate(app, callerTaskNode);
		final NodeInterface assignee = elevate(app, callerAssignee);
		final Traits taskTraits = taskNode.getTraits();

		final String status = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY));
		if (TaskInstanceTraitDefinition.STATUS_COMPLETED.equals(status)) {
			throw new FrameworkException(422, "Cannot assign a completed task");
		}
		if (TaskInstanceTraitDefinition.STATUS_CANCELLED.equals(status)) {
			throw new FrameworkException(422, "Cannot assign a cancelled task");
		}

		// Capture the previous assignee BEFORE overwriting, so we can revoke
		// their grant if it came from an assignment (not from being a candidate).
		final NodeInterface previousAssignee = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_PROPERTY));

		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_PROPERTY),        assignee);
		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY),          TaskInstanceTraitDefinition.STATUS_RESERVED);
		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_SET_BY_PROPERTY), TaskInstanceTraitDefinition.SET_BY_ADMIN);
		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.CLAIMED_TIME_PROPERTY),    new Date());

		// Revoke previous assignee's R+W if (a) it's a real reassignment (different
		// principal) AND (b) they're not a current candidate assignee. Candidate-assignee
		// membership keeps their grant; non-candidate admin-grants are removed cleanly.
		revokePreviousAssigneeGrantIfApplicable(taskNode, taskTraits, previousAssignee, assignee);

		grant(taskNode, assignee.as(Principal.class), Permission.read, Permission.write);
		// Generic participant grant on the parent ProcessInstance for the new assignee.
		final NodeInterface assignedInstance = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.PROCESS_INSTANCE_PROPERTY));
		if (assignedInstance != null) {
			grant(assignedInstance, assignee.as(Principal.class), Permission.read);
		}

		logger.info("Task '{}' assigned to '{}' by admin caller '{}'",
			taskNode.getName(), assignee.getName(), (caller != null ? caller.getName() : "<system>"));

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
	private void revokePreviousAssigneeGrantIfApplicable(final NodeInterface taskNode, final Traits taskTraits,
														 final NodeInterface previousAssignee, final NodeInterface newAssignee) throws FrameworkException {
		if (previousAssignee == null) return;
		if (newAssignee != null && previousAssignee.getUuid().equals(newAssignee.getUuid())) return;

		// Membership check against direct candidateAssignees only. Group membership
		// is a separate inheritance path and we don't revoke at the user level
		// for group-derived grants: we don't have one to revoke.
		final Iterable<NodeInterface> candidates = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.CANDIDATE_ASSIGNEES_PROPERTY));
		if (candidates != null) {
			for (final NodeInterface candidate : candidates) {
				if (candidate.getUuid().equals(previousAssignee.getUuid())) {
					return; // still a candidate, keep their grant
				}
			}
		}

		try {
			final AccessControllable ac = taskNode.as(AccessControllable.class);
			ac.revoke(Permission.read,  previousAssignee.as(Principal.class));
			ac.revoke(Permission.write, previousAssignee.as(Principal.class));
			logger.info("Revoked R+W on task '{}' for previous assignee '{}'",
				taskNode.getName(), previousAssignee.getName());
		} catch (Exception ex) {
			logger.warn("Could not revoke previous assignee's grant on task '{}': {}",
				taskNode.getName(), ex.getMessage());
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

		final App app = StructrApp.getInstance(securityContext);
		final NodeInterface taskNode = elevate(app, callerTaskNode);
		final Traits taskTraits = taskNode.getTraits();

		final String status = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY));
		if (!TaskInstanceTraitDefinition.STATUS_RESERVED.equals(status)) {
			throw new FrameworkException(422, "Task is not assigned (status=" + status + "); cannot release");
		}

		final NodeInterface currentAssignee = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_PROPERTY));
		if (currentAssignee == null || !caller.getUuid().equals(currentAssignee.getUuid())) {
			throw new FrameworkException(403, "Only the current assignee can release this task");
		}

		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_PROPERTY),        null);
		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY),          TaskInstanceTraitDefinition.STATUS_AVAILABLE);
		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_SET_BY_PROPERTY), null);

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

		final App app = StructrApp.getInstance(securityContext);
		final NodeInterface taskNode = elevate(app, callerTaskNode);
		final Traits taskTraits = taskNode.getTraits();

		final String status = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY));
		if (TaskInstanceTraitDefinition.STATUS_COMPLETED.equals(status)
			|| TaskInstanceTraitDefinition.STATUS_CANCELLED.equals(status)) {
			throw new FrameworkException(422, "Cannot decline a task in terminal status (" + status + ")");
		}

		// Authorization: caller must be among the effective candidate assignees
		// (directly or via group membership). Refusing decline from arbitrary
		// users keeps the audit signal meaningful -- only candidates can decline.
		if (!isCallerEffectivePotentialOwner(taskNode, taskTraits)) {
			throw new FrameworkException(403, "Caller is not a candidate assignee of this task; cannot decline");
		}

		// Add caller to declinedBy (idempotent).
		final List<NodeInterface> currentDeclined = collect(taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.DECLINED_BY_PROPERTY)));
		final NodeInterface callerNode = app.getNodeById(caller.getUuid());
		boolean alreadyDeclined = false;
		for (final NodeInterface d : currentDeclined) {
			if (d.getUuid().equals(caller.getUuid())) {
				alreadyDeclined = true;
				break;
			}
		}
		if (!alreadyDeclined && callerNode != null) {
			currentDeclined.add(callerNode);
			taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.DECLINED_BY_PROPERTY), currentDeclined);
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

		final App app = StructrApp.getInstance(securityContext);
		final NodeInterface taskNode = elevate(app, callerTaskNode);
		final NodeInterface delegate = elevate(app, callerDelegate);
		final Traits taskTraits = taskNode.getTraits();

		final String status = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY));
		if (TaskInstanceTraitDefinition.STATUS_COMPLETED.equals(status)
			|| TaskInstanceTraitDefinition.STATUS_CANCELLED.equals(status)) {
			throw new FrameworkException(422, "Cannot delegate a task in terminal status (" + status + ")");
		}

		// Authorization: assigned -> caller must be the assignee. available ->
		// caller must be a candidate assignee.
		if (TaskInstanceTraitDefinition.STATUS_RESERVED.equals(status)) {
			final NodeInterface currentAssignee = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_PROPERTY));
			if (currentAssignee == null || !caller.getUuid().equals(currentAssignee.getUuid())) {
				throw new FrameworkException(403, "Only the current assignee can delegate an assigned task");
			}
		} else if (TaskInstanceTraitDefinition.STATUS_AVAILABLE.equals(status)) {
			if (!isCallerEffectivePotentialOwner(taskNode, taskTraits)) {
				throw new FrameworkException(403, "Caller is not a candidate assignee of this task; cannot delegate");
			}
		} else {
			throw new FrameworkException(422, "Task cannot be delegated in status=" + status);
		}

		// Capture previous assignee (if any) for grant cleanup.
		final NodeInterface previousAssignee = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_PROPERTY));

		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_PROPERTY),        delegate);
		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY),          TaskInstanceTraitDefinition.STATUS_RESERVED);
		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_SET_BY_PROPERTY), TaskInstanceTraitDefinition.SET_BY_DELEGATION);
		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.CLAIMED_TIME_PROPERTY),    new Date());

		// Same revocation rules as admin assignTask: revoke previous assignee's
		// grant if they're not also a candidate assignee.
		revokePreviousAssigneeGrantIfApplicable(taskNode, taskTraits, previousAssignee, delegate);

		grant(taskNode, delegate.as(Principal.class), Permission.read, Permission.write);
		// Generic participant grant on the parent ProcessInstance for the delegate.
		final NodeInterface delegatedInstance = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.PROCESS_INSTANCE_PROPERTY));
		if (delegatedInstance != null) {
			grant(delegatedInstance, delegate.as(Principal.class), Permission.read);
		}

		logger.info("Task '{}' delegated to '{}' by '{}'",
			taskNode.getName(), delegate.getName(), caller.getName());

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

		final App app = StructrApp.getInstance(securityContext);
		final NodeInterface taskNode = elevate(app, callerTaskNode);
		final Traits taskTraits = taskNode.getTraits();

		final String status = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY));
		if (TaskInstanceTraitDefinition.STATUS_COMPLETED.equals(status)) {
			throw new FrameworkException(422, "Task is already completed");
		}
		if (TaskInstanceTraitDefinition.STATUS_CANCELLED.equals(status)) {
			throw new FrameworkException(422, "Task is already cancelled");
		}

		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY),         TaskInstanceTraitDefinition.STATUS_CANCELLED);
		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.CANCELLED_TIME_PROPERTY), new Date());

		// Find and complete the waiting token at the task's element so the
		// instance no longer carries an active token here. Process advancement
		// is intentionally not triggered -- admin must explicitly act on the
		// instance afterwards.
		final NodeInterface instance = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.PROCESS_INSTANCE_PROPERTY));
		final NodeInterface element  = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.DEFINED_BY_PROPERTY));
		if (instance != null && element != null) {
			final NodeInterface waitingToken = findWaitingToken(instance, element);
			if (waitingToken != null) {
				final Traits tokenTraits = waitingToken.getTraits();
				waitingToken.setProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY), ProcessTokenTraitDefinition.STATUS_COMPLETED);
			}
		}

		logger.info("Task '{}' cancelled by admin caller '{}'",
			taskNode.getName(), (caller != null ? caller.getName() : "<system>"));

		fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_CANCELLED, taskNode);

		// Cancel pending boundary timers on this activity.
		final NodeInterface inst = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.PROCESS_INSTANCE_PROPERTY));
		final NodeInterface elem = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.DEFINED_BY_PROPERTY));
		cancelTimersForActivity(app, elem, inst);
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

		final App app = StructrApp.getInstance(securityContext);
		final NodeInterface taskNode = elevate(app, callerTaskNode);
		final Traits taskTraits = taskNode.getTraits();

		final String status = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY));
		if (TaskInstanceTraitDefinition.STATUS_COMPLETED.equals(status)) {
			throw new FrameworkException(422, "Cannot make a completed task available");
		}
		if (TaskInstanceTraitDefinition.STATUS_CANCELLED.equals(status)) {
			throw new FrameworkException(422, "Cannot make a cancelled task available");
		}
		if (TaskInstanceTraitDefinition.STATUS_AVAILABLE.equals(status)) {
			final NodeInterface existingAssignee = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_PROPERTY));
			if (existingAssignee == null) {
				// Already available with no assignee: still useful to re-grant
				// candidate access in case a previous admin tweak removed grants.
				logger.info("Task '{}' already available; re-applying candidate-assignee grants",
					taskNode.getName());
			}
		}

		final NodeInterface previousAssignee = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_PROPERTY));

		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_PROPERTY),        null);
		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY),          TaskInstanceTraitDefinition.STATUS_AVAILABLE);
		taskNode.setProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_SET_BY_PROPERTY), null);

		// Revoke previous assignee's R+W if they're not a current candidate assignee.
		revokePreviousAssigneeGrantIfApplicable(taskNode, taskTraits, previousAssignee, null);

		// Re-grant R+W to every current candidate assignee. Idempotent for those
		// who already hold the grant; restores access where it was missing.
		final Iterable<NodeInterface> candidateAssignees = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.CANDIDATE_ASSIGNEES_PROPERTY));
		if (candidateAssignees != null) {
			for (final NodeInterface candidate : candidateAssignees) {
				grant(taskNode, candidate.as(Principal.class), Permission.read, Permission.write);
			}
		}

		logger.info("Task '{}' returned to available pool by admin caller '{}'",
			taskNode.getName(), (caller != null ? caller.getName() : "<system>"));

		fireTaskEvent(BpmnTaskListenerTraitDefinition.EVENT_AVAILABLE, taskNode);
	}

	// -----------------------------------------------------------------------
	// Lifecycle event dispatch (task listeners)
	// -----------------------------------------------------------------------

	/**
	 * Fire a task lifecycle event. Dispatches to:
	 * <ol>
	 *   <li><b>Per-userTask BPMN listeners</b> (Path A): listeners declared on the
	 *       task's defining BpmnElement via {@code <structr:taskListener>}, filtered
	 *       to those whose {@code event} matches.</li>
	 *   <li><b>Schema-method fallback</b> (Path B): a method named
	 *       {@code on<EventName>} on the {@code TaskInstance} type (e.g.
	 *       {@code onTaskAssigned}). Invoked if defined.</li>
	 * </ol>
	 *
	 * Default dispatch is async: listener exceptions are caught and logged; the
	 * engine state transition stands. A listener with {@code sync=true} can
	 * propagate exceptions and abort the surrounding transaction.
	 *
	 * @param eventName one of {@code created|assigned|claimed|available|declined|completed|cancelled}
	 * @param taskNode  the TaskInstance the event is about
	 */
	private void fireTaskEvent(final String eventName, final NodeInterface taskNode) {
		fireTaskEvent(eventName, taskNode, null);
	}

	/**
	 * Fire a task lifecycle event. The {@code submittedParams} variant is used
	 * for the {@code completed} event so the listener sees the just-submitted
	 * form fields via NamedArguments before they are persisted -- enabling the
	 * "submit"-style pattern where a listener creates and attaches the
	 * instance's subject from the form data.
	 */
	private void fireTaskEvent(final String eventName, final NodeInterface taskNode, final Map<String, Object> submittedParams) {

		// 1. Per-task BPMN listeners (Path A)
		try {
			final Traits taskTraits = taskNode.getTraits();
			final NodeInterface element = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.DEFINED_BY_PROPERTY));
			if (element != null) {
				final Iterable<NodeInterface> listeners = element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.TASK_LISTENERS_PROPERTY));
				if (listeners != null) {
					final Traits listenerTraits = Traits.of(ProcessTraits.BPMN_TASK_LISTENER);
					for (final NodeInterface listener : listeners) {
						final String listenerEvent = listener.getProperty(listenerTraits.key(BpmnTaskListenerTraitDefinition.EVENT_PROPERTY));
						if (!eventName.equals(listenerEvent)) {
							continue;
						}
						final String methodName = listener.getProperty(listenerTraits.key(BpmnTaskListenerTraitDefinition.METHOD_PROPERTY));
						final Boolean sync      = listener.getProperty(listenerTraits.key(BpmnTaskListenerTraitDefinition.SYNC_PROPERTY));
						invokeListenerMethod(taskNode, methodName, eventName, Boolean.TRUE.equals(sync), submittedParams);
					}
				}
			}
		} catch (Exception ex) {
			// Don't let event-dispatch enumeration failures break the engine.
			logger.warn("Task listener enumeration for event '{}' on task '{}' failed: {}",
				eventName, safeName(taskNode), ex.getMessage());
		}

		// 2. Schema-method fallback (Path B). Always attempted as async; failures
		//    don't propagate. The method may simply not exist -- that's expected
		//    and silent.
		invokeListenerMethod(taskNode, fallbackMethodName(eventName), eventName, false, submittedParams);
	}

	/**
	 * Convert event name to convention-based fallback method name on TaskInstance.
	 * "assigned" -> "onTaskAssigned", etc.
	 */
	private String fallbackMethodName(final String eventName) {
		if (eventName == null || eventName.isEmpty()) return null;
		return "onTask" + Character.toUpperCase(eventName.charAt(0)) + eventName.substring(1);
	}

	/**
	 * Invoke a named method on the given task. Resolution order:
	 * <ol>
	 *   <li>Per-element method attached to the task's defining BpmnElement via
	 *       {@code BpmnElementHasMethod} (the (process, step) cell). This is the
	 *       primary home for code bound to a specific (process, step, event) triple.</li>
	 *   <li>Per-process method attached to the task's BpmnDefinitions via
	 *       {@code BpmnDefinitionsHasMethod}. Escape hatch for cross-step utilities.</li>
	 *   <li>Schema method on TaskInstance traits (also catches convention fallbacks
	 *       like {@code onTaskCompleted}).</li>
	 * </ol>
	 *
	 * <p>Every tier is invoked with the same named argument set:
	 * {@code task} (the TaskInstance), {@code processInstance}, {@code subject}
	 * (when set on the instance), and {@code eventName}. So a method body can use
	 * {@code $.methodParameters.task.complete(...)} or
	 * {@code $.methodParameters.processInstance.subject = ...} without needing to
	 * navigate from {@code $.this}. Tier 3 dispatches via
	 * {@link Methods#resolveMethod} so it also receives the named args, instead of
	 * the older {@code ${this.method()}} scripting form which couldn't carry args.</p>
	 *
	 * <p>Errors are caught and logged (async semantics). When {@code sync} is true,
	 * errors are rethrown so the caller can decide whether to abort the surrounding
	 * transaction.</p>
	 */
	private void invokeListenerMethod(final NodeInterface taskNode, final String methodName,
									  final String eventName, final boolean sync) {
		invokeListenerMethod(taskNode, methodName, eventName, sync, null);
	}

	private void invokeListenerMethod(final NodeInterface taskNode, final String methodName,
									  final String eventName, final boolean sync,
									  final Map<String, Object> submittedParams) {

		if (methodName == null || methodName.isEmpty()) {
			return;
		}

		try {
			// Engine context (superuser) so the listener can do whatever the
			// engine itself can do (read all engine state, mutate the task, etc.).
			final ActionContext ctx = new ActionContext(securityContext);
			final Arguments args = buildTaskListenerArgs(taskNode, eventName, submittedParams);

			// Tier 1: per-element method on the task's defining BpmnElement.
			final NodeInterface elementMethod = findElementMethod(taskNode, methodName);
			if (elementMethod != null) {
				new ScriptMethod(elementMethod.as(SchemaMethod.class))
					.execute(ctx, taskNode, args);
				return;
			}

			// Tier 2: per-process method on the task's BpmnDefinitions.
			final NodeInterface processMethod = findProcessMethod(taskNode, methodName);
			if (processMethod != null) {
				new ScriptMethod(processMethod.as(SchemaMethod.class))
					.execute(ctx, taskNode, args);
				return;
			}

			// Tier 3: schema method on TaskInstance traits (covers explicit methods
			// and convention fallbacks like onTaskCompleted). Resolved via
			// Methods.resolveMethod so the named args are passed through.
			final AbstractMethod m = Methods.resolveMethod(taskNode.getTraits(), methodName);
			if (m != null) {
				m.execute(ctx, taskNode, args);
			}
			// No match at any tier: silent. A convention fallback that doesn't exist
			// is fine; an explicit listener with a typo'd method name will appear in
			// the warnings via the catch block below if it actually gets dispatched.

		} catch (Exception ex) {
			final String msg = "Task listener '" + methodName + "' for event '" + eventName +
				"' on task '" + safeName(taskNode) + "' failed: " + ex.getMessage();
			if (sync) {
				logger.error(msg);
				// Wrap so callers can react / propagate. We deliberately do
				// not throw here for v1 -- sync semantics are documented but
				// not enforced as transaction-rollback yet. Upgrade path: rethrow
				// as RuntimeException to abort the surrounding transaction.
			} else {
				logger.warn(msg);
			}
		}
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
	private Arguments buildTaskListenerArgs(final NodeInterface taskNode, final String eventName,
											final Map<String, Object> submittedParams) {
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

	/**
	 * Look up a SchemaMethod by name attached to the BpmnElement that defines this
	 * task. Returns null on no match (caller falls through to the next tier).
	 */
	private NodeInterface findElementMethod(final NodeInterface taskNode, final String methodName) {
		try {
			final Traits taskTraits = taskNode.getTraits();
			final NodeInterface element = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.DEFINED_BY_PROPERTY));
			if (element == null) return null;

			final Traits elemTraits = element.getTraits();
			final PropertyKey<Iterable<NodeInterface>> methodsKey = elemTraits.key(BpmnElementTraitDefinition.METHODS_PROPERTY);
			final PropertyKey<String> nameKey                     = Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

			final Iterable<NodeInterface> methods = element.getProperty(methodsKey);
			if (methods == null) return null;

			for (final NodeInterface m : methods) {
				if (methodName.equals(m.getProperty(nameKey))) {
					return m;
				}
			}
			return null;

		} catch (Exception ex) {
			logger.warn("findElementMethod failed for task '{}', method '{}': {}",
				safeName(taskNode), methodName, ex.getMessage());
			return null;
		}
	}

	/**
	 * Look up a SchemaMethod by name attached to the BpmnDefinitions of the given
	 * ProcessInstance. Engine-context lookup; null on no match.
	 */
	private NodeInterface findProcessMethodForInstance(final NodeInterface instance, final String methodName) {
		try {
			final Traits instTraits = instance.getTraits();
			final NodeInterface definition = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY));
			return findProcessMethodOnDefinition(definition, methodName);
		} catch (Exception ex) {
			logger.warn("findProcessMethodForInstance failed for instance '{}', method '{}': {}",
				safeName(instance), methodName, ex.getMessage());
			return null;
		}
	}

	/**
	 * Look up a SchemaMethod by name attached to the BpmnDefinitions of the task's
	 * ProcessInstance. Returns null when there's no match (so the caller falls
	 * through to the next resolution tier). Read in the engine's superuser
	 * context so per-process methods are visible regardless of caller permissions.
	 */
	private NodeInterface findProcessMethod(final NodeInterface taskNode, final String methodName) {
		try {
			final Traits taskTraits = taskNode.getTraits();
			final NodeInterface instance = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.PROCESS_INSTANCE_PROPERTY));
			if (instance == null) return null;
			final NodeInterface definition = instance.getProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY));
			return findProcessMethodOnDefinition(definition, methodName);

		} catch (Exception ex) {
			logger.warn("findProcessMethod failed for task '{}', method '{}': {}",
				safeName(taskNode), methodName, ex.getMessage());
			return null;
		}
	}

	/**
	 * Iterate a BpmnDefinitions' attached methods, returning the first one whose
	 * name equals {@code methodName}. Null if the definition is null or no match.
	 */
	private NodeInterface findProcessMethodOnDefinition(final NodeInterface definition, final String methodName) {
		if (definition == null) return null;
		final Traits defTraits = definition.getTraits();
		final PropertyKey<Iterable<NodeInterface>> methodsKey = defTraits.key(BpmnProcessTraitDefinition.METHODS_PROPERTY);
		final PropertyKey<String> nameKey                     = Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

		final Iterable<NodeInterface> methods = definition.getProperty(methodsKey);
		if (methods == null) return null;

		for (final NodeInterface m : methods) {
			if (methodName.equals(m.getProperty(nameKey))) {
				return m;
			}
		}
		return null;
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
	 * {@link #fireTaskEvent}, but operates on the process root: listeners are looked
	 * up on the {@code <bpmn:process>} BpmnElement (the root of the definition's
	 * element tree), and the convention-based fallback method name targets either
	 * the subject's schema type or the {@code ProcessInstance} itself.
	 *
	 * <p>Two dispatch paths run independently:
	 * <ol>
	 *   <li><b>Per-process BPMN listeners</b> (Path A): listeners declared on the
	 *       process root via {@code <structr:processListener>}, filtered to those
	 *       whose {@code event} matches.</li>
	 *   <li><b>Schema-method fallback</b> (Path B): a method named
	 *       {@code on<EventName>} (e.g. {@code onProcessCompleted}). Looked up
	 *       first on the subject's schema type if a subject is attached, then on
	 *       {@code ProcessInstance} itself. Invoked if defined on either.</li>
	 * </ol>
	 *
	 * <p>This method is public so it can be called from the
	 * {@link org.structr.process.traits.definitions.ProcessInstanceTraitDefinition}
	 * OnModification hook (which forwards admin-UI / direct-setProperty status
	 * transitions into the engine's lifecycle dispatch path).</p>
	 *
	 * @param eventName one of {@code created|started|subjectAttached|completed|terminated|suspended|resumed}
	 * @param instance  the ProcessInstance the event is about
	 */
	public void fireProcessEvent(final String eventName, final NodeInterface instance) {

		// 1. Per-process BPMN listeners (Path A): declared on the BpmnDefinitions.
		try {
			final Traits instTraits = instance.getTraits();
			final NodeInterface defNode = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY));
			if (defNode != null) {
				final Iterable<NodeInterface> listeners = defNode.getProperty(defNode.getTraits().key(BpmnProcessTraitDefinition.PROCESS_LISTENERS_PROPERTY));
				if (listeners != null) {
					final Traits listenerTraits = Traits.of(ProcessTraits.BPMN_PROCESS_LISTENER);
					for (final NodeInterface listener : listeners) {
						final String listenerEvent = listener.getProperty(listenerTraits.key(BpmnProcessListenerTraitDefinition.EVENT_PROPERTY));
						if (!eventName.equals(listenerEvent)) {
							continue;
						}
						final String methodName = listener.getProperty(listenerTraits.key(BpmnProcessListenerTraitDefinition.METHOD_PROPERTY));
						final Boolean sync      = listener.getProperty(listenerTraits.key(BpmnProcessListenerTraitDefinition.SYNC_PROPERTY));
						invokeProcessListenerMethod(instance, methodName, eventName, Boolean.TRUE.equals(sync));
					}
				}
			}
		} catch (Exception ex) {
			logger.warn("Process listener enumeration for event '{}' on instance '{}' failed: {}",
				eventName, safeName(instance), ex.getMessage());
		}

		// 2. Schema-method fallback (Path B): convention-based method on the subject type
		// or on ProcessInstance. Always async (failures don't propagate).
		invokeProcessListenerMethod(instance, processFallbackMethodName(eventName), eventName, false);
	}

	/**
	 * Convert a process event name to the convention-based fallback method name.
	 * "completed" -> "onProcessCompleted", "subjectAttached" -> "onProcessSubjectAttached", etc.
	 */
	private String processFallbackMethodName(final String eventName) {
		if (eventName == null || eventName.isEmpty()) return null;
		return "onProcess" + Character.toUpperCase(eventName.charAt(0)) + eventName.substring(1);
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
	 * Invoke a named method for a process lifecycle event. The method is looked up first
	 * on the subject's schema type (if a subject is attached) and then on
	 * {@code ProcessInstance} itself. The first match wins; if neither has the method,
	 * silently no-op (the convention is "define the method if you want the event").
	 *
	 * <p>For BPMN-declared listeners (Path A), the {@code methodName} is taken verbatim
	 * from {@code <structr:processListener method="...">} and looked up on the same
	 * resolution path.</p>
	 *
	 * <p>The instance is bound as {@code this}; for subject-type methods, the
	 * subject is also reachable via {@code $.this.subject}.</p>
	 */
	private void invokeProcessListenerMethod(final NodeInterface instance, final String methodName,
											  final String eventName, final boolean sync) {

		if (methodName == null || methodName.isEmpty()) {
			return;
		}

		try {
			final ActionContext ctx  = new ActionContext(securityContext);
			final Traits instTraits  = instance.getTraits();
			final Arguments args = buildProcessListenerArgs(instance, eventName);

			// Tier 1: per-process method on the instance's BpmnDefinitions. Bound as
			// `this = ProcessInstance` so the body has direct access to the instance,
			// its subject (`$.this.subject`), parameters, and tokens.
			final NodeInterface processMethod = findProcessMethodForInstance(instance, methodName);
			if (processMethod != null) {
				new ScriptMethod(processMethod.as(SchemaMethod.class))
					.execute(ctx, instance, args);
				return;
			}

			// Tier 2: subject's type (most specific), then ProcessInstance.
			final NodeInterface subject = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.SUBJECT_PROPERTY));

			boolean invoked = false;
			if (subject != null) {
				final AbstractMethod m = Methods.resolveMethod(subject.getTraits(), methodName);
				if (m != null) {
					m.execute(ctx, subject, args);
					invoked = true;
				}
			}
			if (!invoked) {
				final AbstractMethod m = Methods.resolveMethod(instTraits, methodName);
				if (m != null) {
					m.execute(ctx, instance, args);
					invoked = true;
				}
			}
			// No match: convention-based hooks are optional. Silent.

		} catch (Exception ex) {
			final String msg = "Process listener '" + methodName + "' for event '" + eventName +
				"' on instance '" + safeName(instance) + "' failed: " + ex.getMessage();
			if (sync) {
				logger.error(msg);
				// v1: sync semantics log loudly, future upgrade rethrows to abort.
			} else {
				logger.warn(msg);
			}
		}
	}

	/**
	 * Returns true if the caller is an effective candidate assignee of the task --
	 * i.e. directly listed in {@code candidateAssignees}, or a member of a Group
	 * that is listed.
	 */
	private boolean isCallerEffectivePotentialOwner(final NodeInterface taskNode, final Traits taskTraits) throws FrameworkException {

		if (caller == null) {
			return false;
		}
		final Iterable<NodeInterface> candidateAssignees = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.CANDIDATE_ASSIGNEES_PROPERTY));
		if (candidateAssignees == null) {
			return false;
		}
		final Set<String> callerGroupIds = new HashSet<>();
		for (final NodeInterface g : caller.getParentsPrivileged()) {
			callerGroupIds.add(g.getUuid());
		}
		for (final NodeInterface owner : candidateAssignees) {
			if (owner.getUuid().equals(caller.getUuid()) || callerGroupIds.contains(owner.getUuid())) {
				return true;
			}
		}
		return false;
	}

	private List<NodeInterface> collect(final Iterable<NodeInterface> iter) {
		final List<NodeInterface> out = new ArrayList<>();
		if (iter != null) {
			for (final NodeInterface n : iter) {
				out.add(n);
			}
		}
		return out;
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
	private Date computeFireAt(final String timerType, final String expression) {

		if (expression == null || expression.isEmpty()) {
			return null;
		}
		final String body = expression.trim();

		try {
			if ("timeDate".equals(timerType)) {
				// ISO 8601 instant
				return Date.from(Instant.parse(body));
			}
			if ("timeDuration".equals(timerType)) {
				// ISO 8601 duration. Java's Duration.parse handles PT-style; for P-style
				// (P1D, P2W) we fall through to a small fallback parser.
				if (body.startsWith("PT")) {
					return Date.from(Instant.now().plus(Duration.parse(body)));
				}
				// Handle "PnDTm..." forms -- split at "T" and combine day-component + time-component.
				final long millis = parseIso8601DurationMillis(body);
				if (millis > 0) {
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
	private long parseIso8601DurationMillis(final String s) {
		if (s == null || !s.startsWith("P")) return -1;
		long millis = 0;
		final int tIdx = s.indexOf('T');
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

	private long parseDurationComponent(final String s, final char unit, final long unitMillis) {
		final int idx = s.indexOf(unit);
		if (idx < 0) return 0;
		// scan backwards to find the start of the number
		int start = idx - 1;
		while (start >= 0 && (Character.isDigit(s.charAt(start)) || s.charAt(start) == '.')) {
			start--;
		}
		final String num = s.substring(start + 1, idx);
		if (num.isEmpty()) return 0;
		return (long) (Double.parseDouble(num) * unitMillis);
	}

	/**
	 * Create and persist a new ProcessTimer. The timer is fired by ProcessTimerService
	 * once {@code fireAt} elapses.
	 *
	 * @param app             superuser App
	 * @param fireAt          when to fire (UTC; computed via computeFireAt)
	 * @param timerType       intermediateTimer / boundaryTimer / timerStart
	 * @param expression      original BPMN expression (for audit / re-compute)
	 * @param instance        the ProcessInstance the timer belongs to (null for timerStart)
	 * @param token           the waiting/parent token (null for timerStart)
	 * @param element         the BPMN element (intermediateCatchEvent / boundaryEvent / startEvent)
	 * @param cancelActivity  for boundary events: interrupting (true) or non-interrupting (false)
	 * @return the created ProcessTimer node
	 */
	private NodeInterface createTimer(final App app, final Date fireAt, final String timerType, final String expression,
									  final NodeInterface instance, final NodeInterface token, final NodeInterface element,
									  final boolean cancelActivity) throws FrameworkException {

		final NodeInterface timer = app.create(ProcessTraits.PROCESS_TIMER, (String) null);
		final Traits t = timer.getTraits();
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
		final String elemId = (element != null)
			? (String) element.getProperty(element.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY))
			: "?";
		timer.setProperty(t.key("name"), timerType + "@" + elemId + " fires at " + fireAt);

		logger.info("Scheduled {} for element '{}' fireAt={} (expression='{}')", timerType, elemId, fireAt, expression);
		return timer;
	}

	/**
	 * Cancel any pending timers attached to the given activity element.
	 * Called when a userTask completes / cancels / is reassigned -- boundary
	 * timers are tied to the activity's lifecycle.
	 *
	 * For boundary events on a userTask, the timer's element is the boundary
	 * event, but the boundary event's {@code attachedToRef} (or its parent
	 * relationship) points to the userTask. We find timers whose element is
	 * a boundary event attached to this activity.
	 */
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
		if (taskNode == null || boundaryBpmnId == null || boundaryBpmnId.isEmpty()) return 0;

		final App app = StructrApp.getInstance(securityContext);
		final Traits taskTraits = taskNode.getTraits();
		final NodeInterface instance = taskNode.getProperty(taskTraits.key(TaskInstanceTraitDefinition.PROCESS_INSTANCE_PROPERTY));
		if (instance == null) return 0;

		final Traits timerTraits = Traits.of(ProcessTraits.PROCESS_TIMER);
		final Traits elemTraits  = Traits.of(ProcessTraits.BPMN_ELEMENT);
		int cancelled = 0;

		for (final NodeInterface timer : app.nodeQuery(ProcessTraits.PROCESS_TIMER)
				.key(timerTraits.key(ProcessTimerTraitDefinition.STATUS_PROPERTY), ProcessTimerTraitDefinition.STATUS_PENDING)
				.getResultStream()) {

			final NodeInterface tInstance = timer.getProperty(timerTraits.key(ProcessTimerTraitDefinition.INSTANCE_PROPERTY));
			if (tInstance == null || !tInstance.getUuid().equals(instance.getUuid())) continue;

			final NodeInterface elem = timer.getProperty(timerTraits.key(ProcessTimerTraitDefinition.ELEMENT_PROPERTY));
			if (elem == null) continue;

			final String elemBpmnId = elem.getProperty(elemTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
			if (!boundaryBpmnId.equals(elemBpmnId)) continue;

			timer.setProperty(timerTraits.key(ProcessTimerTraitDefinition.STATUS_PROPERTY), ProcessTimerTraitDefinition.STATUS_CANCELLED);
			cancelled++;
			logger.info("Cancelled boundary timer '{}' on task '{}' (explicit cancellation)", boundaryBpmnId, safeName(taskNode));
		}

		return cancelled;
	}

	void cancelTimersForActivity(final App app, final NodeInterface activityElement, final NodeInterface instance) throws FrameworkException {
		if (activityElement == null || instance == null) return;

		final Traits timerTraits = Traits.of(ProcessTraits.PROCESS_TIMER);
		final Iterable<NodeInterface> timers = app.nodeQuery(ProcessTraits.PROCESS_TIMER)
			.key(timerTraits.key(ProcessTimerTraitDefinition.STATUS_PROPERTY), ProcessTimerTraitDefinition.STATUS_PENDING)
			.getResultStream();

		final String activityId = activityElement.getUuid();
		for (final NodeInterface timer : timers) {
			final NodeInterface tInstance = timer.getProperty(timerTraits.key(ProcessTimerTraitDefinition.INSTANCE_PROPERTY));
			if (tInstance == null || !tInstance.getUuid().equals(instance.getUuid())) continue;

			final NodeInterface elem = timer.getProperty(timerTraits.key(ProcessTimerTraitDefinition.ELEMENT_PROPERTY));
			if (elem == null) continue;

			// Boundary timers: element is the boundaryEvent, attached to our activity.
			final String elemType = elem.getProperty(elem.getTraits().key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY));
			if ("boundaryEvent".equals(elemType)) {
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

		final App app = StructrApp.getInstance(securityContext);
		final NodeInterface timer = elevate(app, callerTimer);
		final Traits t = timer.getTraits();

		final String status = timer.getProperty(t.key(ProcessTimerTraitDefinition.STATUS_PROPERTY));
		if (!ProcessTimerTraitDefinition.STATUS_PENDING.equals(status)) {
			return; // already fired/cancelled
		}

		final String timerType = timer.getProperty(t.key(ProcessTimerTraitDefinition.TIMER_TYPE_PROPERTY));
		final NodeInterface instance = timer.getProperty(t.key(ProcessTimerTraitDefinition.INSTANCE_PROPERTY));
		final NodeInterface token    = timer.getProperty(t.key(ProcessTimerTraitDefinition.TOKEN_PROPERTY));
		final NodeInterface element  = timer.getProperty(t.key(ProcessTimerTraitDefinition.ELEMENT_PROPERTY));

		try {
			if (ProcessTimerTraitDefinition.TIMER_INTERMEDIATE.equals(timerType)) {
				fireIntermediateTimer(app, instance, token, element);
			} else if (ProcessTimerTraitDefinition.TIMER_BOUNDARY.equals(timerType)) {
				final Boolean cancelActivity = timer.getProperty(t.key(ProcessTimerTraitDefinition.CANCEL_ACTIVITY_PROPERTY));
				fireBoundaryTimer(app, instance, token, element, Boolean.TRUE.equals(cancelActivity));
			} else {
				logger.warn("Timer type '{}' not yet supported -- timer {} marked as error", timerType, timer.getUuid());
				timer.setProperty(t.key(ProcessTimerTraitDefinition.STATUS_PROPERTY), ProcessTimerTraitDefinition.STATUS_ERROR);
				timer.setProperty(t.key(ProcessTimerTraitDefinition.ERROR_MESSAGE_PROPERTY), "timerType not implemented: " + timerType);
				return;
			}

			timer.setProperty(t.key(ProcessTimerTraitDefinition.STATUS_PROPERTY), ProcessTimerTraitDefinition.STATUS_FIRED);
			timer.setProperty(t.key(ProcessTimerTraitDefinition.FIRED_AT_PROPERTY), new Date());

		} catch (Exception ex) {
			logger.error("Timer {} failed to fire: {}", timer.getUuid(), ex.getMessage(), ex);
			timer.setProperty(t.key(ProcessTimerTraitDefinition.STATUS_PROPERTY), ProcessTimerTraitDefinition.STATUS_ERROR);
			timer.setProperty(t.key(ProcessTimerTraitDefinition.ERROR_MESSAGE_PROPERTY), ex.getMessage());
		}
	}

	private void fireIntermediateTimer(final App app, final NodeInterface instance, final NodeInterface token,
									   final NodeInterface element) throws FrameworkException {
		if (token == null || element == null) {
			logger.warn("Intermediate timer fire: missing token or element -- skipping");
			return;
		}

		// Reactivate the waiting token and advance past the catch event.
		final Traits tokenTraits = token.getTraits();
		token.setProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY), ProcessTokenTraitDefinition.STATUS_ACTIVE);

		completeTokenAndMoveToNext(app, instance, token, element);
	}

	private void fireBoundaryTimer(final App app, final NodeInterface instance, final NodeInterface parentToken,
								   final NodeInterface boundaryElement, final boolean cancelActivity) throws FrameworkException {
		if (boundaryElement == null) {
			logger.warn("Boundary timer fire: missing boundary element -- skipping");
			return;
		}

		// Find the boundary event's outgoing sequence flow.
		final Iterable<NodeInterface> outgoingFlows = boundaryElement.getProperty(
			boundaryElement.getTraits().key(BpmnElementTraitDefinition.OUTGOING_FLOWS_PROPERTY));
		NodeInterface targetElement = null;
		if (outgoingFlows != null) {
			for (final NodeInterface flow : outgoingFlows) {
				final String targetId = flow.getProperty(flow.getTraits().key(BpmnSequenceFlowTraitDefinition.TARGET_REF_ID_PROPERTY));
				if (targetId != null) {
					targetElement = findElementByBpmnId(app, instance, targetId);
					if (targetElement != null) break;
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
			final Traits ptTraits = parentToken.getTraits();
			parentToken.setProperty(ptTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY), ProcessTokenTraitDefinition.STATUS_COMPLETED);

			// Cancel any TaskInstance that was created for the parent userTask.
			cancelTaskInstanceFor(app, instance, parentToken);
		}

		// Spawn a fresh token at the boundary event's target element and advance.
		final NodeInterface newToken = createToken(app, instance, targetElement);
		advanceToken(app, instance, newToken);
	}

	/**
	 * If the parent token had an associated TaskInstance, mark it cancelled so
	 * the audit trail reflects the boundary-driven interruption.
	 */
	private void cancelTaskInstanceFor(final App app, final NodeInterface instance, final NodeInterface token) throws FrameworkException {
		if (instance == null || token == null) return;
		final NodeInterface element = token.getProperty(token.getTraits().key(ProcessTokenTraitDefinition.AT_ELEMENT_PROPERTY));
		if (element == null) return;

		final Traits taskTraits = Traits.of(ProcessTraits.TASK_INSTANCE);
		final Iterable<NodeInterface> tasks = instance.getProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.TASKS_PROPERTY));
		if (tasks == null) return;
		for (final NodeInterface task : tasks) {
			final String tStatus = task.getProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY));
			if (TaskInstanceTraitDefinition.STATUS_COMPLETED.equals(tStatus)
				|| TaskInstanceTraitDefinition.STATUS_CANCELLED.equals(tStatus)) {
				continue;
			}
			final NodeInterface tElement = task.getProperty(taskTraits.key(TaskInstanceTraitDefinition.DEFINED_BY_PROPERTY));
			if (tElement != null && tElement.getUuid().equals(element.getUuid())) {
				task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY),         TaskInstanceTraitDefinition.STATUS_CANCELLED);
				task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.CANCELLED_TIME_PROPERTY), new Date());
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
	private NodeInterface findElementByBpmnId(final App app, final NodeInterface instance, final String bpmnId) throws FrameworkException {
		if (instance == null || bpmnId == null) return null;
		final NodeInterface defNode = instance.getProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY));
		if (defNode == null) return null;
		final Iterable<NodeInterface> elements = defNode.getProperty(defNode.getTraits().key(BpmnProcessTraitDefinition.ELEMENTS_PROPERTY));
		if (elements == null) return null;
		final Traits elemTraits = Traits.of(ProcessTraits.BPMN_ELEMENT);
		final PropertyKey<String> bpmnIdKey = elemTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY);
		for (final NodeInterface el : elements) {
			if (bpmnId.equals(el.getProperty(bpmnIdKey))) return el;
		}
		return null;
	}

	/**
	 * Schedule any boundary timers attached to the given activity element.
	 * Called when a userTask is created -- iterates the definition's elements
	 * to find boundary events with timerEventDefinition that target this
	 * activity, and creates a ProcessTimer for each.
	 */
	void scheduleBoundaryTimers(final App app, final NodeInterface instance, final NodeInterface token,
								final NodeInterface activityElement) throws FrameworkException {

		if (instance == null || activityElement == null) return;

		final NodeInterface defNode = instance.getProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY));
		if (defNode == null) return;

		final String activityBpmnId = activityElement.getProperty(activityElement.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
		if (activityBpmnId == null) return;

		// Iterate the definition's elements, find boundaryEvents attached to this activity.
		final Iterable<NodeInterface> elements = defNode.getProperty(defNode.getTraits().key(BpmnProcessTraitDefinition.ELEMENTS_PROPERTY));
		if (elements == null) return;

		for (final NodeInterface el : elements) {
			final Traits elemTraits = el.getTraits();
			final String elType = el.getProperty(elemTraits.key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY));
			if (!"boundaryEvent".equals(elType)) continue;

			final String attachedTo = getAttachedToBpmnId(el);
			if (attachedTo == null || !attachedTo.equals(activityBpmnId)) continue;

			final String timerType  = el.getProperty(elemTraits.key(BpmnElementTraitDefinition.TIMER_TYPE_PROPERTY));
			final String timerValue = el.getProperty(elemTraits.key(BpmnElementTraitDefinition.TIMER_VALUE_PROPERTY));
			if (timerType == null || timerValue == null) continue; // non-timer boundary event

			final Date fireAt = computeFireAt(timerType, timerValue);
			if (fireAt == null) continue;

			// Interrupting unless cancelActivity="false"
			final String cancelAttr = getAttributeValue(el, "cancelActivity");
			final boolean interrupting = !"false".equalsIgnoreCase(cancelAttr);

			createTimer(app, fireAt, ProcessTimerTraitDefinition.TIMER_BOUNDARY, timerValue,
				instance, token, el, interrupting);
		}
	}

	// -----------------------------------------------------------------------
	// Token management helpers
	// -----------------------------------------------------------------------

	private NodeInterface createToken(final App app, final NodeInterface instance,
									  final NodeInterface element) throws FrameworkException {

		final NodeInterface token = app.create(ProcessTraits.PROCESS_TOKEN, (String) null);
		final Traits tokenTraits = token.getTraits();

		token.setProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY), ProcessTokenTraitDefinition.STATUS_ACTIVE);
		token.setProperty(tokenTraits.key(ProcessTokenTraitDefinition.PROCESS_INSTANCE_PROPERTY), instance);
		token.setProperty(tokenTraits.key(ProcessTokenTraitDefinition.AT_ELEMENT_PROPERTY), element);

		final String elemName = element.getProperty(element.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
		token.setProperty(tokenTraits.key("name"), "Token@" + elemName);

		return token;
	}

	private void moveTokenToElement(final NodeInterface token, final NodeInterface element) throws FrameworkException {

		final Traits tokenTraits = token.getTraits();
		token.setProperty(tokenTraits.key(ProcessTokenTraitDefinition.AT_ELEMENT_PROPERTY), element);

		final String elemName = element.getProperty(element.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
		token.setProperty(tokenTraits.key("name"), "Token@" + elemName);
	}

	private void completeToken(final NodeInterface token, final Traits tokenTraits) throws FrameworkException {
		token.setProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY), ProcessTokenTraitDefinition.STATUS_COMPLETED);
	}

	/**
	 * Complete the current token, find the single outgoing flow, move to its target, advance.
	 */
	private void completeTokenAndMoveToNext(final App app, final NodeInterface instance,
											final NodeInterface token, final NodeInterface element) throws FrameworkException {

		final List<NodeInterface> outgoingFlows = getOutgoingFlows(element);
		final Traits flowTraits = Traits.of(ProcessTraits.BPMN_SEQUENCE_FLOW);

		if (outgoingFlows.isEmpty()) {

			// No outgoing flow -- check if this is inside a sub-process
			final Traits elemTraits = element.getTraits();
			final String elementType = element.getProperty(elemTraits.key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY));

			if ("endEvent".equals(elementType)) {
				final NodeInterface parentElement = element.getProperty(elemTraits.key(BpmnElementTraitDefinition.PARENT_ELEMENT_PROPERTY));
				if (parentElement != null) {
					final String parentType = parentElement.getProperty(parentElement.getTraits().key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY));
					if ("subProcess".equals(parentType)) {
						// Sub-process end: complete this token, resume the parent
						completeToken(token, token.getTraits());
						resumeSubProcessParent(app, instance, parentElement);
						return;
					}
				}
			}

			// Truly terminal
			completeToken(token, token.getTraits());
			checkProcessCompletion(app, instance);
			return;
		}

		if (outgoingFlows.size() == 1) {
			final NodeInterface target = outgoingFlows.get(0).getProperty(flowTraits.key(BpmnSequenceFlowTraitDefinition.TARGET_ELEMENT_PROPERTY));
			moveTokenToElement(token, target);
			advanceToken(app, instance, token);
		} else {
			// Multiple outgoing flows on a non-gateway -- implicit exclusive gateway
			for (final NodeInterface flow : outgoingFlows) {
				final String condition = flow.getProperty(flowTraits.key(BpmnSequenceFlowTraitDefinition.CONDITION_EXPRESSION_PROPERTY));
				if (condition == null || condition.isEmpty() || evaluateCondition(condition, element, instance)) {
					final NodeInterface target = flow.getProperty(flowTraits.key(BpmnSequenceFlowTraitDefinition.TARGET_ELEMENT_PROPERTY));
					moveTokenToElement(token, target);
					advanceToken(app, instance, token);
					return;
				}
			}
			throw new FrameworkException(422, "No outgoing path matched at element: " + getBpmnId(element));
		}
	}

	private void resumeSubProcessParent(final App app, final NodeInterface instance,
										final NodeInterface subProcessElement) throws FrameworkException {

		final NodeInterface waitingToken = findWaitingToken(instance, subProcessElement);
		if (waitingToken != null) {
			final Traits tokenTraits = waitingToken.getTraits();
			waitingToken.setProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY), ProcessTokenTraitDefinition.STATUS_ACTIVE);
			completeTokenAndMoveToNext(app, instance, waitingToken, subProcessElement);
		}
	}

	// -----------------------------------------------------------------------
	// Process completion
	// -----------------------------------------------------------------------

	private void checkProcessCompletion(final App app, final NodeInterface instance) throws FrameworkException {

		final Traits instTraits = instance.getTraits();
		final Iterable<NodeInterface> tokens = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.TOKENS_PROPERTY));

		if (tokens != null) {
			for (final NodeInterface t : tokens) {
				final String status = t.getProperty(t.getTraits().key(ProcessTokenTraitDefinition.STATUS_PROPERTY));
				if (ProcessTokenTraitDefinition.STATUS_ACTIVE.equals(status) || ProcessTokenTraitDefinition.STATUS_WAITING.equals(status)) {
					return; // Still active tokens
				}
			}
		}

		// All tokens completed
		instance.setProperty(instTraits.key(ProcessInstanceTraitDefinition.STATUS_PROPERTY), ProcessInstanceTraitDefinition.STATUS_COMPLETED);
		instance.setProperty(instTraits.key(ProcessInstanceTraitDefinition.END_TIME_PROPERTY), new Date());

		logger.info("Process instance {} completed", instance.getUuid());
	}

	// -----------------------------------------------------------------------
	// Flow graph navigation
	// -----------------------------------------------------------------------

	private List<NodeInterface> getOutgoingFlows(final NodeInterface element) throws FrameworkException {

		final Traits elemTraits = element.getTraits();
		final Iterable<NodeInterface> flows = element.getProperty(elemTraits.key(BpmnElementTraitDefinition.OUTGOING_FLOWS_PROPERTY));

		final List<NodeInterface> result = new ArrayList<>();
		if (flows != null) {
			for (final NodeInterface flow : flows) {
				result.add(flow);
			}
		}
		return result;
	}

	private List<NodeInterface> getIncomingFlows(final NodeInterface element) throws FrameworkException {

		final Traits elemTraits = element.getTraits();
		final Iterable<NodeInterface> flows = element.getProperty(elemTraits.key(BpmnElementTraitDefinition.INCOMING_FLOWS_PROPERTY));

		final List<NodeInterface> result = new ArrayList<>();
		if (flows != null) {
			for (final NodeInterface flow : flows) {
				result.add(flow);
			}
		}
		return result;
	}

	private NodeInterface findStartEvent(final NodeInterface defNode, final Traits defTraits) throws FrameworkException {

		final Iterable<NodeInterface> elements = defNode.getProperty(defTraits.key(BpmnProcessTraitDefinition.ELEMENTS_PROPERTY));
		if (elements != null) {
			final Traits elemTraits = Traits.of(ProcessTraits.BPMN_ELEMENT);
			for (final NodeInterface elem : elements) {
				final String type = elem.getProperty(elemTraits.key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY));
				if ("startEvent".equals(type)) {
					return elem;
				}
			}
		}
		return null;
	}

	private NodeInterface findWaitingToken(final NodeInterface instance, final NodeInterface element) throws FrameworkException {

		final Traits instTraits = instance.getTraits();
		final Iterable<NodeInterface> tokens = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.TOKENS_PROPERTY));
		final Traits tokenTraits = Traits.of(ProcessTraits.PROCESS_TOKEN);

		if (tokens != null) {
			for (final NodeInterface t : tokens) {
				final String status = t.getProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY));
				final NodeInterface atElement = t.getProperty(tokenTraits.key(ProcessTokenTraitDefinition.AT_ELEMENT_PROPERTY));

				if (ProcessTokenTraitDefinition.STATUS_WAITING.equals(status) && atElement != null && atElement.getUuid().equals(element.getUuid())) {
					return t;
				}
			}
		}
		return null;
	}

	private int countTokensAtElement(final NodeInterface instance, final NodeInterface element) throws FrameworkException {

		final Traits instTraits = instance.getTraits();
		final Iterable<NodeInterface> tokens = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.TOKENS_PROPERTY));
		final Traits tokenTraits = Traits.of(ProcessTraits.PROCESS_TOKEN);

		int count = 0;
		if (tokens != null) {
			for (final NodeInterface t : tokens) {
				final String status = t.getProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY));
				final NodeInterface atElement = t.getProperty(tokenTraits.key(ProcessTokenTraitDefinition.AT_ELEMENT_PROPERTY));

				if (!ProcessTokenTraitDefinition.STATUS_COMPLETED.equals(status) && atElement != null && atElement.getUuid().equals(element.getUuid())) {
					count++;
				}
			}
		}
		return count;
	}

	/**
	 * Count active or waiting tokens in this instance that are NOT at the given element.
	 * Used by inclusive gateway join to determine if more tokens are still in flight.
	 */
	private int countActiveTokensNotAtElement(final NodeInterface instance, final NodeInterface element) throws FrameworkException {

		final Traits instTraits = instance.getTraits();
		final Iterable<NodeInterface> tokens = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.TOKENS_PROPERTY));
		final Traits tokenTraits = Traits.of(ProcessTraits.PROCESS_TOKEN);

		int count = 0;
		if (tokens != null) {
			for (final NodeInterface t : tokens) {
				final String status = t.getProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY));
				if (ProcessTokenTraitDefinition.STATUS_COMPLETED.equals(status)) {
					continue;
				}
				final NodeInterface atElement = t.getProperty(tokenTraits.key(ProcessTokenTraitDefinition.AT_ELEMENT_PROPERTY));
				if (atElement == null || !atElement.getUuid().equals(element.getUuid())) {
					count++;
				}
			}
		}
		return count;
	}

	private void consumeAllTokensAtElement(final App app, final NodeInterface instance,
										   final NodeInterface element) throws FrameworkException {

		final Traits instTraits = instance.getTraits();
		final Iterable<NodeInterface> tokens = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.TOKENS_PROPERTY));
		final Traits tokenTraits = Traits.of(ProcessTraits.PROCESS_TOKEN);

		if (tokens != null) {
			for (final NodeInterface t : tokens) {
				final NodeInterface atElement = t.getProperty(tokenTraits.key(ProcessTokenTraitDefinition.AT_ELEMENT_PROPERTY));
				final String status = t.getProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY));

				if (!ProcessTokenTraitDefinition.STATUS_COMPLETED.equals(status) && atElement != null && atElement.getUuid().equals(element.getUuid())) {
					completeToken(t, tokenTraits);
				}
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
	private void storeParameterValues(final App app, final NodeInterface instance,
									  final NodeInterface element, final Map<String, Object> parameters) throws FrameworkException {

		final Traits pvTraits   = Traits.of(ProcessTraits.PROCESS_PARAMETER_VALUE);
		final Traits instTraits = instance.getTraits();
		final Date now = new Date();

		final NodeInterface subject = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.SUBJECT_PROPERTY));
		final Traits subjectTraits  = subject != null ? subject.getTraits() : null;

		// Split submitted fields by name-match against the subject's schema.
		final Map<String, Object> subjectFields  = new LinkedHashMap<>();
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
			final NodeInterface elevatedSubject = elevate(app, subject);
			final PropertyMap typed = PropertyMap.inputTypeToJavaType(
				SecurityContext.getSuperUserInstance(), subject.getType(), subjectFields);
			elevatedSubject.setProperties(SecurityContext.getSuperUserInstance(), typed);
		}

		// Parameter fields: store as ProcessParameterValue. Each PV is self-describing
		// (carries its own parameterName / parameterType / stringValue) so there's no
		// separate "parameter definition" node to look up.
		for (final Map.Entry<String, Object> entry : parameterFields.entrySet()) {

			final String paramName  = entry.getKey();
			final Object paramValue = entry.getValue();

			final NodeInterface pvNode = app.create(ProcessTraits.PROCESS_PARAMETER_VALUE, (String) null);

			pvNode.setProperty(pvTraits.key(ProcessParameterValueTraitDefinition.PARAMETER_NAME_PROPERTY), paramName);
			pvNode.setProperty(pvTraits.key(ProcessParameterValueTraitDefinition.PARAMETER_TYPE_PROPERTY), inferParameterType(paramValue));
			pvNode.setProperty(pvTraits.key(ProcessParameterValueTraitDefinition.STRING_VALUE_PROPERTY),
				paramValue != null ? paramValue.toString() : null);
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
	private String inferParameterType(final Object paramValue) {
		if (paramValue instanceof Boolean)                              return ProcessParameterValueTraitDefinition.TYPE_BOOLEAN;
		if (paramValue instanceof Integer || paramValue instanceof Long) return ProcessParameterValueTraitDefinition.TYPE_INTEGER;
		if (paramValue instanceof Double  || paramValue instanceof Float) return ProcessParameterValueTraitDefinition.TYPE_DOUBLE;
		if (paramValue instanceof Date)                                  return ProcessParameterValueTraitDefinition.TYPE_DATE;
		return null;
	}

	/**
	 * Load the current (most recent) parameter values for a process instance
	 * into a map suitable for condition evaluation and listener arg-enrichment.
	 * Each PV carries its own name; there is no separate definition node.
	 */
	private Map<String, Object> loadParameterValues(final NodeInterface instance) throws FrameworkException {

		final Traits instTraits = instance.getTraits();
		final Iterable<NodeInterface> pvNodes = instance.getProperty(
			instTraits.key(ProcessInstanceTraitDefinition.PARAMETER_VALUES_PROPERTY));

		if (pvNodes == null) {
			return Collections.emptyMap();
		}

		final Traits pvTraits = Traits.of(ProcessTraits.PROCESS_PARAMETER_VALUE);

		// Collect all values, keeping only the most recent per parameter name
		// (by comparing setAt timestamps)
		final Map<String, Object> values = new LinkedHashMap<>();
		final Map<String, Date> timestamps = new LinkedHashMap<>();

		for (final NodeInterface pvNode : pvNodes) {

			final String paramName = pvNode.getProperty(pvTraits.key(ProcessParameterValueTraitDefinition.PARAMETER_NAME_PROPERTY));
			if (paramName == null) continue;

			final Date setAt = pvNode.getProperty(pvTraits.key(ProcessParameterValueTraitDefinition.SET_AT_PROPERTY));
			final Date existing = timestamps.get(paramName);

			if (existing == null || (setAt != null && setAt.after(existing))) {
				final String stringValue = pvNode.getProperty(pvTraits.key(ProcessParameterValueTraitDefinition.STRING_VALUE_PROPERTY));
				final String paramType   = pvNode.getProperty(pvTraits.key(ProcessParameterValueTraitDefinition.PARAMETER_TYPE_PROPERTY));
				values.put(paramName, convertParameterValue(stringValue, paramType));
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
	private Object convertParameterValue(final String stringValue, final String paramType) {

		if (stringValue == null) return null;
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
	private boolean evaluateCondition(final String condition, final NodeInterface contextElement,
									  final NodeInterface instance) {

		try {

			final ActionContext ctx = new ActionContext(securityContext);
			installProcessContext(ctx, instance, contextElement);

			final String expression;

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
	private void installProcessContext(final ActionContext ctx, final NodeInterface instance,
									   final NodeInterface element) throws FrameworkException {

		final Traits instTraits = instance.getTraits();
		final NodeInterface definition = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY));
		final Map<String, Object> variables = loadProcessVariables(instance);

		ctx.setConstant("process", new ProcessContext(instance, element, definition, variables));
	}

	/**
	 * Load the merged variable view for an instance: subject properties plus
	 * parameter values. PVs win on name collision (they're the explicit
	 * process-state observations; subject is the default fallback for domain
	 * data routed there by the auto-detect).
	 */
	private Map<String, Object> loadProcessVariables(final NodeInterface instance) throws FrameworkException {

		final Map<String, Object> merged = new LinkedHashMap<>();

		final NodeInterface subject = instance.getProperty(
			instance.getTraits().key(ProcessInstanceTraitDefinition.SUBJECT_PROPERTY));
		if (subject != null) {
			final Traits subjectTraits = subject.getTraits();
			for (final PropertyKey<?> key : subjectTraits.getAllPropertyKeys()) {
				final String name = key.jsonName();
				// Skip framework-managed identity / type / system fields. Domain
				// fields keep their natural names.
				if ("id".equals(name) || "type".equals(name) || "internalEntityContextPath".equals(name)) continue;
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

		final Map<String, Object> variables = loadProcessVariables(instance);
		if (variables.isEmpty()) {
			return expression;
		}

		String result = expression;
		for (final String name : variables.keySet()) {
			// Replace word-boundary occurrences of the variable name with $.process.<name>
			// Negative lookbehind for '.' prevents double-rewriting
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
	 * Append a Java value as a JavaScript literal to the StringBuilder.
	 */
	private void appendJsLiteral(final StringBuilder sb, final Object value) {

		if (value == null) {
			sb.append("null");
		} else if (value instanceof Boolean) {
			sb.append(value);
		} else if (value instanceof Number) {
			sb.append(value);
		} else {
			// String: escape quotes and wrap
			sb.append('"').append(value.toString().replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
		}
	}

	/**
	 * Get an attribute value from the bpmnAttributes JSON map.
	 */
	private String getAttributeValue(final NodeInterface element, final String attrName) {

		try {
			final String json = element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.BPMN_ATTRIBUTES_PROPERTY));
			if (json != null) {
				final int idx = json.indexOf("\"" + attrName + "\"");
				if (idx >= 0) {
					final int colonIdx = json.indexOf(':', idx);
					final int startQuote = json.indexOf('"', colonIdx + 1);
					final int endQuote = json.indexOf('"', startQuote + 1);
					if (startQuote >= 0 && endQuote > startQuote) {
						return json.substring(startQuote + 1, endQuote);
					}
				}
			}
		} catch (Exception ex) {
			logger.warn("Error reading attribute '{}': {}", attrName, ex.getMessage());
		}
		return null;
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
				if (hostBpmnId != null && !hostBpmnId.isEmpty()) return hostBpmnId;
			}
		} catch (Exception ex) {
			logger.warn("Error reading attachedTo relationship: {}", ex.getMessage());
		}
		// Legacy fallback: attachedToRef in bpmnAttributes JSON.
		return getAttributeValue(element, "attachedToRef");
	}
}
