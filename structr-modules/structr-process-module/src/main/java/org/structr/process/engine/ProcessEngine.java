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
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.NodeInterface;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.process.traits.definitions.*;
import org.structr.schema.action.ActionContext;
import org.structr.process.ProcessTraits;
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
	 * Creates the ProcessInstance and a token at the start event, then advances.
	 *
	 * @return the created ProcessInstance node
	 */
	public NodeInterface startProcess(final NodeInterface defNode) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
		final Traits defTraits = defNode.getTraits();

		// Find the start event (top-level element with bpmnElementType == "startEvent")
		final NodeInterface startEvent = findStartEvent(defNode, defTraits);
		if (startEvent == null) {
			throw new FrameworkException(422, "No startEvent found in process definition");
		}

		// Create ProcessInstance
		final NodeInterface instance = app.create(ProcessTraits.PROCESS_INSTANCE, (String) null);
		final Traits instTraits = instance.getTraits();

		instance.setProperty(instTraits.key(ProcessInstanceTraitDefinition.STATUS_PROPERTY), ProcessInstanceTraitDefinition.STATUS_RUNNING);
		instance.setProperty(instTraits.key(ProcessInstanceTraitDefinition.START_TIME_PROPERTY), new Date());
		instance.setProperty(instTraits.key(ProcessInstanceTraitDefinition.DEFINITION_PROPERTY), defNode);

		final String processName = defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.PROCESS_NAME_PROPERTY));
		instance.setProperty(instTraits.key("name"), processName != null ? processName : "Process Instance");

		// Grant the initiator read access on the ProcessInstance so they can observe their own instance.
		grant(instance, caller, Permission.read);

		// Create token at start event
		final NodeInterface token = createToken(app, instance, startEvent);

		// Advance the token from the start event (start events are pass-through)
		advanceToken(app, instance, token);

		return instance;
	}

	/**
	 * Complete a user task and advance the process.
	 *
	 * @param parameters optional key-value map of process parameter values set by this task
	 */
	public void completeTask(final NodeInterface taskNode, final Map<String, Object> parameters) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
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

		// Store parameter values provided by the task completion
		if (parameters != null && !parameters.isEmpty()) {
			storeParameterValues(app, instance, userTaskElement, parameters);
		}

		// Reactivate the token and move it past the user task to the next element
		final Traits tokenTraits = waitingToken.getTraits();
		waitingToken.setProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY), ProcessTokenTraitDefinition.STATUS_ACTIVE);

		completeTokenAndMoveToNext(app, instance, waitingToken, userTaskElement);
	}

	/**
	 * Signal an intermediate catch event, resuming the waiting token.
	 * The catch event is identified by its bpmnId within the process instance.
	 *
	 * @param eventBpmnId the bpmnId of the intermediateCatchEvent element
	 * @param parameters  optional key-value map of process parameter values
	 */
	public void signalEvent(final NodeInterface instanceNode, final String eventBpmnId,
						   final Map<String, Object> parameters) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
		final Traits instTraits = instanceNode.getTraits();

		// Verify instance is running
		final String status = instanceNode.getProperty(instTraits.key(ProcessInstanceTraitDefinition.STATUS_PROPERTY));
		if (!ProcessInstanceTraitDefinition.STATUS_RUNNING.equals(status)) {
			throw new FrameworkException(422, "Process instance is not running (status: " + status + ")");
		}

		// Find the catch event element by bpmnId
		final Traits elemTraits = Traits.of(ProcessTraits.BPMN_ELEMENT);
		final NodeInterface catchElement = app.nodeQuery(ProcessTraits.BPMN_ELEMENT)
			.and().key(elemTraits.key(BpmnElementTraitDefinition.BPMN_ID_PROPERTY), eventBpmnId)
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
			currentElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.BPMN_ID_PROPERTY)),
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
				// Create a TaskInstance and put the token in waiting state
				createTaskInstance(app, instance, currentElement);
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
				// Waiting for an external event -- token waits
				token.setProperty(tokenTraits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY), ProcessTokenTraitDefinition.STATUS_WAITING);
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
					currentElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.BPMN_ID_PROPERTY)));
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

			final String flowId = flow.getProperty(flowTraits.key(BpmnSequenceFlowTraitDefinition.BPMN_ID_PROPERTY));

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
				final String flowId = flow.getProperty(flowTraits.key(BpmnSequenceFlowTraitDefinition.BPMN_ID_PROPERTY));

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

		task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY), TaskInstanceTraitDefinition.STATUS_CREATED);
		task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.CREATED_TIME_PROPERTY), new Date());
		task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.PROCESS_INSTANCE_PROPERTY), instance);
		task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.DEFINED_BY_PROPERTY), userTaskElement);

		// Set display name from the BPMN element name
		final Traits elemTraits = userTaskElement.getTraits();
		final String taskName = userTaskElement.getProperty(elemTraits.key(BpmnElementTraitDefinition.BPMN_NAME_PROPERTY));
		task.setProperty(taskTraits.key("name"), taskName != null ? taskName : "User Task");

		// Extract assignee from bpmnAttributes if present (e.g. Camunda assignee)
		final String assignee = getAttributeValue(userTaskElement, "camunda:assignee");
		if (assignee != null) {
			task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.ASSIGNEE_PROPERTY), assignee);
			task.setProperty(taskTraits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY), TaskInstanceTraitDefinition.STATUS_ASSIGNED);

			// Grant the assignee read + write on this TaskInstance so they can see and complete it.
			final NodeInterface assigneeNode = app.nodeQuery(StructrTraits.PRINCIPAL).name(assignee).getFirst();
			if (assigneeNode != null) {
				grant(task, assigneeNode.as(Principal.class), Permission.read, Permission.write);
			} else {
				logger.warn("Assignee '{}' for task '{}' could not be resolved to a Principal; no ACL grant applied.", assignee, taskName);
			}
		}

		logger.info("Created task instance '{}' for process {}", taskName, instance.getUuid());
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

		final String elemName = element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.BPMN_ID_PROPERTY));
		token.setProperty(tokenTraits.key("name"), "Token@" + elemName);

		return token;
	}

	private void moveTokenToElement(final NodeInterface token, final NodeInterface element) throws FrameworkException {

		final Traits tokenTraits = token.getTraits();
		token.setProperty(tokenTraits.key(ProcessTokenTraitDefinition.AT_ELEMENT_PROPERTY), element);

		final String elemName = element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.BPMN_ID_PROPERTY));
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

		final Iterable<NodeInterface> elements = defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.ELEMENTS_PROPERTY));
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
	 * Store parameter values for a process instance. Each key-value pair creates
	 * a new ProcessParameterValue node, preserving the audit trail.
	 */
	private void storeParameterValues(final App app, final NodeInterface instance,
									  final NodeInterface element, final Map<String, Object> parameters) throws FrameworkException {

		final Traits pvTraits = Traits.of(ProcessTraits.PROCESS_PARAMETER_VALUE);
		final Date now = new Date();

		for (final Map.Entry<String, Object> entry : parameters.entrySet()) {

			final String paramName = entry.getKey();
			final Object paramValue = entry.getValue();

			final NodeInterface pvNode = app.create(ProcessTraits.PROCESS_PARAMETER_VALUE, (String) null);

			pvNode.setProperty(pvTraits.key(ProcessParameterValueTraitDefinition.STRING_VALUE_PROPERTY),
				paramValue != null ? paramValue.toString() : null);
			pvNode.setProperty(pvTraits.key(ProcessParameterValueTraitDefinition.SET_AT_PROPERTY), now);
			pvNode.setProperty(pvTraits.key(ProcessParameterValueTraitDefinition.PROCESS_INSTANCE_PROPERTY), instance);
			pvNode.setProperty(pvTraits.key(ProcessParameterValueTraitDefinition.SET_BY_ELEMENT_PROPERTY), element);
			pvNode.setProperty(pvTraits.key("name"), paramName);

			// Find or create the ProcessParameter definition and link it
			final NodeInterface paramDef = findOrCreateParameterDefinition(app, element, paramName, paramValue);
			pvNode.setProperty(pvTraits.key(ProcessParameterValueTraitDefinition.PARAMETER_PROPERTY), paramDef);
		}
	}

	/**
	 * Load the current (most recent) parameter values for a process instance
	 * into a map suitable for condition evaluation.
	 */
	private Map<String, Object> loadParameterValues(final NodeInterface instance) throws FrameworkException {

		final Traits instTraits = instance.getTraits();
		final Iterable<NodeInterface> pvNodes = instance.getProperty(
			instTraits.key(ProcessInstanceTraitDefinition.PARAMETER_VALUES_PROPERTY));

		if (pvNodes == null) {
			return Collections.emptyMap();
		}

		final Traits pvTraits    = Traits.of(ProcessTraits.PROCESS_PARAMETER_VALUE);
		final Traits paramTraits = Traits.of(ProcessTraits.PROCESS_PARAMETER);

		// Collect all values, keeping only the most recent per parameter name
		// (by comparing setAt timestamps)
		final Map<String, Object> values = new LinkedHashMap<>();
		final Map<String, Date> timestamps = new LinkedHashMap<>();

		for (final NodeInterface pvNode : pvNodes) {

			final NodeInterface paramDef = pvNode.getProperty(pvTraits.key(ProcessParameterValueTraitDefinition.PARAMETER_PROPERTY));
			if (paramDef == null) continue;

			final String paramName = paramDef.getProperty(paramTraits.key(ProcessParameterTraitDefinition.PARAMETER_NAME_PROPERTY));
			if (paramName == null) continue;

			final Date setAt = pvNode.getProperty(pvTraits.key(ProcessParameterValueTraitDefinition.SET_AT_PROPERTY));
			final Date existing = timestamps.get(paramName);

			if (existing == null || (setAt != null && setAt.after(existing))) {
				final String stringValue = pvNode.getProperty(pvTraits.key(ProcessParameterValueTraitDefinition.STRING_VALUE_PROPERTY));
				values.put(paramName, convertParameterValue(stringValue, paramDef, paramTraits));
				timestamps.put(paramName, setAt);
			}
		}

		return values;
	}

	/**
	 * Convert a stored string value to the appropriate Java type based on
	 * the ProcessParameter's parameterType.
	 */
	private Object convertParameterValue(final String stringValue, final NodeInterface paramDef,
										 final Traits paramTraits) throws FrameworkException {

		if (stringValue == null) return null;

		final String paramType = paramDef.getProperty(paramTraits.key(ProcessParameterTraitDefinition.PARAMETER_TYPE_PROPERTY));

		if (paramType == null || ProcessParameterTraitDefinition.TYPE_STRING.equals(paramType)) {
			return stringValue;
		}

		try {
			return switch (paramType) {
				case ProcessParameterTraitDefinition.TYPE_BOOLEAN -> Boolean.parseBoolean(stringValue);
				case ProcessParameterTraitDefinition.TYPE_INTEGER -> Integer.parseInt(stringValue);
				case ProcessParameterTraitDefinition.TYPE_DOUBLE  -> Double.parseDouble(stringValue);
				default -> stringValue;
			};
		} catch (NumberFormatException e) {
			return stringValue;
		}
	}

	/**
	 * Find an existing ProcessParameter definition by name on the given element,
	 * or create one if it doesn't exist.
	 */
	private NodeInterface findOrCreateParameterDefinition(final App app, final NodeInterface element,
														  final String paramName, final Object paramValue) throws FrameworkException {

		final Traits elemTraits = element.getTraits();
		final Iterable<NodeInterface> existingParams = element.getProperty(
			elemTraits.key(BpmnElementTraitDefinition.PARAMETERS_PROPERTY));

		if (existingParams != null) {
			final Traits paramTraits = Traits.of(ProcessTraits.PROCESS_PARAMETER);
			for (final NodeInterface param : existingParams) {
				final String name = param.getProperty(paramTraits.key(ProcessParameterTraitDefinition.PARAMETER_NAME_PROPERTY));
				if (paramName.equals(name)) {
					return param;
				}
			}
		}

		// Create new parameter definition
		final NodeInterface paramDef = app.create(ProcessTraits.PROCESS_PARAMETER, (String) null);
		final Traits paramTraits = paramDef.getTraits();

		paramDef.setProperty(paramTraits.key(ProcessParameterTraitDefinition.PARAMETER_NAME_PROPERTY), paramName);
		paramDef.setProperty(paramTraits.key(ProcessParameterTraitDefinition.ELEMENT_PROPERTY), element);
		paramDef.setProperty(paramTraits.key("name"), paramName);

		// Infer parameter type from the value if provided
		if (paramValue instanceof Boolean) {
			paramDef.setProperty(paramTraits.key(ProcessParameterTraitDefinition.PARAMETER_TYPE_PROPERTY), ProcessParameterTraitDefinition.TYPE_BOOLEAN);
		} else if (paramValue instanceof Integer || paramValue instanceof Long) {
			paramDef.setProperty(paramTraits.key(ProcessParameterTraitDefinition.PARAMETER_TYPE_PROPERTY), ProcessParameterTraitDefinition.TYPE_INTEGER);
		} else if (paramValue instanceof Double || paramValue instanceof Float) {
			paramDef.setProperty(paramTraits.key(ProcessParameterTraitDefinition.PARAMETER_TYPE_PROPERTY), ProcessParameterTraitDefinition.TYPE_DOUBLE);
		}

		return paramDef;
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
	 * can access process data via $.process.<param>, $.process.instance, etc.
	 */
	private void installProcessContext(final ActionContext ctx, final NodeInterface instance,
									   final NodeInterface element) throws FrameworkException {

		final Traits instTraits = instance.getTraits();
		final NodeInterface definition = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.DEFINITION_PROPERTY));
		final Map<String, Object> paramValues = loadParameterValues(instance);

		ctx.setConstant("process", new ProcessContext(instance, element, definition, paramValues));
	}

	/**
	 * Rewrite bare variable references in BPMN condition expressions to
	 * $.process.<name> references. For example:
	 *   approved == true       -> $.process.approved == true
	 *   claimAmount < 1000     -> $.process.claimAmount < 1000
	 *   delivery == 'express'  -> $.process.delivery == 'express'
	 *
	 * Only known process parameter names are rewritten; JS keywords, literals,
	 * and operators are left unchanged.
	 */
	private String rewriteConditionExpression(final String expression, final NodeInterface instance) throws FrameworkException {

		final Map<String, Object> paramValues = loadParameterValues(instance);
		if (paramValues.isEmpty()) {
			return expression;
		}

		String result = expression;
		for (final String paramName : paramValues.keySet()) {
			// Replace word-boundary occurrences of the parameter name with $.process.<name>
			// Negative lookbehind for '.' prevents double-rewriting
			result = result.replaceAll("(?<!\\.)\\b" + paramName + "\\b", "\\$.process." + paramName);
		}

		return result;
	}

	// -----------------------------------------------------------------------
	// Utility helpers
	// -----------------------------------------------------------------------

	private String getBpmnId(final NodeInterface element) throws FrameworkException {
		return element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.BPMN_ID_PROPERTY));
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
}
