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
package org.structr.process.entity;

import org.structr.core.graph.NodeInterface;
import org.structr.process.bpmn.BpmnElementType;

/**
 * Typed view of a {@code BpmnElement} node (flow node / activity / gateway /
 * event). Obtain via {@code node.as(BpmnElement.class)}. Replaces the repetitive
 * {@code getProperty(traits.key(...))} navigation the engine used to do inline.
 */
public interface BpmnElement extends NodeInterface {

	String getBpmnId();
	String getBpmnName();

	/** The element type as an enum ({@link BpmnElementType#UNKNOWN} if unrecognised). */
	BpmnElementType getElementType();

	/** The raw {@code bpmnElementType} string (for logging / diagnostics). */
	String getElementTypeName();

	/** True if this element is of the given type. */
	boolean isType(BpmnElementType type);

	Iterable<BpmnSequenceFlow> getOutgoingFlows();
	Iterable<BpmnSequenceFlow> getIncomingFlows();

	/** The containing element (e.g. a sub-process), or {@code null} at top level. */
	BpmnElement getParentElement();
	Iterable<BpmnElement> getChildElements();

	/** For boundary events: the activity this event is attached to, or {@code null}. */
	BpmnElement getAttachedToElement();

	String getScriptContent();
	String getEventDefinitionType();
	String getEventDefinitionId();
	String getEventDefinitionRef();
	String getTimerType();
	String getTimerValue();
	String getTimerExpressionType();
	String getDocumentation();
	String getBpmnAttributes();

	Iterable<BpmnSequenceFlow> getChildFlows();
	Iterable<BpmnPerformer> getPerformers();
	Iterable<BpmnTaskListener> getTaskListeners();
	Iterable<NodeInterface> getMethods();
}
