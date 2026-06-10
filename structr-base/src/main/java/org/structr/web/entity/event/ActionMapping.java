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
package org.structr.web.entity.event;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;

import java.util.Map;

public interface ActionMapping extends NodeInterface {

	String getEvent();
	String getAction();
	String getMethod();
	String getFlow();
	String getDataType();
	String getIdExpression();
	String getOptions();

	// Graph-relationship counterparts of method / flow / dataType. Auto-resolved from the
	// strings by the ActionMapping lifecycle hooks; nullable when the string did not
	// resolve to an existing target node.
	NodeInterface getMethodNode();
	NodeInterface getFlowNode();
	NodeInterface getDataTypeNode();

	// Convenience accessors that prefer the relationship target's name and fall back to
	// the string. Use these in render and dispatch paths that need a stable name and
	// should benefit from refactor-safety where the relationship is set.
	String getResolvedMethodName();
	String getResolvedFlowName();
	String getResolvedDataTypeName();

	// Control-process action: the process definition the action operates on, the BPMN
	// element it targets (when scoped), and the operation to perform. The relationships
	// are the source of truth; processOperation is a closed-vocabulary string enum.
	NodeInterface getControlsProcess();
	NodeInterface getTargetsElement();
	String getProcessOperation();

	String getDialogType();
	String getDialogTitle();
	String getDialogText();

	void setAction(final String action) throws FrameworkException;
	void setMethod(final String method) throws FrameworkException;
	void setSuccessBehaviour(final String successBehaviour) throws FrameworkException;
	void setFailureBehaviour(final String failureBehaviour) throws FrameworkException;

	Iterable<ParameterMapping> getParameterMappings();
	Iterable<DOMElement> getTriggerElements();
	Iterable<DOMNode> getSuccessTargets();
	Iterable<DOMNode> getFailureTargets();
	Iterable<DOMNode> getSuccessHideTargets();
	Iterable<DOMNode> getFailureHideTargets();
	Iterable<DOMNode> getSuccessNotificationElements();
	Iterable<DOMNode> getFailureNotificationElements();

	String getSuccessNotifications();
	String getSuccessBehaviour();
	String getSuccessPartial();
	String getSuccessURL();
	String getSuccessShow();
	String getSuccessHide();
	String getSuccessScope();
	String getSuccessEvent();
	String getSuccessNotificationsPartial();
	String getSuccessNotificationsEvent();
	Integer getSuccessNotificationsDelay();

	String getFailureNotifications();
	String getFailureBehaviour();
	String getFailurePartial();
	String getFailureURL();
	String getFailureShow();
	String getFailureHide();
	String getFailureScope();
	String getFailureEvent();
	String getFailureNotificationsPartial();
	String getFailureNotificationsEvent();
	Integer getFailureNotificationsDelay();

	NodeInterface cloneActionMapping(final Map<String, DOMNode> mapOfClonedNodes) throws FrameworkException;
}
